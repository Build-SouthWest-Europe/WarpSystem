package eu.buildsouthwest.warp.core.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import eu.buildsouthwest.warp.core.model.Warp;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CaffeineWarpCache implements WarpCache{
    private final Cache<UUID, Warp> cache = Caffeine.newBuilder().build();

    @Override
    public Optional<Warp> getWarp(UUID uuid) {
        return Optional.ofNullable(cache.getIfPresent(uuid));
    }

    @Override
    public Collection<Warp> getAll(){ return List.copyOf(cache.asMap().values()); }

    @Override
    public void put(UUID uuid, Warp warp) { cache.put(uuid, warp); }

    @Override
    public void putAll(Collection<Warp> warps) { warps.forEach(w -> cache.put(w.uuid(), w)); }

    @Override
    public void invalidate(UUID uuid) { cache.invalidate(uuid); }

    @Override
    public void invalidateAll() { cache.invalidateAll(); }

    @Override
    public long size() { return cache.estimatedSize(); }
}