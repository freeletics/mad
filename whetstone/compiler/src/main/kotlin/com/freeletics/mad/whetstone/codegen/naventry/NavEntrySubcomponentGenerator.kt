package com.freeletics.mad.whetstone.codegen.naventry

import com.freeletics.mad.whetstone.NavEntryData
import com.freeletics.mad.whetstone.codegen.Generator
import com.freeletics.mad.whetstone.codegen.util.bindsInstanceParameter
import com.freeletics.mad.whetstone.codegen.util.compositeDisposable
import com.freeletics.mad.whetstone.codegen.util.contributesToAnnotation
import com.freeletics.mad.whetstone.codegen.util.coroutineScope
import com.freeletics.mad.whetstone.codegen.util.internalApiAnnotation
import com.freeletics.mad.whetstone.codegen.util.propertyName
import com.freeletics.mad.whetstone.codegen.util.savedStateHandle
import com.freeletics.mad.whetstone.codegen.util.scopeToAnnotation
import com.freeletics.mad.whetstone.codegen.util.subcomponentAnnotation
import com.freeletics.mad.whetstone.codegen.util.subcomponentFactoryAnnotation
import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.internal.decapitalize
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier.ABSTRACT
import com.squareup.kotlinpoet.TypeSpec

internal val Generator<NavEntryData>.navEntrySubcomponentClassName
    get() = ClassName("NavEntry${data.baseName}Component")

internal val Generator<NavEntryData>.navEntrySubcomponentFactoryClassName
    get() = navEntrySubcomponentClassName.nestedClass("Factory")

internal const val navEntrySubcomponentFactoryCreateName = "create"

internal val Generator<NavEntryData>.navEntryParentComponentClassName
    get() = navEntrySubcomponentClassName.nestedClass("ParentComponent")

@OptIn(ExperimentalAnvilApi::class)
internal val Generator<NavEntryData>.navEntryParentComponentGetterName
    get() = "${navEntrySubcomponentClassName.simpleName.decapitalize()}Factory"

internal class NavEntrySubcomponentGenerator(
    override val data: NavEntryData,
) : Generator<NavEntryData>() {

    fun generate(): TypeSpec {
        return TypeSpec.interfaceBuilder(navEntrySubcomponentClassName)
            .addAnnotation(internalApiAnnotation())
            .addAnnotation(scopeToAnnotation(data.scope))
            .addAnnotation(subcomponentAnnotation(data.scope, data.parentScope))
            .addType(navEntrySubcomponentFactory())
            .addType(navEntrySubcomponentFactoryParentComponent())
            .build()
    }

    private fun navEntrySubcomponentFactory(): TypeSpec {
        val createFun = FunSpec.builder(navEntrySubcomponentFactoryCreateName)
            .addModifiers(ABSTRACT)
            .addParameter(bindsInstanceParameter("savedStateHandle", savedStateHandle))
            .addParameter(bindsInstanceParameter(data.route.propertyName, data.route))
            .apply {
                if (data.rxJavaEnabled) {
                    addParameter(bindsInstanceParameter("compositeDisposable", compositeDisposable))
                }
                if (data.coroutinesEnabled) {
                    addParameter(bindsInstanceParameter("coroutineScope", coroutineScope))
                }
            }
            .returns(navEntrySubcomponentClassName)
            .build()
        return TypeSpec.interfaceBuilder(navEntrySubcomponentFactoryClassName)
            .addAnnotation(subcomponentFactoryAnnotation())
            .addFunction(createFun)
            .build()
    }

    private fun navEntrySubcomponentFactoryParentComponent(): TypeSpec {
        val getterFun = FunSpec.builder(navEntryParentComponentGetterName)
            .addModifiers(ABSTRACT)
            .returns(navEntrySubcomponentFactoryClassName)
            .build()
        return TypeSpec.interfaceBuilder(navEntryParentComponentClassName)
            .addAnnotation(contributesToAnnotation(data.parentScope))
            .addFunction(getterFun)
            .build()
    }
}
