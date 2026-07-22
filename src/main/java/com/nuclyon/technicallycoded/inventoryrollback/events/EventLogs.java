package com.nuclyon.technicallycoded.inventoryrollback.events;

import com.nuclyon.technicallycoded.inventoryrollback.data.BackupStorage;
import com.nuclyon.technicallycoded.inventoryrollback.data.LogType;
import com.nuclyon.technicallycoded.inventoryrollback.data.PlayerDataSnapshot;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraftforge.items.IItemHandlerModifiable;

@Mod.EventBusSubscriber(modid = "inventoryrollbackplus", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventLogs {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            savePlayerInventory(player, LogType.JOIN, null);
        }
    }

    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            savePlayerInventory(player, LogType.QUIT, null);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            savePlayerInventory(player, LogType.WORLD_CHANGE, null);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String deathReason = event.getSource().getMsgId();
            savePlayerInventory(player, LogType.DEATH, deathReason);
        }
    }

    public static void savePlayerInventory(ServerPlayer player, LogType logType, String deathReason) {
        long timestamp = System.currentTimeMillis();

        // Copy main inventory
        NonNullList<ItemStack> mainInv = NonNullList.withSize(36, ItemStack.EMPTY);
        for (int i = 0; i < 36; i++) {
            mainInv.set(i, player.getInventory().items.get(i).copy());
        }

        // Copy armor
        NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
        for (int i = 0; i < 4; i++) {
            armor.set(i, player.getInventory().armor.get(i).copy());
        }

        // Copy offhand
        NonNullList<ItemStack> offhand = NonNullList.withSize(1, ItemStack.EMPTY);
        for (int i = 0; i < 1; i++) {
            offhand.set(i, player.getInventory().offhand.get(i).copy());
        }

        // Copy ender chest
        NonNullList<ItemStack> enderChest = NonNullList.withSize(27, ItemStack.EMPTY);
        for (int i = 0; i < 27; i++) {
            enderChest.set(i, player.getEnderChestInventory().getItem(i).copy());
        }

        // Copy Curios if loaded
        NonNullList<ItemStack> curiosList = NonNullList.create();
        if (ModList.get().isLoaded("curios")) {
            CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
                IItemHandlerModifiable equipped = handler.getEquippedCurios();
                for (int i = 0; i < equipped.getSlots(); i++) {
                    curiosList.add(equipped.getStackInSlot(i).copy());
                }
            });
        }

        String dimension = player.level().dimension().location().toString();

        PlayerDataSnapshot snapshot = new PlayerDataSnapshot(
                player.experienceProgress + player.experienceLevel, // Simplified xp
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                dimension,
                player.getX(),
                player.getY(),
                player.getZ(),
                mainInv,
                armor,
                offhand,
                enderChest,
                curiosList,
                logType,
                timestamp,
                deathReason
        );

        BackupStorage.saveBackup(player.getUUID(), snapshot);
    }
}
