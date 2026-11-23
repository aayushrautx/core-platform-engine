plugins {
    base
}

allprojects {
    group = "com.coreplatform"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.register("prepareKotlinBuildScriptModel")

    pluginManager.withPlugin("java") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile> {
            options.compilerArgs.add("-parameters")
        }

        tasks.withType<Test> {
            useJUnitPlatform()
            environment("CORE_PLATFORM_DB_PASSWORD", System.getenv("CORE_PLATFORM_DB_PASSWORD") ?: "")
        }
    }
}
