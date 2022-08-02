
plugins {
    java
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("dev.krud:shapeshift:0.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

group = "dev.krud.shapeshift.examples.java"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

tasks.withType<Test> {
    useJUnitPlatform()
}