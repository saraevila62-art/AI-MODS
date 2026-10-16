package com.example.miyu;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MiyuEntity extends TamableAnimal {
    public MiyuEntity(EntityType<? extends MiyuEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder attrs() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 6, 2));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean isFood(ItemStack stack) { return false; }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) {
            if (!isTame() || isOwnedBy(player)) ClientHooks.open(this);
        } else if (!isTame()) {
            tame(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
}
