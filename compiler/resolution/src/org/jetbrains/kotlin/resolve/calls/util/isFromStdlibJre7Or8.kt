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

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.getSinceKotlinAnnotation

private val kotlin: FqName = KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME
private val kotlinText: FqName = KotlinBuiltIns.TEXT_PACKAGE_FQ_NAME
private val kotlinCollections: FqName = KotlinBuiltIns.COLLECTIONS_PACKAGE_FQ_NAME
private val kotlinStreams: FqName = kotlin.child(Name.identifier("streams"))

private val use: Name = Name.identifier("use")
private val autoCloseable: FqName = FqName("java.lang.AutoCloseable")

private val get: Name = Name.identifier("get")
private val matchGroupCollection: FqName = FqName("kotlin.text.MatchGroupCollection")

private val getOrDefault: Name = Name.identifier("getOrDefault")
private val remove: Name = Name.identifier("remove")
private val map: FqName = KotlinBuiltIns.FQ_NAMES.map
private val mutableMap: FqName = KotlinBuiltIns.FQ_NAMES.mutableMap

fun CallableDescriptor.isLowPriorityFromStdlibJre7Or8(): Boolean {
    val containingPackage = containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingPackage.fqName
    if (!packageFqName.startsWith(KotlinBuiltIns.BUILT_INS_PACKAGE_NAME)) return false

    val extensionReceiver = extensionReceiverParameter?.type?.constructor?.declarationDescriptor ?: return false

    when (packageFqName) {
        kotlin -> {
            if (name != use) return false

            val singleBound = (extensionReceiver as? TypeParameterDescriptor)?.upperBounds?.singleOrNull()
                    ?.constructor?.declarationDescriptor
            if (singleBound?.hasFqName(autoCloseable) != true) return false
        }
        kotlinText -> {
            if (name != get) return false

            if (!extensionReceiver.hasFqName(matchGroupCollection)) return false
            if (!KotlinBuiltIns.isStringOrNullableString(valueParameters.singleOrNull()?.type ?: return false)) return false
        }
        kotlinCollections -> {
            if (name != getOrDefault && !(name == remove && valueParameters.size == 2)) return false

            if (!extensionReceiver.hasFqName(map) && !extensionReceiver.hasFqName(mutableMap)) return false
        }
        kotlinStreams -> {
            // Everything in kotlin.streams in kotlin-stdlib-jre{7,8} is low priority
        }
        else -> return false
    }

    val sinceKotlin = getSinceKotlinAnnotation() ?: return false
    val version = sinceKotlin.allValueArguments.values.singleOrNull()?.value as? String ?: return false

    return version == LanguageVersion.KOTLIN_1_1.versionString
}

fun ClassifierDescriptor.hasFqName(fqName: FqName): Boolean =
        name == fqName.shortName() && fqNameSafe == fqName
