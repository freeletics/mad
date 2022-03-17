package com.freeletics.mad.whetstone

import com.freeletics.mad.whetstone.codegen.FileGenerator
import com.freeletics.mad.whetstone.codegen.util.TopLevelFunctionReference
import com.freeletics.mad.whetstone.codegen.util.composeFqName
import com.freeletics.mad.whetstone.codegen.util.composeFragmentFqName
import com.freeletics.mad.whetstone.codegen.util.composeNavDestinationFqName
import com.freeletics.mad.whetstone.codegen.util.composeRootNavDestinationFqName
import com.freeletics.mad.whetstone.codegen.util.findAnnotation
import com.freeletics.mad.whetstone.codegen.util.fragment
import com.freeletics.mad.whetstone.codegen.util.fragmentNavDestinationFqName
import com.freeletics.mad.whetstone.codegen.util.fragmentRootNavDestinationFqName
import com.freeletics.mad.whetstone.codegen.util.navEntryComponentFqName
import com.freeletics.mad.whetstone.codegen.util.optionalBooleanArgument
import com.freeletics.mad.whetstone.codegen.util.optionalClassArgument
import com.freeletics.mad.whetstone.codegen.util.rendererFragmentFqName
import com.freeletics.mad.whetstone.codegen.util.requireClassArgument
import com.freeletics.mad.whetstone.codegen.util.requireEnumArgument
import com.freeletics.mad.whetstone.codegen.util.toFunctionReference
import com.google.auto.service.AutoService
import com.squareup.anvil.annotations.ExperimentalAnvilApi
import com.squareup.anvil.compiler.api.AnvilContext
import com.squareup.anvil.compiler.api.CodeGenerator
import com.squareup.anvil.compiler.api.GeneratedFile
import com.squareup.anvil.compiler.api.createGeneratedFile
import com.squareup.anvil.compiler.internal.reference.AnnotatedReference
import com.squareup.anvil.compiler.internal.reference.ClassReference
import com.squareup.anvil.compiler.internal.reference.classAndInnerClassReferences
import com.squareup.kotlinpoet.ClassName
import java.io.File
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

@OptIn(ExperimentalAnvilApi::class)
@AutoService(CodeGenerator::class)
public class WhetstoneCodeGenerator : CodeGenerator {

    override fun isApplicable(context: AnvilContext): Boolean = !context.disableComponentMerging

    override fun generateCode(
        codeGenDir: File,
        module: ModuleDescriptor,
        projectFiles: Collection<KtFile>
    ): Collection<GeneratedFile> {
        val rendererFragment = projectFiles
            .classAndInnerClassReferences(module)
            .mapNotNull { reference -> generateRendererCode(codeGenDir, reference) }

        val composeScreen = projectFiles
            .flatMap { it.declarations.filterIsInstance<KtNamedFunction>() }
            .map { it.toFunctionReference(module) }
            .mapNotNull { function -> generateComposeScreenCode(codeGenDir, function) }

        val composeFragment = projectFiles
            .flatMap { it.declarations.filterIsInstance<KtNamedFunction>() }
            .map { it.toFunctionReference(module) }
            .mapNotNull { function -> generateComposeFragmentCode(codeGenDir, function) }

        return rendererFragment.toList() + composeFragment + composeScreen
    }

    private fun generateRendererCode(
        codeGenDir: File,
        declaration: ClassReference
    ): GeneratedFile? {
        val renderer = declaration.findAnnotation(rendererFragmentFqName) ?: return null
        val data = RendererFragmentData(
            baseName = declaration.shortName,
            packageName = declaration.packageName(),
            scope = renderer.requireClassArgument("scope", 0),
            parentScope = renderer.requireClassArgument("parentScope", 1),
            dependencies = renderer.requireClassArgument("dependencies", 2),
            stateMachine = renderer.requireClassArgument("stateMachine", 3),
            factory = renderer.requireClassArgument("rendererFactory", 4),
            fragmentBaseClass = renderer.optionalClassArgument("fragmentBaseClass", 5) ?: fragment,
            coroutinesEnabled = renderer.optionalBooleanArgument("coroutinesEnabled", 6) ?: false,
            rxJavaEnabled = renderer.optionalBooleanArgument("rxJavaEnabled", 7) ?: false,
            navigation = fragmentNavigation(declaration, declaration.packageName()),
        )

        val file = FileGenerator().generate(data)
        return createGeneratedFile(
            codeGenDir = codeGenDir,
            packageName = file.packageName,
            fileName = file.name,
            content = file.toString()
        )
    }

    private fun generateComposeFragmentCode(
        codeGenDir: File,
        declaration: TopLevelFunctionReference.Psi
    ): GeneratedFile? {
        val compose = declaration.findAnnotation(composeFragmentFqName) ?: return null
        val data = ComposeFragmentData(
            baseName = declaration.name,
            packageName = declaration.packageName(),
            scope = compose.requireClassArgument("scope", 0),
            parentScope = compose.requireClassArgument("parentScope", 1),
            dependencies = compose.requireClassArgument("dependencies", 2),
            stateMachine = compose.requireClassArgument("stateMachine", 3),
            fragmentBaseClass = compose.optionalClassArgument("fragmentBaseClass", 4) ?: fragment,
            enableInsetHandling = compose.optionalBooleanArgument("enableInsetHandling", 5) ?: false,
            coroutinesEnabled = compose.optionalBooleanArgument("coroutinesEnabled", 6) ?: false,
            rxJavaEnabled = compose.optionalBooleanArgument("rxJavaEnabled", 7) ?: false,
            navigation = fragmentNavigation(declaration, declaration.packageName()),
        )

        val file = FileGenerator().generate(data)
        return createGeneratedFile(
            codeGenDir = codeGenDir,
            packageName = file.packageName,
            fileName = file.name,
            content = file.toString()
        )
    }

