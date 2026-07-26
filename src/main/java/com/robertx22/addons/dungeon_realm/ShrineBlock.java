package com.robertx22.addons.dungeon_realm;

import com.robertx22.dungeon_realm.configs.DungeonConfig;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.shrine.ShrineBuff;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ShrineDoubleBuff;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
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

    // roughly how many burst particles to spawn per block of buff radius.
    private static final int BURST_POINTS_PER_RADIUS = 24;

    public ShrineBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.LODESTONE).noOcclusion().lightLevel(x -> 10));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 6; i++) {
            level.addParticle(ParticleTypes.WITCH,
                    pos.getX() + 0.6 + (random.nextDouble() - 0.5) * 1.6,
                    pos.getY() + 1.0 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.6 + (random.nextDouble() - 0.5) * 1.6,
                    0, 0.03, 0);
        }
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

    // bursts particles scattered across the buff area (mimics ParticleShape.CIRCLE_2D's random-disc
    // sampling) instead of an evenly-spaced ring, so players get a sense of the shrine's buff reach.
    //
    // Sent as a single sendParticles call with an xyz *spread* rather than one call per point: each
    // call is its own packet to every tracking player, and at a typical radius the per-point version
    // was firing ~200 packets per activation. Vanilla scatters `count` particles over a gaussian of
    // the given spread, which reads the same as the old random-disc sampling from the player's side.
    private void spawnBuffWave(ServerLevel level, BlockPos pos) {
        double buffRadius = DungeonConfig.get().SHRINE_BUFF_RADIUS.get();
        int points = Math.max(16, (int) (buffRadius * BURST_POINTS_PER_RADIUS));
        // /3 so ~99.7% of the gaussian spread lands inside the actual buff radius
        double spread = buffRadius / 3D;
        level.sendParticles(ParticleTypes.WITCH,
                pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                points, spread, 0.1D, spread, 0.05D);
    }

    // Atlas "Twin Blessing" - duration multiplier applied to both buffs when a player has the perk
    private static final float TWIN_BLESSING_DURATION_MULTI = 0.75F;

    // pick a weighted-random buff from the datapack-driven ShrineBuff registry and apply it to every
    // player currently within range of the shrine. Players with the "Twin Blessing" Atlas perk get a
    // second, independently-rolled buff on top, with both durations shortened.
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
            boolean twinBlessing = Load.Unit(pl).getUnit().getCalculatedStat(ShrineDoubleBuff.getInstance()).getValue() > 0;

            if (!twinBlessing) {
                new ExilePotionEvent(CalculatedSpellData.NO_SPELL_RELATED, 1, effect, GiveOrTake2.give,
                        pl, pl, buff.duration_ticks, false).Activate();
                pl.sendSystemMessage(Chats.SHRINE_BUFF_RECEIVED.locName(effect.locName()));
                continue;
            }

            ShrineBuff secondBuff = ExileDB.ShrineBuffs().random();
            // avoid rolling the exact same buff twice, when there's another option available
            if (secondBuff != null && secondBuff.GUID().equals(buff.GUID()) && ExileDB.ShrineBuffs().getAll().size() > 1) {
                secondBuff = ExileDB.ShrineBuffs().random();
            }
            ExileEffect secondEffect = secondBuff != null ? ExileDB.ExileEffects().get(secondBuff.effect_id) : null;

            new ExilePotionEvent(CalculatedSpellData.NO_SPELL_RELATED, 1, effect, GiveOrTake2.give,
                    pl, pl, Math.round(buff.duration_ticks * TWIN_BLESSING_DURATION_MULTI), false).Activate();

            if (secondEffect != null) {
                new ExilePotionEvent(CalculatedSpellData.NO_SPELL_RELATED, 1, secondEffect, GiveOrTake2.give,
                        pl, pl, Math.round(secondBuff.duration_ticks * TWIN_BLESSING_DURATION_MULTI), false).Activate();
                pl.sendSystemMessage(Chats.SHRINE_BUFF_RECEIVED_TWO.locName(effect.locName(), secondEffect.locName()));
            } else {
                pl.sendSystemMessage(Chats.SHRINE_BUFF_RECEIVED.locName(effect.locName()));
            }
        }
    }
}
