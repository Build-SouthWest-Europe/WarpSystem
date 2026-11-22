package eu.buildsouthwest.warp.core.database;

import eu.buildsouthwest.warp.core.model.*;
import eu.buildsouthwest.warp.core.storage.WarpRepository;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Logger;

public class DBWarpRepository implements WarpRepository {
    private final String GET_WARP = "SELECT b.*, a.*, g.*, e.*, c.* FROM bte_warps b LEFT JOIN author a ON a.creatorId = b.id LEFT JOIN geo_meta g ON g.warpID = b.id LEFT JOIN educational e ON e.warpID = b.id LEFT JOIN city_hub c ON c.warpID = b.id WHERE b.id = ?;";
    private final String GET_WARPS = "SELECT b.*, a.*, g.*, e.*, c.* FROM bte_warps b LEFT JOIN author a ON a.creatorId = b.id LEFT JOIN geo_meta g ON g.warpID = b.id LEFT JOIN educational e ON e.warpID = b.id LEFT JOIN city_hub c ON c.warpID = b.id";
    private final String NEW_WARP = "INSERT IGNORE INTO bte_warps (id,`name`,world,`description`,`type`,`x`,`y`,`z`,pitch,yaw,icon,visits,creatorId,creationDate) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,NOW());";
    private final String NEW_AUTHOR = "INSERT IGNORE INTO author (creatorId,creatorName) VALUES (?,?);";
    private final String NEW_GEO = "INSERT IGNORE INTO geo_meta (`warpId`, `latitude`, `longitude`, `state`) VALUES (?,?,?, '');";
    private final String DROP_WARP = "DELETE FROM bte_warps WHERE id = ?";

    private final DatabaseService db;

    public DBWarpRepository(DatabaseService db){
        this.db = db;
    }

    public CompletableFuture<Optional<Warp>> findByID(UUID warp) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = db.getConnection()){
                if(conn == null) throw new SQLException("Could not connect to database.");
                conn.setAutoCommit(false);
                try (PreparedStatement query = conn.prepareStatement(GET_WARP)) {
                    query.setString(1, warp.toString());

                    try (ResultSet rs = query.executeQuery()) {
                        if(!rs.next()) return Optional.empty();

                        return Optional.of((WarpWrapper.map(rs)));
                    } catch (SQLException e) {
                        throw new CompletionException(e);
                    }
                } catch (SQLException sqle) {
                    throw new CompletionException(sqle);
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw new CompletionException(sqle);
            }
        }, db.executor());
    }

    public CompletableFuture<Map<UUID, Warp>> getAllWarps() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Warp> warps = new HashMap<>();
            try (Connection conn = db.getConnection();
                 PreparedStatement query = conn.prepareStatement(GET_WARPS);
                 ResultSet res = query.executeQuery()){
                while(res.next()){
                    Warp w = WarpWrapper.map(res);
                    warps.put(w.uuid(), w);
                }

                return warps;
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw new CompletionException(sqle);
            }
        }, db.executor());
    }

    public CompletableFuture<Boolean> create(Warp warp, Author auth, GeoMetadata geo) {
        return CompletableFuture.supplyAsync( () -> {
            try (Connection conn = db.getConnection();){
                if(conn == null) throw new SQLException("Could not connect to database.");
                conn.setAutoCommit(false);

                try (PreparedStatement queryAuthor = conn.prepareStatement(NEW_AUTHOR);
                    PreparedStatement queryWarp = conn.prepareStatement(NEW_WARP);
                    PreparedStatement queryGeo = conn.prepareStatement(NEW_GEO)) {

                    queryAuthor.setString(1, auth.id().toString());
                    queryAuthor.setString(2, auth.name());

                    queryWarp.setString(1, warp.uuid().toString());
                    queryWarp.setString(2, warp.displayName());
                    queryWarp.setString(3, warp.world());
                    queryWarp.setString(4, warp.description());
                    queryWarp.setString(5, warp.type());
                    queryWarp.setDouble(6, warp.x());
                    queryWarp.setDouble(7, warp.y());
                    queryWarp.setDouble(8, warp.z());
                    queryWarp.setDouble(9, warp.pitch());
                    queryWarp.setDouble(10, warp.yaw());
                    queryWarp.setString(11, warp.icon());
                    queryWarp.setInt(12, 0);
                    queryWarp.setString(13, auth.id().toString());

                    queryGeo.setString(1, warp.uuid().toString());
                    queryGeo.setDouble(2, geo.latitude());
                    queryGeo.setDouble(3, geo.longitude());

                    log("new author", queryAuthor, queryAuthor.executeUpdate());
                    log("new warp", queryWarp, queryWarp.executeUpdate());
                    log("new geo", queryGeo, queryGeo.executeUpdate());

                    conn.commit();
                    Bukkit.getLogger().info("[newWarp] committed " + warp.uuid());

                    return true;
                } catch (SQLException sqle) {
                    conn.rollback();
                    throw sqle;
                }
            } catch (SQLException sqle) {
                throw new CompletionException(sqle);
            }
        }, db.executor());
    }

    public CompletableFuture<Boolean> delete(UUID uuid){
        return CompletableFuture.supplyAsync( () -> {
            try(Connection conn = db.getConnection()){
                if(conn == null) throw new SQLException("Could not connect to database.");
                conn.setAutoCommit(false);

                try (PreparedStatement dropQuery = conn.prepareStatement(DROP_WARP)) {
                    dropQuery.setString(1, uuid.toString());

                    if (dropQuery.executeUpdate() >= 1) {
                        return true;
                    } else {
                        throw new SQLException("Failed to delete warp");
                    }
                } catch (SQLException sqle) {
                    conn.rollback();
                    throw sqle;
                }
            } catch (SQLException sqle) {
                sqle.printStackTrace();
                throw new CompletionException(sqle);
            }
        }, db.executor());
    }

    private void log(String message, PreparedStatement query, int rows) throws SQLException{
        Logger log = Bukkit.getLogger();

        log.info("[Warp_System_Create_Warp] " + message + ", rows = " + rows);
        for(SQLWarning warn = query.getWarnings(); warn != null; warn = warn.getNextWarning()){
            log.warning("[Warp_System_Create_Warp] " + message + " warning: " + warn.getMessage());
        }
    }
}

