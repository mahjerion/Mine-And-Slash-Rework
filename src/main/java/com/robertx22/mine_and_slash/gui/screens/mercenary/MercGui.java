package com.robertx22.mine_and_slash.gui.screens.mercenary;

import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.resources.ResourceLocation;

/**
 * Every coordinate the mercenary screen draws at.
 * <p>
 * These are measured, not guessed: the slot frames in {@code textures/gui/mercenary/background.png}
 * are the pixels at luminance exactly 43, against a panel fill of 65/80, so each cell's edges can be
 * read straight off the art. The hand slots frame at x 80/95, armor at x 161/176, and the skill
 * column's bright border sits at x 11/28 with a 16px icon area at x 12..27 - which is where the
 * numbers below come from.
 */
public class MercGui {

    public static final int SIZE_X = 256;
    public static final int SIZE_Y = 256;

    /** every slot in this screen holds a standard 16x16 item icon */
    public static final int SLOT_SIZE = 16;

    public static final ResourceLocation BACKGROUND = SlashRef.guiId("mercenary/background");
    public static final ResourceLocation BUTTONS = SlashRef.guiId("mercenary/buttons");
    public static final ResourceLocation LEFT_RIGHT = SlashRef.guiId("mercenary/leftright");
    public static final ResourceLocation XP_BAR_BG = SlashRef.guiId("mercenary/experience_bar_background");
    public static final ResourceLocation XP_BAR_FILL = SlashRef.guiId("mercenary/experience_bar_fill");
    public static final ResourceLocation BLOCKED_SLOT = SlashRef.guiId("blocked_slot");
    // 18x18 frame for a 16x16 spell icon, the same one the class tree puts behind its spells
    public static final ResourceLocation SPELL_SLOT = SlashRef.guiId("spells/slots/spell");
    public static final int SPELL_SLOT_SIZE = 18;
    // only the offence/defence icons use this - the art leaves that strip blank, so they need
    // something to sit on. the three core stat boxes are drawn into the background already.
    public static final ResourceLocation STAT_ICON_BG = SlashRef.guiId("mercenary/stat_icon_background");

    public static ResourceLocation combatMode(String id) {
        return SlashRef.guiId("mercenary/combat_mode/" + id);
    }

    // ---------------------------------------------------------------- top left: class

    // the art leaves one 38x38 opening at (21,19); a 36x36 class icon centres inside it
    public static final int CLASS_ICON_X = 22;
    public static final int CLASS_ICON_Y = 20;
    public static final int CLASS_ICON_SIZE = 36;

    // leftright.png is a 44x44 sheet of four 22x22 sprites: left/right across, normal/hover down.
    // the blank band under the portrait is only 18px tall, so these overhang it slightly.
    public static final int ARROW_SIZE = 22;
    public static final int CLASS_ARROW_Y = 60;
    public static final int CLASS_ARROW_LEFT_X = 17;
    public static final int CLASS_ARROW_RIGHT_X = 41;

    // ---------------------------------------------------------------- top middle: hands, model, armor

    public static final int HAND_SLOT_X = 80;
    public static final int MAINHAND_SLOT_Y = 15;
    public static final int OFFHAND_SLOT_Y = 33;

    // the ornate cell is x77..x98, y67..y87; the icon centres in it and the hitbox covers the whole cell
    public static final int MODE_ICON_X = 80;
    public static final int MODE_ICON_Y = 69;
    public static final int MODE_ICON_SIZE = 16;
    public static final int MODE_BUTTON_X = 77;
    public static final int MODE_BUTTON_Y = 67;
    public static final int MODE_BUTTON_W = 22;
    public static final int MODE_BUTTON_H = 21;

    // the dark render panel spans x102..x154, so its centre is 128
    public static final int MODEL_CENTER_X = 128;
    public static final int MODEL_BOTTOM_Y = 84;
    public static final int MODEL_SCALE = 30;
    public static final int LEVEL_TEXT_Y = 14;

    public static final int ARMOR_SLOT_X = 161;
    public static final int ARMOR_SLOT_Y = 15;
    /** head/chest/legs/feet frame at y 15/30, 33/48, 51/66, 69/84 */
    public static final int ARMOR_SLOT_PITCH = 18;

    // ---------------------------------------------------------------- top right: core stats

    // three boxes drawn into the art. the numbers are centred in them with no extra background.
    public static final int CORE_BOX_W = 19;
    public static final int CORE_BOX_H = 14;
    /** strength, intelligence, dexterity - matching the red/blue/green panels in the art */
    public static final int[] CORE_BOX_X = {197, 221, 209};
    public static final int[] CORE_BOX_Y = {24, 24, 40};

