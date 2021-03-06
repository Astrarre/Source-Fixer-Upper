plugins {
    `java-library`
    `maven-publish`
    signing
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

group = "io.github.astrarre"
version = "1.0.0-SNAPSHOT"

val shadowJavac by configurations.registering

repositories {
    mavenCentral()

    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation("net.fabricmc", "mapping-io", "0.3.0")

    implementation(files(RepackagedJavacProvider.createJavacJar(buildDir)))
    // This doesn't work :suffer:
    // "shadowJavac"(files(zipTree("D:\\Programs\\AdoptOpenJDK\\jdk-16.0.1.9-hotspot\\jmods\\jdk.compiler.jmod")))
    // This works
    // "shadowJavac"("net.fabricmc", "mapping-io", "0.3.0")
    // How do I ziptree a single jmod file

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform", "junit-platform-launcher")
}

sourceSets {
    val main by this

    create("console") {
        java {
            compileClasspath += main.output + main.compileClasspath
            runtimeClasspath += main.output + main.runtimeClasspath
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    withSourcesJar()
    // withJavadocJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(16)
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }

    shadowJar {
        configurations = listOf(shadowJavac.get())
        relocate("com.sun", "io.github.astrarre.sfu.shadow.com.sun")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifact(tasks.shadowJar) {
                classifier = null
            }

            artifact(tasks["sourcesJar"])

            if (signing.signatory != null) {
                signing.sign(this)
            }

            pom {
                name.set("Source Fixer Upper")
                packaging = "jar"

                description.set("A fast remapper for Java source code")
                url.set("https://github.com/Astrarre/Source-Fixer-Upper")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://spdx.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()

        maven {
            val releasesRepoUrl = uri("${buildDir}/repos/releases")
            val snapshotsRepoUrl = uri("${buildDir}/repos/snapshots")
            name = "Project"
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }

        if (project.hasProperty("maven_url") && project.hasProperty("maven_username") && project.hasProperty(
                "maven_password"
            )
        ) {
            maven {
                url = uri(project.property("maven_url") as String)

                credentials {
                    username = project.property("maven_username") as String
                    password = project.property("maven_password") as String
                }
            }
        }
    }
}
