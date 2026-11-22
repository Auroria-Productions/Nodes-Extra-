package com.auroria.nodesextra;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import phonon.nodes.objects.Nation;
import phonon.nodes.objects.Territory;
import java.util.*;

final class Logistics {
    private final NodesExtraPlugin plugin;
    Logistics(NodesExtraPlugin plugin){this.plugin=plugin;}
    boolean enabled(){return plugin.getConfig().getBoolean("logistics.enabled",true);}
    void msg(CommandSender s,String text){s.sendMessage("§a[Nodes-Extra Logistics] §f"+text);}
    int value(String key,int fallback,int min,int max){return Math.max(min,Math.min(max,plugin.getConfig().getInt("logistics."+key,fallback)));}
    Material material(String id){
        String name=plugin.getConfig().getString("logistics.depositMaterials."+id, switch(id){case "food"->"BREAD";case "equipment"->"IRON_INGOT";default->"COAL";});
        Material material=Material.matchMaterial(name==null?"":name);
        if(material==null||!material.isItem()||material==Material.AIR){throw new IllegalArgumentException("Invalid logistics material: "+name);}
        return material;
    }
    String depotKey(Nation n,String name){return n.getUuid()+":"+name.toLowerCase(Locale.ROOT);}
    Models.Depot depot(Nation n,String name){return plugin.state().depots.get(depotKey(n,name));}
    boolean blocked(Models.Depot depot){return System.currentTimeMillis()<depot.blockadedUntil;}
    boolean isCommander(Player p,Nation nation){
        return p.hasPermission("nodesextra.admin")||!plugin.getConfig().getBoolean("logistics.depotRegistrationRequiresLeaderOrOfficer",true)
                ||plugin.nodes().isTownCommander(p,nation)||plugin.government().isMarshal(p,nation);
    }
    private boolean validDepot(Models.Depot depot){
        Nation nation=plugin.nodes().nationById(depot.nationId);
        if(nation==null)return false;
        Territory t=plugin.nodes().territoryAt(depot.world,depot.x,depot.z);
        if(t==null||t.getTown()==null||t.getTown().getNation()==null)return false;
        return plugin.nodes().id(t)==depot.territoryId && t.getTown().getNation().getUuid().equals(nation.getUuid());
    }
    void depots(CommandSender sender,String[] a){
        if(!enabled()){msg(sender,"Logistics is disabled");return;}
        if(a.length>=2&&a[1].equalsIgnoreCase("all")&&sender.hasPermission("nodesextra.admin")){
            for(Models.Depot d:plugin.state().depots.values())msg(sender,describe(d));return;
        }
        if(!(sender instanceof Player p)){msg(sender,"Use in-game or /nextra depot all as admin");return;}
        Nation n=plugin.nodes().nation(p);if(n==null){msg(sender,"Join a nation first");return;}
        String command=a.length>1?a[1].toLowerCase(Locale.ROOT):"list";
        switch(command){
            case "list" -> {
                var owned=plugin.state().depots.values().stream().filter(d->n.getUuid().toString().equals(d.nationId)).sorted(Comparator.comparing(d->d.name)).toList();
                if(owned.isEmpty())msg(sender,"No depots. /nextra depot add <name> while standing on owned land");
                else for(var d:owned)msg(sender,describe(d));
            }
            case "add" -> {
                if(a.length!=3||!a[2].matches("[a-zA-Z0-9_-]{2,24}")){msg(sender,"/nextra depot add <2-24 character name>");return;}
                if(!isCommander(p,n)){msg(sender,"You must be a town leader/officer/general or nation marshal");return;}
                Territory t=plugin.nodes().territory(p);if(t==null||!plugin.nodes().friendly(t,n,false,false)){
                    msg(sender,"Must stand in your nation's owned territory");return;}
                String key=depotKey(n,a[2]);if(plugin.state().depots.containsKey(key)){msg(sender,"Depot name already exists");return;}
                long count=plugin.state().depots.values().stream().filter(d->d.nationId.equals(n.getUuid().toString())).count();
                if(count>=value("maxDepotsPerNation",12,1,100)){msg(sender,"Your nation has reached its depot limit");return;}
                Models.Depot d=new Models.Depot();d.id=key;d.name=a[2];d.nationId=n.getUuid().toString();
                d.townId=t.getTown().getUuid().toString();d.world=p.getWorld().getName();
                d.x=p.getLocation().getBlockX();d.y=p.getLocation().getBlockY();d.z=p.getLocation().getBlockZ();d.territoryId=plugin.nodes().id(t);
                plugin.state().depots.put(key,d);msg(sender,"Depot '"+d.name+"' registered in "+t.getName());
            }
            case "remove" -> {
                if(a.length!=3){msg(sender,"/nextra depot remove <name>");return;}
                if(!isCommander(p,n)){msg(sender,"Officer/marshal permission required");return;}
                Models.Depot d=depot(n,a[2]);if(d==null){msg(sender,"Unknown depot");return;}
                // Do not let a remote commander destroy deposits without physically reaching the depot.
                if(!atDepot(p,d)||!validDepot(d)){msg(sender,"Stand in a still-owned depot to dismantle it. Withdraw its materials first.");return;}
                if(d.food>0||d.equipment>0||d.fuel>0){msg(sender,"Depot still has supplies; use /nextra depot withdraw first");return;}
                plugin.state().depots.remove(d.id);msg(sender,"Depot removed");
            }
            case "deposit" -> {
                if(a.length!=5){msg(sender,"/nextra depot deposit <name> <food|equipment|fuel> <amount>");return;}
                Models.Depot d=depot(n,a[2]);if(d==null||!validDepot(d)){msg(sender,"Unknown or inactive depot");return;}
                if(!atDepot(p,d)){msg(sender,"You must be standing at this depot");return;}
                String type=a[3].toLowerCase(Locale.ROOT);if(!Set.of("food","equipment","fuel").contains(type)){msg(sender,"Invalid resource type");return;}
                int amount=parseAmount(a[4]);if(amount<=0){msg(sender,"Amount must be 1-2304");return;}
                Material material;try{material=material(type);}catch(IllegalArgumentException ex){msg(sender,ex.getMessage());return;}
                if(count(p,material)<amount){msg(sender,"You need "+amount+" " + material);return;}
                if((long)quantity(d,type)+amount>200_000){msg(sender,"Depot storage limit reached");return;}
                Map<Integer,ItemStack> leftovers=p.getInventory().removeItem(new ItemStack(material,amount));
                if(!leftovers.isEmpty()){msg(sender,"Inventory changed; deposit unsuccessful. Please check inventory.");return;}
                setQuantity(d,type,quantity(d,type)+amount);
                msg(sender,"Deposited "+amount+" "+type+" at "+d.name);
            }
            case "withdraw" -> {
                if(a.length!=5){msg(sender,"/nextra depot withdraw <name> <food|equipment|fuel> <amount>");return;}
                if(!isCommander(p,n)){msg(sender,"Officer/marshal permission required");return;}
                Models.Depot d=depot(n,a[2]);if(d==null||!atDepot(p,d)||!validDepot(d)){msg(sender,"Stand at your still-owned depot to withdraw");return;}
                String type=a[3].toLowerCase(Locale.ROOT);if(!Set.of("food","equipment","fuel").contains(type)){msg(sender,"Invalid resource type");return;}
                int amount=parseAmount(a[4]);if(amount<=0||amount>quantity(d,type)){msg(sender,"Insufficient stock or invalid amount");return;}
                Material mat;try{mat=material(type);}catch(IllegalArgumentException ex){msg(sender,ex.getMessage());return;}
                if(!hasSpace(p,mat,amount)){msg(sender,"Make inventory space first");return;}
                Map<Integer,ItemStack> extra=p.getInventory().addItem(new ItemStack(mat,amount));
                if(!extra.isEmpty()){msg(sender,"Inventory filled unexpectedly. Recovered items: "+extra.size());
                    for(ItemStack item:extra.values())p.getWorld().dropItemNaturally(p.getLocation(),item);
                }
                setQuantity(d,type,quantity(d,type)-amount);msg(sender,"Withdrew "+amount+" "+type);
            }
            case "info" -> {
                if(a.length!=3){msg(sender,"/nextra depot info <name>");return;}
                Models.Depot d=depot(n,a[2]);if(d==null){msg(sender,"Unknown depot");return;}
                msg(sender,describe(d));msg(sender,"Status: "+(!validDepot(d)?"Land lost":blocked(d)?"BLOCKADED":"Operational"));
            }
            default -> msg(sender,"/nextra depot <list|add|remove|deposit|withdraw|info> ...");
        }
    }
    private boolean atDepot(Player player,Models.Depot d){
        if(!player.getWorld().getName().equals(d.world))return false;
        double dx=player.getLocation().getX()-d.x,dz=player.getLocation().getZ()-d.z;
        return dx*dx+dz*dz<=16*16;
    }
    private String describe(Models.Depot d){return d.name+" ["+d.x+","+d.z+"] F:"+d.food+" E:"+d.equipment+" Fuel:"+d.fuel+(blocked(d)?" §cBLOCKADED":"");}
    private int parseAmount(String amount){try{int n=Integer.parseInt(amount);return n>=1&&n<=2304?n:-1;}catch(NumberFormatException ex){return -1;}}
    private int count(Player p,Material m){int n=0;for(ItemStack it:p.getInventory().getStorageContents())if(it!=null&&it.getType()==m)n+=it.getAmount();return n;}
    private boolean hasSpace(Player p,Material m,int amount){
        int capacity=0;for(ItemStack it:p.getInventory().getStorageContents()){
            if(it==null||it.getType()==Material.AIR)capacity+=m.getMaxStackSize();
            else if(it.getType()==m)capacity+=Math.max(0,it.getMaxStackSize()-it.getAmount());
        }return capacity>=amount;
    }
    private int quantity(Models.Depot d,String category){return switch(category){case "food"->d.food;case "equipment"->d.equipment;default->d.fuel;};}
    private void setQuantity(Models.Depot d,String category,int n){switch(category){case "food"->d.food=n;case "equipment"->d.equipment=n;default->d.fuel=n;}}
    private record Route(Models.Depot depot,List<Integer> territories,Map<Integer,Territory> lookup,int cost){}
    private Route bestRoute(Player p,Nation n,boolean food){
        Territory target=plugin.nodes().territory(p);if(target==null)return null;
        boolean allowAllies=plugin.getConfig().getBoolean("logistics.allowAlliedRoutes",false);
        Map<Integer,Territory> territories=plugin.nodes().allTerritories();
        Set<Integer> friendly=new HashSet<>();Map<Integer,int[]> neighbors=new HashMap<>();
        for(var entry:territories.entrySet()){
            Territory t=entry.getValue();Nation owner=t.getTown()==null?null:t.getTown().getNation();
            boolean openBorders = owner!=null && plugin.government().enabled() &&
                plugin.government().law(n,"OPEN_BORDERS") && plugin.government().law(owner,"OPEN_BORDERS");
            if(plugin.nodes().friendly(t,n,allowAllies,openBorders))friendly.add(entry.getKey());
            neighbors.put(entry.getKey(),plugin.nodes().neighbors(t));
        }
        int targetId=plugin.nodes().id(target);
        int max=value("maxHops",12,0,500);
        Map<Long,Integer> edgeWeights = new HashMap<>();
        plugin.infrastructure().addLinksToGraph(n,territories,neighbors,edgeWeights);
        Route shortest=null;
        for(Models.Depot depot:plugin.state().depots.values()){
            if(!depot.nationId.equals(n.getUuid().toString())||blocked(depot)||!validDepot(depot))continue;
            if(!p.getWorld().getName().equals(depot.world))continue;
            int fuel=value("fuelPerDelivery",1,0,500);
            if(food?depot.food<value("foodPerRation",2,1,64):depot.equipment<value("equipmentPerKit",2,1,64))continue;
            if(depot.fuel<fuel)continue;
            SupplyGraph.WeightedRoute route=SupplyGraph.cheapestPath(depot.territoryId,targetId,neighbors,friendly,edgeWeights,max*4);
            if(!route.nodes().isEmpty() && (shortest==null||route.cost()<shortest.cost())) shortest=new Route(depot,route.nodes(),territories,route.cost());
        }return shortest;
    }
    void supply(CommandSender sender,String[] a){
        if(!enabled()){msg(sender,"Logistics disabled");return;}
        if(!(sender instanceof Player p)){msg(sender,"In-game only");return;}
        Nation n=plugin.nodes().nation(p);if(n==null){msg(sender,"Join a nation first");return;}
        String operation=a.length>1?a[1].toLowerCase(Locale.ROOT):"status";
        if(!Set.of("status","route","ration","kit").contains(operation)){msg(sender,"/nextra supply <status|route|ration|kit>");return;}
        if(operation.equals("ration") && plugin.government().enabled() && !plugin.government().law(n,"STATE_RATIONS")){
            msg(sender,"Your government has disabled state rations");return;
        }
        boolean food=!operation.equals("kit");Route route=bestRoute(p,n,food);
        if(route==null){msg(sender,"No stocked, unblocked supply depot has a connected friendly route to your location");return;}
        if(operation.equals("status")||operation.equals("route")){
            msg(sender,"Supplied by "+route.depot.name+" over "+(route.territories.size()-1)+" territory hops (route cost "+route.cost()+")");
            if(operation.equals("route")){
                List<String> names=new ArrayList<>();for(int id:route.territories){Territory t=route.lookup.get(id); names.add(t==null?Integer.toString(id):t.getName());}
                msg(sender,String.join(" §7→ §f",names));
            }return;
        }
        String cooldownKey=p.getUniqueId()+":"+operation;
        long now=System.currentTimeMillis();long until=plugin.state().cooldowns.getOrDefault(cooldownKey,0L);
        if(now<until){msg(sender,"Available again in "+((until-now+999)/1000)+" seconds");return;}
        int fuel=value("fuelPerDelivery",1,0,500);
        if(operation.equals("ration")){
            int cost=value("foodPerRation",2,1,64);
            if(route.depot.food<cost||route.depot.fuel<fuel){msg(sender,"Depot ran out of supplies");return;}
            int restored=value("rationFoodRestore",6,1,20);
            if(p.getFoodLevel()>=20){msg(sender,"You're already full");return;}
            p.setFoodLevel(Math.min(20,p.getFoodLevel()+restored));
            route.depot.food-=cost;route.depot.fuel-=fuel;
            msg(sender,"Received field rations from "+route.depot.name);
        }else {
            int cost=value("equipmentPerKit",2,1,64);
            int arrows=value("kitArrows",8,1,64);
            if(plugin.government().enabled()&&plugin.government().law(n,"MILITARY_PRIORITY"))arrows=Math.min(64,arrows+4);
            if(route.depot.equipment<cost||route.depot.fuel<fuel){msg(sender,"Depot ran out of supplies");return;}
            if(!hasSpace(p,Material.ARROW,arrows)){msg(sender,"Free inventory space first");return;}
            if(!p.getInventory().addItem(new ItemStack(Material.ARROW,arrows)).isEmpty()){
                msg(sender,"Inventory full; kit delivery failed");return;
            }
            route.depot.equipment-=cost;route.depot.fuel-=fuel;
            msg(sender,"Received "+arrows+" arrows from "+route.depot.name);
        }
        plugin.state().cooldowns.put(cooldownKey, now+value("deliveryCooldownSeconds",180,1,86400)*1000L);
    }
    void sabotage(CommandSender sender,String[] a){
        if(!enabled()||!plugin.getConfig().getBoolean("logistics.sabotage.enabled",true)){msg(sender,"Sabotage disabled");return;}
        if(!(sender instanceof Player p)){msg(sender,"In-game only");return;}
        if(a.length!=2){msg(sender,"/nextra sabotage <enemy-depot-name>");return;}
        Nation attacker=plugin.nodes().nation(p);if(attacker==null){msg(sender,"Join a nation");return;}
        List<Models.Depot> matches=new ArrayList<>();
        for(Models.Depot d:plugin.state().depots.values())if(d.name.equalsIgnoreCase(a[1])&&!d.nationId.equals(attacker.getUuid().toString()))matches.add(d);
        if(matches.size()!=1){msg(sender,"Unknown or ambiguous enemy depot name");return;}
        Models.Depot depot=matches.get(0);Nation defender=plugin.nodes().nationById(depot.nationId);
        if(defender==null||!validDepot(depot)){msg(sender,"Depot inactive");return;}
        if(plugin.getConfig().getBoolean("logistics.sabotage.requireDeclaredEnemies",true)&&!attacker.getEnemies().contains(defender)){
            msg(sender,"Your nation must be an enemy of the depot's nation");return;}
        if(!p.getWorld().getName().equals(depot.world)){msg(sender,"Wrong world");return;}
        int range=value("sabotage.rangeBlocks",48,1,128);
        double dx=p.getLocation().getX()-depot.x,dz=p.getLocation().getZ()-depot.z;
        if(dx*dx+dz*dz>range*range){msg(sender,"Get within "+range+" blocks of the depot");return;}
        long now=System.currentTimeMillis();String user=p.getUniqueId().toString();
        long until=plugin.state().sabotageCooldowns.getOrDefault(user,0L);
        if(now<until){msg(sender,"Sabotage cooldown: "+((until-now+999)/1000)+" seconds");return;}
        if(blocked(depot)){msg(sender,"Depot is already blockaded");return;}
        depot.blockadedUntil=now+value("sabotage.durationSeconds",120,1,86400)*1000L;
        depot.blockadedBy=attacker.getUuid().toString();
        plugin.state().sabotageCooldowns.put(user,now+value("sabotage.cooldownSeconds",1200,1,86400)*1000L);
        msg(sender,"You blockaded "+depot.name+" for "+value("sabotage.durationSeconds",120,1,86400)+" seconds");
    }
}
