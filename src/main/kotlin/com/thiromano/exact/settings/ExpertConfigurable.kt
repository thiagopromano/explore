package com.thiromano.exact.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.thiromano.exact.lsp.ElixirLspServerSupportProvider
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class ExpertConfigurable : Configurable {

    private var panel: JPanel? = null
    private var expertPathField: TextFieldWithBrowseButton? = null
    private var minQueryLengthSpinner: JSpinner? = null
    private var downloadButton: JButton? = null

    override fun getDisplayName(): String = "Exact: Elixir Support"

    override fun createComponent(): JComponent {
        expertPathField = TextFieldWithBrowseButton().apply {
            addBrowseFolderListener(
                "Select Expert Binary",
                "Path to the Expert language server executable",
                null,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
            )
        }

        val pathHint = JBLabel(
            "Path to the expert or start_expert executable. If left empty, expert will be resolved from PATH."
        ).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            font = JBUI.Fonts.smallFont()
        }

        downloadButton = JButton("Download Expert").apply {
            addActionListener { downloadExpert() }
        }

        minQueryLengthSpinner = JSpinner(
            SpinnerNumberModel(ExpertSettings.DEFAULT_MIN_QUERY_LENGTH, 1, 10, 1)
        )

        val queryHint = JBLabel(
            "Minimum number of characters before workspace symbol search returns results."
        ).apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            font = JBUI.Fonts.smallFont()
        }

        val colorSettingsLink = HyperlinkLabel("Elixir Color Scheme Settings").apply {
            addHyperlinkListener {
                ShowSettingsUtil.getInstance().showSettingsDialog(
                    null as com.intellij.openapi.project.Project?,
                    { configurable ->
                        configurable is com.intellij.openapi.options.SearchableConfigurable &&
                            configurable.displayName == "Elixir" &&
                            configurable.id.contains("colors")
                    },
                    null
                )
            }
        }

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Expert binary path:", expertPathField!!, 1, false)
            .addComponentToRightColumn(pathHint, 0)
            .addComponentToRightColumn(downloadButton!!, 0)
            .addLabeledComponent("Min symbol query length:", minQueryLengthSpinner!!, 1, false)
            .addComponentToRightColumn(queryHint, 0)
            .addSeparator()
            .addComponent(colorSettingsLink)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        return panel!!
    }

    private fun downloadExpert() {
        if (ExpertDownloader.getAssetName() == null) {
            downloadButton?.text = "Unsupported platform"
            return
        }

        downloadButton?.isEnabled = false
        downloadButton?.text = "Downloading..."

        ExpertDownloader.download(
            project = null,
            onSuccess = { destFile ->
                expertPathField?.text = destFile.absolutePath
                downloadButton?.text = "Download Expert"
                downloadButton?.isEnabled = true
            },
            onError = {
                downloadButton?.text = "Download failed"
                downloadButton?.isEnabled = true
            }
        )
    }

    override fun isModified(): Boolean {
        val settings = ExpertSettings.getInstance()
        return expertPathField?.text != (settings.expertPath ?: "") ||
            minQueryLengthSpinner?.value != settings.minQueryLength
    }

    override fun apply() {
        val settings = ExpertSettings.getInstance()
        val oldPath = settings.expertPath
        settings.expertPath = expertPathField?.text?.takeIf { it.isNotBlank() }
        settings.minQueryLength = minQueryLengthSpinner?.value as? Int ?: ExpertSettings.DEFAULT_MIN_QUERY_LENGTH

        if (oldPath != settings.expertPath) {
            restartLspServers()
        }
    }

    private fun restartLspServers() {
        for (project in ProjectManager.getInstance().openProjects) {
            LspServerManager.getInstance(project)
                .stopAndRestartIfNeeded(ElixirLspServerSupportProvider::class.java)
        }
    }

    override fun reset() {
        val settings = ExpertSettings.getInstance()
        expertPathField?.text = settings.expertPath ?: ""
        minQueryLengthSpinner?.value = settings.minQueryLength
    }

    override fun disposeUIResources() {
        panel = null
        expertPathField = null
        minQueryLengthSpinner = null
        downloadButton = null
    }
}
