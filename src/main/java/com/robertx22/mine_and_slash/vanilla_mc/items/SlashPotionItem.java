package com.robertx22.mine_and_slash.vanilla_mc.items;

import com.robertx22.library_of_exile.deferred.RegObj;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.profession.ICreativeTabTiered;
import com.robertx22.mine_and_slash.database.data.profession.LeveledItem;
import com.robertx22.mine_and_slash.database.data.profession.all.Professions;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.gui.texts.ExileTooltips;
import com.robertx22.mine_and_slash.gui.texts.textblocks.LeveledItemBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.NameBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.RarityBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.RequirementBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.dropblocks.ProfessionDropSourceBlock;
import com.robertx22.mine_and_slash.gui.texts.textblocks.usableitemblocks.UsageBlock;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.RarityItems;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourceType;
import com.robertx22.mine_and_slash.saveclasses.unit.ResourcesData;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.EventBuilder;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.RestoreType;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.localization.Itemtips;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.StringUTIL;
import com.robertx22.mine_and_slash.vanilla_mc.items.misc.AutoItem;
import com.robertx22.temp.SkillItemTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SlashPotionItem extends AutoItem implements ICreativeTabTiered {

    String rar;
    Type type;

    public SlashPotionItem(String rar, Type type) {
        super(new Properties().stacksTo(64));
        this.rar = rar;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String locNameForLangFile() {
        return StringUTIL.capitalise(rar) + " " + type.name + " Potion";
    }

    @Override
    public String GUID() {
        return null;
    }

    @Override
    public Item getThis() {
        return this;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {

        int num = (int) this.type.getHealPercent(pStack);
        int minLevel = LeveledItem.getTier(pStack).levelRange.getMinLevel();

        pTooltipComponents.clear();
        pTooltipComponents.addAll(new ExileTooltips()
                .accept(new NameBlock(pStack.getHoverName()))
                .accept(new RarityBlock(getRarity()))
                .accept(new RequirementBlock(minLevel))
                .accept(new ProfessionDropSourceBlock(Professions.ALCHEMY))
                .accept(new UsageBlock(Collections.singletonList(Itemtips.Restores.locName(Component.literal(num + "%").withStyle(ChatFormatting.GREEN), this.type.name).withStyle(ChatFormatting.GRAY))))
                .accept(new UsageBlock(Collections.singletonList(Words.COOLDOWN.locName(Component.literal(getCooldownTicks() / 20 + "").withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GOLD))))
                .release());

    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player p, InteractionHand pUsedHand) {
        ItemStack stack = p.getItemInHand(pUsedHand);

        if (!pLevel.isClientSide) {
            handlePotionRestore(p, stack);
        }

        return InteractionResultHolder.pass(p.getItemInHand(pUsedHand));

    }

    public boolean handlePotionRestore(Player p, ItemStack stack) {
        int itemLevel = LeveledItem.getLevel(stack);
        if (itemLevel > Load.Unit(p).getLevel()) {
            p.sendSystemMessage(ExplainedResultUtil.createErrorAndReason(Chats.DRINK_POTION_ERROR, Chats.TOO_LOW_LEVEL));
            return false;
        }

        boolean b = type.restoreResource(p, stack, this);
        if (b) {
            type.getSameTypePotions().forEach(x -> p.getCooldowns().addCooldown(x, getCooldownTicks()));
            SoundUtils.playSound(p, SoundEvents.GENERIC_DRINK);
            stack.shrink(1);
        }
        return b;
    }

    public int getCooldownTicks() {
        return 20 * 10;
    }


    public GearRarity getRarity() {
        return ExileDB.GearRarities().get(rar);
    }

    public enum Type {
        HP("Health", Items.POTATO) {
            @Override
            public boolean restoreResource(Player player, ItemStack itemStack, SlashPotionItem slashPotionItem) {
                float healPercent = this.getHealPercent(itemStack);
                ResourcesData resources = Load.Unit(player).getResources();
                if (HealthUtils.getCurrentHealth(player) < HealthUtils.getMaxHealth(player) || resources.getMagicShield() < resources.getMax(player, ResourceType.magic_shield)) {
                    EventBuilder.ofRestore(player, player, ResourceType.health, RestoreType.potion, HealthUtils.getMaxHealth(player) * healPercent / 100F).build().Activate();
                    EventBuilder.ofRestore(player, player, ResourceType.magic_shield, RestoreType.potion, Load.Unit(player).getUnit().magicShieldData().getValue() * healPercent / 100F).build().Activate();
                    return true;
                }
                return false;

            }

            @Override
            public List<SlashPotionItem> getSameTypePotions() {
                return RarityItems.HEALTH_POTIONS.values().stream().map(RegObj::get).collect(Collectors.toList());
            }

            @Override
            public float getHealPercent(ItemStack stack) {
                var r = ((SlashPotionItem) stack.getItem()).getRarity();
                SkillItemTier tier = LeveledItem.getTier(stack);
                return 5 + (0.25F * r.stat_percents.max * tier.statMulti);
            }
        },
        MANA("Mana", Items.CARROT) {
            @Override
            public boolean restoreResource(Player player, ItemStack itemStack, SlashPotionItem slashPotionItem) {
                float healPercent = this.getHealPercent(itemStack);
                ResourcesData resources = Load.Unit(player).getResources();
                if (resources.getMana() < resources.getMax(player, ResourceType.mana) || resources.getEnergy() < resources.getMax(player, ResourceType.energy)) {
                    EventBuilder.ofRestore(player, player, ResourceType.mana, RestoreType.potion, Load.Unit(player).getUnit().manaData().getValue() * healPercent / 100F).build().Activate();
                    EventBuilder.ofRestore(player, player, ResourceType.energy, RestoreType.potion, Load.Unit(player).getUnit().energyData().getValue() * healPercent / 100F).build().Activate();

                    return true;
                }
                return false;
            }

            @Override
            public List<SlashPotionItem> getSameTypePotions() {
                return RarityItems.RESOURCE_POTIONS.values().stream().map(RegObj::get).collect(Collectors.toList());
            }

            @Override
            public float getHealPercent(ItemStack stack) {
                var r = ((SlashPotionItem) stack.getItem()).getRarity();
                SkillItemTier tier = LeveledItem.getTier(stack);
                return 5 + (0.25F * r.stat_percents.max * tier.statMulti);
            }
        };
        String name;

        Item craftItem;


        public abstract boolean restoreResource(Player player, ItemStack itemStack, SlashPotionItem slashPotionItem);
        public abstract List<SlashPotionItem> getSameTypePotions();
        public abstract float getHealPercent(ItemStack stack);

        Type(String name, Item craftItem) {
            this.name = name;
            this.craftItem = craftItem;
        }
    }
}
