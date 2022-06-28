plugins {
    id("java")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    id("org.web3j") version "4.9.2"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("io.papermc.paperweight.userdev") version "1.3.6"
}

group = "net.diamondverse.craftcoin"
version = "1.0.0-SNAPSHOT"

repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    paperDevBundle("1.18.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.1")
    compileOnly("com.github.CraftToken:WalletConnectMC:main-SNAPSHOT")
    compileOnly("com.github.4drian3d:MiniPlaceholders:1.1.1")
}

bukkit {
    main = "net.diamondverse.craftcoin.CraftCoin"
    apiVersion = "1.18"
    authors = listOf("Sliman4")
    depend = listOf("WalletConnectMC")
    softDepend = listOf("PlaceholderAPI", "MiniPlaceholders")
    permissions {
        register("craftcoin.transfer") {
            description = "/transfer"
            default = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default.TRUE
        }
    }
}

web3j {
    includedContracts = listOf("IERC20")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
    
    shadowJar {
        archiveClassifier.set("")
        relocate("org.web3j", "net.diamondverse.craftcoin") {
            include("org.web3j.ierc20.*")
        }
    }
}

artifacts {
    archives(tasks.shadowJar)
}

configurations.runtimeClasspath {
    // added as implementation by web3j plugin, but should be compileOnly
    exclude(group = "org.web3j", module = "core")
}
