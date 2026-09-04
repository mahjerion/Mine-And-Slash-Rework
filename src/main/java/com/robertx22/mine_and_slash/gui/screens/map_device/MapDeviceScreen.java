package com.robertx22.mine_and_slash.gui.screens.map_device;

import com.robertx22.addons.map_device.MapDeviceActionPacket;
import com.robertx22.addons.map_device.MapDeviceClientState;
import com.robertx22.dungeon_realm.client.DungeonStatsOverlay;
import com.robertx22.dungeon_realm.item.DungeonItemNbt;
import com.robertx22.dungeon_realm.item.relic.RelicItemData;
import com.robertx22.dungeon_realm.item.relic.RelicSlotUtil;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.database.relic.stat.RelicStat;
import com.robertx22.library_of_exile.localization.TranslationType;
import com.robertx22.mine_and_slash.saveclasses.item_classes.tooltips.TooltipStatInfo;
import net.minecraft.network.chat.MutableComponent;
import com.robertx22.library_of_exile.dimension.device.IMapDeviceBlockEntity;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.mine_and_slash.database.OptScaleExactStat;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.vanilla_mc.packets.proxies.OpenGuiWrapper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The shared map device screen: dungeon map device, harvest block and obelisk block. A small vertical
 * panel with no background fade - title, map slot, 2x2 relic grid, the Initiate/Join button and the
 * Entry Tickets line (dungeon only) - with the summed relic bonuses in a panel to its right and the
 * player's allocated Atlas passives in one to its left. The exit arrow and the Atlas button (dungeon
 * only) hang above the panel's corners, outside it.
 * <p>
 * Everything shown comes from the {@link MapDeviceClientState} snapshot the server sent; block entities
 * are not synced to clients. The screen asks for a fresh snapshot once a second so two players at the
 * same device stay consistent.
 */
public class MapDeviceScreen extends BaseScreen {

    private static final int RESYNC_TICKS = 20;

    private MapDeviceClientState state;
    private int builtSignature = 0;
    private int ticks = 0;

    // computed at layout time
    private int titleY, mapSlotY, relicY, actionY, ticketsY;
    private boolean showMapSlot, showTickets;

    // side panels, rebuilt with the state rather than per frame
    private final List<Component> relicLines = new ArrayList<>();
    private final List<Component> atlasLines = new ArrayList<>();

    // atlas panel scrolling. the bar rect is remembered from the last render so the mouse handlers can
    // hit-test it without redoing the layout maths
    private int atlasScroll = 0;
    private boolean draggingScroller = false;
    private int barX, barTop, barBottom;

    public MapDeviceScreen(MapDeviceClientState state) {
        super(MapDeviceGui.PANEL_W, 100);
        this.state = state;
    }

    public MapDeviceClientState getState() {
        return state;
    }

    public Component getTitleText() {
        try {
            return mc.level.getBlockState(state.pos).getBlock().getName();
        } catch (Exception e) {
            return Component.empty();
        }
    }

    /** a fresh snapshot from the server; only relays out if something actually changed */
    public void setState(MapDeviceClientState state) {
        this.state = state;
        if (signature(state) != builtSignature) {
            rebuild();
        }
    }

    private static int signature(MapDeviceClientState s) {
        int hash = 1;
        for (var stack : s.stacks) {
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
            hash = 31 * hash + (stack.getTag() == null ? 0 : stack.getTag().hashCode());
        }
        hash = 31 * hash + (s.activated ? 1 : 0);
        hash = 31 * hash + (s.hasMapSlot ? 1 : 0);
        hash = 31 * hash + (s.freeRunAvailable ? 1 : 0);
        hash = 31 * hash + (s.showTickets ? 1 : 0);
        hash = 31 * hash + (s.unlimitedTickets ? 1 : 0);
        hash = 31 * hash + s.ticketsLeft;
        hash = 31 * hash + s.ticketsMax;
        return hash;
    }

    // ------------------------------------------------------------------ layout

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    @Override
    public void tick() {
        super.tick();
        if (++ticks % RESYNC_TICKS == 0) {
            Packets.sendToServer(MapDeviceActionPacket.requestSync(state.pos));
        }
    }

