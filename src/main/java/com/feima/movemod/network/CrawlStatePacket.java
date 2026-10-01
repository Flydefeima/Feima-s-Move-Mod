package com.feima.movemod.network;

import com.feima.movemod.action.CrawlAction;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 服务端 → 客户端：同步某个玩家的趴下状态。 */
public class CrawlStatePacket {

    private final UUID playerId;
    private final boolean crawling;

    public CrawlStatePacket(UUID playerId, boolean crawling) {
        this.playerId = playerId;
        this.crawling = crawling;
    }

    public static void encode(CrawlStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.crawling);
    }

    public static CrawlStatePacket decode(FriendlyByteBuf buf) {
        return new CrawlStatePacket(buf.readUUID(), buf.readBoolean());
    }

    public static void handle(CrawlStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> handleClient(msg))
        );
        context.setPacketHandled(true);
    }

    private static void handleClient(CrawlStatePacket msg) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        Player player = level.getPlayerByUUID(msg.playerId);
        if (player == null) return;
        CrawlAction.INSTANCE.applyRemoteState(player, msg.crawling);
    }
}