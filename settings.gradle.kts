plugins {
    // Downloads the toolchain JDK when the build host lacks it, which JitPack does for 25.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "LifeProtocol"
