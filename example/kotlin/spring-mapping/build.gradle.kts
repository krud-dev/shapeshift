
plugins {
    java
    id("dev.krud.shapeshift.examples.kotlin.common-conventions")
    id("org.springframework.boot") version "2.6.7"
    kotlin("plugin.spring") version "1.6.21"
}

apply(plugin = "io.spring.dependency-management")
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("dev.krud:spring-boot-starter-shapeshift:0.3.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

group = "dev.krud.shapeshift.examples.kotlin"
description = "spring-mapping"

springBoot {
    mainClass.set("dev.krud.shapeshift.examples.kotlin.MainKt")
}