# UdjatTrack Quick Start Script

$MVN_BIN = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.1\plugins\maven\lib\maven3\bin\mvn.cmd"

if (!(Test-Path $MVN_BIN)) {
    Write-Error "Maven binary not found at $MVN_BIN. Please update the path in this script."
    exit
}

Write-Host "Starting UdjatTrack API..." -ForegroundColor Cyan
& $MVN_BIN spring-boot:run
