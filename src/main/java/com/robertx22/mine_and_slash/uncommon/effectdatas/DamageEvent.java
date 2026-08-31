package com.robertx22.mine_and_slash.uncommon.effectdatas;

import com.robertx22.library_of_exile.events.base.ExileEvents;
import com.robertx22.library_of_exile.main.Packets;
import com.robertx22.library_of_exile.utils.SoundUtils;
import com.robertx22.mine_and_slash.a_libraries.dmg_number_particle.particle.InteractionNotifier;
import com.robertx22.mine_and_slash.aoe_data.database.ailments.Ailment;
import com.robertx22.mine_and_slash.capability.entity.CooldownsData;
import com.robertx22.mine_and_slash.capability.player.data.PlayerConfigData;
import com.robertx22.mine_and_slash.config.forge.ServerContainer;
import com.robertx22.mine_and_slash.config.forge.compat.CompatConfig;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect;
import com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData;
import com.robertx22.mine_and_slash.database.data.game_balance_config.GameBalanceConfig;
import com.robertx22.mine_and_slash.database.data.rarities.MobRarity;
import com.robertx22.mine_and_slash.database.data.spells.spell_classes.SpellCtx;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayerData;
import com.robertx22.mine_and_slash.database.data.stats.layers.StatLayers;
import com.robertx22.mine_and_slash.database.data.stats.types.offense.FullSwingDamage;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.DamageAbsorbedByMana;
import com.robertx22.mine_and_slash.database.data.stats.types.resources.magic_shield.MagicShield;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.event_hooks.damage_hooks.util.AttackInformation;
import com.robertx22.mine_and_slash.loot.LootUtils;
import com.robertx22.mine_and_slash.mixin_ducks.DamageSourceDuck;
import com.robertx22.mine_and_slash.mixin_ducks.LivingEntityAccesor;
import com.robertx22.mine_and_slash.mixin_ducks.ProjectileEntityDuck;
import com.robertx22.mine_and_slash.mmorpg.MMORPG;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import com.robertx22.mine_and_slash.uncommon.MathHelper;
import com.robertx22.mine_and_slash.uncommon.datasaving.Load;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import com.robertx22.mine_and_slash.uncommon.enumclasses.AttackType;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import com.robertx22.mine_and_slash.uncommon.enumclasses.WeaponTypes;
import com.robertx22.mine_and_slash.uncommon.interfaces.EffectSides;
import com.robertx22.mine_and_slash.uncommon.localization.Words;
import com.robertx22.mine_and_slash.uncommon.utilityclasses.*;
import com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity;
import com.robertx22.mine_and_slash.vanilla_mc.packets.DmgNumPacket;
import com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

