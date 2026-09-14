'use strict';

const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const { URL } = require('node:url');

// Paths
const DATA_DIR = path.join(__dirname, 'data');
const STORE_PATH = path.join(DATA_DIR, 'store.json');
const PUBLIC_DIR = path.join(__dirname, 'public');
const CONFIG_PATH = path.join(__dirname, 'config.json');

// Default Config
let config = {
  port: 3000,
  host: '0.0.0.0',
  hubSecret: 'your-token-monitor-secret',
  webPin: '8888',
  currencyRate: 6.708,
  currencySymbol: '¥'
};

if (fs.existsSync(CONFIG_PATH)) {
  try {
    const raw = fs.readFileSync(CONFIG_PATH, 'utf8');
    config = { ...config, ...JSON.parse(raw) };
  } catch (e) {
    console.error('[Config] Failed to parse config.json, using defaults:', e.message);
  }
}

if (process.env.PORT) config.port = parseInt(process.env.PORT, 10);
if (process.env.HUB_SECRET) config.hubSecret = process.env.HUB_SECRET;
if (process.env.WEB_PIN) config.webPin = process.env.WEB_PIN;

// Ensure dirs
if (!fs.existsSync(DATA_DIR)) fs.mkdirSync(DATA_DIR, { recursive: true });

// Data Store
let store = {
  devices: {},
  subscriptions: [],
  lastIngestAt: null
};

if (fs.existsSync(STORE_PATH)) {
  try {
    store = JSON.parse(fs.readFileSync(STORE_PATH, 'utf8'));
    console.log(`[Store] Loaded existing data. Devices count: ${Object.keys(store.devices || {}).length}`);
  } catch (e) {
    console.error('[Store] Could not load store.json, starting fresh:', e.message);
  }
}

function persistStore() {
  try {
    fs.writeFileSync(STORE_PATH, JSON.stringify(store, null, 2), 'utf8');
  } catch (e) {
    console.error('[Store] Failed to persist data:', e.message);
  }
}

// SSE Clients
const sseClients = new Set();
const mobileSseClients = new Set();

function broadcastToClients() {
  const stats = buildFullStats();
  const mobileData = buildMobileData();
  
  const statsPayload = `event: stats\ndata: ${JSON.stringify({ type: 'stats', reason: 'ingest', stats, at: new Date().toISOString() })}\n\n`;
  for (const client of sseClients) {
    try { client.write(statsPayload); } catch (_) { sseClients.delete(client); }
  }

  const mobilePayload = `event: update\ndata: ${JSON.stringify(mobileData)}\n\n`;
  for (const client of mobileSseClients) {
    try { client.write(mobilePayload); } catch (_) { mobileSseClients.delete(client); }
  }
}

// Auth Helper
function isAuthorized(req, secret) {
  if (!secret) return true;
  const auth = req.headers.authorization || '';
  if (auth.toLowerCase().startsWith('bearer ')) {
    return auth.slice(7).trim() === secret;
  }
  return String(req.headers['x-token-monitor-secret'] || '').trim() === secret;
}

function isWebAuthorized(req) {
  if (!config.webPin) return true;
  const pinHeader = req.headers['x-web-pin'] || '';
  if (pinHeader === config.webPin) return true;
  try {
    const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    if (url.searchParams.get('pin') === config.webPin) return true;
  } catch (_) {}
  return false;
}

// Stats Aggregation Helpers
function buildFullStats() {
  const devicesList = Object.values(store.devices || {});
  let periods = { today: { totalTokens: 0, costUsd: 0, clientModels: {}, clientModelCosts: {}, sessions: {} } };
  let limits = { providers: [] };
  let historyPreview = { daily: [], monthly: [], summary: { totalTokens: 0, totalCost: 0 } };

  if (devicesList.length > 0) {
    // Sort latest device
    const primary = devicesList[0];
    if (primary.periods) periods = primary.periods;
    if (primary.limits) limits = primary.limits;
    if (primary.history) {
      historyPreview = {
        daily: primary.history.daily || [],
        monthly: primary.history.monthly || [],
        summary: primary.history.summary || { totalTokens: 0, totalCost: 0 }
      };
    } else if (primary.historyPreview) {
      historyPreview = primary.historyPreview;
    }
  }

  return {
    updatedAt: store.lastIngestAt || new Date().toISOString(),
    devices: devicesList.map(d => ({
      deviceId: d.deviceId || d.id,
      deviceLabel: d.deviceLabel || d.name || 'Desktop',
      platform: d.platform || 'windows',
      lastSeenAt: d.receivedAt || d.updatedAt
    })),
    periods,
    limits,
    historyPreview
  };
}

