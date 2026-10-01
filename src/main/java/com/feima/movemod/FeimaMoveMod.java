package com.feima.movemod;

import com.feima.movemod.config.MoveConfig;
import com.feima.movemod.network.NetworkHandler;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(FeimaMoveMod.MODID)
public class FeimaMoveMod {

    public static final String MODID = "feimamovemod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FeimaMoveMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MoveConfig.SPEC);
        NetworkHandler.register();
        LOGGER.info("[Feima Move] 滑铲动作模组已加载");
    }
}