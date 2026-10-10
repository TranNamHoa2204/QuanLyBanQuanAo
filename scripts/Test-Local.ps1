param(
    [Parameter(Mandatory = $true)][string]$JdkDirectory,
    [string]$JavaDirectory = $JdkDirectory,
    [string[]]$TestClasses = @('DependencySmokeTest', 'SecurityProtocolTest', 'AuthIntegrationTest',
        'ProductIntegrationTest', 'NormalizedCatalogTest', 'ProductUiTest', 'InvoiceIntegrationTest', 'InvoiceUiTest', 'FlatLafUiTest',
        'CatalogManagementIntegrationTest', 'CatalogManagementUiTest')
)
$ErrorActionPreference = 'Stop'
$projectDirectory = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repositoryDirectory = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.m2/repository'
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
    $jarPath = Join-Path $repositoryDirectory $relativePath
    if (-not (Test-Path -LiteralPath $jarPath)) {
        throw "Missing cached dependency: $relativePath. Use Eclipse Maven Update Project/test first."
    }
    $jarPath
}
$javacPath = Join-Path $JdkDirectory 'bin/javac.exe'
$javaPath = Join-Path $JavaDirectory 'bin/java.exe'
$outputDirectory = Join-Path $projectDirectory '.tmp/auth-build/classes'
New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
$classpath = $jarPaths -join ';'
$flatlafPath = Join-Path $repositoryDirectory 'com/formdev/flatlaf/3.7/flatlaf-3.7.jar'
if (-not (Test-Path -LiteralPath $flatlafPath)) {
    $flatlafPath = Join-Path $projectDirectory '.tmp/dependencies/flatlaf-3.7.jar'
}
if (-not (Test-Path -LiteralPath $flatlafPath)) {
    throw 'Missing FlatLaf 3.7. Run Eclipse Maven Update Project first.'
}
$classpath = $classpath + ';' + $flatlafPath
Push-Location $projectDirectory
try {
    $sourceFiles = @(Get-ChildItem -LiteralPath 'src/main/java','src/test/java' -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName)
    & $javacPath --release 8 -encoding UTF-8 -cp $classpath -d $outputDirectory $sourceFiles
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }
    $runtimeClasspath = $outputDirectory + ';' + (Join-Path $projectDirectory 'src/main/resources') + ';' + $classpath
    & $javaPath '-Djava.awt.headless=true' -cp $runtimeClasspath org.junit.runner.JUnitCore @TestClasses
    if ($LASTEXITCODE -ne 0) { throw 'JUnit failed.' }
    & $javaPath -cp $runtimeClasspath vn.edu.iuh.quanao.ScaffoldSmokeTest
    if ($LASTEXITCODE -ne 0) { throw 'Scaffold smoke test failed.' }
} finally { Pop-Location }
