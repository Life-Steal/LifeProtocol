import com.google.protobuf.gradle.proto

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.protobuf)
}

// JitPack coordinates: com.github.Life-Steal:LifeProtocol:<tag>
group = "com.github.Life-Steal"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // api, not implementation: consumers handle the generated messages and stubs directly.
    api(libs.protobuf.java)
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)

    // javax.annotation.Generated, referenced by the generated gRPC stubs but not needed at runtime.
    compileOnly(libs.annotations.api)
}

sourceSets {
    main {
        proto {
            // The .proto files are the source of this project, so they sit at the top level
            // instead of the src/main/proto default.
            // The plugin also copies these into the jar, so consumers can import the schema
            // straight out of the artifact.
            setSrcDirs(listOf("proto"))
        }
    }
}

protobuf {
    protoc {
        artifact = libs.protoc.get().toString()
    }

    plugins {
        create("grpc") {
            artifact = libs.grpc.gen.java.get().toString()
        }
    }

    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