final class WarpWrapper {
    private WarpWrapper() {}

    static Warp map(ResultSet rs) throws SQLException {
        return new Warp(
                UUID.fromString(rs.getString("id")),
                rs.getString("name"),
                rs.getString("world"),
                rs.getString("description"),
                rs.getString("type"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getDouble("pitch"),
                rs.getDouble("yaw"),
                rs.getString("icon"),
                GeoMetaWrapper.map(rs),
                EducationalDataMapper.map(rs),
                CityHubMapper.map(rs)
        );
    }
}

final class GeoMetaWrapper {
    private GeoMetaWrapper() {}

    static Optional<GeoMetadata> map(ResultSet rs) throws SQLException {
        String lat = rs.getString("latitude");
        if(lat == null) return Optional.empty();

        return Optional.of(new GeoMetadata(rs.getDouble("latitude"),rs.getDouble("longitude"),rs.getString("state")));
    }
}

final class EducationalDataMapper{
    private EducationalDataMapper(){}

    static Optional<EducationalData> map(ResultSet rs) throws SQLException {
        String sj = rs.getString("e.warpId");
        if(sj == null) return Optional.empty();

        return Optional.of(new EducationalData());
    }
}

final class CityHubMapper{
    private CityHubMapper(){}

    static Optional<CityHubData> map(ResultSet rs) throws SQLException {
        String sj = rs.getString("c.warpId");
        if(sj == null) return Optional.empty();

        return Optional.of(new CityHubData());
    }
}

final class AuthorMapper{
    private AuthorMapper(){}
    static Optional<Author> map(ResultSet rs) throws SQLException {
        String authName = rs.getString("e.creatorName");

        if(authName == null) return Optional.empty();
        return Optional.of(new Author(UUID.fromString(rs.getString("a_creatorId")), authName));
    }
}