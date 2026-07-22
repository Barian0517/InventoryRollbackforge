package com.nuclyon.technicallycoded.inventoryrollback;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(InventoryRollbackPlus.MODID)
public class InventoryRollbackPlus {
    public static final String MODID = "inventoryrollbackplus";
    public static final Logger LOGGER = LogManager.getLogger();

    public InventoryRollbackPlus() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("InventoryRollbackPlus setup");
    }
}
