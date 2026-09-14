@echo off
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -Command "Start-Process powershell -Verb RunAs -ArgumentList '-NoProfile -Command netsh advfirewall firewall add rule name=\"\"Token Monitor Hub\"\" dir=in action=allow protocol=TCP localport=17321; Write-Host \"\"=================================================\"\"; Write-Host \"\"[OK] Port 17321 successfully opened in Windows Firewall!\"\"; Write-Host \"\"[OK] 端口 17321 已成功在 Windows 防火墙中放行！\"\"; Write-Host \"\"=================================================\"\"; pause'"
