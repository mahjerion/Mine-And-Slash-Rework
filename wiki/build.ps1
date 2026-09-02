# Assembles the CTE2 Compendium pages from wiki/data + wiki/config + wiki/tpl.
#
# Two flavours from one set of templates:
#   artifact  body fragment; nav points at published artifact URLs (config/nav.json)
#   site      standalone HTML documents with relative nav - the GitHub Pages folder
#
#   .\build.ps1                 both flavours
#   .\build.ps1 -Flavour site   just the static site

param([ValidateSet('both','artifact','site')][string]$Flavour = 'both')

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'lib\common.ps1')

$TPL  = Join-Path $PSScriptRoot 'tpl'
$CFG  = Join-Path $PSScriptRoot 'config'
$DATA = Join-Path $PSScriptRoot 'data'
$OUT  = Join-Path $PSScriptRoot 'out'

$css  = Get-Content (Join-Path $TPL 'base.css')  -Raw -Encoding UTF8
$body = Get-Content (Join-Path $TPL 'body.html') -Raw -Encoding UTF8
$js   = Get-Content (Join-Path $TPL 'app.js')    -Raw -Encoding UTF8
$hub  = Get-Content (Join-Path $TPL 'hub.html')  -Raw -Encoding UTF8
$pages = Get-Content (Join-Path $CFG 'pages.json') -Raw -Encoding UTF8 | ConvertFrom-Json

# pageId -> published artifact URL; absent entries simply drop out of the artifact nav
$navMap = @{}
$navPath = Join-Path $CFG 'nav.json'
if (Test-Path $navPath) {
    $nj = Get-Content $navPath -Raw -Encoding UTF8 | ConvertFrom-Json
    foreach ($p in $nj.PSObject.Properties) { $navMap[$p.Name] = [string]$p.Value }
}

# ---------------------------------------------------------------- nav

# Sections in the order pages.json lists them, each holding its pages.
function Build-Nav {
    param([string]$FlavourName)
    $order = New-Object System.Collections.ArrayList
    $bySec = @{}
    foreach ($p in $pages) {
        if (-not $bySec.ContainsKey($p.section)) {
            $bySec[$p.section] = New-Object System.Collections.ArrayList
            [void]$order.Add($p.section)
        }
        $url = if ($FlavourName -eq 'site') { "$($p.file).html" } else { $navMap[$p.id] }
        if ([string]::IsNullOrWhiteSpace($url)) { continue }
        [void]$bySec[$p.section].Add([ordered]@{ id = $p.id; label = $p.navLabel; url = $url })
    }
    $out = New-Object System.Collections.ArrayList
    # the index sits above the sections as its own row
    $homeUrl = if ($FlavourName -eq 'site') { 'index.html' } else { $navMap['index'] }
    if (-not [string]::IsNullOrWhiteSpace($homeUrl)) {
        [void]$out.Add([ordered]@{ label = 'Home'; pages = @(@{ id = 'index'; label = 'Index'; url = $homeUrl }) })
    }
    foreach ($s in $order) {
        if ($bySec[$s].Count -eq 0) { continue }
        [void]$out.Add([ordered]@{ label = $s; pages = @($bySec[$s]) })
    }
    return ,$out
}

# ---------------------------------------------------------------- page shell

$SITE_HEAD = @'
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>__TITLE__ - CTE2 Compendium</title>
<meta name="description" content="__BLURB__">
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700&family=Work+Sans:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">
<style>
  :root { color-scheme: light dark; }
  img { max-width: 100%; }
  [hidden] { display: none !important; }
__CSS__
</style>
</head>
<body>
'@

$ARTIFACT_HEAD = @'
<title>__TITLE__</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Cinzel:wght@500;600;700&family=Work+Sans:wght@400;500;600&family=JetBrains+Mono:wght@400;500;600&display=swap" rel="stylesheet">

<style>
__CSS__
</style>

'@

function Write-Page {
    param([string]$FlavourName, [string]$FileStem, [string]$Title, [string]$Blurb, [string]$BodyHtml)
    if ($FlavourName -eq 'site') {
        $full = if ($Title -eq 'CTE2 Compendium') { $Title } else { "$Title - CTE2 Compendium" }
        $head = $SITE_HEAD.Replace('__TITLE__ - CTE2 Compendium', $full).Replace('__BLURB__', $Blurb).Replace('__CSS__', $css)
        $html = $head + $BodyHtml + "`n</body>`n</html>`n"
        Write-Utf8NoBom (Join-Path $OUT "site\$FileStem.html") $html
    } else {
        $head = $ARTIFACT_HEAD.Replace('__TITLE__', $Title).Replace('__CSS__', $css)
        Write-Utf8NoBom (Join-Path $OUT "artifact\$FileStem.html") ($head + $BodyHtml)
    }
}

# ---------------------------------------------------------------- build

$flavours = if ($Flavour -eq 'both') { @('artifact','site') } else { @($Flavour) }

