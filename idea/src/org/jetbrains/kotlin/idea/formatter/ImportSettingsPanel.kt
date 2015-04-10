/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.application.options.CodeStyleAbstractPanel
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.ui.OptionGroup
import org.jdom.Element
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class ImportSettingsPanelWrapper(settings: CodeStyleSettings) : CodeStyleAbstractPanel(settings) {
    private val importsPanel = ImportSettingsPanel(settings)

    private fun CodeStyleSettings.kotlinSettings() = getCustomSettings(javaClass<JetCodeStyleSettings>())

    override fun getRightMargin() = 0

    override fun createHighlighter(scheme: EditorColorsScheme) = null

    override fun getFileType() = JavaFileType.INSTANCE

    override fun getPreviewText() = null

    override fun apply(settings: CodeStyleSettings) = importsPanel.apply(settings.kotlinSettings())

    override fun isModified(settings: CodeStyleSettings) = importsPanel.isModified(settings.kotlinSettings())

    override fun getPanel() = importsPanel

    override fun resetImpl(settings: CodeStyleSettings) {
        importsPanel.reset(settings.kotlinSettings())
    }

    override fun getTabTitle() = ApplicationBundle.message("title.imports")
}

class ImportSettingsPanel(private val commonSettings: CodeStyleSettings) : JPanel() {
    private val rbUseSingleImports = JRadioButton("With single name")
    private val rbUseStarImports = JRadioButton("With '*'")
    private val rbUseStarImportsIfMore = JRadioButton("With '*' if more than ")
    private val starImportLimitModel = SpinnerNumberModel(4, 1, 100, 1)
    private val starImportLimitField = JSpinner(starImportLimitModel)
    private val cbImportNestedClasses = JCheckBox("Insert imports for nested classes")
    private val cbImportPackages = JCheckBox("Insert imports for packages")

    init {
        setLayout(GridBagLayout())
        add(createGeneralOptionsPanel(),
            GridBagConstraints().init { fill = GridBagConstraints.BOTH; weightx = 1.0; weighty = 1.0; insets = Insets(0, 10, 10, 10) })
    }

    private fun createGeneralOptionsPanel(): JPanel {
        ButtonGroup().init {
            add(rbUseSingleImports)
            add(rbUseStarImports)
            add(rbUseStarImportsIfMore)
        }

        fun updateEnabled() {
            starImportLimitField.setEnabled(rbUseStarImportsIfMore.isSelected())
        }
        rbUseStarImportsIfMore.addChangeListener { updateEnabled() }
        updateEnabled()

        return OptionGroup(ApplicationBundle.message("title.general")).init {
            add(JLabel("Use imports:"))
            add(rbUseSingleImports, true)
            add(rbUseStarImports, true)
            add(JPanel(GridBagLayout()).init {
                val constrains = GridBagConstraints().init { gridx = GridBagConstraints.RELATIVE }
                add(rbUseStarImportsIfMore, constrains)
                add(starImportLimitField, constrains)
                add(JLabel(" names used"), constrains.init { fill = GridBagConstraints.HORIZONTAL; weightx = 1.0 })
            }, true)

            add(cbImportNestedClasses)
            add(cbImportPackages)
        }.createPanel()
    }

    fun reset(settings: JetCodeStyleSettings) {
        when (settings.NAME_COUNT_TO_USE_STAR_IMPORT) {
            Int.MAX_VALUE -> rbUseSingleImports.setSelected(true)

            1 -> rbUseStarImports.setSelected(true)

            else -> {
                rbUseStarImportsIfMore.setSelected(true)
                starImportLimitField.setValue(settings.NAME_COUNT_TO_USE_STAR_IMPORT - 1)
            }
        }

        cbImportNestedClasses.setSelected(settings.IMPORT_NESTED_CLASSES)
        cbImportPackages.setSelected(settings.IMPORT_PACKAGES)
    }

    fun apply(settings: JetCodeStyleSettings) {
        settings.NAME_COUNT_TO_USE_STAR_IMPORT = when {
            rbUseSingleImports.isSelected() -> Int.MAX_VALUE
            rbUseStarImports.isSelected() -> 1
            else -> (starImportLimitModel.getNumber() as Int) + 1
        }
        settings.IMPORT_NESTED_CLASSES = cbImportNestedClasses.isSelected()
        settings.IMPORT_PACKAGES = cbImportPackages.isSelected()
    }

    fun isModified(settings: JetCodeStyleSettings): Boolean {
        val tempSettings = JetCodeStyleSettings(commonSettings)
        apply(tempSettings)
        val root = Element("fake")
        tempSettings.writeExternal(root, settings)
        return root.getChildren().isNotEmpty()
    }
}

fun <T> T.init(initializer: T.() -> Unit): T {
    initializer()
    return this
}