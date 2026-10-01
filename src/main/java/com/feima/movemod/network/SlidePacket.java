package com.feima.movemod.network;

import com.feima.movemod.action.SlideAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：请求触发滑铲 */
public class SlidePacket {

    public SlidePacket() {}

    public static void encode(SlidePacket msg, FriendlyByteBuf buf) {
        // 无数据
    }

    public static SlidePacket decode(FriendlyByteBuf buf) {
        return new SlidePacket();
    }

    public static void handle(SlidePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 已经在滑铲：不重复启动，也不发拒绝
            if (SlideAction.INSTANCE.isSliding(player)) return;

            if (SlideAction.INSTANCE.tryStart(player)) {
                // 权威启动成功 → 广播给所有追踪者
                NetworkHandler.broadcastSlideState(player, true);
            } else {
                // 拒绝（不在地面 / 视线为 0 / 被禁用等）→ 通知客户端回滚预测
                NetworkHandler.sendSlideReject(player);
            }
        });
        context.setPacketHandled(true);
    }
}