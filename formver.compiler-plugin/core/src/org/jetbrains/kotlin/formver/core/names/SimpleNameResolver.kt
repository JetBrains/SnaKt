package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.formver.viper.ast.DomainName
import org.jetbrains.kotlin.formver.viper.ast.NamedDomainAxiomLabel
import org.jetbrains.kotlin.formver.viper.ast.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.ast.UnqualifiedDomainFuncName

/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 *
 * Current strategy (simplified, will be extended later):
 *  1. Concatenate all non-null components (type, scope, baseName) with SEPARATOR.
 *
 * Future strategy:
 *  1. Try to use a short name: <type>_<baseName>.
 *  2. If the short name is reserved or conflicts with an existing name of the same type, fall back to a long name:
 *     <type>_<scope>_<baseName>.
 *  3. Track used names to detect conflicts for future resolutions.
 */
class SimpleNameResolver : NameResolver {

    val cache = mutableMapOf<NamedEntity, String>()

    companion object {
        private fun resolveParts(
            name: NamedEntity,
            skipType: Boolean = false,
            skipScope: Boolean = false
        ): List<String> = when (name) {
            is NameScope -> when (name) {
                is BadScope -> listOf("<BAD>")
                is ClassScope -> (if (skipScope) emptyList() else resolveParts(name.parent)) + resolveParts(name.className)
                is FakeScope -> emptyList()
                is LocalScope -> listOf("l${name.level}")
                is PackageScope -> {
                    return if (name.packageName.isRoot || skipScope) {
                        emptyList()
                    } else {
                        listOf($$"pkg$$${name.packageName.asViperString()}")
                    }
                }

                is ParameterScope -> listOf("par")
                is PrivateScope -> resolveParts(name.parent) + listOf("private")
                is PublicScope -> listOf("public")
            }

            is SymbolicName -> when (name) {
                is FreshName -> (name.nameType.takeIf { it != NameType.Base.Variable }
                    ?.let { resolveParts(name.nameType, skipType) } ?: emptyList()) + when (name) {
                    is NumberedName -> when (name) {
                        is AnonymousBuiltinName -> listOf("anon", "builtin")
                        is AnonymousName -> listOf("anon")
                        is LabelName -> when (name) {
                            is BreakLabelName -> listOf("break")
                            is CatchLabelName -> listOf("catch")
                            is ContinueLabelName -> listOf("cont")
                            is ReturnLabelName -> listOf("ret")
                            is TryExitLabelName -> listOf("tryExit")
                        }

                        is PlaceholderArgumentName -> listOf("arg")
                        is ReturnVariableName -> listOf("ret")
                        is SsaVariableName -> resolveParts(name.baseName)
                    } + listOf(name.n.toString())

                    is PredicateName -> listOf(name.name)
                    DispatchReceiverName -> listOf($$"this$dispatch")
                    is DomainAssociatedFuncName -> listOf(name.name)
                    is DomainFuncParameterName -> listOf(name.name)
                    ExtensionReceiverName -> listOf($$"this$extension")
                    FunctionResultVariableName -> listOf("result")
                    is HavocName -> resolveParts(name.type.name)
                    PlaceholderReturnVariableName -> listOf("ret")
                    is SpecialFieldName -> listOf(name.name)
                }

                is KotlinName -> when (name) {
                    is ClassKotlinName -> resolveParts(name.nameType, skipType) + listOf(name.name.asViperString())
                    is ConstructorKotlinName -> resolveParts(name.nameType, skipType) + resolveParts(name.type.name)
                    is SimpleKotlinName -> listOf(name.name.asStringStripSpecialMarkers())
                    is TypedKotlinName -> resolveParts(
                        name.nameType,
                        skipType
                    ) + listOf(name.name.asStringStripSpecialMarkers())

                    is TypedKotlinNameWithType -> resolveParts(
                        name.nameType,
                        skipType
                    ) + listOf(name.name.asStringStripSpecialMarkers()) + resolveParts(
                        name.type.name
                    )
                }

                is ScopedName -> (name.nameType?.let { resolveParts(it, skipType) } ?: emptyList()) + resolveParts(
                    name.scope,
                    skipScope = skipScope
                ) + resolveParts(name.name, name.nameType == name.name.nameType, skipScope)

                is DomainName -> resolveParts(name.nameType, skipType) + listOf(name.baseName)
                is NamedDomainAxiomLabel -> resolveParts(name.domainName, true) + listOf(name.baseName)
                is QualifiedDomainFuncName -> resolveParts(name.nameType, skipType) + resolveParts(
                    name.domainName,
                    true
                ) + resolveParts(
                    name.funcName
                )

                is UnqualifiedDomainFuncName -> listOf(name.baseName)
                is ListOfNames<*> -> name.names.flatMap { resolveParts(it, skipType, skipScope) }
                is NameOfType -> when (name) {
                    is FunctionTypeName -> resolveParts(
                        name.args,
                        skipType = false,
                        skipScope = skipScope
                    ) + resolveParts(
                        name.returns,
                        skipType = false,
                        skipScope = skipScope
                    )

                    is PretypeName -> listOf(name.name)
                    is TypeName -> resolveParts(name.nameType, skipType) + listOfNotNull(buildString {
                        if (name.nullable) append("N")
                        if (name.pretype is FunctionTypeEmbedding) append("F")
                    }.ifEmpty { null }) + resolveParts(name.pretype.name, skipType = true, skipScope = true)
                }

                else -> throw SnaktInternalException(null, "Unexpected name type: ${name::class.simpleName}")
            }

            is NameType -> if (skipType) emptyList() else when (name) {
                NameType.Member.Property -> listOf("p")
                NameType.Member.BackingField -> listOf("bf")
                NameType.Member.Getter -> listOf("g")
                NameType.Member.Setter -> listOf("s")
                NameType.Member.ExtensionSetter -> listOf("es")
                NameType.Member.ExtensionGetter -> listOf("eg")
                NameType.TypeCategory.GeneralType -> listOf("t")
                NameType.TypeCategory.Class -> listOf("c")
                NameType.Base.Constructor -> listOf("con")
                NameType.Base.Function -> listOf("f")
                NameType.Base.Predicate -> listOf("pred")
                NameType.Base.Havoc -> listOf("havoc")
                NameType.Base.Label -> listOf("lbl")
                NameType.Base.Variable -> listOf("v")
                NameType.Base.Domain -> listOf("d")
                NameType.Base.DomainFunction -> listOf("df")
            }

            else -> throw SnaktInternalException(null, "Unexpected name type: ${name::class.simpleName}")
        }

        fun debugResolve(name: SymbolicName): String = resolveParts(name).joinToString(SEPARATOR)
    }

    override fun lookup(name: SymbolicName): String = cache.getOrPut(
        name
    ) { resolveParts(name).joinToString(SEPARATOR) }

    override fun resolve() {}

    override fun register(name: NamedEntity) {}
}


val SymbolicName.debugMangled: String
    get() {
        return SimpleNameResolver.debugResolve(this)
    }
