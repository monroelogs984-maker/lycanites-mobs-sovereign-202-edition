package com.lycanitesmobs.core.event;

import com.lycanitesmobs.LycanitesMobs;
import com.lycanitesmobs.core.capabilities.level.ExtendedWorld;
import com.lycanitesmobs.core.data.config.ConfigExtra;
import com.lycanitesmobs.core.entity.IGroupBoss;
import com.lycanitesmobs.core.event.mobevent.MobEventSchedule;
import com.lycanitesmobs.core.event.mobevent.trigger.AltarMobEventTrigger;
import com.lycanitesmobs.core.event.mobevent.trigger.MobEventTrigger;
import com.lycanitesmobs.core.event.mobevent.trigger.RandomMobEventTrigger;
import com.lycanitesmobs.core.event.mobevent.trigger.TickMobEventTrigger;
import com.lycanitesmobs.core.manager.CreatureManager;
import com.lycanitesmobs.core.manager.MobEventManager;
import com.lycanitesmobs.core.manager.ObjectManager;
import com.lycanitesmobs.core.util.helpers.LMHelperClass;
import com.lycanitesmobs.core.util.math.SchismMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.lycanitesmobs.core.entity.creature.aberration.EntityFear;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Starts mob events (schedules, tick triggers and the random event countdown) and, like the official class, also
 * implements the behaviour of most custom effects (paralysis, weight, instability, plague, smited, bleed,
 * smouldering, swiftswimming, immunization, cleansed, lifeleak, fallresist, penetration, leech, repulsion,
 * rejuvenation, decay, insomnia, aphagia).
 *
 * Port (NeoForge 1.21.1) event mapping: LivingTickEvent -> EntityTickEvent.Pre; LivingAttackEvent + LivingHurtEvent ->
 * one LivingIncomingDamageEvent listener (lifeleak first, as its cancel used to stop the hurt event too);
 * PlayerSleepInBedEvent -> CanPlayerSleepEvent; LivingEntityUseItemEvent (all phases) -> its cancellable Start and
 * Tick phases. Swiftswimming modifiers use ResourceLocation ids (1.21 dropped modifier UUIDs).
 * Not ported: the fear effect's haunting (needs EntityFear, the unported dummy creature) and its login cleanup; the
 * jump cancel (LivingJumpEvent was never cancellable, so it was dead code upstream too); the extra random
 * MessageEntityVelocity sent with instability (the vanilla motion packet the official sent first still syncs it).
 */
public class MobEventListener {
    private static MobEventListener INSTANCE;

    private final List<RandomMobEventTrigger> randomMobEventTriggers = new ArrayList<>();
    private final List<TickMobEventTrigger> tickMobEventTriggers = new ArrayList<>();

