package com.robertx22.mine_and_slash.saveclasses.spells;

import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.util.ExplainedResult;
import com.robertx22.mine_and_slash.a_libraries.player_animations.PlayerAnimations;
import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.entity.EntityData;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.capability.player.helper.GemInventoryHelper;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.spells.components.Spell;
import com.robertx22.mine_and_slash.database.data.spells.components.actions.SummonPetAction;
import com.robertx22.mine_and_slash.database.data.spells.entities.CalculatedSpellData;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.CastingWeapon;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellCastContext;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.bases.SpellPredicates;
import com.robertx22.mine_and_slash.database.data.stats.types.LearnSpellStat;
import com.robertx22.mine_and_slash.database.data.stats.types.MaxAllSpellLevels;
import com.robertx22.mine_and_slash.database.data.stats.types.MaxSpellLevel;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.saveclasses.ExactStatData;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.saveclasses.skill_gem.SkillGemData;
import com.robertx22.mine_and_slash.saveclasses.unit.Unit;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.effectdatas.SpendResourceEvent;
import com.robertx22.mine_and_slash.uncommon.localization.Chats;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.RepairUtils;
import com.robertx22.mine_and_slash.vanilla_mc.packets.NoManaPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.spells.TellClientEntityCastingSpell;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class SpellCastingData {

    public static final int SPELL_KEY_NOT_EXIST = -1;
    public HashMap<Integer, String> hotbar = new HashMap<>();


    public static class HotbarSpellData {
        public Spell spell;
        public int hotbarkey;

        public HotbarSpellData(Spell spell, int hotbarkey) {
            this.spell = spell;
            this.hotbarkey = hotbarkey;
        }
    }


    public boolean learnedSpellButHotbarIsEmpty() {
        return getAllHotbarSpells().isEmpty() && !spells.isEmpty();

    }

    public int keyOfSpell(String spell) {

        for (Map.Entry<Integer, String> en : hotbar.entrySet()) {
            if (en.getValue().equals(spell)) {
                return en.getKey();
            }
        }
        return SPELL_KEY_NOT_EXIST;
    }

    public List<HotbarSpellData> getAllHotbarSpellsInfo() {
        List<HotbarSpellData> list = new ArrayList<>();
        for (Integer i : hotbar.keySet()) {
            String spell = hotbar.getOrDefault(i, "");
            if (ExileDB.Spells().isRegistered(spell)) {
                list.add(new HotbarSpellData(ExileDB.Spells().get(spell), i));
            }
        }
        return list;
    }

    public List<InsertedSpell> getAllHotbarSpells() {
        List<InsertedSpell> list = new ArrayList<>();
        for (Integer i : hotbar.keySet()) {
            list.add(getSpellData(i));
        }
        list.removeIf(x -> x == null || x.getData() == null);
        return list;
    }

    public List<InsertedSpell> spells = new ArrayList<>();


    public void setHotbar(Player p, int slot, String spell) {

        // compared as sets so moving a skill from one slot to another doesn't count as unequipping it
        Set<String> before = new HashSet<>(hotbar.values());

        for (Map.Entry<Integer, String> en : hotbar.entrySet()) {
            if (en.getValue().equals(spell)) {
                hotbar.put(en.getKey(), "");
            }
        }

        hotbar.put(slot, spell);

        // the slot just got a skill put in it by hand. if that skill unlocks fewer links than there
        // are gems socketed, the extras go back to the player now. done here and not on the recalc
        // path because this is an explicit action - the link count cannot be a one tick dip
        if (p != null && !p.level().isClientSide) {
            var gem = Load.player(p).getSkillGemInventory().getHotbarGem(slot);
            if (gem != null) {
                gem.ejectSupportsPastLinks(p);
            }
        }

        before.removeAll(hotbar.values());

        // a skill that left the bar takes its self buffs and its summons with it. otherwise a player
        // casts every buff skill in turn, swaps each one out for the next and ends up with all of
        // them at once - and the same trick collects every pet in the game onto one hotbar slot
        if (!before.isEmpty() && p != null && !p.level().isClientSide) {
            var statuses = Load.Unit(p).getStatusEffectsData();

            boolean removed = false;
            for (String unequipped : before) {
                removed |= statuses.removeSelfBuffsOfSpell(p, unequipped);
                SummonPetAction.despawnSummonsOfSpell(p, unequipped);
            }
            if (removed) {
                Load.Unit(p).sync.setDirty();
            }
        }
    }

    public void resetSpells() {
        spells.clear();
    }

    // Skills that exist only because the weapon granting them is sitting in the hotbar rather than in
    // the player's hand. They are learned in every other sense - allocatable, socketable, drawn on the
    // bar - but not castable, so a unique that grants a skill stays a weapon you wield instead of a
    // skill unlock you carry in your bag. Serialized like `spells` is; the client needs it to draw.
    public List<String> carriedOnlySpells = new ArrayList<>();

    public void calcSpellLevels(Unit unit, LivingEntity en) {

        // what was learned a moment ago, so we can notice a skill that just stopped being learned
        Set<String> before = new HashSet<>();
        for (InsertedSpell x : spells) {
            before.add(x.id);
        }

        resetSpells();
        carriedOnlySpells.clear();


        unit.getStats().stats.values()
                .forEach(x -> {
                    if (x.GetStat() instanceof LearnSpellStat learn) {
                        addSpell(new SpellCastingData.InsertedSpell(learn.spell.GUID(), (int) x.getValue()));
                    }
                });

        if (en instanceof Player p) {
            // everything above came from worn gear plus the perk tree, so it is castable. whatever the
            // hotbar pass adds on top of that is carried rather than wielded
            Set<String> wielded = new HashSet<>();
            for (InsertedSpell x : spells) {
                wielded.add(x.id);
            }

            mergeHotbarWeaponSpells(p, wielded);

            for (InsertedSpell x : spells) {
                if (!wielded.contains(x.id)) {
                    carriedOnlySpells.add(x.id);
                }
            }
        }

        unit.getStats().stats.values().forEach(x -> {
            if (x.GetStat() instanceof MaxSpellLevel max) {
                for (InsertedSpell spell : this.spells) {
                    if (spell.getSpell().config.tags.contains(max.tag)) {
                        spell.bonus_ranks += x.getValue();
                    }
                }
            } else if (x.GetStat() instanceof MaxAllSpellLevels) {
                for (InsertedSpell spell : this.spells) {
                    spell.bonus_ranks += x.getValue();
                }
            }
        });
        // caps the bonus ranks to config value max
        for (InsertedSpell spell : this.spells) {
            spell.bonus_ranks = MathHelper.clamp(spell.bonus_ranks, 0, GameBalanceConfig.get().MAX_BONUS_SPELL_LEVELS);
            spell.rank += spell.bonus_ranks;
        }

        if (en instanceof Player p) {
            cleanUpUnlearnedSpells(p, before);
        }
    }

    /**
     * A weapon that grants a skill - Poets Pen granting Kinetic Blast - keeps granting it while it sits
     * anywhere in the hotbar, not only while it is the selected item. Minecrafts main hand is whatever
     * slot you scrolled to, so without this, scrolling to a torch unlearned the skill, which zeroed its
     * link count and tore all five support gems out of their sockets. Casting is not loosened by this:
     * canCast refuses anything that ended up in carriedOnlySpells.
     */
    private void mergeHotbarWeaponSpells(Player p, Set<String> wielded) {

        var data = Load.Unit(p);

        for (int i = 0; i < 9; i++) { // the hotbar slots
            if (!Inventory.isHotbarSlot(i)) {
                continue;
            }
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.isEmpty() || !StackSaving.GEARS.has(stack)) {
                continue; // cheap nbt key check, this runs for every slot
            }
            GearItemData gear = StackSaving.GEARS.loadFrom(stack);
            if (gear == null || !gear.isValidItem() || !gear.GetBaseGearType().isWeapon()) {
                continue;
            }
            if (!gear.canPlayerWear(data)) {
                continue;
            }
            for (ExactStatData stat : gear.GetAllStats(ExileStack.of(stack))) {
                if (stat.getStat() instanceof LearnSpellStat learn) {
                    if (wielded.contains(learn.spell.GUID())) {
                        // already granted by worn gear or the perk tree, at whatever rank those add up
                        // to. a stowed weapon keeps a skill alive, it never buys it extra ranks
                        continue;
                    }
                    grantSpellRank(learn.spell.GUID(), (int) stat.getFirstValue());
                }
            }
        }
    }

    // max, never sum: carrying two copies of the same unique must not double the rank, and rank is what
    // buys support slots
    private void grantSpellRank(String id, int rank) {
        for (InsertedSpell x : spells) {
            if (x.id.equals(id)) {
                if (rank > x.rank) {
                    x.rank = rank;
                    x.rankBeforePlusSkills = rank;
                }
                return;
            }
        }
        addSpell(new InsertedSpell(id, rank));
    }

    /**
     * A skill that leaves the bar takes its self buffs and its summons with it - setHotbar does that
     * when the player swaps a slot by hand. Losing the item that granted the skill is the same event,
     * and nothing ran the cleanup on that path, so a buff or a pet from a weapon granted skill outlived
     * the weapon that granted it.
     */
    private void cleanUpUnlearnedSpells(Player p, Set<String> before) {

        if (p.level().isClientSide || before.isEmpty()) {
            return;
        }

        var statuses = Load.Unit(p).getStatusEffectsData();
        boolean removed = false;

        for (String id : before) {
            if (getSpellData(id).getData() != null) {
                continue; // still learned
            }
            if (!hotbar.containsValue(id)) {
                continue; // never on the bar, so nothing was ever cast off it
            }
            removed |= statuses.removeSelfBuffsOfSpell(p, id);
            SummonPetAction.despawnSummonsOfSpell(p, id);
        }

        if (removed) {
            Load.Unit(p).sync.setDirty();
        }
    }

    public void addSpell(InsertedSpell spell) {
        spells.add(spell);
    }

    public InsertedSpell getSpellData(int slot) {
        String id = getSpellId(slot);
        return spells.stream().filter(x -> x.id.equals(id)).findAny().orElse(new InsertedSpell("", 0));
    }

    public InsertedSpell getSpellData(String id) {
        return spells.stream().filter(x -> x.id.equals(id)).findAny().orElse(new InsertedSpell("", 0));
    }


    public String getSpellId(int slot) {
        return hotbar.getOrDefault(slot, "");
    }


    public static class InsertedSpell {

        public String id;
        public int rankBeforePlusSkills = 0;
        public int rank;
        public int bonus_ranks = 0;

        public Spell getSpell() {
            return ExileDB.Spells().get(id);
        }

        public InsertedSpell(String id, int rank) {
            this.id = id;
            this.rank = rank;
            this.rankBeforePlusSkills = rank;
        }

        public SkillGemData getData() {

            if (id.isEmpty()) {
                return null;
            }

            SkillGemData data = new SkillGemData();
            data.id = id;
            data.type = SkillGemData.SkillGemType.SKILL;

            data.perc = (int) ((rankBeforePlusSkills / (float) data.getSpell().max_lvl) * 100);

            // bonus ranks from +skill gear grant power but no extra slots, hence rankBeforePlusSkills
            data.setLinks(Math.min(
                    rankBeforePlusSkills / GemInventoryHelper.RANKS_PER_SUPPORT_SLOT,
                    GemInventoryHelper.SUPPORT_GEMS_PER_SKILL));

            return data;
        }
    }

    public int castTickLeft = 0;
    public int castTicksDone = 0;
    public int spellTotalCastTicks = 0;
    public CalculatedSpellData calcSpell = null;
    public Boolean casting = false;
    public ChargeData charges = new ChargeData();

    // How long a press stays castable after the key comes back up. Short on purpose: it exists so a
    // tap is not swallowed, not so a cast can happen once the player has moved on
    static final int INPUT_BUFFER_TICKS = 5;

    // Every hotbar slot whose key is down right now, one bit each. the server fills this in from the
    // input packet, the client from its own keybinds, so both agree on when a channel is still held
    public transient int heldSlotMask = 0;
    // The client's copy of the above. it cannot live on this object: PlayerData.loadOrBlank replaces
    // spellCastingData wholesale on every sync, which zeroes every transient field on it. that made
    // the client read "no keys held" for a tick and falsely end its own channel
    public static int CLIENT_HELD_SLOT_MASK = 0;
    // Slots pressed within the last INPUT_BUFFER_TICKS, whether or not they are still down
    transient int bufferedSlotMask = 0;
    transient int bufferTicks = 0;
    // Where the next slot walk starts. advancing it past every cast is what makes a key bound to
    // several skills play the next one instead of the same one forever
    transient int rotationSlot = 0;
    // How long the client may go silent before its held keys are forgotten. Measured in real time,
    // not server ticks: a server catching up after a hitch replays its missed ticks back to back in
    // a few milliseconds, so a tick countdown expired before any keepalive could possibly arrive and
    // the channel dropped on every small lag spike. Generous on purpose. it is only a safety net for
    // a client that stopped talking, a release is signalled at once by the mask-change packet
    static final long CHANNEL_INPUT_TIMEOUT_MS = 1000;
    // Util.getMillis() reading past which heldSlotMask is treated as stale
    transient long spellInputDeadlineMillis = 0;

    // called from the client keybind poll. the server goes through onSpellInputPressed instead
    public void setHeldSlots(int heldMask) {
        CLIENT_HELD_SLOT_MASK = heldMask;
    }

    public void onSpellInputPressed(int heldMask) {
        // a key that just went down is remembered for a moment even after it comes back up. without
        // this a tap whose press and release land in the same server tick is lost entirely, and a
        // press arriving a few ticks before the global cooldown opens is thrown away
        int justPressed = heldMask & ~heldSlotMask;
        if (justPressed != 0) {
            bufferedSlotMask |= justPressed;
            bufferTicks = INPUT_BUFFER_TICKS;
        }
        heldSlotMask = heldMask;
        spellInputDeadlineMillis = Util.getMillis() + CHANNEL_INPUT_TIMEOUT_MS;
    }

    public boolean tryStartSpellCast(Player player, Spell spell) {

        var data = Load.player(player);
        var cds = Load.Unit(player).getCooldowns();

        if (player.isBlocking() || player.swinging) {
            return false;
        }

        if (spell != null) {

            // the gate holds every skill except one that is off the global cooldown - those are
            // exactly the ones meant to be pressed while another skill is still recovering
            if (cds.isOnCooldown(CooldownsData.GLOBAL_COOLDOWN) && !spell.getConfig().isOffGlobalCooldown()) {
                return false;
            }

            var can = canCast(spell, player);

            if (can.can) {

                ItemStack wep = player.getMainHandItem();

                if (!wep.isEmpty() && !RepairUtils.isItemBroken(wep)) {
                    wep.hurt(1, player.getRandom(), (ServerPlayer) player);
                }

                SpellCastContext c = new SpellCastContext(player, 0, spell);
                setToCast(c);
                if (!spell.getConfig().isChannel()) {
                    // a channel pays per pulse in tryChannelPulse, so letting go early costs nothing
                    spell.spendResources(c);
                    // no cooldown is armed here. the cast is not recovery - it is the skill happening,
                    // and isCasting() already holds the input for its whole duration. everything the
                    // cast costs is armed once by onSpellCastFinished, at the end
                    Load.Unit(player).sync.setDirty();
                }

                data.playerDataSync.setDirty();
                return true;
            } else if (!cds.isOnCooldown("spell_fail")) {
                cds.setOnCooldown("spell_fail", 40);
                if (can.answer != null) {
                    if (Load.Unit(player).getLevel() < 15 || Load.player(player).config.isConfigEnabled(PlayerConfigData.Config.CAST_FAIL)) {
                        player.sendSystemMessage(Chats.CAST_FAILED.locName().append(can.answer));
                    }
                }
            }

        }
        return false;
    }

    public boolean tryStartSpellCast(Player player, int number) {
        // getHotbarGem returns null on its internal exception path, and this runs every tick from
        // processSpellInputs - outside the try in onTimePass, so an NPE here eats the rest of the tick
        var gem = Load.player(player).getSkillGemInventory().getHotbarGem(number);
        if (gem == null) {
            return false;
        }
        return tryStartSpellCast(player, gem.getSpell());
    }

    public void cancelCast(LivingEntity entity) {
        try {
            if (isCasting()) {
                SpellCastContext ctx = new SpellCastContext(entity, 0, getSpellBeingCast());

                Spell spell = getSpellBeingCast();
                if (spell != null && !entity.level().isClientSide) {
                    // server side only, same reason as setCooldownOnCasted
                    int cd = ctx.spell.getEffectiveCooldownTicks(ctx);
                    Load.Unit(entity)
                            .getCooldowns()
                            .setOnCooldown(spell.GUID(), cd);

                    // an interrupted cast still owes the recovery, but only from where it stopped -
                    // the gate was holding the remaining cast time and that time is not being spent
                    armGlobalCooldown(ctx);
                    Load.Unit(entity).sync.setDirty();
                }

                this.calcSpell = null;
                castTickLeft = 0;
                spellTotalCastTicks = 0;
                castTicksDone = 0;
                this.casting = false;

                if (entity instanceof ServerPlayer p) {
                    TellClientEntityCastingSpell.sendUpdates(PlayerAnimations.CastEnum.CAST_FINISH, p, spell);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isCasting() {
        return calcSpell != null && casting && ExileDB.Spells()
                .isRegistered(calcSpell.spell_id);
    }

    public boolean isChannelling() {
        Spell spell = getSpellBeingCast();
        return isCasting() && spell != null && spell.getConfig().isChannel();
    }

    // a channel keeps going only while a key that maps to it is still down
    private boolean isChannelInputHeld(LivingEntity entity) {
        Spell channelled = getSpellBeingCast();
        if (channelled == null || !(entity instanceof Player p)) {
            return false;
        }
        // the client tracks its own keybinds in a static, the server gets the mask from the packet
        int mask = entity.level().isClientSide ? CLIENT_HELD_SLOT_MASK : heldSlotMask;

        // any held slot holding this same skill counts. tracking a single slot instead would drop the
        // channel the moment a shared bind reported one of the other skills on that key
        var inv = Load.player(p).getSkillGemInventory();
        for (int slot = 0; slot < GemInventoryHelper.MAX_SKILL_GEMS; slot++) {
            if ((mask & (1 << slot)) == 0) {
                continue;
            }
            var gem = inv.getHotbarGem(slot);
            Spell held = gem == null ? null : gem.getSpell();
            if (held != null && held.GUID().equals(channelled.GUID())) {
                return true;
            }
        }
        return false;
    }

    // the server decides whether a channel may keep going. the client keeps predicting pulses until the
    // cast finished packet lands, otherwise it stutters whenever its resource values are a tick stale.
    private boolean canPulseChannel(SpellCastContext ctx, Spell spell) {
        if (ctx.caster.level().isClientSide) {
            return true;
        }
        if (ctx.caster instanceof Player p && p.isCreative()) {
            return true;
        }
        if (!spell.isAllowedInDimension(ctx.caster.level())) {
            return false;
        }
        if (RepairUtils.isItemBroken(ctx.caster.getMainHandItem())) {
            return false;
        }
        return ctx.data.getResources().hasEnoughForBoth(spell.getManaCostCtx(ctx), spell.getEnergyCostCtx(ctx));
    }

    // stops a channel part way through a pulse interval. nothing was pre paid, so this costs nothing
    private void endChannel(LivingEntity entity) {
        Spell spell = getSpellBeingCast();
        SpellCastContext ctx = new SpellCastContext(entity, castTicksDone, spell);

        onSpellCastFinished(ctx);

        this.calcSpell = null;
        this.castTickLeft = 0;
        this.spellTotalCastTicks = 0;
        this.castTicksDone = 0;

        if (entity instanceof ServerPlayer p) {
            TellClientEntityCastingSpell.sendUpdates(PlayerAnimations.CastEnum.CAST_FINISH, p, spell);
        }
    }

    transient static Spell lastSpell = null;

    private void processSpellInputs(Player player) {

        if (Util.getMillis() > spellInputDeadlineMillis) {
            heldSlotMask = 0; // client went quiet
        }
        if (bufferTicks > 0) {
            bufferTicks--;
        } else {
            bufferedSlotMask = 0;
        }

        // an input lives exactly as long as the key is down, plus INPUT_BUFFER_TICKS. nothing is
        // stored beyond that, so a skill can never fire from a press the player already let go of
        int mask = heldSlotMask | bufferedSlotMask;
        if (mask == 0) {
            return;
        }

        // a cast in progress owns the input. going through tryStartSpellCast here would fail with
        // ALREADY_CASTING every tick and spam the cast failed message. this covers instant skills too,
        // where the gate below is empty because there is no cast time to hold it
        if (isCasting()) {
            return;
        }

        // one skill per global cooldown. this is what turns a shared bind from a simultaneous volley
        // into a rotation, and it also covers the cast time of whatever is already going off.
        // decided per slot rather than up front: an off global cooldown skill (a buff, a curse, a
        // dodge) is let through the gate, that is the whole point of it being off the cooldown
        boolean onGlobalCooldown = Load.Unit(player).getCooldowns().isOnCooldown(CooldownsData.GLOBAL_COOLDOWN);
        var gems = Load.player(player).getSkillGemInventory();

        // walk the live slots once from the rotation point, so a key holding several skills plays the
        // next one rather than all of them, and a slot that cannot cast right now yields to the next
        for (int i = 0; i < GemInventoryHelper.MAX_SKILL_GEMS; i++) {
            int slot = (rotationSlot + i) % GemInventoryHelper.MAX_SKILL_GEMS;
            if ((mask & (1 << slot)) == 0) {
                continue;
            }
            if (onGlobalCooldown) {
                // resolved here so canCast, which runs a full stat event, is never reached for a gated
                // skill - this keeps the walk as cheap as the early return it replaces
                var gem = gems.getHotbarGem(slot);
                Spell held = gem == null ? null : gem.getSpell();
                if (held == null || !held.getConfig().isOffGlobalCooldown()) {
                    continue;
                }
            }
            if (tryStartSpellCast(player, slot)) {
                rotationSlot = (slot + 1) % GemInventoryHelper.MAX_SKILL_GEMS;
                bufferedSlotMask &= ~(1 << slot); // this press has been spent
                return;
            }
            // it could not be cast at all - own cooldown, no mana, no charges, mid swing
        }
    }

    public void onTimePass(LivingEntity entity) {

        if (entity instanceof ServerPlayer player) {
            processSpellInputs(player);
        }

        if (isCasting()) {
            try {
                Spell spell = this.calcSpell.getSpell();

                if (entity.level().isClientSide && spell != null && !spell.getConfig().isChannel()) {
                    // the client only draws the bar. it does not finish the cast: the server's
                    // CAST_FINISH does, via TellClientEntityCastingSpell -> cancelCast. counting to
                    // zero here and clearing the state made the bar complete on the client's clock
                    // while a lagging server still had ticks to go, so the player saw the cast end,
                    // nothing happen, and then the cooldown land. parked at zero the bar stays full
                    // until the server actually fires. a channel keeps predicting its pulse loop
                    if (castTickLeft > 0) {
                        castTickLeft--;
                        castTicksDone++;
                    }
                    return;
                }

                if (isChannelling() && !isChannelInputHeld(entity)) {
                    endChannel(entity);
                    return;
                }

                castTickLeft--;
                castTicksDone++;

                SpellCastContext ctx = new SpellCastContext(entity, castTicksDone, spell);
                ctx.castTotalTicks = this.spellTotalCastTicks;

                if (spell != null && ExileDB.Spells()
                        .isRegistered(spell)) {
                    spell.onCastingTick(ctx);
                }

                tryCast(ctx);

                lastSpell = spell;

                if (castTickLeft <= 0) {

                    if (!spell.getConfig().isChannel()) {
                        // a channel consumes these per pulse in tryChannelPulse instead, so that a stack
                        // is spent per cast either way no matter how the channel ends
                        consumeBuffsRemovedOnCast(ctx, spell);
                    }

                    if (ctx.caster instanceof ServerPlayer p) {
                        Load.Unit(ctx.caster).sync.setDirty();
                        TellClientEntityCastingSpell.sendUpdates(PlayerAnimations.CastEnum.CAST_FINISH, p, ctx.spell);
                    }

                    this.calcSpell = null;
                }
            } catch (Exception e) {
                // named so a live log can tell which skill died here: cancelCast stamps the full
                // cooldown with nothing fired, which looks exactly like a successful cast to the player
                System.err.println("Spell cast tick failed for " + (calcSpell == null ? "?" : calcSpell.spell_id) + ", cancelling:");
                e.printStackTrace();
                this.cancelCast(entity);
                // cancel when error, cus this is called on tick, so it doesn't crash servers when 1 spell fails
            }
        } else {
            lastSpell = null;
        }
    }

    public List<String> getSpellsOnCooldown(LivingEntity en) {
        return Load.Unit(en)
                .getCooldowns()
                .getAllSpellsOnCooldown();
    }

    public void setToCast(SpellCastContext ctx) {

        this.calcSpell = ctx.calcData;
        this.castTickLeft = ctx.spell.getCastTimeTicks(ctx);
        this.spellTotalCastTicks = this.castTickLeft;
        this.castTicksDone = 0;
        this.casting = true;

        if (ctx.caster instanceof ServerPlayer p) {
            TellClientEntityCastingSpell.sendUpdates(PlayerAnimations.CastEnum.CAST_START, p, ctx.spell);
        }
    }

    public void tryCast(SpellCastContext ctx) {

        if (getSpellBeingCast() != null) {
            if (castTickLeft <= 0) {
                Spell spell = getSpellBeingCast();

                if (spell.getConfig().isChannel()) {
                    tryChannelPulse(ctx, spell);
                    return;
                }

                int timesToCast = ctx.spell.getConfig().times_to_cast;

                if (timesToCast == 1) {
                    spell.cast(ctx);
                }

                onSpellCastFinished(ctx);
                this.calcSpell = null;

            }
        }

    }

    // effects flagged remove_on_spell_cast lose a stack when a matching spell actually goes off
    private void consumeBuffsRemovedOnCast(SpellCastContext ctx, Spell spell) {
        for (Map.Entry<String, ExileEffectInstanceData> en : ctx.data.statusEffects.exileMap.entrySet()) {
            ExileEffect eff = ExileDB.ExileEffects().get(en.getKey());
            if (eff.remove_on_spell_cast != null) {
                if (spell.config.tags.contains(eff.remove_on_spell_cast)) {
                    en.getValue().stacks--;
                }
            }
        }
    }

    // one beat of a channel: pay for it, fire it, then arm the timer for the next one. the caller has
    // already seen castTickLeft hit zero, so falling through here ends the channel and starts the cd.
    private void tryChannelPulse(SpellCastContext ctx, Spell spell) {

        if (!isChannelInputHeld(ctx.caster) || !canPulseChannel(ctx, spell)) {
            onSpellCastFinished(ctx);
            this.calcSpell = null;
            return;
        }

        if (!ctx.caster.level().isClientSide) {
            spell.spendResources(ctx);
        }
        spell.cast(ctx);
        consumeBuffsRemovedOnCast(ctx, spell);

        // recomputed every pulse, so cast speed changes take effect on the very next one
        this.castTickLeft = spell.getCastTimeTicks(ctx);
        this.spellTotalCastTicks = this.castTickLeft;
        this.castTicksDone = 0;
    }

    public Spell getSpellBeingCast() {

        if (calcSpell != null) {
            return calcSpell.getSpell();
        }


        return null;
    }

    public ExplainedResult canCast(Spell spell, Player player) {

        if (player.level().isClientSide) {
            return ExplainedResult.failure(Component.literal("Client side"));
        }
        if (isCasting()) {
            return ExplainedResult.failure(Chats.ALREADY_CASTING.locName());
        }


        if (spell == null) {
            return ExplainedResult.failure(Component.literal("Trying to cast NULL Spell, this shouldn't happen"));
        }

        if (spell.getLevelOf(player) < 1) {
            return ExplainedResult.failure(Component.literal("You did not learn this spell"));
        }

        // learned only because the weapon granting it is stowed in the hotbar. the skill keeps its slot
        // and its support gems, but a granting unique still has to be wielded to fire it - otherwise it
        // is just a skill unlock you carry while swinging a better weapon
        if (carriedOnlySpells.contains(spell.GUID())) {
            return ExplainedResult.failure(Chats.MUST_HOLD_GRANTING_WEAPON.locName());
        }

        if (Load.Unit(player).getCooldowns().isOnCooldown(spell.GUID())) {
            // dont spam chat with no cd msgs for stuff like fireball
            if (Load.Unit(player).getCooldowns().getCooldownTicks(spell.GUID()) > 40) {
                return ExplainedResult.failure(Chats.SPELL_IS_ON_CD.locName());
            }
            return ExplainedResult.silentlyFail();
        }


        if (player.isCreative()) {
            return ExplainedResult.success();
        }

        if (spell.GUID().contains("test")) {
            if (!MMORPG.RUN_DEV_TOOLS) {
                return ExplainedResult.failure(Chats.USING_TEST_SPELL.locName());
            }
        }

        if (spell.config.charges > 0) {
            if (!charges.hasCharge(spell.config.charge_name)) {
                return ExplainedResult.failure(Chats.NO_CHARGES.locName());
            }
        }

        SpellCastContext ctx = new SpellCastContext(player, 0, spell);


        EntityData data = Load.Unit(player);

        if (data != null) {

            if (!spell.isAllowedInDimension(player.level())) {
                return ExplainedResult.failure(Chats.NOT_IN_THIS_DIMENSION.locName());
            }

            SpendResourceEvent mana = spell.getManaCostCtx(ctx);
            SpendResourceEvent energy = spell.getEnergyCostCtx(ctx);


            if (data.getResources().hasEnoughForBoth(mana, energy)) {

                var opt = Load.Unit(player).equipmentCache.getWeaponOpt();

                if (RepairUtils.isItemBroken(player.getMainHandItem())) {
                    return ExplainedResult.failure(Chats.CANT_CAST_WITH_BROKEN_WEAPON.locName());
                }


                if (!CompatConfig.get().ignoreWeaponReqForSpells()) {

                    GearItemData wep = opt.map(x -> x.gear).orElse(null);

                    if (wep == null) {
                        return ExplainedResult.failure(Chats.NOT_MNS_WEAPON.locName());
                    }

                    if (!spell.getConfig().castingWeapon.predicate.predicate.test(player)) {
                        // If the spell requires a mage weapon and the player is a battlemage, allow casting
                        if (spell.getConfig().castingWeapon == CastingWeapon.MAGE_WEAPON && data.getUnit().isBattlemage()) {
                            // Do nothing, allow casting
                        } else {
                            return ExplainedResult.failure(Chats.WRONG_CASTING_WEAPON.locName());
                        }
                    }

                    if (!wep.canPlayerWear(ctx.data)) {
                        return ExplainedResult.failure(Chats.WEAPON_REQ_NOT_MET.locName());
                    }
                }

                return ExplainedResult.success();
            } else {
                if (player instanceof ServerPlayer) {
                    Packets.sendToClient((Player) player, new NoManaPacket());
                    return ExplainedResult.failure(Chats.NO_MANA.locName());
                }
            }
        }
        return ExplainedResult.silentlyFail();

    }

    public void setCooldownOnCasted(SpellCastContext ctx) {

        // cooldowns are the server's to decide - the client only ticks down and draws what it is sent.
        // canCast already refuses client side, so the only way we get here on the client is a
        // mispredicted channel end, and letting that stamp a cooldown flashes the whole hotbar
        if (ctx.caster.level().isClientSide) {
            return;
        }

        // the long cooldown skills are the only ones where cooldown_ticks still decides
        int cd = ctx.spell.getEffectiveCooldownTicks(ctx);

        ctx.data.getCooldowns().setOnCooldown(ctx.spell.GUID(), cd);

        if (ctx.spell.config.charges > 0) {
            if (ctx.caster instanceof Player) {
                int chargecd = ctx.spell.getChargeCooldownTicks(ctx);
                this.charges.spendCharge((Player) ctx.caster, ctx.spell, chargecd);
            }
        }

        if (ctx.caster instanceof Player) {
            Player p = (Player) ctx.caster;
            if (p.isCreative()) {
                if (cd > 20) {
                    ctx.data.getCooldowns().setOnCooldown(ctx.spell.GUID(), 20);
                }
            }
        }

    }

    // the one place the global cooldown is written from a player cast. an off global cooldown skill
    // never writes it: it was allowed to start while another skill's recovery was running, and
    // stamping its own 2 tick floor over that would hand the player a recovery reset - fireball,
    // buff, fireball again 7 ticks later instead of 20. the skill's own cooldown is stamped
    // separately in setCooldownOnCasted, so it still cannot fire twice in one tick
    private void armGlobalCooldown(SpellCastContext ctx) {
        if (ctx.spell.getConfig().isOffGlobalCooldown()) {
            return;
        }
        ctx.data.getCooldowns().setOnCooldown(CooldownsData.GLOBAL_COOLDOWN, ctx.spell.getCastSpeedTicks(ctx));
    }

    public void onSpellCastFinished(SpellCastContext ctx) {

        setCooldownOnCasted(ctx);
        this.casting = false;

        if (!ctx.caster.level().isClientSide) {
            // recovery starts when the cast ends, never when it began - a cast time is time spent, not
            // time recovered. a channel follows the same rule, its cast simply runs until the key is up
            armGlobalCooldown(ctx);
            ctx.data.sync.setDirty(); // same reason as in tryStartSpellCast

            if (ctx.spell.getConfig().isChannel() && ctx.caster instanceof ServerPlayer p) {
                // the client predicts the pulse loop, so it needs to hear the channel is over right away
                Load.player(p).playerDataSync.setDirty();
            }
        }

        /*
        if (ctx.caster instanceof ServerPlayer p) {
            Load.Unit(ctx.caster).sync.setDirty();
            Packets.sendToClient(p, new TellClientEntityCastingSpell(PlayerAnimations.CastEnum.CAST_FINISH, p, ctx.spell));
        }

         */
    }

}
