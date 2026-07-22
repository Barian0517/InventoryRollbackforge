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

    public enum ViewType { MAIN, ENDER_CHEST, CURIOS }

    private final PlayerDataSnapshot snapshot;
    private final UUID targetUUID;
    private final String targetName;
    private final ViewType viewType;
    private final int curiosPage;
    private final Runnable onBack;

    public BackupViewMenuProvider(PlayerDataSnapshot snapshot, UUID targetUUID, String targetName, ViewType viewType, int curiosPage, Runnable onBack) {
        this.snapshot = snapshot;
        this.targetUUID = targetUUID;
        this.targetName = targetName;
        this.viewType = viewType;
        this.curiosPage = curiosPage;
        this.onBack = onBack;
    }

    @Override
    public Component getDisplayName() {
        if (viewType == ViewType.ENDER_CHEST) return Component.literal("Ender Chest: " + targetName);
        if (viewType == ViewType.CURIOS) return Component.literal("Curios (P" + (curiosPage + 1) + "): " + targetName);
        return Component.literal("Backup: " + targetName);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        SimpleContainer container = new SimpleContainer(54); // 6 rows
        
        if (viewType == ViewType.ENDER_CHEST) {
            for (int i = 0; i < snapshot.enderChest.size() && i < 27; i++) {
                if (!snapshot.enderChest.get(i).isEmpty()) {
                    container.setItem(i, snapshot.enderChest.get(i).copy());
                }
            }
        } else if (viewType == ViewType.CURIOS) {
            int startIdx = curiosPage * 45;
            for (int i = 0; i < 45; i++) {
                int listIdx = startIdx + i;
                if (listIdx < snapshot.curios.size()) {
                    if (!snapshot.curios.get(listIdx).isEmpty()) {
                        container.setItem(i, snapshot.curios.get(listIdx).copy());
                    }
                }
            }
        } else {
            // Populate container with backup items
            for (int i = 0; i < snapshot.mainInventory.size() && i < 36; i++) {
                if (!snapshot.mainInventory.get(i).isEmpty()) {
                    container.setItem(i, snapshot.mainInventory.get(i).copy());
                }
            }
            // Armor (41-44 typically in Bukkit UI, let's place them backward from 44)
            int armorSlot = 44;
            for (int i = 0; i < snapshot.armor.size() && i < 4; i++) {
                if (!snapshot.armor.get(i).isEmpty()) {
                    container.setItem(armorSlot, snapshot.armor.get(i).copy());
                }
                armorSlot--;
            }
            // Offhand
            if (snapshot.offhand.size() > 0) {
                container.setItem(40, snapshot.offhand.get(0).copy());
            }
        }
        
        // Slot 45: Back
        container.setItem(45, createButton(net.minecraft.world.item.Items.WHITE_BANNER, "§fBack"));
        
        // Slot 46: Curios
        if (viewType == ViewType.CURIOS) {
            container.setItem(46, createButton(net.minecraft.world.item.Items.CHEST, "§6Main Inventory Backup"));
        } else {
            container.setItem(46, createButton(net.minecraft.world.item.Items.GOLD_NUGGET, "§6Curios Backup"));
        }

        // Slot 47: Chest Bundle
        container.setItem(47, createButton(net.minecraft.world.item.Items.CHEST, "§aChest Bundle"));
        // Slot 48: Restore All
        container.setItem(48, createButton(net.minecraft.world.item.Items.NETHER_STAR, "§6Restore All Items"));
        // Slot 49: Teleport
        container.setItem(49, createButton(net.minecraft.world.item.Items.ENDER_PEARL, "§dTeleport to Location"));
        
        // Slot 50: Ender Chest
        if (viewType == ViewType.ENDER_CHEST) {
            container.setItem(50, createButton(net.minecraft.world.item.Items.CHEST, "§6Main Inventory Backup"));
        } else {
            container.setItem(50, createButton(net.minecraft.world.item.Items.ENDER_CHEST, "§5Ender Chest Backup"));
        }

        if (viewType == ViewType.CURIOS) {
            if (curiosPage > 0) {
                container.setItem(52, createButton(net.minecraft.world.item.Items.ARROW, "§aPrevious Page"));
            }
            if ((curiosPage + 1) * 45 < snapshot.curios.size()) {
                container.setItem(53, createButton(net.minecraft.world.item.Items.ARROW, "§aNext Page"));
            }
        } else {
            // Slot 51: Health
            container.setItem(51, createButton(net.minecraft.world.item.Items.GLISTERING_MELON_SLICE, "§cRestore Health"));
            // Slot 52: Hunger
            container.setItem(52, createButton(net.minecraft.world.item.Items.ROTTEN_FLESH, "§eRestore Hunger"));
            // Slot 53: Experience
            container.setItem(53, createButton(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, "§aRestore XP"));
        }

        return new ReadOnlyChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6, onBack, true) {
            @Override
            protected void handleCustomClick(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
                if (!(player instanceof ServerPlayer serverPlayer)) return;

                ServerPlayer targetPlayer = serverPlayer.getServer().getPlayerList().getPlayer(targetUUID);

                switch (slotId) {
                    case 45: // Back
                        serverPlayer.closeContainer(); // triggers onBack
                        break;
                    case 46: // Curios Switch
                        if (player.containerMenu instanceof ReadOnlyChestMenu menu) menu.setSkipOnClose(true);
                        if (viewType == ViewType.CURIOS) {
                            serverPlayer.openMenu(new BackupViewMenuProvider(snapshot, targetUUID, targetName, ViewType.MAIN, 0, onBack));
                        } else {
                            serverPlayer.openMenu(new BackupViewMenuProvider(snapshot, targetUUID, targetName, ViewType.CURIOS, 0, onBack));
                        }
                        break;
                    case 47: // Chest Bundle
                        giveChestBundle(serverPlayer);
                        break;
                    case 48: // Restore All Items
                        if (targetPlayer != null) {
                            if (viewType == ViewType.ENDER_CHEST) {
                                for (int i = 0; i < 27; i++) {
                                    targetPlayer.getEnderChestInventory().setItem(i, i < snapshot.enderChest.size() ? snapshot.enderChest.get(i).copy() : ItemStack.EMPTY);
                                }
                                serverPlayer.sendSystemMessage(Component.literal("§aRestored Ender Chest for " + targetName));
                            } else if (viewType == ViewType.CURIOS) {
                                if (net.minecraftforge.fml.ModList.get().isLoaded("curios")) {
                                    top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(targetPlayer).ifPresent(handler -> {
                                        net.minecraftforge.items.IItemHandlerModifiable equipped = handler.getEquippedCurios();
                                        for (int i = 0; i < equipped.getSlots() && i < snapshot.curios.size(); i++) {
                                            equipped.setStackInSlot(i, snapshot.curios.get(i).copy());
                                        }
                                    });
                                    serverPlayer.sendSystemMessage(Component.literal("§aRestored Curios for " + targetName));
                                } else {
                                    serverPlayer.sendSystemMessage(Component.literal("§cCurios is not loaded on the server."));
                                }
                            } else {
                                for (int i = 0; i < 36; i++) targetPlayer.getInventory().items.set(i, i < snapshot.mainInventory.size() ? snapshot.mainInventory.get(i).copy() : ItemStack.EMPTY);
                                for (int i = 0; i < 4; i++) targetPlayer.getInventory().armor.set(i, i < snapshot.armor.size() ? snapshot.armor.get(i).copy() : ItemStack.EMPTY);
                                for (int i = 0; i < 1; i++) targetPlayer.getInventory().offhand.set(i, i < snapshot.offhand.size() ? snapshot.offhand.get(i).copy() : ItemStack.EMPTY);
                                serverPlayer.sendSystemMessage(Component.literal("§aRestored items for " + targetName));
                            }
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
                    case 50: // Ender Chest Switch
                        if (player.containerMenu instanceof ReadOnlyChestMenu menu) menu.setSkipOnClose(true);
                        if (viewType == ViewType.ENDER_CHEST) {
                            serverPlayer.openMenu(new BackupViewMenuProvider(snapshot, targetUUID, targetName, ViewType.MAIN, 0, onBack));
                        } else {
                            serverPlayer.openMenu(new BackupViewMenuProvider(snapshot, targetUUID, targetName, ViewType.ENDER_CHEST, 0, onBack));
                        }
                        break;
                    case 51: // Health
                        if (viewType != ViewType.CURIOS) {
                            if (targetPlayer != null) {
                                targetPlayer.setHealth(snapshot.health);
                                serverPlayer.sendSystemMessage(Component.literal("§aRestored health for " + targetName));
                            } else serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        }
                        break;
                    case 52: // Hunger or Previous Page
                        if (viewType == ViewType.CURIOS) {
                            if (curiosPage > 0) {
                                if (player.containerMenu instanceof ReadOnlyChestMenu menu) menu.setSkipOnClose(true);
                                serverPlayer.openMenu(new BackupViewMenuProvider(snapshot, targetUUID, targetName, ViewType.CURIOS, curiosPage - 1, onBack));
                            }
                        } else {
                            if (targetPlayer != null) {
                                targetPlayer.getFoodData().setFoodLevel(snapshot.foodLevel);
                                targetPlayer.getFoodData().setSaturation(snapshot.saturation);
                                serverPlayer.sendSystemMessage(Component.literal("§aRestored hunger for " + targetName));
                            } else serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        }
                        break;
                    case 53: // Experience or Next Page
                        if (viewType == ViewType.CURIOS) {
                            if ((curiosPage + 1) * 45 < snapshot.curios.size()) {
                                if (player.containerMenu instanceof ReadOnlyChestMenu menu) menu.setSkipOnClose(true);
                                serverPlayer.openMenu(new BackupViewMenuProvider(snapshot, targetUUID, targetName, ViewType.CURIOS, curiosPage + 1, onBack));
                            }
                        } else {
                            if (targetPlayer != null) {
                                int level = (int) snapshot.xp;
                                float progress = snapshot.xp - level;
                                targetPlayer.experienceLevel = level;
                                targetPlayer.experienceProgress = progress;
                                targetPlayer.totalExperience = 0; // Optional reset
                                serverPlayer.sendSystemMessage(Component.literal("§aRestored XP for " + targetName));
                            } else serverPlayer.sendSystemMessage(Component.literal("§cPlayer is offline."));
                        }
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
        
        chest.enchant(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 1);
        chest.getOrCreateTag().putInt("HideFlags", 1);

        net.minecraft.nbt.CompoundTag display = chest.getOrCreateTagElement("display");
        net.minecraft.nbt.ListTag lore = new net.minecraft.nbt.ListTag();
        lore.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.literal("§7Contains backed up items for " + targetName).withStyle(net.minecraft.network.chat.Style.EMPTY.withItalic(false)))));
        lore.add(net.minecraft.nbt.StringTag.valueOf(net.minecraft.network.chat.Component.Serializer.toJson(Component.literal("§7Place this chest down to access them.").withStyle(net.minecraft.network.chat.Style.EMPTY.withItalic(false)))));
        display.put("Lore", lore);

        net.minecraft.nbt.CompoundTag blockEntityTag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.ListTag itemsList = new net.minecraft.nbt.ListTag();
        
        int slot = 0;
        
        java.util.List<ItemStack> itemsToBundle = viewType == ViewType.ENDER_CHEST ? snapshot.enderChest : 
                                                  (viewType == ViewType.CURIOS ? snapshot.curios : snapshot.mainInventory);
        
        for (ItemStack item : itemsToBundle) {
            if (!item.isEmpty()) {
                net.minecraft.nbt.CompoundTag itemTag = new net.minecraft.nbt.CompoundTag();
                itemTag.putByte("Slot", (byte) slot);
                item.save(itemTag);
                itemsList.add(itemTag);
            }
            slot++;
            if (slot >= 27) break;
        }
        blockEntityTag.put("Items", itemsList);
        blockEntityTag.putString("id", "minecraft:chest");
        chest.getOrCreateTag().put("BlockEntityTag", blockEntityTag);
        
        admin.getInventory().add(chest);
        admin.sendSystemMessage(Component.literal("§aGiven backup chest bundle."));
    }
}
