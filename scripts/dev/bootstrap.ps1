param(
  [ValidateSet("db", "full", "down", "reset-db")]
  [string]$Mode = "db"
)

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $RootDir

if (-not (Test-Path ".env")) {
  Copy-Item ".env.example" ".env"
  Write-Host "[bootstrap] .env file was created from .env.example"
}

switch ($Mode) {
  "db" {
    Write-Host "[bootstrap] Starting local postgres only..."
    docker compose -f compose.yml up -d postgres
  }
  "full" {
    Write-Host "[bootstrap] Starting postgres + paybridge container..."
    docker compose -f compose.yml --profile full up --build -d postgres paybridge
  }
  "down" {
    Write-Host "[bootstrap] Stopping compose stack..."
    docker compose -f compose.yml down
  }
  "reset-db" {
    Write-Host "[bootstrap] Resetting postgres volume..."
    docker compose -f compose.yml down -v
    docker compose -f compose.yml up -d postgres
  }
}
