package com.robertx22.mine_and_slash.mixins;

import com.robertx22.mine_and_slash.capability.CapNbtCache;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.PlayerBackpackData;
import com.robertx22.mine_and_slash.capability.player.PlayerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// our capabilities cache what serializeNBT() builds, because outside code reads a player's nbt far
// more often than anything actually changes. that cache must never be what gets written to disk, so
// force a rebuild here. this is the one place it matters - autosave, logout and shutdown all come
// through PlayerDataStorage.save, and it does not share a path with the cheap readers, which call
// Entity.saveWithoutId directly.
@Mixin(PlayerDataStorage.class)
public class PlayerSaveInvalidateCacheMixin {

    @Inject(method = "save", at = @At(value = "HEAD"))
    public void mns$invalidateNbtCaches(Player player, CallbackInfo ci) {
        try {
            invalidate(PlayerData.get(player) == null ? null : PlayerData.get(player).getNbtCache());
            invalidate(PlayerBackpackData.get(player) == null ? null : PlayerBackpackData.get(player).getNbtCache());
            invalidate(EntityData.get(player) == null ? null : EntityData.get(player).getNbtCache());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void invalidate(CapNbtCache cache) {
        if (cache != null) {
            cache.invalidate();
        }
    }
}
