$ErrorActionPreference = 'Stop'
$root = 'D:\Create Forge of the Horizons\CFHCore'
$worldDir = Join-Path $root 'src\main\java\com\onehumanawa\cfhcore\content\world'
$cfhCore = Join-Path $root 'src\main\java\com\onehumanawa\cfhcore\CFHCore.java'

$enc = New-Object System.Text.UTF8Encoding($false)

# Ordered, case-sensitive ordinal replacements.
$pairs = @(
    @('ReTerraForged', 'CFHWorldGen'),
    @('TerraForged',   'CFHWorldGen'),
    @('RTF',           'CFHWorldGen'),
    @('rtf',           'cfhWorldGen')
)

$targets = @()
$targets += Get-ChildItem -Recurse -File -Path $worldDir -Filter *.java
$targets += Get-Item $cfhCore

$changedFiles = 0
$totalReplacements = 0
foreach ($f in $targets) {
    $text = [System.IO.File]::ReadAllText($f.FullName, $enc)
    $orig = $text
    foreach ($p in $pairs) {
        $count = ([regex]::Matches($text, [regex]::Escape($p[0]))).Count
        if ($count -gt 0) {
            $text = $text.Replace($p[0], $p[1])
            $totalReplacements += $count
        }
    }
    if ($text -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $text, $enc)
        $changedFiles++
    }
}

Write-Output ("Content: changed {0} files, {1} replacements" -f $changedFiles, $totalReplacements)

# Rename files whose name contains RTF.
$renamed = 0
foreach ($f in (Get-ChildItem -Recurse -File -Path $worldDir -Filter *.java)) {
    if ($f.Name -clike '*RTF*') {
        $newName = $f.Name.Replace('RTF', 'CFHWorldGen')
        Rename-Item -LiteralPath $f.FullName -NewName $newName
        Write-Output ("Renamed: {0} -> {1}" -f $f.Name, $newName)
        $renamed++
    }
}
Write-Output ("Renamed {0} files" -f $renamed)