public class DamageEvent extends EffectEvent {
    public static ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, SlashRef.id("mod"));

    public static String ID = "on_damage";
    public static String dmgSourceName = SlashRef.MODID + ".custom_damage";
    static AttributeModifier NO_KNOCKBACK = new AttributeModifier(
            UUID.fromString("e926df30-c376-11ea-87d0-0242ac131053"),
            Attributes.KNOCKBACK_RESISTANCE.getDescriptionId(),
            100,
            AttributeModifier.Operation.ADDITION
    );
    public LivingEntity petEntity;
    public float wepdmgMulti = 1;
    public boolean absorbedCompletely = false;
    public AttackInformation attackInfo;
    private HashMap<Elements, Integer> bonusElementDamageMap = new HashMap();

    // kept apart from bonusElementDamageMap on purpose. this damage isn't extra damage the attacker
    // deals, it's the hit the target is already taking, re-elemented by the target's own "taken as"
    // stats. it has been through every attacker multiplier once already, so the event built from it
    // must not run them again - see buildBonusElementEvent.
    private HashMap<Elements, Integer> damageTakenAsMap = new HashMap();

    public float unconvertedDamagePercent = 100;
    public float unconvertedDamageTakenAsPercent = 100;

    // how many times this damage has already been split off into another element. the original hit is
    // 0, the bonus element events it spawns are 1, anything those convert away is 2. conversion stats
    // stop firing at the cap so a pair of stats converting into each other can't loop forever.
    public static final int MAX_CONVERSION_DEPTH = 2;
    public int conversionDepth = 0;

    // these two are identical for this hit and for every bonus-element copy of it (same source,
    // same target, same tick), but each costs world/map data lookups to work out. resolve them
    // lazily once and hand the answers down to the copies in calculateAllBonusElementalDamage.
    private Boolean sourceInMapWorld = null;
    private static final float MAP_RES_MULTI_NOT_CALCULATED = -1;
    private float mapResReqDmgMulti = MAP_RES_MULTI_NOT_CALCULATED;

    private boolean isSourceInMapWorld() {
        if (sourceInMapWorld == null) {
            sourceInMapWorld = WorldUtils.isMapWorldClass(source.level(), source.blockPosition());
        }
        return sourceInMapWorld;
    }

    // returns 1 when there's no penalty - addMoreMulti ignores a multi of 1, and the real penalty
    // is always clamped to at least 2, so 1 is safe to use as the "no penalty" value.
    private float getMapResReqDmgMulti(GameBalanceConfig balance) {
        if (mapResReqDmgMulti == MAP_RES_MULTI_NOT_CALCULATED) {
            mapResReqDmgMulti = 1;

            var map = Load.mapAt(target.level(), target.blockPosition());
            if (map != null && map.map != null) {
                var req = map.map.getStatReq();
                var targetUnit = Load.Unit(target);

                if (!req.meetsReq(map.map.lvl, targetUnit)) {
                    float minusres = req.getLackingResistNumber(map.map.lvl, targetUnit);
                    mapResReqDmgMulti = Math.max((float) (minusres * balance.MOB_DMG_MULTI_PER_MAP_RES_REQ_LACKING), 2.0f);
                }
            }
        }
        return mapResReqDmgMulti;
    }

    protected DamageEvent(AttackInformation attackInfo, LivingEntity source, LivingEntity target, float dmg) {
        super(dmg, source, target);
        this.attackInfo = attackInfo;
        calcBlock();

        if (this.targetData.immuneTicks > 0 && attackInfo != null) {
            this.cancelDamage();
        }

        // config blacklisted entities never take mns damage, they only take plain vanilla damage.
        // don't use cancelDamage() here, it zeroes attackInfo's amount and would eat the vanilla damage too
        if (target != null && ServerContainer.get().isMnsDamageBlacklisted(target)) {
            this.data.getNumber(EventData.NUMBER).number = 0;
            this.data.setBoolean(EventData.DISABLE_KNOCKBACK, true);
            this.data.setBoolean(EventData.CANCELED, true);
        }

    }

    @Override
    public String GUID() {
        return ID;
    }

    public void addMobDamageMultipliers() {

        try {

            if (!calcSourceEffects && !calcTargetEffects) {
                return;// temp fix for mobs doing too much ailment proc dmg
            }

            if (source instanceof Player == false) {


                if (target instanceof Player) {
                    if (sourceData.getLevel() > targetData.getLevel()) {
                        float penalty = LootUtils.getLevelDistancePunishmentMulti(sourceData.getLevel(), targetData.getLevel());

                        if (penalty < 1) {
                            float dmgmulti = 2F - penalty;
                            this.addMoreMulti(() -> Words.HIGH_LVL_MOB_DMG_MULTI.locName(), EventData.NUMBER, dmgmulti);
                        }
                    }
                }

                var balance = GameBalanceConfig.get();


                if (balance.MOB_DMG_POWER_SCALING != 1) {
                    float multi = (float) (balance.MOB_DMG_POWER_SCALING_BASE * (float) Math.pow(balance.MOB_DMG_POWER_SCALING, sourceData.getLevel()));
                    this.addMoreMulti(() -> Words.LVL_EXPONENT_MOB_DMG.locName(), EventData.NUMBER, multi);
                }

                MobRarity rar = sourceData.getMobRarity();

                float enconfigmulti = (float) sourceData.getEntityConfig().dmg_multi;

                this.addMoreMulti(() -> Words.MOB_RARITY_MULTI.locName(), EventData.NUMBER, rar.DamageMultiplier());

                if (enconfigmulti != 1) {
                    this.addMoreMulti(() -> Words.MOB_CONFIG_MULTI.locName(), EventData.NUMBER, enconfigmulti);
                }

                if (isSourceInMapWorld()) {
                    if (target instanceof Player) {
                        this.addMoreMulti(() -> Words.MAP_RES_REQ_LACK_DMG_MULTI.locName(), EventData.NUMBER, getMapResReqDmgMulti(balance));
                    }
                }

            } else {
                if (targetData.getLevel() > sourceData.getLevel()) {
                    if (target instanceof Player == false) {
                        float penalty = LootUtils.getLevelDistancePunishmentMulti(sourceData.getLevel(), targetData.getLevel());
                        if (penalty < 1) {
                            this.addMoreMulti(() -> Words.DMG_TO_HIGH_LVL_MOB_DMG_MULTI.locName(), EventData.NUMBER, penalty);
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Optional<Ailment> getAilment() {
        var id = data.getString(EventData.AILMENT);

        if (ExileDB.Ailments().isRegistered(id)) {
            var ailment = ExileDB.Ailments().get(id);
            return Optional.of(ailment);
        }
        return Optional.empty();
    }

    public Component getDamageName() {

        try {
            if (this.data.isBasicAttack()) {
                return Words.BASIC_ATTACK.locName();
            }

            // ailment lines must name the ailment, not the spell that triggered them. both the shatter
            // burst and the freeze application carry the triggering spell's guid, so checking the spell
            // first made all three lines of a single cast read identically
            var ailment = getAilment();
            if (ailment.isPresent()) {
                if (data.getBoolean(EventData.IS_AILMENT_PROC)) {
                    return ailment.get().procNameWord().locName();
                }
                return ailment.get().locName();
            }

            if (this.data.isSpellEffect()) {
                return getSpell().locName();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Words.UNKNOWN_DAMAGE.locName();
    }

    public AttackType getAttackType() {
        return data.getAttackType();
    }

    public Elements getElement() {
        return data.getElement();
    }

    public void setElement(Elements ele) {
        this.data.setElement(ele);
    }

    public void addBonusEleDmg(Elements element, float dmg, EffectSides side) {
        if (element == getElement()) {
            this.getLayer(StatLayers.Offensive.FLAT_DAMAGE, EventData.NUMBER, side).add(dmg);
        } else {
            bonusElementDamageMap.put(element, (int) (bonusElementDamageMap.getOrDefault(element, 0) + dmg));
        }
    }

    // damage the target's own "x damage taken as y" stats moved out of this hit. it can't go through
    // addBonusEleDmg: that would route it back into the FLAT_DAMAGE layer when the element matches
    // (already applied by the time this runs, so the damage would just vanish), and would make the
    // event that carries it re-run every attacker multiplier the damage already went through.
    public void addDamageTakenAsEleDmg(Elements element, float dmg) {
        damageTakenAsMap.put(element, (int) (damageTakenAsMap.getOrDefault(element, 0) + dmg));
    }

    /*
    public void addBonusEleDmgFinal(DamageConversionEvent event, Elements element, float dmg) {
        if (element == getElement()) {
            this.getLayer(StatLayers.Offensive.FLAT_DAMAGE, EventData.NUMBER, EffectSides.Source).add(dmg);
        } else {
            bonusElementDamageMap.put(element, (int) (bonusElementDamageMap.getOrDefault(element, 0) + dmg));
        }
    }

     */

    private void calcBlock() {

        if (!canAvoidHit()) {
            return;
        }

        if (targetData
                .getResources()
                .getEnergy() < 1) {
            return;
        }

        // blocking check
        if (target.isBlocking() && attackInfo != null) {
            Vec3 vec3d = attackInfo.getSource()
                    .getSourcePosition();
            if (vec3d != null) {
                Vec3 vec3d2 = target.getViewVector(1.0F);
                Vec3 vec3d3 = vec3d.vectorTo(target.position())
                        .normalize();
                vec3d3 = new Vec3(vec3d3.x, 0.0D, vec3d3.z);
                if (vec3d3.dot(vec3d2) < 0.0D) {
                    this.data.setHitAvoided(EventData.IS_BLOCKED);
                    SoundUtils.playSound(source, SoundEvents.SHIELD_BLOCK);
                }
            }
        }
    }

    public float getActualDamage() {
        float dmg = this.data.getNumber();
        if (dmg <= 0) {
            return 0;
        }
        return dmg;
    }


    private void calcAttackCooldown() {
        if (data.isNumberSetup(EventData.ATTACK_COOLDOWN)) {
            return;
        }

        float cool = 1;

        WeaponTypes weaponType = data.getWeaponType();

        if (this.data.getAttackType() != AttackType.dot && weaponType.isMelee() && !isSpell()) {

            if (this.source instanceof Player) {

                var wep = this.sourceData.equipmentCache.getWeapon();
                if (wep != null) {
                    GearItemData gear = wep.gear;

                    if (gear != null) {
                        var attri = source.getAttribute(Attributes.ATTACK_SPEED);

                        float atkpersec = attri != null ? (float) attri.getValue() : 1;

                        //float secWaited = (float) (target.tickCount - target.getLastHurtByMobTimestamp()) / 20F;

                        String cdname = source.getStringUUID();

                        if (!targetData.getCooldowns().isOnCooldown(cdname)) {
                            cool = 1;
                        } else {
                            float atkpersecticks = (1F / atkpersec * 20);
                            float atkcd = targetData.getCooldowns().getCooldownTicks(cdname);

                            float waitedticks = atkpersecticks - atkcd;

                            float multi = waitedticks / atkpersecticks;

                            multi = MathHelper.clamp(multi, 0, 1);

                            cool = multi;

                        }
                        int cd = (int) (1F / atkpersec * 20);
                        targetData.getCooldowns().setOnCooldown(cdname, cd);

                        if (cool < 0.3) {
                            this.cancelDamage();
                        }

                    }
                }
            }
        }
        data.setupNumber(EventData.ATTACK_COOLDOWN, cool);
    }

    private float getAttackSpeedDamageMulti() {

        if (ServerContainer.get().REMOVE_ATK_SPEED_COOLDOWN.get()) {
            return 1;
        }

        float cool = data.getNumber(EventData.ATTACK_COOLDOWN).number;

        // we no longer remove damage from fast clicked attacks, energy exists! dmg *= cool;

        if (cool < 0.1F) {
            // we dont want to allow too fast mob clickings
            cool = 0;
            this.cancelDamage();
        }
        return cool;
    }


    private void modifyIfArrowDamage() {
        if (attackInfo != null && attackInfo.getSource() != null) {
            if (attackInfo.getSource().getDirectEntity() instanceof ProjectileEntityDuck) {
                if (data.getWeaponType() == WeaponTypes.bow) {

                    if (!ServerContainer.get().REMOVE_DRAW_SPEED_COOLDOWN.get()) {

                        // don't use this for crossbows, only bows need to be charged fully

                        ProjectileEntityDuck duck = (ProjectileEntityDuck) attackInfo.getSource().getDirectEntity();

                        float arrowmulti = duck.my$getDmgMulti();

                        this.addMoreMulti(() -> Words.ARROW_DRAW_AMOUNT_MULTI.locName(), EventData.NUMBER, arrowmulti);

                        // multiply dmg by saved charge value
                    }
                }

                disableIframesForFastRangedAttacks();
            }
        }

    }

    // a high attack speed bow/crossbow build (auto fire, Quickdraw) can land consecutive shots well
    // under vanilla's ~10 tick mob invulnerability window, so follow-up arrows silently deal no
    // damage instead of missing visibly - the same "bouncing off" symptom disableMobIframes() already
    // fixes for spells/DoTs that call target.hurt() directly (see the attackInfo == null branch in
    // activate()). that existing bypass never reaches arrows, because arrow damage takes the
    // attackInfo != null / overridesDamage branch instead, which lets vanilla's own already-running
    // hurt() call reach its unmodified iframe check on its own. this runs earlier in that same call -
    // initBeforeActivating happens before activate(), both before vanilla ever reaches its check - so
    // zeroing it here lands in time
    private void disableIframesForFastRangedAttacks() {
        if (!data.getWeaponType().isProjectile) {
            return;
        }
        if (target instanceof Player) {
            return; // keep vanilla iframes for PvP
        }
        if (!CompatConfig.get().disableMobIframes()) {
            return;
        }
        target.invulnerableTime = 0;
    }

    public boolean areBothPlayers() {
        if (source instanceof ServerPlayer && target instanceof ServerPlayer) {
            return true;
        }
        return false;
    }

    // the player is both the caster and the target. self inflicted costs should read as a resource
    // cost, not as taking a hit, so they skip the hurt sound (and the vanilla tilt/knockback, see
    // SelfDamageNoTiltMixin)
    public boolean isPlayerSelfDamage() {
        return source == target && target instanceof Player;
    }

    // you can't dodge or block a hit you inflicted on yourself - self damage is a resource cost,
    // not an incoming attack. mitigation still applies, only avoidance is skipped. mirrors
    // NO_SELF_DAMAGE_STATS, which turns off the attacker half of the same hit.
    public boolean canAvoidHit() {
        return source != target;
    }

    public void cancelDamage() {
        this.data.getNumber(EventData.NUMBER).number = 0;
        this.data.setBoolean(EventData.DISABLE_KNOCKBACK, true);

        this.data.setBoolean(EventData.CANCELED, true);
        if (attackInfo != null) {
            attackInfo.setCanceled(true);
        }
        return;
    }

    public boolean allowSelfDamage = false;


    public boolean stopFriendlyFire() {

        if (allowSelfDamage) {
            if (source == target) {
                return false;
            }
        }
        if (isSourceInMapWorld()) {
            // in maps, we dont want mobs to damage each other
            if (AllyOrEnemy.allies.is(source, target)) {
                cancelDamage();
                return true;
            }
        } else {
            // outside maps, we want zombies to kill villagers etc
            if (source instanceof Player) {
                if (AllyOrEnemy.allies.is(source, target)) {
                    cancelDamage();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return "Damage Event";
    }

    @Override
    public void initBeforeActivating() {

        var initevent = new DamageInitEvent(this);
        initevent.Activate();

        calcAttackCooldown();

        addMobDamageMultipliers();


        if (source instanceof Player) {
            if (data.isBasicAttack()) {
                float multi = getAttackSpeedDamageMulti();
                if (multi > 0.8F) {
                    float fullswing = sourceData.getUnit().getCalculatedStat(FullSwingDamage.getInstance()).getMultiplier();
                    this.addMoreMulti(FullSwingDamage.getInstance(), EventData.NUMBER, fullswing);
                }
                this.addMoreMulti(() -> Words.ATTACK_SPEED_MULTI.locName(), EventData.NUMBER, multi);
            }
            modifyIfArrowDamage();
        }

        // todo this should be in layers too or multis
        if (areBothPlayers()) {
            this.addMoreMulti(() -> Words.PVP_DMG_MULTI.locName(), EventData.NUMBER, ServerContainer.get().PVP_DMG_MULTI.get().floatValue());
        }


        if (this.data.isBasicAttack()) {
            if (this.attackInfo != null && attackInfo.weaponData != null) {
                if (!data.getBoolean(EventData.UNARMED_ATTACK)) {
                    float multi = attackInfo.weaponData.GetBaseGearType().getGearSlot().getBasicDamageMulti();
                    this.wepdmgMulti = multi;
                    this.addMoreMulti(() -> Words.WEAPON_BASIC_ATTACK_DMG_MULTI.locName(), EventData.NUMBER, multi);
                }
            }
        }
    }


    // wait, bonus archmage dmg is applied to any bonus ele dmg??

    // todo this is using total ele dmg and saying only 1 ele, fuck
    public MutableComponent getDamageMessage(DmgByElement info) {

        MutableComponent ele = Component.literal(getElement().getIconNameDmg());

        if (info.isMixedDamage()) {
            ele = Component.literal("\u2600" + " ").append(Words.MULTI_ELEMENT.locName()).withStyle(ChatFormatting.LIGHT_PURPLE);
        }

        Words word = Words.DAMAGE_MESSAGE;

        var ailment = getAilment();
        if (disableActivation && ailment.isPresent()) {
            // this event never deals its damage, it only feeds the dot or the shatter/shock pool
            word = ailment.get().isDot ? Words.AILMENT_PROC_MESSAGE : Words.AILMENT_ACCUMULATE_MESSAGE;
        }

        return word.locName(
                        source.getDisplayName(),
                        MMORPG.DECIMAL_FORMAT.format(info.totalDmg),
                        ele,
                        getDamageName()
                )
                .withStyle(Style.EMPTY.applyFormat(ChatFormatting.RED)
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, getInfoHoverMessage(info, true))));

    }

    public MutableComponent getInfoHoverMessage(DmgByElement info, boolean doBonusDmg) {
        // int main = info.dmgmap.getOrDefault(getElement(), 0F).intValue();


        MutableComponent msg = Component.empty();

        if (this.isSpell()) {
            msg.append(Words.DAMAGE_TYPE_SPELL.locName(getSpell().locName().plainCopy()).withStyle(ChatFormatting.AQUA));
        }
        if (this.data.isBasicAttack()) {
            msg.append(Words.DAMAGE_TYPE_BASIC_ATTACK.locName().withStyle(ChatFormatting.RED));
        }
        // the shatter/shock burst is sent as AttackType.dot so it doesn't knock back, but it's a one
        // time hit, not damage over time - don't label it as such
        if (this.data.getBoolean(EventData.IS_AILMENT_PROC)) {
            msg.append(Words.DAMAGE_TYPE_AILMENT_PROC.locName(getDamageName()).withStyle(ChatFormatting.RED));
        } else if (this.data.getAttackType() == AttackType.dot) {
            msg.append(Words.DAMAGE_TYPE_AILMENT.locName().withStyle(ChatFormatting.RED));
        }

        if (!this.data.getString(EventData.AILMENT).isEmpty()) {
            String ailment = this.data.getString(EventData.AILMENT);
            var ai = ExileDB.Ailments().get(ailment);
            msg.append(Words.AILMENT_DAMAGE.locName().append(ai.locName()).append("\n").withStyle(ai.element.format));
        }

        msg.append(Words.ELEMENTAL_DAMAGE.locName(getElement().getIconNameDmg()).withStyle(getElement().format));

        msg.append(Words.BASE_DAMAGE.locName((int) this.data.getOriginalNumber(EventData.NUMBER).number).withStyle(ChatFormatting.BLUE));

        msg.append(Words.DAMAGE_INFO.locName().withStyle(ChatFormatting.RED));

        for (StatLayerData layerData : this.getSortedLayers()) {
            if (layerData.numberID.equals(EventData.NUMBER)) {
                msg.append(layerData.getLayer().getTooltip(this, layerData).append("\n"));
            }
        }

        if (!getMoreMultis().isEmpty()) {
            msg.append(Words.MULTIPLIERS.locName().withStyle(ChatFormatting.LIGHT_PURPLE));

            for (MoreMultiData multi : this.getMoreMultis()) {
                if (multi.numberid.equals(EventData.NUMBER)) {
                    msg.append(multi.getText().append(": ").append(Component.literal("x" + MMORPG.DECIMAL_FORMAT.format(multi.multi)))).append("\n");
                }
            }
        }

        msg.append(Words.FINAL_DAMAGE.locName(info.dmgmap.getOrDefault(getElement(), 0F).intValue()).withStyle(ChatFormatting.GOLD));


        if (doBonusDmg) {
            if (info.isMixedDamage()) {

                for (Entry<Elements, Float> en : info.dmgmap.entrySet()) {
                    if (en.getKey() != getElement()) {

                        msg.append(Words.BONUS_DAMAGE_TYPE.locName().withStyle(ChatFormatting.YELLOW));

                        var dmg = info.eventMap.get(en.getKey());
                        msg.append(dmg.getInfoHoverMessage(info, false));

                    }
                }
            }
        }

        if (doBonusDmg) {
            msg.append(Component.literal("\n"));
            msg.append(Words.TOTAL_COMBINE_DAMAGE.locName((int) info.totalDmg).withStyle(ChatFormatting.GOLD));
        }

        // only the application event shows damage it never deals. the shatter/shock burst and the dot
        // ticks are hurting the enemy right now, so the note would contradict what the player sees
        if (disableActivation && getAilment().isPresent()) {
            msg.append(Words.AILMENT_DAMAGE_NOTE.locName().withStyle(ChatFormatting.BLUE));
        }

        return msg;
    }

    public void sendDamageMessage(DmgByElement info) {
        if (target instanceof Player p) {
            if (Load.player(p).config.isConfigEnabled(PlayerConfigData.Config.DAMAGE_MESSAGES)) {
                try {
                    p.sendSystemMessage(getDamageMessage(info));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (source instanceof Player p) {
            if (Load.player(p).config.isConfigEnabled(PlayerConfigData.Config.DAMAGE_MESSAGES)) {
                try {
                    p.sendSystemMessage(getDamageMessage(info));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void activate() {

        if (target.getHealth() <= 0F || !target.isAlive()) {
            return;
        }
        if (attackInfo != null) {
            if (CompatConfig.get().damageSystem().overridesDamage) {
                //attackInfo.setToMinimalNonZero();
                attackInfo.setAmount(0);
            }
        }
        if (stopFriendlyFire()) {
            return;
        }


        this.targetData.lastDamageTaken = this;

        // this has to be checked before the bonus element damage is calculated. every bonus element
        // builds a whole extra DamageEvent and sweeps every stat of both entities, and an avoided
        // hit throws all of that work away.
        if (data.isHitAvoided()) {
            if (attackInfo != null) {
                attackInfo.setCanceled(true);
            }
            cancelDamage();
            if (source instanceof ServerPlayer) {
                InteractionNotifier.notifyClient(getAttackType().isAttack() ? IParticleSpawnMaterial.Type.DODGE : IParticleSpawnMaterial.Type.RESIST, (ServerPlayer) source, target);
            }

            //move this sound to InteractionResultHandler.
            //SoundUtils.playSound(target, SoundEvents.SHIELD_BLOCK, 1, 1.5F);
            return;
        }

        DmgByElement info = calculateAllBonusElementalDamage();

        float dmg = info.totalDmg;


        sendDamageMessage(info);


        if (target instanceof Player p) { // todo this code sucks
            // a getter should not modify anything
            dmg = DamageAbsorbedByMana.modifyEntityDamage(this, dmg);
            dmg = MagicShield.modifyEntityDamage(this, info, dmg);
        }
        
        // (START NEW) trigger resource lost event for health
        if (target instanceof ServerPlayer sp) {
            com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceLost.trigger(
                sp,
                com.robertx22.mine_and_slash.saveclasses.unit.ResourceType.health,
                dmg,
                com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceLost.LossSource.Damage
            );
        }
        // (END NEW)

        float vanillaDamage = HealthUtils.realToVanilla(target, dmg);

        if (absorbedCompletely) {
            if (vanillaDamage < 0.0001F) {
                vanillaDamage = 0.0001F;
            }
        }

        if (this.data.isCanceled()) {
            cancelDamage();
            return;
        }


        AttributeInstance attri = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);

        boolean suppressKnockback = attri != null
                && (data.getBoolean(EventData.DISABLE_KNOCKBACK) || this.getAttackType() == AttackType.dot);

        if (suppressKnockback) {
            if (!attri.hasModifier(NO_KNOCKBACK)) {
                // transient and not permanent on purpose: this is added and removed inside this
                // method, and a permanent modifier is written into the entity's nbt, so failing to
                // remove it once would leave the mob knockback immune across restarts
                attri.addTransientModifier(NO_KNOCKBACK);
            }
            targetData.noKnockbackDepth++;
        }

        try {
            DamageSource dmgsource = new DamageSource(source.level().registryAccess().registry(Registries.DAMAGE_TYPE).get().getHolderOrThrow(DAMAGE_TYPE), source, source, source.position());


            if (this.data.isSpellEffect()) {
                if (!data.getBoolean(EventData.DISABLE_KNOCKBACK) && dmg > 0 && !data.isHitAvoided()) {
                    // if magic shield absorbed the damage, still do knockback
                    DashUtils.knockback(source, target);
                }
                // play spell hurt sounds or else spells will feel like they do nothing
                if (!isPlayerSelfDamage()) {
                    LivingEntityAccesor duck = (LivingEntityAccesor) target;
                    SoundEvent sound = SoundEvents.GENERIC_HURT;
                    float volume = duck.myGetHurtVolume();
                    float pitch = duck.myGetHurtPitch();
                    SoundUtils.playSound(target, sound, volume, pitch);
                }
            }


            var config = Load.Unit(target).getEntityConfig();

            if (target instanceof Player == false && config != null && config.set_health_damage_override) {
                float hp = MathHelper.clamp(target.getHealth() - vanillaDamage, 0, target.getMaxHealth() + 1);
                target.setHealth(hp);
                // todo this might create bugs but its probably better that damage actually works..
                if (target.getHealth() <= 0) {
                    ExileEvents.DAMAGE_BEFORE_CALC.callEvents(new ExileEvents.OnDamageEntity(dmgsource, vanillaDamage, target));
                    ExileEvents.DAMAGE_AFTER_CALC.callEvents(new ExileEvents.OnDamageEntity(dmgsource, vanillaDamage, target));
                    target.die(target.damageSources().mobAttack(this.source));
                }
                if (attackInfo != null) {
                    attackInfo.setAmount(0.000001F);
                }
            } else {

                if (attackInfo != null && CompatConfig.get().damageSystem().overridesDamage) {
                    DamageSourceDuck duck = (DamageSourceDuck) attackInfo.getSource();
                    duck.setMnsDamage(vanillaDamage);
                    duck.tryOverrideDmgWithMns(attackInfo);
                    attackInfo.setAmount(vanillaDamage);
                } else {

                    DamageSourceDuck duck = (DamageSourceDuck) dmgsource;
                    duck.setMnsDamage(vanillaDamage);

                    if (target instanceof Player == false) {
                        int inv = target.invulnerableTime;
                        target.invulnerableTime = 0;
                        target.hurt(dmgsource, vanillaDamage);
                        target.invulnerableTime = inv;
                    } else {
                        target.hurt(dmgsource, vanillaDamage);
                    }
                }
            }
        } finally {
            // target.hurt() runs foreign code (other mods, death handling). if it throws, the
            // exception is swallowed further up in LivingHurtUtils.onAttack, so without this the
            // modifier would stay on the mob for good.
            if (attri != null) {
                if (attri.getValue() >= 1.0) {
                    target.hurtMarked = false;
                }
                if (suppressKnockback) {
                    targetData.noKnockbackDepth--;
                }
                // remove once no nested event still needs it. this stays unconditional so it also
                // strips a modifier left stuck on the entity by an older version, which used to
                // clean itself up here on the next hit.
                if (targetData.noKnockbackDepth <= 0) {
                    targetData.noKnockbackDepth = 0;
                    if (attri.hasModifier(NO_KNOCKBACK)) {
                        attri.removeModifier(NO_KNOCKBACK);
                    }
                }
            }
        }

        if (dmg > 0) {
            // todo can this be done better?
            if (this.data.isBasicAttack()) {
                for (Entry<String, ExileEffectInstanceData> e : targetData.getStatusEffectsData().exileMap.entrySet().stream().toList()) {
                    if (!e.getValue().shouldRemove()) {
                        var data = e.getValue();
                        var sd = data.calcSpell;
                        var ctx = SpellCtx.onEntityBasicAttacked(this.source, sd, target).setSourceEffect(data);
                        ExileEffect eff = ExileDB.ExileEffects().get(e.getKey());
                        if (eff.spell != null) {
                            eff.spell.tryActivate(SpellCtx.ON_ENTITY_ATTACKED, ctx); // i can use this kind of as event
                        }
                    }
                }
            }


            if (source instanceof Player p) {

                p.setLastHurtMob(target); // this allows summons to know who to attack

                sourceData.getCooldowns().setOnCooldown(CooldownsData.IN_COMBAT, 20 * 10);
                if (target instanceof Mob) {
                    // the player is the source in both cases so the threat scales off the same unit that
                    // scaled the damage - for a minion that's the owner's pet spell unit, which routes
                    // support gems back to the summon spell. only the aggro credit goes to the summon.
                    GenerateThreatEvent threatEvent = new GenerateThreatEvent(p, (Mob) target, ThreatGenType.deal_dmg, dmg, getSpellOrNull());

                    if (petEntity instanceof LivingEntity && Load.Unit(petEntity).isSummon()) {
                        threatEvent.threatOwner = petEntity;
                    }
                    threatEvent.Activate();
                }
                InteractionNotifier.notifyClient(IParticleSpawnMaterial.DamageInformation.fromDmgByElement(info, data.isCrit()), (ServerPlayer) source, target);

            } else if (source instanceof Mob) {
                if (target instanceof Player) {
                    targetData.getCooldowns().setOnCooldown(CooldownsData.IN_COMBAT, 20 * 10);

                    GenerateThreatEvent threatEvent = new GenerateThreatEvent((Player) target, (Mob) source, ThreatGenType.take_dmg, dmg);
                    threatEvent.Activate();
                }
            }

            // a mercenary's damage numbers go to its owner - the only feedback the owner gets that it
            // is contributing anything. deliberately just the numbers: the player-only side effects
            // above (last hurt mob, in-combat cooldown, threat credit) stay with the player, and chat
            // stays silent because sendDamageMessage is gated on `source instanceof Player` on its own.
            if (source instanceof MercenaryEntity mercSource && mercSource.getOwner() instanceof ServerPlayer mercOwner) {
                InteractionNotifier.notifyClient(
                        IParticleSpawnMaterial.DamageInformation.fromDmgByElement(info, data.isCrit()), mercOwner, target);
            }
            //sendDamageParticle(info);

            // target.invulnerableTime = 20;

            if (CompatConfig.get().disableMobIframes()) {
                if (attackInfo == null) {
                    if (target instanceof Player == false) {
                        target.invulnerableTime = 0;
                    }
                }
            }

        }

    }

    // reintroduce if i can't figure how to fix his dmg particle
    private void sendDamageParticle(DmgByElement info) {

        String text = "";

        if (source instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) source;

            if (data.isHitAvoided()) {
                if (getAttackType().isAttack()) {
                    text = "Dodge";
                } else {
                    text = "Resist";
                }

                DmgNumPacket packet = new DmgNumPacket(target, text, false, ChatFormatting.GOLD);
                Packets.sendToClient(player, packet);
                return;
            }

            for (Entry<Elements, Float> entry : info.dmgmap.entrySet()) {
                if (entry.getValue()
                        .intValue() > 0) {

                    text = entry.getKey().format + NumberUtils.formatDamageNumber(this, entry.getValue()
                            .intValue());

                    DmgNumPacket packet = new DmgNumPacket(target, text, data.isCrit(), entry.getKey().format);
                    Packets.sendToClient(player, packet);
                }
            }

        }
    }

    // builds the extra event that carries one element's worth of bonus damage. every copy takes its
    // metadata from this event, the original hit, no matter how many conversion rounds deep it is -
    // depth is the only thing that differs, and it's what stops conversion from recursing forever.
    //
    // takenAs flips it from "extra damage the attacker deals" to "the same damage, re-elemented by the
    // target". that damage has already been through every attacker multiplier on the parent event, so
    // it skips initBeforeActivating (attack speed, full swing, weapon, pvp and high level mob multis)
    // and the whole source stat sweep (increased damage, crit, penetration, conversion, ailments) and
    // only picks up the target's mitigation for its new element.
    private DamageEvent buildBonusElementEvent(Elements element, float amount, int depth, boolean takenAs) {
        // this how do i make a copy of the same event that it was at the start..except element
        DamageEvent bonus = EventBuilder.ofDamage(attackInfo, source, target, amount)
                .setupDamage(AttackType.bonus_dmg, data.getWeaponType(), data.getStyle())
                .set(x -> {
                    if (isSpell()) {
                        x.data.setString(EventData.SPELL, this.data.getString(EventData.SPELL));
                    }
                    x.data.setBoolean(EventData.IS_BONUS_ELEMENT_DAMAGE, true);

                    // same hit, same positions - don't redo the map lookups per element
                    x.sourceInMapWorld = this.sourceInMapWorld;
                    x.mapResReqDmgMulti = this.mapResReqDmgMulti;

                    x.conversionDepth = depth;
                    x.calcSourceEffects = !takenAs;

                    x.data.setBoolean(EventData.IS_BASIC_ATTACK, this.data.getBoolean(EventData.IS_BASIC_ATTACK));
                    // a conversion/added flat event re-runs the whole source stat sweep, so every proc
                    // stat fires on it again. without carrying this, converted damage from a summon (or
                    // from a spell a summon's hit procced) reads as a hit the player landed themselves
                    // and slips past every "is_summon_attack is false" guard - which is the only thing
                    // stopping summon procs from summoning more summons forever.
                    x.data.setBoolean(EventData.IS_SUMMON_ATTACK, this.data.getBoolean(EventData.IS_SUMMON_ATTACK));
                    // a full block avoids the hit and returns before any of this is built, so this
                    // only ever carries a partial block (Block Damage Reduction below 100). without
                    // it every element split would roll block again on its own and one attack would
                    // end up blocked in pieces.
                    x.data.setBoolean(EventData.IS_BLOCKED, this.data.getBoolean(EventData.IS_BLOCKED));
                    // one attack gets one avoidance decision. dodge is allowed to run on bonus_dmg (added
                    // flat physical on a non physical skill arrives as its own event and would otherwise
                    // bypass Dodge Rating entirely), so without carrying this every element split of an
                    // already resolved hit would roll again - and each roll charges the entropy counter,
                    // which would push the realised dodge rate well past the listed one.
                    x.data.setBoolean(EventData.AVOIDANCE_ROLLED, this.data.getBoolean(EventData.AVOIDANCE_ROLLED));
                    x.data.setBoolean(EventData.IS_ATTACK_FULLY_CHARGED, this.data.getBoolean(EventData.IS_ATTACK_FULLY_CHARGED));
                    x.data.setupNumber(EventData.ATTACK_COOLDOWN, this.data.getNumber(EventData.ATTACK_COOLDOWN).number);
                    x.data.setupNumber(EventData.DMG_EFFECTIVENESS, this.data.getNumber(EventData.DMG_EFFECTIVENESS).number);
                    if (wepdmgMulti != 1) {
                        //  x.addMoreMulti(Words.WEAPON_BASIC_ATTACK_DMG_MULTI.locName(), EventData.NUMBER, wepdmgMulti);
                    }

                    x.setElement(element);
                })
                .build();

        if (!takenAs) {
            bonus.initBeforeActivating();
        }
        bonus.calculateEffects();

        bonus.setElement(element);
        bonus.calculateEffects();

        return bonus;
    }

    // this calculates all the bonus elemental damages, uses the specific numbers for particles only, and the totalvalue for actually dealing dmg, ONCE
    public DmgByElement calculateAllBonusElementalDamage() {
        DmgByElement info = new DmgByElement();

        // a bonus element event runs the full stat sweep, so conversion and "taken as" stats fire on it
        // too and push part of it into a *different* element - into that event's own maps, which nothing
        // else reads. drain them round by round or that damage is silently lost. conversionDepth caps
        // this; today only physical converts and nothing converts back to it, so round 2 always comes
        // back empty, but the cap keeps a future two way conversion from looping forever.
        HashMap<Elements, Integer> pending = new HashMap<>(bonusElementDamageMap);
        HashMap<Elements, Integer> pendingTakenAs = new HashMap<>(damageTakenAsMap);

        for (int depth = conversionDepth + 1;
             (!pending.isEmpty() || !pendingTakenAs.isEmpty()) && depth <= MAX_CONVERSION_DEPTH;
             depth++) {

            HashMap<Elements, Integer> next = new HashMap<>();
            HashMap<Elements, Integer> nextTakenAs = new HashMap<>();

            for (Entry<Elements, Integer> entry : pending.entrySet()) {
                resolveBonusElement(info, entry, depth, false, next, nextTakenAs);
            }
            for (Entry<Elements, Integer> entry : pendingTakenAs.entrySet()) {
                resolveBonusElement(info, entry, depth, true, next, nextTakenAs);
            }

            pending = next;
            pendingTakenAs = nextTakenAs;
        }

        info.addDmg(this, this.getActualDamage(), this.getElement());

        return info;

    }

    private void resolveBonusElement(DmgByElement info, Entry<Elements, Integer> entry, int depth, boolean takenAs,
                                     HashMap<Elements, Integer> next, HashMap<Elements, Integer> nextTakenAs) {
        if (entry.getValue() <= 0) {
            return;
        }

        DamageEvent bonus = buildBonusElementEvent(entry.getKey(), entry.getValue(), depth, takenAs);

        float dmg = bonus.getActualDamage();

        // a fully converted element resolves to 0. adding it anyway would leave an empty entry in the
        // map and make isMixedDamage() call a single element hit "Multi Element"
        if (dmg > 0) {
            info.addDmg(bonus, dmg, bonus.getElement());
        }

        bonus.bonusElementDamageMap.forEach((ele, num) -> next.merge(ele, num, Integer::sum));
        bonus.damageTakenAsMap.forEach((ele, num) -> nextTakenAs.merge(ele, num, Integer::sum));
    }

    public Elements GetElement() {
        return getElement();
    }

    public void setisAilmentDamage(Ailment al) {
        this.data.setString(EventData.AILMENT, al.GUID());
    }

    public float getPenetration() {
        return this.data.getNumber(EventData.PENETRATION).number;
    }

    public void setPenetration(float val) {
        this.data.getNumber(EventData.PENETRATION).number = val;
    }

    public static class DmgByElement {

        public float totalDmg = 0;
        private HashMap<Elements, Float> dmgmap = new HashMap<>();
        private HashMap<Elements, DamageEvent> eventMap = new HashMap<>();

        public boolean isMixedDamage() {
            int bonusdmg = (int) dmgmap.entrySet().stream().filter(x -> true).count();
            return bonusdmg > 1;
        }

        public HashMap<Elements, Float> getDmgmap() {
            return dmgmap;
        }

        public void addDmg(DamageEvent event, float dmg, Elements element) {

            Elements ele = element;

            if (ele == null) {
                ele = Elements.Physical;
            }

            // read and write under the same key - reading under the raw `element` would look up a
            // null key, miss, and overwrite Physical's running total instead of adding to it
            float total = (dmgmap.getOrDefault(ele, 0F) + dmg);

            dmgmap.put(ele, total);
            eventMap.put(ele, event);

            totalDmg += dmg;
        }

    }

}