    private void rebuild() {
        this.clearWidgets();
        builtSignature = signature(state);

        showMapSlot = state.hasMapSlot;
        showTickets = state.kind.showsEntryTickets && state.showTickets;

        // stack the rows, then centre the whole panel on the screen
        int y = MapDeviceGui.PAD_TOP;
        titleY = y;
        y += MapDeviceGui.TITLE_H;
        if (showMapSlot) {
            mapSlotY = y;
            y += MapDeviceGui.MAP_SLOT_TEX_SIZE + MapDeviceGui.ROW_GAP;
        }
        relicY = y;
        y += MapDeviceGui.RELIC_SLOTS_TEX_SIZE + MapDeviceGui.ROW_GAP;
        actionY = y;
        y += MapDeviceGui.BUTTON_H;
        if (state.kind.showsEntryTickets) {
            // reserved whether or not a map is in yet, so the panel doesn't grow the moment one goes in
            // and the button keeps the same room below it in both states
            y += MapDeviceGui.TICKETS_GAP;
            ticketsY = y;
            y += MapDeviceGui.TICKETS_H;
        }
        y += MapDeviceGui.PAD_BOTTOM;

        // wide enough for the tickets line ("Entry Tickets: 10 / 10" overran the old fixed width)
        sizeX = MapDeviceGui.PANEL_W;
        if (showTickets) {
            sizeX = Math.max(sizeX, mc.font.width(ticketsLine()) + MapDeviceGui.SIDE_PAD * 2);
        }
        sizeY = y;
        guiLeft = (this.width - sizeX) / 2;
        guiTop = (this.height - sizeY) / 2;

        int centerX = guiLeft + sizeX / 2;

        // the slot art is drawn in render(); the buttons are the 18px cells laid over it, one pixel in
        // from each cell origin so they cover the shadow line and the icon area, not the outer frame
        int inset = MapDeviceGui.SLOT_INSET;
        if (showMapSlot) {
            publicAddButton(new MapDeviceSlotButton(this, IMapDeviceBlockEntity.MAP_SLOT,
                    mapSlotX() + inset, guiTop + mapSlotY + inset));
        }

        for (int i = 0; i < IMapDeviceBlockEntity.RELIC_SLOTS; i++) {
            int col = i % 2;
            int row = i / 2;
            publicAddButton(new MapDeviceSlotButton(this, IMapDeviceBlockEntity.RELIC_SLOT_START + i,
                    relicGridX() + inset + col * MapDeviceGui.SLOT_SIZE,
                    guiTop + relicY + inset + row * MapDeviceGui.SLOT_SIZE));
        }

        Words label = state.wouldStartNew() || !state.activated ? Words.MAP_DEVICE_INITIATE : Words.MAP_DEVICE_JOIN;
        var action = new MapDeviceTextButton(centerX - MapDeviceGui.BUTTON_W / 2, guiTop + actionY, label.locName(), this::onActionPressed);
        action.active = state.canStartOrJoin();
        if (!action.active) {
            action.setTooltip(Tooltip.create(Words.MAP_DEVICE_NO_MAP.locName().withStyle(ChatFormatting.RED)));
        } else if (state.freeRunAvailable) {
            action.setTooltip(Tooltip.create(Words.MAP_DEVICE_FREE_RUN.locName().withStyle(ChatFormatting.GREEN)));
        }
        publicAddButton(action);

        if (state.kind.showsAtlas) {
            // above the panel's top-right corner, opposite the exit arrow. clamped like the arrow so it
            // stays on screen at gui scale 4. client side straight to the atlas; the wrapper already copes
            // with datapacks not being synced yet
            int atlasY = Math.max(2, guiTop - MapDeviceGui.BUTTON_H - MapDeviceGui.TOP_BUTTON_GAP);
            publicAddButton(new MapDeviceTextButton(guiLeft + sizeX - MapDeviceGui.BUTTON_W, atlasY,
                    Words.Atlas.locName(), OpenGuiWrapper::openAtlasMap));
        }

        rebuildRelicLines();
        rebuildAtlasLines();

        // has to be added here rather than in init(): rebuild() clears every widget and runs again from
        // setState whenever the server sends a changed snapshot. this screen has no parent, so the arrow
        // simply closes it
        addBackButton(Words.MAP_DEVICE_EXIT.locName(), () -> ClientOnly.setScreen(null));
    }

