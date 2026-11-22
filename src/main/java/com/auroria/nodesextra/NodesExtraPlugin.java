package com.auroria.nodesextra;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;

public final class NodesExtraPlugin extends JavaPlugin {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private Models.State state;
    private NodeBridge nodes;
    private Logistics logistics;
    private Infrastructure infrastructure;
    private Government government;
    private Path dataPath;
    private Path publicPath;
    private NodesCommandBridge nodesCommandBridge;

    @Override public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("milzzynodes")) {
            getLogger().severe("Requires an enabled 'milzzynodes' core plugin");
            getServer().getPluginManager().disablePlugin(this); return;
        }
        saveDefaultConfig();
        reloadConfig();
        try {
            nodes = new NodeBridge();
            dataPath = safeFile(getConfig().getString("storage.stateFilename", "state.json"), "state.json");
            publicPath = safeFile(getConfig().getString("storage.publicFilename", "public.json"), "public.json");
            loadState();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Cannot initialize Nodes-Extra; existing state will NOT be overwritten",ex);
            getServer().getPluginManager().disablePlugin(this); return;
        }
        government = new Government(this);
        logistics = new Logistics(this);
        infrastructure = new Infrastructure(this);
        ExtraCommands commands = new ExtraCommands(this);
        Objects.requireNonNull(getCommand("nextra")).setExecutor(commands);
        Objects.requireNonNull(getCommand("nextra")).setTabCompleter(commands);
        nodesCommandBridge = NodesCommandBridge.install(this, commands);
        long period = Math.max(20, Math.min(3600, getConfig().getLong("storage.saveIntervalSeconds",120))) * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            try {government.finishExpiredElections(); saveState();} catch (Exception ex) {
                getLogger().log(Level.SEVERE, "Nodes-Extra periodic save failed",ex);
            }
        }, period, period);
        getLogger().info("Nodes-Extra enabled: Logistics=" + logistics.enabled() + ", Government=" + government.enabled());
    }
    @Override public void onDisable() {
        if (nodesCommandBridge != null) nodesCommandBridge.uninstall();
        if (state != null) try {saveState();} catch(Exception ex) {getLogger().log(Level.SEVERE,"Save on disable failed",ex);}
    }
    private Path safeFile(String configured, String fallback) {
        String filename = configured == null ? fallback : configured.trim();
        // State paths cannot escape the plugin folder or target subdirectories.
        if (!filename.matches("[A-Za-z0-9_.-]+") || !filename.endsWith(".json"))
            throw new IllegalArgumentException("Invalid JSON file name in config: " + configured);
        return getDataFolder().toPath().resolve(filename);
    }
    private void loadState() throws IOException {
        Files.createDirectories(getDataFolder().toPath());
        if (Files.exists(dataPath)) {
            if (Files.size(dataPath)>15_000_000) throw new IOException("State too large; refusing to overwrite it");
            state = gson.fromJson(Files.readString(dataPath, StandardCharsets.UTF_8), Models.State.class);
            if (state == null || state.schema != 1) throw new IOException("Unsupported or null state file");
            if (state.links == null) state.links = new HashMap<>(); // old Nodes-Extra schema
            if (state.governments==null || state.depots==null || state.cooldowns==null || state.sabotageCooldowns==null)
                throw new IOException("Invalid state maps");
        } else state = new Models.State();
    }
    synchronized void saveState() throws IOException {
        if (state == null) return;
        writeAtomic(dataPath, gson.toJson(state));
        if (getConfig().getBoolean("logistics.exportMapData",true)) exportPublic();
    }
    static void writeAtomic(Path path, String contents) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temp, contents, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch (AtomicMoveNotSupportedException ex) {Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
    }
    private void exportPublic() throws IOException {
        Models.PublicExport out = new Models.PublicExport(); out.generatedAt = Instant.now().toEpochMilli();
        for (var entry : state.governments.entrySet()) {
            var n = nodes.nationById(entry.getKey()); if (n==null) continue;
            var g = entry.getValue();
            Map<String,Object> one = new LinkedHashMap<>();
            one.put("nation",n.getName()); one.put("nationId",n.getUuid().toString());
            one.put("type",g.type);
            one.put("head",g.head == null ? "" : nodes.citizenName(g.head,n));
            one.put("laws",g.laws);
            one.put("electionActive",g.election!=null);
            out.governments.add(one);
        }
        for (var d : state.depots.values()) {
            var n = nodes.nationById(d.nationId); if (n==null) continue;
            Map<String,Object> one = new LinkedHashMap<>();
            one.put("id",d.id); one.put("name",d.name); one.put("nation",n.getName());
            one.put("nationId",n.getUuid().toString()); one.put("world",d.world);
            one.put("x",d.x); one.put("y",d.y); one.put("z",d.z);
            one.put("suppliesAvailable", d.food>0 || d.equipment>0);
            if(getConfig().getBoolean("logistics.mapExposeStock",false)){
                one.put("food",d.food);one.put("equipment",d.equipment);one.put("fuel",d.fuel);
            }
            one.put("blockadedUntil",d.blockadedUntil);
            out.depots.add(one);
        }
        for(var l : state.links.values()) {
            var n=nodes.nationById(l.nationId); if(n==null)continue;
            Map<String,Object> one=new LinkedHashMap<>(); one.put("nation",n.getName());
            one.put("type",l.type); one.put("fromTerritory",l.from); one.put("toTerritory",l.to);
            one.put("world",l.world);out.infrastructure.add(one);
        }
        writeAtomic(publicPath,gson.toJson(out));
    }
    void loadSettings() {reloadConfig();getLogger().info("Reloaded Nodes-Extra config; persistent state was not replaced");}
    Models.State state() {return state;}
    NodeBridge nodes() {return nodes;}
    Logistics logistics() {return logistics;}
    Infrastructure infrastructure() {return infrastructure;}
    Government government() {return government;}
    String pretty(Object value) {return gson.toJson(value);}
}
