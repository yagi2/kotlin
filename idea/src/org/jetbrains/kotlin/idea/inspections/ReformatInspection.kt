/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.ProblemDescriptorImpl
import com.intellij.formatting.Block
import com.intellij.formatting.FormattingDocumentModel
import com.intellij.formatting.FormattingModel
import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.formatter.FormattingDocumentModelImpl
import org.jetbrains.kotlin.idea.inspections.CollectChangesWithoutApplyModel.FormattingChange
import org.jetbrains.kotlin.idea.inspections.CollectChangesWithoutApplyModel.FormattingChange.ReplaceWhiteSpace
import org.jetbrains.kotlin.idea.inspections.CollectChangesWithoutApplyModel.FormattingChange.ShiftIndentInsideRange
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.NotNullableCopyableUserDataProperty
import org.jetbrains.kotlin.psi.UserDataProperty

var PsiFile.collectFormattingChanges: Boolean by NotNullableCopyableUserDataProperty(Key.create("COLLECT_FORMATTING_CHANGES"), false)
var PsiFile.collectChangesFormattingModel: CollectChangesWithoutApplyModel? by UserDataProperty(Key.create("COLLECT_CHANGES_FORMATTING_MODEL"))

fun collectFormattingChanges(file: PsiFile): Set<FormattingChange> {
    try {
        file.collectFormattingChanges = true
        CodeStyleManager.getInstance(file.project).reformat(file)
        return file.collectChangesFormattingModel?.requestedChanges ?: emptySet()
    }
    finally {
        file.collectFormattingChanges = false
        file.collectChangesFormattingModel = null
    }
}

class CollectChangesWithoutApplyModel(val file: PsiFile, val block: Block) : FormattingModel {
    sealed class FormattingChange {
        data class ShiftIndentInsideRange(val node: ASTNode?, val range: TextRange, val indent: Int) : FormattingChange()
        data class ReplaceWhiteSpace(val textRange: TextRange, val whiteSpace: String) : FormattingChange()
    }

    private val documentModel = FormattingDocumentModelImpl.createOn(file)
    private val changes = HashSet<FormattingChange>()

    val requestedChanges: Set<FormattingChange> get() = changes

    override fun commitChanges() {
        /* do nothing */
    }

    override fun getDocumentModel(): FormattingDocumentModel = documentModel
    override fun getRootBlock(): Block = block

    override fun shiftIndentInsideRange(node: ASTNode?, range: TextRange, indent: Int): TextRange {
        changes.add(ShiftIndentInsideRange(node, range, indent))
        return range
    }

    override fun replaceWhiteSpace(textRange: TextRange, whiteSpace: String): TextRange {
        changes.add(ReplaceWhiteSpace(textRange, whiteSpace))
        return textRange
    }
}

class ReformatInspection : LocalInspectionTool() {
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (file !is KtFile || !ProjectRootsUtil.isInProjectSource(file)) {
            return null
        }

        val changes = collectFormattingChanges(file)
        if (changes.isEmpty()) return null

        val elements = changes.map {
            val rangeOffset = when (it) {
                is ShiftIndentInsideRange -> it.range.startOffset
                is ReplaceWhiteSpace -> it.textRange.startOffset
            }

            val leaf = file.findElementAt(rangeOffset) ?: return@map null
            if (!leaf.isValid) return@map null
            if (leaf is PsiWhiteSpace && isEmptyLineReformat(leaf, it)) return@map null

            leaf
        }.filterNotNull()

        return elements.map {
            ProblemDescriptorImpl(it, it,
                                  "File is not properly formatted",
                                  arrayOf(ReformatQuickFix),
                                  ProblemHighlightType.WEAK_WARNING, false, null,
                                  isOnTheFly)
        }.toTypedArray()
    }

    private fun isEmptyLineReformat(whitespace: PsiWhiteSpace, change: FormattingChange): Boolean {
        if (change !is FormattingChange.ReplaceWhiteSpace) return false

        val text = whitespace.text
        val replacedWith = change.whiteSpace

        return text.count { it == '\n' } == replacedWith.count { it == '\n' } &&
               text.substringAfterLast('\n') == replacedWith.substringAfterLast('\n')
    }

    private object ReformatQuickFix : LocalQuickFix {
        override fun getFamilyName(): String = "Reformat File"
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            CodeStyleManager.getInstance(project).reformat(descriptor.psiElement.containingFile)
        }
    }
}