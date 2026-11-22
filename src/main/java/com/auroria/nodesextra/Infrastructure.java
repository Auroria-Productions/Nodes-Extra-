package com.auroria.nodesextra;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import phonon.nodes.Nodes;
import phonon.nodes.objects.Nation;
import phonon.nodes.objects.Port;
import phonon.nodes.objects.PortGroup;
import phonon.nodes.objects.Territory;
import java.util.*;

/** Territory-bound logical routes; no terrain edits or Dynmap writes. */
final class Infrastructure {
    private final NodesExtraPlugin plugin;
    Infrastructure(NodesExtraPlugin p){plugin=p;}
    boolean enabled(){return plugin.getConfig().getBoolean("logistics.enabled",true)&&plugin.getConfig().getBoolean("logistics.infrastructure.enabled",true);}
    private String key(Nation n,int a,int b){return n.getUuid()+":"+Math.min(a,b)+":"+Math.max(a,b);}
    boolean linkActive(Models.InfraLink link,Nation nation, Map<Integer,Territory> all){
        if(!enabled()||!nation.getUuid().toString().equals(link.nationId))return false;
        Territory a=all.get(link.from),b=all.get(link.to);
        return a!=null&&b!=null&&plugin.nodes().friendly(a,nation,false,false)
                &&plugin.nodes().friendly(b,nation,false,false)
                &&(!link.type.equals("SEA") || sharedPortGroup(link.from,link.to));
    }
    void addLinksToGraph(Nation n,Map<Integer,Territory> territories,Map<Integer,int[]> neighbors,Map<Long,Integer> weights){
        if(!enabled())return;
        for(Models.InfraLink link:plugin.state().links.values()){
            if(!linkActive(link,n,territories))continue;
            int weight=switch(link.type){case "RAIL"->1;case "ROAD"->3;case "SEA"->2;default->4;};
            int from=link.from,to=link.to;
            // ROAD and RAIL reuse natural adjacency; SEA adds a verified Nodes port connection.
            if(link.type.equals("SEA")){
                neighbors.compute(from,(k,v)->append(v,to));neighbors.compute(to,(k,v)->append(v,from));
            }
            weights.merge(SupplyGraph.edgeKey(from,to),weight,Math::min);
        }
    }
    private int[] append(int[] src,int value){
        int[] a=src==null?new int[0]:src;for(int n:a)if(n==value)return a;
        int[] next=Arrays.copyOf(a,a.length+1);next[a.length]=value;return next;
    }
    void command(CommandSender sender,String[] args){
        if(!enabled()){msg(sender,"Infrastructure disabled");return;}
        if(!(sender instanceof Player p)){msg(sender,"Use this command in-game");return;}
        Nation n=plugin.nodes().nation(p);if(n==null){msg(sender,"Join a nation");return;}
        String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"list";
        Map<Integer,Territory> all=plugin.nodes().allTerritories();
        switch(action){
            case "list" -> {
                List<Models.InfraLink> links=plugin.state().links.values().stream().filter(l->l.nationId.equals(n.getUuid().toString())).toList();
                if(links.isEmpty())msg(sender,"No infrastructure routes built yet");
                for(Models.InfraLink l:links)msg(sender,l.type+" "+l.from+" ⇄ "+l.to+(linkActive(l,n,all)?"":" §c(inactive)"));
            }
            case "near" -> {
                Territory current=plugin.nodes().territory(p);if(current==null){msg(sender,"No Nodes territory at this location");return;}
                int id=plugin.nodes().id(current);
                msg(sender,"Standing in #"+id+" ("+current.getName()+")");
                int[] others=plugin.nodes().neighbors(current);
                List<String> lines=new ArrayList<>();for(int other:others){Territory t=all.get(other);lines.add("#"+other+" "+(t==null?"?":t.getName()));}
                msg(sender,"Adjacent territories: "+String.join(", ",lines));
            }
            case "build" -> {
                if(args.length!=4){msg(sender,"/nextra infra build <road|rail|sea> <target-territory-id>");return;}
                if(!plugin.logistics().isCommander(p,n)){msg(sender,"Town officer, general or national marshal required");return;}
                String type=args[2].toUpperCase(Locale.ROOT);
                if(!Set.of("ROAD","RAIL","SEA").contains(type)){msg(sender,"Type must be road, rail or sea");return;}
                int to;try{to=Integer.parseInt(args[3]);}catch(NumberFormatException ex){msg(sender,"Target territory ID must be a number");return;}
                Territory current=plugin.nodes().territory(p),destination=all.get(to);
                if(current==null||destination==null||!plugin.nodes().friendly(current,n,false,false)||!plugin.nodes().friendly(destination,n,false,false)){
                    msg(sender,"Both territories must be owned by your nation");return;}
                int from=plugin.nodes().id(current);if(from==to){msg(sender,"Choose a different territory");return;}
                String k=key(n,from,to);
                boolean adjacent=false;for(int near:plugin.nodes().neighbors(current))if(near==to)adjacent=true;
                if(!type.equals("SEA")&&!adjacent){msg(sender,"Road and rail routes require adjacent Nodes territories");return;}
                if(type.equals("SEA")&&!sharedPortGroup(from,to)){
                    msg(sender,"Sea routes require existing Nodes ports in both territories sharing a port group");return;}
                Models.InfraLink prior=plugin.state().links.get(k);
                if(prior!=null&&prior.type.equals(type)){msg(sender,"That link is already built");return;}
                String path="logistics.infrastructure.costs."+type.toLowerCase(Locale.ROOT);
                Material material=Material.matchMaterial(plugin.getConfig().getString(path+".material","COBBLESTONE"));
                int amount=Math.max(1,Math.min(2304,plugin.getConfig().getInt(path+".amount",32)));
                if(material==null||!material.isItem()){msg(sender,"Invalid infrastructure material configuration");return;}
                int found=0;for(ItemStack item:p.getInventory().getStorageContents())if(item!=null&&item.getType()==material)found+=item.getAmount();
                if(found<amount){msg(sender,"Building costs "+amount+" "+material);return;}
                if(!p.getInventory().removeItem(new ItemStack(material,amount)).isEmpty()){
                    msg(sender,"Inventory changed; insufficient resources");return;}
                Models.InfraLink link=new Models.InfraLink();link.id=k;link.nationId=n.getUuid().toString();link.from=from;link.to=to;link.type=type;
                link.world=p.getWorld().getName();plugin.state().links.put(k,link);
                msg(sender,"Built "+type+" network link #"+from+" ⇄ #"+to+(prior==null?"":" (replaced older link)"));
            }
            case "remove" -> {
                if(args.length!=3){msg(sender,"/nextra infra remove <target-territory-id>");return;}
                if(!plugin.logistics().isCommander(p,n)){msg(sender,"Officer/marshal authority required");return;}
                Territory current=plugin.nodes().territory(p);if(current==null||!plugin.nodes().friendly(current,n,false,false)){msg(sender,"Stand in your nation's territory");return;}
                int to;try{to=Integer.parseInt(args[2]);}catch(NumberFormatException ex){msg(sender,"Invalid target ID");return;}
                int from=plugin.nodes().id(current);
                if(plugin.state().links.remove(key(n,from,to))!=null)msg(sender,"Infrastructure link removed. No refund.");
                else msg(sender,"No such link from this territory");
            }
            default -> msg(sender,"/nextra infra <list|near|build|remove>");
        }
    }
    /** Only allow long-range sea lanes between port nodes already connected in native Nodes port groups. */
    private boolean sharedPortGroup(int a,int b){
        Set<String> groupA=new HashSet<>(),groupB=new HashSet<>();
        for(Port port: Nodes.INSTANCE.getPorts$nodes().values()){
            Territory t=plugin.nodes().territoryAt("",port.getLocX(),port.getLocZ());
            if(t==null)continue;int id=plugin.nodes().id(t);
            if(id!=a&&id!=b)continue;
            for(PortGroup group:port.getGroups())(id==a?groupA:groupB).add(group.getName());
        }
        groupA.retainAll(groupB);return !groupA.isEmpty();
    }
    private void msg(CommandSender sender,String text){sender.sendMessage("§a[Nodes-Extra Infrastructure] §f"+text);}
}
