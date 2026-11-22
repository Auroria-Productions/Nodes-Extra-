# Nodes-Extra 1.1.0

**Nodes-Extra** is a single Paper add-on for **nodes** that combines two configurable gameplay systems:

1. **NodesLogistics:** territorial supply lines, depots, roads, rail routes, native-port sea lanes, provisioning and enemy sabotage/blockades.
2. **NodesGovernment:** political systems, ministerial offices, laws, citizen elections and manually confirmed succession.

This plugin **does not replace or modify `milzzynodes.jar` or `milzzynodesmap.jar`**. It uses the Nodes nation, town, resident, territory and port APIs directly. It is deliberately isolated in its own data folder.

## Requirements

- Paper 1.21.x / Java 21 (compiled with `--release 21`)
- Installed, functioning `milzzynodes.jar` (tested against the supplied 2026-11-21 dev.5.11 build)
- Any required dependencies of your existing MilzzyNodes server (including **Vault**) already installed
- Optional MilzzyNodesMap (export file supported, but **the existing map viewer is not yet modified** to display these overlays)

## Installation

1. Stop your server and back up `plugins/milzzynodes/` and `plugins/milzzynodesmap/`.
2. Copy **`Nodes-Extra.jar`** into your Paper `plugins/` directory.
3. Keep your current `milzzynodes.jar` installed; it is a dependency.
4. Start the server. The plugin creates `plugins/Nodes-Extra/config.yml`.
5. Verify with `/plugins`, `/nodes extra` or `/nodes depot list`. The original `/nextra` is also available.
6. Change the configuration as needed; `/nextra reload` only reloads settings, without touching stored state.

The add-on does **not** require an online admin to display government or supply information. Mutating infrastructure or government state requires appropriate in-game authorities; administrative actions require `nodesextra.admin`.

## Shared `/nodes` command (v1.1.0)

Nodes-Extra hooks the **existing** `/nodes` command owned by the `milzzynodes` core. It does not register a second `/nodes` command or replace the core JAR.

```text
/nodes                  # original MilzzyNodes status, plus Nodes-Extra hint
/nodes help             # original MilzzyNodes help, plus Nodes-Extra hint
/nodes extra            # Nodes-Extra help
/nodes extra status     # complete Nodes-Extra status
/nodes logistics        # Nodes-Extra help
/nodes logistics depot add <name>
/nodes government info
/nodes government type <type>
/nodes depot list
/nodes depot deposit <name> <food|equipment|fuel> <amount>
/nodes supply route
/nodes infra build road <territory-id>
/nodes sabotage <depot-name>
/nodes gov appoint <office> <citizen>
/nodes law
/nodes election vote <candidate>
```

All the original core namespaces, including `/nodes town`, `/nodes nation`, `/nodes territory`, `/nodes player`, `/nodes resource`, and `/nodes war`, are passed through without modification. Existing `/nextra` and its aliases still work. Tab completion combines the original and additional commands. `/nd` (the core alias of `/nodes`) uses the same bridge.

A core version where another plugin owns `/nodes` will **not** be hooked; the add-on logs a warning and `/nextra` remains usable. Existing permission rules remain in force: the sender must be permitted to use the core `/nodes` command and the Nodes-Extra command, plus any action-specific permissions.

**Deployment:** replace only `plugins/Nodes-Extra.jar` and restart Paper. Do not put the bundled `lib/milzzynodes.jar` into `plugins/`; it is only a compile-time dependency.

## Logistics commands

| Command | What it does |
|---|---|
| `/nextra depot add <name>` | Register a depot in the player's nation-owned territory; requires town officer/leader/general, appointed marshal or admin |
| `/nextra depot list` | Show all depots belonging to your nation |
| `/nextra depot info <name>` | Display depot inventory and blockade state |
| `/nextra depot deposit <name> <food\|equipment\|fuel> <amount>` | Deposit **actual inventory items** within 16 blocks of the depot |
| `/nextra depot withdraw <name> <food\|equipment\|fuel> <amount>` | Officers recover materials at an owned depot |
| `/nextra depot remove <name>` | Dismantle an **empty** depot while physically nearby |
| `/nextra supply status` | Find an operational and stocked route to your current territory |
| `/nextra supply route` | Display the connected Nodes territory path |
| `/nextra supply ration` | Consume food and fuel at a depot to restore some hunger, subject to cooldown and law |
| `/nextra supply kit` | Consume equipment and fuel at a depot to issue arrows, subject to cooldown |
| `/nextra sabotage <depot-name>` | Enemy citizens near the target blockade it temporarily (with cooldown) |
| `/nextra infra near` | Show your current territory ID and neighbouring territory IDs |
| `/nextra infra build road <id>` | Build a logistical road link between adjacent owned territories |
| `/nextra infra build rail <id>` | Build a logistical rail link between adjacent owned territories |
| `/nextra infra build sea <id>` | Link two owned territories if each contains a Nodes port sharing a port group |
| `/nextra infra list` | List infrastructure links and their active status |
| `/nextra infra remove <id>` | Remove a link from the territory you're standing in |

