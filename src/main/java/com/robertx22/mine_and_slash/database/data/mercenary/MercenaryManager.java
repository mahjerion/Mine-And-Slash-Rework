package com.robertx22.mine_and_slash.database.data.mercenary;

import com.robertx22.library_of_exile.main.ExileLog;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.player.helper.MyInventory;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.mmorpg.registers.common.SlashEntities;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryInventories;
import com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryStorageData;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import net.minecraft.ChatFormatting;
import net.minecraft.sounds.SoundEvents;
import com.robertx22.mine_and_slash.vanilla_mc.packets.mercenary.MercenarySlotType;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.LevelUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Owns the one live mercenary entity a player has out: spawning it, dismissing it on a class switch,
 * bringing it back after it dies, and keeping the entity's equipment slots in step with the stored
 * loadout.
 * <p>
 * The entity itself is never written to the world save ({@code MercenaryEntity.shouldBeSaved} is
 * false), so a reload, a crash or a dimension change can't leave an orphan wandering around. The
 * mercenary is respawned from the player's data instead, which is the only copy that matters.
 */
public class MercenaryManager {

    /** ticks between dismissing one mercenary and the replacement arriving - the design's 3 seconds */
    public static final int RESPAWN_TICKS = 20 * 3;

    /** how long a mercenary may be owed but absent before we say so in the log, once */
    private static final int STALL_WARN_TICKS = 20 * 30;

    // ------------------------------------------------------------------ lookup

    @Nullable
    public static MercenaryEntity getMerc(Player player) {
        if (player == null || player.level().isClientSide) {
            return null;
        }
        MercenaryStorageData mercs = Load.player(player).mercs;
        if (mercs.spawnedId == null) {
            return null;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return null;
        }
        // the overwhelmingly common case, and worth its own lookup: onPlayerTick dismisses the
        // mercenary the moment its level stops matching the owner's, so it is in the player's own
        // level essentially always. this runs every tick for every player, and getAllLevels() is a
        // long list on a server running an instanced map dimension per party.
        MercenaryEntity own = findIn(player.level(), mercs.spawnedId);
        if (own != null) {
            return own;
        }
        // a dimension change moves the player into a different ServerLevel, which has its own
        // independent entity manager - looking the UUID up only in player.level() would go blind the
        // moment the player leaves the dimension the mercenary is actually in, which is exactly what
        // let a dismiss/respawn strand the old entity instead of discarding it.
        for (ServerLevel level : server.getAllLevels()) {
            MercenaryEntity merc = findIn(level, mercs.spawnedId);
            if (merc != null) {
                return merc;
            }
        }
        return null;
    }

