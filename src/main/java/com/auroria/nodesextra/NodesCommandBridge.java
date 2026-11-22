package com.auroria.nodesextra;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Extend the /nodes command owned by milzzynodes without replacing its commands.
 * It is important NOT to declare another `nodes:` in Nodes-Extra's plugin.yml:
 * Paper command collisions make that approach unpredictable.
 */
final class NodesCommandBridge implements CommandExecutor, TabCompleter {
    private static final Set<String> DIRECT_SUBCOMMANDS = Set.of(
        "depot", "depots", "supply", "supplies", "infra", "infrastructure",
        "sabotage", "blockade", "gov", "law", "laws", "election"
    );
    private static final List<String> ROOT_COMPLETIONS = List.of(
        "extra", "logistics", "government", "depot", "supply", "infra",
        "sabotage", "gov", "law", "election"
    );
    private final ExtraCommands extras;
    private final PluginCommand command;
    private final CommandExecutor previousExecutor;
    private final TabCompleter previousTabCompleter;

    private NodesCommandBridge(NodesExtraPlugin plugin, ExtraCommands extras, PluginCommand command) {
        this.extras = extras;
        this.command = command;
        this.previousExecutor = command.getExecutor();
        this.previousTabCompleter = command.getTabCompleter();
    }

    static NodesCommandBridge install(NodesExtraPlugin plugin, ExtraCommands extras) {
        PluginCommand command = Bukkit.getPluginCommand("nodes");
        Plugin core = Bukkit.getPluginManager().getPlugin("milzzynodes");
        if (command == null || command.getPlugin() != core) {
            plugin.getLogger().warning("Cannot extend /nodes: the command is not owned by milzzynodes. /nextra still works.");
            return null;
        }
        NodesCommandBridge bridge = new NodesCommandBridge(plugin, extras, command);
        command.setExecutor(bridge);
        command.setTabCompleter(bridge);
        plugin.getLogger().info("Connected /nodes extra, /nodes logistics, /nodes government and direct subcommands to Nodes-Extra");
        return bridge;
    }

    void uninstall() {
        // Restore exactly the handlers we replaced, but never overwrite another plugin's later changes.
        if (command.getExecutor() == this) command.setExecutor(previousExecutor);
        if (command.getTabCompleter() == this) command.setTabCompleter(previousTabCompleter);
    }

    /** Pure routing for unit tests; null means let MilzzyNodes handle it. */
    static String[] route(String[] args) {
        if (args.length == 0) return null;
        String verb = args[0].toLowerCase(Locale.ROOT);
        if (verb.equals("extra") || verb.equals("nodesextra")) {
            return args.length == 1 ? new String[] {"help"} : Arrays.copyOfRange(args, 1, args.length);
        }
        if (verb.equals("logistics")) {
            // /nodes logistics depot add ... ; /nodes logistics -> show add-on help
            return args.length == 1 ? new String[] {"help"} : Arrays.copyOfRange(args, 1, args.length);
        }
        if (verb.equals("government")) {
            String[] converted = new String[args.length];
            converted[0] = "gov";
            System.arraycopy(args, 1, converted, 1, args.length - 1);
            return converted;
        }
        if (DIRECT_SUBCOMMANDS.contains(verb)) return args;
        return null;
    }

    @Override public boolean onCommand(CommandSender sender, Command ignored, String label, String[] args) {
        String[] extraArgs = route(args);
        if (extraArgs == null) {
            boolean result = previousExecutor.onCommand(sender, command, label, args);
            // /nodes and /nodes help still show the usual core information.
            if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
                sender.sendMessage("§6[Nodes-Extra] §fUse §e/nodes extra§f for logistics and government commands.");
                sender.sendMessage("§7Shortcuts: /nodes depot, /nodes supply, /nodes infra, /nodes gov, /nodes election");
            }
            return result;
        }
        if (!sender.hasPermission("nodesextra.use")) {
            sender.sendMessage("§cYou do not have permission to use Nodes-Extra.");
            return true;
        }
        // Delegate to the original add-on command handler; all role and admin checks remain intact.
        return extras.onCommand(sender, command, "nextra", extraArgs);
    }

    @Override public List<String> onTabComplete(CommandSender sender, Command ignored, String alias, String[] args) {
        // First argument completion always includes both the core and add-on commands,
        // even if the typed word exactly matches an add-on namespace.
        if (args.length == 1) {
            List<String> original = previousTabCompleter == null
                ? List.of()
                : previousTabCompleter.onTabComplete(sender, command, alias, args);
            if (!sender.hasPermission("nodesextra.use")) return original == null ? List.of() : original;
            LinkedHashSet<String> suggestions = new LinkedHashSet<>();
            if (original != null) suggestions.addAll(original);
            String prefix = args[0].toLowerCase(Locale.ROOT);
            for (String name : ROOT_COMPLETIONS) if (name.startsWith(prefix)) suggestions.add(name);
            return new ArrayList<>(suggestions);
        }
        String[] extraArgs = route(args);
        if (extraArgs != null) {
            if (!sender.hasPermission("nodesextra.use")) return List.of();
            List<String> result = extras.onTabComplete(sender, command, "nextra", extraArgs);
            return result == null ? List.of() : result;
        }
        List<String> original = previousTabCompleter == null
            ? List.of()
            : previousTabCompleter.onTabComplete(sender, command, alias, args);
        return original == null ? List.of() : original;
    }
}
