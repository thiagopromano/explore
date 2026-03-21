plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform")
}

group = "com.thiromano.exact"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        rubymine("2025.3.3")
        bundledPlugin("org.jetbrains.plugins.textmate")
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "com.thiromano.exact"
        name = "Exact: Elixir Support"
        version = "0.1.0"
        ideaVersion {
            sinceBuild = "253"
        }
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    prepareSandbox {
        from("src/main/resources/textmate") {
            into("${project.name}/textmate")
        }
    }

    runIde {
        // Enable DEBUG logging for LSP communication and plugin internals
        systemProperty("idea.log.debug.categories", "#com.intellij.platform.lsp,#com.thiromano.exact")
    }
}
