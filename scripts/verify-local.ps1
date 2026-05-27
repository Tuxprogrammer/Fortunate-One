#Requires -Version 5.1
<#
.SYNOPSIS
    Local verification script that mirrors the CI checks run on every PR.

.DESCRIPTION
    Runs two verification layers in order:

    1. Mixin package purity check (equivalent to check-mixin-purity.yml)
       Greps every .java file in src/main/java/**/mixins/ for an @Mixin annotation.
       Any file without one would cause SpongeMixin to throw IllegalClassLoadError
       at runtime the first time worldgen fires. This catches that regression in
       under a second without needing a running Minecraft server.

    2. Minecraft server integration test (equivalent to build-and-test.yml runServer step)
       Starts the deobfuscated Forge server via Gradle. The FortunateOneTestMod @Mod fires
       on FMLServerStartedEvent and runs the JUnit 5 tests inside the live environment.
       The script monitors stdout for "Done" (server fully started) then sends "stop",
       waits for the process to exit, and checks for crash reports.

.PARAMETER SkipBuild
    Skip the Gradle build step (use the jar already in build/libs/).

.PARAMETER ServerTimeoutSeconds
    How long to wait for the server to reach "Done" before giving up. Default: 180.

.EXAMPLE
    .\scripts\verify-local.ps1

.EXAMPLE
    .\scripts\verify-local.ps1 -SkipBuild -ServerTimeoutSeconds 300
