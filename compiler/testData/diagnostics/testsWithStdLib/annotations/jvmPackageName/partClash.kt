// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !API_VERSION: 1.2

// FILE: a.kt
@file:JvmPackageName("test")
package test1

fun a() {}

// FILE: b.kt
@file:JvmPackageName("test")
package test2

fun a() {}
fun b() {}
