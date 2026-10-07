plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.0" apply false
}

stonecutter active "1.21.1"

tasks.register("build") {
    group = "build"
    description = "Builds KVXStorage for every registered Minecraft version."
    dependsOn(stonecutter.tasks.named("build").map { it.values })
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds every Minecraft version from the shared sources."
    dependsOn("build")
}

tasks.register<Delete>("clean") {
    group = "build"
    delete(layout.buildDirectory)
    dependsOn(stonecutter.tasks.named("clean").map { it.values })
}

tasks.register("check") {
    group = "verification"
    dependsOn(stonecutter.tasks.named("check").map { it.values })
}
