package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.robertx22.library_of_exile.utils.Watch;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.stats.types.UnknownStat;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree.SchoolType;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.bases.BackToHubButton;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.IAlertScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons.PerkButton;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons.PerkConnectionRender;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons.PerkPointPair;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons.PerkScreenContext;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.component.TipsWidget;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.PointData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Gui;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;

public abstract class SkillTreeScreen extends BaseScreen implements INamedScreen, IAlertScreen {
    static ResourceLocation BIG_PANEL = new ResourceLocation(SlashRef.MODID, "textures/gui/skill_tree/bar.png");

    public SchoolType schoolType;

    public HashSet<PerkConnectionRender> buttonConnections = new HashSet<>();

    static ResourceLocation CON = SlashRef.id("textures/gui/skill_tree/skill_connection.png");
    public VertexContainer vertexContainer = new VertexContainer();
    private TipsWidget tips;

    // top-left "back to the Main Hub" button. Registered for input only and drawn manually after the
    // zoom pose is popped - see the note on SEARCH/tips for why renderable widgets can't be used here.
    private BackToHubButton backButton;

    // subclasses that already occupy the top-left corner can opt out - see AtlasPassiveTreeScreen
    protected boolean showBackToHubButton() {
        return true;
    }


    private void renderConnection(GuiGraphics graphics, PerkConnectionRender renderer) {

        graphics.pose().pushPose();
        PerkPointPair pair = renderer.pair();
        PerkButton button1 = pointPerkButtonMap.get(pair.data1());
        PerkButton button2 = pointPerkButtonMap.get(pair.data2());

        // reuse the screen's per-frame context instead of allocating one per connection per frame
        PerkScreenContext ctx = this.ctx;

        double xadd = button1.perk.getType().size / 2F; // todo idk if this is the problem or
        double yadd = button1.perk.getType().size / 2F;


        double connectionX = button1.getX() + xadd;
        double connectionY = button1.getY() + yadd;

        connectionX -= ctx.scrollX;
        connectionY -= ctx.scrollY;


        graphics.pose().translate(connectionX + scrollX, connectionY + scrollY, 0);
        float rotation = getAngleBetweenButtons(button1, button2);
        graphics.pose().mulPose(Axis.ZP.rotation(rotation));
        int length = (int) getDistanceBetweenButtons(button1, button2);

        //graphics.pose().scale(1F, 1.5F, 1F); // thicken it a bit

        int off = 0;

        if (renderer.connection() == Perk.Connection.LINKED) {
            off = 0;
        }
        if (renderer.connection() == Perk.Connection.POSSIBLE) {
            off = 6;
        }
        if (renderer.connection() == Perk.Connection.BLOCKED) {
            off = (6 + 5);
        }

        this.vertexContainer.put(VertexContainer.LAYER_CONNECTION,
                SlashRef.id("textures/gui/skill_tree/skill_connection.png"),
                BufferInfo.of(0, -3, length, 6, -5, (float) 0, off, length, 6, 50, 16, graphics.pose().last().pose()));


        //graphics.blit(CON, 0, -3, length, 6, 0, off, length, 6, 50, 16);

        graphics.pose().popPose();
    }

    protected float getDistanceBetweenButtons(PerkButton button1, PerkButton button2) {
        float x1 = button1.getX() + button1.getWidth() / 2F;
        float y1 = button1.getY() + button1.getHeight() / 2F;
        float x2 = button2.getX() + button2.getWidth() / 2F;
        float y2 = button2.getY() + button2.getHeight() / 2F;
        return getDistanceBetweenPoints(x1, y1, x2, y2);
    }

    protected float getDistanceBetweenPoints(float x1, float y1, float x2, float y2) {
        return Mth.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }


    protected float getAngleBetweenButtons(PerkButton button1, PerkButton button2) {
        float x1 = button1.getX() + button1.getWidth() / 2F;
        float y1 = button1.getY() + button1.getHeight() / 2F;
        float x2 = button2.getX() + button2.getWidth() / 2F;
        float y2 = button2.getY() + button2.getHeight() / 2F;
        return getAngleBetweenPoints(x1, y1, x2, y2);
    }

    protected float getAngleBetweenPoints(float x1, float y1, float x2, float y2) {
        return (float) Mth.atan2(y2 - y1, x2 - x1);
    }

    public SkillTreeScreen(SchoolType type) {
        super(Minecraft.getInstance()
                .getWindow()
                .getGuiScaledWidth(), Minecraft.getInstance()
                .getWindow()
                .getGuiScaledHeight());
        this.schoolType = type;

    }

