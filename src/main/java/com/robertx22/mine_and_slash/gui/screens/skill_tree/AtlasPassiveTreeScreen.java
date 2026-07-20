package com.robertx22.mine_and_slash.gui.screens.skill_tree;

import com.robertx22.mine_and_slash.database.data.talent_tree.TalentTree;
import com.robertx22.mine_and_slash.gui.screens.atlas_map.AtlasMapScreen;
import com.robertx22.mine_and_slash.gui.screens.atlas_map.AtlasNavButton;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class AtlasPassiveTreeScreen extends SkillTreeScreen {

    private AtlasNavButton mapButton;

    public AtlasPassiveTreeScreen() {
        super(TalentTree.SchoolType.ATLAS);
    }

    @Override
    protected void init() {
        super.init();
        // same top-left spot as the button on AtlasMapScreen, so switching back and forth
        // doesn't require moving the mouse
        mapButton = new AtlasNavButton(4, 4, this, new AtlasMapScreen());
        // registered for input/hover only (not addButtonPublic/addRenderableWidget) - SkillTreeScreen's
        // render() draws all renderables inside a gui.pose().scale(zoom, ...) transform, which would
        // shrink and misplace this button relative to its (unscaled) click hitbox. Rendered manually
        // below, after super.render() has already reset the pose back to 1:1 - same trick SkillTreeScreen
        // itself uses for its SEARCH box and tips widget.
        addWidget(mapButton);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        super.render(gui, mouseX, mouseY, partialTick);
        mapButton.render(gui, mouseX, mouseY, partialTick);
    }

    @Override
    public ResourceLocation iconLocation() {
        return new ResourceLocation(SlashRef.MODID, "textures/gui/main_hub/icons/map_upgrade.png");
    }

    @Override
    public Words screenName() {
        return Words.AtlasPassives;
    }

    @Override
    public boolean shouldAlert() {
        return Load.player(ClientOnly.getPlayer()).talents.hasFreePoints(ClientOnly.getPlayer(), TalentTree.SchoolType.ATLAS);
    }
}
