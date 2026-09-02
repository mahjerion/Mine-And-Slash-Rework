# Shared helpers for the CTE2 Codex generator.
#
# Everything reads two roots and lets the CTE2 pack win:
#   repo  src/generated/resources/data/mmorpg/<registry>      (mod defaults, generated in-game)
#   pack  <instance>/config/openloader/data/cte_mns/data/mmorpg/<registry>   (pack overrides)
#
# Windows PowerShell 5.1 only: ConvertFrom-Json returns PSCustomObjects (no -AsHashtable),
# and every file is written through a BOM-less UTF8 encoder.

$script:REPO_ROOT = 'C:\Users\Kelvin\Documents\GitHub\Mine-And-Slash-Rework'
$script:PACK_ROOT = 'C:\Users\Kelvin\curseforge\minecraft\Instances\Craft to Exile 2'

$script:DATA_REPO = Join-Path $REPO_ROOT 'src\generated\resources\data\mmorpg'
$script:DATA_PACK = Join-Path $PACK_ROOT 'config\openloader\data\cte_mns\data\mmorpg'
$script:LANG_REPO = Join-Path $REPO_ROOT 'src\main\resources\assets\mmorpg\lang\en_us.json'
$script:LANG_PACK = Join-Path $PACK_ROOT 'config\openloader\resources\resources\assets\mmorpg\lang\en_us.json'

$script:SECT = [char]0xA7

function Write-Utf8NoBom {
    param([string]$Path, [string]$Text)
    $dir = Split-Path $Path -Parent
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    [System.IO.File]::WriteAllText($Path, $Text, (New-Object System.Text.UTF8Encoding($false)))
}

# ---------------------------------------------------------------- lang

function Import-Lang {
    $lang = @{}
    foreach ($f in @($script:LANG_REPO, $script:LANG_PACK)) {
        if (-not (Test-Path $f)) { continue }
        $j = Get-Content $f -Raw -Encoding UTF8 | ConvertFrom-Json
        foreach ($p in $j.PSObject.Properties) { $lang[$p.Name] = [string]$p.Value }
    }
    return $lang
}

# Strip Minecraft colour codes and Enlighten link markup: "[Bleed](bleed) Damage" -> "Bleed Damage"
function Clear-Markup {
    param([string]$s)
    if ($null -eq $s) { return '' }
    $s = [regex]::Replace($s, "$($script:SECT).", '')
    $s = [regex]::Replace($s, '\[([^\]]*)\]\(([^)]*)\)', '$1')
    return $s.Trim()
}

function ConvertTo-TitleCase {
    param([string]$id)
    if ([string]::IsNullOrWhiteSpace($id)) { return '' }
    $parts = $id -replace '[:/]', '_' -split '_' | Where-Object { $_.Length -gt 0 }
    return (($parts | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1) }) -join ' ')
}

# First lang key that resolves to a non-empty, non-placeholder string; else the title-cased id.
function Resolve-Name {
    param([hashtable]$Lang, [string]$Id, [string[]]$Keys, [string]$Fallback)
    foreach ($k in $Keys) {
        if (-not $Lang.ContainsKey($k)) { continue }
        $t = Clear-Markup $Lang[$k]
        if ([string]::IsNullOrWhiteSpace($t) -or $t -eq 'Unused') { continue }
        return $t
    }
    if ($Fallback) { return $Fallback }
    return (ConvertTo-TitleCase $Id)
}

# ---------------------------------------------------------------- stat labels
#
# Three shapes, told apart by the lang string itself:
#   tmpl  contains [VAL1]  -> a whole sentence, value substituted inline
#   flag  has a colour code but no [VAL1] -> a sentence with no value (the immunities)
#   frag  starts with a lowercase connective ("of ...", "To ...") -> "<range> <label>"
#   plain everything else -> label + badge + range

function Get-StatLabel {
    param([hashtable]$Lang, [string]$Id)
    $raw = $Lang["mmorpg.stat.$Id"]
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return [ordered]@{ k = 'plain'; t = (ConvertTo-TitleCase $Id) }
    }
    $hasVal  = $raw -match '\[VAL1\]'
    $hasSect = $raw.Contains($script:SECT)
    $txt = Clear-Markup $raw
    if ($hasVal)  { return [ordered]@{ k = 'tmpl'; t = $txt } }
    if ($hasSect) { return [ordered]@{ k = 'flag'; t = $txt } }
    if ($txt -match '^(of|to)\s') { return [ordered]@{ k = 'frag'; t = $txt } }
    return [ordered]@{ k = 'plain'; t = $txt }
}