    public PointData pointClicked = new PointData(0, 0);
    public int mouseRecentlyClickedTicks = 0;

    @Override
    public boolean mouseReleased(double x, double y, int ticks) {

        mouseRecentlyClickedTicks = 25;

        return super.mouseReleased(x, y, ticks);

    }

    public static int sizeX() {
        return Minecraft.getInstance().getWindow().getWidth();
    }

    public static int sizeY() {
        return Minecraft.getInstance().getWindow().getHeight();
    }

    int ticks = 0;


    private void renderConnections(GuiGraphics gui) {
        for (PerkConnectionRender con : this.buttonConnections) {
            this.renderConnection(gui, con);
        }
    }


    // Culling for perk buttons. This used to compare button coordinates (which are gui-scaled tree
    // space) against sizeX()/sizeY(), which return RAW window pixels - on any setup with a gui scale
    // above 1 that made the cull box several times larger than the screen, so it barely culled
    // anything. That is the "todo this doesnt seem to work perfect" the old comment referred to, and
    // on the talents tree (961 buttons) it meant almost every button paid full render cost every
    // frame. Ascendancy has 212 and atlas passives 10, which is why only talents felt it.
    //
    // Buttons live in unzoomed tree space and the whole tree is drawn inside a pose scaled by zoom,
    // so a button at tree-space x lands on screen at x * zoom. Cull against the real gui-scaled
    // window with a one-node margin so partially visible nodes still draw.
    private static final float CULL_MARGIN = 64;

