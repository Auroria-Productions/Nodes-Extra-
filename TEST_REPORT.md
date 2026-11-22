# Tests — Nodes-Extra v1.0.0

- `SupplyGraphTest`: traversal on friendly owned territories, hop caps, disconnected or hostile regions, weighted railway/sea routes, same-territory supply.
- `ElectionTest`: election majority, deterministic ties, invalid candidate ballots, empty candidate list.
- `SerializationTest`: Gson round-trip for nation government, votes, depot inventories, and state schema.
- `ApiCompatibilityTest`: reflection on the current Nodes Territory Kotlin value-class getters and Bukkit plugin inheritance. Core integration method calls are compile-checked.
- Release JAR archive integrity and `plugin.yml` metadata.

**Not covered:** live Paper startup, Vault server plugin wiring, actual PvP/sabotage gameplay, territorial changes mid-save, and the map viewer UI. Test first on a staging server.

## v1.1.0 command bridge
- All 5 standalone tests passed, including `NodesCommandBridgeRoutingTest` for preservation of core commands and dispatch of extra commands.
- Paper API compilation and ZIP integrity passed.
- A live Paper server was not available: actual chat dispatch and tab completion still need an in-server smoke test.