    public static MobEventListener getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MobEventListener();
        }
        return INSTANCE;
    }

    public static void register() {
        MobEventListener listener = getInstance();
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Pre event) -> listener.onWorldUpdate(event.getLevel()));
        NeoForge.EVENT_BUS.addListener(listener::onEntityUpdate);
        NeoForge.EVENT_BUS.addListener(listener::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, listener::onLivingDamage);
        NeoForge.EVENT_BUS.addListener(listener::onEntityHeal);
        NeoForge.EVENT_BUS.addListener(listener::onSleep);
        NeoForge.EVENT_BUS.addListener(listener::onLivingUseItemStart);
        NeoForge.EVENT_BUS.addListener(listener::onLivingUseItemTick);
    }


    // ==================================================
    //                     Triggers
    // ==================================================
    /**
     * Adds a new Mob Event Trigger.
     * @return True on success, false if it failed to add (could happen if the Trigger type has no matching list created yet).
     */
    public boolean addTrigger(MobEventTrigger mobEventTrigger) {
        if (mobEventTrigger instanceof RandomMobEventTrigger randomMobEventTrigger && !this.randomMobEventTriggers.contains(randomMobEventTrigger)) {
            this.randomMobEventTriggers.add(randomMobEventTrigger);
            return true;
        }
        if (mobEventTrigger instanceof TickMobEventTrigger tickMobEventTrigger && !this.tickMobEventTriggers.contains(tickMobEventTrigger)) {
            this.tickMobEventTriggers.add(tickMobEventTrigger);
            return true;
        }
        return false;
    }

    public void removeTrigger(MobEventTrigger mobEventTrigger) {
        this.randomMobEventTriggers.remove(mobEventTrigger);
        this.tickMobEventTriggers.remove(mobEventTrigger);
        if (mobEventTrigger instanceof AltarMobEventTrigger altarMobEventTrigger) {
            altarMobEventTrigger.onRemove();
        }
    }


    // ==================================================
    //                   World Update
    // ==================================================
    /**
     * Called every tick in a world and counts down to the next event then fires it! The countdown is paused during an event.
     **/
    public void onWorldUpdate(Level world) {
        if (world.isClientSide) {
            return;
        }
        ExtendedWorld worldExt = ExtendedWorld.getForWorld(world);
        if (worldExt == null) {
            return;
        }

        if (!MobEventManager.getInstance().areMobEventsEnabled() || world.getDifficulty() == Difficulty.PEACEFUL) {
            if (worldExt.hasServerWorldEventPlayer()) {
                worldExt.stopWorldEvent();
            }
            return;
        }

        if (!worldExt.markEventScheduleTickIfFresh(world.getGameTime())) {
            return;
        }
        long eventScheduleTick = worldExt.getLastEventScheduleTime();

        for (MobEventSchedule mobEventSchedule : MobEventManager.getInstance().getMobEventSchedules()) {
            if (mobEventSchedule.canStart(world)) {
                mobEventSchedule.start(worldExt);
            }
        }

        for (TickMobEventTrigger mobEventTrigger : this.tickMobEventTriggers) {
            mobEventTrigger.onTick(world, eventScheduleTick);
        }

        if (MobEventManager.getInstance().areRandomMobEventsEnabled()) {
            if (MobEventManager.getInstance().getMinEventsRandomDay() > 0
                    && Math.floor(worldExt.getConfiguredDayBaseTime(world) / 24000D) < MobEventManager.getInstance().getMinEventsRandomDay()) {
                return;
            }
            if (worldExt.getWorldEventStartTargetTime() <= 0
                    || worldExt.getWorldEventStartTargetTime() > world.getGameTime() + MobEventManager.getInstance().getMaxTicksUntilEvent()) {
                worldExt.setWorldEventStartTargetTime(world.getGameTime() + worldExt.getRandomEventDelay(world.random));
            }
            if (world.getGameTime() == worldExt.getWorldEventStartTargetTime()) {
                this.triggerRandomMobEvent(world, worldExt, 1);
            } else if (world.getGameTime() > worldExt.getWorldEventStartTargetTime()) {
                worldExt.setWorldEventStartTargetTime(0);
            }
        }
    }

    /**
     * Triggers a Random Mob Event Trigger if one is available.
     **/
    public void triggerRandomMobEvent(Level world, ExtendedWorld worldExt, int level) {
        List<RandomMobEventTrigger> validTriggers = new ArrayList<>();
        int totalWeights = 0;
        int highestPriority = 0;
        for (RandomMobEventTrigger mobEventTrigger : this.randomMobEventTriggers) {
            if (mobEventTrigger.getPriority() >= highestPriority && mobEventTrigger.canTrigger(world, null)) {
                if (mobEventTrigger.getPriority() > highestPriority) {
                    totalWeights = 0;
                    validTriggers.clear();
                }
                totalWeights += mobEventTrigger.getWeight();
                highestPriority = mobEventTrigger.getPriority();
                validTriggers.add(mobEventTrigger);
            }
        }
        if (totalWeights <= 0) {
            return;
        }

        int randomWeight = 1;
        if (totalWeights > 1) {
            randomWeight = world.random.nextInt(totalWeights - 1) + 1;
        }
        int searchWeight = 0;
        for (RandomMobEventTrigger mobEventTrigger : validTriggers) {
            if (mobEventTrigger.getWeight() + searchWeight > randomWeight) {
                mobEventTrigger.trigger(world, null, new BlockPos(0, 0, 0), level, -1);
                return;
            }
            searchWeight += mobEventTrigger.getWeight();
        }
    }


    // ==================================================
    //                  Effect Behaviour
    // ==================================================
    private static final ResourceLocation[] SWIFTSWIMMING_MODIFIER_IDS = {
            ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "swiftswimming_1"),
            ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "swiftswimming_2"),
            ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "swiftswimming_3"),
            ResourceLocation.fromNamespaceAndPath(LycanitesMobs.MODID, "swiftswimming_4")
    };

    private boolean tickEffectsResolved;
    private Holder<MobEffect> paralysisEffect;
    private Holder<MobEffect> weightEffect;
    private Holder<MobEffect> fearEffect;
    private Holder<MobEffect> instabilityEffect;
    private Holder<MobEffect> plagueEffect;
    private Holder<MobEffect> smitedEffect;
    private Holder<MobEffect> bleedEffect;
    private Holder<MobEffect> smoulderingEffect;
    private Holder<MobEffect> swiftswimmingEffect;
    private Holder<MobEffect> immunizationEffect;
    private Holder<MobEffect> cleansedEffect;

    private static Holder<MobEffect> effect(String name) {
        return ObjectManager.getEffectHolder(name);
    }

    private static boolean has(LivingEntity entity, Holder<MobEffect> effect) {
        return effect != null && entity.hasEffect(effect);
    }

    public void onEntityUpdate(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        this.resolveTickEffects();
        if (entity.getActiveEffects().isEmpty()) {
            this.handleSwiftswimming(entity);
            return;
        }

        this.clearBlindnessForNightVision(entity);
        this.removeDisabledNausea(entity);
        this.handleSwiftswimming(entity);

        if (!this.hasLycanitesTickEffect(entity)) {
            return;
        }

        boolean invulnerable = this.isInvulnerable(entity);

        this.handleParalysis(entity, invulnerable);
        this.handleWeight(entity, invulnerable);
        this.handleFear(entity, invulnerable);
        this.handleInstability(entity, invulnerable);
        this.handlePlague(entity, invulnerable);
        this.handleSmited(entity, invulnerable);
        this.handleBleed(entity, invulnerable);
        this.handleSmouldering(entity, invulnerable);

        this.handleImmunization(entity);
        this.handleCleansed(entity);
    }

    /** Official LivingAttackEvent (lifeleak) + LivingHurtEvent (the rest). **/
    public void onLivingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Entity attacker = event.getSource().getEntity();

        // Lifeleak: the attacker's hits heal the target instead.
        Holder<MobEffect> lifeleak = effect("lifeleak");
        if (lifeleak != null && !target.level().isClientSide && attacker instanceof LivingEntity livingAttacker && livingAttacker.hasEffect(lifeleak)) {
            event.setCanceled(true);
            target.heal(event.getAmount());
            return;
        }

        this.handleFallResistance(event, target);
        if (event.isCanceled()) {
            return;
        }
        this.handlePenetration(event, target);
        this.handleFearWallCollision(event, target);
        if (event.isCanceled()) {
            return;
        }
        this.handleLeech(event);
        this.handleRepulsion(target, attacker);
    }

    public void onEntityHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();

        Holder<MobEffect> rejuvenation = effect("rejuvenation");
        if (rejuvenation != null && entity.hasEffect(rejuvenation)) {
            event.setAmount((float) Math.ceil(event.getAmount() * (2 * (1 + entity.getEffect(rejuvenation).getAmplifier()))));
        }

        Holder<MobEffect> decay = effect("decay");
        if (decay != null && entity.hasEffect(decay)) {
            event.setAmount((float) Math.floor(event.getAmount() / (2 * (1 + entity.getEffect(decay).getAmplifier()))));
        }
    }

    public void onSleep(CanPlayerSleepEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || event.getProblem() != null) {
            return;
        }

        Holder<MobEffect> insomnia = effect("insomnia");
        if (insomnia != null && player.hasEffect(insomnia)) {
            event.setProblem(Player.BedSleepingProblem.NOT_SAFE);
        }
    }

    public void onLivingUseItemStart(LivingEntityUseItemEvent.Start event) {
        if (this.hasAphagia(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    public void onLivingUseItemTick(LivingEntityUseItemEvent.Tick event) {
        if (this.hasAphagia(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private boolean hasAphagia(LivingEntity entity) {
        Holder<MobEffect> aphagia = effect("aphagia");
        return aphagia != null && !entity.level().isClientSide && entity.hasEffect(aphagia);
    }

    private void clearBlindnessForNightVision(LivingEntity entity) {
        if (entity.hasEffect(MobEffects.BLINDNESS) && entity.hasEffect(MobEffects.NIGHT_VISION)) {
            entity.removeEffect(MobEffects.BLINDNESS);
        }
    }

    private void removeDisabledNausea(LivingEntity entity) {
        if (!(entity instanceof Player) || !entity.hasEffect(MobEffects.CONFUSION)) {
            return;
        }
        if (ConfigExtra.INSTANCE.disableNausea.get()) {
            entity.removeEffect(MobEffects.CONFUSION);
        }
    }

    private boolean isInvulnerable(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.isCreative() || player.isSpectator();
        }
        return false;
    }

    private void handleParalysis(LivingEntity entity, boolean invulnerable) {
        if (!invulnerable && has(entity, this.paralysisEffect)) {
            entity.setDeltaMovement(0, entity.getDeltaMovement().y() > 0 ? 0 : entity.getDeltaMovement().y(), 0);
            entity.setOnGround(false);
        }
    }

    /** A feared player is haunted by EntityFear ghosts (one per fear level), which do the fear movement. **/
    private void handleFear(LivingEntity entity, boolean invulnerable) {
        if (!invulnerable && !entity.level().isClientSide && entity instanceof Player player && has(entity, this.fearEffect)) {
            EntityFear.spawnForPlayer(player, null);
        }
    }

    /** Ghosts aren't saved, but ones still following a player who relogged are removed. **/
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        for (EntityFear fearEntity : player.level().getEntitiesOfClass(EntityFear.class, player.getBoundingBox().inflate(128.0D),
                fear -> player.equals(fear.getHauntTarget()))) {
            fearEntity.discard();
        }
    }

    private void handleWeight(LivingEntity entity, boolean invulnerable) {
        if (!invulnerable && has(entity, this.weightEffect) && !entity.hasEffect(MobEffects.DAMAGE_BOOST)) {
            if (entity.getDeltaMovement().y() > -0.2D) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, -0.2D, 0));
            }
        }
    }

    private void handleInstability(LivingEntity entity, boolean invulnerable) {
        if (this.instabilityEffect == null || entity.level().isClientSide || entity instanceof IGroupBoss) {
            return;
        }
        if (invulnerable || !entity.hasEffect(this.instabilityEffect) || entity.level().random.nextDouble() > 0.1D) {
            return;
        }

        double strength = 1 + entity.getEffect(this.instabilityEffect).getAmplifier();
        double motionX = strength * (entity.level().random.nextDouble() - 0.5D);
        double motionY = strength * (entity.level().random.nextDouble() - 0.5D);
        double motionZ = strength * (entity.level().random.nextDouble() - 0.5D);
        entity.setDeltaMovement(entity.getDeltaMovement().add(motionX, motionY, motionZ));
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(entity));
        } else {
            entity.hurtMarked = true;
        }
    }

    private void handlePlague(LivingEntity entity, boolean invulnerable) {
        if (this.plagueEffect == null || entity.level().isClientSide || invulnerable || !entity.hasEffect(this.plagueEffect)) {
            return;
        }

        int poisonAmplifier = entity.getEffect(this.plagueEffect).getAmplifier();
        int poisonDuration = entity.getEffect(this.plagueEffect).getDuration();
        if (entity.hasEffect(MobEffects.POISON)) {
            poisonAmplifier = Math.max(poisonAmplifier, entity.getEffect(MobEffects.POISON).getAmplifier());
            poisonDuration = Math.max(poisonDuration, entity.getEffect(MobEffects.POISON).getDuration());
        }
        entity.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDuration, poisonAmplifier));

        if (entity.level().getGameTime() % 20 != 0) {
            return;
        }

        List<LivingEntity> aoeTargets = entity.level().getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(2));
        for (LivingEntity target : aoeTargets) {
            if (target == entity || entity.isAlliedTo(target)) {
                continue;
            }
            if (target instanceof Player && !entity.hasLineOfSight(target)) {
                continue;
            }

            int amplifier = entity.getEffect(this.plagueEffect).getAmplifier();
            int duration = entity.getEffect(this.plagueEffect).getDuration();
            if (amplifier > 0) {
                target.addEffect(new MobEffectInstance(this.plagueEffect, duration, amplifier - 1));
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
            }
        }
    }

    private void handleSmited(LivingEntity entity, boolean invulnerable) {
        if (!entity.level().isClientSide && !invulnerable && has(entity, this.smitedEffect) && entity.level().getGameTime() % 20 == 0) {
            float brightness = LMHelperClass.getBrightness(entity);
            if (brightness > 0.5F && entity.level().canSeeSkyFromBelowWater(entity.blockPosition())) {
                entity.igniteForSeconds(4);
            }
        }
    }

    private void handleBleed(LivingEntity entity, boolean invulnerable) {
        if (!entity.level().isClientSide && !invulnerable && has(entity, this.bleedEffect) && entity.level().getGameTime() % 20 == 0 && entity.getVehicle() == null) {
            if (entity.walkDistO != entity.walkDist) {
                entity.hurt(entity.level().damageSources().magic(), entity.getEffect(this.bleedEffect).getAmplifier() + 1);
            }
        }
    }

    private void handleSmouldering(LivingEntity entity, boolean invulnerable) {
        if (!entity.level().isClientSide && !invulnerable && has(entity, this.smoulderingEffect) && entity.level().getGameTime() % 20 == 0) {
            entity.igniteForSeconds(4 + (4 * entity.getEffect(this.smoulderingEffect).getAmplifier()));
        }
    }

    private void handleSwiftswimming(LivingEntity entity) {
        if (!(entity instanceof Player) || this.swiftswimmingEffect == null) {
            return;
        }

        AttributeInstance movement = entity.getAttribute(NeoForgeMod.SWIM_SPEED);
        if (movement == null) {
            return;
        }

        boolean hasModifier = false;
        for (ResourceLocation modifierId : SWIFTSWIMMING_MODIFIER_IDS) {
            hasModifier |= movement.getModifier(modifierId) != null;
        }
        if (entity.getActiveEffects().isEmpty() && !hasModifier) {
            return;
        }

        int amplifier = -1;
        if (entity.hasEffect(this.swiftswimmingEffect)) {
            amplifier = entity.getEffect(this.swiftswimmingEffect).getAmplifier();
        }

        for (int i = 0; i < SWIFTSWIMMING_MODIFIER_IDS.length; i++) {
            boolean shouldApply = i == SWIFTSWIMMING_MODIFIER_IDS.length - 1 ? amplifier >= i : amplifier == i;
            ResourceLocation modifierId = SWIFTSWIMMING_MODIFIER_IDS[i];
            if (shouldApply && movement.getModifier(modifierId) == null) {
                movement.addPermanentModifier(new AttributeModifier(modifierId, i + 1, AttributeModifier.Operation.ADD_VALUE));
            } else if (!shouldApply && movement.getModifier(modifierId) != null) {
                movement.removeModifier(modifierId);
            }
        }
    }

    private void handleImmunization(LivingEntity entity) {
        if (entity.level().isClientSide || !has(entity, this.immunizationEffect)) {
            return;
        }
        entity.removeEffect(MobEffects.POISON);
        entity.removeEffect(MobEffects.HUNGER);
        entity.removeEffect(MobEffects.WEAKNESS);
        entity.removeEffect(MobEffects.CONFUSION);
        if (has(entity, this.paralysisEffect)) {
            entity.removeEffect(this.paralysisEffect);
        }
    }

    private void handleCleansed(LivingEntity entity) {
        if (entity.level().isClientSide || !has(entity, this.cleansedEffect)) {
            return;
        }
        entity.removeEffect(MobEffects.WITHER);
        entity.removeEffect(MobEffects.UNLUCK);
        if (has(entity, this.fearEffect)) {
            entity.removeEffect(this.fearEffect);
        }
        Holder<MobEffect> insomnia = effect("insomnia");
        if (has(entity, insomnia)) {
            entity.removeEffect(insomnia);
        }
    }

    private boolean hasLycanitesTickEffect(LivingEntity entity) {
        return has(entity, this.paralysisEffect)
                || has(entity, this.weightEffect)
                || has(entity, this.fearEffect)
                || has(entity, this.instabilityEffect)
                || has(entity, this.plagueEffect)
                || has(entity, this.smitedEffect)
                || has(entity, this.bleedEffect)
                || has(entity, this.smoulderingEffect)
                || has(entity, this.immunizationEffect)
                || has(entity, this.cleansedEffect);
    }

    private void resolveTickEffects() {
        if (this.tickEffectsResolved) {
            return;
        }
        this.paralysisEffect = effect("paralysis");
        this.weightEffect = effect("weight");
        this.fearEffect = effect("fear");
        this.instabilityEffect = effect("instability");
        this.plagueEffect = effect("plague");
        this.smitedEffect = effect("smited");
        this.bleedEffect = effect("bleed");
        this.smoulderingEffect = effect("smouldering");
        this.swiftswimmingEffect = effect("swiftswimming");
        this.immunizationEffect = effect("immunization");
        this.cleansedEffect = effect("cleansed");
        this.tickEffectsResolved = this.paralysisEffect != null;
    }

    private void handleFallResistance(LivingIncomingDamageEvent event, LivingEntity target) {
        Holder<MobEffect> fallresist = effect("fallresist");
        if (fallresist != null && target.hasEffect(fallresist) && event.getSource().is(DamageTypes.FALL)) {
            event.setAmount(0);
            event.setCanceled(true);
        }
    }

    private void handlePenetration(LivingIncomingDamageEvent event, LivingEntity target) {
        Holder<MobEffect> penetration = effect("penetration");
        if (penetration != null && target.hasEffect(penetration)) {
            float damage = event.getAmount();
            float multiplier = 0.25F * (target.getEffect(penetration).getAmplifier() + 1);
            event.setAmount(damage + (damage * multiplier));
        }
    }

    private void handleFearWallCollision(LivingIncomingDamageEvent event, LivingEntity target) {
        if (has(target, this.fearEffect) && event.getSource().is(DamageTypes.IN_WALL)) {
            event.setAmount(0);
            event.setCanceled(true);
        }
    }

    private void handleLeech(LivingIncomingDamageEvent event) {
        Holder<MobEffect> leech = effect("leech");
        if (leech == null || event.getSource().getEntity() == null) {
            return;
        }

        LivingEntity leechingEntity = null;
        if (event.getSource().getDirectEntity() instanceof LivingEntity directLiving) {
            leechingEntity = directLiving;
        } else if (event.getSource().getEntity() instanceof LivingEntity sourceLiving) {
            leechingEntity = sourceLiving;
        }

        if (leechingEntity != null && leechingEntity.hasEffect(leech)) {
            int leeching = leechingEntity.getEffect(leech).getAmplifier() + 1;
            leechingEntity.heal(Math.max(leeching, 1));
        }
    }

    private void handleRepulsion(LivingEntity target, Entity attacker) {
        Holder<MobEffect> repulsion = effect("repulsion");
        if (repulsion == null || attacker == null) {
            return;
        }

        boolean attackerIsBoss = attacker instanceof IGroupBoss;
        if (!attackerIsBoss && CreatureManager.getInstance().getCreatureGroup("boss") != null) {
            attackerIsBoss = CreatureManager.getInstance().getCreatureGroup("boss").hasEntity(attacker);
        }
        if (!attackerIsBoss && target.hasEffect(repulsion)) {
            double knockback = target.getEffect(repulsion).getAmplifier() + 2;
            double xDist = attacker.position().x() - target.position().x();
            double zDist = attacker.position().z() - target.position().z();
            double xzDist = SchismMath.horizontalDistanceAtLeast(xDist, zDist, 0.01D);
            double motionCap = 10;
            double xVel = xDist / xzDist * knockback;
            double zVel = zDist / xzDist * knockback;
            if (attacker.getDeltaMovement().x() < motionCap && attacker.getDeltaMovement().x() > -motionCap && attacker.getDeltaMovement().z() < motionCap && attacker.getDeltaMovement().z() > -motionCap) {
                attacker.push(xVel, 0, zVel);
                attacker.hurtMarked = true;
            }
        }
    }
}
