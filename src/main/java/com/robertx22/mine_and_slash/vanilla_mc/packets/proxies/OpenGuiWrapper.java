package com.robertx22.mine_and_slash.vanilla_mc.packets.proxies;

import com.robertx22.library_of_exile.registry.Database;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.card_picker.CardPickScreen;
import com.robertx22.mine_and_slash.gui.screens.atlas_map.AtlasMapScreen;
import com.robertx22.mine_and_slash.gui.card_picker.ICard;
import com.robertx22.mine_and_slash.gui.card_picker.ProphecyCurseCard;
import com.robertx22.mine_and_slash.gui.screens.character_screen.MainHubScreen;
import com.robertx22.mine_and_slash.gui.wiki.BestiaryGroup;
import com.robertx22.mine_and_slash.gui.wiki.reworked.NewWikiScreen;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public class OpenGuiWrapper {

    // ATLAS_NODE/ATLAS_NODE_LAYOUT are SyncTime.ON_LOGIN - a real multiplayer client only has
    // this data once the server's login sync packets arrive, unlike singleplayer where the
    // client and integrated server share the same in-memory registry with no sync delay. If the
    // server sends this OpenGuiPacket before that sync completes (only realistically possible
    // right after joining), retry each client tick instead of opening a screen with an
    // empty/partial node list - see OnClientTick.onEndTick.
    private static boolean pendingAtlasMap = false;

    // how long the request stays queued. Without it the flag is sticky: a player who leaves the world
    // (or whose sync never completes) keeps it set, and the atlas would then hijack whatever screen
    // they have open the moment some later world finishes loading its datapacks.
    private static final int PENDING_ATLAS_MAP_TIMEOUT_TICKS = 20 * 10;
    private static int pendingAtlasMapTicks = 0;

    public static void clearPendingAtlasMap() {
        pendingAtlasMap = false;
        pendingAtlasMapTicks = 0;
    }

    public static void tryOpenPendingAtlasMap() {
        if (!pendingAtlasMap) {
            return;
        }
        Player player = ClientOnly.getPlayer();
        if (player == null) {
            clearPendingAtlasMap();
            return;
        }
        if (++pendingAtlasMapTicks > PENDING_ATLAS_MAP_TIMEOUT_TICKS) {
            clearPendingAtlasMap();
            return;
        }
        if (!Database.areDatapacksLoaded(player.level())) {
            return;
        }
        // the player opened something else in the meantime - honour that rather than yanking them
        // into the atlas for a request they made seconds ago
        if (net.minecraft.client.Minecraft.getInstance().screen != null) {
            return;
        }
        clearPendingAtlasMap();
        net.minecraft.client.Minecraft.getInstance().setScreen(new AtlasMapScreen());
    }

    public static void openMainHub() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new MainHubScreen());
    }

 
    public static CardPickScreen getProphecyCardsScreen() {
        Player p = ClientOnly.getPlayer();

        List<ICard> cards = Load.player(p).prophecy.affixOffers.stream().map(x -> new ProphecyCurseCard(ExileDB.MapAffixes().get(x))).collect(Collectors.toList());

        if (cards.size() == 3) {
            return new CardPickScreen(cards, Words.PROPHECIES, "prophecy");
        }
        return null;
    }


    public static void openProphecyCards() {
        net.minecraft.client.Minecraft.getInstance().setScreen(getProphecyCardsScreen());
    }

    public static void openAtlasMap() {
        Player player = ClientOnly.getPlayer();
        if (player != null && !Database.areDatapacksLoaded(player.level())) {
            pendingAtlasMap = true;
            pendingAtlasMapTicks = 0;
            return;
        }
        net.minecraft.client.Minecraft.getInstance().setScreen(new AtlasMapScreen());
    }

    public static void openWikiRunewords() {

        var sc = new NewWikiScreen();
        net.minecraft.client.Minecraft.getInstance().setScreen(sc);
        sc.setGroup(BestiaryGroup.RUNEWORD);

    }
}
