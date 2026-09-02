package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.mine_and_slash.a_libraries.neat.NeatConfig;
import com.robertx22.mine_and_slash.database.data.mercenary.ClientMercenary;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.types.core_stats.AllAttributes;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.saveclasses.PointData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryInventories;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.stat_calculation.MercenaryStatUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenaryActionPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

/**
 * The mercenary screen, laid out to match {@code textures/gui/mercenary/background.png}.
 * <p>
 * Everything it shows comes from the player's synced {@link MercenaryData}, so it works whether or not
 * the mercenary is currently out. Live stats - the core attributes and the offence/defence summaries -
 * additionally need the entity, because those are calculated on it.
 */
public class MercenaryScreen extends BaseScreen implements INamedScreen {

    public MercenaryScreen() {
        super(MercGui.SIZE_X, MercGui.SIZE_Y);
    }

    // ------------------------------------------------------------------ data access

    public Player getPlayer() {
        return ClientOnly.getPlayer();
    }

    public String getActiveClassId() {
        Player p = getPlayer();
        return p == null ? "" : Load.player(p).mercs.getActiveId();
    }

    public MercenaryData getMercData() {
        Player p = getPlayer();
        return p == null ? null : Load.player(p).mercs.getActive();
    }

    public MercenaryClass getMercClass() {
        MercenaryData data = getMercData();
        return data == null ? null : data.getMercClass();
    }

    // shared with the effect overlay - the lookup is an AABB entity query, so it is cached per client
    // tick rather than run per frame by the paper doll, the spirit readout and every stat button.
    public MercenaryEntity getMercEntity() {
        return ClientMercenary.get();
    }

    public boolean isMercSummoned() {
        return getMercEntity() != null;
    }

    public int getMercLevel() {
        MercenaryData data = getMercData();
        return data == null ? 1 : data.lvl;
    }

    /** live calculated stat when the mercenary is out, 0 otherwise */
    public float getStatValue(Stat stat) {
        MercenaryEntity merc = getMercEntity();
        if (merc == null) {
            return 0;
        }
        return Load.Unit(merc).getUnit().getCalculatedStat(stat).getValue();
    }

    // ------------------------------------------------------------------ layout

    private String builtForClass = "";
    private int builtForLevel = -1;

    @Override
    protected void init() {
        super.init();
        rebuild();
    }

    /**
     * The stance keybind, repeated here because vanilla only feeds KeyMapping state while no screen
     * is open - so OnKeyPress is dead the moment this screen goes up, and the one place the mode is
     * actually displayed would be the one place the key did nothing.
     * <p>
     * Same shape BaseScreen already uses for the hub key. No EditBox on this screen, so it needs none
     * of the focus guards that one carries, and nothing here has to refresh: MercModeButton re-reads
     * the live mode every frame, so the icon follows the sync on its own.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeybindsRegister.CYCLE_MERC_MODE.matches(keyCode, scanCode)) {
            Packets.sendToServer(MercenaryActionPacket.cycleMode());
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();

        // the class arrows and any level up change what the skill track holds, and both arrive from
        // the server after the click. checked off the render path, the way SkillGemsScreen keeps its
        // spirit refresh out of it.
        if (!builtForClass.equals(getActiveClassId()) || builtForLevel != getMercLevel()) {
            rebuild();
        }
    }

    /** rebuilt whenever the layout depends on data that can change while the screen is open */
    private void rebuild() {
        this.clearWidgets();

        builtForClass = getActiveClassId();
        builtForLevel = getMercLevel();

        int left = guiLeft;
        int top = guiTop;

        // class portrait and the arrows under it. the portrait draws itself so the rect has one owner
        publicAddButton(new MercClassIconButton(this, left + MercGui.CLASS_ICON_X, top + MercGui.CLASS_ICON_Y));
        publicAddButton(new MercClassArrowButton(this, false, left + MercGui.CLASS_ARROW_LEFT_X, top + MercGui.CLASS_ARROW_Y));
        publicAddButton(new MercClassArrowButton(this, true, left + MercGui.CLASS_ARROW_RIGHT_X, top + MercGui.CLASS_ARROW_Y));

        // hands
        publicAddButton(new MercSlotButton(this, MercenarySlotType.GEAR,
                MercenaryInventories.gearIndexOf(net.minecraft.world.entity.EquipmentSlot.MAINHAND),
                left + MercGui.HAND_SLOT_X, top + MercGui.MAINHAND_SLOT_Y, MercGui.SLOT_SIZE));
        publicAddButton(new MercSlotButton(this, MercenarySlotType.GEAR,
                MercenaryInventories.gearIndexOf(net.minecraft.world.entity.EquipmentSlot.OFFHAND),
                left + MercGui.HAND_SLOT_X, top + MercGui.OFFHAND_SLOT_Y, MercGui.SLOT_SIZE));

        // behaviour toggle
        publicAddButton(new MercModeButton(this, left + MercGui.MODE_BUTTON_X, top + MercGui.MODE_BUTTON_Y));
        publicAddButton(new MercToggleButton(left + MercGui.TOGGLE_BUTTON_X, top + MercGui.TOGGLE_BUTTON_Y));

        // armor, top to bottom
        net.minecraft.world.entity.EquipmentSlot[] armor = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < armor.length; i++) {
            publicAddButton(new MercSlotButton(this, MercenarySlotType.GEAR,
                    MercenaryInventories.gearIndexOf(armor[i]),
                    left + MercGui.ARMOR_SLOT_X, top + MercGui.ARMOR_SLOT_Y + i * MercGui.ARMOR_SLOT_PITCH, MercGui.SLOT_SIZE));
        }

