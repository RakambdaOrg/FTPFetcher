import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    idea
    java
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.names)
    alias(libs.plugins.jib)
}

group = "fr.rakambda"
description = "FTPFetcher"

dependencies {
    implementation(platform(libs.jacksonBom))
    implementation(platform(libs.log4j2Bom))  

    implementation(libs.sshj)

    implementation(libs.slf4j)
    implementation(libs.bundles.log4j2)

    implementation(libs.hikaricp)
    implementation(libs.h2)

    implementation(libs.commonsIo)
    implementation(libs.commonsCollections)
    implementation(libs.progressbar)
    implementation(libs.guava)

    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    compileOnly(libs.jetbrainsAnnotations)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)
}

repositories {
    mavenCentral()
}

tasks {
    processResources {
        expand(project.properties)
    }

    compileJava {
        val moduleName: String by project
        inputs.property("moduleName", moduleName)

        options.encoding = "UTF-8"
        options.isDeprecation = true
    }

    jar {
        manifest {
            attributes["Multi-Release"] = "true"
        }
    }

    shadowJar {
        archiveBaseName.set(project.name)
        archiveClassifier.set("shaded")
        archiveVersion.set("")

        transform(Log4j2PluginsCacheFileTransformer::class.java)
    }

    wrapper {
        val wrapperVersion: String by project
        gradleVersion = wrapperVersion
    }
}

application {
    val moduleName: String by project
    val className: String by project

    mainModule.set(moduleName)
    mainClass.set(className)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

jib {
    from {
        image = "eclipse-temurin:21-jdk"
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
            platform {
                os = "linux"
                architecture = "amd64"
            }
        }
    }
    container {
        creationTime.set("USE_CURRENT_TIMESTAMP")
        user = "1052:100"
    }
}
