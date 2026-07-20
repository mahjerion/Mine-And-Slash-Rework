package com.robertx22.mine_and_slash.gui.screens.atlas_map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.dungeon_realm.database.atlas.AtlasNode;
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
import net.minecraft.client.gui.screens.Screen;
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

    private final AtlasMapScreen screen;
    public final AtlasNode node;
    public final AtlasMapScreen.NodeState state;
    public final Component displayName;

    public AtlasNodeButton(AtlasMapScreen screen, int x, int y, AtlasNode node, AtlasMapScreen.NodeState state, Component displayName) {
        super(x, y, SIZE, SIZE, displayName);
        this.screen = screen;
        this.node = node;
        this.state = state;
        this.displayName = displayName;
        // tooltip is (re)computed each frame in renderWidget instead of once here - AtlasMapScreen
        // can be zoomed, and the button's logical getX()/getY() stay in unzoomed space while the
        // mouse position is real screen space, so hover has to be checked with zoom-corrected
        // coordinates (same reason SkillTreeScreen's PerkButton manages its own tooltip manually
        // instead of relying on the inherited, zoom-unaware isHovered flag)
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

    private float opacity(boolean hovered) {
        return switch (state) {
            case LOCKED -> 0.35F;
            case UNLOCKED -> hovered ? 1F : 0.85F;
            case COMPLETED -> 1F;
        };
    }

    // zoom-corrected hover check - getX()/getY()/width/height live in unzoomed tree space, but
    // mouseX/mouseY here are raw screen coordinates, so they need the same 1/zoom correction
    // PerkButton uses for its own isInside/mouseClicked
    private boolean isInside(int x, int y) {
        return x >= getX() && y >= getY() && x < getX() + width && y < getY() + height;
    }

    private void updateTooltip(int mouseX, int mouseY) {
        int zx = (int) (1F / screen.zoom * mouseX);
        int zy = (int) (1F / screen.zoom * mouseY);

        // name already renders as a persistent label below the node (AtlasMapScreen.renderLabels), so the
        // tooltip is used for the atlas point reward instead - only relevant for available, uncompleted nodes
        if ((state == AtlasMapScreen.NodeState.UNLOCKED || state == AtlasMapScreen.NodeState.LOCKED) && isInside(zx, zy)) {
            MutableComponent reward = Component.literal(node.atlas_points_reward
                    + " Atlas Point" + (node.atlas_points_reward == 1 ? "" : "s") + " Available");
            Component req = describeRequirement(node);
            if (req != null) {
                reward.append("\n").append(req);
            }
            setTooltip(Tooltip.create(reward));

            Screen mcScreen = Minecraft.getInstance().screen;
            if (mcScreen != null) {
                mcScreen.setTooltipForNextRenderPass(this.getTooltip(), this.createTooltipPositioner(), true);
            }
        } else {
            setTooltip(null);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateTooltip(mouseX, mouseY);

        int zx = (int) (1F / screen.zoom * mouseX);
        int zy = (int) (1F / screen.zoom * mouseY);
        boolean hovered = isInside(zx, zy);

        PerkStatus status = perkStatus();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShaderColor(1F, 1F, 1F, opacity(hovered));

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
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(1F / screen.zoom * mouseX, 1F / screen.zoom * mouseY, button);
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
