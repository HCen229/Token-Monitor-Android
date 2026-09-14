$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "🚀 开始编译构建 TokenMonitor Jetpack Compose 原生应用..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$root = "D:\Work\Token-Monitor-Android"
$outApk = "$root\TokenMonitor.apk"
$gradleReleaseApk = "$root\app\build\outputs\apk\release\app-release.apk"

Push-Location $root

Write-Host "[1/3] 执行 Gradle assembleRelease 编译..." -ForegroundColor Yellow
& ".\gradlew.bat" assembleRelease --offline
if ($LASTEXITCODE -ne 0) {
    throw "Gradle assembleRelease failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $gradleReleaseApk)) {
    Write-Error "Release APK 未生成，请检查编译日志！"
}

Write-Host "[2/3] 复制 Release APK 到发布目录..." -ForegroundColor Yellow
Copy-Item $gradleReleaseApk -Destination $outApk -Force

Write-Host "[3/3] 校验 APK 签名与完整性..." -ForegroundColor Yellow
& "D:\Android\Sdk\build-tools\36.1.0\apksigner.bat" verify -v $outApk

Pop-Location

Write-Host "==========================================" -ForegroundColor Green
Write-Host "✅ 成功构建完整版原生应用：TokenMonitor.apk" -ForegroundColor Green
Write-Host "文件位置：$outApk" -ForegroundColor Green
Write-Host "文件大小：$([Math]::Round((Get-Item $outApk).Length / 1MB, 2)) MB" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