function Get-StatDesc {
    param([hashtable]$Lang, [string]$Id)
    return (Clear-Markup $Lang["mmorpg.stat_desc.$Id"])
}

# ---------------------------------------------------------------- registry loading

# Merge a registry across both roots, pack last so it wins. Keyed on the entry's own id
# field, never the filename: mmorpg_unique_gears keeps a deprecated flat copy beside the
# live per-slot one, so path- or basename-keyed merges double-count or silently drop.
function Import-Registry {
    param(
        [string]$Registry,
        [string]$IdField = 'id',       # 'id' | 'guid' | 'identifier' | 'data.id' | 'auto'
        [switch]$KeepHidden,
        [switch]$KeepZeroWeight
    )
    $out = [ordered]@{}
    foreach ($root in @($script:DATA_REPO, $script:DATA_PACK)) {
        $dir = Join-Path $root $Registry
        if (-not (Test-Path $dir)) { continue }
        foreach ($f in (Get-ChildItem $dir -Recurse -Filter *.json | Sort-Object FullName)) {
            $j = Get-Content $f.FullName -Raw | ConvertFrom-Json
            $id = switch ($IdField) {
                'data.id' { $j.data.id }
                'auto'    { if ($j.id) { $j.id } elseif ($j.data -and $j.data.id) { $j.data.id } else { $null } }
                default   { $j.$IdField }
            }
            if ([string]::IsNullOrWhiteSpace($id)) { $id = $f.BaseName }
            $sub = ''
            $rel = $f.FullName.Substring($dir.Length + 1)
            if ($rel.Contains('\')) { $sub = ($rel -split '\\')[0] }
            Add-Member -InputObject $j -NotePropertyName '_sub'  -NotePropertyValue $sub  -Force
            Add-Member -InputObject $j -NotePropertyName '_id'   -NotePropertyValue ([string]$id) -Force
            $out[[string]$id] = $j
        }
    }
    # wiki filters
    $keys = @($out.Keys)
    foreach ($k in $keys) {
        $j = $out[$k]
        if (-not $KeepHidden -and $j.hide_from_wiki -eq $true) { $out.Remove($k); continue }
        if (-not $KeepZeroWeight -and ($j.PSObject.Properties.Name -contains 'weight') -and [double]$j.weight -le 0) { $out.Remove($k); continue }
    }
    return $out
}

# ---------------------------------------------------------------- stat blocks

# min/max form, as used by affixes, auras, uniques, runewords, map affixes...
function ConvertTo-StatList {
    param($Stats)
    $l = New-Object System.Collections.ArrayList
    foreach ($s in $Stats) {
        if ($null -eq $s -or [string]::IsNullOrWhiteSpace($s.stat)) { continue }
        $mn = if ($null -ne $s.min) { [double]$s.min } elseif ($null -ne $s.v1) { [double]$s.v1 } else { 0 }
        $mx = if ($null -ne $s.max) { [double]$s.max } elseif ($null -ne $s.v1) { [double]$s.v1 } else { $mn }
        $ty = if ($s.type) { ([string]$s.type).ToUpper() } else { 'FLAT' }
        [void]$l.Add([ordered]@{ stat = [string]$s.stat; type = $ty; min = $mn; max = $mx })
    }
    # the leading comma matters: PowerShell unrolls a function's return value, so a plain
    # "return @($l)" hands back a bare object for one stat and nothing for none, which then
    # serialises as {...} or null instead of an array
    return ,@($l)
}

function Add-StatIds {
    param([hashtable]$Bag, $StatList)
    foreach ($s in $StatList) { $Bag[$s.stat] = 1 }
}

# Filler entries that exist only so a registry is never empty. Not content.
$script:PLACEHOLDER_IDS = @('empty', 'unknown', 'none')

function Test-Placeholder {
    param([string]$Id)
    return ($script:PLACEHOLDER_IDS -contains $Id)
}

function New-Fact {
    param([string]$Label, $Value)
    return [ordered]@{ k = $Label; v = [string]$Value }
}

function Format-Multi {
    param($v)
    if ($null -eq $v) { return '' }
    $d = [double]$v
    return ([string]([math]::Round($d, 3))) + 'x'
}
