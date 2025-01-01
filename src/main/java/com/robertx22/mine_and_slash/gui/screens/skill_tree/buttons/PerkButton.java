package com.robertx22.mine_and_slash.gui.screens.skill_tree.buttons;

import com.google.common.collect.HashMultimap;
import com.mojang.blaze3d.systems.RenderSystem;
import com.robertx22.library_of_exile.utils.Watch;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import com.robertx22.mine_and_slash.database.data.perks.Perk;
import com.robertx22.mine_and_slash.database.data.perks.PerkStatus;
import com.robertx22.mine_and_slash.database.data.stats.types.UnknownStat;
import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.BufferInfo;
import com.robertx22.mine_and_slash.gui.screens.skill_tree.SkillTreeScreen;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.PointData;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.ModRange;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.StatRangeInfo;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.vanilla_mc.packets.perks.PerkChangePacket;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.GuiUtils;
import com.robertx22.library_of_exile.utils.TextUTIL;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.time.StopWatch;
import org.joml.Matrix4f;

import java.util.List;

public class PerkButton extends ImageButton {

    public static int SPACING = 26;
    public static int BIGGEST = 33;

    static ResourceLocation ID = new ResourceLocation(SlashRef.MODID, "textures/gui/skill_tree/perk_buttons.png");
    public static ResourceLocation LOCKED_TEX = new ResourceLocation(SlashRef.MODID, "textures/gui/locked.png");

    public Perk perk;
    public PointData point;
    public TalentTree school;
    public PlayerData playerData;

    public int originalWidth;
    public int originalHeight;

    public int origX;
    public int origY;
    Minecraft mc = Minecraft.getInstance();
    SkillTreeScreen screen;

    public String perkid = "";

    public PerkButton(SkillTreeScreen screen, PlayerData playerData, TalentTree school, PointData point, Perk perk, int x, int y) {
        super(x, y, perk.getType().size, perk.getType().size, 0, 0, 1, ID, (action) -> {
        });
        this.perk = perk;
        this.point = point;
        this.school = school;
        this.playerData = playerData;

        this.origX = x;
        this.origY = y;
        this.originalWidth = this.width;
        this.originalHeight = this.height;
        this.screen = screen;

    }

    public boolean isInside(int x, int y) {

        float scale = 2 - screen.zoom;
        return GuiUtils.isInRect((int) (this.getX() - ((width / 4) * scale)), (int) (this.getY() - ((height / 4) * scale)), (int) (width * scale), (int) (height * scale), x, y);
    }


