# Kill any existing Node processes that might be holding Vitest
Get-Process | Where-Object {$_.Name -like "*node*"} | Stop-Process -Force -ErrorAction SilentlyContinue

# Run tests and save output
$ErrorActionPreference = 'Continue'
npx vitest run --reporter=verbose 2>&1 | Out-File -FilePath "test-results.txt" -Encoding UTF8

Write-Host "Tests completed. Results saved to test-results.txt"
