package com.feima.movemod.client;

import com.feima.movemod.FeimaMoveMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 给每个玩家挂一个动画层，供后续播放/停止动画。
 * 对应示例里的 PlayerAnimatorExample。
 */
@Mod.EventBusSubscriber(
        modid = FeimaMoveMod.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PlayerAnimationSetup {

    /** 本模组的动画层 ID，之后要用它取层 */
    public static final ResourceLocation LAYER_ID =
            new ResourceLocation(FeimaMoveMod.MODID, "animation");

    private PlayerAnimationSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                LAYER_ID,
                42,
                PlayerAnimationSetup::createLayer
        );
    }

    private static IAnimation createLayer(AbstractClientPlayer player) {
        return new ModifierLayer<>();
    }
}