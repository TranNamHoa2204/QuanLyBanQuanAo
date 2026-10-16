param(
    [Parameter(Mandatory = $true)][string]$JdkDirectory,
    [string]$JavaDirectory = $JdkDirectory,
    [string]$RepositoryDirectory,
    [string[]]$TestClasses = @()
)

$ErrorActionPreference = 'Stop'
$projectDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($RepositoryDirectory)) {
    $RepositoryDirectory = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
}
if (-not (Test-Path -LiteralPath $RepositoryDirectory -PathType Container)) {
    throw "Maven dependency cache directory not found: $RepositoryDirectory"
}
$RepositoryDirectory = (Resolve-Path -LiteralPath $RepositoryDirectory).Path

$javacPath = Join-Path $JdkDirectory 'bin/javac.exe'
$javaPath = Join-Path $JavaDirectory 'bin/java.exe'
if (-not (Test-Path -LiteralPath $javacPath -PathType Leaf)) {
    throw "javac.exe not found under JDK directory: $JdkDirectory"
}
if (-not (Test-Path -LiteralPath $javaPath -PathType Leaf)) {
    throw "java.exe not found under Java directory: $JavaDirectory"
}
$javacVersion = (& $javacPath -version 2>&1 | Out-String).Trim()
if ($LASTEXITCODE -ne 0) { throw "Could not run javac: $javacVersion" }
$versionMatch = [regex]::Match($javacVersion, '^javac\s+(\d+)')
if (-not $versionMatch.Success -or [int]$versionMatch.Groups[1].Value -lt 9) {
    throw "JDK 9 or newer is required for --release 8. Detected: $javacVersion"
}

$jarRelativePaths = @(
    'javax/persistence/javax.persistence-api/2.2/javax.persistence-api-2.2.jar',
    'org/hibernate/hibernate-core/5.6.15.Final/hibernate-core-5.6.15.Final.jar',
    'org/jboss/logging/jboss-logging/3.4.3.Final/jboss-logging-3.4.3.Final.jar',
    'net/bytebuddy/byte-buddy/1.12.18/byte-buddy-1.12.18.jar',
    'antlr/antlr/2.7.7/antlr-2.7.7.jar',
    'org/jboss/spec/javax/transaction/jboss-transaction-api_1.2_spec/1.1.1.Final/jboss-transaction-api_1.2_spec-1.1.1.Final.jar',
    'org/jboss/jandex/2.4.2.Final/jandex-2.4.2.Final.jar',
    'com/fasterxml/classmate/1.5.1/classmate-1.5.1.jar',
    'javax/activation/javax.activation-api/1.2.0/javax.activation-api-1.2.0.jar',
    'org/hibernate/common/hibernate-commons-annotations/5.1.2.Final/hibernate-commons-annotations-5.1.2.Final.jar',
    'javax/xml/bind/jaxb-api/2.3.1/jaxb-api-2.3.1.jar',
    'org/glassfish/jaxb/jaxb-runtime/2.3.1/jaxb-runtime-2.3.1.jar',
    'org/glassfish/jaxb/txw2/2.3.1/txw2-2.3.1.jar',
    'com/sun/istack/istack-commons-runtime/3.0.7/istack-commons-runtime-3.0.7.jar',
    'org/jvnet/staxex/stax-ex/1.8/stax-ex-1.8.jar',
    'com/sun/xml/fastinfoset/FastInfoset/1.2.15/FastInfoset-1.2.15.jar',
    'com/fasterxml/jackson/core/jackson-core/2.21.4/jackson-core-2.21.4.jar',
    'com/fasterxml/jackson/core/jackson-databind/2.21.4/jackson-databind-2.21.4.jar',
    'com/fasterxml/jackson/core/jackson-annotations/2.21/jackson-annotations-2.21.jar',
    'com/fasterxml/jackson/datatype/jackson-datatype-jsr310/2.21.4/jackson-datatype-jsr310-2.21.4.jar',
    'org/slf4j/slf4j-api/2.0.20/slf4j-api-2.0.20.jar',
    'org/slf4j/slf4j-simple/2.0.20/slf4j-simple-2.0.20.jar',
    'com/microsoft/sqlserver/mssql-jdbc/13.2.1.jre8/mssql-jdbc-13.2.1.jre8.jar',
    'com/h2database/h2/2.1.214/h2-2.1.214.jar',
    'junit/junit/4.13.2/junit-4.13.2.jar',
    'org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar'
)
$jarPaths = foreach ($relativePath in $jarRelativePaths) {
    $jarPath = Join-Path $RepositoryDirectory $relativePath
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "Missing cached dependency: $relativePath. Run Maven/Eclipse Update Project first, or pass the correct -RepositoryDirectory."
    }
    $jarPath
}
$flatlafPath = Join-Path $RepositoryDirectory 'com/formdev/flatlaf/3.7/flatlaf-3.7.jar'
if (-not (Test-Path -LiteralPath $flatlafPath -PathType Leaf)) {
    $flatlafPath = Join-Path $projectDirectory '.tmp/dependencies/flatlaf-3.7.jar'
}
if (-not (Test-Path -LiteralPath $flatlafPath -PathType Leaf)) {
    throw 'Missing FlatLaf 3.7. Run Maven/Eclipse Update Project first, or place the jar at .tmp/dependencies/flatlaf-3.7.jar.'
}
$classpath = (@($jarPaths) + $flatlafPath) -join ';'

