package com.auroria.nodesextra;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Territory;
import phonon.nodes.objects.Nation;
import phonon.nodes.objects.Town;
import org.bukkit.plugin.java.JavaPlugin;
public final class ApiCompatibilityTest {
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        new NodeBridge();
        Class.forName("phonon.nodes.Nodes");
        // Nodes itself references Vault API, which is provided by the actual Paper server.
        // Direct Nodes method calls are compile-checked above, but offline reflection skips Vault classes.
        check(Territory.class.getMethod("getId-L9_GXOM")!=null,"mangled territory id");
        check(Territory.class.getMethod("getNeighbors-CCo7D7I")!=null,"mangled neighbors");
        check(Town.class.getMethod("getNation")!=null,"town nation");
        check(Nation.class.getMethod("getUuid")!=null,"nation stable id");
        check(JavaPlugin.class.isAssignableFrom(NodesExtraPlugin.class),"plugin superclass");
        System.out.println("PASS ApiCompatibilityTest");
    }
}
