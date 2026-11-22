package com.auroria.nodesextra;
import java.util.*;
public final class SupplyGraphTest {
    static void check(boolean result,String why){if(!result)throw new AssertionError(why);}
    public static void main(String[]args){
        Map<Integer,int[]> edges=new HashMap<>();edges.put(1,new int[]{2,3});edges.put(2,new int[]{1,4});edges.put(3,new int[]{1,4});edges.put(4,new int[]{2,3,5});edges.put(5,new int[]{4});
        check(SupplyGraph.path(1,5,edges,Set.of(1,2,3,4,5),2).isEmpty(),"route max hop ceiling");
        check(SupplyGraph.path(1,5,edges,Set.of(1,2,3,4,5),4).size()==4 || SupplyGraph.path(1,5,edges,Set.of(1,2,3,4,5),4).size()==5,"route exists");
        check(SupplyGraph.path(1,5,edges,Set.of(1,3,4,5),4).equals(List.of(1,3,4,5)),"disallowed territories excluded");
        check(SupplyGraph.path(1,5,edges,Set.of(1,2,5),5).isEmpty(),"hostile territory blocks route");
        check(SupplyGraph.path(3,3,edges,Set.of(3),0).equals(List.of(3)),"same territory can be supplied");
        check(SupplyGraph.path(3,3,edges,Set.of(),5).isEmpty(),"no supply on hostile origin");
        Map<Long,Integer> roads=Map.of(SupplyGraph.edgeKey(1,3),1, SupplyGraph.edgeKey(3,4),1,SupplyGraph.edgeKey(4,5),2);
        check(SupplyGraph.cheapestPath(1,5,edges,Set.of(1,2,3,4,5),roads,5).nodes().equals(List.of(1,3,4,5)),"rail and sea weighted path");
        check(SupplyGraph.cheapestPath(1,5,edges,Set.of(1,2,3,4,5),Map.of(),5).nodes().isEmpty(),"plain route cost ceiling");
        System.out.println("PASS SupplyGraphTest");
    }
}
