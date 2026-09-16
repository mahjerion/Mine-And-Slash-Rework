package com.robertx22.mine_and_slash.mmorpg;

import com.robertx22.addons.ancient_obelisks.ObeliskAddonEvents;
import com.robertx22.addons.dungeon_realm.DungeonAddonEvents;
import com.robertx22.addons.map_device.MapDeviceEvents;
import com.robertx22.addons.the_harvest.HarvestAddonEvents;
import com.robertx22.addons.orbs_of_crafting.currency.reworked.addon.OrbAddonEvents;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.localization.ExileLangFile;
import com.robertx22.library_of_exile.localization.ExileTranslation;
import com.robertx22.library_of_exile.localization.TranslationBuilder;
import com.robertx22.library_of_exile.registry.ExileRegistryType;
import com.robertx22.library_of_exile.registry.helpers.OrderedModConstructor;
import com.robertx22.library_of_exile.registry.register_info.HardcodedRegistration;
import com.robertx22.library_of_exile.registry.register_info.ModRequiredRegisterInfo;
import com.robertx22.library_of_exile.registry.register_info.SeriazableRegistration;
import com.robertx22.library_of_exile.registry.util.ExileRegistryUtil;
import com.robertx22.library_of_exile.utils.Watch;
import com.robertx22.mine_and_slash.a_libraries.curios.CurioEvents;
import com.robertx22.mine_and_slash.a_libraries.curios.RefCurios;
import com.robertx22.mine_and_slash.a_libraries.neat.NeatForgeConfig;
import com.robertx22.mine_and_slash.aoe_data.database.stat_conditions.StatConditions;
import com.robertx22.mine_and_slash.aoe_data.database.stat_effects.StatEffects;
import com.robertx22.mine_and_slash.aoe_data.database.stats.Stats;
import com.robertx22.mine_and_slash.aoe_data.datapacks.lang_file.CreateLangFile;
import com.robertx22.mine_and_slash.characters.PlayerStats;
import com.robertx22.mine_and_slash.config.forge.ClientConfigs;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.profession.ProfessionEvents;
import com.robertx22.mine_and_slash.database.data.profession.all.ProfessionRecipes;
import com.robertx22.mine_and_slash.database.data.spells.components.conditions.EffectCondition;
import com.robertx22.mine_and_slash.database.data.spells.map_fields.MapField;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.priority.StatPriority;
import com.robertx22.mine_and_slash.events.MineAndSlashEventsInit;
import com.robertx22.mine_and_slash.gui.SocketTooltip;
import com.robertx22.mine_and_slash.maps.MapEvents;
import com.robertx22.mine_and_slash.mixin_ducks.tooltip.ItemTooltipsRegister;
import com.robertx22.mine_and_slash.mmorpg.event_registers.CommonEvents;
import com.robertx22.mine_and_slash.mmorpg.init.ClientInit;
import com.robertx22.mine_and_slash.mmorpg.registers.client.KeybindsRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.client.RenderRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.client.S2CPacketRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.common.C2SPacketRegister;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashCapabilities;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashItemTags;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.tags.ModTags;
import com.robertx22.mine_and_slash.uncommon.coins.Coin;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.action.StatEffect;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.condition.StatCondition;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.VanillaRarities;
import com.robertx22.test.test2.SchemaTest;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.function.Consumer;

@Mod(SlashRef.MODID)
public class MMORPG {

    // DISABLE WHEN PUBLIC BUILD
    public static boolean RUN_DEV_TOOLS = false;

    public static SeriazableRegistration SERIAZABLE_REGISTRATION_INFO = new SeriazableRegistration(SlashRef.MODID);
    public static HardcodedRegistration HARDCODED_REGISTRATION_INFO = new HardcodedRegistration(SlashRef.MODID);

    public static ModRequiredRegisterInfo REGISTER_INFO = new ModRequiredRegisterInfo(SlashRef.MODID);

    public static String formatNumber(float num) {

        if (num < ClientConfigs.getConfig().SHOW_DECIMALS_ON_NUMBER_SMALLER_THAN.get()) {
            return DECIMAL_FORMAT.format(num);
        } else {
            return ((int) num) + "";
        }
    }

    // idk how else to pass arguments to a method reference
    public static void createMnsLangFile() {
        TranslationBuilder.of(SlashRef.MODID).name(ExileTranslation.item(SlashItems.RELIC.get(), ChatFormatting.GREEN + "Dungeon Relic")).build();

        // todo eventually i'll replace the entire old CreateLangFile class
        ExileLangFile.createFile(SlashRef.MODID, CreateLangFile.create());
    }

