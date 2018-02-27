package org.gradle.dm.locking

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolutionResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

open class DependencyResolveTask : DefaultTask() {

    @Input
    @Option(option = "configuration", description = "The configuration to resolve")
    lateinit var configuration: String

    @Input
    @Option(option = "write-lock", description = "Write lock file")
    var writeLock: Boolean = false

    @get: Input
    val lockFile: File by lazy {
        project.file("dependency-locks/${configuration}.lockfile")
    }

    @TaskAction
    fun resolveDependencies() {
        val hasLock = lockFile.exists()
        if (!writeLock && hasLock) {
            println("Found lock file $lockFile")
            readLock {
                // add a dependency constraint for each module found in the lock file.
                // In practice we won't do this on the constraint container itself probably,
                // because it would influence what we publish (constraints that are not in
                // the build file itself)
                project.dependencies.constraints.add(configuration, it)
            }
        }
        val modules = modules(project.configurations.getByName(configuration).incoming.resolutionResult)
        if (writeLock) {
            println("Writing lock file to ${lockFile}")
            val parentFile = lockFile.parentFile
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            writeLock(modules)
        } else if (hasLock) {
            println("Checking lock file")
            readLock { moduleNotation ->
                var found = false
                modules.forEach {
                    val prefix = it.group + ":" + it.module + ":"
                    if (moduleNotation.startsWith(prefix)) {
                        if (moduleNotation != it.toString()) {
                            throw LockOutOfDateException("Expected $it but found $moduleNotation in lock file")
                        }
                        found = true
                    }
                }
                if (!found) {
                    throw LockOutOfDateException("Found an unexpected dependency in lock file: $moduleNotation")
                }
            }
        }
    }

    private
    fun writeLock(modules: List<ModuleComponentIdentifier>) = lockFile.bufferedWriter(Charsets.UTF_8).use { writer ->
        modules.forEach {
            writer.write(it.toString())
            writer.newLine()
        }
    }

    private
    fun modules(resolutionResult: ResolutionResult) =
            resolutionResult.allComponents.filter(::isModule).map { it.id as ModuleComponentIdentifier }

    private
    fun isModule(component: ResolvedComponentResult) = component.id is ModuleComponentIdentifier

    private
    fun readLock(forEachModule: (String) -> Unit) =
            lockFile.bufferedReader(Charsets.UTF_8).forEachLine(forEachModule)
}