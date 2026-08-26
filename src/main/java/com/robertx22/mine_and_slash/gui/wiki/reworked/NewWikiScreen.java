package com.robertx22.mine_and_slash.gui.wiki.reworked;

import com.robertx22.mine_and_slash.gui.bases.BackToHubButton;
import com.robertx22.mine_and_slash.gui.bases.INamedScreen;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryEntry;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryGroup;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.ClearFilterButton;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.FilterGroupButton;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.GroupFilterEntry;
import com.robertx22.mine_and_slash.gui.wiki.reworked.filters.GroupFilterType;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.stream.Collectors;

// from SelectWorldScreen
public class NewWikiScreen extends Screen implements INamedScreen {


    protected EditBox searchBox;
    private WikiEntryList list;

    public NewWikiScreen() {
        super(Words.WIKI.locName().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    public BestiaryEntry selectedEntry = null;

    public BestiaryGroup<?> group = BestiaryGroup.UNIQUE_GEAR;


    public HashMap<GroupFilterType, GroupFilterEntry> filter = new HashMap<>();


    public GroupFilterEntry getFilter(GroupFilterType t) {
        return filter.getOrDefault(t, GroupFilterEntry.NONE);
    }

    public void setGroup(BestiaryGroup<?> g) {

        this.group = g;
        this.filter = new HashMap<>();

        // rebuildWidgets instead of init, it also drops the focus on the widgets that are about to be thrown away
        rebuildWidgets();
    }


    public void setFilterGroup(GroupFilterType type, GroupFilterEntry f) {
        this.filter.put(type, f);
        this.list.forceFilter(this.searchBox.getValue());
    }

    private void refreshFilterButtons() {

        this.children().stream().filter(x -> x instanceof FilterGroupButton).collect(Collectors.toList()).forEach(e -> this.removeWidget(e));

        int x = 15;
        int y = 55;

        int spacing = FilterGroupButton.HEIGHT + 5;

        for (GroupFilterType t : GroupFilterType.ALL) {
            if (t.forGroup == this.group) {
                this.addRenderableWidget(new FilterGroupButton(this, t, x, y));
                this.addRenderableWidget(new ClearFilterButton(this, t, x + FilterGroupButton.WIDTH + 5, y));
                y += spacing;
            }
        }
    }

    public void tick() {
        this.searchBox.tick();
    }

    public Checkbox searchTooltipsCheckbox;

    // the checkbox itself is recreated on every init, this is what actually holds the setting
    public boolean searchTooltips = false;

    // gotta call add filter button first or it cant be clicked for some reason
    protected void init() {


        this.clearWidgets();


        refreshFilterButtons();


        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.translatable("selectWorld.search"));
        this.searchBox.setResponder((p_232980_) -> {
            this.list.tryFilter(p_232980_);
        });

        this.searchTooltipsCheckbox = new Checkbox(searchBox.getX() + searchBox.getWidth() + 5, searchBox.getY(), 20, 20, Component.literal("Search Tooltips"), searchTooltips) {
            @Override
            public void onPress() {
                super.onPress();
                searchTooltips = this.selected();
                list.forceFilter(searchBox.getValue());
            }
        };
        this.addRenderableWidget(searchTooltipsCheckbox);


        this.list = new WikiEntryList(this, this.minecraft, this.width, this.height, 48, this.height - 64, 36);


        this.addWidget(this.searchBox);

        this.addWidget(this.list);


        this.setupGroupButtons();

        // the edit box keeps its text across inits, the list has to be filtered by it
        this.list.forceFilter(this.searchBox.getValue());

        // full window screen, so the same top-left corner the talent trees use. the filter buttons
        // start at x=15,y=55, so the corner is free
        this.addRenderableWidget(new BackToHubButton(4, 4));
    }

    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        return super.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    public void onClose() {
        super.onClose();
    }

    public boolean charTyped(char pCodePoint, int pModifiers) {
        // whatever is focused gets first say, typing only falls through to the search box
        if (super.charTyped(pCodePoint, pModifiers)) {
            return true;
        }
        this.setFocused(this.searchBox);
        return this.searchBox.charTyped(pCodePoint, pModifiers);
    }

    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {

        this.list.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.searchBox.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }


    private void setupGroupButtons() {


        int spacing = (int) (NewGroupButton.SIZE * 1.25F);

        int total = BestiaryGroup.getAll().size() * spacing;

        int gx = this.width / 2 - (total / 2);
        int gy = this.height - 35;


        for (BestiaryGroup bestiaryGroup : BestiaryGroup.getAll()) {
            addRenderableWidget(new NewGroupButton(this, bestiaryGroup, gx, gy));
            gx += spacing;
        }

    }

    public void removed() {
        if (this.list != null) {
            //  this.list.children().forEach(WorldSelectionList.Entry::close);
        }

    }

    @Override
    public ResourceLocation iconLocation() {
        return new ResourceLocation(SlashRef.MODID, "textures/gui/main_hub/icons/wiki.png");
    }

    @Override
    public Words screenName() {
        return Words.WIKI;
    }
}