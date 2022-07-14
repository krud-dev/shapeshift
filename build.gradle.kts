import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.6.0"
}

if (hasProperty("release")) {
    subprojects {
        apply(plugin = "java-library")
        apply(plugin = "signing")
        apply(plugin = "maven-publish")
        apply(plugin = "org.jetbrains.dokka")
        group = "dev.krud"
        version = "0.3.0-SNAPSHOT"
        java.sourceCompatibility = JavaVersion.VERSION_1_8
        val isSnapshot = version.toString().endsWith("-SNAPSHOT")
        val repoUri = if (isSnapshot) {
            "https://s01.oss.sonatype.org/content/repositories/snapshots/"
        } else {
            "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
        }

        if (!isSnapshot) {
            java {
                withJavadocJar()
                withSourcesJar()
            }
        }

        publishing {
            publications.create<MavenPublication>("maven") {
                from(components["java"])
                repositories {
                    maven {
                        name = "OSSRH"
                        url = uri(repoUri)
                        credentials {
                            username = System.getenv("OSSRH_USERNAME") ?: extra["ossrh.username"]?.toString()
                            password = System.getenv("OSSRH_PASSWORD") ?: extra["ossrh.password"]?.toString()
                        }
                    }
                }
                pom {
                    name.set(this@subprojects.name)
                    description.set("A Kotlin library for intelligent object mapping.")
                    url.set("https://github.com/krud-dev/shapeshift/shapeshift")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            name.set("KRUD")
                            email.set("admin@krud.dev")
                            organization.set("KRUD")
                            organizationUrl.set("https://www.krud.dev")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/krud-dev/shapeshift.git")
                        developerConnection.set("scm:git:ssh://git@github.com/krud-dev/shapeshift.git")
                        url.set("https://github.com/krud-dev/shapeshift")
                    }
                }
            }
        }

        if (!isSnapshot) {
            val javadocTask = tasks.named<Javadoc>("javadoc").get()

            tasks.withType<DokkaTask> {
                javadocTask.dependsOn(this)
                outputDirectory.set(javadocTask.destinationDir)
            }

            signing {
                sign(publishing.publications["maven"])
            }
        }
    }
}