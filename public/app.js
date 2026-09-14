// Token Monitor Mobile PWA Application Logic
(function () {
  'use strict';

  let currentData = null;
  let activeTab = 'today';
  let sseSource = null;

  // DOM Elements
  const statusBadge = document.getElementById('statusBadge');
  const statusText = document.getElementById('statusText');
  const refreshBtn = document.getElementById('refreshBtn');
  const heroCardTag = document.getElementById('heroCardTag');
  const heroDeviceName = document.getElementById('heroDeviceName');
  const heroMainNum = document.getElementById('heroMainNum');
  const heroApproxPill = document.getElementById('heroApproxPill');
  const heroSubText = document.getElementById('heroSubText');
  const gridLabel1 = document.getElementById('gridLabel1');
  const gridVal1 = document.getElementById('gridVal1');
  const gridLabel2 = document.getElementById('gridLabel2');
  const gridVal2 = document.getElementById('gridVal2');
  const modelCountLabel = document.getElementById('modelCountLabel');
  const modelList = document.getElementById('modelList');
  const quotaGrid = document.getElementById('quotaGrid');
  const trendChart = document.getElementById('trendChart');
  const trendSummary = document.getElementById('trendSummary');
  const lastUpdatedLabel = document.getElementById('lastUpdatedLabel');

  // PIN Elements
  const pinModal = document.getElementById('pinModal');
  const pinInput = document.getElementById('pinInput');
  const pinError = document.getElementById('pinError');
  const pinSubmitBtn = document.getElementById('pinSubmitBtn');

  // Register PWA Service Worker
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('/sw.js').catch((err) => {
      console.warn('[SW] Registration failed:', err);
    });
  }

  // PIN Storage
  function getStoredPin() {
    return localStorage.getItem('tm_web_pin') || '';
  }

  function setStoredPin(pin) {
    localStorage.setItem('tm_web_pin', pin);
  }

  function showPinModal(errorMsg) {
    pinModal.classList.add('active');
    pinInput.value = '';
    pinInput.focus();
    pinError.textContent = errorMsg || '';
  }

  function hidePinModal() {
    pinModal.classList.remove('active');
    pinError.textContent = '';
  }

  // Format Numbers
  function formatTokens(num) {
    if (!num) return '0';
    if (num >= 1e9) return (num / 1e9).toFixed(2) + 'B';
    if (num >= 1e6) return (num / 1e6).toFixed(1) + 'M';
    if (num >= 1e3) return (num / 1e3).toFixed(1) + 'k';
    return String(num);
  }

  function timeAgo(dateString) {
    if (!dateString) return '从未';
    const seconds = Math.floor((Date.now() - new Date(dateString).getTime()) / 1000);
    if (seconds < 10) return '刚刚';
    if (seconds < 60) return seconds + ' 秒前';
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return minutes + ' 分钟前';
    const hours = Math.floor(minutes / 60);
    if (hours < 24) return hours + ' 小时前';
    return Math.floor(hours / 24) + ' 天前';
  }

  // Render UI
  function renderAll() {
    if (!currentData) return;

    const { isOnline, lastSeenAt, device, today, summary, models, limits, daily, currencySymbol } = currentData;

    // Status Badge
    if (isOnline) {
      statusBadge.className = 'status-badge';
      statusText.textContent = '在线';
    } else {
      statusBadge.className = 'status-badge offline';
      statusText.textContent = lastSeenAt ? '已离线 (' + timeAgo(lastSeenAt) + ')' : '离线';
    }

    heroDeviceName.textContent = device?.label || 'Desktop';
    lastUpdatedLabel.textContent = '最后同步: ' + timeAgo(lastSeenAt);

    // Hero Section based on Tab
    if (activeTab === 'today') {
      heroCardTag.textContent = 'TODAY TOKENS';
      heroMainNum.textContent = Number(today.tokens || 0).toLocaleString();
      heroApproxPill.textContent = '≈ ' + (today.tokensShort || '0');
      heroSubText.textContent = '今日累计产生';

      gridLabel1.textContent = '今日预计支出';
      gridVal1.textContent = (currencySymbol || '¥') + today.costCny;
      gridLabel2.textContent = '今日交互消息';
      gridVal2.textContent = (today.messages || 0) + ' 条';
    } else {
      heroCardTag.textContent = 'ALL-TIME TOKENS';
      heroMainNum.textContent = Number(summary.totalTokens || 0).toLocaleString();
      heroApproxPill.textContent = '≈ ' + formatTokens(summary.totalTokens);
      heroSubText.textContent = '历史累计 Token';

      gridLabel1.textContent = '历史累计支出';
      gridVal1.textContent = (currencySymbol || '¥') + summary.totalCostCny;
      gridLabel2.textContent = '连续活跃天数';
      gridVal2.textContent = (summary.streak || 0) + ' 天 (总 ' + summary.activeDays + ' 天)';
    }

    // Model Distribution
    renderModels(models);

    // Limits & Quotas
    renderQuotas(limits);

    // 7-Day Chart
    renderChart(daily);
  }

  function renderModels(models) {
    if (!models || models.length === 0) {
      modelCountLabel.textContent = '暂无模型数据';
      modelList.innerHTML = '<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; padding: 12px;">今日暂无活跃模型</div>';
      return;
    }

    modelCountLabel.textContent = models.length + ' 个活跃模型';
    let html = '';
    models.forEach((m) => {
      html += `
        <div class="model-row">
          <div class="model-info-row">
            <div>
              <span class="model-name">${m.name}</span>
              <span class="model-provider-badge">${m.provider}</span>
            </div>
            <div class="model-stats">
              <span>${formatTokens(m.tokens)}</span>
              <span style="color: var(--accent-emerald);">¥${m.costCny}</span>
            </div>
          </div>
          <div class="bar-track">
            <div class="bar-fill" style="width: ${Math.max(2, m.percent)}%;"></div>
          </div>
        </div>
      `;
    });
    modelList.innerHTML = html;
  }

  function renderQuotas(limits) {
    if (!limits || limits.length === 0) {
      quotaGrid.innerHTML = '<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; padding: 12px;">暂无配额限制数据</div>';
      return;
    }

    let html = '';
    limits.forEach((q) => {
      let percentDisplay = q.remainingPercent != null ? q.remainingPercent + '%' : '正常';
      let pillClass = 'quota-pill';
      if (q.remainingPercent != null) {
        if (q.remainingPercent < 15) pillClass += ' danger';
        else if (q.remainingPercent < 40) pillClass += ' warn';
      }

      let resetText = '';
      if (q.resetsAt) {
        const ms = new Date(q.resetsAt).getTime() - Date.now();
        if (ms > 0) {
          const hours = Math.floor(ms / (1000 * 60 * 60));
          const days = Math.floor(hours / 24);
          resetText = days > 0 ? `${days}天${hours % 24}时重置` : `${hours}小时重置`;
        }
      }

      html += `
        <div class="quota-item">
          <div class="quota-top">
            <span class="quota-title">${q.provider.toUpperCase()} <span style="font-weight:400; color:var(--text-muted); font-size:0.75rem;">(${q.account || q.label})</span></span>
            <span class="${pillClass}">剩余 ${percentDisplay}</span>
          </div>
          ${q.remainingPercent != null ? `
            <div class="bar-track">
              <div class="bar-fill" style="width: ${q.remainingPercent}%; background: ${q.remainingPercent < 20 ? 'var(--accent-rose)' : 'var(--accent-emerald)'};"></div>
            </div>
          ` : ''}
          <div class="quota-detail-row">
            <span>${q.label}</span>
            <span>${resetText || (q.remaining ? `余量: ${q.remaining}` : '实时同步')}</span>
          </div>
        </div>
      `;
    });
    quotaGrid.innerHTML = html;
  }

  function renderChart(daily) {
    if (!daily || daily.length === 0) {
      trendSummary.textContent = '暂无历史';
      trendChart.innerHTML = '<div style="color: var(--text-muted); font-size: 0.8rem; text-align: center; width: 100%;">暂无历史记录</div>';
      return;
    }

    const maxTokens = Math.max(...daily.map((d) => d.tokens || 1));
    trendSummary.textContent = '近 ' + daily.length + ' 天峰值: ' + formatTokens(maxTokens);

    let html = '';
    daily.forEach((d) => {
      const heightPercent = Math.max(8, Math.round((d.tokens / maxTokens) * 100));
      const shortDate = d.date.slice(5); // MM-DD
      html += `
        <div class="chart-col" title="${d.date}: ${Number(d.tokens).toLocaleString()} Tokens (¥${d.costCny})">
          <div class="chart-bar-wrap">
            <div class="chart-bar" style="height: ${heightPercent}%;"></div>
          </div>
          <span class="chart-date">${shortDate}</span>
        </div>
      `;
    });
    trendChart.innerHTML = html;
  }

  // Fetch Data
  async function loadData() {
    const pin = getStoredPin();
    try {
      const headers = {};
      if (pin) headers['x-web-pin'] = pin;

      const resp = await fetch('/api/mobile/data', { headers });
      if (resp.status === 401) {
        showPinModal();
        return;
      }

      if (!resp.ok) throw new Error('HTTP ' + resp.status);

      currentData = await resp.json();
      renderAll();
      hidePinModal();
    } catch (err) {
      console.error('[App] Load error:', err);
      statusBadge.className = 'status-badge offline';
      statusText.textContent = '加载失败';
    }
  }

  // SSE Stream
  function setupSSE() {
    if (sseSource) sseSource.close();
    const pin = getStoredPin();
    const sseUrl = '/api/mobile/stream' + (pin ? '?pin=' + encodeURIComponent(pin) : '');

    sseSource = new EventSource(sseUrl);

    sseSource.addEventListener('update', (e) => {
      try {
        currentData = JSON.parse(e.data);
        renderAll();
      } catch (err) {
        console.error('[SSE] Parse error:', err);
      }
    });

    sseSource.onerror = () => {
      sseSource.close();
      // Reconnect after 8s
      setTimeout(setupSSE, 8000);
    };
  }

  // Event Listeners
  document.querySelectorAll('.tab-btn').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      activeTab = btn.dataset.tab;
      renderAll();
    });
  });

  refreshBtn.addEventListener('click', () => {
    refreshBtn.style.transform = 'rotate(180deg)';
    loadData().finally(() => {
      setTimeout(() => { refreshBtn.style.transform = 'none'; }, 300);
    });
  });

  pinSubmitBtn.addEventListener('click', () => {
    const val = pinInput.value.trim();
    if (!val) {
      pinError.textContent = '请输入 PIN 码';
      return;
    }
    setStoredPin(val);
    loadData().then(() => {
      if (currentData) {
        hidePinModal();
        setupSSE();
      } else {
        pinError.textContent = 'PIN 码错误，请重新输入';
      }
    });
  });

  pinInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') pinSubmitBtn.click();
  });

  // Init
  loadData().then(() => {
    setupSSE();
  });
})();
