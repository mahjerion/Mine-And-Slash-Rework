package com.robertx22.mine_and_slash.gui.bases;

import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.vanilla_mc.packets.proxies.OpenGuiWrapper;

// Returns the player to the Main Hub - the common case of BackButton, kept as its own type because
// the full screen screens (talent trees, character list, wiki) drop one in with nothing to configure.
public class BackToHubButton extends BackButton {

    public BackToHubButton(int x, int y) {
        // Words.Main_Hub already exists, so this needs no new localization entry
        super(x, y, Words.Main_Hub.locName(), OpenGuiWrapper::openMainHub);
    }
}
