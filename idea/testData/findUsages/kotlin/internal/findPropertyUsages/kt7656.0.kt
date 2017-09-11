// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
data class A(public val field: Int) {
    internal val field<caret>: String
}

fun main(args: Array<String>) {
    val a = A(10)

    println(a.field)
}