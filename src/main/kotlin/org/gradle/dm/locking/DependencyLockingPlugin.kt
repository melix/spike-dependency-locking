package org.gradle.dm.locking

import org.gradle.api.Plugin
import org.gradle.api.Project

open class DependencyLockingPlugin: Plugin<Project> {
    override
    fun apply(project: Project?) {
        project!!.tasks.create("resolveDependencies", DependencyResolveTask::class.java)
    }
}