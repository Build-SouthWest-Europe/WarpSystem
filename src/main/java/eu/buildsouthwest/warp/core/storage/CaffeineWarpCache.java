package eu.buildsouthwest.warp.core.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import eu.buildsouthwest.warp.core.model.Warp;

import java.util.*;

public class CaffeineWarpCache implements WarpCache{
    private final Cache<UUID, Warp> cache = Caffeine.newBuilder().build();

    @Override
    public Optional<Warp> getWarp(UUID uuid) {
        return Optional.ofNullable(cache.getIfPresent(uuid));
    }

    @Override
    public Map<UUID, Warp> getAll(){ return cache.asMap(); }

    @Override
    public void put(UUID uuid, Warp warp) { cache.put(uuid, warp); }

    @Override
    public void putAll(Map<UUID, Warp> warps) {
        cache.putAll(warps);
    }

    @Override
    public void invalidate(UUID uuid) { cache.invalidate(uuid); }

    @Override
    public void invalidateAll() { cache.invalidateAll(); }

    @Override
    public void replaceAll(Map<UUID, Warp> warps){
        cache.putAll(warps);
        cache.asMap().keySet().removeIf(id -> !warps.containsKey(id));
    };

    @Override
    public long size() { return cache.estimatedSize(); }
}