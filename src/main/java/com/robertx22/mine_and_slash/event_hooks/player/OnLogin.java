package com.robertx22.mine_and_slash.event_hooks.player;

import com.robertx22.library_of_exile.utils.Watch;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.testing.TestManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

public class OnLogin {


    public static void onLoad(ServerPlayer player) {

        Watch total = null;
        if (MMORPG.RUN_DEV_TOOLS) {
            total = new Watch();
        }

        try {

        
            if (ModList.get().isLoaded("majruszlibrary")) {
                player.sendSystemMessage(Component.literal("[WARNING] You have majruszlibrary mod installed, which currently has a bug and makes Mine and Slash professions not work! It's recommended to remove the mod (and all the mods that depend on that library), until the issue is fixed."));
            }
            if (ModList.get().isLoaded("enchantments_plus")) {
                player.sendSystemMessage(Component.literal("[WARNING] You have Mo' Enchantments mod installed, which currently has a bug and makes Mine and Slash NBT on items break!!! It's recommended to remove the mod, until the issue is fixed."));
            }


            if (!player.getServer()
                    .isCommandBlockEnabled()) {
                player.displayClientMessage(Chats.COMMAND_BLOCK_UNAVALIABLE.locName().withStyle(ChatFormatting.RED), false);
                player.displayClientMessage(Chats.HOW_TO_ENABLE_COMMAND_BLOCK.locName().withStyle(ChatFormatting.GREEN), false);
            }

            if (MMORPG.RUN_DEV_TOOLS) {
                player.displayClientMessage(Chats.Dev_tools_enabled_contact_the_author.locName(), false);
            }

            EntityData data = Load.Unit(player);

            data.onLogin(player);

            Load.player(player).config.onLoginFillDefaults();


            data.setAllDirtyOnLoginEtc();

            // the mercenary entity is never saved to the world, so it always has to be brought back
            // from the player's data. same 3 second delay as any other respawn.
            MercenaryManager.requestRespawn(player);

            if (MMORPG.RUN_DEV_TOOLS) {
                TestManager.RunAllTests(player);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (MMORPG.RUN_DEV_TOOLS) {
            total.print("Total on login actions took ");
        }
    }

    public static void GiveStarterItems(Player player) {

        if (player.level().isClientSide) {
            return;
        }

        player.getInventory().add(new ItemStack(SlashItems.NEWBIE_GEAR_BAG.get()));

    }

}
