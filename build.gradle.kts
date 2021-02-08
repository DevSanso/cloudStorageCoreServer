import com.google.protobuf.gradle.*;

plugins {
    java
    kotlin("jvm") version "1.4.21"
    idea
    id("com.google.protobuf") version "0.8.8"

}

group = "com.github.DevSanso"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    google()
}

buildscript {

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.14")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.21")
    }
}

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.21")
    implementation("io.grpc:grpc-core:1.34.1")
    implementation("io.grpc:grpc-stub:1.34.1")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("io.grpc:grpc-protobuf:1.35.0")
    testCompile("junit", "junit", "4.12")
}


protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.7.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.20.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
            }
        }
    }
}

idea {
    module {
        generatedSourceDirs.addAll(listOf(
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/grpc"),
            file("${protobuf.protobuf.generatedFilesBaseDir}/main/java")
        ))
    }
}