#>
param(
    [switch]$SkipBuild,
    [int]$ServerTimeoutSeconds = 180
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $RepoRoot

function Write-Step([string]$msg) { Write-Host "`n=== $msg ===" -ForegroundColor Cyan }
function Write-Pass([string]$msg) { Write-Host "PASS: $msg" -ForegroundColor Green }
function Write-Fail([string]$msg) { Write-Host "FAIL: $msg" -ForegroundColor Red }

$overallPass = $true

# ---------------------------------------------------------------------------
# Step 1: Build
# ---------------------------------------------------------------------------
if (-not $SkipBuild) {
    Write-Step "Building mod jar"
    & ".\gradlew.bat" "build" "--console=plain"
    if ($LASTEXITCODE -ne 0) {
        Write-Fail "Gradle build failed (exit $LASTEXITCODE). Fix build errors before running tests."
        exit 1
    }
    Write-Pass "Build succeeded"
}

# ---------------------------------------------------------------------------
# Step 2: Mixin package purity check  (mirrors check-mixin-purity.yml)
# ---------------------------------------------------------------------------
Write-Step "Mixin package purity check"

$mixinFiles = Get-ChildItem -Path "src\main\java" -Recurse -Filter "*.java" |
    Where-Object { $_.DirectoryName -match '\\mixins$' -or $_.DirectoryName -match '\\mixins\\' }

$purityPass = $true
foreach ($f in $mixinFiles) {
    $content = Get-Content $f.FullName -Raw
    if ($content -notmatch '@Mixin') {
        Write-Fail "$($f.FullName)"
        Write-Host "       ^ is in the mixin package but has no @Mixin annotation." -ForegroundColor Red
        Write-Host "         Non-mixin classes here cause SpongeMixin to throw IllegalClassLoadError" -ForegroundColor Red
        Write-Host "         at runtime. Move it to src\main\java\io\github\tuxprogrammer\fortunateone\" -ForegroundColor Red
        $purityPass = $false
        $overallPass = $false
    }
}

if ($purityPass) {
    Write-Pass "All $($mixinFiles.Count) file(s) in the mixin package have @Mixin annotations"
}

# ---------------------------------------------------------------------------
# Step 3: Minecraft server integration test  (mirrors build-and-test.yml runServer step)
# ---------------------------------------------------------------------------
Write-Step "Minecraft server integration test (timeout: ${ServerTimeoutSeconds}s)"

# Clean up any leftover crash reports so we detect only new ones.
$crashDir = Join-Path $RepoRoot "run\crash-reports"
if (Test-Path $crashDir) {
    Remove-Item "$crashDir\crash-*.txt" -ErrorAction SilentlyContinue
}

$serverLog = Join-Path $RepoRoot "run\server-test.log"
$null = New-Item -ItemType Directory -Force -Path (Join-Path $RepoRoot "run")
"" | Set-Content $serverLog -Encoding UTF8

# Write a temp batch file that:
#   1. Pipes "stop" into gradlew runServer so the server shuts down after FMLServerStartedEvent
#   2. Redirects all output (stdout + stderr) to the log file
#   3. Uses --no-daemon so stdin is forwarded from the batch pipe into the Minecraft server JVM
# The server only processes "stop" AFTER its console thread starts, which is after
# FMLServerStartedEvent fires and our tests run.
$tmpBat = [System.IO.Path]::ChangeExtension([System.IO.Path]::GetTempFileName(), ".bat")
[System.IO.File]::WriteAllText($tmpBat,
    "@echo off`r`necho stop | .\gradlew.bat runServer --no-daemon --console=plain > `"$serverLog`" 2>&1`r`n",
    [System.Text.Encoding]::ASCII)

Write-Host "Starting server (--no-daemon, output -> $(Split-Path $serverLog -Leaf))..." -ForegroundColor Yellow
Write-Host "This takes 2-4 minutes. Polling every 5s for 'Done'..." -ForegroundColor Gray

$proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c `"$tmpBat`"" `
    -WorkingDirectory $RepoRoot -PassThru -NoNewWindow

$serverPass = $true
$startTime = [DateTime]::UtcNow
$reached = $false
$lastLineCount = 0

while (([DateTime]::UtcNow - $startTime).TotalSeconds -lt $ServerTimeoutSeconds) {
    Start-Sleep -Seconds 5
    if (Test-Path $serverLog) {
        $content = Get-Content $serverLog -Raw -ErrorAction SilentlyContinue
        if ($content) {
            $lineCount = ($content -split "`n").Count
            if ($lineCount -ne $lastLineCount) {
                $elapsed = [int](([DateTime]::UtcNow - $startTime).TotalSeconds)
                Write-Host "  [${elapsed}s] $lineCount lines..." -ForegroundColor Gray
                $lastLineCount = $lineCount
            }
            if ($content -match 'Done \(') {
                $reached = $true
                Write-Host "Server reached 'Done' - waiting for orderly shutdown..." -ForegroundColor Yellow
                break
            }
        }
    }
    if ($proc.HasExited) {
        break  # Server exited early (crash or error)
    }
}

# Wait up to 90s for the server to finish shutting down (processes "stop" then exits)
if (-not $proc.HasExited) {
    $proc.WaitForExit(90000) | Out-Null
}
if (-not $proc.HasExited) {
    Write-Host "Server still running after shutdown wait - force killing" -ForegroundColor Yellow
    $proc.Kill()
    $proc.WaitForExit(10000) | Out-Null
}

Remove-Item $tmpBat -ErrorAction SilentlyContinue

if (-not $reached) {
    Write-Fail "Server did not reach 'Done (X)!' within ${ServerTimeoutSeconds}s - possible crash or hang"
    Write-Host "  Check $serverLog for details" -ForegroundColor Red
    $serverPass = $false
    $overallPass = $false
}

# Check for crash reports
$crashFiles = @()
if (Test-Path $crashDir) {
    $crashFiles = @(Get-ChildItem "$crashDir\crash-*.txt" -ErrorAction SilentlyContinue)
}

if ($crashFiles.Count -gt 0) {
    Write-Fail "Server generated $($crashFiles.Count) crash report(s):"
    foreach ($cr in $crashFiles) {
        Write-Host "  $($cr.FullName)" -ForegroundColor Red
        Get-Content $cr.FullName | Select-Object -First 30 | ForEach-Object { Write-Host "    $_" -ForegroundColor Red }
    }
    $serverPass = $false
    $overallPass = $false
}

# Check for test failure message from FortunateOneTestMod
$logContent = Get-Content $serverLog -Raw -ErrorAction SilentlyContinue
if ($logContent -match 'Fortunate One integration tests failed') {
    Write-Fail "Integration tests reported failures - check $serverLog for details"
    $serverPass = $false
    $overallPass = $false
}

if ($serverPass) {
    Write-Pass "Server started, ran integration tests, and stopped cleanly"
}

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
Write-Step "Summary"
if ($overallPass) {
    Write-Pass "All checks passed"
    Pop-Location
    exit 0
} else {
    Write-Fail "One or more checks failed (see above)"
    Pop-Location
    exit 1
}
