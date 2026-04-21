package org.jetbrains.kotlin.formver.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.embeddings.types.FunctionTypeEmbedding
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.*

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

    val cache = mutableMapOf<AnyName, String>()

    enum class NameTypeAction {
        SHOW,
        SKIP;

        fun show() = this == SHOW
    }
    companion object {
        context(nameTypeAction: NameTypeAction)
        private fun resolveParts(
            name: AnyName
        ): List<String> = when (name) {
            is NameScope -> resolveNameScope(name)
            is SymbolicName -> resolveSymbolicName(name)
            is NameType -> if (nameTypeAction.show()) listOf(resolveNameType(name)) else emptyList()

            else -> throw SnaktInternalException(null, "Unexpected name type: ${name::class.simpleName}")
        }

        context(nameTypeAction: NameTypeAction)
        private fun resolveOptionalNameScope(name: NameScope?): List<String> =
            if (name == null) emptyList() else resolveNameScope(name)

        context(nameTypeAction: NameTypeAction)
        private fun resolveNameScope(
            name: NameScope
        ): List<String> {

            // Since every public field with the same name should be matched to the same string, we should not add the
            // parent scope (which is the class, making it non-unique)
            val parentScopes = if (name !is PublicScope) {
                resolveOptionalNameScope(name.parent)
            } else {
                emptyList()
            }
            val scope = when (name) {
                is BadScope -> throw SnaktInternalException(null, "BadScope should never be resolved")
                is ClassScope -> resolveParts(name.className)
                is FakeScope -> emptyList()
                is LocalScope -> listOf("l${name.level}")
                is PackageScope -> {
                    if (name.packageName.isRoot) {
                        emptyList()
                    } else {
                        listOf($$"pkg$$${name.packageName.asViperString()}")
                    }
                }

                is ParameterScope -> listOf("par")
                is PrivateScope -> listOf("private")
                is PublicScope -> listOf("public")
            }
            return parentScopes + scope
        }

        context(nameTypeAction: NameTypeAction)
        private fun resolveSymbolicName(
            name: SymbolicName
        ): List<String> = when (name) {
            is FreshName -> resolveFreshName(name)
            is KotlinName -> resolveKotlinName(name)
            is ScopedName -> listOfNotNull(resolveOptionalNameType(name.nameType)) + resolveParts(
                name.scope
            ) + with(if (name.nameType == name.name.nameType) NameTypeAction.SKIP else nameTypeAction) {
                resolveParts(
                    name.name
                )
            }

            is DomainName -> resolveParts(name.nameType) + listOf(name.baseName)
            is NamedDomainAxiomLabel -> with(NameTypeAction.SKIP) { resolveParts(name.domainName) } + listOf(name.baseName)
            is QualifiedDomainFuncName -> resolveParts(name.nameType) + with(NameTypeAction.SKIP) {
                resolveParts(
                    name.domainName
                )
            } + resolveParts(name.funcName)

            is UnqualifiedDomainFuncName -> listOf(name.baseName)
            is ListOfNames<*> -> name.names.flatMap { resolveParts(it) }
            is FunctionTypeName -> with(NameTypeAction.SHOW) {
                resolveParts(
                    name.args
                ) + resolveParts(
                    name.returns
                )
            }

            is PretypeName -> listOf(name.name)
            is AdtName -> resolveParts(name.nameType) + resolveParts(name.className)
            is AdtConstructorName -> resolveParts(name.nameType) +
                    with(NameTypeAction.SKIP) { resolveParts(name.adtName) } +
                    resolveParts(name.className)
            is AdtInjectionName -> resolveParts(name.className) + listOf(name.suffix)
            is TypeName -> resolveParts(name.nameType) + listOfNotNull(buildString {
                if (name.nullable) append("N")
                if (name.pretype is FunctionTypeEmbedding) append("F")
            }.ifEmpty { null }) + with(NameTypeAction.SKIP) { resolveParts(name.pretype.name) }

            else -> throw SnaktInternalException(null, "Unexpected name type: ${name::class.simpleName}")
        }

        context(nameTypeAction: NameTypeAction)
        private fun resolveFreshName(name: FreshName): List<String> {
            val typeParts =
                name.nameType.takeIf { it != NameType.Base.Variable }.let { resolveOptionalNameType(it) }
            val nameParts = when (name) {
                is NumberedName -> resolveNumberedName(name)
                is PredicateName -> listOf(name.name)
                DispatchReceiverName -> listOf("this", "dispatch")
                is DomainAssociatedFuncName -> listOf(name.name)
                is DomainFuncParameterName -> listOf(name.name)
                ExtensionReceiverName -> listOf("this", "extension")
                FunctionResultVariableName -> listOf("result")
                is HavocName -> resolveParts(name.type.name)
                PlaceholderReturnVariableName -> listOf("ret")
                is SpecialFieldName -> listOf(name.name)
            }
            return listOfNotNull(typeParts) + nameParts
        }

        context(nameTypeAction: NameTypeAction)
        private fun resolveNumberedName(name: NumberedName): List<String> = when (name) {
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

        context(nameTypeAction: NameTypeAction)
        private fun resolveKotlinName(name: KotlinName): List<String> = when (name) {
            is ClassKotlinName -> resolveParts(name.nameType) + listOf(name.name.asViperString())
            is ConstructorKotlinName -> resolveParts(name.nameType) + resolveParts(name.type.name)
            is SimpleKotlinName -> listOf(name.name.asStringStripSpecialMarkers())
            is TypedKotlinName -> resolveParts(
                name.nameType
            ) + listOf(name.name.asStringStripSpecialMarkers())

            is TypedKotlinNameWithType -> resolveParts(
                name.nameType
            ) + listOf(name.name.asStringStripSpecialMarkers()) + resolveParts(name.type.name)
        }

        context(nameTypeAction: NameTypeAction)
        private fun resolveOptionalNameType(name: NameTypeBase?): String? =
            if (name == null || !nameTypeAction.show()) null else {
                resolveNameType(name)
            }

        private fun resolveNameType(name: NameTypeBase): String = when (name) {
            NameType.Member.Property -> "p"
            NameType.Member.BackingField -> "bf"
            NameType.Member.Getter -> "g"
            NameType.Member.Setter -> "s"
            NameType.Member.ExtensionSetter -> "es"
            NameType.Member.ExtensionGetter -> "eg"
            NameType.TypeCategory.GeneralType -> "t"
            NameType.TypeCategory.Class -> "c"
            NameType.Base.Constructor -> "con"
            NameType.Base.Function -> "f"
            NameType.Base.Predicate -> "pred"
            NameType.Base.Havoc -> "havoc"
            NameType.Base.Label -> "lbl"
            NameType.Base.Variable -> "v"
            NameType.Base.Domain -> "d"
            NameType.Base.DomainFunction -> "df"
            NameType.Base.Adt -> "adt"
            NameType.Base.AdtCons -> "adtc"
            is NameTypeBase -> throw SnaktInternalException(
                null, "NameTypeBase should never been inherited by something else than NameType"
            )
        }

        fun debugResolve(name: SymbolicName): String = with(NameTypeAction.SHOW) {
            resolveParts(name).joinToString(SEPARATOR)
        }
    }

    override fun lookup(name: SymbolicName): String = cache.getOrPut(
        name
    ) {
        with(NameTypeAction.SHOW) {
            resolveParts(name).joinToString(SEPARATOR)
        }
    }

    override fun resolve() {}

    override fun register(name: AnyName) {}
}


val SymbolicName.debugMangled: String
    get() {
        return SimpleNameResolver.debugResolve(this)
    }
