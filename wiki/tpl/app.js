  const PAGE = __CONFIG__;
  const NAV = __NAV__;
  const DATA = __DATA__;
  const LABELS = __LABELS__;
  const KEYMETA = __KEYMETA__;

  // label + category for every GearSlot tag that appears anywhere in the datapack
  const TAG_META = {
    // weapon
    weapon_family: { label: "Any Weapon", cat: "weapon" },
    mage_weapon: { label: "Caster Weapon", cat: "weapon" },
    melee_weapon: { label: "Melee Weapon", cat: "weapon" },
    ranged_weapon: { label: "Ranged Weapon", cat: "weapon" },
    ranger_casting_weapon: { label: "Ranger Casting Weapon", cat: "weapon" },
    two_handed: { label: "Two-Handed", cat: "weapon" },
    axe: { label: "Axe", cat: "weapon" },
    bow: { label: "Bow", cat: "weapon" },
    crossbow: { label: "Crossbow", cat: "weapon" },
    dagger: { label: "Dagger", cat: "weapon" },
    gauntlet: { label: "Gauntlet", cat: "weapon" },
    greatsword: { label: "Greatsword", cat: "weapon" },
    hammer: { label: "Hammer", cat: "weapon" },
    scythe: { label: "Scythe", cat: "weapon" },
    spear: { label: "Spear", cat: "weapon" },
    staff: { label: "Staff", cat: "weapon" },
    sword: { label: "Sword", cat: "weapon" },
    trident: { label: "Trident", cat: "weapon" },

    // armor
    armor_family: { label: "Any Armor", cat: "armor" },
    boots: { label: "Boots", cat: "armor" },
    chest: { label: "Chest", cat: "armor" },
    head: { label: "Head", cat: "armor" },
    helmet: { label: "Helmet", cat: "armor" },
    pants: { label: "Pants", cat: "armor" },
    cloth: { label: "Cloth", cat: "armor" },
    cloth_pants: { label: "Cloth Pants", cat: "armor" },
    cloth_helmet: { label: "Cloth Helmet", cat: "armor" },
    cloth_boots: { label: "Cloth Boots", cat: "armor" },
    cloth_chest: { label: "Cloth Chest", cat: "armor" },
    leather: { label: "Leather", cat: "armor" },
    leather_helmet: { label: "Leather Helmet", cat: "armor" },
    leather_boots: { label: "Leather Boots", cat: "armor" },
    leather_chest: { label: "Leather Chest", cat: "armor" },
    leather_pants: { label: "Leather Pants", cat: "armor" },
    plate: { label: "Plate", cat: "armor" },
    plate_helmet: { label: "Plate Helmet", cat: "armor" },
    plate_boots: { label: "Plate Boots", cat: "armor" },
    plate_chest: { label: "Plate Chest", cat: "armor" },
    plate_pants: { label: "Plate Pants", cat: "armor" },
    brigandine: { label: "Brigandine", cat: "armor" },
    chainmail: { label: "Chainmail", cat: "armor" },
    vest: { label: "Vest", cat: "armor" },
    elytra: { label: "Elytra", cat: "armor" },
    armor_stat: { label: "Armor-Scaling", cat: "armor" },
    armor_stat_half: { label: "Armor-Scaling (Hybrid)", cat: "armor" },
    dodge_stat: { label: "Dodge-Scaling", cat: "armor" },
    dodge_stat_half: { label: "Dodge-Scaling (Hybrid)", cat: "armor" },
    magic_shield_stat: { label: "Shield-Scaling", cat: "armor" },
    magic_shield_stat_half: { label: "Shield-Scaling (Hybrid)", cat: "armor" },
    dual_stat: { label: "Hybrid Defense", cat: "armor" },

    // jewelry
    jewelry_family: { label: "Any Jewelry", cat: "jewelry" },
    necklace: { label: "Necklace", cat: "jewelry" },
    ring: { label: "Ring", cat: "jewelry" },

    // offhand
    offhand_family: { label: "Any Offhand", cat: "offhand" },
    shield: { label: "Shield", cat: "offhand" },
    tome: { label: "Tome", cat: "offhand" },
    totem: { label: "Totem", cat: "offhand" },

    // jewels
    any_jewel: { label: "Any Jewel", cat: "jewel" },
    jewel_str: { label: "Strength Jewel", cat: "jewel" },
    jewel_dex: { label: "Dexterity Jewel", cat: "jewel" },
    jewel_int: { label: "Intelligence Jewel", cat: "jewel" },
    crafted_jewel_unique: { label: "Crafted Unique Jewel", cat: "jewel" },

    // profession tools
    tool: { label: "Any Tool", cat: "tool" },
    mining_tool: { label: "Mining Tool", cat: "tool" },
    farming_tool: { label: "Farming Tool", cat: "tool" },
    husbandry_tool: { label: "Husbandry Tool", cat: "tool" },
    fishing_tool: { label: "Fishing Tool", cat: "tool" },

    // attribute-scaling gear
    strength: { label: "Strength Gear", cat: "attribute" },
    dexterity: { label: "Dexterity Gear", cat: "attribute" },
    intelligence: { label: "Intelligence Gear", cat: "attribute" },

    enchantment: { label: "Enchantment", cat: "other" },

    // crafting style pools - which guaranteed-tag orb can force this affix
    might: { label: "Might", cat: "style" },
    finesse: { label: "Finesse", cat: "style" },
    ingenuity: { label: "Ingenuity", cat: "style" },
  };

  const CAT_TITLES = {
    style: "Style", weapon: "Weapon", armor: "Armor", jewelry: "Jewelry", offhand: "Offhand",
    jewel: "Jewel", tool: "Tool", attribute: "Attribute", aura: "Augment", league: "Source",
    profession: "Profession", tier: "Tier", scope: "Scope", rarity: "Rarity", group: "Group",
    affected: "Affects", element: "Element", effect: "Kind", other: "Other",
  };
  const CAT_ORDER = ["style", "league", "profession", "effect", "affected", "group", "scope",
    "weapon", "armor", "jewelry", "offhand", "jewel", "tool", "attribute", "element",
    "rarity", "tier", "aura", "other"];

  const IGNORED = new Set(PAGE.ignore || []);
  const STYLE = new Set(["might", "finesse", "ingenuity"]);
  const HAS_STYLE = DATA.some(r => arr(r.tags).some(t => STYLE.has(t)));

  // ---- helpers ----

  // The generator should always hand us arrays, but a wiki rebuilt from changing data
  // should degrade one row rather than blank a page, so every collection read goes through this.
  function arr(x) { return Array.isArray(x) ? x : (x == null ? [] : [x]); }

  function titleCase(id) {
    return String(id).split(/[_:/]/).filter(Boolean)
      .map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(" ");
  }
  function esc(s) {
    return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }
  function keyInfo(k) {
    if (KEYMETA[k]) return KEYMETA[k];
    if (TAG_META[k]) return { label: TAG_META[k].label, cat: TAG_META[k].cat, dim: STYLE.has(k) ? "style" : "slot" };
    return { label: titleCase(k), cat: "other", dim: "slot" };
  }
  function tagLabel(t) { return TAG_META[t] ? TAG_META[t].label : keyInfo(t).label; }
  function tagCat(t) { return TAG_META[t] ? TAG_META[t].cat : keyInfo(t).cat; }

  function fmtNum(n) { return Number(Number(n).toFixed(3)).toString(); }
  function fmtRange(min, max, unit) {
    const u = unit || "";
    if (min === max) return fmtNum(min) + u;
    return fmtNum(min) + u + " – " + fmtNum(max) + u;
  }

  // ---- stat rendering ----

  function statLines(input) {
    const stats = arr(input);
    if (!stats.length) return '<span class="muted">none</span>';
    return stats.map(s => {
      const meta = LABELS[s.stat] || { t: titleCase(s.stat), k: "plain" };
      const pct = s.type !== "FLAT";
      const range = fmtRange(s.min, s.max, pct ? "%" : "");

      if (meta.k === "tmpl") {
        return '<div class="stat-line sentence">'
          + esc(meta.t).replace("[VAL1]", '<span class="v">' + range + '</span>') + '</div>';
      }
      if (meta.k === "flag") return '<div class="stat-line sentence">' + esc(meta.t) + '</div>';
      if (meta.k === "frag") {
        return '<div class="stat-line sentence"><span class="v">' + range + '</span> ' + esc(meta.t) + '</div>';
      }
      const badge = s.type === "MORE" ? '<span class="badge more">more</span>'
        : s.type === "PERCENT" ? '<span class="badge">%</span>'
        : '<span class="badge">flat</span>';
      return '<div class="stat-line"><span class="stat-label">' + esc(meta.t) + badge
        + '</span><span class="stat-value">' + range + '</span></div>';
    }).join("");
  }

  // ---- cell renderers, one per column type ----

  const CELL = {
    name: (r) => '<td class="name-cell"><span class="name">' + esc(r.name) + '</span>'
      + '<span class="guid">' + esc(r.id) + '</span></td>',

    chips: (r) => {
      const inc = arr(r.tags).filter(t => !IGNORED.has(t) && !STYLE.has(t))
        .map(t => '<span class="chip cat-' + tagCat(t) + '">' + esc(tagLabel(t)) + '</span>').join("");
      const out = arr(r.excl).filter(t => !IGNORED.has(t))
        .map(t => '<span class="chip chip-excl cat-other"><span class="x">not</span> '
          + esc(tagLabel(t)) + '</span>').join("");
      const body = (inc || out) ? inc + out
        : '<span class="chip cat-other">' + esc(PAGE.noTagsLabel || "Any") + '</span>';
      return '<td><div class="chips">' + body + '</div></td>';
    },

    style: (r) => {
      const st = arr(r.tags).filter(t => STYLE.has(t));
      const body = st.length
        ? st.map(t => '<span class="chip cat-style">' + esc(tagLabel(t)) + '</span>').join("")
        : '<span class="chip cat-other">None</span>';
      return '<td class="style-cell"><div class="chips">' + body + '</div></td>';
    },

    stats: (r) => '<td><div class="stat-list">' + statLines(r.stats) + '</div></td>',

    groups: (r) => {
      const gs = arr(r.groups).filter(g => arr(g.stats).length);
      if (!gs.length) return '<td><span class="muted">none</span></td>';
      return '<td><div class="stat-list">' + gs.map(g =>
        '<div class="grp"><div class="grp-h">' + esc(g.label) + '</div>' + statLines(arr(g.stats)) + '</div>'
      ).join("") + '</div></td>';
    },

    facts: (r) => {
      const fs = arr(r.facts);
      if (!fs.length) return '<td><span class="muted">none</span></td>';
      return '<td><dl class="facts">' + fs.map(x =>
        '<dt>' + esc(x.k) + '</dt><dd>' + esc(x.v) + '</dd>').join("") + '</dl></td>';
    },

    items: (r) => {
      const it = arr(r.items);
      if (!it.length) return '<td><span class="muted">none</span></td>';
      return '<td><ul class="items">' + it.map(x =>
        '<li>' + (x.num ? '<span class="n">' + x.num + '×</span> ' : '')
        + '<code>' + esc(x.id) + '</code>'
        + (x.note ? ' <span class="note">' + esc(x.note) + '</span>' : '')
        + '</li>').join("") + '</ul></td>';
    },

    text: (r, col) => {
      const v = r.f ? r.f[col.field] : null;
      if (v == null || v === "") return '<td><span class="muted">none</span></td>';
      const cn = (col.mono ? 'mono ' : '') + (col.wrap ? 'wrap-cell ' : '');
      return '<td' + (cn ? ' class="' + cn.trim() + '"' : '') + '>' + esc(v) + '</td>';
    },

    num: (r, col) => {
      const v = r.f ? r.f[col.field] : null;
      if (v == null) return '<td class="num"><span class="muted">none</span></td>';
      return '<td class="num">' + esc(v) + (col.unit ? esc(col.unit) : "") + '</td>';
    },

    weight: (r, col) => {
      const v = r.f ? r.f[col.field || "weight"] : null;
      if (v == null) return '<td class="weight-cell"><span class="muted">none</span></td>';
      if (r.f.noRoll) {
        return '<td class="weight-cell"><span class="weight-num">0</span>'
          + '<div class="noroll">not randomly generated</div></td>';
      }
      const max = SEC_MAX[r.cat] || 1;
      return '<td class="weight-cell"><span class="weight-num">' + esc(v) + '</span>'
        + '<div class="weight-bar-track"><div class="weight-bar-fill" style="width:'
        + (v / max * 100).toFixed(0) + '%"></div></div></td>';
    },
  };

  // weight bars scale within each section: the pools are rolled separately
  const SEC_MAX = {};
  DATA.forEach(r => {
    const w = r.f && r.f.weight;
    if (typeof w === "number" && w > 0) SEC_MAX[r.cat] = Math.max(SEC_MAX[r.cat] || 0, w);
  });

  // ---- filtering keys ----

  function rowKeys(r) { return arr(r.keys).filter(k => !IGNORED.has(k)); }

  function searchText(r) {
    const words = [r.name, r.id];
    rowKeys(r).forEach(k => words.push(keyInfo(k).label));
    arr(r.tags).forEach(t => words.push(tagLabel(t)));
    arr(r.excl).forEach(t => words.push("not " + tagLabel(t)));
    arr(r.facts).forEach(x => words.push(x.k, x.v));
    arr(r.items).forEach(x => words.push(x.id, x.note));
    if (r.f) { for (const k in r.f) { const v = r.f[k]; if (typeof v === "string") words.push(v); } }
    arr(r.stats).forEach(s => words.push((LABELS[s.stat] || {}).t || s.stat, s.stat));
    arr(r.groups).forEach(g => arr(g.stats).forEach(s => words.push((LABELS[s.stat] || {}).t || s.stat, s.stat)));
    return words.join(" ").toLowerCase();
  }

  // ---- sections + columns ----

  // a section may override the page columns - sets, runewords and dimensions do not
  // describe themselves the same way the rows above them do
  const sections = arr(PAGE.sections).map((sec, i) => ({
    cat: sec.cat, title: sec.title, noun: sec.noun, countLabel: sec.countLabel, i: i,
    columns: arr(sec.columns && sec.columns.length ? sec.columns : PAGE.columns),
    rows: DATA.filter(r => r.cat === sec.cat),
    sort: { key: "name", dir: 1 },
  }));
  const COLS_BY_CAT = {};
  sections.forEach(s => { COLS_BY_CAT[s.cat] = s.columns; });

  function renderRow(r) {
    const cols = COLS_BY_CAT[r.cat] || arr(PAGE.columns);
    try {
      const cells = cols.map(c => (CELL[c.type] || CELL.text)(r, c)).join("");
      return '<tr data-search="' + esc(searchText(r)) + '"'
        + ' data-keys="' + esc(rowKeys(r).join("|")) + '"'
        + ' data-excl="' + esc(arr(r.excl).join("|")) + '">' + cells + '</tr>';
    } catch (e) {
      // a malformed entry costs one row, never the whole table
      return '<tr data-search="' + esc(r.id) + '" data-keys="" data-excl="">'
        + '<td colspan="' + cols.length + '" class="row-error">'
        + esc(r.id) + ' could not be rendered: ' + esc(e && e.message) + '</td></tr>';
    }
  }

  function headCells(sec) {
    const cat = sec.cat;
    return arr(sec.columns).map(c => {
      const sortKey = c.sort === false ? null
        : c.type === "name" ? "name"
        : (c.type === "weight" || c.type === "num") ? (c.field || "weight") : null;
      if (!sortKey) return '<th>' + esc(c.head) + '</th>';
      return '<th data-sort="' + sortKey + '" data-group="' + cat + '">' + esc(c.head)
        + ' <span class="arrow"></span></th>';
    }).join("");
  }

  document.getElementById("sections").innerHTML = sections.map(sec =>
    '<section class="group sec-' + (sec.i % 3) + '">'
    + '<div class="group-head"><h2>' + esc(sec.title) + '</h2>'
    + '<span class="sub" id="sub-' + sec.cat + '">' + sec.rows.length + ' entries</span></div>'
    + '<div class="table-scroll"><table id="table-' + sec.cat + '"><thead><tr>'
    + headCells(sec)
    + '</tr></thead><tbody></tbody></table>'
    + '<div class="empty-state">No ' + esc(sec.noun) + ' match the current filters.</div>'
    + '</div></section>'
  ).join("");

  document.getElementById("counts").innerHTML =
    sections.map(sec => '<div class="count-item sec-' + (sec.i % 3) + '"><span class="swatch"></span>'
      + '<span class="n">' + sec.rows.length + '</span>'
      + '<span class="l">' + esc(sec.countLabel || sec.title) + '</span></div>').join("")
    + (sections.length > 1 ? '<div class="count-item"><span class="n">' + DATA.length
      + '</span><span class="l">Total</span></div>' : "");

  // ---- nav ----

  if (NAV && NAV.length) {
    const navEl = document.getElementById("nav");
    navEl.hidden = false;
    navEl.innerHTML = arr(NAV).map(section =>
      '<div class="nav-sec"><span class="nav-lbl">' + esc(section.label) + '</span>'
      + '<div class="nav-links">' + arr(section.pages).map(p => p.id === PAGE.id
        ? '<span class="here" aria-current="page">' + esc(p.label) + '</span>'
        : '<a href="' + esc(p.url) + '">' + esc(p.label) + '</a>').join("")
      + '</div></div>').join("");
  }

  // ---- legend ----

  document.getElementById("legend").innerHTML = arr(PAGE.legend)
    .map(l => '<div class="row"><span class="k' + (l.cat ? " cat-" + l.cat : "") + '">'
      + esc(l.k) + '</span><span>' + l.html + '</span></div>').join("");

  // ---- filter panel, built from the keys this page actually uses ----

  const keysByCat = {};
  DATA.forEach(r => rowKeys(r).forEach(k => {
    const c = keyInfo(k).cat;
    if (!keysByCat[c]) keysByCat[c] = new Set();
    keysByCat[c].add(k);
  }));

  const activeCats = CAT_ORDER.filter(c => keysByCat[c] && keysByCat[c].size);
  const sortedKeys = {};
  activeCats.forEach(c => {
    sortedKeys[c] = [...keysByCat[c]].sort((a, b) => {
      const la = keyInfo(a).label, lb = keyInfo(b).label;
      const aAny = la.indexOf("Any ") === 0, bAny = lb.indexOf("Any ") === 0;
      if (aAny !== bAny) return aAny ? -1 : 1;
      return la.localeCompare(lb, undefined, { numeric: true });
    });
  });

  const selected = new Set();
  const filterGroupsEl = document.getElementById("filterGroups");
  const filterPanelEl = document.querySelector(".filter-panel");

  if (!activeCats.length) {
    filterPanelEl.hidden = true;
  } else {
    filterGroupsEl.innerHTML = activeCats.map(cat =>
      '<div class="filter-group cat-' + cat + '" data-cat="' + cat + '">'
      + '<button class="group-toggle" data-cat-toggle="' + cat + '">'
      + esc(CAT_TITLES[cat] || titleCase(cat)) + '</button>'
      + '<div class="chip-row">'
      + sortedKeys[cat].map(k => '<button class="tag-btn" data-key="' + esc(k) + '">'
        + esc(keyInfo(k).label) + '</button>').join("")
      + '</div></div>').join("");
  }

  function updateClearButton() {
    document.getElementById("clearFilters").classList.toggle("active", selected.size > 0);
  }

  filterGroupsEl.querySelectorAll(".tag-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      const k = btn.getAttribute("data-key");
      if (selected.has(k)) { selected.delete(k); btn.classList.remove("active"); }
      else { selected.add(k); btn.classList.add("active"); }
      updateClearButton();
      applyFilter();
    });
  });

  filterGroupsEl.querySelectorAll("[data-cat-toggle]").forEach(btn => {
    btn.addEventListener("click", () => {
      const cat = btn.getAttribute("data-cat-toggle");
      const keys = sortedKeys[cat];
      const all = keys.every(k => selected.has(k));
      keys.forEach(k => { if (all) selected.delete(k); else selected.add(k); });
      filterGroupsEl.querySelector('.filter-group[data-cat="' + cat + '"]')
        .querySelectorAll(".tag-btn")
        .forEach(b => b.classList.toggle("active", selected.has(b.getAttribute("data-key"))));
      updateClearButton();
      applyFilter();
    });
  });

  document.getElementById("clearFilters").addEventListener("click", () => {
    selected.clear();
    filterGroupsEl.querySelectorAll(".tag-btn.active").forEach(b => b.classList.remove("active"));
    updateClearButton();
    applyFilter();
  });

  // ---- sorting ----

  function sortRows(rows, key, dir) {
    return [...rows].sort((a, b) => {
      let av, bv;
      if (key === "name") { av = a.name.toLowerCase(); bv = b.name.toLowerCase(); }
      else { av = (a.f && a.f[key] != null) ? a.f[key] : -Infinity; bv = (b.f && b.f[key] != null) ? b.f[key] : -Infinity; }
      if (av < bv) return -dir;
      if (av > bv) return dir;
      return a.name.localeCompare(b.name);
    });
  }

  function redraw() {
    sections.forEach(sec => {
      document.querySelector("#table-" + sec.cat + " tbody").innerHTML =
        sortRows(sec.rows, sec.sort.key, sec.sort.dir).map(renderRow).join("");
    });
    applyFilter();
  }

  document.querySelectorAll("th[data-sort]").forEach(th => {
    th.addEventListener("click", () => {
      const key = th.getAttribute("data-sort");
      const group = th.getAttribute("data-group");
      const sec = sections.find(s => s.cat === group);
      if (!sec) return;
      if (sec.sort.key === key) sec.sort.dir *= -1; else { sec.sort.key = key; sec.sort.dir = 1; }
      document.querySelectorAll('th[data-group="' + group + '"] .arrow').forEach(a => a.textContent = "");
      th.querySelector(".arrow").textContent = sec.sort.dir === 1 ? "▲" : "▼";
      redraw();
    });
  });

  // ---- filtering ----
  //
  // Keys of the same dimension are alternatives (OR); dimensions combine with AND, so
  // "Might + Sword" means Might affixes that can land on a sword, not the union.

  function applyFilter() {
    const q = document.getElementById("search").value.trim().toLowerCase();
    let shown = 0;

    const byDim = {};
    selected.forEach(k => {
      const d = keyInfo(k).dim || "slot";
      (byDim[d] = byDim[d] || []).push(k);
    });
    const dims = Object.keys(byDim);

    function pass(tr) {
      if (q && tr.getAttribute("data-search").indexOf(q) < 0) return false;
      if (!dims.length) return true;
      const keys = tr.getAttribute("data-keys").split("|");
      const excl = (tr.getAttribute("data-excl") || "").split("|");
      for (let i = 0; i < dims.length; i++) {
        const want = byDim[dims[i]];
        let hit = false;
        for (let j = 0; j < want.length; j++) {
          if (keys.indexOf(want[j]) >= 0 && excl.indexOf(want[j]) < 0) { hit = true; break; }
        }
        if (!hit) return false;
      }
      return true;
    }

    sections.forEach(sec => {
      const table = document.querySelector("#table-" + sec.cat);
      let n = 0;
      table.querySelectorAll("tbody tr").forEach(tr => {
        const ok = pass(tr);
        tr.style.display = ok ? "" : "none";
        if (ok) n++;
      });
      shown += n;
      document.getElementById("sub-" + sec.cat).textContent = n + " entries";
      const empty = table.parentElement.querySelector(".empty-state");
      const blank = (q || selected.size) && n === 0;
      empty.style.display = blank ? "block" : "none";
      table.style.display = blank ? "none" : "table";
    });

    document.getElementById("searchHint").textContent =
      (q || selected.size) ? shown + " of " + DATA.length + " match" : "";
  }

  document.getElementById("search").addEventListener("input", applyFilter);

  redraw();
