import com.novoda.gradle.release.PublishExtension

buildscript {
    mapOf(
        "graphql" to "com.graphql-java:graphql-java:11.0"
        , "aliyunFc" to "com.aliyun.fc.runtime:fc-java-core:1.2.0"
        , "http4k" to "org.http4k:http4k-server-apache:3.100.0"
        ,  "caffeine" to "com.github.ben-manes.caffeine:caffeine:2.6.2"
    ).entries.forEach {
        extra.set(it.key, parent?.extra?.run { if (has(it.key)) get(it.key) else null } ?: it.value)
    }
}

plugins {
    application
    kotlin("jvm") version "1.3.10"
    id("bintray-release") version "SNAPSHOT-9"
}

version = "1.1.3"
group = "com.github.xuybin"

application {
    mainClassName = "com.github.xuybin.fc.graphql.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile(extra["caffeine"].toString())
    compile(extra["http4k"].toString())
    api(extra["graphql"].toString())
    api(extra["aliyunFc"].toString())
}

tasks {
    jar {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to project.application.mainClassName
                    , "Implementation-Title" to project.name
                    , "Implementation-Version" to project.version
                )
            )
        }
//        into("lib"){
//            from(configurations.compile.get().resolve().map { if (it.isDirectory) it else it })
//        }
    }

    processResources {
        filesMatching("**/defaults.properties") {
            expand(project.properties)
        }
    }
}

configure<PublishExtension> {
    userOrg = "xuybin"
    groupId = "${project.group}"
    artifactId = "${project.name}"
    publishVersion = "${project.version}"
    desc = "Function Compute Graphql"
    website = "https://github.com/xuybin/fc-graphql"
    dryRun = false
    override = true
}
