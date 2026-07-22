package com.nuclyon.technicallycoded.inventoryrollback.gui;

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
import java.util.UUID;

public class BackupViewMenuProvider implements MenuProvider {

    private final PlayerDataSnapshot snapshot;
    private final UUID targetUUID;
    private final String targetName;
    private final Runnable onBack;

    public BackupViewMenuProvider(PlayerDataSnapshot snapshot, UUID targetUUID, String targetName, Runnable onBack) {
        this.snapshot = snapshot;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.onBack = onBack;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("View: " + targetName);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        SimpleContainer container = new SimpleContainer(54); // 6 rows
        
        // Slots 0-35: Main Inventory
        for (int i = 0; i < 36; i++) {
            if (i < snapshot.mainInventory.size()) {
                container.setItem(i, snapshot.mainInventory.get(i).copy());
            }
        }
        
        // Slots 36-39: Armor
        for (int i = 0; i < 4; i++) {
            if (i < snapshot.armor.size()) {
                container.setItem(36 + i, snapshot.armor.get(i).copy());
            }
        }
        
        // Slot 40: Offhand
        if (snapshot.offhand.size() > 0) {
            container.setItem(40, snapshot.offhand.get(0).copy());
        }
        // Slot 45: Back
        container.setItem(45, createButton(net.minecraft.world.item.Items.WHITE_BANNER, "§fBack"));
        // Slot 47: Chest Bundle
        container.setItem(47, createButton(net.minecraft.world.item.Items.CHEST, "§aChest Bundle"));
        // Slot 48: Restore All
        container.setItem(48, createButton(net.minecraft.world.item.Items.NETHER_STAR, "§6Restore All Items"));
        // Slot 49: Teleport
        container.setItem(49, createButton(net.minecraft.world.item.Items.ENDER_PEARL, "§dTeleport to Location"));
        // Slot 50: Ender Chest
        container.setItem(50, createButton(net.minecraft.world.item.Items.ENDER_CHEST, "§5Ender Chest Backup"));
        // Slot 51: Health
        container.setItem(51, createButton(net.minecraft.world.item.Items.GLISTERING_MELON_SLICE, "§cRestore Health"));
        // Slot 52: Hunger
        container.setItem(52, createButton(net.minecraft.world.item.Items.ROTTEN_FLESH, "§eRestore Hunger"));
        // Slot 53: Experience
        container.setItem(53, createButton(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, "§aRestore XP"));

        return new ReadOnlyChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6, onBack, true) {
            @Override
            protected void handleCustomClick(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
                if (!(player instanceof ServerPlayer serverPlayer)) return;

                ServerPlayer targetPlayer = serverPlayer.getServer().getPlayerList().getPlayer(targetUUID);

                switch (slotId) {
                    case 45: // Back
                        serverPlayer.closeContainer(); // triggers onBack
                        break;
                    case 47: // Chest Bundle
                        giveChestBundle(serverPlayer);
                        break;
                    case 48: // Restore All Items
                        if (targetPlayer != null) {
                            for (int i = 0; i < 36; i++) targetPlayer.getInventory().items.set(i, i < snapshot.mainInventory.size() ? snapshot.mainInventory.get(i).copy() : ItemStack.EMPTY);
                            for (int i = 0; i < 4; i++) targetPlayer.getInventory().armor.set(i, i < snapshot.armor.size() ? snapshot.armor.get(i).copy() : ItemStack.EMPTY);
                            for (int i = 0; i < 1; i++) targetPlayer.getInventory().offhand.set(i, i < snapshot.offhand.size() ? snapshot.offhand.get(i).copy() : ItemStack.EMPTY);
                            serverPlayer.sendSystemMessage(Component.literal("§aRestored items for " + targetName));
                        } else {
                            serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        }
                        break;
                    case 49: // Teleport
                        if (snapshot.dimension != null) {
                            net.minecraft.server.level.ServerLevel level = null;
                            for (net.minecraft.server.level.ServerLevel l : serverPlayer.getServer().getAllLevels()) {
                                if (l.dimension().location().toString().equals(snapshot.dimension)) { level = l; break; }
                            }
                            if (level != null) {
                                serverPlayer.teleportTo(level, snapshot.x, snapshot.y, snapshot.z, serverPlayer.getYRot(), serverPlayer.getXRot());
                                serverPlayer.sendSystemMessage(Component.literal("§dTeleported to backup location."));
                            } else {
                                serverPlayer.sendSystemMessage(Component.literal("§cUnknown dimension."));
                            }
                        }
                        break;
                    case 50: // Ender Chest (WIP)
                        serverPlayer.sendSystemMessage(Component.literal("§eEnder Chest backup feature coming soon."));
                        break;
                    case 51: // Health
                        if (targetPlayer != null) {
                            targetPlayer.setHealth(snapshot.health);
                            serverPlayer.sendSystemMessage(Component.literal("§aRestored health for " + targetName));
                        } else serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        break;
                    case 52: // Hunger
                        if (targetPlayer != null) {
                            targetPlayer.getFoodData().setFoodLevel(snapshot.foodLevel);
                            targetPlayer.getFoodData().setSaturation(snapshot.saturation);
                            serverPlayer.sendSystemMessage(Component.literal("§aRestored hunger for " + targetName));
                        } else serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        break;
                    case 53: // Experience
                        if (targetPlayer != null) {
                            // Reset xp before adding
                            targetPlayer.experienceProgress = 0.0f;
                            targetPlayer.experienceLevel = 0;
                            targetPlayer.totalExperience = 0;
                            targetPlayer.giveExperiencePoints((int)snapshot.xp); // Rough approximation of total xp
                            serverPlayer.sendSystemMessage(Component.literal("§aRestored XP for " + targetName));
                        } else serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        break;
                }
            }
        };
    }

    private ItemStack createButton(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    private void giveChestBundle(ServerPlayer admin) {
        ItemStack chest = new ItemStack(net.minecraft.world.item.Items.CHEST);
        chest.setHoverName(Component.literal("§e§l⭐ Backup Bundle: " + targetName + " ⭐").withStyle(net.minecraft.network.chat.Style.EMPTY.withItalic(false)));
        
        // Add enchantment glint by adding an empty enchantment and hiding it
        chest.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
        chest.getOrCreateTag().putInt("HideFlags", 1); // Hide enchantments

        // Add lore
        net.minecraft.nbt.CompoundTag display = chest.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.literal("§7Contains backed up items for " + targetName).withStyle(net.minecraft.network.chat.Style.EMPTY.withItalic(false)))));
        lore.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.literal("§7Place this chest down to access them.").withStyle(net.minecraft.network.chat.Style.EMPTY.withItalic(false)))));
        display.put("Lore", lore);

        net.minecraft.nbt.CompoundTag blockEntityTag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag itemsList = new net.minecraft.nbt.ListTag();
        
        int slot = 0;
        for (ItemStack item : snapshot.mainInventory) {
            if (!item.isEmpty()) {
                net.minecraft.nbt.CompoundTag itemTag = new net.minecraft.nbt.CompoundTag();
                itemTag.putByte("Slot", (byte) slot);
                item.save(itemTag);
                itemsList.add(itemTag);
            }
            slot++;
            if (slot >= 27) break; // Chest only has 27 slots
        }
        blockEntityTag.put("Items", itemsList);
        blockEntityTag.putString("id", "minecraft:chest");
        chest.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
        
        admin.getInventory().add(chest);
        admin.sendSystemMessage(Component.literal("§aGiven backup chest bundle."));
    }
}
