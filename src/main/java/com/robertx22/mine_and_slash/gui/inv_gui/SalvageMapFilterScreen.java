package com.robertx22.mine_and_slash.gui.inv_gui;

import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.inv_gui.actions.auto_salvage.ToggleMapLayoutSalvage;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.InvGuiPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// every registered map layout, each either kept or crossed off. a crossed off layout is salvaged at
// any rarity, so this is a flat list of toggles rather than the rarity matrix the gear pages use.
//
// not an InvGuiScreen: there is no per layout art anywhere in the mod (even the atlas map draws all
// its nodes with one shared icon and labels them underneath), so 18 identical tinted 18x18 cells
// would be unreadable. named text rows instead.
public class SalvageMapFilterScreen extends BaseScreen {

    // this page is text rows rather than the icon grid, so it gets its own panel art
    static ResourceLocation TEX = SlashRef.guiId("inv_gui/background_map_salvage");

    static int COLS = 2;
    static int ROWS = 6;
    static int PER_PAGE = COLS * ROWS;

    static int ROW_X = 79;
    static int ROW_Y = 16;
    static int ROW_SPACING = 18;
    static int GUTTER = 3; // px between the two columns

    static int NAV_SIZE_X = 55;
    static int NAV_SIZE_Y = 16;

    int page;

    List<Dungeon> shown = new ArrayList<>();
    List<Button> buttons = new ArrayList<>();
    // the state each label was built from, so render only rebuilds the ones that actually moved
    List<Boolean> shownStates = new ArrayList<>();
    // built once in init: layouts() sorts and translates the whole registry, too much for a render loop
    Component title = Component.empty();

    public SalvageMapFilterScreen(int page) {
        super(177, 136);
        this.page = page;
    }

    // dungeon_realm generates the layout name keys, so a datapacked layout may have none.
    // fall back to the raw id, which is still something a player can act on
    public static MutableComponent layoutName(Dungeon layout) {
        String key = DungeonWords.MapGUID(layout.GUID());

        if (Language.getInstance().has(key)) {
            return Component.translatable(key);
        }
        return Component.literal(layout.GUID());
    }

    public static List<Dungeon> layouts() {
        var list = DungeonDatabase.Dungeons().getList();
        list.sort(Comparator.comparing(x -> layoutName(x).getString()));
        return list;
    }

    @Override
    protected void init() {
        super.init();

        shown.clear();
        buttons.clear();
        shownStates.clear();

        var all = layouts();
        int from = page * PER_PAGE;

        // centre the whole block, so the outer margins stay even if the constants above move
        int margin = (sizeX - (COLS * ROW_X + (COLS - 1) * GUTTER)) / 2;

        for (int i = from; i < Math.min(from + PER_PAGE, all.size()); i++) {
            Dungeon layout = all.get(i);
            int slot = i - from;

            int x = guiLeft + margin + (slot % COLS) * (ROW_X + GUTTER);
            int y = guiTop + 17 + (slot / COLS) * ROW_SPACING;

            Button b = Button.builder(layoutName(layout), p -> Packets.sendToServer(new InvGuiPacket(new GuiItemData(new ToggleMapLayoutSalvage(layout)))))
                    .bounds(x, y, ROW_X, ROW_Y)
                    .build();

            shown.add(layout);
            buttons.add(publicAddButton(b));
            shownStates.add(null); // nothing built yet, so the first render fills every label in
        }

        refreshLabels();

        int navY = guiTop + sizeY + 2;
        int pages = Math.max(1, (int) Math.ceil(all.size() / (double) PER_PAGE));

        title = pages > 1
                ? Component.empty().append(Words.MapLayouts.locName()).append(Component.literal(" (" + (page + 1) + "/" + pages + ")"))
                : Words.MapLayouts.locName();

        // this is a sub screen of the salvage config, so the corner arrow returns there, not to the hub
        addBackButton(Words.Salvaging.locName(), () -> ClientOnly.setScreen(new SalvageConfigScreen()));

        // paging only - the corner arrow is the way back now, so Prev/Next centre as a pair rather
        // than flanking a Back button
        if (page > 0) {
            publicAddButton(Button.builder(Words.SalvagePrevPage.locName(), b -> ClientOnly.setScreen(new SalvageMapFilterScreen(page - 1)))
                    .bounds(guiLeft + sizeX / 2 - NAV_SIZE_X - 2, navY, NAV_SIZE_X, NAV_SIZE_Y)
                    .build());
        }
        if (page < pages - 1) {
            publicAddButton(Button.builder(Words.SalvageNextPage.locName(), b -> ClientOnly.setScreen(new SalvageMapFilterScreen(page + 1)))
                    .bounds(guiLeft + sizeX / 2 + 2, navY, NAV_SIZE_X, NAV_SIZE_Y)
                    .build());
        }
    }

    private Component label(Dungeon layout, boolean filtered) {
        return filtered
                ? layoutName(layout).withStyle(ChatFormatting.RED, ChatFormatting.STRIKETHROUGH)
                : layoutName(layout).withStyle(ChatFormatting.GREEN);
    }

    private Component tooltip(Dungeon layout, boolean filtered) {
        Component state = filtered
                ? Gui.SALVAGE_MAP_FILTERED.locName(layoutName(layout)).withStyle(ChatFormatting.RED)
                : Gui.SALVAGE_MAP_KEEP.locName(layoutName(layout)).withStyle(ChatFormatting.GREEN);

        return Component.empty()
                .append(state)
                .append(Component.literal("\n"))
                .append(Gui.SALVAGE_MAP_UBER_NOTE.locName().withStyle(ChatFormatting.GRAY))
                .append(Component.literal("\n"))
                .append(Gui.SALVAGE_MAP_TOGGLE_TIP.locName().withStyle(ChatFormatting.DARK_GRAY));
    }

    // a click only takes effect once the server syncs back, so poll instead of assuming it landed.
    // one capability lookup, then only the buttons whose state actually moved get rebuilt
    private void refreshLabels() {
        var salvage = Load.player(mc.player).config.salvage;

        for (int i = 0; i < buttons.size(); i++) {
            Dungeon layout = shown.get(i);
            boolean filtered = salvage.isMapLayoutFiltered(layout.GUID());

            if (Boolean.valueOf(filtered).equals(shownStates.get(i))) {
                continue;
            }
            shownStates.set(i, filtered);
            buttons.get(i).setMessage(label(layout, filtered));
            buttons.get(i).setTooltip(Tooltip.create(tooltip(layout, filtered)));
        }
    }

    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {
        refreshLabels();

        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        gui.blit(TEX, guiLeft, guiTop, 0, 0, sizeX, sizeY);

        super.render(gui, x, y, ticks);

        // the panel has no header space, so the title sits above it
        gui.drawString(mc.font, title, guiLeft + sizeX / 2 - mc.font.width(title) / 2, guiTop - 12, ChatFormatting.YELLOW.getColor(), true);
    }
}
