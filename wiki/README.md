# CTE2 Compendium

A generated reference for the **Craft to Exile 2** modpack, built straight from the datapack the
server runs: the Mine and Slash defaults under `src/generated/resources/data/mmorpg`, with the pack's
own overrides from `config/openloader/data/cte_mns/data/mmorpg` layered on top. Where both define an
entry the pack wins, which is the same precedence the game applies at load, so a page shows what the game
actually rolls, not what the mod ships.

18 pages: an index plus 17 subject pages across Affixes, Gear, Crafting, Skills, World and Reference.

## Rebuilding

Windows PowerShell 5.1, no other tooling needed.

```powershell
cd wiki
.\build-data.ps1              # read both datapacks + lang -> data\*.json
.\build.ps1                   # data + templates -> out\artifact and out\site
.\build.ps1 -Flavour site     # just the static site
```

`build-data.ps1` is the only script that touches the game data. Rerun it after any datapack or lang
change; rerun `build.ps1` after any template, config or copy change.

## Layout

| Path | What it is |
|---|---|
| `build-data.ps1` | Per-registry extraction. One block per page; add a registry here. |
| `build.ps1` | Assembles pages in both flavours and builds the index. |
| `lib/common.ps1` | Datapack merging, lang resolution, markup stripping, stat-label classification. |
| `tpl/base.css` | The whole stylesheet, light and dark. |
| `tpl/app.js` | The page engine: columns, filters, sort, search, nav. |
| `tpl/body.html`, `tpl/hub.html` | Page and index skeletons. |
| `config/pages.json` | Every page's copy, columns, sections and legend. |
| `config/nav.json` | Page id → published artifact URL. Only the artifact flavour uses it. |
| `data/`, `out/` | Generated. Gitignored. |

**Keep the `.ps1` files ASCII-only.** PowerShell 5.1 reads scripts as ANSI, so a stray `·` or an em
dash in a script literal becomes mojibake in the output. The templates and JSON are read explicitly as
UTF-8 and can hold anything.

## Adding a page

1. Add an extraction block in `build-data.ps1` ending in `Save-Page '<id>' @($rows)`.
2. Add an entry to `config/pages.json` with its `section`, `columns` and `sections`.
3. Rebuild. The index and the nav pick it up automatically from `pages.json` order.

Row fields the engine understands: `cat`, `id`, `name`, `keys[]`, `tags[]`, `excl[]`, `f{}`,
`facts[]`, `stats[]`, `groups[]`, `items[]`. Column types: `name`, `chips`, `style`, `stats`,
`groups`, `facts`, `items`, `text`, `num`, `weight`. A section may override `columns` when its rows
describe themselves differently. Sets, runewords and dimensions all do.

Filter keys carry a `dim`. Keys of the same dimension are alternatives (OR); different dimensions
combine with AND, so *Might + Sword* means Might affixes that can land on a sword.

## Hosting the site

`out/site/` is a complete static site: 18 standalone HTML documents, `index.html` at the root, no
build step and no runtime dependency beyond the Google Fonts link each page carries. Nav links are
relative, so the folder works unchanged at a domain root, in a subdirectory, or opened straight off
disk.

```powershell
.\build.ps1 -Flavour site      # writes out\site\, clearing the previous build first
```

**`out/` is gitignored, so committing this folder publishes nothing.** A commit here carries the
generator, not the pages. Hosting is a separate, deliberate step: copy `out\site\` into whatever
repo or bucket serves it, and rerun the build plus the copy after every datapack change.

**Do not copy it into this repo's `docs/`.** That folder is already a
[Sinytra wiki](https://moddedmc.wiki) (`docs/sinytra-wiki.json`, `_meta.json`, and the hand-written
`.mdx` guides under `equipment/`, `mechanics/`, `professions/` and friends) published as
`mine-and-slash-reloaded`. Dropping HTML in there collides with it.

If you ever do want these pages inside that wiki rather than beside it, the generator would need an
`mdx` flavour writing into `docs/` with `_meta.json` entries to match. That trades away search,
filtering and column sorting, since those are the inline JavaScript, so it is a real decision rather
than a format switch.

## What is left out

- Entries flagged `hide_from_wiki` in the data. These are older duplicates the pack keeps beside the live
  entry, plus three golem buffs.
- Filler ids (`empty`, `unknown`, `none`).
- On the affix pages only, entries with `weight: 0`, since there weight 0 means the affix can never
  appear. Everywhere else a weight-0 entry is kept and marked *not randomly generated*, because it
  usually still exists in game through crafting or a scripted drop.
