package eu.buildsouthwest.warp.core.storage;

import eu.buildsouthwest.warp.core.model.Warp;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WarpCache {

    Optional<Warp> getWarp(UUID uuid);
    Collection<Warp> getAll();
    void put(UUID uuid, Warp warp);
    void putAll(Collection<Warp> warps);
    void invalidate(UUID uuid);
    void invalidateAll();
    long size();
}