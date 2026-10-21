[CmdletBinding()]
param(
    [switch]$SkipTests
)

$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Root = Split-Path -Parent $PSScriptRoot
$Tools = Join-Path $Root '.tools'
$Dist = Join-Path $Root 'dist'
$Logs = Join-Path $Root 'build-logs'
New-Item -ItemType Directory -Force -Path $Tools, $Logs | Out-Null
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$Log = Join-Path $Logs "build-$Stamp.log"

$AppName = 'Pyhoma'
$AppVersion = '1.0.0'
$UpgradeUuid = '241ea7a4-95f2-539c-b574-4d367c3e5c72'
$MavenVersion = '3.9.9'
$JdkUrl = 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse'
$MavenUrl = "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
$WixUrls = @(
    'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip',
    'https://github.com/wixtoolset/wix3/releases/download/wix3112rtm/wix311-binaries.zip'
)
$Modules = @(
    'java.base', 'java.compiler', 'java.datatransfer', 'java.desktop', 'java.instrument', 'java.logging',
    'java.management', 'java.naming', 'java.net.http', 'java.prefs', 'java.rmi', 'java.scripting',
    'java.security.jgss', 'java.security.sasl', 'java.sql', 'java.transaction.xa', 'java.xml', 'java.xml.crypto',
    'jdk.charsets', 'jdk.crypto.cryptoki', 'jdk.crypto.ec', 'jdk.localedata', 'jdk.management', 'jdk.naming.dns',
    'jdk.unsupported', 'jdk.zipfs'
) -join ','

function Write-Step([string]$Text) {
    $line = "==> $Text"
    Write-Host $line -ForegroundColor Cyan
    Add-Content -Path $Log -Value $line -Encoding UTF8
}

function Fail([string]$Text) {
    Write-Host "ERRO / ERROR: $Text" -ForegroundColor Red
    Add-Content -Path $Log -Value "ERROR: $Text" -Encoding UTF8
    Write-Host "Log: $Log"
    exit 1
}

function Invoke-Logged([string]$Exe, [string[]]$Arguments) {
    & $Exe @Arguments 2>&1 | ForEach-Object {
        $line = "$_"
        Write-Host $line
        Add-Content -Path $Log -Value $line -Encoding UTF8
    }
    return $LASTEXITCODE
}

function Get-Archive([string[]]$Urls, [string]$Target, [string]$Name) {
    $zip = Join-Path $Tools "$Name.zip"
    foreach ($url in $Urls) {
        try {
            Write-Step "Baixando / Downloading $Name ($url)"
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing -ErrorAction Stop
            if (Test-Path $Target) { Remove-Item -Recurse -Force $Target }
            Expand-Archive -Path $zip -DestinationPath $Target -Force -ErrorAction Stop
            Remove-Item $zip -Force
            return
        } catch {
            Add-Content -Path $Log -Value "Download failed: $url - $($_.Exception.Message)" -Encoding UTF8
        }
    }
    Fail "Nao foi possivel baixar $Name. Verifique a internet ou instale manualmente. / Could not download $Name."
}

function Get-JavaMajor([string]$JavaExe) {
    if (-not (Test-Path $JavaExe)) { return 0 }
    $out = & $JavaExe -version 2>&1 | Out-String
    if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    return 0
}

Write-Step 'Procurando JDK 21+ / Looking for JDK 21+'
$JavaHome = $null
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$javaOnPath = Get-Command javac.exe -ErrorAction SilentlyContinue
if ($javaOnPath) { $candidates += (Split-Path -Parent (Split-Path -Parent $javaOnPath.Source)) }
$candidates += Get-ChildItem -Path (Join-Path $Tools 'jdk') -Directory -ErrorAction SilentlyContinue | ForEach-Object { $_.FullName }
foreach ($c in $candidates) {
    $java = Join-Path $c 'bin\java.exe'
    if ((Get-JavaMajor $java) -ge 21 -and (Test-Path (Join-Path $c 'bin\jpackage.exe'))) { $JavaHome = $c; break }
}
if (-not $JavaHome) {
    Get-Archive @($JdkUrl) (Join-Path $Tools 'jdk') 'jdk-21'
    $JavaHome = (Get-ChildItem -Path (Join-Path $Tools 'jdk') -Directory | Select-Object -First 1).FullName
}
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
Write-Step "JDK: $JavaHome"

