package com.nuclyon.technicallycoded.inventoryrollback.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BackupStorage {

    private static Path getBackupDirectory(UUID playerUUID) {
        // Save to world/inventoryrollbackplus/backups/<UUID>
        File worldDir = ServerLifecycleHooks.getCurrentServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
        Path backupDir = worldDir.toPath().resolve("inventoryrollbackplus").resolve("backups").resolve(playerUUID.toString());
        if (!Files.exists(backupDir)) {
            try {
                Files.createDirectories(backupDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return backupDir;
    }

    public static void saveBackup(UUID playerUUID, PlayerDataSnapshot snapshot) {
        Path backupDir = getBackupDirectory(playerUUID);
        String filename = snapshot.timestamp + "_" + snapshot.logType.name() + ".dat";
        Path backupFile = backupDir.resolve(filename);

        try {
            NbtIo.writeCompressed(snapshot.toNBT(), backupFile.toFile());
            purgeExcessSaves(playerUUID, snapshot.logType, 50); // Hardcoded limit for now, should be from config
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<PlayerDataSnapshot> getBackups(UUID playerUUID, LogType type) {
        Path backupDir = getBackupDirectory(playerUUID);
        List<PlayerDataSnapshot> backups = new ArrayList<>();

        if (!Files.exists(backupDir)) {
            return backups;
        }

        try (Stream<Path> paths = Files.walk(backupDir)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".dat"))
                 .forEach(path -> {
                     String filename = path.getFileName().toString();
                     if (type == null || filename.contains("_" + type.name() + ".dat")) {
                         try {
                             CompoundTag tag = NbtIo.readCompressed(path.toFile());
                             if (tag != null) {
                                 backups.add(PlayerDataSnapshot.fromNBT(tag));
                             }
                         } catch (IOException e) {
                             e.printStackTrace();
                         }
                     }
                 });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Sort descending by timestamp
        backups.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return backups;
    }

    public static void purgeExcessSaves(UUID playerUUID, LogType logType, int maxSaves) {
        if (maxSaves <= 0) return;
        Path backupDir = getBackupDirectory(playerUUID);
        if (!Files.exists(backupDir)) return;

        try (Stream<Path> paths = Files.list(backupDir)) {
            List<Path> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("_" + logType.name() + ".dat"))
                    .sorted((p1, p2) -> {
                        // Extract timestamp from filename
                        long t1 = extractTimestamp(p1);
                        long t2 = extractTimestamp(p2);
                        return Long.compare(t2, t1); // Sort descending
                    })
                    .collect(Collectors.toList());

            if (files.size() > maxSaves) {
                for (int i = maxSaves; i < files.size(); i++) {
                    Files.deleteIfExists(files.get(i));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long extractTimestamp(Path path) {
        String name = path.getFileName().toString();
        try {
            return Long.parseLong(name.substring(0, name.indexOf('_')));
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            return 0;
        }
    }
}
