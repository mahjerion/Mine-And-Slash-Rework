package com.robertx22.addons.dungeon_realm;

import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.shrine.ShrineBuff;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.effectdatas.ExilePotionEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.GiveOrTake2;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

// A "shrine" bonus-map encounter (PoE Shrine-style), placed by the MapContent system
// (MnsMapContents.SHRINE). Right-clicking it once grants a beneficial buff to EVERY player within
// range (the user and any allies nearby), then the shrine is consumed. Lives in the dungeon-realm
// glue package for consistency with the other bonus encounters.
public class ShrineBlock extends Block {

    // how far apart (blocks) particles are spaced along the wave ring's circumference.
    private static final double WAVE_POINT_SPACING = 1.0;

    public ShrineBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.LODESTONE).noOcclusion().lightLevel(x -> 10));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        level.addParticle(ParticleTypes.ENCHANT,
                pos.getX() + 0.2 + random.nextDouble() * 0.6,
                pos.getY() + 1.0 + random.nextDouble() * 0.5,
                pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                0, 0.03, 0);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        grantBuff((ServerLevel) level, pos);
        spawnBuffWave((ServerLevel) level, pos);
        SoundUtils.playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
        // one-shot: the shrine is spent the moment it's used
        level.removeBlock(pos, false);
        return InteractionResult.SUCCESS;
    }

    // bursts a single particle ring out at BUFF_RADIUS, so players can see exactly how far the
    // shrine's buff reaches.
    private void spawnBuffWave(ServerLevel level, BlockPos pos) {
        double buffRadius = DungeonConfig.get().SHRINE_BUFF_RADIUS.get();
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.3;
        double centerZ = pos.getZ() + 0.5;
        int points = Math.max(8, (int) Math.ceil(2 * Math.PI * buffRadius / WAVE_POINT_SPACING));
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = centerX + Math.cos(angle) * buffRadius;
            double z = centerZ + Math.sin(angle) * buffRadius;
            level.sendParticles(ParticleTypes.ENCHANT, x, centerY, z, 1, 0, 0.05, 0, 0);
        }
    }

    // pick a weighted-random buff from the datapack-driven ShrineBuff registry and apply it to every
    // player currently within range of the shrine.
    private void grantBuff(ServerLevel level, BlockPos pos) {
        if (ExileDB.ShrineBuffs().isEmpty()) {
            return;
        }
        ShrineBuff buff = ExileDB.ShrineBuffs().random();
        if (buff == null) {
            return;
        }
        ExileEffect effect = ExileDB.ExileEffects().get(buff.effect_id);
        if (effect == null) {
            return;
        }
        AABB area = new AABB(pos).inflate(DungeonConfig.get().SHRINE_BUFF_RADIUS.get());
        List<Player> players = level.getEntitiesOfClass(Player.class, area);
        for (Player pl : players) {
            new ExilePotionEvent(CalculatedSpellData.NO_SPELL_RELATED, 1, effect, GiveOrTake2.give,
                    pl, pl, buff.duration_ticks, false).Activate();
            pl.sendSystemMessage(Chats.SHRINE_BUFF_RECEIVED.locName(effect.locName()));
        }
    }
}
