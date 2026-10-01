package com.feima.movemod.network;

import com.feima.movemod.action.SlideAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：请求滑铲跳。
 *
 * 为什么需要：
 *   客户端 LivingJumpEvent 在部分 Forge 版本不触发，客户端无法及时本地结束滑铲。
 *   客户端通过 keyJump 边沿检测 → 立即本地 trySlideJump() + 发送此包；
 *   服务端收到后立即 trySlideJump()（幂等，重复调用安全）。
 */
public class SlideJumpPacket {

    public SlideJumpPacket() {}

    public static void encode(SlideJumpPacket msg, FriendlyByteBuf buf) {
        // no payload
    }

    public static SlideJumpPacket decode(FriendlyByteBuf buf) {
        return new SlideJumpPacket();
    }

    public static void handle(SlideJumpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            // 幂等：不在滑铲中会返回 false，不会重复触发
            SlideAction.INSTANCE.trySlideJump(player);
        });
        context.setPacketHandled(true);
    }
}