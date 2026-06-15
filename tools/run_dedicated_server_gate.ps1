param(
    [int]$TimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $PSScriptRoot
$ProjectRoot = Join-Path $Root '1.20.1'
$RunRoot = Join-Path $ProjectRoot 'run'
$AuditRoot = Join-Path $Root 'audit'
$OutLog = Join-Path $RunRoot 'dedicated-server-gate.out.log'
$ErrLog = Join-Path $RunRoot 'dedicated-server-gate.err.log'
$SummaryPath = Join-Path $AuditRoot 'dedicated_server_startup_summary.json'

New-Item -ItemType Directory -Force -Path $RunRoot | Out-Null
New-Item -ItemType Directory -Force -Path $AuditRoot | Out-Null
Set-Content -LiteralPath (Join-Path $RunRoot 'eula.txt') -Value "# Created for automated dedicated-server startup validation.`neula=true`n" -Encoding UTF8
Remove-Item -LiteralPath $OutLog, $ErrLog -ErrorAction SilentlyContinue

$Started = Get-Date
$Process = Start-Process `
    -FilePath 'cmd.exe' `
    -ArgumentList '/c', 'gradlew.bat --no-daemon -D "net.minecraftforge.gradle.check.certs=false" runServer' `
    -WorkingDirectory $ProjectRoot `
    -RedirectStandardOutput $OutLog `
    -RedirectStandardError $ErrLog `
    -WindowStyle Hidden `
    -PassThru

$Ready = $false
$ExitedBeforeReady = $false
$Deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $Deadline) {
    Start-Sleep -Seconds 2
    $Text = ''
    if (Test-Path $OutLog) {
        $Text += Get-Content -LiteralPath $OutLog -Raw -ErrorAction SilentlyContinue
    }
    if (Test-Path $ErrLog) {
        $Text += "`n" + (Get-Content -LiteralPath $ErrLog -Raw -ErrorAction SilentlyContinue)
    }
    if ($Text -match 'Done \(' -or $Text -match 'For help, type') {
        $Ready = $true
        break
    }
    if ($Process.HasExited) {
        $ExitedBeforeReady = $true
        break
    }
}

$Targets = Get-CimInstance Win32_Process | Where-Object {
    $_.CreationDate -gt $Started -and (
        $_.CommandLine -like '*runServer*' -or
        $_.CommandLine -like '*forgeserveruserdev*' -or
        $_.CommandLine -like '*narutomod-1.20.1-port*' -or
        $_.CommandLine -like "*$ProjectRoot*"
    )
}
foreach ($Target in $Targets) {
    try {
        Stop-Process -Id $Target.ProcessId -Force -ErrorAction SilentlyContinue
    } catch {
    }
}
Start-Sleep -Seconds 2

$Combined = ''
if (Test-Path $OutLog) {
    $Combined += Get-Content -LiteralPath $OutLog -Raw -ErrorAction SilentlyContinue
}
if (Test-Path $ErrLog) {
    $Combined += "`n" + (Get-Content -LiteralPath $ErrLog -Raw -ErrorAction SilentlyContinue)
}

$FatalPatterns = @(
    'NoClassDefFoundError',
    'ClassNotFoundException',
    'ModLoadingException',
    'Failed to start',
    'Error during pre-loading phase',
    'Encountered an unexpected exception',
    'Crash report saved',
    'Exception in server tick loop'
)
$FatalMatches = @()
foreach ($Pattern in $FatalPatterns) {
    if ($Combined -match $Pattern) {
        $FatalMatches += $Pattern
    }
}

$Summary = [ordered]@{
    ready = $Ready
    exitedBeforeReady = $ExitedBeforeReady
    fatalIssueCount = $FatalMatches.Count
    fatalPatterns = $FatalMatches
    stdout = '1.20.1/run/dedicated-server-gate.out.log'
    stderr = '1.20.1/run/dedicated-server-gate.err.log'
    doneLine = (($Combined -split "`r?`n") | Where-Object { $_ -match 'Done \(' } | Select-Object -First 1)
    checkedAt = (Get-Date).ToString('s')
}

$Summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $SummaryPath -Encoding UTF8
$Summary | ConvertTo-Json -Depth 4

if (-not $Ready -or $FatalMatches.Count -gt 0) {
    exit 1
}