function buildMobileData() {
  const devicesList = Object.values(store.devices || {});
  const activeDevice = devicesList[0] || null;
  const rate = config.currencyRate || 7.0;
  const sym = config.currencySymbol || '¥';

  let todayTokens = 0;
  let todayCostUsd = 0;
  let todayMessages = 0;
  let todayActiveTimeMs = 0;
  let modelsList = [];
  let limitsList = [];
  let dailyList = [];
  let summary = { totalTokens: 0, totalCostUsd: 0, totalCostCny: 0, activeDays: 0, streak: 0 };

  if (activeDevice) {
    // Today Stats
    const today = activeDevice.periods?.today || {};
    todayTokens = today.totalTokens || 0;
    todayCostUsd = today.costUsd || 0;

    // Sessions & Messages
    if (today.sessions) {
      const sessList = Object.values(today.sessions);
      todayMessages = sessList.reduce((sum, s) => sum + (s.messageCount || 0), 0);
    }

    // Models breakdown
    if (today.clientModels) {
      for (const [provider, modelMap] of Object.entries(today.clientModels)) {
        for (const [modelName, tokens] of Object.entries(modelMap)) {
          const cost = today.clientModelCosts?.[provider]?.[modelName] || 0;
          modelsList.push({
            provider,
            name: modelName,
            tokens,
            costUsd: cost,
            costCny: (cost * rate).toFixed(2),
            percent: todayTokens > 0 ? ((tokens / todayTokens) * 100).toFixed(1) : 0
          });
        }
      }
    }
    modelsList.sort((a, b) => b.tokens - a.tokens);

    // Limits & Quotas
    if (activeDevice.limits?.providers) {
      for (const p of activeDevice.limits.providers) {
        if (p.windows && p.windows.length > 0) {
          for (const w of p.windows) {
            limitsList.push({
              provider: p.provider,
              account: p.accountLabel || '',
              metric: w.metric || 'tokens',
              label: w.label || p.provider,
              usedPercent: w.usedPercent != null ? Number(w.usedPercent).toFixed(1) : null,
              remainingPercent: w.remainingPercent != null ? Number(w.remainingPercent).toFixed(1) : null,
              used: w.used,
              limit: w.limit,
              remaining: w.remaining,
              resetsAt: w.resetsAt,
              windowMinutes: w.windowMinutes
            });
          }
        }
      }
    }

    // Daily History (last 7 days)
    const history = activeDevice.history || activeDevice.historyPreview || {};
    if (Array.isArray(history.daily)) {
      dailyList = history.daily.slice(-7).map(d => ({
        date: d.date,
        tokens: d.tokens,
        tokensDisplay: d.tokens > 1e6 ? `${(d.tokens / 1e6).toFixed(1)}M` : `${Math.round(d.tokens / 1e3)}k`,
        costUsd: d.cost,
        costCny: (d.cost * rate).toFixed(2)
      }));
    }

    if (history.summary) {
      summary = {
        totalTokens: history.summary.totalTokens || 0,
        totalCostUsd: (history.summary.totalCost || 0).toFixed(2),
        totalCostCny: ((history.summary.totalCost || 0) * rate).toFixed(2),
        activeDays: history.summary.activeDays || 0,
        streak: history.summary.currentStreak || 0
      };
    }
  }

  const lastSeen = activeDevice?.receivedAt || store.lastIngestAt;
  const isOnline = lastSeen ? (Date.now() - new Date(lastSeen).getTime() < 10 * 60 * 1000) : false;

  return {
    isOnline,
    lastSeenAt: lastSeen,
    currencySymbol: sym,
    currencyRate: rate,
    device: activeDevice ? {
      id: activeDevice.deviceId || activeDevice.id,
      label: activeDevice.deviceLabel || 'Windows Desktop',
      platform: activeDevice.platform || 'windows'
    } : null,
    today: {
      tokens: todayTokens,
      tokensFormatted: Number(todayTokens).toLocaleString(),
      tokensShort: todayTokens > 1e6 ? `${(todayTokens / 1e6).toFixed(1)}M` : `${Math.round(todayTokens / 1e3)}k`,
      costUsd: todayCostUsd.toFixed(2),
      costCny: (todayCostUsd * rate).toFixed(2),
      messages: todayMessages
    },
    summary,
    models: modelsList,
    limits: limitsList,
    daily: dailyList
  };
}

