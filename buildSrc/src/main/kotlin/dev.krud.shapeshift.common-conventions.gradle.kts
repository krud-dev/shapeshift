plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    // Misc
    api("org.jetbrains:annotations:23.0.0")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("io.strikt:strikt-core:0.31.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}