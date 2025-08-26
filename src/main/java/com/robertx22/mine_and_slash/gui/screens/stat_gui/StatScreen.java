package com.robertx22.mine_and_slash.gui.screens.stat_gui;

import com.robertx22.mine_and_slash.a_libraries.neat.HealthBarRenderer;
import com.robertx22.mine_and_slash.a_libraries.neat.NeatConfig;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import com.robertx22.mine_and_slash.database.data.stats.StatGuiGroup;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.Armor;
import com.robertx22.mine_and_slash.database.data.stats.types.defense.DodgeRating;
import com.robertx22.mine_and_slash.database.data.stats.types.generated.ElementalResist;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.health.Health;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.mana.Mana;
import com.robertx22.mine_and_slash.gui.bases.BaseScreen;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.unit.StatData;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class StatScreen extends BaseScreen implements INamedScreen {
    static ResourceLocation BG = SlashRef.guiId("stat_gui/background");

    private LivingEntity target;

    public StatScreen(LivingEntity target) {
        super(199, 222);
        this.target = target;
    }


    @Override
    public void render(GuiGraphics gui, int x, int y, float ticks) {
        gui.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (target != ClientOnly.getPlayer()) {
            // Show entity being viewed
            int paperDollX = this.guiLeft - 88;
            int paperDollY = this.guiTop + sizeY / 2 + 30;
            float mouseOffsetX = (float) (paperDollX - x);
            float mouseOffsetY = (float) (paperDollY - 50 - y);

            boolean neatDraw = NeatConfig.draw;
            NeatConfig.draw = false; // don't draw health bar
            InventoryScreen.renderEntityInInventoryFollowsMouse(gui, paperDollX, paperDollY, 30, mouseOffsetX, mouseOffsetY, target);
            NeatConfig.draw = neatDraw;

            Component nameText = HealthBarRenderer.getNameString(target, target, mc);
            int nameTextX = paperDollX - mc.font.width(nameText) / 2;
            int nameTextY = paperDollY + 5;
            gui.drawString(mc.font, nameText, nameTextX, nameTextY, ChatFormatting.WHITE.getColor());
        }

        gui.blit(BG, mc.getWindow().getGuiScaledWidth() / 2 - sizeX / 2, mc.getWindow().getGuiScaledHeight() / 2 - sizeY / 2, 0, 0, sizeX, sizeY);
        super.render(gui, x, y, ticks);

        SEARCH.setX(this.guiLeft - (SEARCH_WIDTH / 2) + sizeX / 2);
        SEARCH.setY(this.guiTop - SEARCH_HEIGHT - 5);
        SEARCH.render(gui, 0, 0, 0);
    }

    private static int SEARCH_WIDTH = 100;
    private static int SEARCH_HEIGHT = 14;
    public static EditBox SEARCH = new EditBox(Minecraft.getInstance().font, 0, 0, SEARCH_WIDTH, SEARCH_HEIGHT,
            Component.translatable("fml.menu.mods.search"));


    int currentElement = 0;
    public List<Stat> stats = new ArrayList<>();
    public List<Stat> searched = new ArrayList<>();
    //   int elementsAmount = 1;


    public void setupStatButtons() {
        this.renderables.removeIf(x -> x instanceof EditBox == false);
        this.children().removeIf(x -> x instanceof EditBox == false);

        //    this.children().clear();
        //  this.renderables.clear();

        int secX = guiLeft + 9;
        int secY = guiTop + 18;

        for (StatGuiGroupSection sec : StatGuiGroupSection.values()) {
            this.publicAddButton(new StatSectionButton(this, sec, secX, secY));
            secY += StatSectionButton.ySize + 2;
        }


        //  this.children().removeIf(x -> x instanceof StatPanelButton || x instanceof StatIconAndNumberButton);

        int x = this.guiLeft + 30;
        int y = this.guiTop + 16;
        int yNavigation = y;
        int xNavigation = this.guiLeft + this.sizeX;


        int spaceleft = 143;
        int yNavigationDownOffset = spaceleft;

        var data = Load.Unit(target);

        int addedAmount = 0;
        for (int i = currentElement; i < currentElement + 15; i++) {
            if (i >= this.stats.size()) {
                continue;
            } else {
                if (searched.size() > i) {
                    Stat entry = searched.get(i);
                    int ysize = entry.getStatGuiPanelButtonYSize() + 3;

                    if (spaceleft >= ysize) {
                        var stat = data.getUnit().getCalculatedStat(entry);
                        if (stat.GetStat() != null) {
                            this.publicAddButton(new StatPanelButton(this, stat, x, y));
                            y += ysize;
                            spaceleft -= ysize;
                            addedAmount++;
                        }
                    }
                }

            }

        }


        if (currentElement > 0) {
            this.publicAddButton(new StatDirectionNavigationButton(this, xNavigation, yNavigation, 1, false));
        }

        if (currentElement + addedAmount < searched.size()) {
            this.publicAddButton(new StatDirectionNavigationButton(this, xNavigation, yNavigation + yNavigationDownOffset - StatDirectionNavigationButton.ySize, 1, true));
        }
    }

    @Override
    public boolean mouseScrolled(double num1, double num2, double num3) {

        this.setCurrentElement((int) (currentElement - num3));

        return super.mouseScrolled(num1, num2, num3);

    }

    public void setCurrentElement(int element) {
        this.currentElement = MathHelper.clamp(element, 0, searched.size());
        setupStatButtons();
    }


    // todo

    public void setInfo(StatData stat) {

        this.renderables.removeIf(x -> x instanceof IStatInfoButton);
        this.children().removeIf(x -> x instanceof IStatInfoButton);


        int x = guiLeft + 38;
        int y = guiTop + 172;

        for (StatInfoButton.StatInfoType type : StatInfoButton.StatInfoType.values()) {
            if (type.shouldShow(stat)) {
                this.publicAddButton(new StatInfoButton(this, type, stat, x, y));
                x += StatInfoButton.xSize + 12;
            }
        }

    }

    @Override
    public void tick() {
        SEARCH.tick();

    }

    public void showStats(List<Stat> stats, boolean replaceSaved) {

        if (replaceSaved) {
            this.stats = stats;
        }
        this.searched = stats;
        this.currentElement = 0;

        setupStatButtons();


    }

    public List<Stat> getAllStats() {

        if (true) {

            var stats = Load.Unit(target).getUnit().getStats().stats.values().stream().filter(x -> x.GetStat().show_in_gui).map(x -> x.GetStat()).collect(Collectors.toList());

            var ungrouped = stats.stream().filter(x -> !x.gui_group.isValid()).collect(Collectors.toList());
            List<Stat> grouped = new ArrayList<>();
            for (StatGuiGroup group : StatGuiGroup.values()) {
                if (group.isValid()) {
                    stats.stream().filter(x -> x.gui_group == group).findFirst().ifPresent(x -> grouped.add(x));
                }
            }

            List<Stat> all = new ArrayList<>();
            all.addAll(grouped);

            all.addAll(ungrouped);
            return all;
        }

        return Arrays.asList(new ElementalResist(Elements.Physical), DodgeRating.getInstance(), Armor.getInstance(), Health.getInstance(), Mana.getInstance());
    }

    public LivingEntity getTarget() {
        return target;
    }

    @Override
    protected void init() {
        super.init();


        SEARCH.setFocused(false);
        SEARCH.setCanLoseFocus(true);
        publicAddButton(SEARCH);

        SEARCH.setResponder(x -> {
            showStats(stats.stream().filter(s -> {
                String name = s.locName().getString();
                return name.toLowerCase(Locale.ROOT).contains(x.toLowerCase(Locale.ROOT));
            }).collect(Collectors.toList()), false);
        });


        showStats(StatGuiGroupSection.CORE.getStats(target), true);

    }

    @Override
    public ResourceLocation iconLocation() {
        return SlashRef.guiId("main_hub/icons/stats");
    }

    @Override
    public Words screenName() {
        return Words.Stats;
    }

    public void moveCurrentElementBy(int amount) {
        this.setCurrentElement(MathHelper.clamp(currentElement + amount, 0, searched.size()));
    }
}