// Server Creation
const server = http.createServer((req, res) => {
  // CORS
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Authorization, Content-Type, x-token-monitor-secret, x-web-pin');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    return res.end();
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);

  // --- API Routes ---

  // Health check
  if (url.pathname === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({
      ok: true,
      role: 'hub',
      runtime: 'node-hub-mobile',
      version: 1,
      deviceCount: Object.keys(store.devices).length,
      secretRequired: Boolean(config.hubSecret),
      now: new Date().toISOString()
    }));
  }

  // Token Monitor Ingest
  if (req.method === 'POST' && url.pathname === '/api/ingest') {
    if (!isAuthorized(req, config.hubSecret)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'unauthorized' }));
    }

    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        const payload = JSON.parse(body);
        const deviceId = String(payload.deviceId || payload.id || 'default');
        store.devices[deviceId] = {
          ...store.devices[deviceId],
          ...payload,
          deviceId,
          receivedAt: new Date().toISOString()
        };
        store.lastIngestAt = new Date().toISOString();
        persistStore();
        broadcastToClients();

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          ok: true,
          deviceId,
          stats: buildFullStats()
        }));
        console.log(`[Ingest] Received update from ${deviceId} (${new Date().toLocaleTimeString()})`);
      } catch (err) {
        console.error('[Ingest] Error parsing payload:', err.message);
        res.writeHead(400, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'bad_request', message: err.message }));
      }
    });
    return;
  }

  // Desktop Client SSE Stream
  if (req.method === 'GET' && url.pathname === '/api/stats/stream') {
    if (!isAuthorized(req, config.hubSecret)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'unauthorized' }));
    }
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no'
    });
    const snapshot = `event: stats\ndata: ${JSON.stringify({ type: 'stats', reason: 'snapshot', stats: buildFullStats(), at: new Date().toISOString() })}\n\n`;
    res.write(snapshot);
    sseClients.add(res);
    req.on('close', () => sseClients.delete(res));
    return;
  }

  // Full Stats
  if (req.method === 'GET' && url.pathname === '/api/stats') {
    if (!isAuthorized(req, config.hubSecret) && !isWebAuthorized(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'unauthorized' }));
    }
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(JSON.stringify(buildFullStats()));
  }

  // Mobile App Data Endpoint
  if (req.method === 'GET' && url.pathname === '/api/mobile/data') {
    if (!isWebAuthorized(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'invalid_pin' }));
    }
    res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8' });
    return res.end(JSON.stringify(buildMobileData()));
  }

  // Mobile App SSE Stream
  if (req.method === 'GET' && url.pathname === '/api/mobile/stream') {
    if (!isWebAuthorized(req)) {
      res.writeHead(401, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ error: 'invalid_pin' }));
    }
    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache, no-transform',
      'Connection': 'keep-alive',
      'X-Accel-Buffering': 'no'
    });
    const initial = `event: update\ndata: ${JSON.stringify(buildMobileData())}\n\n`;
    res.write(initial);
    mobileSseClients.add(res);
    const hb = setInterval(() => { try { res.write(': hb\n\n'); } catch (_) {} }, 25000);
    req.on('close', () => {
      clearInterval(hb);
      mobileSseClients.delete(res);
    });
    return;
  }

  // --- Static Files (PWA Frontend) ---
  let reqPath = url.pathname === '/' ? '/index.html' : url.pathname;
  const safePath = path.normalize(reqPath).replace(/^(\.\.[\/\\])+/, '');
  const filePath = path.join(PUBLIC_DIR, safePath);

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    const mimeTypes = {
      '.html': 'text/html; charset=utf-8',
      '.css': 'text/css; charset=utf-8',
      '.js': 'application/javascript; charset=utf-8',
      '.json': 'application/json; charset=utf-8',
      '.svg': 'image/svg+xml',
      '.png': 'image/png',
      '.ico': 'image/x-icon'
    };

    const headers = { 'Content-Type': mimeTypes[ext] || 'application/octet-stream' };
    if (ext === '.html' || safePath.includes('sw.js')) {
      headers['Cache-Control'] = 'no-cache, no-store, must-revalidate';
    } else {
      headers['Cache-Control'] = 'public, max-age=86400';
    }

    res.writeHead(200, headers);
    fs.createReadStream(filePath).pipe(res);
    return;
  }

  // 404
  res.writeHead(404, { 'Content-Type': 'text/plain' });
  res.end('Not Found');
});

server.listen(config.port, config.host, () => {
  console.log(`=======================================================`);
  console.log(`🚀 Token Monitor Mobile Hub Server running!`);
  console.log(`📡 Local:   http://localhost:${config.port}`);
  console.log(`📱 Mobile:  Open in browser with PIN: ${config.webPin}`);
  console.log(`🔐 Hub Key: ${config.hubSecret.slice(0, 8)}...`);
  console.log(`=======================================================`);
});