        // core attributes - the three CoreStats the mod has, in the panel colours the art uses.
        //
        // read out of the database rather than off the DatapackStats statics, which is what
        // MainHubScreen does for the player's own panel. the statics carry the values the mod was
        // compiled with; the database entry carries whatever the loaded datapack says, which in a
        // modpack is a different set of numbers. taking the statics here meant the mercenary panel
        // described base Mine and Slash strength while the player panel two screens away described
        // the pack's.
        Stat[] core = {
                ExileDB.Stats().get(AllAttributes.STR_ID),
                ExileDB.Stats().get(AllAttributes.INT_ID),
                ExileDB.Stats().get(AllAttributes.DEX_ID)
        };
        ChatFormatting[] colors = {ChatFormatting.RED, ChatFormatting.BLUE, ChatFormatting.GREEN};
        for (int i = 0; i < core.length; i++) {
            publicAddButton(new MercCoreStatButton(this, core[i], colors[i],
                    left + MercGui.CORE_BOX_X[i], top + MercGui.CORE_BOX_Y[i]));
        }

        publicAddButton(new MercInfoButton(this, MercInfoButton.Kind.OFFENCE, left + MercGui.OFFENCE_ICON_X, top + MercGui.INFO_ICON_Y));
        publicAddButton(new MercInfoButton(this, MercInfoButton.Kind.DEFENCE, left + MercGui.DEFENCE_ICON_X, top + MercGui.INFO_ICON_Y));

        // active skills and their support sockets
        for (int i = 0; i < MercenaryClass.EQUIPPED_SKILLS; i++) {
            int x = left + MercGui.SKILL_SLOT_X + i * MercGui.SKILL_COLUMN_PITCH;
            publicAddButton(new MercSkillButton(this, i, x, top + MercGui.SKILL_SLOT_Y, MercGui.SLOT_SIZE));

            for (int s = 0; s < MercenaryClass.SUPPORTS_PER_SKILL; s++) {
                publicAddButton(new MercSlotButton(this, MercenarySlotType.SUPPORT,
                        MercenaryInventories.supportIndex(i, s),
                        x, top + MercGui.SUPPORT_SLOT_Y + s * MercGui.SUPPORT_ROW_PITCH, MercGui.SLOT_SIZE));
            }
        }

        // aura gems
        for (int i = 0; i < MercenaryClass.AURA_SLOTS; i++) {
            publicAddButton(new MercSlotButton(this, MercenarySlotType.AURA, i,
                    left + MercGui.AURA_SLOT_X + i * MercGui.AURA_SLOT_PITCH, top + MercGui.AURA_SLOT_Y, MercGui.SLOT_SIZE));
        }

        publicAddButton(new MercStatsButton(this, left + MercGui.STATS_BUTTON_X, top + MercGui.STATS_BUTTON_Y));

        addSkillTrack(left, top);

