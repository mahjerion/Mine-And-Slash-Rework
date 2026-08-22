package com.robertx22.mine_and_slash.gui.overlays.spell_hotbar;

import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.library_of_exile.utils.CLOC;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.entity.SummonedData;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.Locale;

public class SpellOnHotbarRender {
    static int CHARGE_SIZE = 20;

    // sits along the bottom edge of the icon, so it never fights the cooldown overlay, the key badge
    // or the summon count for pixels
    private static final int CHARGE_BAR_BG = 0xFF1C1C1C;
    private static final int CHARGE_BAR_FILL = 0xFF4FC3F7;

    private static final ResourceLocation CHARGE = new ResourceLocation(SlashRef.MODID, "textures/gui/spells/charges/full_charges.png");
    private static final ResourceLocation LOW_CHARGE = new ResourceLocation(SlashRef.MODID, "textures/gui/spells/charges/low_charges.png");
    private static final ResourceLocation NO_CHARGE = new ResourceLocation(SlashRef.MODID, "textures/gui/spells/charges/no_charges.png");
    private static final ResourceLocation KEY_BG = new ResourceLocation(SlashRef.MODID, "textures/gui/spells/keybind_bg.png");
    private static final ResourceLocation MOD_BG = new ResourceLocation(SlashRef.MODID, "textures/gui/spells/modbg.png");
    private static final ResourceLocation COOLDOWN_TEX = new ResourceLocation(SlashRef.MODID, "textures/gui/spells/cooldown.png");


    public int place;
    public GuiGraphics gui;
    public int x;
    public int y;
    public Spell spell;

    int keyNum = 0;

    boolean disableKeyRender = false;

    Minecraft mc = Minecraft.getInstance();

    boolean horizontal;

    public SpellOnHotbarRender(boolean horizontal, int place, GuiGraphics gui, int x, int y) {
        this.place = place;
        this.gui = gui;
        this.horizontal = horizontal;
        this.x = x;
        this.y = y;

        if (ClientConfigs.getConfig().HOTBAR_SWAPPING.get()) {
            if (place > 3) {
                if (!SpellKeybind.IS_ON_SECONd_HOTBAR) {
                    disableKeyRender = true;
                }
            } else {
                if (SpellKeybind.IS_ON_SECONd_HOTBAR) {
                    disableKeyRender = true;
                }
            }

        }

        this.keyNum = place;

        if (ClientConfigs.getConfig().HOTBAR_SWAPPING.get()) {
            if (SpellKeybind.IS_ON_SECONd_HOTBAR) {
                keyNum -= 4;
            }
        }

        this.spell = Load.player(ClientOnly.getPlayer()).getSkillGemInventory().getHotbarGem(place).getSpell();
    }


