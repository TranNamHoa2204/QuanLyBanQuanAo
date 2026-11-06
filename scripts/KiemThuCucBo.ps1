param([string]$JdkDirectory, [string]$JavaDirectory, [string]$RepositoryDirectory,
    [string[]]$TestClasses = @())

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'CongCuBienDich.ps1')
$workspace = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$build = & (Join-Path $PSScriptRoot 'XuatThuVienClient.ps1') -JdkDirectory $JdkDirectory -RepositoryDirectory $RepositoryDirectory -PassThru
$testJars = @(Get-ThuVien -Nhom KiemThu -RepositoryDirectory $RepositoryDirectory)
$testRoot = Join-Path $workspace 'QuanLyBanQuanAo_Server/src/test/java'
$testFiles = @(Get-ChildItem -LiteralPath $testRoot -Recurse -Filter '*.java')
if ($TestClasses.Count -eq 0) {
    $TestClasses = @(foreach ($file in $testFiles) {
        $text = [IO.File]::ReadAllText($file.FullName)
        if ($text -match '(?m)^\s*@(?:org\.junit\.)?Test\b') {
            $package = [regex]::Match($text, '(?m)^package\s+([\w.]+);').Groups[1].Value
            $package + '.' + $file.BaseName
        }
    })
}
$testOutput = Join-Path $build.BuildDirectory 'test-classes'
New-Item -ItemType Directory -Path $testOutput -Force | Out-Null
$compileCp = $build.MayChuClasspath + ';' + $build.MayKhachClasspath + ';' + ($testJars -join ';')
& $build.Javac --release 8 -encoding UTF-8 -cp $compileCp -sourcepath $testRoot -d $testOutput @($testFiles | Select-Object -ExpandProperty FullName)
if ($LASTEXITCODE -ne 0) { throw 'Bien dich kiem thu that bai.' }
$runtimeCp = $testOutput + ';' + $compileCp
$java = $build.Java
if (-not [string]::IsNullOrWhiteSpace($JavaDirectory)) { $java = Join-Path $JavaDirectory 'bin/java.exe' }
Push-Location $workspace
try {
    & $java '-Djava.awt.headless=true' -cp $runtimeCp org.junit.runner.JUnitCore @TestClasses
    if ($LASTEXITCODE -ne 0) { throw 'JUnit that bai.' }
    & $java '-Djava.awt.headless=true' -cp $runtimeCp kiemthu.KiemTraNenTang
    if ($LASTEXITCODE -ne 0) { throw 'Kiem tra nen tang that bai.' }
    & $java '-Djava.awt.headless=true' -cp $runtimeCp kiemthu.KiemTraTaiNguyenAnh
    if ($LASTEXITCODE -ne 0) { throw 'Kiem tra anh that bai.' }
} finally { Pop-Location }
