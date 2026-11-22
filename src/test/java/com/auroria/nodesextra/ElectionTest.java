package com.auroria.nodesextra;
import java.util.*;
public final class ElectionTest {
    static void check(boolean result,String why){if(!result)throw new AssertionError(why);}
    public static void main(String[]args){
        check(ElectionMath.winner(List.of(),Map.of())==null,"no candidates");
        check(ElectionMath.winner(Set.of("a","b"),Map.of("voter1","b","voter2","b","voter3","a")).equals("b"),"majority");
        check(ElectionMath.winner(Set.of("a","b"),Map.of("voter1","a","voter2","b")).equals("a"),"deterministic tie");
        check(ElectionMath.tally(Set.of("a"),Map.of("voter1","bogus")).get("a")==0,"ignore invalid candidate votes");
        System.out.println("PASS ElectionTest");
    }
}