    private fun fragmentNavigation(
        declaration: AnnotatedReference,
        packageName: String,
    ): Navigation.Fragment? {
        val navigation = declaration.findAnnotation(fragmentNavDestinationFqName)
        if (navigation != null) {
            val route = navigation.requireClassArgument("route", 0)
            val destinationScope = navigation.requireClassArgument("destinationScope", 2)
            return Navigation.Fragment(
                route = route,
                destinationType = navigation.requireEnumArgument("type", 1),
                destinationScope = destinationScope,
                navEntryData = navEntryData(
                    declaration = declaration,
                    packageName = packageName,
                    route = route,
                    destinationScope = destinationScope,
                ),
            )
        }
        val rootNavigation = declaration.findAnnotation(fragmentRootNavDestinationFqName)
        if (rootNavigation != null) {
            val route = rootNavigation.requireClassArgument("root", 0)
            val destinationScope = rootNavigation.requireClassArgument("destinationScope", 1)
            return Navigation.Fragment(
                route = route,
                destinationType = "SCREEN",
                destinationScope = destinationScope,
                navEntryData = navEntryData(
                    declaration = declaration,
                    packageName = packageName,
                    route = route,
                    destinationScope = destinationScope,
                ),
            )
        }
        return null
    }

    private fun generateComposeScreenCode(
        codeGenDir: File,
        declaration: TopLevelFunctionReference.Psi,
    ): GeneratedFile? {
        val compose = declaration.findAnnotation(composeFqName) ?: return null
        val data = ComposeScreenData(
            baseName = declaration.name,
            packageName = declaration.packageName(),
            scope = compose.requireClassArgument("scope", 0),
            parentScope = compose.requireClassArgument("parentScope", 1),
            dependencies = compose.requireClassArgument("dependencies", 2),
            stateMachine = compose.requireClassArgument("stateMachine", 3),
            coroutinesEnabled = compose.optionalBooleanArgument("coroutinesEnabled", 4) ?: false,
            rxJavaEnabled = compose.optionalBooleanArgument("rxJavaEnabled", 5) ?: false,
            navigation = composeNavigation(declaration, declaration.packageName()),
        )

        val file = FileGenerator().generate(data)
        return createGeneratedFile(
            codeGenDir = codeGenDir,
            packageName = file.packageName,
            fileName = file.name,
            content = file.toString()
        )
    }

    private fun composeNavigation(
        declaration: AnnotatedReference,
        packageName: String
    ): Navigation.Compose? {
        val navigation = declaration.findAnnotation(composeNavDestinationFqName)
        if (navigation != null) {
            val route = navigation.requireClassArgument("route", 0)
            val destinationScope = navigation.requireClassArgument("destinationScope", 2)
            return Navigation.Compose(
                route = route,
                destinationType = navigation.requireEnumArgument("type", 1),
                destinationScope = destinationScope,
                navEntryData = navEntryData(
                    declaration = declaration,
                    packageName = packageName,
                    route = route,
                    destinationScope = destinationScope,
                ),
            )
        }
        val rootNavigation = declaration.findAnnotation(composeRootNavDestinationFqName)
        if (rootNavigation != null) {
            val route = rootNavigation.requireClassArgument("root", 0)
            val destinationScope = rootNavigation.requireClassArgument("destinationScope", 1)
            return Navigation.Compose(
                route = route,
                destinationType = "SCREEN",
                destinationScope = destinationScope,
                navEntryData = navEntryData(
                    declaration = declaration,
                    packageName = packageName,
                    route = route,
                    destinationScope = destinationScope,
                ),
            )
        }
        return null
    }

    private fun navEntryData(
        declaration: AnnotatedReference,
        packageName: String,
        route: ClassName,
        destinationScope: ClassName,
    ): NavEntryData? {
        val component = declaration.findAnnotation(navEntryComponentFqName) ?: return null
        return NavEntryData(
            packageName = packageName,
            scope = component.requireClassArgument("scope", 0),
            parentScope = component.requireClassArgument("parentScope", 1),
            destinationScope = destinationScope,
            route = route,
            coroutinesEnabled = component.optionalBooleanArgument("coroutinesEnabled", 2) ?: false,
            rxJavaEnabled = component.optionalBooleanArgument("rxJavaEnabled", 3) ?: false,
        )
    }

    private fun TopLevelFunctionReference.packageName(): String {
        return when(this) {
            is TopLevelFunctionReference.Psi -> function.containingKtFile.packageFqName
            is TopLevelFunctionReference.Descriptor -> function.containingPackage()!!
        }.packageString()
    }

    private fun ClassReference.packageName(): String {
        return packageFqName.packageString()
    }

    private fun FqName.packageString(): String {
        return pathSegments().joinToString(separator = ".")
    }
}
