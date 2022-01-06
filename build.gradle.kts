plugins {
    idea
    java
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.names)
    alias(libs.plugins.lombok)
}

group = "fr.raksrinana"
description = "FTPFetcher"

dependencies {
    implementation(libs.jsch) {
        exclude(module = "commons-io")
        exclude(module = "commons-lang3")
    }

    implementation(libs.slf4j)
    implementation(libs.bundles.log4j2)

    implementation(libs.bundles.raksrinanaUtils)

    implementation(libs.commonsIo)
    implementation(libs.commonsCollections)
    implementation(libs.progressbar)

    implementation(libs.picocli)
    implementation(libs.bundles.jackson)

    compileOnly(libs.jetbrainsAnnotations)
}

repositories {
    val githubRepoUsername: String by project
    val githubRepoPassword: String by project

    maven {
        url = uri("https://maven.pkg.github.com/RakSrinaNa/JavaUtils/")
        credentials {
            username = githubRepoUsername
            password = githubRepoPassword
        }
    }
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

        doFirst {
            val compilerArgs = options.compilerArgs
            compilerArgs.add("--module-path")
            compilerArgs.add(classpath.asPath)
            classpath = files()
        }
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