Write-Step 'Procurando Maven / Looking for Maven'
$Mvn = $null
$mvnOnPath = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($mvnOnPath) { $Mvn = $mvnOnPath.Source }
if (-not $Mvn) {
    $local = Join-Path $Tools "maven\apache-maven-$MavenVersion\bin\mvn.cmd"
    if (-not (Test-Path $local)) { Get-Archive @($MavenUrl) (Join-Path $Tools 'maven') 'maven' }
    $Mvn = $local
}
Write-Step "Maven: $Mvn"

Write-Step 'Procurando WiX Toolset 3 / Looking for WiX Toolset 3'
$candle = Get-Command candle.exe -ErrorAction SilentlyContinue
if (-not $candle) {
    $wixDir = Join-Path $Tools 'wix'
    if (-not (Test-Path (Join-Path $wixDir 'candle.exe'))) { Get-Archive $WixUrls $wixDir 'wix' }
    $env:Path = "$wixDir;$env:Path"
}

Push-Location $Root
try {
    $mvnArgs = @('-B', '-ntp', 'clean', 'verify')
    if ($SkipTests) { $mvnArgs += '-DskipTests' }
    Write-Step "mvn $($mvnArgs -join ' ')"
    $rc = Invoke-Logged $Mvn $mvnArgs
    if ($rc -ne 0) { Fail 'A compilacao Maven falhou. / The Maven build failed.' }

    $AppInput = Join-Path $Root 'pyhoma-desktop\target\app'
    if (-not (Test-Path (Join-Path $AppInput 'pyhoma.jar'))) { Fail "pyhoma.jar nao encontrado em $AppInput" }

    $ResDir = Join-Path $Root 'pyhoma-desktop\target\jpackage-resources'
    New-Item -ItemType Directory -Force -Path $ResDir | Out-Null
    Copy-Item (Join-Path $Root 'packaging\windows\pyhoma.ico') (Join-Path $ResDir "$AppName.ico") -Force
    $BannerBmp = Join-Path $Root 'packaging\windows\installer-banner.bmp'
    $DialogBmp = Join-Path $Root 'packaging\windows\installer-dialog.bmp'
    $Extract = Join-Path $ResDir 'jdk'
    if (Test-Path $Extract) { Remove-Item -Recurse -Force $Extract }
    $null = Invoke-Logged (Join-Path $JavaHome 'bin\jimage.exe') @('extract', '--dir', $Extract, '--include', 'regex:.*/main\.wxs', (Join-Path $JavaHome 'lib\modules'))
    $MainWxs = Get-ChildItem -Path $Extract -Recurse -Filter 'main.wxs' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($MainWxs) {
        $Xml = Get-Content -Path $MainWxs.FullName -Raw
        $Branding = "<WixVariable Id=`"WixUIBannerBmp`" Value=`"$BannerBmp`" />`r`n    <WixVariable Id=`"WixUIDialogBmp`" Value=`"$DialogBmp`" />`r`n    <UIRef Id=`"JpUI`"/>"
        $Xml = $Xml.Replace('<UIRef Id="JpUI"/>', $Branding)
        $CleanupActions = Get-Content -Path (Join-Path $Root 'packaging\windows\cleanup-actions.xml') -Raw
        $CleanupSequence = Get-Content -Path (Join-Path $Root 'packaging\windows\cleanup-sequence.xml') -Raw
        $Xml = $Xml.Replace('<InstallExecuteSequence>', $CleanupActions + "`r`n    <InstallExecuteSequence>`r`n" + $CleanupSequence)
        if (-not $Xml.Contains('PyhomaCleanData')) { Fail 'Nao foi possivel incluir a limpeza na desinstalacao. / Could not add the uninstall cleanup.' }
        Set-Content -Path (Join-Path $ResDir 'main.wxs') -Value $Xml -Encoding UTF8
    } else {
        Write-Step 'Modelo main.wxs nao encontrado; instalador sem imagens personalizadas. / main.wxs template not found.'
    }

    Get-ChildItem -Path $Root -Filter "$AppName-*.msi" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
    if (Test-Path $Dist) {
        Get-ChildItem -Path $Dist -Filter "$AppName-*.*" -ErrorAction SilentlyContinue | Remove-Item -Force -ErrorAction SilentlyContinue
        if (-not (Get-ChildItem -Path $Dist -Force -ErrorAction SilentlyContinue)) { Remove-Item -Force $Dist -ErrorAction SilentlyContinue }
    }
    foreach ($t in @('exe')) {
        Write-Step "jpackage --type $t"
        $OutDir = $Root
        $old = Get-ChildItem -Path $Root -Filter "$AppName-*.$t" -ErrorAction SilentlyContinue
        if ($old) { $old | Remove-Item -Force -ErrorAction SilentlyContinue }
        Get-ChildItem -Path $Root -Filter "$AppName-*.$t" -ErrorAction SilentlyContinue | ForEach-Object {
            Move-Item -Path $_.FullName -Destination (Join-Path $Logs "old-$Stamp-$($_.Name)") -Force -ErrorAction SilentlyContinue
        }
        if (Get-ChildItem -Path $Root -Filter "$AppName-*.$t" -ErrorAction SilentlyContinue) {
            Fail "Feche o instalador antigo ($AppName-*.$t), ele esta em uso, e rode de novo. / Close the old installer ($AppName-*.$t), it is in use, and run again."
        }
        $jpArgs = @(
            '--type', $t,
            '--name', $AppName,
            '--app-version', $AppVersion,
            '--vendor', 'Pyhoma',
            '--description', 'Pyhoma. - IT service management, help desk and asset inventory',
            '--copyright', 'MIT License',
            '--input', $AppInput,
            '--main-jar', 'pyhoma.jar',
            '--main-class', 'io.pyhoma.desktop.Main',
            '--icon', (Join-Path $Root 'packaging\windows\pyhoma.ico'),
            '--resource-dir', $ResDir,
            '--license-file', (Join-Path $Root 'LICENSE'),
            '--add-modules', $Modules,
            '--java-options', '-Xms128m',
            '--java-options', '-Xmx1024m',
            '--java-options', '-XX:+DisableAttachMechanism',
            '--java-options', '-XX:-HeapDumpOnOutOfMemoryError',
            '--java-options', '-Dprism.forceGPU=true',
            '--java-options', '-Dprism.vsync=true',
            '--java-options', '-Dfile.encoding=UTF-8',
            '--java-options', '-Dstdout.encoding=UTF-8',
            '--win-dir-chooser',
            '--win-menu',
            '--win-menu-group', 'Pyhoma',
            '--win-shortcut',
            '--win-upgrade-uuid', $UpgradeUuid,
            '--install-dir', 'Pyhoma',
            '--dest', $OutDir,
            '--temp', (Join-Path $Logs "jpackage-$t-$Stamp"),
            '--verbose'
        )
        $rc = Invoke-Logged (Join-Path $JavaHome 'bin\jpackage.exe') $jpArgs
        if ($rc -ne 0) { Fail "jpackage ($t) falhou. / jpackage ($t) failed." }
        Remove-Item -Recurse -Force (Join-Path $Logs "jpackage-$t-$Stamp") -ErrorAction SilentlyContinue
    }
} finally {
    Pop-Location
}

Write-Host ''
Write-Host 'Pronto! / Done!' -ForegroundColor Green
Get-ChildItem -Path (Join-Path $Root "$AppName-*.exe") -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "  $($_.FullName)  ($([math]::Round($_.Length / 1MB, 1)) MB)" }
Add-Content -Path $Log -Value "BUILD SUCCESS" -Encoding UTF8
Write-Host "Log: $Log"
