import org.jetbrains.dokka.gradle.DokkaTask
import java.util.*

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka") version "1.6.0"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

if (hasProperty("release")) {
    val ossrhUsername = System.getenv("OSSRH_USERNAME")
    val ossrhPassword = System.getenv("OSSRH_PASSWORD")
    val releaseVersion = System.getenv("RELEASE_VERSION")
    group = "dev.krud"
    version = releaseVersion
    nexusPublishing {
        this@nexusPublishing.repositories {
            sonatype {
                username.set(ossrhUsername)
                password.set(ossrhPassword)
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            }
        }
    }
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    if (hasProperty("release")) {
        val releaseVersion = System.getenv("RELEASE_VERSION")
        val signingKeyBase64 = System.getenv("OSSRH_GPG_SECRET_KEY_BASE64")
        val signingPassword = System.getenv("OSSRH_GPG_SECRET_KEY_PASSWORD")
        group = "dev.krud"
        version = releaseVersion
        val isSnapshot = version.toString().endsWith("-SNAPSHOT")
        java {
            withJavadocJar()
            withSourcesJar()
        }

        publishing {
            publications.create<MavenPublication>("maven") {
                from(components["java"])
                artifactId = project.name

                pom {
                    name.set(project.name)
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

        val javadocTask = tasks.named<Javadoc>("javadoc").get()

        tasks.withType<DokkaTask> {
            javadocTask.dependsOn(this)
            outputDirectory.set(javadocTask.destinationDir)
        }
        signing {
            val signingKey = signingKeyBase64?.let { decodeBase64(it) }
            useInMemoryPgpKeys(
                signingKey, signingPassword
            )
            sign(publishing.publications["maven"])
        }
    }
}

fun decodeBase64(base64: String): String {
    return String(Base64.getDecoder().decode(base64))
}
