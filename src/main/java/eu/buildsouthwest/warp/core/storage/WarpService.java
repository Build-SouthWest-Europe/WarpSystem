package eu.buildsouthwest.warp.core.storage;

import eu.buildsouthwest.warp.WarpSystem;
import eu.buildsouthwest.warp.core.model.Author;
import eu.buildsouthwest.warp.core.model.GeoMetadata;
import eu.buildsouthwest.warp.core.model.Warp;
import org.bukkit.Bukkit;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WarpService {
    private final WarpSystem plugin;
    private final WarpRepository repo;
    private final WarpCache cache;

    public WarpService(WarpSystem plugin, WarpRepository repo, WarpCache cache) {
        this.plugin = plugin;
        this.repo = repo;
        this.cache = cache;
    }

    public Map<UUID, Warp> getAll(){
        return cache.getAll();
    }

    public Optional<Warp> findById(UUID uuid){
        return cache.getWarp(uuid);
    }

    public CompletableFuture<Boolean> create(Warp warp, Author author, GeoMetadata geo){
        return repo.create(warp, author, geo).thenApply(ok -> {
            if(ok) cache.put(warp.uuid(),warp);
            return ok;
        });
    }

    public CompletableFuture<Boolean> delete(UUID uuid){
        return repo.delete(uuid).thenApply(ok -> {
            if (ok) cache.invalidate(uuid);
            return ok;
        });
    }

    public void startRefreshTask(){
        long time = 20L*60*5;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::refresh, time, time);
    }

    public void refresh(){
        repo.getAllWarps().whenComplete((warps, err) -> {
            if(err != null){
                plugin.getLogger().log(Level.SEVERE, "Warp cache refresh failed, no data was replaced", err);
                return;
            }

            cache.replaceAll(warps);
            plugin.getLogger().info("Warp cache refreshed: " + cache.size() + " warps");
        });
    }
}
