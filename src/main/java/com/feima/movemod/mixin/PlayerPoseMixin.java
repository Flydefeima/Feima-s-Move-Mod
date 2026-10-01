package com.feima.movemod.mixin;

import com.feima.movemod.action.CrawlAction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 趴下时把 pose 强制为 {@link net.minecraft.world.entity.Pose#SWIMMING}。
 * 碰撞箱 / 眼高由原版 Player 处理，不引用滑铲 mixin。
 */
@Mixin(Player.class)
public abstract class PlayerPoseMixin {

    @Inject(method = "updatePlayerPose", at = @At("TAIL"))
    private void fmm$forceCrawlPose(CallbackInfo ci) {
        CrawlAction.INSTANCE.forcePose((Player) (Object) this);
    }
}