    /** left edge of the 19x19 map slot art, centred on the panel */
    private int mapSlotX() {
        return guiLeft + sizeX / 2 - MapDeviceGui.MAP_SLOT_TEX_SIZE / 2;
    }

    /** left edge of the 37x37 relic grid art, centred on the panel */
    private int relicGridX() {
        return guiLeft + sizeX / 2 - MapDeviceGui.RELIC_SLOTS_TEX_SIZE / 2;
    }

    private void onActionPressed() {
        Packets.sendToServer(MapDeviceActionPacket.startOrJoin(state.pos));
        // a start teleports you away; if the server refused it, it sends the snapshot back and this
        // screen reopens with the gate's chat message underneath
        ClientOnly.setScreen(null);
    }

    // ------------------------------------------------------------------ side panel content

    /** the summed bonuses, then one line per slotted relic with its uses left */
    private void rebuildRelicLines() {
        relicLines.clear();
        if (!state.hasAnyRelic()) {
            return;
        }
        try {
            var container = state.asContainer();
            List<RelicItemData> applying = RelicSlotUtil.loadEquippable(container, IMapDeviceBlockEntity.RELIC_SLOT_START, IMapDeviceBlockEntity.RELIC_SLOTS);
            Map<RelicStat, Float> totals = RelicSlotUtil.aggregate(applying);

            relicLines.add(Words.MAP_DEVICE_RELIC_STATS.locName().withStyle(ChatFormatting.GOLD));
            totals.entrySet().stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<RelicStat, Float> e) -> e.getValue()).reversed())
                    .forEach(e -> relicLines.add(e.getKey().getTooltip(e.getValue())));

            relicLines.add(Component.empty());

            // slot order, same wording as the relic's own tooltip. a relic past its type's cap is listed
            // greyed so it's visible that it won't apply (and won't be consumed). same first-slots-win
            // rule as RelicItemData.filterEquippable, counted here per slot
            Map<String, Integer> perType = new java.util.HashMap<>();
            for (int i = IMapDeviceBlockEntity.RELIC_SLOT_START; i < IMapDeviceBlockEntity.SIZE; i++) {
                var stack = container.getItem(i);
                if (!RelicSlotUtil.isRelic(stack)) {
                    continue;
                }
                RelicItemData data = DungeonItemNbt.RELIC.loadFrom(stack);
                int nth = perType.merge(data.type, 1, Integer::sum);
                boolean applies = nth <= data.getType().max_equipped;

                // "Epic Relic - Uses Remaining: 3/3" on one line: the rarity in its own colour, the rest aqua
                MutableComponent rarity = data.getRarity().getTranslation(TranslationType.NAME).getTranslatedName()
                        .withStyle(applies ? data.getRarity().base_data.color() : ChatFormatting.DARK_GRAY);
                MutableComponent uses = DungeonWords.RELIC_USES_REMAINING.get(data.uses, data.getMaxUses());
                relicLines.add(Words.MAP_DEVICE_RELIC_USES.locName(rarity, uses)
                        .withStyle(applies ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
            }
        } catch (Exception e) {
            relicLines.clear();
        }
    }


    /** the player's allocated Atlas passives, each once with a count, and the stats under it */
    private void rebuildAtlasLines() {
        atlasLines.clear();
        atlasScroll = 0;
        if (!state.kind.showsAtlas) {
            return;
        }
        Player p = ClientOnly.getPlayer();
        if (p == null || !Database.areDatapacksLoaded(p.level())) {
            return;
        }
        try {
            var allocated = Load.player(p).talents.getAllAllocatedPerks(TalentTree.SchoolType.ATLAS);
            if (allocated.isEmpty()) {
                return;
            }
            var info = new StatRangeInfo(ModRange.hide());
            int level = Load.Unit(p).getLevel();

            // ordinary nodes have no names worth showing (most aren't localized) - every stat of every
            // allocated point goes into one pile and the duplicates are summed, so three "+4%" nodes and
            // a "+10%" one read as a single "+22%" line. game changers keep their name and their own stats
            List<TooltipStatInfo> tally = new ArrayList<>();
            List<Perk> gameChangers = new ArrayList<>();
            for (Perk perk : allocated.values()) {
                if (perk == null) {
                    continue;
                }
                if (perk.type == Perk.PerkType.MAJOR) {
                    gameChangers.add(perk);
                    continue;
                }
                for (OptScaleExactStat stat : perk.stats) {
                    tally.add(new TooltipStatInfo(stat.toExactStat(stat.scale_to_lvl ? level : 1), -99, info));
                }
            }

            atlasLines.add(Words.AtlasPassives.locName().withStyle(ChatFormatting.GOLD));

            TooltipStatInfo.mergeDuplicates(tally).stream()
                    .sorted(Comparator.comparing(x -> x.stat.locName().getString()))
                    .forEach(x -> atlasLines.addAll(x.GetTooltipString()));

            if (!gameChangers.isEmpty()) {
                atlasLines.add(Component.empty());
                gameChangers.stream()
                        .sorted(Comparator.comparing(x -> x.locName().getString()))
                        .forEach(perk -> {
                            atlasLines.add(perk.locName().withStyle(perk.type.format));
                            for (OptScaleExactStat stat : perk.stats) {
                                for (var line : stat.GetTooltipString(info)) {
                                    atlasLines.add(Component.literal("  ").append(line));
                                }
                            }
                        });
            }
        } catch (Exception e) {
            atlasLines.clear();
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics gui, int mx, int my, float partial) {
        gui.setColor(1F, 1F, 1F, 1F);

        DungeonStatsOverlay.renderNinePatchWithFallback(gui, guiLeft, guiTop, sizeX, sizeY);

        int centerX = guiLeft + sizeX / 2;
        gui.drawCenteredString(mc.font, getTitleText().copy().withStyle(ChatFormatting.YELLOW), centerX, guiTop + titleY, 0xFFFFFFFF);

        if (showTickets) {
            gui.drawCenteredString(mc.font, ticketsLine(), centerX, guiTop + ticketsY, 0xFFFFFFFF);
        }

        // the slot art, under the slot buttons that super.render draws the items into
        if (showMapSlot) {
            gui.blit(MapDeviceGui.MAP_SLOT_TEX, mapSlotX(), guiTop + mapSlotY, 0, 0,
                    MapDeviceGui.MAP_SLOT_TEX_SIZE, MapDeviceGui.MAP_SLOT_TEX_SIZE,
                    MapDeviceGui.MAP_SLOT_TEX_SIZE, MapDeviceGui.MAP_SLOT_TEX_SIZE);
        }
        gui.blit(MapDeviceGui.RELIC_SLOTS_TEX, relicGridX(), guiTop + relicY, 0, 0,
                MapDeviceGui.RELIC_SLOTS_TEX_SIZE, MapDeviceGui.RELIC_SLOTS_TEX_SIZE,
                MapDeviceGui.RELIC_SLOTS_TEX_SIZE, MapDeviceGui.RELIC_SLOTS_TEX_SIZE);

        renderRelicPanel(gui);
        renderAtlasPanel(gui);

        super.render(gui, mx, my, partial);
    }

    /** "Entry Tickets: n / m", coloured the way the in-map screen does it, or Unlimited for Uber/Pinnacle */
    private Component ticketsLine() {
        if (state.unlimitedTickets) {
            return Words.MAP_ENTRY_TICKETS.locName()
                    .append(Component.literal(": "))
                    .append(Words.MAP_ENTRY_TICKETS_UNLIMITED.locName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        int left = state.ticketsLeft;
        ChatFormatting color = left <= 0 ? ChatFormatting.RED : (left == 1 ? ChatFormatting.GOLD : ChatFormatting.AQUA);
        return Words.MAP_ENTRY_TICKETS.locName()
                .append(Component.literal(": " + left + " / " + state.ticketsMax))
                .withStyle(color);
    }

    private void renderRelicPanel(GuiGraphics gui) {
        if (relicLines.isEmpty()) {
            return;
        }
        int w = textWidth(relicLines) + MapDeviceGui.SIDE_PAD * 2;
        int h = relicLines.size() * MapDeviceGui.LINE_H + MapDeviceGui.SIDE_PAD * 2;
        int x = guiLeft + sizeX + MapDeviceGui.SIDE_GAP;
        int y = guiTop;
        DungeonStatsOverlay.renderNinePatchWithFallback(gui, x, y, w, h);
        int ty = y + MapDeviceGui.SIDE_PAD;
        for (Component line : relicLines) {
            gui.drawString(mc.font, line, x + MapDeviceGui.SIDE_PAD, ty, 0xFFFFFFFF);
            ty += MapDeviceGui.LINE_H;
        }
    }

    // ------------------------------------------------------------------ atlas panel + scrollbar

    private int maxAtlasLines() {
        return Math.max(3, (this.height - guiTop - 4 - MapDeviceGui.SIDE_PAD * 2) / MapDeviceGui.LINE_H);
    }

    private int visibleAtlasLines() {
        return Math.min(atlasLines.size(), maxAtlasLines());
    }

    private int maxAtlasScroll() {
        return Math.max(0, atlasLines.size() - visibleAtlasLines());
    }

    private boolean atlasOverflows() {
        return atlasLines.size() > visibleAtlasLines();
    }

    private void renderAtlasPanel(GuiGraphics gui) {
        if (atlasLines.isEmpty()) {
            return;
        }
        int visible = visibleAtlasLines();
        boolean overflow = atlasOverflows();

        int w = textWidth(atlasLines) + MapDeviceGui.SIDE_PAD * 2 + (overflow ? MapDeviceGui.SCROLLBAR_W : 0);
        int h = visible * MapDeviceGui.LINE_H + MapDeviceGui.SIDE_PAD * 2;
        int x = guiLeft - MapDeviceGui.SIDE_GAP - w;
        int y = guiTop;
        DungeonStatsOverlay.renderNinePatchWithFallback(gui, x, y, w, h);

        int ty = y + MapDeviceGui.SIDE_PAD;
        for (int i = atlasScroll; i < atlasScroll + visible && i < atlasLines.size(); i++) {
            gui.drawString(mc.font, atlasLines.get(i), x + MapDeviceGui.SIDE_PAD, ty, 0xFFFFFFFF);
            ty += MapDeviceGui.LINE_H;
        }

        if (overflow) {
            // the bar column sits inside the right padding; the handle slides along it
            barX = x + w - MapDeviceGui.SIDE_PAD - MapDeviceGui.SCROLLER_W + 2;
            barTop = y + MapDeviceGui.SIDE_PAD;
            barBottom = y + h - MapDeviceGui.SIDE_PAD;
            int travel = barBottom - barTop - MapDeviceGui.SCROLLER_H;
            int handleY = barTop + (int) (travel * (atlasScroll / (float) maxAtlasScroll()));
            gui.blit(MapDeviceGui.SCROLLER_SHEET, barX, handleY, MapDeviceGui.SCROLLER_U, MapDeviceGui.SCROLLER_V,
                    MapDeviceGui.SCROLLER_W, MapDeviceGui.SCROLLER_H, MapDeviceGui.SCROLLER_TEX_SIZE, MapDeviceGui.SCROLLER_TEX_SIZE);
        } else {
            barX = barTop = barBottom = 0;
        }
    }

    private int textWidth(List<Component> lines) {
        int max = MapDeviceGui.SIDE_MIN_W;
        for (Component line : lines) {
            max = Math.max(max, mc.font.width(line));
        }
        return max;
    }

    private boolean isOverScrollbar(double mx, double my) {
        return atlasOverflows() && barBottom > barTop
                && mx >= barX && mx < barX + MapDeviceGui.SCROLLER_W
                && my >= barTop && my < barBottom;
    }

    /** the handle is centred on the cursor, the same maths the creative inventory uses */
    private void scrollToMouse(double my) {
        int travel = barBottom - barTop - MapDeviceGui.SCROLLER_H;
        if (travel <= 0) {
            return;
        }
        float f = (float) (my - barTop - MapDeviceGui.SCROLLER_H / 2.0) / travel;
        atlasScroll = Math.max(0, Math.min(maxAtlasScroll(), Math.round(f * maxAtlasScroll())));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!atlasLines.isEmpty() && mx < guiLeft) {
            atlasScroll = Math.max(0, Math.min(maxAtlasScroll(), atlasScroll - (int) Math.signum(delta) * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && isOverScrollbar(mx, my)) {
            draggingScroller = true;
            scrollToMouse(my);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingScroller) {
            scrollToMouse(my);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingScroller = false;
        return super.mouseReleased(mx, my, button);
    }
}
