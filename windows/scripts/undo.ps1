# TwinDesk 简体中文语言包 —— 一键还原（需管理员权限）
# 把注入过补丁的 HTML / main.js 恢复为备份的原始文件，并删除 zh-cn 目录。
param(
  [string]$AppDir = 'C:\Program Files\TwinDesk\resources\app',
  [string]$BackupDir = 'F:\deep\_twindesk_i18n\backup',
  [string]$LogFile = 'F:\deep\_twindesk_i18n\apply_log.txt'
)
$ErrorActionPreference = 'Stop'
function Log($m) {
  $line = ('[{0}] {1}' -f (Get-Date -Format 'HH:mm:ss'), $m)
  Write-Output $line
  Add-Content -Path $LogFile -Value $line -Encoding UTF8
}
try {
  Get-Process TwinDesk -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
  Start-Sleep -Seconds 2
  if (-not (Test-Path $BackupDir)) { throw "no backup dir: $BackupDir" }
  $n = 0
  Get-ChildItem -Path $BackupDir -Recurse -File | ForEach-Object {
    $rel = $_.FullName.Substring($BackupDir.Length).TrimStart('\')
    $dst = Join-Path $AppDir $rel
    New-Item -ItemType Directory -Force -Path (Split-Path $dst) | Out-Null
    Copy-Item -LiteralPath $_.FullName -Destination $dst -Force
    $n++
  }
  Log ("restored " + $n + " original files")
  $zh = Join-Path $AppDir 'zh-cn'
  if (Test-Path $zh) { Remove-Item $zh -Recurse -Force; Log "zh-cn dir removed" }
  Remove-Item Env:ELECTRON_RUN_AS_NODE -ErrorAction SilentlyContinue
  Start-Process -FilePath 'C:\Program Files\TwinDesk\TwinDesk.exe'
  Log "app restarted (English)"
  Log "DONE"
} catch {
  Log ("ERROR: " + $_.Exception.Message)
  exit 1
}