package com.robertx22.mine_and_slash.characters.reworked_gui;

import com.robertx22.mine_and_slash.characters.CharacterData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ToonList extends ObjectSelectionList<ToonEntry> {

    public static int HEIGHT = 50;
    ToonScreen screen;

    public ToonList(ToonScreen screen, Minecraft mc, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
        super(mc, pWidth, pHeight, 48, screen.height - 64, 36);
        this.screen = screen;


        forceFilter("");

        this.setRenderBackground(false);
    }

    private List<ToonData> all = new ArrayList<>();

    private String filter = "donutuse";


    private void reloadAllEntries() {

        this.all = new ArrayList<>();

        for (Map.Entry<Integer, CharacterData> en : Load.player(ClientOnly.getPlayer()).characters.map.entrySet()) {
            all.add(new ToonData(en.getValue(), en.getKey()));
        }
    }

    public void tryFilter(String pFilter) {
        if (!pFilter.equals(this.filter)) {
            this.forceFilter(pFilter);
        }
        this.filter = pFilter;
    }

    public void forceFilter(String cFilter) {
        this.clearEntries();
        reloadAllEntries();

        String search = cFilter.toLowerCase(Locale.ROOT);

        for (ToonData data : this.all) {
            if (matchesSearch(data, search)) {
                addEntry(new ToonEntry(this, data));
            }
        }
        this.filter = cFilter;
    }

    private boolean matchesSearch(ToonData data, String search) {
        if (search.isEmpty()) {
            return true;
        }
        return data.data.name != null && data.data.name.toLowerCase(Locale.ROOT).contains(search);
    }


    @Override
    protected void renderBackground(GuiGraphics pGuiGraphics) {
        pGuiGraphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
    }


}