    @Nullable
    private static MercenaryEntity findIn(Level level, UUID id) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity en = serverLevel.getEntity(id);
        return en instanceof MercenaryEntity merc && merc.isAlive() ? merc : null;
    }

    /** the owner of a mercenary, when they are still online and in the same world */
    @Nullable
    public static Player getOwnerOf(LivingEntity entity) {
        if (entity instanceof MercenaryEntity merc && merc.getOwner() instanceof Player p) {
            return p;
        }
        return null;
    }

    // ------------------------------------------------------------------ unlock

    /**
     * Whether this character is high enough level to have a mercenary at all. Safe on both sides:
     * the level comes off EntityData, which is synced (the hub already prints it), and the unlock
     * level is a Forge SERVER config, which Forge syncs to clients on join.
     */
    public static boolean isUnlocked(Player player) {
        return Load.Unit(player).getLevel() >= ServerContainer.get().MERCENARY_UNLOCK_LEVEL.get();
    }

    /**
     * Whether this character should actually have a mercenary out right now - unlocked by level and
     * not switched off by the player. The gui reads this to draw its toggle; the tick enforces it.
     */
    public static boolean isEnabled(Player player) {
        return isUnlocked(player) && !Load.player(player).mercs.disabled;
    }

    /**
     * Say it once, the first tick this character qualifies.
     * <p>
     * Driven off a persisted flag here rather than off {@code EntityData.LevelUp} on purpose: level
     * up is only the exp route. A level command, a character import, or the unlock level being
     * lowered would all reach the threshold without passing through it, and every character that was
     * already above the threshold when this shipped would never be told at all.
     */
    private static void announceUnlockOnce(ServerPlayer player, MercenaryStorageData mercs) {
        if (mercs.unlock_announced) {
            return;
        }
        mercs.unlock_announced = true;
        player.displayClientMessage(Chats.MERCENARIES_UNLOCKED.locName().withStyle(ChatFormatting.GOLD), false);
        Load.player(player).playerDataSync.setDirtyAndSync(player);
    }

    // ------------------------------------------------------------------ tick

    public static void onPlayerTick(ServerPlayer player) {
        try {
            MercenaryStorageData mercs = Load.player(player).mercs;

            // before anything else, including warnIfStalled - a character that hasn't unlocked
            // mercenaries yet is not "owed" one, so it must not be counted as stalled either.
            if (!isUnlocked(player)) {
                // the level can go back down (an admin command, a character import) or the unlock
                // level can be raised under a character that already had one - take it back.
                if (mercs.spawnedId != null) {
                    dismiss(player);
                }
                mercs.respawnTimer = 0;
                mercs.blockedTicks = 0;
                return;
            }

            // after the announcement on purpose - switching mercenaries off must never swallow the
            // "they are unlocked" message for a character that crosses the level while disabled.
            announceUnlockOnce(player, mercs);

            if (mercs.disabled) {
                if (mercs.spawnedId != null) {
                    dismiss(player);
                }
                mercs.respawnTimer = 0;
                mercs.blockedTicks = 0;
                return;
            }

            MercenaryEntity merc = getMerc(player);

            if (merc != null) {
                // the mercenary follows its owner across dimensions rather than being stranded in the
                // one it was spawned into.
                if (merc.level() != player.level()) {
                    dismiss(player);
                    mercs.respawnTimer = RESPAWN_TICKS;
                    return;
                }
                // a class switch dismisses the old one immediately and the new one arrives after the
                // usual delay, per the design.
                if (!merc.getClassId().equals(mercs.getActiveId())) {
                    dismiss(player);
                    mercs.respawnTimer = RESPAWN_TICKS;
                    return;
                }
                keepInStep(player, merc);
                mercs.blockedTicks = 0;
                return;
            }

            warnIfStalled(player, mercs);

            // no live mercenary. count down, but only once the owner is out of combat - the design
            // has it return "once out of combat and after 3s", not mid fight.
            if (isOwnerInCombat(player)) {
                return;
            }

            if (mercs.respawnTimer > 0) {
                mercs.respawnTimer--;
                if (mercs.respawnTimer <= 0 && spawn(player) == null) {
                    // spawn failed. re-arm rather than sitting at 0 forever - that turned any
                    // transient failure into "no mercenary until you relog", with nothing in the log
                    // to say why. the warning inside spawn() names the reason.
                    mercs.respawnTimer = RESPAWN_TICKS;
                }
                return;
            }

            // belt and braces: the mercenary should be out and isn't, and no countdown is running.
            // that means something dropped the timer without spawning, so start one.
            mercs.respawnTimer = RESPAWN_TICKS;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The mod's own definition of in combat - the same flag that stops you swapping characters and
     * halves your regen, set on the player in {@code DamageEvent} whether they dealt the hit or took it.
     * <p>
     * Deliberately NOT {@code player.getLastHurtByMobTimestamp()}. That value is written to the save
     * file as {@code HurtByTimestamp} and comes back on login, while {@code tickCount} restarts at 0
     * for the fresh player entity - so on any world that had ever seen combat the subtraction went
     * negative, read as "still in combat", and stayed that way until tickCount climbed past the saved
     * number. On a world carrying 14853 that was twelve minutes of every session in which the
     * mercenary could not spawn, silently. A cooldown counts down instead of comparing two clocks, so
     * it can't drift like that.
     */
    private static boolean isOwnerInCombat(Player player) {
        return Load.Unit(player).getCooldowns().isOnCooldown(CooldownsData.IN_COMBAT);
    }

    /**
     * A mercenary that is owed but never arrives used to be completely silent - no exception, no
     * warning, nothing to grep for. Say it once, with the reason, and then shut up.
     */
    private static void warnIfStalled(ServerPlayer player, MercenaryStorageData mercs) {
        mercs.blockedTicks++;

        if (mercs.blockedTicks != STALL_WARN_TICKS) {
            return;
        }
        ExileLog.get().warn("No mercenary out for " + player.getName().getString() + " after "
                + (STALL_WARN_TICKS / 20) + "s. Held back by: "
                + (isOwnerInCombat(player) ? "the owner being in combat" : "respawn timer at " + mercs.respawnTimer)
                + ". Active class id: '" + mercs.getActiveId() + "'.");
    }

    /** called every tick a mercenary is out, to catch anything that drifted */
    private static void keepInStep(Player player, MercenaryEntity merc) {
        MercenaryData data = merc.getMercData();
        if (data == null) {
            return;
        }
        // max health comes from the Health stat, which isn't known until the first recalc has run a
        // few ticks after spawning. topping up here means a fresh mercenary arrives at full health
        // rather than at whatever the placeholder attribute happened to be.
        if (merc.tickCount == 5) {
            merc.setHealth(merc.getMaxHealth());
        }
        // a mercenary can never outlevel its owner (design section 3)
        int ownerLvl = Load.Unit(player).getLevel();
        if (data.lvl > ownerLvl) {
            data.lvl = ownerLvl;
            data.exp = 0;
            Load.Unit(merc).setLevel(data.lvl);
            Load.Unit(merc).setEquipsChanged();
        } else if (Load.Unit(merc).getLevel() != data.lvl) {
            Load.Unit(merc).setLevel(data.lvl);
            Load.Unit(merc).setEquipsChanged();
        }
    }

    // ------------------------------------------------------------------ spawn / dismiss

    /** ask for the mercenary to come back after the usual delay. safe to call when one is already out. */
    public static void requestRespawn(Player player) {
        if (getMerc(player) != null) {
            return;
        }
        Load.player(player).mercs.respawnTimer = RESPAWN_TICKS;
    }

    /** dismiss the current mercenary and bring the newly selected one after the delay */
    public static void switchTo(Player player, String classId) {
        MercenaryStorageData mercs = Load.player(player).mercs;
        mercs.setActive(classId);
        dismiss(player);
        mercs.respawnTimer = RESPAWN_TICKS;
        Load.player(player).playerDataSync.setDirtyAndSync(player);
    }

    /**
     * A mercenary whose owner is holding a different one's handle is an orphan, and nothing else can
     * ever remove it - {@link #dismiss} resolves the entity through {@code spawnedId}, so an entity
     * that id doesn't point at is unreachable. Left alone it follows its owner forever, because the
     * owner UUID is stable and FollowOwnerGoal doesn't care who the owner thinks is out.
     * <p>
     * O(1) and driven from the per-mercenary branch OnEntityTick already runs, so there is no scan
     * and no extra hook. A belt-and-braces guard: the Clone handler in CommonEvents is what stops
     * orphans being created in the first place.
     */
    public static void discardIfOrphan(MercenaryEntity merc) {
        if (!(merc.getOwner() instanceof Player owner)) {
            return;
        }
        UUID claimed = Load.player(owner).mercs.spawnedId;
        // null means "none out yet" - the gap between a dismiss and the respawn, during which this
        // entity may still legitimately be the one about to be adopted. never discard on null.
        if (claimed != null && !claimed.equals(merc.getUUID())) {
            merc.discard();
        }
    }

    public static void dismiss(Player player) {
        MercenaryEntity merc = getMerc(player);
        if (merc != null) {
            merc.discard();
        }
        Load.player(player).mercs.spawnedId = null;
    }

    @Nullable
    public static MercenaryEntity spawn(ServerPlayer player) {
        MercenaryStorageData mercs = Load.player(player).mercs;
        String classId = mercs.getActiveId();

        MercenaryData data = mercs.getOrCreate(classId);
        if (data.getMercClass() == null) {
            // silent nulls here are what made a spawn failure impossible to diagnose from a log
            ExileLog.get().warn("Mercenary spawn failed for " + player.getName().getString()
                    + ": no registered mercenary class for id '" + classId + "'.");
            return null;
        }

        dismiss(player);

        ServerLevel level = player.serverLevel();
        MercenaryEntity merc = SlashEntities.MERCENARY.get().create(level);
        if (merc == null) {
            ExileLog.get().warn("Mercenary spawn failed for " + player.getName().getString()
                    + ": entity type could not be created.");
            return null;
        }

        merc.setClassId(classId);
        merc.moveTo(spawnPos(player));
        merc.finalizeSpawn(level, level.getCurrentDifficultyAt(merc.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        merc.tame(player);

        // must come before the first stat calc - the gear is half of where the stats come from
        applyGear(merc, data);

        Load.Unit(merc).setLevel(Math.min(data.lvl, Load.Unit(player).getLevel()));
        Load.Unit(merc).setRarity(IRarity.COMMON_ID);
        // marks the mob stats as already set, so OnMobSpawn's EntityJoinLevelEvent handler takes the
        // "already configured" branch instead of rolling a mob level and rarity over the top.
        Load.Unit(merc).mobStatsAreSet();
        Load.Unit(merc).setEquipsChanged();

        level.addFreshEntity(merc);

        mercs.spawnedId = merc.getUUID();
        mercs.respawnTimer = 0;
        mercs.blockedTicks = 0;

        return merc;
    }

    private static Vec3 spawnPos(Player player) {
        // just behind the owner. FollowOwnerGoal sorts out the rest, and this avoids dropping the
        // mercenary inside whatever the player is standing against.
        Vec3 look = player.getLookAngle().scale(-1.5D);
        return player.position().add(look.x, 0, look.z);
    }

    // ------------------------------------------------------------------ gear

    /**
     * The stored inventory is the source of truth; the entity's equipment slots are a mirror of it.
     * Doing it this way means {@code CachedEntityStats.recalcGears/recalcWeapon} - which already walk
     * a non player's slots - produce the mercenary's gear stats with no new stat code at all, and the
     * loadout survives the mercenary being dismissed.
     */
    public static void applyGear(MercenaryEntity merc, MercenaryData data) {
        MyInventory inv = data.getGear();

        for (int i = 0; i < MercenaryInventories.GEAR_SIZE; i++) {
            EquipmentSlot slot = MercenaryInventories.gearSlotAt(i);
            ItemStack stack = inv.getItem(i);
            merc.setItemSlot(slot, stack.copy());
            // never drop the loadout on death - the stored inventory is what owns these items
            merc.setDropChance(slot, 0F);
        }
        Load.Unit(merc).setEquipsChanged();
    }

    /** re-mirror the loadout of whatever mercenary this player currently has out */
    public static void refreshGear(Player player) {
        MercenaryEntity merc = getMerc(player);
        if (merc == null) {
            return;
        }
        MercenaryData data = merc.getMercData();
        if (data != null) {
            data.setSpellUnitsDirty();
            applyGear(merc, data);
        }
    }

    /**
     * Hands back anything the mercenary is no longer allowed to have. A level change is the usual
     * cause - dropping a level can lock a support slot or shrink Spirit below what the equipped auras
     * reserve - and the design is clear that nothing should ever be destroyed, so it goes to the
     * owner rather than simply ceasing to apply.
     */
    public static void validateEquipment(Player player, MercenaryData data) {
        for (MercenarySlotType type : MercenarySlotType.values()) {
            var inv = type.inventoryOf(data);

            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                // ask as though the slot were empty, otherwise an aura always fails its own spirit
                // check by counting itself, and a mainhand fails against itself.
                inv.setItem(i, ItemStack.EMPTY);
                if (!type.mayPlace(player, data, i, stack)) {
                    MercenarySlotType.giveBack(player, stack);
                } else {
                    inv.setItem(i, stack);
                }
            }
        }
        // an equipped skill can also fall out of reach when the level drops
        for (int i = 0; i < MercenaryClass.EQUIPPED_SKILLS; i++) {
            String id = data.getEquippedSkill(i);
            MercenaryClass mc = data.getMercClass();
            if (id != null && mc != null && !mc.isSkillUnlocked(id, data.lvl)) {
                data.setEquippedSkill(i, "");
            }
        }
        refreshGear(player);
    }

    // ------------------------------------------------------------------ experience

    /**
     * The mercenary levels alongside the player and is never allowed past them. It keeps its own exp
     * curve, so a mercenary hired late still has to catch up, but it takes no level distance penalty
     * (design section 3) and loses nothing on death.
     */
    public static void giveExp(Player owner, MercenaryData data, int exp) {
        if (exp < 1) {
            return;
        }
        int ownerLvl = Load.Unit(owner).getLevel();
        if (data.lvl >= ownerLvl) {
            return;
        }

        int startLvl = data.lvl;
        data.exp += exp;

        while (data.lvl < ownerLvl) {
            int needed = LevelUtils.getExpRequiredForLevel(data.lvl + 1);
            if (data.exp < needed) {
                break;
            }
            data.exp -= needed;
            data.lvl++;
        }
        if (data.lvl >= ownerLvl) {
            // sitting at the owner's level, so banked exp would silently carry into a level the
            // mercenary hasn't earned. hold it at the cap instead.
            data.lvl = ownerLvl;
            data.exp = 0;
        }

        // this is the only place a mercenary ever gains a level - keepInStep only ever clamps it down
        // to the owner's, which is not a level up and must stay silent. one message per batch naming
        // the level landed on, so a kill worth several levels doesn't spam the chat.
        if (data.lvl > startLvl && owner instanceof ServerPlayer sp) {
            sp.displayClientMessage(Chats.MERCENARY_LEVEL_UP.locName(data.lvl).withStyle(ChatFormatting.GREEN), false);
            SoundUtils.playSound(owner, SoundEvents.ANVIL_LAND, 1F, 0.75F);
        }

        MercenaryEntity merc = getMerc(owner);
        if (merc != null && Load.Unit(merc).getLevel() != data.lvl) {
            Load.Unit(merc).setLevel(data.lvl);
            Load.Unit(merc).setEquipsChanged();
            // a level up can open a support slot or grow Spirit, which never invalidates anything -
            // but the same call keeps skills and gems honest if anything else moved the level.
            validateEquipment(owner, data);
        }
        Load.player(owner).playerDataSync.setDirty();
    }
}
