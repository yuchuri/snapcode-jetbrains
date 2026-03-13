package com.github.yuchuri.snapcodejetbrains.ui

import com.github.yuchuri.snapcodejetbrains.SnapcodeBundle
import com.github.yuchuri.snapcodejetbrains.render.SnapcodeRenderer
import com.github.yuchuri.snapcodejetbrains.settings.SnapcodeSettings
import com.github.yuchuri.snapcodejetbrains.settings.SnapcodeSettingsState
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

class SnapcodeDialog(
    private val project: Project,
    private val code: String,
    private val fileType: FileType
) : DialogWrapper(project) {

    // Work on a mutable copy so the user can cancel without persisting changes.
    private val settings: SnapcodeSettings = SnapcodeSettingsState.instance.state.copy()
    private var previewImage: BufferedImage? = null
    private val previewLabel = JLabel()

    init {
        title = SnapcodeBundle.message("snapcode.dialog.title")
        init()
        renderPreview()
    }

    // ─── DialogWrapper overrides ──────────────────────────────────────────────

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(0, 8))

        val previewPanel = JPanel(BorderLayout())
        previewPanel.border = BorderFactory.createTitledBorder(
            SnapcodeBundle.message("snapcode.dialog.preview")
        )
        val scrollPane = JBScrollPane(previewLabel)
        scrollPane.preferredSize = Dimension(820, 420)
        previewPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(previewPanel, BorderLayout.CENTER)

        mainPanel.add(buildOptionsPanel(), BorderLayout.SOUTH)
        return mainPanel
    }

    override fun createActions(): Array<Action> = arrayOf(
        object : AbstractAction(SnapcodeBundle.message("snapcode.action.copy")) {
            override fun actionPerformed(e: ActionEvent?) = copyToClipboard()
        },
        object : AbstractAction(SnapcodeBundle.message("snapcode.action.save")) {
            override fun actionPerformed(e: ActionEvent?) = saveAsPng()
        },
        cancelAction
    )

    // ─── Options panel ────────────────────────────────────────────────────────

    private fun buildOptionsPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createTitledBorder(
            SnapcodeBundle.message("snapcode.dialog.options")
        )
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 8, 4, 8)
            anchor = GridBagConstraints.WEST
        }
        var row = 0

        // Theme
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.theme")), gbc)
        val themeCombo = JComboBox(arrayOf("DARK", "LIGHT")).apply {
            selectedItem = settings.theme
            addActionListener {
                settings.theme = selectedItem as String
                renderPreview()
            }
        }
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(themeCombo, gbc)
        row++

        // Background start color
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.bgStart")), gbc)
        val bgStartPicker = ColorPanel().apply {
            selectedColor = settings.backgroundStartColor()
            addActionListener {
                selectedColor?.let {
                    settings.backgroundColorStart = colorToHex(it)
                    renderPreview()
                }
            }
        }
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(bgStartPicker, gbc)
        row++

        // Background end color
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.bgEnd")), gbc)
        val bgEndPicker = ColorPanel().apply {
            selectedColor = settings.backgroundEndColor()
            addActionListener {
                selectedColor?.let {
                    settings.backgroundColorEnd = colorToHex(it)
                    renderPreview()
                }
            }
        }
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(bgEndPicker, gbc)
        row++

        // Padding
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.padding")), gbc)
        val paddingValueLabel = JBLabel("${settings.padding}px")
        val paddingSlider = JSlider(10, 80, settings.padding).apply {
            addChangeListener {
                settings.padding = value
                paddingValueLabel.text = "${settings.padding}px"
                if (!valueIsAdjusting) renderPreview()
            }
        }
        val paddingRow = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)).apply {
            add(paddingSlider)
            add(paddingValueLabel)
        }
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0
        panel.add(paddingRow, gbc)
        row++

        // Show window chrome
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.windowChrome")), gbc)
        val chromeCheck = JCheckBox().apply {
            isSelected = settings.showWindowChrome
            addActionListener {
                settings.showWindowChrome = isSelected
                renderPreview()
            }
        }
        gbc.gridx = 1
        panel.add(chromeCheck, gbc)
        row++

        // Shadow
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.shadow")), gbc)
        val shadowCheck = JCheckBox().apply {
            isSelected = settings.shadowEnabled
            addActionListener {
                settings.shadowEnabled = isSelected
                renderPreview()
            }
        }
        gbc.gridx = 1
        panel.add(shadowCheck, gbc)
        row++

        // Font size
        gbc.gridx = 0; gbc.gridy = row; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0
        panel.add(JBLabel(SnapcodeBundle.message("snapcode.settings.fontSize")), gbc)
        val fontSpinner = JSpinner(SpinnerNumberModel(settings.fontSize.toInt(), 8, 32, 1)).apply {
            addChangeListener {
                settings.fontSize = (value as Int).toFloat()
                renderPreview()
            }
        }
        gbc.gridx = 1
        panel.add(fontSpinner, gbc)

        return panel
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private fun renderPreview() {
        val img = ApplicationManager.getApplication().runReadAction<BufferedImage> {
            SnapcodeRenderer.render(project, code, fileType, settings)
        }
        previewImage = img
        previewLabel.icon = ImageIcon(img)
        previewLabel.revalidate()
        // Persist settings so next time the dialog opens with the user's last choices
        SnapcodeSettingsState.instance.loadState(settings.copy())
    }

    private fun copyToClipboard() {
        val img = previewImage ?: return
        val transferable = object : Transferable {
            override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
            override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
            override fun getTransferData(flavor: DataFlavor): Any {
                if (!isDataFlavorSupported(flavor)) throw UnsupportedFlavorException(flavor)
                return img
            }
        }
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    private fun saveAsPng() {
        val img = previewImage ?: return
        val descriptor = FileSaverDescriptor(
            SnapcodeBundle.message("snapcode.save.title"),
            SnapcodeBundle.message("snapcode.save.description"),
            "png"
        )
        val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val wrapper = saveDialog.save(null, "snapshot") ?: return
        val virtualFile = wrapper.getVirtualFile(true) ?: return
        ImageIO.write(img, "PNG", virtualFile.toNioPath().toFile())
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private fun colorToHex(color: Color): String =
        String.format("#%02x%02x%02x", color.red, color.green, color.blue)
}