    public boolean shouldRender(int x, int y, PerkScreenContext ctx) {

        float sx = x * ctx.zoom;
        float sy = y * ctx.zoom;

        return sx >= -CULL_MARGIN && sx <= mc.getWindow().getGuiScaledWidth() + CULL_MARGIN
                && sy >= -CULL_MARGIN && sy <= mc.getWindow().getGuiScaledHeight() + CULL_MARGIN;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32) { // space
            this.goToCenter();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);

    }

    int tick_count = 0;
    public int scrollX = 0;
    public int scrollY = 0;

    HashMap<AbstractWidget, PointData> originalButtonLocMap = new HashMap<>();
    HashMap<PointData, PerkButton> pointPerkButtonMap = new HashMap<>();


    public Minecraft mc = Minecraft.getInstance();

    PlayerData playerData = Load.player(mc.player);

    public TalentTree school;

    private static int SEARCH_WIDTH = 100;
    private static int SEARCH_HEIGHT = 14;
    public static EditBox SEARCH = new EditBox(Minecraft.getInstance().font, 0, 0, SEARCH_WIDTH, SEARCH_HEIGHT, Component.translatable("fml.menu.mods.search"));

    public static boolean searchFocused() {
        if (SkillTreeScreen.SEARCH != null) {
            return SkillTreeScreen.SEARCH.isFocused();
        } else {
            // Handle the case when search is not initialized
            return false;
        }
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        SEARCH.setFocused(true);
        return SEARCH.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public void tick() {
        SEARCH.tick();
    }

    @Override
    protected void init() {
        super.init();

        try {


            SkillTreeScreen.SEARCH.setFocused(false);
            SkillTreeScreen.SEARCH.setCanLoseFocus(true);

            this.school = ExileDB.TalentTrees().getFilterWrapped(x -> x.getSchool_type().equals(this.schoolType)).list.get(0);

            refreshButtons();
            this.tips = new TipsWidget(0, 0, 10, 10, Gui.TALENT_SCREEN_SEARCH_TIPS.locName(Gui.TALENT_SCREEN_SEARCH_KEYWORD_ALL.locName().withStyle(ChatFormatting.GOLD), Gui.TALENT_SCREEN_SEARCH_KEYWORD_GAME_CHANGER.locName().withStyle(ChatFormatting.GOLD)));
            addWidget(tips);

            if (showBackToHubButton()) {
                this.backButton = new BackToHubButton(4, 4);
                addWidget(this.backButton);
            }

            goToCenter();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void addButtonPublic(AbstractWidget b) {
        this.addRenderableWidget(b);
    }

    private void addConnections() {

        buttonConnections = new HashSet<>(3000);


        HashSet<PerkPointPair> integers = new HashSet(3000);
        children().forEach(b -> {
            if (b instanceof PerkButton pb) {

                Set<PointData> connections = this.school.calcData.connections.getOrDefault(pb.point, Collections.EMPTY_SET);

                for (PointData p : connections) {

                    PerkButton sb = this.pointPerkButtonMap.get(p);
                    PerkPointPair pair = new PerkPointPair(pb.point, sb.point);
                    if (!integers.contains(pair)) {

                        var con = this.playerData.talents.getConnection(this.school, sb.point, pb.point);
                        var result = new PerkConnectionRender(pair, con);
                        buttonConnections.add(result);
                        integers.add(pair);
                    }

                }
            }

        });

    }


    public void refreshButtons() {

        //Watch watch = new Watch();

        originalButtonLocMap.clear();
        pointPerkButtonMap.clear();

        // this.buttons.clear();
        this.clearWidgets();

        this.scrollX = 0;
        this.scrollY = 0;

        for (Map.Entry<PointData, String> e : school.calcData.perks.entrySet()) {

            Perk perk = ExileDB.Perks().get(e.getValue());

            if (perk == null) {
                perk = ExileDB.Perks().get(new UnknownStat().GUID()); // we show unknown stat so its visible ingame that something is wrong on the GUI
                //continue;
            }

            try {

                var pos = getPosForPoint(e.getKey(), perk);

                int x = pos.x;
                int y = pos.y;

                var button = new PerkButton(this, playerData, school, e.getKey(), perk, x, y);

                button.perkid = e.getValue();

                this.newButton(button);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        addConnections();

        this.addWidget(SEARCH);

    }

    private Point getPosForPoint(PointData point, Perk perk) {

        float halfx = mc.getWindow().getGuiScaledWidth() / 2F;
        float halfy = mc.getWindow().getGuiScaledHeight() / 2F;

        float x = (point.x - school.calcData.center.x) * PerkButton.SPACING;
        float y = (point.y - school.calcData.center.y) * PerkButton.SPACING;

        // todo
        x -= perk.getType().size / 2F;
        y -= perk.getType().size / 2F;

        float tx = (int) (halfx + x);
        float ty = (int) (halfy + y);


        return new Point((int) tx, (int) ty);

    }

    public void goToCenter() {
        this.scrollX = 0;
        this.scrollY = 0;
    }

    private void newButton(AbstractWidget b) {

        this.addButtonPublic(b);

        originalButtonLocMap.put(b, new PointData(b.getX(), b.getY()));
        if (b instanceof PerkButton) {
            this.pointPerkButtonMap.put(((PerkButton) b).point, (PerkButton) b);
        }
    }


    public float zoom = 0.3F;
    public float targetZoom = zoom;


    @Override
    public void onClose() {

        super.onClose();

        resetZoom();

        SkillTreeScreen.SEARCH.setFocused(false);

    }

    @Override
    public void removed() {
        super.removed();

        resetZoom();

    }

    private void resetZoom() {
        zoom = 1;
        targetZoom = zoom;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (scroll < 0) {
            targetZoom -= 0.1F;
        }
        if (scroll > 0) {
            targetZoom += 0.1F;
        }

        this.targetZoom = Mth.clamp(targetZoom, 0.15F, 1);

        this.zoom = targetZoom;

        return true;
    }

    public PointData getZoomedPosition(PointData pos) {
        return new PointData((int) (this.zoom / (float) pos.x), (int) (this.zoom / (float) pos.y));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.scrollX += 1F / zoom * deltaX;
        this.scrollY += 1F / zoom * deltaY;

        scrollY = Mth.clamp(scrollY, -3333, 3333);

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    public PerkScreenContext ctx = new PerkScreenContext(this);

    // Recomputed once per frame in render() and read by every PerkButton. Both values are identical
    // for every perk on the screen, but they used to be derived per button inside
    // TalentsData.canAllocate -> hasFreePoints -> PlayerPointsType.getFreePoints, which calls
    // GameBalanceConfig.get() twice (each a Forge config read) plus several capability resolutions.
    // On the talents tree that was ~2000 Forge config reads per frame to produce one number.
    public boolean cachedHasFreePoints = false;
    public int cachedAllocatedPoints = 0;

    private void refreshPerFrameCache() {
        try {
            this.cachedHasFreePoints = this.schoolType.getPointType().getFreePoints(mc.player) > 0;
            this.cachedAllocatedPoints = this.playerData.talents.getAllocatedPoints(this.schoolType);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {

        Watch watch = new Watch();


        ctx = new PerkScreenContext(this);
        refreshPerFrameCache();

        // String searchTerm = SkillTreeScreen.SEARCH.getValue();

        // Watch watch = new Watch();
        mouseRecentlyClickedTicks--;

        renderBackgroundDirt(gui, this, 0);
        zoom = Mth.lerp(ClientConfigs.getConfig().SKILL_TREE_ZOOM_SPEED.get().floatValue(), zoom, targetZoom);
        gui.pose().scale(zoom, zoom, zoom);

        try {

            float addx = (1F / zoom - 1) * this.width / 2F;
            float addy = (1F / zoom - 1) * this.height / 2F;

            for (Renderable e : this.renderables) {
                if (e instanceof PerkButton b)
                    if (originalButtonLocMap.containsKey(b)) {

                        int xp = (int) (originalButtonLocMap.get(b).x + addx + scrollX);
                        int yp = (int) (originalButtonLocMap.get(b).y + addy + scrollY);

                        b.setX(xp);
                        b.setY(yp);

                        //b.search = searchTerm;
                    }
            }


            this.renderConnections(gui);

            ticks++;

            if (mouseRecentlyClickedTicks > 1) {
                addConnections();
                mouseRecentlyClickedTicks = 0;
            }


            super.render(gui, x, y, ticks);
            // draw
            this.vertexContainer.draw(gui.bufferSource());
            this.vertexContainer.refresh();
            this.tick_count++;

        } catch (Exception e) {
            e.printStackTrace();
        }


        gui.pose().scale(1F / zoom, 1F / zoom, 1F / zoom);

        // header chrome (bar, point counts, search box, tips) is drawn AFTER the tree, not before.
        // Everything above renders into the same GUI layer with no depth write, so drawing the bar
        // first just meant the tree's nodes and connection lines painted straight over the search box.
        renderPanels(gui);
        tips.render(gui, x, y, ticks);

        // real mouse coords so ImageButton picks up its hover frame
        if (backButton != null) {
            backButton.render(gui, x, y, ticks);
        }

        this.msstring = watch.getPrint();

        //watch.print(" rendering ");
    }


    String msstring = "";

    static ResourceLocation BACKGROUND = SlashRef.guiId("skill_tree/background");

    public static void renderBackgroundDirt(GuiGraphics gui, Screen screen, int vOffset) {
        //copied from Screen

        Minecraft mc = Minecraft.getInstance();

        // todo test
        //gui.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        int i = 32;
        gui.blit(BACKGROUND, 0, 0, -10, 0.0F, 0.0F, mc.screen.width, mc.screen.height, 32, 32);
        //gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

    }


    private void renderPanels(GuiGraphics gui) {

        Minecraft mc = Minecraft.getInstance();


        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        int BG_WIDTH = 256;
        int BG_HEIGHT = 22;

        int xp = (int) (mc.getWindow().getGuiScaledWidth() / 2F - BG_WIDTH / 2F);
        int yp = 0;

        gui.blit(BIG_PANEL, xp, yp, 0, 0, BG_WIDTH, BG_HEIGHT);

        RenderSystem.depthFunc(GL11.GL_LEQUAL);

        int savedx = xp;
        int savedy = yp;


        int points = schoolType.getPointType().getFreePoints(mc.player);

        MutableComponent text = Gui.TALENT_POINTS.locName().append(points + "");

        int yx = 4;

        gui.drawString(mc.font, text, savedx + 15, yx, ChatFormatting.YELLOW.getColor());
        text = Gui.TALENT_RESET_POINTS.locName().append(String.valueOf(this.schoolType.getPointType().getResetPoints(mc.player)));
        gui.drawString(mc.font, text, savedx + (BG_WIDTH) - mc.font.width(text) - 15, yx, ChatFormatting.YELLOW.getColor());

        int tx = savedx + BG_WIDTH;
        tx = (tx - savedx) / 2 + savedx - SEARCH_WIDTH / 2;
        yx = yx - (SEARCH_HEIGHT - mc.font.lineHeight) / 2;

        SkillTreeScreen.SEARCH.setX(tx);
        SkillTreeScreen.SEARCH.setY(yx);
        SkillTreeScreen.SEARCH.render(gui, 0, 0, 0);

        tips.setX(tx + SkillTreeScreen.SEARCH.getWidth() - tips.getWidth() - 2);
        tips.setY(yx + (SkillTreeScreen.SEARCH.getHeight() - tips.getHeight()) / 2);


        if (MMORPG.RUN_DEV_TOOLS) {
            MutableComponent debug = Component.literal("Widgets: " + this.children().size() + " - " + msstring);
            gui.drawString(mc.font, debug, savedx + 277, yx, ChatFormatting.GREEN.getColor());
        }


    }

}
