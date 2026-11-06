[CmdletBinding()]
param([string]$JdkDirectory, [string]$RepositoryDirectory, [switch]$ConfigureDatabase,
    [switch]$CreateInitialAccount, [switch]$Check)

$ErrorActionPreference = 'Stop'
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$build = & (Join-Path $PSScriptRoot 'XuatThuVienClient.ps1') -JdkDirectory $JdkDirectory -RepositoryDirectory $RepositoryDirectory -PassThru
if ($Check) { Write-Host 'Da bien dich rieng Server/Client va xuat lib/server.jar. Chua ket noi database.'; exit 0 }
$configFile = Join-Path $workspace 'application.local.properties'
if (-not (Test-Path -LiteralPath $configFile)) { throw 'Thieu application.local.properties tai thu muc workspace.' }
$network = @{ 'server.host' = 'localhost'; 'server.port' = '8888' }
foreach ($line in Get-Content -LiteralPath $configFile) {
    if ($line -match '^\s*(server\.host|server\.port)\s*=\s*(.*?)\s*$') { $network[$Matches[1]] = $Matches[2] }
}
$serverHost = [string]$network['server.host']
$serverPort = 0
if (-not [int]::TryParse([string]$network['server.port'], [ref]$serverPort) -or $serverPort -lt 1 -or $serverPort -gt 65535 -or [string]::IsNullOrWhiteSpace($serverHost)) {
    throw 'Kiem tra server.host va server.port trong application.local.properties.'
}
function Test-MayChu {
    $tcp = New-Object Net.Sockets.TcpClient
    try {
        $pending = $tcp.BeginConnect($serverHost, $serverPort, $null, $null)
        if (-not $pending.AsyncWaitHandle.WaitOne(300)) { return $false }
        $tcp.EndConnect($pending)
        return $tcp.Connected
    } catch { return $false } finally { $tcp.Dispose() }
}
if (Test-MayChu) { throw 'Server da chay. Chay ClientMain bang Run As > Java Application trong Eclipse de ket noi.' }
if ($CreateInitialAccount) {
    $answer = Read-Host 'Chi dung khi database chua co tai khoan. Nhap CREATE-MANAGER de tao quan ly'
    if ($answer -cne 'CREATE-MANAGER') { throw 'Da huy tao tai khoan.' }
    & $build.Java ('-Dquanao.config=' + $configFile) -cp $build.MayChuClasspath server.app.TaoTaiKhoanQuanLy
    if ($LASTEXITCODE -ne 0) { throw 'Tao tai khoan that bai; khong tu dong thu lai.' }
}
$logDirectory = Join-Path $workspace '.tmp'
New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
$serverProcess = $null
try {
    $serverArgs = '-Dfile.encoding=UTF-8 -Dquanao.config="' + $configFile + '" -cp "' + $build.MayChuClasspath + '" server.app.ServerMain'
    $serverErrors = Join-Path $logDirectory 'chay-server.stderr.log'
    $serverProcess = Start-Process -FilePath $build.Java -ArgumentList $serverArgs -WorkingDirectory (Join-Path $workspace 'QuanLyBanQuanAo_Server') -PassThru -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDirectory 'chay-server.stdout.log') -RedirectStandardError $serverErrors
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        $serverProcess.Refresh()
        if ($serverProcess.HasExited) { throw ('Server dung khi khoi tao: ' + (Get-Content -LiteralPath $serverErrors -Tail 20 | Out-String)) }
        if (Test-MayChu) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $ready) { throw 'Server chua mo cong sau 30 giay. Kiem tra application.local.properties va database.' }
    Write-Host 'Server san sang. Dang mo Client Java Project.'
    & $build.Java '-Dfile.encoding=UTF-8' ('-Dquanao.config=' + $configFile) -cp $build.MayKhachClasspath client.app.ClientMain
    if ($LASTEXITCODE -ne 0) { throw 'Client ket thuc voi loi.' }
} finally {
    if ($serverProcess) {
        $serverProcess.Refresh()
        if (-not $serverProcess.HasExited) { Stop-Process -Id $serverProcess.Id -Force }
        $serverProcess.Dispose()
    }
}