        // has to be added here rather than in init(): rebuild() clears every widget and runs again
        // from tick() whenever the class or level changes
        addBackToHubButton();
    }

    /**
     * The class track down the right. The art already has seven level plaques baked in, with their
     * gridlines 21px apart and level 1 at the bottom, so a skill's grid row maps straight onto one of
     * them: row 0 is level 1 at y 236, row 6 is level 30 at y 110.
     */
    private void addSkillTrack(int left, int top) {
        MercenaryClass mercClass = getMercClass();
        if (mercClass == null) {
            return;
        }
        int rows = Math.max(1, mercClass.lvl_reqs.size());
        int half = MercGui.TRACK_ICON_SIZE / 2;

        for (Map.Entry<String, PointData> en : mercClass.skills.entrySet()) {
            Spell spell = ExileDB.Spells().isRegistered(en.getKey()) ? ExileDB.Spells().get(en.getKey()) : null;
            if (spell == null) {
                continue;
            }
            PointData point = en.getValue();
            int row = Math.max(0, Math.min(point.y, rows - 1));
            int col = Math.max(0, point.x);

            // centre the icon on the baked gridline for its row
            int y = top + MercGui.TRACK_BOTTOM_Y - row * MercGui.TRACK_ROW_PITCH - half;
            // and clamp the column so a datapack can't push a skill off the right edge of the panel
            int x = left + Math.min(MercGui.TRACK_FIRST_X + col * MercGui.TRACK_COLUMN_PITCH, MercGui.TRACK_MAX_X);

            publicAddButton(new MercTrackSkillButton(this, spell, mercClass.getLevelNeededFor(point), x, y));
        }
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics gui, int mx, int my, float ticks) {
        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(MercGui.BACKGROUND, guiLeft, guiTop, 0, 0, sizeX, sizeY, 256, 256);

        // the class portrait is drawn by MercClassIconButton, inside super.render() below - nothing
        // overlaps its corner, so the later draw order doesn't matter
        renderModel(gui, mx, my);
        renderExpBar(gui);
        renderSpirit(gui);

        super.render(gui, mx, my, ticks);
    }

    private void renderModel(GuiGraphics gui, int mx, int my) {
        MercenaryEntity merc = getMercEntity();

        int px = guiLeft + MercGui.MODEL_CENTER_X;
        int py = guiTop + MercGui.MODEL_BOTTOM_Y;

        if (merc != null) {
            // same paper doll treatment StatScreen uses, health bar suppressed
            boolean neatDraw = NeatConfig.draw;
            NeatConfig.draw = false;
            InventoryScreen.renderEntityInInventoryFollowsMouse(gui, px, py, MercGui.MODEL_SCALE,
                    (float) (px - mx), (float) (py - 50 - my), merc);
            NeatConfig.draw = neatDraw;
        }

        Component level = Words.MercenaryLevel.locName(getMercLevel()).withStyle(ChatFormatting.WHITE);
        gui.drawString(mc.font, level, px - mc.font.width(level) / 2, guiTop + MercGui.LEVEL_TEXT_Y, 0xFFFFFF, true);
    }

    private void renderExpBar(GuiGraphics gui) {
        MercenaryData data = getMercData();
        if (data == null) {
            return;
        }
        int x = guiLeft + MercGui.XP_BAR_X;
        int y = guiTop + MercGui.XP_BAR_Y;

        gui.setColor(1F, 1F, 1F, 1F);
        gui.blit(MercGui.XP_BAR_BG, x, y, 0, 0, MercGui.XP_BAR_W, MercGui.XP_BAR_H, MercGui.XP_BAR_W, MercGui.XP_BAR_H);

        int needed = LevelUtils.getExpRequiredForLevel(data.lvl + 1);
        float pct = needed <= 0 ? 0 : Math.min(1F, data.exp / (float) needed);
        int filled = (int) (MercGui.XP_FILL_W * pct);

        if (filled > 0) {
            gui.blit(MercGui.XP_BAR_FILL, guiLeft + MercGui.XP_FILL_X, guiTop + MercGui.XP_FILL_Y,
                    0, 0, filled, MercGui.XP_FILL_H, MercGui.XP_FILL_W, MercGui.XP_FILL_H);
        }
    }

    /** just the remaining number, in the same light purple the skill gem screen uses for spirit */
    private void renderSpirit(GuiGraphics gui) {
        MercenaryData data = getMercData();
        if (data == null) {
            return;
        }
        String text = "" + MercenaryStatUtils.getRemainingSpirit(getMercEntity(), data);

        gui.drawString(mc.font, text, guiLeft + MercGui.SPIRIT_X, guiTop + MercGui.SPIRIT_Y,
                ChatFormatting.LIGHT_PURPLE.getColor(), true);
    }

    // no level axis is drawn here on purpose - the 1/5/10/15/20/25/30 plaques are part of the
    // background art, and addSkillTrack lines the icons up with them.

    // ------------------------------------------------------------------ INamedScreen

    @Override
    public ResourceLocation iconLocation() {
        return SlashRef.id("textures/gui/main_hub/icons/mercenary.png");
    }

    @Override
    public Words screenName() {
        return Words.Mercenary;
    }
}
