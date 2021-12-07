plugins {
    val kotlinVersion = "1.6.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.9.0-M1"
}

group = "org.laolittle.plugin.joinorquit"
version = "1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}
