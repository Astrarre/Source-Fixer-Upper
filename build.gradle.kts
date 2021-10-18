plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "org.example"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
}

sourceSets {
    val main by this

    create("console") {
        java {
            compileClasspath += main.output
            runtimeClasspath += main.output
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16

    withSourcesJar()
    withJavadocJar()
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
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

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

        if (project.hasProperty("maven_url") && project.hasProperty("maven_username") && project.hasProperty("maven_password")) {
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
