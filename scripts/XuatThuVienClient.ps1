[CmdletBinding()]
param([string]$JdkDirectory, [string]$RepositoryDirectory, [switch]$PassThru)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'CongCuBienDich.ps1')
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$server = Join-Path $workspace 'QuanLyBanQuanAo_Server'
$client = Join-Path $workspace 'QuanLyBanQuanAo_Client'
$tools = Get-CongCuJava -JdkDirectory $JdkDirectory
$serverJars = @(Get-ThuVien -Nhom MayChu -RepositoryDirectory $RepositoryDirectory)
$clientJars = @(Get-ThuVien -Nhom MayKhach -RepositoryDirectory $RepositoryDirectory)
$build = Join-Path ([IO.Path]::GetTempPath()) ('QuanAo-HaiProject-' + [guid]::NewGuid().ToString('N'))
$serverClasses = Join-Path $build 'server-classes'
$clientClasses = Join-Path $build 'client-bin'
New-Item -ItemType Directory -Path $serverClasses,$clientClasses -Force | Out-Null
$serverSources = @(Get-ChildItem -LiteralPath (Join-Path $server 'src/main/java') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
Write-Host 'Bien dich Maven Server (server + common).'
& $tools.Javac --release 8 -encoding UTF-8 -cp ($serverJars -join ';') -d $serverClasses @serverSources
if ($LASTEXITCODE -ne 0) { throw 'Bien dich Server that bai.' }
foreach ($entry in Get-ChildItem -LiteralPath (Join-Path $server 'src/main/resources')) {
    Copy-Item -LiteralPath $entry.FullName -Destination $serverClasses -Recurse -Force
}
$jarFile = Join-Path $build 'server.jar'
& $tools.Jar --create --file $jarFile -C $serverClasses .
if ($LASTEXITCODE -ne 0) { throw 'Xuat server.jar that bai.' }
$serverTarget = Join-Path $server 'target'
$clientLib = Join-Path $client 'lib'
New-Item -ItemType Directory -Path $serverTarget,$clientLib -Force | Out-Null
Copy-Item -LiteralPath $jarFile -Destination (Join-Path $serverTarget 'server.jar') -Force
Copy-Item -LiteralPath $jarFile -Destination (Join-Path $clientLib 'server.jar') -Force
foreach ($jar in $clientJars) { Copy-Item -LiteralPath $jar -Destination $clientLib -Force }
Publish-KetQuaBienDich -FreshDirectory $serverClasses -DestinationDirectory (Join-Path $serverTarget 'classes') -ArchiveDirectory (Join-Path $build 'server-classes-cu')

# Client chi duoc dung src cua no va cac JAR trong lib; khong them source/target Server.
$clientClasspath = @((Join-Path $clientLib 'server.jar')) + @($clientJars | ForEach-Object { Join-Path $clientLib ([IO.Path]::GetFileName($_)) })
$clientSources = @(Get-ChildItem -LiteralPath (Join-Path $client 'src/client') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
Write-Host 'Bien dich Java Client rieng bang lib/server.jar va cac JAR trong lib.'
& $tools.Javac --release 8 -encoding UTF-8 -cp ($clientClasspath -join ';') -sourcepath (Join-Path $client 'src') -d $clientClasses @clientSources
if ($LASTEXITCODE -ne 0) { throw 'Client khong bien dich duoc bang cac JAR trong lib.' }
Copy-TaiNguyenClient -SourceDirectory (Join-Path $client 'src') -DestinationDirectory $clientClasses
Publish-KetQuaBienDich -FreshDirectory $clientClasses -DestinationDirectory (Join-Path $client 'bin') -ArchiveDirectory (Join-Path $build 'client-bin-cu')
Write-Host 'Da xuat QuanLyBanQuanAo_Client/lib/server.jar va cap nhat bin cho Client.'
if ($PassThru) { [pscustomobject]@{
    Java = $tools.Java
    Javac = $tools.Javac
    MayChuClasspath = ((@((Join-Path $serverTarget 'server.jar')) + $serverJars) -join ';')
    MayKhachClasspath = ((@((Join-Path $client 'bin')) + $clientClasspath) -join ';')
    ServerJar = (Join-Path $clientLib 'server.jar')
    BuildDirectory = $build
} }
