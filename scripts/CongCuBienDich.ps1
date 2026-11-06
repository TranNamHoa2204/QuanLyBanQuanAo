Set-StrictMode -Version Latest

function Get-CongCuJava {
    param([string]$JdkDirectory)
    if ([string]::IsNullOrWhiteSpace($JdkDirectory)) {
        if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) {
            $JdkDirectory = $env:JAVA_HOME
        } else {
            $javaSource = (Get-Command java -CommandType Application -ErrorAction Stop).Source
            $settings = & $env:ComSpec /d /c ('"' + $javaSource + '" -XshowSettings:properties -version 2>&1') | Out-String
            $match = [regex]::Match($settings, '(?m)^\s*java\.home\s*=\s*(.+?)\r?$')
            if (-not $match.Success) { throw 'Khong xac dinh duoc JDK. Truyen -JdkDirectory.' }
            $JdkDirectory = $match.Groups[1].Value.Trim()
        }
    }
    $JdkDirectory = (Resolve-Path -LiteralPath $JdkDirectory).Path
    foreach ($name in @('java', 'javac', 'jar')) {
        if (-not (Test-Path -LiteralPath (Join-Path $JdkDirectory "bin/$name.exe"))) {
            throw "Thieu $name.exe trong JDK: $JdkDirectory"
        }
    }
    [pscustomobject]@{
        JdkDirectory = $JdkDirectory
        Java = (Join-Path $JdkDirectory 'bin/java.exe')
        Javac = (Join-Path $JdkDirectory 'bin/javac.exe')
        Jar = (Join-Path $JdkDirectory 'bin/jar.exe')
    }
}

function Get-ThuVien {
    param(
        [Parameter(Mandatory = $true)][ValidateSet('MayChu', 'MayKhach', 'KiemThu')][string]$Nhom,
        [string]$RepositoryDirectory
    )
    if ([string]::IsNullOrWhiteSpace($RepositoryDirectory)) {
        $RepositoryDirectory = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
    }
    $registry = Import-PowerShellDataFile -LiteralPath (Join-Path $PSScriptRoot 'ThuVien.psd1')
    foreach ($relative in $registry[$Nhom]) {
        $jarPath = Join-Path $RepositoryDirectory $relative
        if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
            throw "Thieu thu vien $relative. Eclipse: Maven > Update Project tren project Server."
        }
        (Resolve-Path -LiteralPath $jarPath).Path
    }
}

function Copy-TaiNguyenClient {
    param([string]$SourceDirectory, [string]$DestinationDirectory)
    $SourceDirectory = (Resolve-Path -LiteralPath $SourceDirectory).Path
    foreach ($file in Get-ChildItem -LiteralPath $SourceDirectory -Recurse -File) {
        if ($file.Extension -eq '.java') { continue }
        $relative = $file.FullName.Substring($SourceDirectory.Length).TrimStart('\', '/')
        $destination = Join-Path $DestinationDirectory $relative
        New-Item -ItemType Directory -Path (Split-Path $destination -Parent) -Force | Out-Null
        Copy-Item -LiteralPath $file.FullName -Destination $destination -Force
    }
}

function Publish-KetQuaBienDich {
    param([string]$FreshDirectory, [string]$DestinationDirectory, [string]$ArchiveDirectory)
    $workspace = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..')).TrimEnd('\') + '\'
    $destination = [IO.Path]::GetFullPath($DestinationDirectory)
    $archive = [IO.Path]::GetFullPath($ArchiveDirectory)
    $tempPrefix = [IO.Path]::GetFullPath([IO.Path]::GetTempPath()).TrimEnd('\') + '\'
    if (-not $destination.StartsWith($workspace, [StringComparison]::OrdinalIgnoreCase) -or
        -not $archive.StartsWith($tempPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Duong dan build/archive nam ngoai pham vi cho phep.'
    }
    if (Test-Path -LiteralPath $destination) {
        if (Test-Path -LiteralPath $archive) { throw 'Thu muc archive da ton tai.' }
        New-Item -ItemType Directory -Path (Split-Path $archive -Parent) -Force | Out-Null
        Move-Item -LiteralPath $destination -Destination $archive
    }
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
    foreach ($entry in Get-ChildItem -LiteralPath $FreshDirectory -Force) {
        Copy-Item -LiteralPath $entry.FullName -Destination $destination -Recurse -Force
    }
}
