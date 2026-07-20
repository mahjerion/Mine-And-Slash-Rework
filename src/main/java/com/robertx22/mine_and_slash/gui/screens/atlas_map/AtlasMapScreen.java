package com.robertx22.mine_and_slash.gui.screens.atlas_map;

import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.library_of_exile.database.atlas.AtlasNode;
import com.robertx22.library_of_exile.database.init.LibDatabase;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.AtlasPassiveTreeScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.atlas.AtlasData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

// Informational Atlas map: shows node unlock/completion state and how nodes connect.
// Does not set map targets - random maps already respect the unlock gate (see DungeonMapItem),
// and Fixed Dungeon Map recipes stay recipe-based.
public class AtlasMapScreen extends BaseScreen implements INamedScreen {

    public enum NodeState {
        LOCKED, UNLOCKED, COMPLETED
    }

    private static final int SPACING = 60;
    // extra travel allowed past the point where the outermost node reaches the screen edge,
    // so panning can't drag the whole graph out of view
    private static final int SCROLL_PAD = 40;

    // provided separately - drop the PNG at src/main/resources/assets/mmorpg/textures/gui/atlas_map/background.png
    private static final ResourceLocation BACKGROUND = SlashRef.guiId("atlas_map/background");

    private final Map<String, AtlasNodeButton> nodeButtons = new HashMap<>();

    private int originX;
    private int originY;
    private int maxScrollX;
    private int maxScrollY;
    public int scrollX = 0;
    public int scrollY = 0;

    public AtlasMapScreen() {
        super(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    @Override
    protected void init() {
        super.init();
        nodeButtons.clear();
        scrollX = 0;
        scrollY = 0;

        AtlasData atlas = Load.player(ClientOnly.getPlayer()).atlas;

        var nodes = LibDatabase.AtlasNodes().getList();

        int minX = nodes.stream().mapToInt(n -> n.x).min().orElse(0);
        int maxX = nodes.stream().mapToInt(n -> n.x).max().orElse(0);
        int minY = nodes.stream().mapToInt(n -> n.y).min().orElse(0);
        int maxY = nodes.stream().mapToInt(n -> n.y).max().orElse(0);

        int graphCenterX = (minX + maxX) * SPACING / 2;
        int graphCenterY = (minY + maxY) * SPACING / 2;

        originX = this.width / 2 - graphCenterX;
        originY = this.height / 2 - graphCenterY;

        int graphWidth = (maxX - minX) * SPACING;
        int graphHeight = (maxY - minY) * SPACING;
        maxScrollX = Math.max(0, graphWidth / 2 - this.width / 2 + SCROLL_PAD);
        maxScrollY = Math.max(0, graphHeight / 2 - this.height / 2 + SCROLL_PAD);

        for (AtlasNode node : nodes) {
            Component name = resolveName(node);
            NodeState state = computeState(atlas, node);
            AtlasNodeButton btn = new AtlasNodeButton(0, 0, node, state, name);
            nodeButtons.put(node.id, btn);
            addRenderableWidget(btn);
        }

        updateNodePositions();

        addRenderableWidget(new AtlasNavButton(4, 4, this, new AtlasPassiveTreeScreen()));
    }

    private void updateNodePositions() {
        for (AtlasNodeButton btn : nodeButtons.values()) {
            int x = originX + btn.node.x * SPACING - AtlasNodeButton.SIZE / 2 + scrollX;
            int y = originY + btn.node.y * SPACING - AtlasNodeButton.SIZE / 2 + scrollY;
            btn.setX(x);
            btn.setY(y);
        }
    }

    // same key the map item's own tooltip uses (MapTooltip.MapLayoutName) - the Dungeon object's own
    // ITranslated name ("dungeon_realm.dungeon.<id>") is an internal/dev label, not the player-facing map name
    private Component resolveName(AtlasNode node) {
        return Component.translatable(DungeonWords.MapGUID(node.dungeon));
    }

    private NodeState computeState(AtlasData atlas, AtlasNode node) {
        if (atlas.isCompleted(node.id)) {
            return NodeState.COMPLETED;
        }
        if (atlas.isUnlocked(node.id)) {
            return NodeState.UNLOCKED;
        }
        return NodeState.LOCKED;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        scrollX = Mth.clamp((int) (scrollX + dragX), -maxScrollX, maxScrollX);
        scrollY = Mth.clamp((int) (scrollY + dragY), -maxScrollY, maxScrollY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderAtlasBackground(graphics);
        updateNodePositions();
        renderConnections(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderLabels(graphics);
    }

    // same blit shape as SkillTreeScreen.renderBackgroundDirt, pointed at the Atlas map's own texture
    private void renderAtlasBackground(GuiGraphics gui) {
        gui.blit(BACKGROUND, 0, 0, -10, 0.0F, 0.0F, this.width, this.height, 32, 32);
    }

    private void renderLabels(GuiGraphics graphics) {
        for (AtlasNodeButton btn : nodeButtons.values()) {
            int nameColor = switch (btn.state) {
                case LOCKED -> 0x60AAAAAA;
                case UNLOCKED -> 0xFFE0E0E0;
                case COMPLETED -> 0xFFFFD700;
            };
            String suffix = btn.state == NodeState.COMPLETED ? " ✓" : " ✗";
            ChatFormatting suffixColor = btn.state == NodeState.COMPLETED ? ChatFormatting.GREEN : ChatFormatting.RED;
            Component label = btn.displayName.copy().append(Component.literal(suffix).withStyle(suffixColor));
            graphics.drawCenteredString(Minecraft.getInstance().font, label,
                    btn.getX() + btn.getWidth() / 2, btn.getY() + btn.getHeight() + 3, nameColor);
        }
    }

    private void renderConnections(GuiGraphics graphics) {
        for (AtlasNode node : LibDatabase.AtlasNodes().getList()) {
            AtlasNodeButton from = nodeButtons.get(node.id);
            if (from == null) {
                continue;
            }
            for (String neighborId : node.neighbors) {
                if (node.id.compareTo(neighborId) >= 0) {
                    continue; // draw each edge once
                }
                AtlasNodeButton to = nodeButtons.get(neighborId);
                if (to == null) {
                    continue;
                }
                int color = lineColor(from.state, to.state);
                drawLine(graphics,
                        from.getX() + from.getWidth() / 2F, from.getY() + from.getHeight() / 2F,
                        to.getX() + to.getWidth() / 2F, to.getY() + to.getHeight() / 2F, color);
            }
        }
    }

    private int lineColor(NodeState a, NodeState b) {
        if (a == NodeState.LOCKED || b == NodeState.LOCKED) {
            return 0x30555555;
        }
        if (a == NodeState.COMPLETED && b == NodeState.COMPLETED) {
            return 0xFFFFD700;
        }
        return 0xFFAAAAAA;
    }

    private void drawLine(GuiGraphics graphics, float x1, float y1, float x2, float y2, int color) {
        graphics.pose().pushPose();
        graphics.pose().translate(x1, y1, 0);
        float angle = (float) Mth.atan2(y2 - y1, x2 - x1);
        graphics.pose().mulPose(Axis.ZP.rotation(angle));
        int length = (int) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        graphics.fill(0, -1, length, 1, color);
        graphics.pose().popPose();
    }

    @Override
    public ResourceLocation iconLocation() {
        return new ResourceLocation(SlashRef.MODID, "textures/gui/main_hub/icons/map.png");
    }

    @Override
    public Words screenName() {
        return Words.Atlas;
    }
}