foreach ($fl in $flavours) {
    $navJson = ConvertTo-Json -InputObject (Build-Nav $fl) -Depth 8 -Compress
    Write-Host ""
    Write-Host "=== $fl ==="

    # Clear the flavour's folder first. Renaming a page used to leave the old file behind,
    # which then shipped stale copy and, on the static site, a second live URL for the page.
    $dir = Join-Path $OUT $fl
    if (Test-Path $dir) {
        $stale = Get-ChildItem $dir -Filter *.html
        if ($stale) {
            Remove-Item $stale.FullName -Force
            Write-Host ("  cleared {0} previous file(s)" -f @($stale).Count)
        }
    }

    foreach ($p in $pages) {
        $d = Get-Content (Join-Path $DATA "$($p.id).json") -Raw -Encoding UTF8 | ConvertFrom-Json

        # page id rides along in the config so the nav can mark the current page
        $cfgObj = $p.config | ConvertTo-Json -Depth 12 | ConvertFrom-Json
        $cfgObj | Add-Member -NotePropertyName id -NotePropertyValue $p.id -Force

        # the datapack folders a page was built from belong in the footer, not the header:
        # useful when you are editing the pack, noise when you are playing it
        if ($p.source) {
            $src = [pscustomobject]@{
                k = 'SOURCE'; cat = ''
                html = "Generated from <code>$($p.source)</code> in the mod's own data and the pack's openloader overrides. Where both define an entry the pack wins, which is the same order the game loads them in."
            }
            $cfgObj.legend = @(@($cfgObj.legend) + $src)
        }
        $cfg = $cfgObj | ConvertTo-Json -Depth 12 -Compress

        $b = $body.Replace('__EYEBROW__', $p.eyebrow).Replace('__H1__', $p.h1).Replace('__DEK__', $p.dek).
                   Replace('__EXTRA__', $(if ($p.extra) { $p.extra } else { '' })).
                   Replace('__FILTER_TITLE__', $p.filterTitle)
        $s = $js.Replace('__CONFIG__', $cfg).Replace('__NAV__', $navJson).
                 Replace('__DATA__',   (ConvertTo-Json -InputObject $d.rows    -Depth 12 -Compress)).
                 Replace('__LABELS__', (ConvertTo-Json -InputObject $d.labels  -Depth 6  -Compress)).
                 Replace('__KEYMETA__',(ConvertTo-Json -InputObject $d.keyMeta -Depth 6  -Compress))

        $stem = if ($fl -eq 'site') { $p.file } else { $p.id }
        Write-Page $fl $stem $p.title $p.blurb ($b + "`n<script>`n" + $s + "`n</script>`n")
        Write-Host ("  {0,-14} {1,-28} {2,8} bytes  rows={3}" -f $p.id, $p.title, (Get-Item (Join-Path $OUT "$fl\$stem.html")).Length, @($d.rows).Count)
    }

    # ---- hub ----
    $order = New-Object System.Collections.ArrayList
    $bySec = @{}
    $total = 0
    foreach ($p in $pages) {
        $d = Get-Content (Join-Path $DATA "$($p.id).json") -Raw -Encoding UTF8 | ConvertFrom-Json
        $n = @($d.rows).Count
        $total += $n
        if (-not $bySec.ContainsKey($p.section)) { $bySec[$p.section] = New-Object System.Collections.ArrayList; [void]$order.Add($p.section) }
        $url = if ($fl -eq 'site') { "$($p.file).html" } else { $navMap[$p.id] }
        [void]$bySec[$p.section].Add([ordered]@{ label = $p.navLabel; title = $p.title; blurb = $p.blurb; n = $n; url = $url; icon = $p.favicon })
    }
    $secHtml = ''
    $i = 0
    foreach ($s in $order) {
        $cards = ''
        foreach ($e in $bySec[$s]) {
            $link = if ([string]::IsNullOrWhiteSpace($e.url)) { '' } else { $e.url }
            $open = if ($link) { '<a class="hub-card" href="' + $link + '">' } else { '<div class="hub-card unlinked">' }
            $close = if ($link) { '</a>' } else { '</div>' }
            $cards += $open +
                '<span class="hub-icon">' + $e.icon + '</span>' +
                '<span class="hub-body"><span class="hub-title">' + $e.title + '</span>' +
                '<span class="hub-blurb">' + $e.blurb + '</span></span>' +
                '<span class="hub-n">' + $e.n + '</span>' + $close
        }
        $secHtml += '<section class="hub-sec sec-' + ($i % 3) + '"><h2>' + $s + '</h2><div class="hub-grid">' + $cards + '</div></section>'
        $i++
    }
    $counts = '<div class="count-item"><span class="n">' + $pages.Count + '</span><span class="l">Pages</span></div>' +
              '<div class="count-item"><span class="n">' + $total + '</span><span class="l">Entries</span></div>' +
              '<div class="count-item"><span class="n">' + $order.Count + '</span><span class="l">Sections</span></div>'
    $hubBody = $hub.Replace('__SECTIONS__', $secHtml).Replace('__COUNTS__', $counts)
    $hubJs = "  const NAV = $navJson;`n" + @'
  if (NAV && NAV.length) {
    const navEl = document.getElementById("nav");
    navEl.hidden = false;
    navEl.innerHTML = NAV.map(s =>
      '<div class="nav-sec"><span class="nav-lbl">' + s.label + '</span><div class="nav-links">'
      + s.pages.map(p => p.id === "index"
        ? '<span class="here" aria-current="page">' + p.label + '</span>'
        : '<a href="' + p.url + '">' + p.label + '</a>').join("")
      + '</div></div>').join("");
  }
'@
    $stem = if ($fl -eq 'site') { 'index' } else { 'index' }
    Write-Page $fl $stem 'CTE2 Compendium' 'A player reference for the Craft to Exile 2 modpack.' ($hubBody + "`n<script>`n" + $hubJs + "`n</script>`n")
    Write-Host ("  {0,-14} {1,-28} {2,8} bytes" -f 'index', 'CTE2 Compendium', (Get-Item (Join-Path $OUT "$fl\$stem.html")).Length)
}

Write-Host ""
Write-Host "done -> $OUT"
