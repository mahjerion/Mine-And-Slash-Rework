package com.robertx22.mine_and_slash.gui.screens.atlas_map;

import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.atlas.AtlasNode;
import com.robertx22.dungeon_realm.main.DungeonWords;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.atlas.AtlasNodeLayout;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.AtlasPassiveTreeScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.PointData;
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
import java.util.Set;

// Informational Atlas map: shows node unlock/completion state and how nodes connect.
// Does not set map targets - random maps already respect the unlock gate (see DungeonMapItem),
// and Fixed Dungeon Map recipes stay recipe-based.
// Positions and connections come from the single AtlasNodeLayout datapack entry (one grid string,
// same authoring format as TalentTree.perks) rather than per-node x/y/neighbors fields.
public class AtlasMapScreen extends BaseScreen implements INamedScreen {

    public enum NodeState {
        LOCKED, UNLOCKED, COMPLETED
    }

    // the grid places one connector cell between adjacent nodes, so a "1 grid step" in the old
    // x/y system is 2 grid cells here - half the old SPACING (60) keeps the layout visually the same
    private static final int PIXELS_PER_CELL = 30;
    // generous fixed pan range on both axes (not fitted to the tree's actual content box) - the
    // current tree is much taller than wide, so a content-fitted horizontal limit clamps to 0 at
    // typical screen widths even though vertical stays positive. Same magnitude SkillTreeScreen
    // already uses for its own pan clamp, so it's not an arbitrary new number.
    private static final int MAX_SCROLL = 3333;

    // provided separately - drop the PNG at src/main/resources/assets/mmorpg/textures/gui/atlas_map/background.png
    private static final ResourceLocation BACKGROUND = SlashRef.guiId("atlas_map/background");

    private final Map<String, AtlasNodeButton> nodeButtons = new HashMap<>();
    private final Map<String, PointData> nodePoints = new HashMap<>();
    private AtlasNodeLayout.CalcData layout;

    private int originX;
    private int originY;
    private int maxScrollX;
    private int maxScrollY;
    public int scrollX = 0;
    public int scrollY = 0;

    // 1 = default/full size, matching the screen's pre-zoom layout exactly, down to 0.15 zoomed
    // out - same range/step/smoothing as SkillTreeScreen's zoom, so it feels consistent across
    // both Atlas screens
    public float zoom = 1F;
    public float targetZoom = zoom;

    private AtlasNavButton navButton;

    // computed once in init() from that AtlasNode/AtlasData snapshot - the player can't earn
    // progress while this screen is open, so no need to recompute every frame
    private int pinnacleDone;
    private int pinnacleTotal;
    private boolean pinnacleUnlocked;

    public AtlasMapScreen() {
        super(Minecraft.getInstance().getWindow().getGuiScaledWidth(), Minecraft.getInstance().getWindow().getGuiScaledHeight());
    }

    @Override
    protected void init() {
        super.init();
        nodeButtons.clear();
        nodePoints.clear();
        scrollX = 0;
        scrollY = 0;

        AtlasData atlas = Load.player(ClientOnly.getPlayer()).atlas;
        layout = AtlasNodeLayout.mainCalcData();

        // only count pinnacle nodes actually placed on this layout, not every registered one
        var placed = layout.pointOf.keySet();
        var pinnacleNodes = DungeonDatabase.AtlasNodes().getList().stream()
                .filter(n -> n.is_pinnacle_unlock)
                .filter(n -> placed.contains(n.id))
                .toList();
        pinnacleTotal = pinnacleNodes.size();
        pinnacleDone = (int) pinnacleNodes.stream().filter(n -> atlas.isCompleted(n.id)).count();
        pinnacleUnlocked = atlas.pinnacleUnlocked;

        var points = layout.nodes.keySet();

        int minX = points.stream().mapToInt(p -> p.x).min().orElse(0);
        int maxX = points.stream().mapToInt(p -> p.x).max().orElse(0);
        int minY = points.stream().mapToInt(p -> p.y).min().orElse(0);
        int maxY = points.stream().mapToInt(p -> p.y).max().orElse(0);

        int graphCenterX = (minX + maxX) * PIXELS_PER_CELL / 2;
        int graphCenterY = (minY + maxY) * PIXELS_PER_CELL / 2;

        originX = this.width / 2 - graphCenterX;
        originY = this.height / 2 - graphCenterY;

        maxScrollX = MAX_SCROLL;
        maxScrollY = MAX_SCROLL;

        for (Map.Entry<PointData, String> entry : layout.nodes.entrySet()) {
            AtlasNode node = DungeonDatabase.AtlasNodes().get(entry.getValue());
            if (node == null) {
                continue;
            }
            Component name = resolveName(node);
            NodeState state = computeState(atlas, node);
            AtlasNodeButton btn = new AtlasNodeButton(this, 0, 0, node, state, name);
            nodeButtons.put(node.id, btn);
            nodePoints.put(node.id, entry.getKey());
            addRenderableWidget(btn);
        }

        updateNodePositions();

        // registered for input/hover only (not addRenderableWidget) - render() draws all
        // renderables inside a gui.pose().scale(zoom, ...) transform so the tree can zoom, but the
        // nav button should stay fixed-size/unzoomed in the corner like the rest of the screen
        // chrome - rendered manually below, after the scale is popped back to 1:1
        navButton = new AtlasNavButton(4, 4, this, new AtlasPassiveTreeScreen());
        addWidget(navButton);
    }

