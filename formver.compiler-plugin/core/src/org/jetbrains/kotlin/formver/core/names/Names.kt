package org.jetbrains.kotlin.formver.core.names


interface ScopeName : OurName
interface TypeName : OurName
interface OurName {
    val candidates: List<(Resolver) -> String>
    val numCandidates: Int
        get() = candidates.size

    fun candidate(num: Int, resolver: Resolver): String = candidates[num](resolver)
    fun parents(): Set<OurName>
    fun shortName(): String
    fun longName(): String
}

interface Register<Ret : Register<Ret>> : OurName {
    fun register(): Ret {
        CreatedNames.add(this)
        return this as Ret
    }
}

// SCOPES
data class BasicName(val name: String) : Register<BasicName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = emptySet()
    override fun shortName() = name
    override fun longName() = name
}

data class PackageName(val parent: ScopeName, val name: BasicName) : ScopeName, Register<PackageName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(parent)
    override fun shortName() = name.shortName()
    override fun longName() = "${parent.longName()}.${name.shortName()}"
}

object PackageRoot : ScopeName, Register<PackageRoot> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() }
    )

    override fun parents(): Set<OurName> = emptySet()
    override fun shortName() = "root"
    override fun longName() = "root"
}

// CLASS
data class ClassName(val parent: ScopeName, val name: BasicName) : ScopeName, TypeName, Register<ClassName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "class_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(parent)}_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(parent)}_class_${shortName()}" },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(parent)
    override fun shortName() = name.shortName()
    override fun longName() = "${parent.longName()}.${name.shortName()}"
}

interface FieldName : OurName


data class UserFieldName(val klass: ClassName, val name: BasicName) : FieldName, Register<UserFieldName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "field_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(klass)}_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(klass)}_field_${shortName()}" },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(klass)
    override fun shortName() = name.shortName()
    override fun longName() = "${klass.longName()}.${name.shortName()}"
}

data class BackingFieldName(val klass: ClassName, val name: BasicName) : FieldName, Register<BackingFieldName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "bfield_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(klass)}_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(klass)}_bfield_${shortName()}" },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(klass)
    override fun shortName() = name.shortName()
    override fun longName() = "${klass.longName()}.${name.shortName()}"
}

data class FieldGetter(val field: FieldName) : Register<FieldGetter> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${resolver.currentCandidate(field)}_${shortName()}" },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(field)
    override fun shortName() = "get"
    override fun hashCode(): Int = field.hashCode() + "get".hashCode()
    override fun longName() = "${field.longName()}.get"
}

data class FieldSetter(val field: FieldName) : Register<FieldSetter> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${resolver.currentCandidate(field)}_${shortName()}" },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(field)
    override fun shortName() = "set"
    override fun hashCode(): Int = field.hashCode() + "set".hashCode()
    override fun longName() = "${field.longName()}.set"

}

data class Constructor(val klass: ClassName, val type: FunctionType) : Register<Constructor> {
    override fun parents(): Set<OurName> = setOf(klass, type)
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${resolver.currentCandidate(klass)}_${shortName()}" },
        { resolver -> "${resolver.currentCandidate(type)}_${shortName()}" },
        { resolver -> longName() }
    )


    override fun shortName() = "con"
    override fun longName() = "${klass.longName()}.con"

}


// TYPES
data class PrimitiveType(val name: BasicName) : TypeName, Register<PrimitiveType> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = emptySet()

    override fun shortName() = name.shortName()
    override fun longName() = "type_${name.shortName()}"
}

data class ComplexType(val inner: TypeName, val nullable: Boolean) : TypeName, Register<ComplexType> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = setOf(inner)
    override fun shortName() = "${inner.shortName()}${if (nullable) "?" else "!"}"
    override fun longName() = "${inner.longName()}${if (nullable) "?" else "!"}"
}

data class FunctionType(val args: List<TypeName>, val returnType: TypeName) : TypeName, Register<FunctionType> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver ->
            "(" + args.joinToString("$") { resolver.currentCandidate(it) } + ")_to_" + resolver.currentCandidate(
                returnType
            )
        },
        { resolver -> longName() }
    )

    override fun parents(): Set<OurName> = (args + returnType).toSet()
    override fun shortName(): String = "funType"
    override fun longName() = "funType(${args.joinToString(", ") { it.longName() }}) -> ${returnType.longName()}"
}


