package com.robertx22.mine_and_slash.gui.screens.atlas_map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.library_of_exile.database.atlas.AtlasNode;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.perks.PerkStatus;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class AtlasNodeButton extends AbstractWidget {

    // reuses the talent/ascendancy tree's own node chrome (background + border textures per status)
    // so Atlas nodes match the rest of the skill tree UI, without pulling in the Perk/connection system -
    // this button keeps its own simple LOCKED/UNLOCKED/COMPLETED state and click handling
    private static final Perk.PerkType CHROME = Perk.PerkType.STAT;
    public static final int SIZE = CHROME.size;
    private static final ResourceLocation ICON = SlashRef.id("textures/gui/atlas_map/icons/map.png");

    public final AtlasNode node;
    public final AtlasMapScreen.NodeState state;
    public final Component displayName;

    public AtlasNodeButton(int x, int y, AtlasNode node, AtlasMapScreen.NodeState state, Component displayName) {
        super(x, y, SIZE, SIZE, displayName);
        this.node = node;
        this.state = state;
        this.displayName = displayName;
        // name already renders as a persistent label below the node (AtlasMapScreen.renderLabels), so the
        // tooltip is used for the atlas point reward instead - only relevant for available, uncompleted nodes
        if (state == AtlasMapScreen.NodeState.UNLOCKED) {
            MutableComponent reward = Component.literal(node.atlas_points_reward
                    + " Atlas Point" + (node.atlas_points_reward == 1 ? "" : "s") + " Available");
            Component req = describeRequirement(node);
            if (req != null) {
                reward.append("\n").append(req);
            }
            setTooltip(Tooltip.create(reward));
        }
    }

    // null if this node has no extra completion requirement beyond just clearing the dungeon
    private static Component describeRequirement(AtlasNode node) {
        if (node.require_uber) {
            return Component.literal("Requires an Uber map").withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        if (!node.min_rarity.isEmpty()) {
            GearRarity rar = ExileDB.GearRarities().get(node.min_rarity);
            return Component.literal("Requires ").append(rar.coloredName()).append(" rarity or higher");
        }
        if (node.min_tier > 0) {
            return Component.literal("Requires Tier " + node.min_tier + "+ map").withStyle(ChatFormatting.YELLOW);
        }
        return null;
    }

    private PerkStatus perkStatus() {
        return switch (state) {
            case LOCKED -> PerkStatus.BLOCKED;
            case UNLOCKED -> PerkStatus.POSSIBLE;
            case COMPLETED -> PerkStatus.CONNECTED;
        };
    }

    private float opacity() {
        return switch (state) {
            case LOCKED -> 0.35F;
            case UNLOCKED -> isHovered ? 1F : 0.85F;
            case COMPLETED -> 1F;
        };
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PerkStatus status = perkStatus();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, opacity());

        blitFull(graphics, CHROME.getColorTexture(status), getX(), getY(), width);
        blitFull(graphics, CHROME.getBorderTexture(status), getX(), getY(), width);

        int iconSize = CHROME.iconSize;
        int iconOff = (int) CHROME.getOffset();
        blitFull(graphics, ICON, getX() + iconOff, getY() + iconOff, iconSize);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    private static void blitFull(GuiGraphics graphics, ResourceLocation tex, int x, int y, int size) {
        graphics.blit(tex, x, y, 0, 0, size, size, size, size);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (state == AtlasMapScreen.NodeState.LOCKED) {
            return;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }
}
