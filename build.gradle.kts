plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.astrarre"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        name = "FabricMC"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation("net.fabricmc", "mapping-io", "0.3.0")

    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.8.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine")
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
    withJavadocJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        // Not compatible with --add-exports
        // options.release.set(16)
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Javadoc> {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }

    compileJava {
        val exports = listOf(
            "jdk.compiler/com.sun.tools.javac.api",
            "jdk.compiler/com.sun.tools.javac.file",
            "jdk.compiler/com.sun.tools.javac.tree",
            "jdk.compiler/com.sun.tools.javac.util",
            "jdk.javadoc/com.sun.tools.javac.parser",
            "jdk.javadoc/jdk.javadoc.internal.tool",
        )

        options.compilerArgs.addAll(exports.flatMap { listOf("--add-exports", "$it=io.github.astrarre.sfu") })
        println(options.compilerArgs.joinToString(separator = " "))
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
