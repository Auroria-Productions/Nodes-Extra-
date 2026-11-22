package com.auroria.nodesextra;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import phonon.nodes.objects.Nation;
import phonon.nodes.objects.Resident;
import java.time.Instant;
import java.util.*;

final class Government {
    static final Set<String> TYPES = Set.of("MONARCHY", "REPUBLIC", "EMPIRE", "DICTATORSHIP", "COUNCIL");
    static final Set<String> OFFICES = Set.of("CHANCELLOR","TREASURER","MARSHAL","DIPLOMAT","GOVERNOR");
    static final Set<String> LAWS = Set.of("OPEN_BORDERS","STATE_RATIONS","MILITARY_PRIORITY");
    private final NodesExtraPlugin plugin;
    Government(NodesExtraPlugin p) {plugin=p;}
    boolean enabled() {return plugin.getConfig().getBoolean("government.enabled",true);}
    Models.Government record(Nation n) {
        return plugin.state().governments.computeIfAbsent(n.getUuid().toString(),k->{
            Models.Government g = new Models.Government();
            String type = plugin.getConfig().getString("government.defaultType","MONARCHY").toUpperCase(Locale.ROOT);
            g.type = TYPES.contains(type)?type:"MONARCHY";
            g.laws.put("STATE_RATIONS",true);
            g.laws.put("OPEN_BORDERS",false);
            g.laws.put("MILITARY_PRIORITY",false);
            return g;
        });
    }
    boolean law(Nation n,String law) {return record(n).laws.getOrDefault(law,false);}
    boolean isHead(Player p,Nation n) {return p.getUniqueId().toString().equals(record(n).head);}
    boolean isOfficer(Player p,Nation n,String office) {return p.getUniqueId().toString().equals(record(n).offices.get(office));}
    boolean isExecutive(Player p,Nation n) {return p.hasPermission("nodesextra.admin") || plugin.nodes().isSovereign(p,n) || isHead(p,n);}
    boolean isMarshal(Player p,Nation n) {return isExecutive(p,n)||isOfficer(p,n,"MARSHAL");}
    void message(CommandSender s,String msg) {s.sendMessage("§6[Nodes-Extra Government] §f"+msg);}
    void history(Models.Government g,String msg) {
        g.history.add(Instant.now().toString()+" "+msg);
        if (g.history.size()>60) g.history.remove(0);
    }
    void handle(CommandSender sender,String[] a) {
        if (!enabled()) {message(sender,"Government system is disabled in config.yml");return;}
        if (a.length>=2 && a[1].equalsIgnoreCase("inspect") && sender.hasPermission("nodesextra.admin")) {
            if (a.length<3) {message(sender,"Usage: /nextra gov inspect <nation>");return;}
            Nation n=plugin.nodes().nationByName(String.join(" ",Arrays.copyOfRange(a,2,a.length)));
            if(n==null) {message(sender,"Unknown nation");return;}
            info(sender,n);return;
        }
        if(!(sender instanceof Player p)) {message(sender,"Use /nextra gov inspect <nation> from console");return;}
        Nation n=plugin.nodes().nation(p);
        if (n==null) {message(sender,"Join a Nodes nation first");return;}
        Models.Government g=record(n);
        if(a.length<2 || a[1].equalsIgnoreCase("info")) {info(sender,n);return;}
        String cmd=a[1].toLowerCase(Locale.ROOT);
        switch(cmd) {
            case "type" -> {
                if(a.length!=3) {message(sender,"/nextra gov type <monarchy|republic|empire|dictatorship|council>");return;}
                if(!p.hasPermission("nodesextra.admin")&&!plugin.nodes().isSovereign(p,n)) {message(sender,"Only the nation capital's mayor can change the government type");return;}
                String type=a[2].toUpperCase(Locale.ROOT);if(!TYPES.contains(type)){message(sender,"Invalid type");return;}
                g.type=type; if(!type.equals("REPUBLIC")&&!type.equals("COUNCIL")) g.election=null;
                history(g,p.getName()+" changed government to "+type);
                message(sender,"Government type updated to "+type);
            }
            case "appoint" -> {
                if(a.length!=4){message(sender,"/nextra gov appoint <office> <citizen>");return;}
                if(!isExecutive(p,n)){message(sender,"Requires sovereign or head-of-government authority");return;}
                String office=a[2].toUpperCase(Locale.ROOT);
                if(!OFFICES.contains(office)){message(sender,"Offices: "+String.join(", ",OFFICES));return;}
                Resident r=plugin.nodes().citizenByName(n,a[3]);if(r==null){message(sender,"Citizen not in this nation");return;}
                g.offices.put(office,r.getUuid().toString());history(g,p.getName()+" appointed "+r.getName()+" as "+office);
                message(sender,r.getName()+" appointed as "+office);
            }
            case "dismiss" -> {
                if(a.length!=3){message(sender,"/nextra gov dismiss <office>");return;}
                if(!isExecutive(p,n)){message(sender,"Requires executive authority");return;}
                String office=a[2].toUpperCase(Locale.ROOT);
                if(g.offices.remove(office)!=null){history(g,p.getName()+" dismissed "+office);message(sender,"Office dismissed");}
                else message(sender,"Office not filled");
            }
            case "successor" -> {
                if(a.length!=3){message(sender,"/nextra gov successor <citizen>");return;}
                if(!isExecutive(p,n)||!plugin.getConfig().getBoolean("government.enableSuccession",true)){message(sender,"No succession authority");return;}
                Resident r=plugin.nodes().citizenByName(n,a[2]);if(r==null){message(sender,"Citizen not in nation");return;}
                g.successor=r.getUuid().toString(); history(g,p.getName()+" named successor "+r.getName());message(sender,"Successor recorded: "+r.getName());
            }
            case "succession" -> {
                if(!plugin.getConfig().getBoolean("government.enableSuccession",true)||(!p.hasPermission("nodesextra.admin")&&!plugin.nodes().isSovereign(p,n))){message(sender,"Only the sovereign can confirm succession");return;}
                if(g.successor==null||!plugin.nodes().isCitizenId(g.successor,n)){message(sender,"No valid named successor");return;}
                g.head=g.successor;g.successor=null;history(g,p.getName()+" confirmed succession to "+plugin.nodes().citizenName(g.head,n));
                message(sender,"Government succession completed (Nodes capital mayor unchanged)");
            }
            case "history" -> {
                if(g.history.isEmpty()){message(sender,"No recorded government changes");return;}
                for(String event:g.history.subList(Math.max(0,g.history.size()-8),g.history.size())) message(sender,event);
            }
            default -> message(sender,"/nextra gov [info|type|appoint|dismiss|successor|succession|history]");
        }
    }
    void info(CommandSender sender,Nation n) {
        Models.Government g=record(n);
        message(sender,"§e"+n.getName()+"§f — "+g.type);
        message(sender,"Head: "+(g.head==null?"Not appointed":plugin.nodes().citizenName(g.head,n))
                +" | Sovereign: "+(n.getCapital()!=null&&n.getCapital().getLeader()!=null?n.getCapital().getLeader().getName():"Unknown"));
        for(var e:g.offices.entrySet()) message(sender,e.getKey()+": "+plugin.nodes().citizenName(e.getValue(),n));
        message(sender,"Laws: "+g.laws);
        if(g.election!=null)message(sender,"Election: "+g.election.candidates.size()+" candidates, "+g.election.ballots.size()+" votes, closes "+Instant.ofEpochMilli(g.election.closesAt));
    }
    void laws(CommandSender sender,String[] a) {
        if(!enabled()){message(sender,"Government disabled");return;}
        if(!(sender instanceof Player p)){message(sender,"In-game only");return;}
        Nation n=plugin.nodes().nation(p); if(n==null){message(sender,"Join a nation");return;}
        Models.Government g=record(n);
        if(a.length==1){message(sender,"Current laws: "+g.laws+". /nextra law <open_borders|state_rations|military_priority> <on|off>");return;}
        if(a.length!=3||!LAWS.contains(a[1].toUpperCase(Locale.ROOT))||!(a[2].equalsIgnoreCase("on")||a[2].equalsIgnoreCase("off"))) {
            message(sender,"/nextra law <open_borders|state_rations|military_priority> <on|off>");return;
        }
        if(!isExecutive(p,n)){message(sender,"Requires executive authority");return;}
        String law=a[1].toUpperCase(Locale.ROOT);boolean on=a[2].equalsIgnoreCase("on");g.laws.put(law,on);
        history(g,p.getName()+" set "+law+" to "+on);message(sender,law+" = "+on);
    }
    void election(CommandSender sender,String[] a) {
        if(!enabled()){message(sender,"Government disabled");return;}
        if(!(sender instanceof Player p)){message(sender,"In-game only");return;}
        Nation n=plugin.nodes().nation(p);if(n==null){message(sender,"Join a nation");return;}
        Models.Government g=record(n);String cmd=a.length>1?a[1].toLowerCase(Locale.ROOT):"status";
        switch(cmd){
            case "start" -> {
                if(!isExecutive(p,n)) {message(sender,"Requires executive authority");return;}
                if(!(g.type.equals("REPUBLIC")||g.type.equals("COUNCIL"))){message(sender,"Elections only apply to REPUBLIC and COUNCIL governments");return;}
                if(g.election!=null){message(sender,"An election is already active");return;}
                if(a.length!=3){message(sender,"/nextra election start <minutes>");return;}
                long minutes;try{minutes=Long.parseLong(a[2]);}catch(NumberFormatException ex){message(sender,"Invalid minutes");return;}
                long min=plugin.getConfig().getLong("government.minElectionMinutes",5),max=plugin.getConfig().getLong("government.maxElectionMinutes",10080);
                if(minutes<min||minutes>max){message(sender,"Duration must be "+min+"–"+max+" minutes");return;}
                g.election=new Models.Election();g.election.openedAt=System.currentTimeMillis();g.election.closesAt=g.election.openedAt+minutes*60_000L;
                history(g,p.getName()+" opened election");message(sender,"Election started; citizen nominations are open for "+minutes+" minutes");
            }
            case "nominate" -> {
                if(g.election==null||g.election.closesAt<=System.currentTimeMillis()){message(sender,"No active election");return;}
                if(a.length!=2){message(sender,"/nextra election nominate (nominates yourself)");return;}
                g.election.candidates.add(p.getUniqueId().toString());message(sender,"You are now a candidate");
            }
            case "vote" -> {
                if(g.election==null||g.election.closesAt<=System.currentTimeMillis()){message(sender,"No active election");return;}
                if(a.length!=3){message(sender,"/nextra election vote <candidate>");return;}
                Resident candidate=plugin.nodes().citizenByName(n,a[2]);
                if(candidate==null||!g.election.candidates.contains(candidate.getUuid().toString())){message(sender,"Not a nominated candidate");return;}
                String voter=p.getUniqueId().toString();if(g.election.ballots.containsKey(voter)){message(sender,"You already voted in this election");return;}
                g.election.ballots.put(voter,candidate.getUuid().toString());message(sender,"Your vote has been recorded");
            }
            case "end" -> {
                if(!isExecutive(p,n)){message(sender,"Requires executive authority");return;}
                if(g.election==null){message(sender,"No active election");return;}
                finish(n,g,true);
                message(sender,"Election closed");
            }
            case "status" -> {
                if(g.election==null){message(sender,"No election running");return;}
                message(sender,"Candidates: "+g.election.candidates.stream().map(x->plugin.nodes().citizenName(x,n)).toList());
                message(sender,"Votes cast: "+g.election.ballots.size()+". Closes "+Instant.ofEpochMilli(g.election.closesAt));
            }
            default -> message(sender,"/nextra election [start <minutes>|nominate|vote <candidate>|status|end]");
        }
    }
    void finishExpiredElections(){
        if(!enabled())return;
        for(var e:plugin.state().governments.entrySet()){
            Models.Government g=e.getValue();Nation n=plugin.nodes().nationById(e.getKey());
            if(n!=null&&g.election!=null&&g.election.closesAt<=System.currentTimeMillis()) finish(n,g,false);
        }
    }
    void finish(Nation n,Models.Government g,boolean early){
        Models.Election e=g.election;if(e==null)return;
        // Remove candidates/voters who left the nation before counting.
        e.candidates.removeIf(x->!plugin.nodes().isCitizenId(x,n));
        e.ballots.entrySet().removeIf(x->!plugin.nodes().isCitizenId(x.getKey(),n));
        int minimum=Math.max(1,plugin.getConfig().getInt("government.minVoters",1));
        if(e.ballots.size()>=minimum && !e.candidates.isEmpty()){
            String winner=ElectionMath.winner(e.candidates,e.ballots);
            g.head=winner;
            history(g,"Election "+(early?"ended early":"concluded")+": "+plugin.nodes().citizenName(winner,n)+" ("+e.ballots.size()+" voters)");
            Bukkit.broadcastMessage("§6[Nodes-Extra] §e"+n.getName()+"§f elected "+plugin.nodes().citizenName(winner,n)+" as head of government");
        }else history(g,"Election ended without a winner (insufficient votes/candidates)");
        g.election=null;
    }
}
