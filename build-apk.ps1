$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "[*] Building TokenMonitor Jetpack Compose Release APK..." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

$root = $PSScriptRoot
if (-not $root) { $root = (Get-Location).Path }
$outApk = Join-Path $root "TokenMonitor.apk"
$gradleReleaseApk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"

Push-Location $root

Write-Host "[1/3] Running Gradle assembleRelease..." -ForegroundColor Yellow
& ".\gradlew.bat" assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Gradle assembleRelease failed with exit code $LASTEXITCODE"
}

if (-not (Test-Path $gradleReleaseApk)) {
    Write-Error "Release APK was not generated. Please inspect build logs."
}

Write-Host "[2/3] Copying Release APK to root directory..." -ForegroundColor Yellow
Copy-Item $gradleReleaseApk -Destination $outApk -Force

Write-Host "[3/3] Verifying APK signature..." -ForegroundColor Yellow
$apksignerCandidates = @(
    (Get-Command apksigner.bat -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source),
    "$env:ANDROID_HOME\build-tools\*\apksigner.bat",
    "$env:LOCALAPPDATA\Android\Sdk\build-tools\*\apksigner.bat"
)
$apksigner = $null
foreach ($cand in $apksignerCandidates) {
    if ($cand) {
        $resolved = Resolve-Path $cand -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved -and (Test-Path $resolved.Path)) {
            $apksigner = $resolved.Path
            break
        }
    }
}
if ($apksigner) {
    & $apksigner verify -v $outApk
} else {
    Write-Host "[Info] apksigner not found in standard paths, skipping standalone verification." -ForegroundColor Gray
}

Pop-Location

$sizeMb = [Math]::Round((Get-Item $outApk).Length / 1MB, 2)
Write-Host "==========================================" -ForegroundColor Green
Write-Host "[OK] Successfully built release APK: TokenMonitor.apk" -ForegroundColor Green
Write-Host "Path: $outApk" -ForegroundColor Green
Write-Host "Size: $sizeMb MB" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
