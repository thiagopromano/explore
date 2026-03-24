package com.thiromano.exact

import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.roots.SyntheticLibrary
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class ElixirAdditionalLibraryRootsProvider : AdditionalLibraryRootsProvider() {

    override fun getAdditionalProjectLibraries(project: Project): Collection<SyntheticLibrary> {
        val baseDir = project.guessProjectDir() ?: return emptyList()
        if (baseDir.findChild("mix.exs") == null) return emptyList()
        val depsDir = baseDir.findChild("deps") ?: return emptyList()
        return listOf(ElixirDepsLibrary(depsDir))
    }

    private class ElixirDepsLibrary(private val depsDir: VirtualFile) : SyntheticLibrary(), ItemPresentation {

        override fun getSourceRoots(): Collection<VirtualFile> = listOf(depsDir)

        override fun getPresentableText(): String = "Elixir Dependencies (deps)"

        override fun getIcon(unused: Boolean): Icon = ElixirIcons.ELIXIR

        override fun equals(other: Any?): Boolean = other is ElixirDepsLibrary && other.depsDir == depsDir

        override fun hashCode(): Int = depsDir.hashCode()
    }
}
