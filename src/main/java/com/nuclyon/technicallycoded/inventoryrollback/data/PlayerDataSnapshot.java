package com.nuclyon.technicallycoded.inventoryrollback.data;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

public class PlayerDataSnapshot {
    public final float xp;
    public final float health;
    public final int foodLevel;
    public final float saturation;
    public final String dimension;
    public final double x, y, z;
    public final NonNullList<ItemStack> mainInventory;
    public final NonNullList<ItemStack> armor;
    public final NonNullList<ItemStack> offhand;
    public final NonNullList<ItemStack> enderChest;
    
    public final LogType logType;
    public final long timestamp;
    public final String deathReason;

    public PlayerDataSnapshot(float xp, float health, int foodLevel, float saturation, String dimension, 
                              double x, double y, double z, NonNullList<ItemStack> mainInventory, 
                              NonNullList<ItemStack> armor, NonNullList<ItemStack> offhand, NonNullList<ItemStack> enderChest,
                              LogType logType, long timestamp, String deathReason) {
        this.xp = xp;
        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.mainInventory = mainInventory;
        this.armor = armor;
        this.offhand = offhand;
        this.enderChest = enderChest;
        this.logType = logType;
        this.timestamp = timestamp;
        this.deathReason = deathReason;
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("xp", xp);
        tag.putFloat("health", health);
        tag.putInt("foodLevel", foodLevel);
        tag.putFloat("saturation", saturation);
        tag.putString("dimension", dimension != null ? dimension : "minecraft:overworld");
        tag.putDouble("x", x);
        tag.putDouble("y", y);
        tag.putDouble("z", z);
        tag.putString("logType", logType.name());
        tag.putLong("timestamp", timestamp);
        if (deathReason != null) {
            tag.putString("deathReason", deathReason);
        }

        tag.put("mainInventory", saveItemList(mainInventory));
        tag.put("armor", saveItemList(armor));
        tag.put("offhand", saveItemList(offhand));
        tag.put("enderChest", saveItemList(enderChest));

        return tag;
    }

    public static PlayerDataSnapshot fromNBT(CompoundTag tag) {
        return new PlayerDataSnapshot(
                tag.getFloat("xp"),
                tag.getFloat("health"),
                tag.getInt("foodLevel"),
                tag.getFloat("saturation"),
                tag.getString("dimension"),
                tag.getDouble("x"),
                tag.getDouble("y"),
                tag.getDouble("z"),
                loadItemList(tag.getList("mainInventory", 10), 36),
                loadItemList(tag.getList("armor", 10), 4),
                loadItemList(tag.getList("offhand", 10), 1),
                loadItemList(tag.getList("enderChest", 10), 27),
                LogType.valueOf(tag.getString("logType")),
                tag.getLong("timestamp"),
                tag.contains("deathReason") ? tag.getString("deathReason") : null
        );
    }

    private static ListTag saveItemList(NonNullList<ItemStack> list) {
        ListTag tagList = new ListTag();
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = list.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                stack.save(itemTag);
                tagList.add(itemTag);
            }
        }
        return tagList;
    }

    private static NonNullList<ItemStack> loadItemList(ListTag tagList, int size) {
        NonNullList<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
        for (int i = 0; i < tagList.size(); i++) {
            CompoundTag itemTag = tagList.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < size) {
                list.set(slot, ItemStack.of(itemTag));
            }
        }
        return list;
    }
}
