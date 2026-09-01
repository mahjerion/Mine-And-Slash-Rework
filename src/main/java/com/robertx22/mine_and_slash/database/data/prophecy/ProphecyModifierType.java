package com.robertx22.mine_and_slash.database.data.prophecy;

import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.omen.OmenBlueprint;
import com.robertx22.mine_and_slash.database.data.prophecy.starts.GearProphecy;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.loot.blueprints.GearBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.ItemBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.JewelBlueprint;
import com.robertx22.mine_and_slash.loot.blueprints.SkillGemBlueprint;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

public enum ProphecyModifierType {

    GEAR_TYPE() {
        @Override
        public MutableComponent getTooltip(String data) {
            return ExileDB.GearTypes().get(data).locName().withStyle(ChatFormatting.WHITE);
        }

        @Override
        public float chanceToSpawn() {
            return ServerContainer.get().PROPHECY_GEAR_TYPE_CHANCE.get();
        }

        @Override
        public void set(ItemBlueprint b, String data) {
            if (b instanceof GearBlueprint gb) {
                gb.gearItemSlot.set(data);
            }
        }

        @Override
        public boolean canApplyTo(ProphecyStart start, ItemBlueprint b) {
            return start instanceof GearProphecy;
        }
    },
    JEWEL_RARITY() {
        @Override
        public MutableComponent getTooltip(String data) {
            var rar = ExileDB.GearRarities().get(data);
            return rar.locName().withStyle(rar.textFormatting());
        }

        @Override
        public float chanceToSpawn() {
            return ServerContainer.get().PROPHECY_JEWEL_RARITY_CHANCE.get();
        }

        @Override
        public void set(ItemBlueprint b, String data) {
            if (b instanceof JewelBlueprint gb) {
                gb.rarity.set(ExileDB.GearRarities().get(data));
            }
        }

        @Override
        public boolean canApplyTo(ProphecyStart start, ItemBlueprint b) {
            return b instanceof JewelBlueprint;
        }
    },
    OMEN_RARITY() {
        @Override
        public MutableComponent getTooltip(String data) {
            var rar = ExileDB.GearRarities().get(data);
            return rar.locName().withStyle(rar.textFormatting());
        }

        @Override
        public float chanceToSpawn() {
            return ServerContainer.get().PROPHECY_OMEN_RARITY_CHANCE.get();
        }

        @Override
        public void set(ItemBlueprint b, String data) {
            if (b instanceof OmenBlueprint ob) {
                ob.rarity.set(ExileDB.GearRarities().get(data));
            }
        }

        @Override
        public boolean canApplyTo(ProphecyStart start, ItemBlueprint b) {
            return b instanceof OmenBlueprint;
        }
    },
    SKILL_GEM_RARITY() {
        @Override
        public MutableComponent getTooltip(String data) {
            var rar = ExileDB.GearRarities().get(data);
            return rar.locName().withStyle(rar.textFormatting());
        }

        @Override
        public float chanceToSpawn() {
            return ServerContainer.get().PROPHECY_SKILL_GEM_RARITY_CHANCE.get();
        }

        @Override
        public void set(ItemBlueprint b, String data) {
            if (b instanceof SkillGemBlueprint gb) {
                gb.rarity.set(ExileDB.GearRarities().get(data));
            }
        }

        @Override
        public boolean canApplyTo(ProphecyStart start, ItemBlueprint b) {
            return b instanceof SkillGemBlueprint;
        }
    },


    GEAR_RARITY() {
        @Override
        public MutableComponent getTooltip(String data) {
            var rar = ExileDB.GearRarities().get(data);
            return rar.locName().withStyle(rar.textFormatting());
        }

        @Override
        public float chanceToSpawn() {
            return ServerContainer.get().PROPHECY_GEAR_RARITY.get();
        }

        @Override
        public void set(ItemBlueprint b, String data) {
            if (b instanceof GearBlueprint gb) {
                gb.rarity.set(ExileDB.GearRarities().get(data));
            }
        }

        @Override
        public boolean canApplyTo(ProphecyStart start, ItemBlueprint b) {
            return b instanceof GearBlueprint;
        }
    };


    public abstract MutableComponent getTooltip(String data);

    public abstract float chanceToSpawn();

    public abstract void set(ItemBlueprint b, String data);

    public abstract boolean canApplyTo(ProphecyStart start, ItemBlueprint b);
}
