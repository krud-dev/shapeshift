
plugins {
    java
    id("dev.krud.shapeshift.examples.java.common-conventions")
    id("org.springframework.boot") version "2.6.7"
}

apply(plugin = "io.spring.dependency-management")
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("dev.krud:spring-boot-starter-shapeshift:0.4.0")
    implementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

group = "dev.krud.shapeshift.examples.java"
description = "spring-mapping"

springBoot {
    mainClass.set("dev.krud.shapeshift.examples.java.SpringMappingApplication")
}