    private void setTooltipMOD(GuiGraphics gui, int mouseX, int mouseY) {

        int MmouseX = (int) (1F / screen.zoom * mouseX);
        int MmouseY = (int) (1F / screen.zoom * mouseY);

        if (this.isInside(MmouseX, MmouseY)) {

            List<Component> tooltip = perk.GetTooltipString(new StatRangeInfo(ModRange.hide()));

            if (perk.stats.stream().anyMatch(x -> x.stat.equals(new UnknownStat().GUID()))) {
                tooltip.add(Component.literal("No Perk of this ID found: " + perkid));
            } else {
                if (MMORPG.RUN_DEV_TOOLS || Screen.hasShiftDown()) {
                    tooltip.add(Component.literal("Perk ID: " + perkid));
                }
            }

            setTooltip(Tooltip.create(TextUTIL.mergeList(tooltip)));

            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                screen.setTooltipForNextRenderPass(this.getTooltip(), this.createTooltipPositioner(), true);
            }
            //GuiUtils.renderTooltip(gui, tooltip, mouseX, mouseY);
        } else {
            setTooltip(null);
        }
    }

    // copied from abstractbutton
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {


        screen.mouseRecentlyClickedTicks = 25;
        screen.pointClicked = this.point;

        mouseX = 1F / screen.zoom * mouseX;
        mouseY = 1F / screen.zoom * mouseY;

        if (this.active && this.visible) {
            boolean bl = this.clicked(mouseX, mouseY);
            if (bl) {
//                ExileLog.get().log(this.getX() + "_" + getY() + " : " + perk.GUID());


                this.playDownSound(Minecraft.getInstance()
                        .getSoundManager());

                if (button == 0) {
                    Packets.sendToServer(new PerkChangePacket(school, point, PerkChangePacket.ACTION.ALLOCATE));
                }
                if (button == 1) {
                    Packets.sendToServer(new PerkChangePacket(school, point, PerkChangePacket.ACTION.REMOVE));
                }
                this.onClick(mouseX, mouseY);

                return true;
            }

            return false;
        } else {
            return false;
        }
    }

    float xPos(float offset, float multi) {
        return ((this.getX() * multi) + offset);

    }

    float yPos(float offset, float multi) {
        return ((getY() * multi) + offset);
    }

    @Override
    public void renderWidget(GuiGraphics gui, int mouseX, int mouseY, float pPartialTick) {


        if (!screen.shouldRender(getX(), getY(), screen.ctx)) {
            return;
        }

        setTooltipMOD(gui, mouseX, mouseY);


        gui.pose().pushPose();

        float scale = getScale(mouseX, mouseY);

        float posMulti = 1F / scale;


        float add = MathHelper.clamp(scale - 1, 0, 2);
        float off = width / -2F * add;
        gui.pose().translate(off, off, 0);
        gui.pose().scale(scale, scale, 1.0f);


        PerkStatus status = playerData.talents.getStatus(Minecraft.getInstance().player, school, point);

        float offset = perk.getType().getOffset();

        // background

        RenderSystem.enableDepthTest();

        var search = SkillTreeScreen.SEARCH.getValue();


        float opacity;

        if (!search.isEmpty()) {
            if (search.equals("all")) {
                if (status != PerkStatus.CONNECTED) {
                    opacity = 0.2F;
                } else {
                    opacity = 1;
                }
            } else {
                boolean containsSearchStat = perk.stats.stream()
                        .anyMatch(item -> item.getStat().locName().getString().toLowerCase().contains(search.toLowerCase()));

                boolean containsName = perk.locName().getString().toLowerCase().contains(search.toLowerCase());
                opacity = containsSearchStat || containsName ? 1F : 0.2f;
            }
        } else {
            opacity = status.getOpacity();
        }

        // if newbie, show only the starter perks he can pick
        if (playerData.talents.getAllocatedPoints(TalentTree.SchoolType.TALENTS) < 1) {
            opacity = this.perk.getType() == Perk.PerkType.START ? 1 : 0.2F;
        }

        var type = perk.type;

        //gui.blit(ID, xPos(0, posMulti), yPos(0, posMulti), perk.getType().getXOffset(), status.getYOffset(), this.width, this.height);

        int offcolor = (int) ((perk.getType().size - 20) / 2F);

        //gui.setColor(1.0F, 1.0F, 1.0F, opacity);

        HashMultimap<ResourceLocation, BufferInfo> container = screen.vertexContainer.map;

        //BlitOffset indicate the distance to the camera.
        //bigger = closer
        container.put(perk.getType().getColorTexture(status), BufferInfo.of(xPos(offcolor, posMulti), yPos(offcolor, posMulti),20, 20, -3, 0.0f, 0, 20, 20, 20, 20, gui.pose().last().pose()).withRenderInfo(new BufferInfo.RenderInfo(opacity)));

        container.put(perk.getType().getBorderTexture(status), BufferInfo.of(xPos(0, posMulti), yPos(0, posMulti), -2, 0, 0, this.width, this.height, this.width, this.height, gui.pose().last().pose()).withRenderInfo(new BufferInfo.RenderInfo(opacity)));



        if (search.isEmpty()) {
            opacity += 0.2F;
        }


        //gui.setColor(1.0F, 1.0F, 1.0F, MathHelper.clamp(opacity, 0, 1));

        container.put(perk.getIcon(), BufferInfo.of(xPos(offset, posMulti), yPos(offset, posMulti), -1, 0, 0, type.iconSize, type.iconSize, type.iconSize, type.iconSize, gui.pose().last().pose()).withRenderInfo(new BufferInfo.RenderInfo(opacity)));



        //   gui.pose().scale(1F / scale, 1F / scale, 1F / scale);
        //gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        gui.pose().popPose();

    }

    private float getScale(int mouseX, int mouseY) {
        float scale = 2 - screen.zoom;
        if (isInside((int) (1F / screen.zoom * mouseX), (int) (1F / screen.zoom * mouseY))) {
            scale = MathHelper.clamp(scale * 1.3f, 1.7f, 2.0f);
        }

        return MathHelper.clamp(scale, 1.5f, 2.0f);
    }

}