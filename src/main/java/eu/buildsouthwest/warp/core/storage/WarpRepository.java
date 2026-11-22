package eu.buildsouthwest.warp.core.storage;

import eu.buildsouthwest.warp.core.model.Author;
import eu.buildsouthwest.warp.core.model.GeoMetadata;
import eu.buildsouthwest.warp.core.model.Warp;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface WarpRepository {

    CompletableFuture<Optional<Warp>> findByID(UUID id);

    CompletableFuture<Boolean> create(Warp warp, Author auth, GeoMetadata geo);

    CompletableFuture<Boolean> delete(UUID uuid);

    CompletableFuture<Map<UUID, Warp>> getAllWarps();
}