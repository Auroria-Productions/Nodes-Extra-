package com.auroria.nodesextra;

import java.util.*;

/** Gson-compatible states; UUIDs are stored as strings for easy backup/editing. */
final class Models {
    static final class State {
        int schema = 1;
        Map<String, Government> governments = new HashMap<>(); // nation UUID -> state
        Map<String, Depot> depots = new HashMap<>(); // normalized depot ID -> depot
        Map<String, InfraLink> links = new HashMap<>(); // Roads, railways and sea lanes
        Map<String,Long> cooldowns = new HashMap<>(); // player UUID + action -> unix milliseconds
        Map<String,Long> sabotageCooldowns = new HashMap<>();
    }
    static final class Government {
        String type = "MONARCHY";
        String head = null; // Government office only; never changes Nodes capital mayor
        String successor = null;
        Map<String,String> offices = new HashMap<>(); // e.g. MARSHAL -> resident UUID
        Map<String,Boolean> laws = new HashMap<>();
        Election election = null;
        List<String> history = new ArrayList<>();
    }
    static final class Election {
        long openedAt;
        long closesAt;
        Set<String> candidates = new HashSet<>();
        Map<String,String> ballots = new HashMap<>(); // voter UUID -> candidate UUID
    }
    static final class Depot {
        String id, name, nationId, townId, world;
        int x,y,z, territoryId;
        int food, equipment, fuel;
        long blockadedUntil;
        String blockadedBy;
    }
    static final class InfraLink {
        String id, nationId, type, world;
        int from, to;
    }
    static final class PublicExport {
        int schema = 1;
        long generatedAt;
        List<Map<String,Object>> governments = new ArrayList<>();
        List<Map<String,Object>> depots = new ArrayList<>();
        List<Map<String,Object>> infrastructure = new ArrayList<>();
    }
}
