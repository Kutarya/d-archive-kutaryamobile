param([switch]$Kaynak)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$env:JAVA_HOME = Join-Path $taskRoot '.tools\jbr'
$outRoot = Join-Path $taskRoot 'Dagitim'
New-Item -ItemType Directory -Path $outRoot -Force | Out-Null
$apkSource = Join-Path $taskRoot 'app\build\outputs\apk\release\app-release.apk'
if (-not (Test-Path -LiteralPath $apkSource)) { throw 'Önce assembleRelease ile imzalı APK oluştur.' }
$taskBuildConfig = Get-Content -LiteralPath (Join-Path $taskRoot 'app\build.gradle.kts') -Raw
$taskVersionMatch = [regex]::Match($taskBuildConfig,'versionName\s*=\s*"([0-9]+\.[0-9]+\.[0-9]+)"')
if (-not $taskVersionMatch.Success) { throw 'Sürüm adı derleme ayarından okunamadı.' }
$taskVersion = $taskVersionMatch.Groups[1].Value
$taskApkName = "Kutarya-Mobile-$taskVersion.apk"
$apkTarget = Join-Path $outRoot $taskApkName
Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force
$buildTools = Join-Path $taskRoot '.tools\android-sdk\build-tools\36.0.0'
$signResult = & "$buildTools\apksigner.bat" verify --verbose --print-certs $apkTarget 2>&1
if ($LASTEXITCODE -ne 0) { throw 'APK imzası doğrulanamadı.' }
$signResult | Set-Content -LiteralPath (Join-Path $outRoot 'APK-IMZA.txt') -Encoding utf8BOM
$badging = & "$buildTools\aapt.exe" dump badging $apkTarget 2>&1
if ($LASTEXITCODE -ne 0 -or -not ($badging -match "name='com.kutarya.mobile'")) { throw 'APK paket kimliği doğrulanamadı.' }
$badging | Set-Content -LiteralPath (Join-Path $outRoot 'APK-BILGI.txt') -Encoding utf8BOM
if (-not (($badging -join [Environment]::NewLine).Contains("versionName='$taskVersion'"))) { throw 'APK sürümü kaynak ayarıyla uyuşmuyor.' }
$hash = (Get-FileHash -LiteralPath $apkTarget -Algorithm SHA256).Hash
$record = [ordered]@{app='Kutarya Mobile';assistant='Kutarya Us';version=$taskVersion;package='com.kutarya.mobile';minSdk=26;targetSdk=36;file=$taskApkName;bytes=(Get-Item -LiteralPath $apkTarget).Length;sha256=$hash;builtAt=(Get-Item -LiteralPath $apkSource).LastWriteTime.ToString('o');packagedAt=(Get-Date).ToString('o')}
$record | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outRoot 'APK-KAYDI.json') -Encoding utf8BOM
$record | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outRoot "APK-KAYDI-$taskVersion.json") -Encoding utf8BOM
"$hash  $taskApkName" | Set-Content -LiteralPath (Join-Path $outRoot 'SHA256.txt') -Encoding ascii
if ($Kaynak) {
    Add-Type -AssemblyName System.IO.Compression
    $sourceNames=@('.gitignore','AGENTS.md','DEVIR.md','README.md','build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew.bat','app\build.gradle.kts')
    $sourceFiles=@($sourceNames | ForEach-Object { Join-Path $taskRoot $_ })
    foreach ($folder in @('app\src','gradle','scripts','docs')) { $sourceFiles += Get-ChildItem -LiteralPath (Join-Path $taskRoot $folder) -Recurse -File | Select-Object -ExpandProperty FullName }
    $archivePath = Join-Path $outRoot "Kutarya-Mobile-$taskVersion-Kaynak.zip"
    $stream=[IO.File]::Open($archivePath,[IO.FileMode]::Create)
    $archive=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($sourceFile in $sourceFiles | Sort-Object -Unique) {
            $absolute=[IO.Path]::GetFullPath($sourceFile)
            if (-not $absolute.StartsWith($taskRoot+'\',[StringComparison]::OrdinalIgnoreCase)) { throw 'Arşiv kaynağı proje dışında.' }
            $name=$absolute.Substring($taskRoot.Length+1).Replace('\','/')
            if ($name -match '(^|/)(\.secrets|\.tools|build|Kanitlar|Dagitim)(/|$)' -or $name -match '\.(jks|keystore)$|signing\.properties$|local\.properties$') { throw 'Özel dosya arşiv listesine girdi.' }
            $entry=$archive.CreateEntry($name,[IO.Compression.CompressionLevel]::Optimal)
            $input=[IO.File]::OpenRead($absolute); $output=$entry.Open()
            try { $input.CopyTo($output) } finally { $input.Dispose();$output.Dispose() }
        }
    } finally { $archive.Dispose();$stream.Dispose() }
}
$record | ConvertTo-Json
