plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "org.gradle"
version = "1.0"

gradlePlugin {
    (plugins) {
        "dependencyLockPlugin" {
            id = "org.gradle.dependency-lock"
            implementationClass = "org.gradle.dm.locking.DependencyLockingPlugin"
        }
    }
}

