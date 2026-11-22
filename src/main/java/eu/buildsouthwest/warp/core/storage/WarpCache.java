package eu.buildsouthwest.warp.core.storage;

import eu.buildsouthwest.warp.core.model.Warp;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface WarpCache {

    Optional<Warp> getWarp(UUID uuid);
    Map<UUID, Warp> getAll();
    void put(UUID uuid, Warp warp);
    void putAll(Map<UUID, Warp> warps);
    void invalidate(UUID uuid);
    void invalidateAll();
    void replaceAll(Map<UUID, Warp> warps);
    long size();
}