    public void render() {
        var mc = Minecraft.getInstance();


        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (horizontal) {
            x += place * 20;
        } else {
            y += place * 20;
        }

        try {
            int xs = (int) (x);
            int ys = (int) (y);


            if (spell != null) {
                gui.blit(spell.getIconLoc(), xs, ys, 0, 0, 16, 16, 16, 16);

                if (spell.config.charges > 0) {
                    drawCharge(xs, ys);
                }

                // the global cooldown darkens every skill, not only the one just cast, so a shared
                // bind visibly plays its skills one at a time instead of looking frozen
                drawCooldown(gui);

                drawChargeRecharge(gui);

                SummonedData summonedData = Load.player(mc.player).getSummonedData();
                if (summonedData.getSummonedAmount(spell.GUID()) > 0) {
                    drawSummoned(xs + 3, ys + 3, summonedData.getSummonedAmount(spell.GUID()));
                }


                int xkey = xs + 15;
                int ykey = y + 14;

                drawKey(xkey, ykey);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawCharge(int xs, int ys) {

        int charges = Load.player(ClientOnly.getPlayer()).spellCastingData.charges.getCharges(spell.config.charge_name);

        ResourceLocation chargeTex = CHARGE;

        if (charges == 0) {
            chargeTex = NO_CHARGE;

        } else {
            if (charges != spell.config.charges) {
                chargeTex = LOW_CHARGE;
            }
        }

        int chargex = x - 2;

        gui.blit(chargeTex, chargex, y - 2, 0, 0, CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE, CHARGE_SIZE);
    }

    private void drawKey(int xkey, int ykey) {
        if (disableKeyRender) {
            return;
        }
        int bgsize = 10;

        RenderSystem.enableBlend(); // enables transparency

        float alpha = 0.75f;
        gui.setColor(alpha, alpha, alpha, alpha);
        gui.blit(KEY_BG, xkey - 6, ykey - 6, 0, 0, bgsize, bgsize, bgsize, bgsize);

        gui.setColor(1.0F, 1.0F, 1.0F, 1);


        String txt = "";
        if (!KeybindsRegister.getSpellHotbar(keyNum).key.isUnbound()) {
            txt = CLOC.translate(KeybindsRegister.getSpellHotbar(keyNum).key.getKey().getDisplayName()).toUpperCase(Locale.ROOT);
            if (!txt.isEmpty()) {
                txt = txt.substring(0, 1);
            }
        }
        // todo renderScaledText doesnt do push and pop but does antiscale.. FIX THIS
        GuiUtils.renderScaledText(gui, xkey - 1, ykey, 1, txt, ChatFormatting.GREEN);

        var key = KeybindsRegister.getSpellHotbar(keyNum);
        if (key.key.getKeyModifier() != KeyModifier.NONE) {

            RenderSystem.enableBlend(); // enables transparency
            gui.setColor(alpha, alpha, alpha, alpha);
            gui.blit(MOD_BG, xkey - 18, ykey - 6, 0, 0, 13, bgsize, 13, bgsize);
            gui.setColor(1.0F, 1.0F, 1.0F, 1F);

            String modtext = KeybindsRegister.getSpellHotbar(keyNum).key.getKeyModifier().toString().substring(0, 3);
            GuiUtils.renderScaledText(gui, xkey - 11, ykey, 0.6F, modtext, ChatFormatting.YELLOW);
        }


        RenderSystem.disableBlend(); // enables transparency
    }

    // a charge coming back is progress, not a lock: with a charge in hand the skill is usable, so it
    // must not darken the icon. it gets its own slim meter under the icon rather than sharing the
    // cooldown bar, where a 30 second regen next to a 2 second recovery just looked frozen
    private void drawChargeRecharge(GuiGraphics gui) {
        if (spell.config.charges < 1) {
            return;
        }
        var charges = Load.player(mc.player).spellCastingData.charges;
        float left = charges.getRechargeFractionLeft(spell.config.charge_name, spell.config.charge_regen);
        if (left <= 0) {
            return; // nothing on its way back
        }

        // fills toward the next charge, so it reads as progress rather than as another cooldown
        int barY = this.y + 15;
        int filled = (int) (16 * Mth.clamp(1F - left, 0F, 1F));

        gui.fill(this.x, barY, this.x + 16, barY + 1, CHARGE_BAR_BG);
        if (filled > 0) {
            gui.fill(this.x, barY, this.x + filled, barY + 1, CHARGE_BAR_FILL);
        }
    }

    private void drawCooldown(GuiGraphics gui) {

        CooldownsData cds = Load.Unit(mc.player).getCooldowns();

        // whichever wait has the most left is the one the player actually cares about. the global
        // cooldown is in here so every slot darkens together, not just the skill that was cast
        float percent = 0F;
        int longestLeft = 0;

        for (String id : new String[]{spell.GUID(), CooldownsData.GLOBAL_COOLDOWN}) {
            int left = cds.getCooldownTicks(id);
            int need = cds.getNeededTicks(id);
            // compared by time left, not by fraction. the two waits have different lengths, so a full
            // 2s recovery is a smaller wait than a 25s cooldown at 40% - picking by fraction made the
            // long one snap back to full and then halt where it had been
            if (left > 1 && need > 0 && left > longestLeft) {
                longestLeft = left;
                percent = (float) left / (float) need;
            }
        }

        if (percent > 0) {
            drawCooldownBar(gui, percent);
        }
    }

    private void drawCooldownBar(GuiGraphics gui, float percent) {
        gui.blit(COOLDOWN_TEX, this.x, this.y, 0, 0, 16, (int) (16 * Mth.clamp(percent, 0, 1F)), 16, 16);
    }

    private void drawSummoned(int x, int y, int summonedAmount) {
        int bgsize = 10;
        float alpha = 0.75f;
        gui.setColor(alpha, alpha, alpha, alpha);
        gui.blit(KEY_BG, x - 6, y - 6, 0, 0, bgsize, bgsize, bgsize, bgsize);

        gui.setColor(1.0F, 1.0F, 1.0F, 1);


        String txt = String.valueOf(summonedAmount);
        // todo renderScaledText doesnt do push and pop but does antiscale.. FIX THIS
        GuiUtils.renderScaledText(gui, x - 1, y, 1, txt, ChatFormatting.RED);

        RenderSystem.disableBlend(); // enables transparency
    }
}
