package com.auroria.nodesextra;

import org.bukkit.entity.Player;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Nation;
import phonon.nodes.objects.Resident;
import phonon.nodes.objects.Territory;
import phonon.nodes.objects.Town;
import java.lang.reflect.Method;
import java.util.*;

/** Public Nodes API adapter. Uses reflection only for Kotlin inline TerritoryId method names. */
final class NodeBridge {
    private final Method territoryId;
    private final Method territoryNeighbors;
    NodeBridge() {
        try {
            territoryId = Territory.class.getMethod("getId-L9_GXOM");
            territoryNeighbors = Territory.class.getMethod("getNeighbors-CCo7D7I");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unsupported MilzzyNodes territory API", ex);
        }
    }
    Resident resident(Player player) { return Nodes.INSTANCE.getResident(player); }
    Nation nation(Player player) { Resident r = resident(player); return r == null ? null : r.getNation(); }
    Town town(Player player) { Resident r = resident(player); return r == null ? null : r.getTown(); }
    Territory territory(Player player) { return Nodes.INSTANCE.getTerritoryFromPlayer(player); }
    Nation nationById(String id) {
        if (id == null) return null;
        for (Nation n : Nodes.INSTANCE.getNations$nodes().values()) if (n.getUuid().toString().equals(id)) return n;
        return null;
    }
    Nation nationByName(String name) {
        if (name == null) return null;
        for (Nation n : Nodes.INSTANCE.getNations$nodes().values()) if (n.getName().equalsIgnoreCase(name)) return n;
        return null;
    }
    boolean isCitizen(Player player, Nation nation) {
        Nation playerNation = nation(player);
        return playerNation != null && nation != null && playerNation.getUuid().equals(nation.getUuid());
    }
    boolean isSovereign(Player player, Nation nation) {
        if (nation == null || nation.getCapital() == null || nation.getCapital().getLeader() == null) return false;
        return player.getUniqueId().equals(nation.getCapital().getLeader().getUuid());
    }
    boolean isTownCommander(Player player, Nation nation) {
        if (!isCitizen(player, nation)) return false;
        Resident r = resident(player); if (r == null) return false;
        Town t = r.getTown(); if (t == null || t.getNation() != nation) return false;
        return (t.getLeader() != null && t.getLeader().getUuid().equals(player.getUniqueId()))
                || t.getOfficers().contains(r) || t.getGenerals().contains(r);
    }
    boolean isCitizenId(String uuid, Nation nation) {
        if (nation == null) return false;
        for (Resident r : nation.getResidents()) if (r.getUuid().toString().equals(uuid)) return true;
        return false;
    }
    String citizenName(String uuid, Nation nation) {
        if (nation == null) return uuid;
        for (Resident r : nation.getResidents()) if (r.getUuid().toString().equals(uuid)) return r.getName();
        return uuid;
    }
    Resident citizenByName(Nation nation, String name) {
        if (nation == null) return null;
        for (Resident r : nation.getResidents()) if (r.getName().equalsIgnoreCase(name)) return r;
        return null;
    }
    int id(Territory territory) {
        try { return (Integer) territoryId.invoke(territory); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException("Failed to read territory ID",ex); }
    }
    int[] neighbors(Territory territory) {
        try { return (int[]) territoryNeighbors.invoke(territory); }
        catch (ReflectiveOperationException ex) { throw new IllegalStateException("Failed to read territory neighbors",ex); }
    }
    Territory territoryAt(String world, int x, int z) {
        // Nodes territories use one configured world; caller verifies the player's world.
        return Nodes.INSTANCE.getTerritoryFromBlock(x,z);
    }
    Map<Integer, Territory> allTerritories() {
        Map<Integer,Territory> out = new HashMap<>();
        for (Map.Entry<?,Territory> entry : Nodes.INSTANCE.iterTerritories()) out.put(id(entry.getValue()), entry.getValue());
        return out;
    }
    boolean friendly(Territory territory, Nation nation, boolean allied, boolean openBorders) {
        if (territory == null || territory.getTown() == null) return false;
        Nation owner = territory.getTown().getNation();
        return owner != null && (owner.getUuid().equals(nation.getUuid())
                || (allied && openBorders && nation.getAllies().contains(owner)));
    }
}
