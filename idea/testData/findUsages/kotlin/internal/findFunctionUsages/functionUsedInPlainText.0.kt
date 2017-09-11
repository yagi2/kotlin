// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: textOccurrences
package test

class Foo {
    internal fun <caret>foo() {

    }
}
