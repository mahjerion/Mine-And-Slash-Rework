package com.robertx22.mine_and_slash.prophecy;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.library_of_exile.utils.geometry.Circle2d;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.stats.types.loot.ProphecyDoubleCurse;
import com.robertx22.mine_and_slash.uncommon.ExplainedResultUtil;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.AllyOrEnemy;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.EntityFinder;
import com.robertx22.mine_and_slash.vanilla_mc.packets.OpenGuiPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ProphecyAltarBlock extends Block {

    public ProphecyAltarBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.LECTERN).noOcclusion());
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        Circle2d c = new Circle2d(pPos.above(), 0.3F);
        c.doXTimes(15, x -> {
            c.spawnParticle(pLevel, c.getRandomPos(), ParticleTypes.WITCH);
        });
    }


    @Override
    public InteractionResult use(BlockState pState, Level level, BlockPos pPos, Player p, InteractionHand pHand, BlockHitResult pHit) {

        if (!level.isClientSide) {

            if (!EntityFinder.start(p, Mob.class, p.blockPosition()).radius(ServerContainer.get().PROPHECY_SEARCH_RADIUS.get()).searchFor(AllyOrEnemy.enemies).build().isEmpty()) {
                ExplainedResultUtil.sendErrorMessage(p, Chats.PROPHECY_ALTAR_USE_ERROR, Chats.ENEMY_TOO_CLOSE);
                return InteractionResult.FAIL;
            }

            if (Load.player(p).prophecy.numMobAffixesCanAdd > 0) {
                ExplainedResultUtil.sendErrorMessage(p, Chats.PROPHECY_ALTAR_USE_ERROR, Chats.PROPHECY_PLEASE_SPEND);
                return InteractionResult.SUCCESS;
            }

            var prophecy = Load.player(p).prophecy;

            // Atlas "Twin Curse" - forces 2 curse picks per altar instead of 1 (the altar already
            // refuses reuse while numMobAffixesCanAdd > 0, so this alone is enough to require both
            // be spent before the next altar)
            boolean doubleCurse = Load.Unit(p).getUnit().getCalculatedStat(ProphecyDoubleCurse.getInstance()).getValue() > 0;
            prophecy.numMobAffixesCanAdd += doubleCurse ? 2 : 1;

            // the first altar touched in a map "initiates" the prophecy event, granting a free look
            // at reward offers - every altar after that (and every reroll from then on) only grants
            // a curse; further reward rolls have to go through the paid GUI reroll
            if (!prophecy.usedFreeRoll) {
                prophecy.usedFreeRoll = true;
                prophecy.regenerateNewOffers(p);
            }

            prophecy.regenAffixOffers();

            p.sendSystemMessage(Chats.PROPHECY_ALTAR_MSG.locName().withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));

            SoundUtils.playSound(p, SoundEvents.EXPERIENCE_ORB_PICKUP);


            level.setBlock(pPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);


            Load.player(p).playerDataSync.setDirty();
            Load.player(p).playerDataSync.onTickTrySync(p);
            // todo does this open before the player receives sync data packet?
            Packets.sendToClient(p, new OpenGuiPacket(OpenGuiPacket.GuiType.PICK_PROPHECY_CURSE));


            // nothing is stored in this block, instead by clicking the altar, the player gains the options

        } else {

            // need to delay this for 1 tick at least
            //ClientOnly.openProphecy();
        }

        return InteractionResult.SUCCESS;
    }

}
