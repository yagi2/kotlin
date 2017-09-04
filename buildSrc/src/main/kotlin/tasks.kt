@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.internal.tasks.testing.filter.DefaultTestFilter
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.extra

open class KotlinProjectTest : Test() {
    override fun getCandidateClassFiles(): FileTree {
        val patterns = filter.includePatterns + ((filter as? DefaultTestFilter)?.commandLineIncludePatterns ?: emptySet())
        if (patterns.isEmpty()) {
            return super.getCandidateClassFiles()
        }
        val classFilePaths = patterns.mapTo(HashSet()) { pattern ->
            val maybeMethodName = pattern.substringAfterLast('.')
            val className = if (maybeMethodName.isNotEmpty() && maybeMethodName[0].isLowerCase())
                pattern.substringBeforeLast('.')
            else
                pattern

            className.replace('.', '/') + ".class"
        }
        val allFiles = testClassesDirs.files.flatMap { dir ->
            classFilePaths.map { name -> File(dir, name) }
        }
        return project.files(allFiles).asFileTree
    }
}

fun Project.projectTest(body: KotlinProjectTest.() -> Unit = {}): Test = getOrCreateTask<KotlinProjectTest>("test") {
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    environment("KOTLIN_HOME", rootProject.extra["distKotlinHomeDir"])
    systemProperty("jps.kotlin.home", rootProject.extra["distKotlinHomeDir"])
    ignoreFailures = System.getenv("kotlin_build_ignore_test_failures")?.let { it == "yes" } ?: false
    body()
}

inline fun<reified T: Task> Project.getOrCreateTask(taskName: String, body: T.() -> Unit): T =
        (tasks.findByName(taskName) as? T) ?: tasks.replace(taskName, T::class.java).apply{ body() }
