# TwinDesk 简体中文语言包 —— 应用/更新补丁（需管理员权限）
# 用法:
#   powershell -ExecutionPolicy Bypass -File apply.ps1              # 备份(首次) -> 还原干净副本 -> 打补丁 -> 重启应用
#   powershell -ExecutionPolicy Bypass -File apply.ps1 -NoLaunch    # 只打补丁不启动
param(
  [string]$AppDir = 'C:\Program Files\TwinDesk\resources\app',
  [string]$PatchDir = 'F:\deep\_twindesk_i18n\patch',
  [string]$BackupDir = 'F:\deep\_twindesk_i18n\backup',
  [string]$LogFile = 'F:\deep\_twindesk_i18n\apply_log.txt',
  [switch]$NoLaunch
)

$ErrorActionPreference = 'Stop'
function Log($m) {
  $line = ('[{0}] {1}' -f (Get-Date -Format 'HH:mm:ss'), $m)
  Write-Output $line
  Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

try {
  New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
  if (-not (Test-Path $AppDir)) { throw "app dir not found: $AppDir" }

  $htmlFiles = Get-ChildItem -Path $AppDir -Recurse -Filter *.html -File -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch '\\node_modules\\' -and $_.FullName -notmatch '\\zh-cn\\' }
  $mainFiles = @(
    (Join-Path $AppDir 'windows\main.js'),
    (Join-Path $AppDir 'windows\mesh\main.js')
  ) | Where-Object { Test-Path $_ }

  $targets = @($htmlFiles | ForEach-Object { $_.FullName }) + $mainFiles
  Log ("targets: " + $targets.Count + " files (html=" + $htmlFiles.Count + ", main=" + $mainFiles.Count + ")")

  # 1) 首次备份原始文件（后续每次先从这里还原，保证幂等）
  $needBackup = @()
  foreach ($t in $targets) {
    $rel = $t.Substring($AppDir.Length).TrimStart('\')
    $b = Join-Path $BackupDir $rel
    if (-not (Test-Path $b)) { $needBackup += $t }
  }
  if ($needBackup.Count) {
    Write-Output ("backing up " + $needBackup.Count + " pristine files")
    foreach ($t in $needBackup) {
      $rel = $t.Substring($AppDir.Length).TrimStart('\')
      $b = Join-Path $BackupDir $rel
      New-Item -ItemType Directory -Force -Path (Split-Path $b) | Out-Null
      Copy-Item -LiteralPath $t -Destination $b -Force
    }
  } else { Write-Output "backup already complete" }

  # 2) 还原为原始版本
  foreach ($t in $targets) {
    $rel = $t.Substring($AppDir.Length).TrimStart('\')
    $b = Join-Path $BackupDir $rel
    if (Test-Path $b) { Copy-Item -LiteralPath $b -Destination $t -Force }
  }
  Log "originals restored"

  # 3) 安装语言包文件
  $dst = Join-Path $AppDir 'zh-cn'
  if (Test-Path $dst) { Remove-Item $dst -Recurse -Force }
  New-Item -ItemType Directory -Force -Path $dst | Out-Null
  Copy-Item -Path (Join-Path $PatchDir 'zh-cn\*') -Destination $dst -Recurse -Force
  Log ("zh-cn files installed: " + ((Get-ChildItem $dst -Recurse -File | Measure-Object).Count))

  # 4) 注入渲染进程脚本
  $injected = 0
  foreach ($h in $htmlFiles) {
    $relDir = [IO.Path]::GetRelativePath($AppDir, $h.DirectoryName)
    $depth = if ($relDir -eq '.') { 0 } else { ($relDir -split '\\').Count }
    $prefix = ('../' * $depth) + 'zh-cn/'
    $snippet = '<script src="' + $prefix + 'zh-dict.js"></script><script src="' + $prefix + 'zh-engine.js"></script>'
    $text = Get-Content -LiteralPath $h.FullName -Raw -Encoding UTF8
    if ($text -match 'zh-engine\.js') { continue }
    if ($text -match '</body>') {
      $text = [regex]::Replace($text, '</body>', [System.Text.RegularExpressions.MatchEvaluator] { param($m) $snippet + "`r`n</body>" }, 1)
    } else {
      $text = $text + "`r`n" + $snippet
    }
    Set-Content -LiteralPath $h.FullName -Value $text -Encoding UTF8 -NoNewline
    $injected++
  }
  Log ("html injected: " + $injected)

  # 5) 主进程挂钩
  foreach ($m in $mainFiles) {
    $text = Get-Content -LiteralPath $m -Raw -Encoding UTF8
    if ($text -match 'zh-cn/zh-main\.js') { continue }
    $rel = [IO.Path]::GetRelativePath([IO.Path]::GetDirectoryName($m), (Join-Path $AppDir 'zh-cn\zh-main.js')) -replace '\\', '/'
    $text = 'require("' + $rel + '"); // TwinDesk zh-CN 语言包（非官方补丁）' + "`r`n" + $text
    Set-Content -LiteralPath $m -Value $text -Encoding UTF8 -NoNewline
    Log ("main hooked: " + $m)
  }

  # 6) 校验
  $ok = (Test-Path (Join-Path $dst 'zh-dict.js')) -and (Test-Path (Join-Path $dst 'zh-engine.js')) -and (Test-Path (Join-Path $dst 'zh-dict-main.json'))
  $mainOk = (Get-Content -LiteralPath (Join-Path $AppDir 'windows\main.js') -Raw) -match 'zh-cn/zh-main\.js'
  $htmlOk = (Get-Content -LiteralPath (Join-Path $AppDir 'windows\app.html') -Raw) -match 'zh-engine\.js'
  Log ("verify: files=$ok mainHook=$mainOk htmlHook=$htmlOk")

  # 7) 重启应用
  if (-not $NoLaunch) {
    Get-Process TwinDesk -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Remove-Item Env:ELECTRON_RUN_AS_NODE -ErrorAction SilentlyContinue
    Start-Process -FilePath 'C:\Program Files\TwinDesk\TwinDesk.exe'
    Log "app restarted"
  }
  Log "DONE"
} catch {
  Log ("ERROR: " + $_.Exception.Message)
  Log ($_.ScriptStackTrace)
  exit 1
}