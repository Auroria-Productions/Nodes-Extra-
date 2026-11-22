# Changelog

## 1.1.0 — Shared `/nodes` command
- Routes `/nodes extra`, `/nodes logistics`, `/nodes government` and direct logistics/government subcommands to Nodes-Extra.
- Keeps the original MilzzyNodes `/nodes` command, help, existing subcommands and `/nd` alias.
- Adds combined tab completion and restores the original executor and completer during shutdown.
- Keeps `/nextra` and the existing permissions working.
- Adds command-routing regression tests.

# Nodes-Extra 1.0.0 — 21 November 2026

- Introduced a single Paper add-on containing Logistics and Government modules.
- Territory graph supply and weighted routes, depots, food/fuel/equipment inventory and provision cooldowns.
- Abstract road, rail and native Nodes port-group sea routes, with material build costs and ownership checks.
- Enemy blockade/sabotage actions with range, war relations and cooldown controls.
- Five configurable government types, five cabinet offices, three logistics-linked national laws.
- Republic/Council elections with eligible citizen nominees, one vote per citizen, automated close and deterministic tie-breaking.
- Explicit successor nomination and sovereign-controlled handover of the add-on government office.
- JSON persistence and separate safe public map feed; no changes to Nodes or NodesMap JARs.
- Compiled against Java 21 and the supplied MilzzyNodes dev.5.11 core; pure model, graph, elections, serialisation and API-adapter tests.
