package com.auroria.nodesextra;

import org.bukkit.command.*;
import java.util.*;

final class ExtraCommands implements CommandExecutor, TabCompleter {
    private final NodesExtraPlugin plugin;
    ExtraCommands(NodesExtraPlugin plugin){this.plugin=plugin;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] original) {
        String[] args=original;
        // Friendly aliases: /logistics depot ..., /government type ...
        if(label.equalsIgnoreCase("government")&&(args.length==0 || !Set.of("gov","election","law").contains(args[0].toLowerCase(Locale.ROOT))))args=prepend("gov",args);
        if(args.length==0||args[0].equalsIgnoreCase("help")) {help(sender);return true;}
        String cmd=args[0].toLowerCase(Locale.ROOT);
        try {
            switch(cmd){
                case "status" -> {
                    sender.sendMessage("§6[Nodes-Extra] §fLogistics: "+(plugin.logistics().enabled()?"enabled":"disabled")+" | Government: "+(plugin.government().enabled()?"enabled":"disabled"));
                    sender.sendMessage("§7Depots: "+plugin.state().depots.size()+" | Governments: "+plugin.state().governments.size());
                }
                case "gov", "government" -> plugin.government().handle(sender,args);
                case "law", "laws" -> plugin.government().laws(sender,args);
                case "election" -> plugin.government().election(sender,args);
                case "infra", "infrastructure" -> plugin.infrastructure().command(sender,args);
                case "depot", "depots" -> plugin.logistics().depots(sender,args);
                case "supply", "supplies" -> plugin.logistics().supply(sender,args);
                case "sabotage", "blockade" -> plugin.logistics().sabotage(sender,args);
                case "reload" -> {
                    if(!sender.hasPermission("nodesextra.admin")){sender.sendMessage("§cAdmin permission required");return true;}
                    plugin.loadSettings();sender.sendMessage("§aNodes-Extra settings reloaded");
                }
                case "save" -> {
                    if(!sender.hasPermission("nodesextra.admin")){sender.sendMessage("§cAdmin permission required");return true;}
                    plugin.saveState();sender.sendMessage("§aNodes-Extra state and public overlay exported");
                }
                default -> help(sender);
            }
        }catch(Exception ex){
            sender.sendMessage("§c[Nodes-Extra] An operation failed; see server console");
            plugin.getLogger().log(java.util.logging.Level.SEVERE,"/nextra command failed",ex);
        }
        return true;
    }
    static String[] prepend(String first,String[] args){String[] result=new String[args.length+1];result[0]=first;System.arraycopy(args,0,result,1,args.length);return result;}
    private void help(CommandSender sender){
        sender.sendMessage("§6──── Nodes-Extra 1.1.0 ────");
        sender.sendMessage("§7Also available as §e/nodes extra§7, with §e/nodes depot§7, §e/nodes gov§7, etc.");
        sender.sendMessage("§e/nextra status §7— enabled systems and counts");
        sender.sendMessage("§e/nextra depot [list|add|info|remove|deposit|withdraw]");
        sender.sendMessage("§e/nextra supply [status|route|ration|kit]");
        sender.sendMessage("§e/nextra infra [list|near|build|remove] §7— road, rail, sea networks");
        sender.sendMessage("§e/nextra sabotage <depot> §7— blockade enemy supply");
        sender.sendMessage("§e/nextra gov [info|type|appoint|dismiss|successor|succession|history]");
        sender.sendMessage("§e/nextra law [open_borders|state_rations|military_priority] <on|off>");
        sender.sendMessage("§e/nextra election [start|nominate|vote|status|end]");
        if(sender.hasPermission("nodesextra.admin"))sender.sendMessage("§e/nextra [save|reload] §7— admin only");
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] a) {
        if(a.length==1) return starts(List.of("help","status","depot","infra","supply","sabotage","gov","law","election","reload","save"),a[0]);
        if(a.length==2){
            return switch(a[0].toLowerCase(Locale.ROOT)){
                case "infra"->starts(List.of("list","near","build","remove"),a[1]);
                case "depot"->starts(List.of("list","add","info","remove","deposit","withdraw"),a[1]);
                case "supply"->starts(List.of("status","route","ration","kit"),a[1]);
                case "gov"->starts(List.of("info","type","appoint","dismiss","successor","succession","history"),a[1]);
                case "election"->starts(List.of("start","nominate","vote","status","end"),a[1]);
                case "law"->starts(List.of("open_borders","state_rations","military_priority"),a[1]);
                default->List.of();
            };
        }
        if(a.length==3){
            if(a[0].equalsIgnoreCase("gov")&&a[1].equalsIgnoreCase("type"))return starts(List.of("monarchy","republic","empire","dictatorship","council"),a[2]);
            if(a[0].equalsIgnoreCase("gov")&&a[1].equalsIgnoreCase("appoint"))return starts(List.of("chancellor","treasurer","marshal","diplomat","governor"),a[2]);
            if(a[0].equalsIgnoreCase("infra")&&a[1].equalsIgnoreCase("build"))return starts(List.of("road","rail","sea"),a[2]);
            if(a[0].equalsIgnoreCase("law"))return starts(List.of("on","off"),a[2]);
        }
        if(a.length==4&&a[0].equalsIgnoreCase("depot")&&(a[1].equalsIgnoreCase("deposit")||a[1].equalsIgnoreCase("withdraw")))return starts(List.of("food","equipment","fuel"),a[3]);
        return List.of();
    }
    private static List<String> starts(List<String> candidates,String prefix){return candidates.stream().filter(s->s.startsWith(prefix.toLowerCase(Locale.ROOT))).toList();}
}
