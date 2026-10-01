package com.feima.movemod.network;

import com.feima.movemod.action.CrawlAction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 → 服务端：请求设置趴下状态。 */
public class CrawlSetPacket {

    private final boolean crawling;

    public CrawlSetPacket(boolean crawling) {
        this.crawling = crawling;
    }

    public static void encode(CrawlSetPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.crawling);
    }

    public static CrawlSetPacket decode(FriendlyByteBuf buf) {
        return new CrawlSetPacket(buf.readBoolean());
    }

    public static void handle(CrawlSetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            boolean cur = CrawlAction.INSTANCE.isCrawling(player);
            if (msg.crawling == cur) return; // 幂等

            if (msg.crawling) {
                if (CrawlAction.INSTANCE.tryStart(player)) {
                    NetworkHandler.broadcastCrawlState(player, true);
                } else {
                    NetworkHandler.sendCrawlReject(player);
                }
            } else {
                CrawlAction.INSTANCE.stop(player);
                NetworkHandler.broadcastCrawlState(player, false);
            }
        });
        context.setPacketHandled(true);
    }
}