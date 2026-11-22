package com.auroria.nodesextra;
import java.util.Arrays;
public final class NodesCommandBridgeRoutingTest {
    static void eq(String[] expected, String[] actual) {
        if (!Arrays.equals(expected,actual)) throw new AssertionError("expected="+Arrays.toString(expected)+" actual="+Arrays.toString(actual));
    }
    public static void main(String[] args) {
        eq(null, NodesCommandBridge.route(new String[]{}));
        eq(null, NodesCommandBridge.route(new String[]{"help"}));
        eq(null, NodesCommandBridge.route(new String[]{"war"}));
        eq(null, NodesCommandBridge.route(new String[]{"town","test"}));
        eq(null, NodesCommandBridge.route(new String[]{"nation"}));
        eq(new String[]{"help"}, NodesCommandBridge.route(new String[]{"extra"}));
        eq(new String[]{"status"}, NodesCommandBridge.route(new String[]{"extra","status"}));
        eq(new String[]{"depot","add","Alpha"}, NodesCommandBridge.route(new String[]{"logistics","depot","add","Alpha"}));
        eq(new String[]{"gov"}, NodesCommandBridge.route(new String[]{"government"}));
        eq(new String[]{"gov","type","republic"}, NodesCommandBridge.route(new String[]{"government","type","republic"}));
        eq(new String[]{"gov","info"}, NodesCommandBridge.route(new String[]{"gov","info"}));
        eq(new String[]{"election","vote","Alice"}, NodesCommandBridge.route(new String[]{"election","vote","Alice"}));
        eq(new String[]{"DePoT","list"}, NodesCommandBridge.route(new String[]{"DePoT","list"}));
        System.out.println("PASS NodesCommandBridgeRoutingTest");
    }
}
