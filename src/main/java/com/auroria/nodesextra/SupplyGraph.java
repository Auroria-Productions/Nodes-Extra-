package com.auroria.nodesextra;

import java.util.*;

/** Pure bounded breadth-first search; independent of Bukkit for testability. */
public final class SupplyGraph {
    private SupplyGraph() {}
    public static List<Integer> path(int origin, int target, Map<Integer,int[]> neighbors,
                                     Set<Integer> friendlyTerritories, int maxHops) {
        if (maxHops < 0 || !friendlyTerritories.contains(origin) || !friendlyTerritories.contains(target)) return List.of();
        if (origin == target) return List.of(origin);
        Map<Integer,Integer> parents = new HashMap<>();
        Map<Integer,Integer> depth = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(origin); parents.put(origin, origin); depth.put(origin, 0);
        while (!queue.isEmpty()) {
            int current = queue.remove();
            int d = depth.get(current);
            if (d >= maxHops) continue;
            for (int next : neighbors.getOrDefault(current, new int[0])) {
                if (!friendlyTerritories.contains(next) || parents.containsKey(next)) continue;
                parents.put(next,current); depth.put(next,d+1);
                if (next == target) {
                    List<Integer> route = new ArrayList<>(); int at=target;
                    while (at != origin) {route.add(at); at=parents.get(at);}
                    route.add(origin); Collections.reverse(route); return route;
                }
                queue.add(next);
            }
        }
        return List.of();
    }
    /** Weighted route: normal territory hop costs 4, roads 3, railways 1, sea lanes 2. */
    public record WeightedRoute(List<Integer> nodes, int cost) {}
    public static long edgeKey(int a,int b) {
        return (((long)Math.min(a,b)) << 32) | (Math.max(a,b)&0xffffffffL);
    }
    public static WeightedRoute cheapestPath(int origin, int target,
            Map<Integer,int[]> neighbors, Set<Integer> friendly, Map<Long,Integer> edgeWeights, int maxCost) {
        if (!friendly.contains(origin) || !friendly.contains(target) || maxCost<0) return new WeightedRoute(List.of(),Integer.MAX_VALUE);
        if(origin==target)return new WeightedRoute(List.of(origin),0);
        Map<Integer,Integer> costs=new HashMap<>(),parent=new HashMap<>();
        PriorityQueue<int[]> queue=new PriorityQueue<>(Comparator.comparingInt(a->a[1]));
        costs.put(origin,0);queue.add(new int[]{origin,0});
        while(!queue.isEmpty()) {
            int[] at=queue.poll();int current=at[0],distance=at[1];
            if(distance!=costs.getOrDefault(current,Integer.MAX_VALUE) || distance>maxCost)continue;
            if(current==target){
                List<Integer> route=new ArrayList<>();int node=target;
                while(node!=origin){route.add(node);node=parent.get(node);}
                route.add(origin);Collections.reverse(route);return new WeightedRoute(route,distance);
            }
            for(int next:neighbors.getOrDefault(current,new int[0])){
                if(!friendly.contains(next))continue;
                int weight=Math.max(1,edgeWeights.getOrDefault(edgeKey(current,next),4));
                int n=distance+weight;
                if(n<=maxCost && n<costs.getOrDefault(next,Integer.MAX_VALUE)){
                    costs.put(next,n);parent.put(next,current);queue.add(new int[]{next,n});
                }
            }
        }
        return new WeightedRoute(List.of(),Integer.MAX_VALUE);
    }
}
