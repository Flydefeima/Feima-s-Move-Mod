package com.feima.movemod.event;

import com.feima.movemod.FeimaMoveMod;
import com.feima.movemod.action.SlideAction;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 监听跳跃事件，用于触发滑铲跳。
 *
 * 时序说明：
 *   1. Player.tick() 内检查玩家跳跃 → 调 jumpFromGround() → 应用原版跳跃速度
 *   2. jumpFromGround() 末尾触发 LivingJumpEvent
 *   3. 这里 → trySlideJump()：取消滑铲、覆盖速度为滑铲跳速度、服务端广播
 *   4. Player.tick() 结束 → PlayerSlideMixin 的 TAIL 注入调 SlideAction.tick()
 *      此时滑铲状态已被移除，直接 return，不会再覆盖速度
 */
@Mod.EventBusSubscriber(modid = FeimaMoveMod.MODID)
public final class SlideJumpHandler {

    private SlideJumpHandler() {}

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        SlideAction.INSTANCE.trySlideJump(player);
    }
}