    private void updateNodePositions() {
        // re-centers the zoom on screen middle, same formula SkillTreeScreen uses - node
        // positions/scroll stay in unzoomed tree space, this term shifts them so scaling the pose
        // around the origin still visually zooms around the screen center instead of the corner
        float addx = (1F / zoom - 1) * this.width / 2F;
        float addy = (1F / zoom - 1) * this.height / 2F;
        for (Map.Entry<String, AtlasNodeButton> entry : nodeButtons.entrySet()) {
            PointData p = nodePoints.get(entry.getKey());
            int x = (int) (originX + p.x * PIXELS_PER_CELL - AtlasNodeButton.SIZE / 2 + scrollX + addx);
            int y = (int) (originY + p.y * PIXELS_PER_CELL - AtlasNodeButton.SIZE / 2 + scrollY + addy);
            entry.getValue().setX(x);
            entry.getValue().setY(y);
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
        // divide by zoom so a given mouse-pixel drag always covers the same visual screen
        // distance regardless of zoom level, matching SkillTreeScreen's pan behavior
        scrollX = Mth.clamp((int) (scrollX + dragX / zoom), -maxScrollX, maxScrollX);
        scrollY = Mth.clamp((int) (scrollY + dragY / zoom), -maxScrollY, maxScrollY);
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (scroll < 0) {
            targetZoom -= 0.1F;
        }
        if (scroll > 0) {
            targetZoom += 0.1F;
        }
        targetZoom = Mth.clamp(targetZoom, 0.15F, 1F);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderAtlasBackground(graphics);
        zoom = Mth.lerp(ClientConfigs.getConfig().SKILL_TREE_ZOOM_SPEED.get().floatValue(), zoom, targetZoom);
        updateNodePositions();

        graphics.pose().scale(zoom, zoom, zoom);
        renderConnections(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderLabels(graphics);
        graphics.pose().scale(1F / zoom, 1F / zoom, 1F / zoom);

        navButton.render(graphics, mouseX, mouseY, partialTick);
        renderPinnacleProgress(graphics);
    }

    // fixed/unzoomed corner HUD text, same treatment as navButton - not tied to any node's
    // position so it doesn't belong inside the zoomed/scrolled tree render block
    private void renderPinnacleProgress(GuiGraphics graphics) {
        if (pinnacleTotal <= 0) {
            return;
        }
        Component text = pinnacleUnlocked
                ? Component.literal("Pinnacle Unlocked").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                : Component.literal("Pinnacle Progress: " + pinnacleDone + "/" + pinnacleTotal).withStyle(ChatFormatting.DARK_RED);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, text, this.width - font.width(text) - 8, 8, 0xFFFFFFFF, true);
    }

    // same blit shape as SkillTreeScreen.renderBackgroundDirt, pointed at the Atlas map's own texture
    private void renderAtlasBackground(GuiGraphics gui) {
        gui.blit(BACKGROUND, 0, 0, -10, 0.0F, 0.0F, this.width, this.height, 32, 32);
    }

    private static final float LABEL_TEXT_SCALE = 1.0F;

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

            float centerX = btn.getX() + btn.getWidth() / 2F;
            float y = btn.getY() + btn.getHeight() + 3;

            graphics.pose().pushPose();
            graphics.pose().translate(centerX, y, 0);
            graphics.pose().scale(LABEL_TEXT_SCALE, LABEL_TEXT_SCALE, 1F);
            graphics.drawCenteredString(Minecraft.getInstance().font, label, 0, 0, nameColor);
            graphics.pose().popPose();
        }
    }

    private void renderConnections(GuiGraphics graphics) {
        for (Map.Entry<PointData, Set<PointData>> entry : layout.connections.entrySet()) {
            PointData from = entry.getKey();
            AtlasNodeButton fromBtn = nodeButtons.get(layout.nodes.get(from));
            if (fromBtn == null) {
                continue;
            }
            for (PointData to : entry.getValue()) {
                if (from.x > to.x || (from.x == to.x && from.y >= to.y)) {
                    continue; // draw each edge once
                }
                AtlasNodeButton toBtn = nodeButtons.get(layout.nodes.get(to));
                if (toBtn == null) {
                    continue;
                }
                int color = lineColor(fromBtn.state, toBtn.state);
                drawLine(graphics,
                        fromBtn.getX() + fromBtn.getWidth() / 2F, fromBtn.getY() + fromBtn.getHeight() / 2F,
                        toBtn.getX() + toBtn.getWidth() / 2F, toBtn.getY() + toBtn.getHeight() / 2F, color);
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
