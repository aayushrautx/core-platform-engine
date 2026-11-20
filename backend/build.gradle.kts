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
}