if ($TestClasses.Count -eq 0) {
    $discoveredTests = foreach ($testFile in Get-ChildItem -LiteralPath (Join-Path $projectDirectory 'src/test/java') -Recurse -Filter '*.java') {
        $source = [System.IO.File]::ReadAllText($testFile.FullName)
        if ($source -match '(?m)^\s*@(?:org\.junit\.)?Test\b') {
            $packageMatch = [regex]::Match($source, '(?m)^\s*package\s+([\w.]+)\s*;')
            if ($packageMatch.Success) {
                $packageMatch.Groups[1].Value + '.' + $testFile.BaseName
            } else {
                $testFile.BaseName
            }
        }
    }
    $TestClasses = @($discoveredTests | Sort-Object -Unique)
}
if ($TestClasses.Count -eq 0) { throw 'No JUnit 4 test classes containing @Test were found.' }

# Use a fresh OS-temp output directory so deleted/renamed sources cannot leave
# stale .class files that make this fallback runner report a false pass.
$buildDirectory = Join-Path ([IO.Path]::GetTempPath()) ('QuanAo-Test-Local-' + [guid]::NewGuid().ToString('N'))
$outputDirectory = Join-Path $buildDirectory 'classes'
New-Item -ItemType Directory -Path $outputDirectory | Out-Null

Push-Location $projectDirectory
try {
    $sourceFiles = @(Get-ChildItem -LiteralPath 'src/main/java','src/test/java' -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
    if ($sourceFiles.Count -eq 0) { throw 'No Java source files were found.' }
    & $javacPath --release 8 -encoding UTF-8 -cp $classpath -d $outputDirectory $sourceFiles
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    $runtimeClasspath = $outputDirectory + ';' + (Join-Path $projectDirectory 'src/main/resources') + ';' + (Join-Path $projectDirectory 'src/test/resources') + ';' + $classpath
    & $javaPath '-Djava.awt.headless=true' -cp $runtimeClasspath org.junit.runner.JUnitCore @TestClasses
    if ($LASTEXITCODE -ne 0) { throw 'JUnit failed.' }
    & $javaPath '-Djava.awt.headless=true' -cp $runtimeClasspath vn.edu.iuh.quanao.ScaffoldSmokeTest
    if ($LASTEXITCODE -ne 0) { throw 'Scaffold smoke test failed.' }
} finally { Pop-Location }
