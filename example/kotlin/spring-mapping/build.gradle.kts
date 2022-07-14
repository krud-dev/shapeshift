
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
    implementation("dev.krud:spring-boot-starter-shapeshift:0.1.0")
}

group = "dev.krud.shapeshift.examples.kotlin"
description = "spring-mapping"

springBoot {
    mainClass.set("dev.krud.shapeshift.examples.kotlin.MainKt")
}