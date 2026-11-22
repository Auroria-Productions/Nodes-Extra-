# Third-party API dependencies (compile-time only)

The `lib/` directory contains the previously supplied MilzzyNodes server JAR and Paper API ecosystem JARs used solely to compile Nodes-Extra offline. Nodes-Extra's own release JAR does **not** contain these dependencies, and none of them should be copied into the server `plugins/` folder as separate plugins.

For public distribution, verify and retain the individual upstream license and notice files for Paper, Kotlin, Gson, Adventure, SnakeYAML, ASM, Guava, and the MilzzyNodes core project; replace the binary snapshots with Maven/Gradle dependency resolution if publishing source publicly.
