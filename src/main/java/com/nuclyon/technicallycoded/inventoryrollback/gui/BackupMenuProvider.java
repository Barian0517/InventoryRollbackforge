package com.nuclyon.technicallycoded.inventoryrollback.gui;

import com.nuclyon.technicallycoded.inventoryrollback.data.BackupStorage;
import com.nuclyon.technicallycoded.inventoryrollback.data.LogType;
import com.nuclyon.technicallycoded.inventoryrollback.data.PlayerDataSnapshot;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class BackupMenuProvider implements MenuProvider {

    private final UUID targetUUID;
    private final String targetName;

    public BackupMenuProvider(UUID targetUUID, String targetName) {
        this.targetUUID = targetUUID;
        this.targetName = targetName;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Backups: " + targetName);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        SimpleContainer container = new SimpleContainer(54); // 6 rows
        
        List<PlayerDataSnapshot> backups = BackupStorage.getBackups(targetUUID, null);
        
        // Populate container with backup icons
        int slot = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (PlayerDataSnapshot backup : backups) {
            if (slot >= 54) break;
            
            ItemStack icon = new ItemStack(Items.PAPER);
            if (backup.logType == LogType.DEATH) icon = new ItemStack(Items.SKELETON_SKULL);
            else if (backup.logType == LogType.JOIN) icon = new ItemStack(Items.SLIME_BALL);
            else if (backup.logType == LogType.QUIT) icon = new ItemStack(Items.REDSTONE);
            else if (backup.logType == LogType.WORLD_CHANGE) icon = new ItemStack(Items.ENDER_PEARL);
            
            icon.setHoverName(Component.literal("§e" + backup.logType.name()));
            
            CompoundTag tag = icon.getOrCreateTag();
            CompoundTag display = new CompoundTag();
            ListTag lore = new ListTag();
            lore.add(StringTag.valueOf("[{\"text\":\"§7Time: §f" + sdf.format(new Date(backup.timestamp)) + "\"}]"));
            if (backup.deathReason != null) {
                lore.add(StringTag.valueOf("[{\"text\":\"§7Reason: §c" + backup.deathReason + "\"}]"));
            }
            display.put("Lore", lore);
            tag.put("display", display);
            icon.setTag(tag);
            
            container.setItem(slot, icon);
            slot++;
        }
        
        return new ReadOnlyChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6, null) {
            @Override
            public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
                super.clicked(slotId, button, clickType, player);
                com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus.LOGGER.info("Clicked slot " + slotId + " in BackupMenuProvider");
                if (slotId >= 0 && slotId < backups.size()) {
                    PlayerDataSnapshot selectedBackup = backups.get(slotId);
                    if (player instanceof ServerPlayer serverPlayer) {
                        com.nuclyon.technicallycoded.inventoryrollback.InventoryRollbackPlus.LOGGER.info("Opening BackupViewMenuProvider for backup at " + selectedBackup.timestamp);
                        serverPlayer.openMenu(new BackupViewMenuProvider(selectedBackup, targetUUID, targetName, () -> {
                            openMenu(serverPlayer, targetUUID, targetName);
                        }));
                    }
                }
            }
        };
    }

    public static void openMenu(ServerPlayer execPlayer, UUID targetUUID, String targetName) {
        execPlayer.openMenu(new BackupMenuProvider(targetUUID, targetName));
    }

    public static void openMenu(ServerPlayer execPlayer, ServerPlayer targetPlayer) {
        openMenu(execPlayer, targetPlayer.getUUID(), targetPlayer.getName().getString());
    }
}