    // todo test
    public static String formatBigNumber(float num) {
        return NumberFormat.getInstance().format(num);
    }


    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");


    public static boolean RUN_DEV_TOOLS_REMOVE_WHEN_DONE = RUN_DEV_TOOLS; // this exists to stop me from making dumb mistakes when testing and forgetting about it


    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SlashRef.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public MMORPG() {

        SchemaTest.run();

        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        OrderedModConstructor.register(new MnsConstructor(SlashRef.MODID), bus);

        if (MMORPG.RUN_DEV_TOOLS) {
            ExileRegistryUtil.setCurrentRegistarMod(SlashRef.MODID);
        }

        Watch watch = new Watch();


        ModLoadingContext.get()
                .registerConfig(ModConfig.Type.SERVER, ServerContainer.spec, NeatForgeConfig.defaultConfigName(ModConfig.Type.SERVER, "mine_and_slash"));


        ExileEvents.CHECK_IF_DEV_TOOLS_SHOULD_RUN.register(new EventConsumer<ExileEvents.OnCheckIsDevToolsRunning>() {
            @Override
            public void accept(ExileEvents.OnCheckIsDevToolsRunning event) {
                event.run = MMORPG.RUN_DEV_TOOLS;
            }
        });

        VanillaRarities.init();
        Coin.init();
        StatPriority.init();
        StackSaving.init();
        StatEffect.init();
        StatCondition.loadclass();

        ForgeEvents.registerForgeEvent(RegisterClientTooltipComponentFactoriesEvent.class, x -> {
            x.register(SocketTooltip.SocketComponent.class, SocketTooltip::new);
        });


        DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {

            NeatForgeConfig.register();

            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfigs.clientSpec, NeatForgeConfig.defaultConfigName(ModConfig.Type.CLIENT, "mine_and_slash"));
            bus.addListener(ClientInit::onInitializeClient);

            ForgeEvents.registerForgeEvent(RegisterKeyMappingsEvent.class, x -> {
                KeybindsRegister.register(x);
            });

            //bus.addListener(KeybindsRegister::register);
            FMLJavaModLoadingContext.get().getModEventBus().addListener((Consumer<EntityRenderersEvent.RegisterRenderers>) x -> {
                RenderRegister.regRenders(x);
            });

        });


        bus.addListener(this::commonSetupEvent);
        bus.addListener(this::interMod);
        MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, CurioEvents::attachCapability);
        ItemTooltipsRegister.init();

        CurioEvents.reg();


        ModTags.init();

        StatLayers.init();
        StatEffects.addSerializers();
        StatConditions.loadClass();
        Stats.loadClass();

        //ExileDBInit.initRegistries();
        //SpecialStats.init();


        MapField.init();
        EffectCondition.init();
        SlashItemTags.init();
        //   ExileDBInit.registerAllItems(); // after config registerAll
        CommonEvents.register();

        C2SPacketRegister.register();
        S2CPacketRegister.register();

        LifeCycleEvents.register();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(EventPriority.LOW, new Consumer<GatherDataEvent>() {
            @Override
            public void accept(GatherDataEvent x) {
                for (ExileRegistryType type : ExileRegistryType.getAllInRegisterOrder()) {
                    x.getGenerator().addProvider(true, type.getDatapackGenerator());
                }
            }
        });

        MapEvents.init();
        ProfessionEvents.init();
        OrbAddonEvents.register();
        MapDeviceEvents.init();

        PlayerStats.register();
        PlayerStats.initialize();

        DerivedRegistries.init();

        // OnClick.register();

        DungeonAddonEvents.init();
        ObeliskAddonEvents.init();
        HarvestAddonEvents.init();

        watch.print("Mine and slash mod initialization ");

        MineAndSlashEventsInit.initEvents();
    }


    public void interMod(InterModEnqueueEvent event) {


/*
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder(RefCurio.RING).size(2).build());
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder(RefCurio.NECKLACE).size(1).build());
        InterModComms.sendTo("curios", SlotTypeMessage.REGISTER_TYPE, () -> new SlotTypeMessage.Builder(RefCurio.OMEN).size(1).build());
*/
        ModLoadingContext.get()
                .registerConfig(ModConfig.Type.SERVER, CompatConfig.spec, NeatForgeConfig.defaultConfigName(ModConfig.Type.SERVER, "mine_and_slash_compatibility"));

    }

    public void commonSetupEvent(FMLCommonSetupEvent event) {

        ProfessionRecipes.init();


        SlashCapabilities.register();

    }


}
