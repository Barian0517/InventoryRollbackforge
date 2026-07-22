package com.nuclyon.technicallycoded.inventoryrollback.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ReadOnlyChestMenu extends ChestMenu {
    private final Runnable onClose;
    private final boolean allowInteraction;
    private boolean skipOnClose = false;

    public ReadOnlyChestMenu(MenuType<?> type, int containerId, Inventory playerInventory, Container container, int rows, Runnable onClose, boolean allowInteraction) {
        super(type, containerId, playerInventory, container, rows);
        this.onClose = onClose;
        this.allowInteraction = allowInteraction;
    }

    public ReadOnlyChestMenu(MenuType<?> type, int containerId, Inventory playerInventory, Container container, int rows, Runnable onClose) {
        this(type, containerId, playerInventory, container, rows, onClose, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!allowInteraction) {
            return ItemStack.EMPTY; // No shift-clicking in list menu
        }
        
        // Prevent shift-clicking items from player inventory (>= 54) into the backup GUI
        if (index >= 54) {
            return ItemStack.EMPTY;
        }
        
        return super.quickMoveStack(player, index);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Always allow interacting with own inventory or dropping outside
        if (slotId >= 54 || slotId == -999) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        if (!allowInteraction) {
            handleCustomClick(slotId, button, clickType, player);
            return;
        }
        
        // Allow grabbing items from the backup inventory (slots 0-44)
        // Prevent clicking in the utility bar (slots 45-53) and player's own inventory (slots >= 54)
        if (slotId >= 0 && slotId < 45) {
            super.clicked(slotId, button, clickType, player);
        } else if (slotId >= 45 && slotId < 54) {
            handleCustomClick(slotId, button, clickType, player);
        }
    }

    protected void handleCustomClick(int slotId, int button, ClickType clickType, Player player) {
        // Subclasses can override this
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }


    public void setSkipOnClose(boolean skipOnClose) {
        this.skipOnClose = skipOnClose;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!skipOnClose && this.onClose != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.getServer().execute(this.onClose);
            }
        }
    }
}
