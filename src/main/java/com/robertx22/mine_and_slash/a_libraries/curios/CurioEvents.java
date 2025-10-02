package com.robertx22.mine_and_slash.a_libraries.curios;

import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.database.data.omen.OmenItem;
import com.robertx22.mine_and_slash.mmorpg.ForgeEvents;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.baubles.ItemNecklace;
import com.robertx22.mine_and_slash.vanilla_mc.items.gearitems.baubles.ItemRing;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.event.CurioChangeEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CurioEvents {

    private static final List<String> id = List.of("jewelry/necklace", "jewelry/ring", "omen");

    public static void reg() {
        ForgeEvents.registerForgeEvent(CurioChangeEvent.class, event -> {

            LivingEntity entity = event.getEntity();
            if (entity != null) {
                if (!entity.level().isClientSide) {
                    EntityData data = Load.Unit(entity);
                    if (data != null) {
                        data.setEquipsChanged();
                    }
                }
            }

        });
    }

    public static void attachCapability(AttachCapabilitiesEvent<ItemStack> evt){
        Item item = evt.getObject().getItem();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
        if (!key.getNamespace().equals(SlashRef.MODID)) return;
        if (!(item instanceof ItemNecklace || item instanceof ItemRing || item instanceof OmenItem)) return;
        //copy from charm of undying
        //also see https://docs.illusivesoulworks.com/curios/items/curio-creation#attaching-an-icurio-capability
        ICurio curio = new ICurio() {
            @Override
            public ItemStack getStack() {
                return evt.getObject();
            }

            @Override
            public boolean canEquipFromUse(SlotContext ctx) {
                return true;
            }
        };
        ICapabilityProvider provider = new ICapabilityProvider() {
            private final LazyOptional<ICurio> curioOpt = LazyOptional.of(() -> curio);

            @Nonnull
            @Override
            public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap,
                                                     @Nullable Direction side) {
                return CuriosCapability.ITEM.orEmpty(cap, curioOpt);
            }
        };
        evt.addCapability(CuriosCapability.ID_ITEM, provider);
    }

}