**The infrastructure is an abstract territorial network, not placed road, track or harbour blocks.** Roads/rails are logical edges. Default path costs per territory connection: ordinary 4, road 3, railway 1, sea lane 2. The available route budget is `maxHops × 4`. Ports can connect non-adjacent territories if already joined through Nodes' own port groups. Roads and railways must link adjacent territories and cost actual inventory materials.

Supply routes traverse territories owned by the depot's nation. If `allowAlliedRoutes: true` and **both nations** enable `OPEN_BORDERS`, an allied nation's territory may be traversed. Lost territory and blockaded depots stop supplying. Rations and equipment are **issued to the player**, not automatically applied to KeyWar participants. The system does not bypass KeyWar war sickness or one-life restrictions.

## Government commands

| Command | What it does |
|---|---|
| `/nextra gov` or `/nextra gov info` | Current nation type, head, sovereign, cabinet and laws |
| `/nextra gov type <monarchy\|republic\|empire\|dictatorship\|council>` | The Nodes nation capital mayor changes the government form |
| `/nextra gov appoint <chancellor\|treasurer\|marshal\|diplomat\|governor> <citizen>` | Sovereign or head appoints a citizen |
| `/nextra gov dismiss <office>` | Removes an appointment |
| `/nextra gov successor <citizen>` | Names a successor |
| `/nextra gov succession` | Sovereign confirms succession of the **add-on** head of government |
| `/nextra gov history` | Last recorded government changes |
| `/nextra law` | View laws |
| `/nextra law <open_borders\|state_rations\|military_priority> <on\|off>` | Executive updates laws affecting logistics |
| `/nextra election start <minutes>` | Executive calls election in a Republic or Council |
| `/nextra election nominate` | Any citizen nominates themselves |
| `/nextra election vote <candidate>` | One vote per citizen per election |
| `/nextra election status` | Show nominees, turnout and closing time |
| `/nextra election end` | Executive ends election before deadline (auto-closes on periodic tick) |

**Important distinction:** the `head of government` is a **Nodes-Extra political position**, not the actual nation/capital-town leader. Elections and succession **do not mutate the Nodes core nation or town leader**, avoiding corruption of existing claims and political hierarchy. Likewise, appointees do not automatically receive Nodes core permission roles. The `MARSHAL` role can manage Nodes-Extra supply depots; other cabinet roles are currently organisational.

## Saved files and map data

- `plugins/Nodes-Extra/state.json` — durable political state, candidates/votes, depots and supplies, infrastructure and cooldowns; private server-side only
- `plugins/Nodes-Extra/public.json` — safe map integration feed with government types, public laws, depot location/availability/blockade (exact stock only when `mapExposeStock: true`) and infrastructure links; **no voter identities** or UUIDs for private citizens
- Save interval defaults to 120 seconds; saves also occur on normal plugin disable and `/nextra save`.
- Atomic temporary-file rename prevents partial writes; malformed/unrecognised state **disables the plugin rather than overwriting it**.
- Files are stored under the plugin's data folder, not the Nodes core data folder.

The public JSON is an **integration-ready feed**. MilzzyNodesMap requires a separate viewer extension change to actually render it; this release does **not** edit the map plugin.

## Controls and configuration

`plugins/Nodes-Extra/config.yml` independently enables/disables `logistics.enabled`, `logistics.infrastructure.enabled`, `logistics.sabotage.enabled` and `government.enabled`. Also configures depot capacity, item material requirements, cooldowns, route length, election duration and save interval. Review `src/main/resources/config.yml` for all defaults.

Permissions:

- `nodesextra.use` (default: everyone): read and use Nodes-Extra commands; individual actions are additionally checked against nation membership and authority
- `nodesextra.admin` (default: OP): global administrative command options and exceptional authority on supported actions

## Rebuilding

Run `bash build.sh` on a Java 21 host. The `lib/` directory includes *compile-time-only* API jars and a matching **snapshot of your MilzzyNodes core JAR**; these must **not** be copied into your server's plugins directory. The build emits `dist/Nodes-Extra.jar` and runs standalone regression checks.

For a later Nodes core build, set `NODES_JAR=/path/to/milzzynodes.jar` and rebuild. If Kotlin's generated `TerritoryId` method names change, the adapter will reject incompatible versions rather than silently routing across the wrong regions.

## Current limitations

- This version is compiled and offline-tested, **not live-server-tested**.
- No taxes, automatic revenue collection, physical road placement or automatic combat debuffs yet.
- Government elections are scoped to the add-on office and do not change actual Nodes leadership.
- The map overlay feed is present, but visual integration with the viewer is a separate change.
- Nodes-Extra only targets the MilzzyNodes core build described above; other forks may require recompilation.
