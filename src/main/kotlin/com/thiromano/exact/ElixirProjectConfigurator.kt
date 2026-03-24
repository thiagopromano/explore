package com.thiromano.exact

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile

class ElixirProjectConfigurator : ProjectActivity {

    companion object {
        private val EXCLUDED_DIR_NAMES = listOf(
            "_build",
            "cover",
            "deps",
            "doc",
            "node_modules",
            ".elixir_ls",
            ".expert",
        )
    }

    override suspend fun execute(project: Project) {
        val baseDir = project.guessProjectDir() ?: return
        if (baseDir.findChild("mix.exs") == null) return

        val testDir = baseDir.findChild("test")
        val excludedDirs = EXCLUDED_DIR_NAMES.mapNotNull { baseDir.findChild(it) }
        if (testDir == null && excludedDirs.isEmpty()) return

        for (module in ModuleManager.getInstance(project).modules) {
            val rootManager = ModuleRootManager.getInstance(module)
            if (isAlreadyConfigured(rootManager, testDir, excludedDirs)) continue

            writeAction {
                val model = rootManager.modifiableModel
                for (entry in model.contentEntries) {
                    val entryFile = entry.file ?: continue

                    if (testDir != null && VfsUtil.isAncestor(entryFile, testDir, true)) {
                        val alreadyMarked = entry.sourceFolders.any { it.file == testDir }
                        if (!alreadyMarked) {
                            entry.addSourceFolder(testDir, /* isTestSource = */ true)
                        }
                    }

                    for (dir in excludedDirs) {
                        if (VfsUtil.isAncestor(entryFile, dir, true)) {
                            val alreadyExcluded = entry.excludeFolders.any { it.file == dir }
                            if (!alreadyExcluded) {
                                entry.addExcludeFolder(dir)
                            }
                        }
                    }
                }
                model.commit()
            }
        }
    }

    private fun isAlreadyConfigured(
        rootManager: ModuleRootManager,
        testDir: VirtualFile?,
        excludedDirs: List<VirtualFile>,
    ): Boolean {
        val entries = rootManager.contentEntries
        val testOk = testDir == null || entries.any { e -> e.sourceFolders.any { it.file == testDir } }
        val excludeOk = excludedDirs.all { dir ->
            entries.any { e -> e.excludeFolders.any { it.file == dir } }
        }
        return testOk && excludeOk
    }
}
