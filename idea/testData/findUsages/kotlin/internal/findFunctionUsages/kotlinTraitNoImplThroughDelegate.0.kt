// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtNamedFunction
// OPTIONS: usages, skipImports
package server

interface TraitNoImpl {
    internal fun <caret>foo()
}

public class TraitWithDelegatedNoImpl(f: TraitNoImpl): TraitNoImpl by f

fun test(twdni: TraitWithDelegatedNoImpl) = twdni.foo()