package com.thiromano.exact

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider.PluginBundle

class ElixirTextMateBundleProvider : TextMateBundleProvider {
    override fun getBundles(): List<PluginBundle> {
        val plugin = PluginManagerCore.getPlugin(
            PluginId.getId("com.thiromano.exact")
        ) ?: return emptyList()

        val bundlePath = plugin.pluginPath.resolve("textmate")
        return listOf(PluginBundle("Elixir", bundlePath))
    }
}
