package com.feima.movemod.mixin;

import com.feima.movemod.action.SlideAction;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 两端都 tick：服务端负责权威移动，客户端负责本地预测，
 * 否则客户端会把本地玩家拉回原地。
 */
@Mixin(Player.class)
public abstract class PlayerSlideMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void fmm$tickSlide(CallbackInfo ci) {
        SlideAction.INSTANCE.tick((Player) (Object) this);
    }
}