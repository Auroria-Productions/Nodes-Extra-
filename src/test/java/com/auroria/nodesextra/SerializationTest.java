package com.auroria.nodesextra;
import com.google.gson.Gson;
import java.util.*;
public final class SerializationTest {
    static void check(boolean result,String why){if(!result)throw new AssertionError(why);}
    public static void main(String[]args){
        Models.State state=new Models.State();Models.Government gov=new Models.Government();gov.type="REPUBLIC";gov.head="head-uuid";
        Models.Election election=new Models.Election();election.candidates.add("x");election.ballots.put("voter","x");gov.election=election;
        state.governments.put("nation-id",gov);Models.Depot depot=new Models.Depot();depot.id="d";depot.food=15;state.depots.put("d",depot);
        Gson gson=new Gson();Models.State round=gson.fromJson(gson.toJson(state),Models.State.class);
        check(round.governments.get("nation-id").election.ballots.get("voter").equals("x"),"ballots persist");
        check(round.depots.get("d").food==15,"stocks persist");
        check(round.schema==1,"schema persists");
        System.out.println("PASS SerializationTest");
    }
}
