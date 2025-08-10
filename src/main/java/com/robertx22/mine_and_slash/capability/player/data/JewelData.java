package com.robertx22.mine_and_slash.capability.player.data;

import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.database.data.stats.types.JewelSocketStat;
import com.robertx22.mine_and_slash.saveclasses.gearitem.gear_bases.IStatCtx;
import com.robertx22.mine_and_slash.saveclasses.jewel.JewelItemData;
import com.robertx22.mine_and_slash.saveclasses.unit.stat_ctx.StatContext;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.ClientOnly;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.PlayerUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

public class JewelData implements IStatCtx {

    public MyInventory jewelInventory;
    private transient Player player;
    private transient List<String> wearingUniqueJewel = new ArrayList<>();

    public JewelData(Player player) {
        this((int) Load.Unit(player).getUnit().getCalculatedStat(JewelSocketStat.getInstance()).getValue(), player);
    }

    public JewelData(int size, Player player) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            //this is horrible but I don't know why this could be null.
            //also smth is using new PlayerData() on client every tick but I can't find the source
            if (player == null) this.player = ClientOnly.getPlayer();
        } else {
            this.player = player;
        }
        this.jewelInventory = new MyInventory(size);

        this.jewelInventory.addListener(container -> {
            List<String> uniques = new ArrayList<>();
            for (int i = 0; i < container.getContainerSize(); i++) {
                var stack = jewelInventory.getItem(i);
                JewelItemData data = StackSaving.JEWEL.loadFrom(stack);

                if (data != null) {
                    //unique id getter
                    //I don't think we need to check if there are same uniques jewels in inventory.
                    if (!data.uniq.id.isEmpty()) {
                        uniques.add(data.uniq.id);
                    }
                }
            }
            this.wearingUniqueJewel = uniques;
            updatePlayerData(player);
        });
    }

    public static boolean updatePlayerData(Player player){
        if (player instanceof ServerPlayer){
            Load.player(player).cachedStats.ALLOCATED.setDirty();
        }
        return false;
    }

    public void recalc(Player player) {
        //check if all jewels are wearable.
        for (int i = 0; i < jewelInventory.getContainerSize(); i++) {
            if (!isWearable(jewelInventory.getItem(i), player)) unequip(player, i);
        }
        int jewelSocketsMaxStat = getJewelSocketsMaxStat(player);
        //if the size is change, make a new inventory.
        if (this.jewelInventory.getContainerSize() != jewelSocketsMaxStat) {
            MyInventory newInventory = new MyInventory(jewelSocketsMaxStat);

            for (int i = 0; i < this.jewelInventory.getContainerSize(); i++) {
                ItemStack item = this.jewelInventory.getItem(i);
                if (i < newInventory.getContainerSize() - 1) {
                    newInventory.addItem(item);
                } else {
                    PlayerUtils.giveItem(item.copy(), player);
                    item.shrink(100);
                }
            }
            this.jewelInventory = newInventory;

        }


    }

    public boolean isWearable(ItemStack itemStack, Player player) {
        //why the player member in JewelData could be null??
        JewelItemData jewelItemData = StackSaving.JEWEL.loadFrom(itemStack);
        if (jewelItemData == null || player == null) {
            return false;
        }
        return jewelItemData.canWear(Load.Unit(player)) && (!jewelItemData.isUnique() || (jewelItemData.isUnique() && !wearingUniqueJewel.contains(jewelItemData.uniq.id)));
    }

    public void unequip(Player p, int i) {

        var stack = jewelInventory.getItem(i);
        if (!stack.isEmpty()) {
            PlayerUtils.giveItem(stack.copy(), p);
            stack.shrink(100);
        }
    }


    public void socket(ItemStack stack) {
        for (int i = 0; i < jewelInventory.getTotalSlots(); i++) {
            if (jewelInventory.getItem(i).isEmpty()) {
                jewelInventory.setItem(i, stack.copy());
            }
        }
    }

    public static int getJewelSocketsMaxStat(Player p) {
        int max = (int) Load.Unit(p).getUnit().getCalculatedStat(JewelSocketStat.getInstance()).getValue();
        return max;
    }

    public boolean hasFreeJewelSlots(Player p) {
        //considering 1 slot for 1 jewel, use hasFreeSlots() is ok
        return this.jewelInventory.hasFreeSlots();
    }


    public List<JewelItemData> getAllJewels() {
        List<JewelItemData> list = new ArrayList<>();
        for (int i = 0; i < jewelInventory.getTotalSlots(); i++) {
            var stack = jewelInventory.getItem(i);
            JewelItemData data = StackSaving.JEWEL.loadFrom(stack);
            if (data != null) {
                list.add(data);
            }
        }
        return list;
    }

    @Override
    public List<StatContext> getStatAndContext(LivingEntity en) {
        List<StatContext> list = new ArrayList<>();
        for (JewelItemData jewel : this.getAllJewels()) {
            for (StatContext stac : jewel.getStatAndContext(en)) {
                list.add(stac);
            }
        }


        return list;
    }
}