// TYPE DOMAIN
interface TypeDomainTypesName : OurName
data class TypeDomainName(val name: BasicName) : Register<TypeDomainName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = emptySet()
    override fun shortName(): String = name.shortName()
    override fun longName() = "TypeDomain${name.shortName()}"
}

data class RuntimeTypeName(val domain: TypeDomainName, val name: BasicName) : TypeDomainTypesName,
    Register<RuntimeTypeName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = setOf(domain)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${domain.longName()}.${name.shortName()}"
}

data class AxiomName(val domain: TypeDomainName, val name: BasicName) : Register<AxiomName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = setOf(domain)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${domain.longName()}.${name.shortName()}"
}

data class PrimitiveViperTypeNames(val name: BasicName) : TypeDomainTypesName, Register<PrimitiveViperTypeNames> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = emptySet()
    override fun shortName(): String = name.shortName()
    override fun longName() = name.shortName()
}

data class TypeDomainFunctionType(val args: List<TypeDomainTypesName>, val returnType: TypeDomainTypesName) :
    Register<TypeDomainFunctionType> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = (args + returnType).toSet()
    override fun shortName(): String = "funType"
    override fun longName() = "funType(${args.joinToString(", ") { it.longName() }}) -> ${returnType.longName()}"
}

data class TypeDomainFunction(val domain: TypeDomainName, val name: BasicName, val type: TypeDomainFunctionType) :
    Register<TypeDomainFunction> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = setOf(domain, type)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${domain.longName()}.${name.shortName()}"
}

interface TypeDomainFunctionArgument : OurName
data class TypeDomainFunctionArgumentNamed(
    val function: TypeDomainFunction,
    val name: BasicName,
    val type: TypeDomainTypesName
) : TypeDomainFunctionArgument, Register<TypeDomainFunctionArgumentNamed> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = setOf(function, type)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${function.longName()}.${name.shortName()}"
}

data class TypeDomainFunctionArgumentNumbered(
    val function: TypeDomainFunction,
    val num: Int,
    val type: TypeDomainTypesName
) : TypeDomainFunctionArgument, Register<TypeDomainFunctionArgumentNumbered> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> longName() },
    )

    override fun parents(): Set<OurName> = setOf(function, type)
    override fun shortName(): String = "arg${num}"
    override fun longName() = "${function.longName()}.${shortName()}"
}


// Methods
data class MethodName(val scope: ScopeName, val name: BasicName, val type: FunctionType) : ScopeName,
    Register<MethodName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${shortName()}_returns_${resolver.currentCandidate(type.returnType)}" },
        { resolver -> "${resolver.currentCandidate(scope)}${shortName()}_returns_${resolver.currentCandidate(type.returnType)}" },
    )

    override fun parents(): Set<OurName> = setOf(scope, type)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${scope.longName()}.${name.shortName()}"
}

data class ArgumentName(val method: MethodName, val name: BasicName, val type: TypeName) : Register<ArgumentName> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${shortName()}_${resolver.currentCandidate(type)}" },
        { resolver -> "${resolver.currentCandidate(method)}_${shortName()}_${resolver.currentCandidate(type)}" },
    )

    override fun parents(): Set<OurName> = setOf(method, type)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${method.longName()}.${name.shortName()}"

}

data class MethodScope(val parent: ScopeName, val level: Int) : ScopeName, Register<MethodScope> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${resolver.currentCandidate(parent)}_${shortName()}" },

        )

    override fun parents(): Set<OurName> = setOfNotNull(parent).toSet()
    override fun shortName(): String = "scope${level}"
    override fun longName() = "${parent.longName()}scope${level}"
}

data class LocalVariable(val methodScope: MethodScope, val name: BasicName, val type: TypeName) :
    Register<LocalVariable> {
    override val candidates: List<(Resolver) -> String> = listOf(
        { resolver -> shortName() },
        { resolver -> "${shortName()}_${resolver.currentCandidate(type)}" },
        { resolver -> "${resolver.currentCandidate(methodScope)}${shortName()}_${resolver.currentCandidate(type)}" },
    )

    override fun parents(): Set<OurName> = setOf(methodScope, type)
    override fun shortName(): String = name.shortName()
    override fun longName() = "${methodScope.longName()}.${name.shortName()}"
}

// LABELS

