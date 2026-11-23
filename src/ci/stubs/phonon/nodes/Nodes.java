package phonon.nodes;
import org.bukkit.entity.Player;
import phonon.nodes.objects.*;
import java.util.*;
/** Compile-time signatures ONLY: never package or execute these stubs. */
public final class Nodes {
 public static final Nodes INSTANCE = new Nodes();
 public Resident getResident(Player p){throw new UnsupportedOperationException();}
 public Territory getTerritoryFromPlayer(Player p){throw new UnsupportedOperationException();}
 public Territory getTerritoryFromBlock(int x,int z){throw new UnsupportedOperationException();}
 public LinkedHashMap<String,Nation> getNations$nodes(){throw new UnsupportedOperationException();}
 public LinkedHashMap<String,Port> getPorts$nodes(){throw new UnsupportedOperationException();}
 public Iterable<Map.Entry<TerritoryId,Territory>> iterTerritories(){throw new UnsupportedOperationException();}
}
