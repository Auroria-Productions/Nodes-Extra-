package com.auroria.nodesextra;
import java.util.*;
public final class ElectionMath {
    private ElectionMath() {}
    public static Map<String,Integer> tally(Collection<String> candidates, Map<String,String> votes) {
        Map<String,Integer> counts = new TreeMap<>();
        for (String c : candidates) counts.put(c,0);
        for (String c : votes.values()) if (counts.containsKey(c)) counts.merge(c,1,Integer::sum);
        return counts;
    }
    public static String winner(Collection<String> candidates, Map<String,String> votes) {
        Map<String,Integer> counts = tally(candidates,votes);
        if (counts.isEmpty()) return null;
        return counts.entrySet().stream().sorted((a,b)->{
            int n=Integer.compare(b.getValue(),a.getValue());
            return n==0?a.getKey().compareTo(b.getKey()):n;
        }).findFirst().orElseThrow().getKey();
    }
}