    public static final int INFO_ICON_SIZE = 20;
    public static final int OFFENCE_ICON_X = 196;
    public static final int DEFENCE_ICON_X = 220;
    public static final int INFO_ICON_Y = 58;

    // ---------------------------------------------------------------- experience bar

    // the recess runs x6..x249, y94..y98; the 244x7 background sits at (6,93) and the 240x3 fill
    // lands two pixels inside it
    public static final int XP_BAR_X = 6;
    public static final int XP_BAR_Y = 93;
    public static final int XP_BAR_W = 244;
    public static final int XP_BAR_H = 7;
    public static final int XP_FILL_X = 8;
    public static final int XP_FILL_Y = 95;
    public static final int XP_FILL_W = 240;
    public static final int XP_FILL_H = 3;

    // ---------------------------------------------------------------- bottom left: skills

    public static final int SKILL_SLOT_X = 12;
    public static final int SKILL_SLOT_Y = 105;
    public static final int SKILL_COLUMN_PITCH = 25;

    /** the three round sockets sit in the same columns, at y 127 / 145 / 163 */
    public static final int SUPPORT_SLOT_Y = 127;
    public static final int SUPPORT_ROW_PITCH = 18;

    // blocked_slot.png is 20x19 and is drawn 2px out from the slot it covers, the same way
    // SkillGemsScreen offsets SuppGemOverlayButton from its gem slots
    public static final int LOCK_W = 20;
    public static final int LOCK_H = 19;
    public static final int LOCK_OFFSET = 2;

    // ---------------------------------------------------------------- bottom left: auras and spirit

    // two ornate sockets, interiors x11..x22 and x29..x40; a 16x16 icon centres on each
    public static final int AURA_SLOT_X = 9;
    public static final int AURA_SLOT_Y = 188;
    public static final int AURA_SLOT_PITCH = 18;

    /** the blank band to the right of both sockets */
    public static final int SPIRIT_X = 50;
    public static final int SPIRIT_Y = 192;

    // ---------------------------------------------------------------- render box: on/off toggle

    // the two generic toggle icons the config gui already uses, so this needs no art of its own
    public static final ResourceLocation TOGGLE_ON = SlashRef.id("textures/gui/inv_gui/icons/config_on.png");
    public static final ResourceLocation TOGGLE_OFF = SlashRef.id("textures/gui/inv_gui/icons/config.png");

    // tucked into the bottom right of the model panel, which is a flat dark field with no cell to
    // sit in - so the hitbox is just the icon. the panel's interior is x102..x154, and its bottom
    // corners are chamfered a pixel a row across y83..y86, so the icon stops short of both: it
    // occupies x136..x151 / y68..y83, still solid panel on every one of those rows.
    public static final int TOGGLE_BUTTON_X = 136;
    public static final int TOGGLE_BUTTON_Y = 68;
    public static final int TOGGLE_BUTTON_SIZE = 16;

    // ---------------------------------------------------------------- bottom left: stats button

    // buttons.png holds the collapsed sprite in rows 0..27 (35 wide) and the hover bar in rows
    // 28..55 (88 wide). blitting the collapsed one at 18 wide was what clipped it.
    public static final int STATS_BUTTON_X = 6;
    public static final int STATS_BUTTON_Y = 220;
    public static final int STATS_BUTTON_W = 35;
    public static final int STATS_BUTTON_H = 28;
    public static final int STATS_BUTTON_HOVER_W = 88;
    /** where the "Stats" label starts on the expanded bar, past its icon */
    public static final int STATS_LABEL_OFFSET = 34;
    // both sprites carry the same recessed square, framed at x7/x26 and rows 4/23, so its 18x18
    // interior starts at (8,5) and a 16x16 icon centres one pixel further in - in either state.
    public static final int STATS_ICON_DX = 9;
    public static final int STATS_ICON_DY = 6;

    // ---------------------------------------------------------------- right: skill track

    // seven level plaques are baked into the art, their gridlines at y 110/131/152/173/194/215/236,
    // level 1 at the bottom. an icon centres on row r (r=0 is level 1) at TRACK_BOTTOM - r*PITCH.
    public static final int TRACK_BOTTOM_Y = 236;
    public static final int TRACK_ROW_PITCH = 21;
    public static final int TRACK_ICON_SIZE = 16;
    public static final int TRACK_FIRST_X = 134;
    public static final int TRACK_COLUMN_PITCH = 20;
    public static final int TRACK_MAX_X = 228;
}
