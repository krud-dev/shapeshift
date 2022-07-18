
plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("dev.krud:shapeshift:0.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("io.strikt:strikt-core:0.31.0")
}

group = "dev.krud.shapeshift.examples.kotlin"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

tasks.withType<Test> {
    useJUnitPlatform()
}