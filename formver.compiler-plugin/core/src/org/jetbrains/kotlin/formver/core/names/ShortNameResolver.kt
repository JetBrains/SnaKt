package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.*
import org.jetbrains.kotlin.formver.viper.ast.DomainName
import org.jetbrains.kotlin.formver.viper.ast.NamedDomainAxiomLabel
import org.jetbrains.kotlin.formver.viper.ast.QualifiedDomainFuncName
import org.jetbrains.kotlin.formver.viper.ast.UnqualifiedDomainFuncName
import kotlin.math.absoluteValue

// START UTILITY SECTION

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun NamePart.name(): String = when (this) {
    is NamePart.Dependent -> name()
    is NamePart.Basic -> this.name
}

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun NamePart.Dependent.name(): String = resolver.current(name)

/**
 * Gives the current name.
 */
context(resolver: ShortNameResolver)
fun CandidateName.name(): String = parts.joinToString(SEPARATOR) { it.name() }


/**
 * Gives the most specific name.
 */
context(resolver: ShortNameResolver)
fun NamePart.fullName(): String = when (this) {
    is NamePart.Dependent -> fullName()
    is NamePart.Basic -> this.name
}

/**
 * Gives the most specific name.
 */
context(resolver: ShortNameResolver)
fun NamePart.Dependent.fullName(): String = name.candidates.last().fullName()

/**
 * Gives the most specific name.
 */
context(resolver: ShortNameResolver)
fun CandidateName.fullName(): String = parts.joinToString(SEPARATOR) { it.fullName() }

// END UTILITY SECTION

internal enum class Relation {

    /** scope SCOPE_OF scopedName **/
    SCOPE_OF,

    /** name SCOPED_BY nameScoped **/
    SCOPED_BY,

    /** Int TYPE_OF Function**/
    TYPE_OF,

    /** a IS_PART_OF (a,b) -> c **/
    IS_PART_OF,

    /** name NAME_TYPE nameType **/
    KIND_OF,

    REPRESENTED_BY,
}


/**
 * Resolves mangled names into Viper identifiers while maintaining uniqueness.
 * The priority lies on the short and readable names.
 */
class ShortNameResolver : NameResolver {

    /**
     * This Mapping stores the mangled names. It is only initialized after calling `mangle()`
     */
    private lateinit var mangledNames: Map<NamedEntity, String>


    /**
     * Stores which candidate is currently used for a given name.
     * Since the name can depend on other names, recursive lookups may be necessary.
     */
    private val currentCandidate = mutableMapOf<NamedEntity, Int>()

    /**
     * Stores all the names that exist in the system.
     */
    private val elements: MutableSet<NamedEntity> = ViperKeywords.keywords.toMutableSet()
    internal fun addElement(entity: NamedEntity) = elements.add(entity)

    /**
     * All names that appear in Viper. 
     */
    internal fun viperElements() = elements.filter { endUpInViper(it) }

    /**
     * Stores all the relations between names.
     */
    private val tripleStore = mutableSetOf<Triple<NamedEntity, Relation, NamedEntity>>()

    /**
     * Adds a relation between two entities.
     */
    internal fun link(a: NamedEntity, rel: Relation, b: NamedEntity) {
        tripleStore.add(Triple(a, rel, b))
        addElement(a)
        addElement(b)
    }

    /**
     * Returns true iff the ``entity`` is scoped. Meaning that there is a name, which wraps around `entity`
     */
    private fun isScoped(entity: NamedEntity): Boolean =
        tripleStore.any { (a, rel, _) -> a == entity && rel == Relation.SCOPED_BY }

    /**
     * Returns the entities `elem` is dependent on.
     */
    fun dependsOn(entity: NamedEntity): Set<NamedEntity> =
        tripleStore.filter { (a, rel, _) -> a == entity && rel == Relation.IS_PART_OF }.map { (_, _, b) -> b }.toSet()

    fun isRepresented(entity: NamedEntity): Boolean = representedBy(entity) != null
    fun representedBy(entity: NamedEntity): NamedEntity? =
        tripleStore.find { (a, rel, _) -> a == entity && rel == Relation.REPRESENTED_BY }?.third


    private val registrator = Registrator()
    override fun register(name: SymbolicName) = registrator.visit(name, this)


    // START RESOLVE NAMES
    fun fullName(entity: NamedEntity): String = entity.candidates.last().fullName()

    override fun resolve(name: SymbolicName): String = mangledNames.getOrElse(name) {
        throw SnaktInternalException(null, "Name not resolved: $name")
    }

    fun current(entity: NamedEntity): String {
        if (entity.candidates.isEmpty()) {
            print("empty candidates")
        }
        return currentCandidate(entity).name()
    }

    private fun currentCandidate(name: NamedEntity): CandidateName {
        var representative = name
        // find the root of representation
        while (true) {
            representative = representedBy(representative) ?: break
        }
        return name.candidates[currentCandidate[representative] ?: 0]
    }
    // END RESOLVE NAMES


    // START MANGLE
    override fun mangle() {
        createRepresentatives()
        var collisions = collisions()
        while (collisions.isNotEmpty()) {
//            val graphviz = graph.toGraphviz(this, showCollisions = true, showCurrent = true)
            val toResolve = collisions.entries.first().value

            val candidateToMove = toResolve.firstOrNull { canMove(it) }

            if (candidateToMove != null) {
                move(candidateToMove)
            } else {
                throw SnaktInternalException(null, "Unable to make names unique")
            }
            collisions = collisions()
        }

        // Fix the names
        mangledNames = viperElements().associateWith { current(it) }
    }

    private fun createRepresentatives() {
        // Field Names which are public
        val publicFields = elements.filter {
            it is ScopedKotlinName && it.scope is PublicScope
        }
        // Group them according to their name
        val grouped = mutableMapOf<KotlinName, Set<ScopedKotlinName>>()
        publicFields.forEach {
            grouped.merge((it as ScopedKotlinName).name, setOf(it), Set<ScopedKotlinName>::plus)
        }
        // The first is the representative.
        // TODO: if one of them already has a representative, this must be taken into account. But can this even happen?
        grouped.forEach { (_, fields) ->
            val representative = fields.first()
            fields.drop(1).forEach {
                link(it, Relation.REPRESENTED_BY, representative)
            }
        }
    }

    /**
     * Returns the name collisions of the current candidates.
     * 
     * Which names must be considered?
     * - We do need to make all the names unique, but only once that actually end up in viper. E.g., a variable and
     *   a scope can have the same name.
     * - Sometimes we want to have name collisions, e.g., public fields with the same name can be merged. This behavior
     *   is captured with the representation system. Hence, names that are represented by someone else must not be considered.
     */
    fun collisions(): Map<String, Set<NamedEntity>> {
        val toConsider = elements.filter { endUpInViper(it) && !isRepresented(it) }
        val names = toConsider.map { Pair(current(it), it) }
        val result = mutableMapOf<String, MutableSet<NamedEntity>>()
        names.forEach { (name, entity) ->
            result.getOrPut(name) { mutableSetOf() }.add(entity)
        }
        return result.filterValues { it.size > 1 }
    }

    /**
     * Returns true iff the ``entity`` could end up in viper.
     */
    private fun endUpInViper(entity: NamedEntity): Boolean = when (entity) {
        is NameScope -> false // Just scopes should never appear as an actual name in viper
        is NameType -> false // Name Types are only used to distinguish what a name describes
        is SymbolicName -> {
            // maybe
            when (entity) {
                is FreshName -> true  // likely yes
                is KotlinName -> !isScoped(entity)
                is ScopedKotlinName -> (!isScoped(entity) && entity.name !is ClassKotlinName) || true
                is DomainName -> true // yes the domain is used as a name
                is NamedDomainAxiomLabel -> true
                is QualifiedDomainFuncName -> true
                is UnqualifiedDomainFuncName -> false
                is NameOfType -> false // names of types are not necessary to be unique
                else -> true
            }
        }

        is ViperKeyword -> true

        else -> false
    }


    /**
     * Moves `name` one position.
     * We generally try to move first the parts, and only if not successful do we move the `name`
     */
    private fun move(entity: NamedEntity): Boolean {
        val movableParts = currentCandidate(entity).moveableParts()
        // maybe some priority sorting?
        for (part in movableParts) {
            if (move(part.name)) return true
        }

        if (canMove(entity)) {
            currentCandidate[entity] = (currentCandidate[entity] ?: 0) + 1
            return true
        }
        return false
    }

    /**
     * Returns true if `name` can be moved or one of the parts of the current candidate can be moved
     */
    private fun canMove(entity: NamedEntity): Boolean {
        val currentIndex = currentCandidate[entity] ?: 0
        if (currentIndex + 1 < entity.candidates.size) return true
        return currentCandidate(entity).moveableParts().any {
            canMove(it.name)
        }
    }

    // END MANGLE


    // START RENDER

    fun render(showCollisions: Boolean = true, showCurrent: Boolean = true): String {
        // BEGIN HELPER

        val nl = "\\n"

        /**
         * Returns the candidate Name, but not recursively resolved. Recursive Names are replaced with
         * placeholders.
         */
        fun candidateNameTemplate(candidate: CandidateName): String = candidate.parts.joinToString(SEPARATOR) {
            when (it) {
                is NamePart.Basic -> it.name
                is NamePart.Dependent -> when (it.name) {
                    is NameScope -> "<scope>"
                    is NameType -> "<type>"
                    is SymbolicName -> when (it.name) {
                        is FreshName -> "<fresh>"
                        is KotlinName -> "<kotlin>"
                        is ScopedKotlinName -> "<Kscope>"
                        is DomainName -> "<domain>"
                        is NameOfType -> "<typeName>"
                        else -> "<other>"
                    }

                    else -> it.name.toString()
                }
            }
        }

        fun candidates(entity: NamedEntity): String = entity.candidates.joinToString(nl) { candidateNameTemplate(it) }

        fun id(entity: NamedEntity): String {
            val hashCode = entity.hashCode() + entity::class.qualifiedName.hashCode()
            val name = "n${hashCode.absoluteValue}"
            return name
        }

        fun nodeColor(entity: NamedEntity): String =
            if (endUpInViper(entity)) {
                "lime"
            } else {
                "black"
        }


        val sb = StringBuilder()
        sb.appendLine("digraph NameSystem {")
        sb.appendLine("node [shape=box];")


        fun nodeLabel(entity: NamedEntity): String {
            var res = ""
            if (showCurrent) res += current(entity) + "$nl---$nl"
            res += candidates(entity)
            return res
        }

        // Helper for labels
        fun label(entity: NamedEntity): String = when (entity) {
            is NameScope -> "Scope: "
            is NameType -> "Type: "
            is SymbolicName -> {
                if (entity.nameType != null) {
                    entity.nameType!!.name + ": "
                } else {
                    when (entity) {
                        is FreshName -> "Fresh: "
                        is KotlinName -> "Kotlin: "
                        is ScopedKotlinName -> "Scoped: "
                        is NameOfType -> "NameOfType: "
                        is DomainName -> "Domain: "
                        is NamedDomainAxiomLabel -> "Axiom: "
                        is QualifiedDomainFuncName -> "Qual Func: "
                        is UnqualifiedDomainFuncName -> "Unqual Func: "
                        else -> "Unknown: "
                    }
                }
            }
            is ViperKeyword -> "Viper Keyword: "
            else -> throw IllegalArgumentException("Unsupported entity type: ${entity.javaClass.name}")
        } + fullName(entity) + nl + nodeLabel(entity)


        fun relationLabel(relation: Relation) = when (relation) {
            Relation.SCOPE_OF -> "[color=blue]"
            Relation.IS_PART_OF -> "[color=black]"
            Relation.TYPE_OF -> "[color=pink]"
            Relation.SCOPED_BY -> "[color=lightblue]"
            Relation.KIND_OF -> "[color=green]"
            Relation.REPRESENTED_BY -> "[color=purple]"
        }


        // add all elements
        elements.forEach {
            sb.appendLine("\"${id(it)}\" [label=\"${label(it)}\" color=\"${nodeColor(it)}\"];\n")
        }

        if (showCollisions) {
            val collisions = collisions()
            collisions.keys.forEach {
                sb.appendLine("\"$it\" [label=\"$it\" color=\"red\"];")
            }
            // add name nodes
            collisions.forEach { (key, names) ->
                names.forEach { sb.appendLine("\"${id(it)}\" -> \"$key\" [color=red];") }
            }
        }

        // relations
        tripleStore.forEach { (a, relation, b) ->
            sb.appendLine("\"${id(a)}\" -> \"${id(b)}\" ${relationLabel(relation)};\n")
        }

        sb.append("}\n")
        return sb.toString()
    }

    // END RENDER


}


internal class Registrator : NamedEntityVisitor<ShortNameResolver, Unit> {
    override val nameScopeVisitor: NameScopeVisitor<ShortNameResolver, Unit>
        get() = object : NameScopeVisitor<ShortNameResolver, Unit> {
            override fun visitBadScope(
                scope: BadScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
            }

            override fun visitClassScope(
                scope: ClassScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
                data.link(scope.parent, Relation.SCOPE_OF, scope)
                data.link(scope.className, Relation.SCOPED_BY, scope)
                scope.parent.accept(this@Registrator.nameScopeVisitor, data)
                scope.className.accept(this@Registrator.symbolicNameVisitor, data)
            }

            override fun visitFakeScope(
                scope: FakeScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
            }

            override fun visitLocalScope(
                scope: LocalScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
            }

            override fun visitPackageScope(
                scope: PackageScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
            }

            override fun visitParameterScope(
                scope: ParameterScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
            }

            override fun visitPrivateScope(
                scope: PrivateScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
                data.link(scope.parent, Relation.SCOPE_OF, scope)
                scope.parent.accept(this@Registrator.nameScopeVisitor, data)
            }

            override fun visitPublicScope(
                scope: PublicScope, data: ShortNameResolver
            ) {
                data.addElement(scope)
                data.link(scope.parent, Relation.SCOPE_OF, scope)
                scope.parent.accept(this@Registrator.nameScopeVisitor, data)
            }
        }

    override val nameTypeVisitor: NameTypeVisitor<ShortNameResolver, Unit>
        get() = object : NameTypeVisitor<ShortNameResolver, Unit> {
            override fun visit(type: NameType, data: ShortNameResolver) {
                data.addElement(type)
            }

            override fun visitProperty(type: NameType.Property, data: ShortNameResolver) {}
            override fun visitBackingField(type: NameType.BackingField, data: ShortNameResolver) {}
            override fun visitGetter(type: NameType.Getter, data: ShortNameResolver) {}
            override fun visitSetter(type: NameType.Setter, data: ShortNameResolver) {}
            override fun visitExtensionSetter(type: NameType.ExtensionSetter, data: ShortNameResolver) {}
            override fun visitExtensionGetter(type: NameType.ExtensionGetter, data: ShortNameResolver) {}
            override fun visitType(type: NameType.Type, data: ShortNameResolver) {}
            override fun visitTypeClass(type: NameType.Type.Class, data: ShortNameResolver) {}
            override fun visitConstructor(type: NameType.Constructor, data: ShortNameResolver) {}
            override fun visitFunction(type: NameType.Function, data: ShortNameResolver) {}
            override fun visitPredicate(type: NameType.Predicate, data: ShortNameResolver) {}
            override fun visitHavoc(type: NameType.Havoc, data: ShortNameResolver) {}
            override fun visitReturn(type: NameType.Label.Return, data: ShortNameResolver) {}
            override fun visitBreak(type: NameType.Label.Break, data: ShortNameResolver) {}
            override fun visitContinue(type: NameType.Label.Continue, data: ShortNameResolver) {}
            override fun visitCatch(type: NameType.Label.Catch, data: ShortNameResolver) {}
            override fun visitTryExit(type: NameType.Label.TryExit, data: ShortNameResolver) {}
            override fun visitVariables(type: NameType.Variables, data: ShortNameResolver) {}
            override fun visitDomain(type: NameType.Domain, data: ShortNameResolver) {}
            override fun visitDomainFunction(type: NameType.DomainFunction, data: ShortNameResolver) {}
            override fun visitSpecial(type: NameType.Special, data: ShortNameResolver) {}


        }
    override val symbolicNameVisitor: SymbolicNameVisitor<ShortNameResolver, Unit>
        get() = object : SymbolicNameVisitor<ShortNameResolver, Unit> {
            override fun visitAnonymousBuiltinName(
                name: AnonymousBuiltinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitAnonymousName(
                name: AnonymousName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitDispatchReceiverName(
                name: DispatchReceiverName, data: ShortNameResolver
            ) {
                data.addElement(name)
            }

            override fun visitDomainFuncParameterName(
                name: DomainFuncParameterName, data: ShortNameResolver
            ) {
                data.addElement(name)
            }

            override fun visitExtensionReceiverName(
                name: ExtensionReceiverName, data: ShortNameResolver
            ) {
                data.addElement(name)
            }

            override fun visitPlaceholderReturnVariableName(
                name: PlaceholderReturnVariableName,
                data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitFunctionResultVariableName(
                name: FunctionResultVariableName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitNumberedLabelName(
                name: NumberedLabelName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitPlaceholderArgumentName(
                name: PlaceholderArgumentName, data: ShortNameResolver
            ) {
                data.addElement(name)
            }

            override fun visitReturnVariableName(
                name: ReturnVariableName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitSpecialName(
                name: SpecialName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitSsaVariableName(
                name: SsaVariableName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.baseName, Relation.IS_PART_OF, name)
                name.baseName.accept(this@Registrator.symbolicNameVisitor, data)
            }

            override fun visitConstructorKotlinName(
                name: ConstructorKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.type.name, Relation.TYPE_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitHavocKotlinName(
                name: HavocKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.type.name, Relation.IS_PART_OF, name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.type.name.accept(this@Registrator.symbolicNameVisitor, data)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)

            }

            override fun visitPredicateKotlinName(
                name: PredicateKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitSimpleKotlinName(
                name: SimpleKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
            }

            override fun visitTypedKotlinNameWithType(
                name: TypedKotlinNameWithType, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.type.name, Relation.TYPE_OF, name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.type.name.accept(this@Registrator.symbolicNameVisitor, data)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitClassKotlinName(
                name: ClassKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitTypedKotlinName(
                name: TypedKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitScopedKotlinName(
                name: ScopedKotlinName, data: ShortNameResolver
            ) {
                data.addElement(name)
                name.nameType?.let {
                    data.link(it, Relation.KIND_OF, name)
                    it.accept(this@Registrator.nameTypeVisitor, data)
                }
                data.link(name.name, Relation.SCOPED_BY, name)
                name.name.accept(this@Registrator.symbolicNameVisitor, data)
                data.link(name.scope, Relation.SCOPE_OF, name)
                name.name.accept(this@Registrator.symbolicNameVisitor, data)
            }

            override fun visitDomainName(
                name: DomainName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitNamedDomainAxiomLabel(
                name: NamedDomainAxiomLabel, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.domainName, Relation.IS_PART_OF, name)
                name.domainName.accept(this@Registrator.symbolicNameVisitor, data)
            }

            override fun visitQualifiedDomainFuncName(
                name: QualifiedDomainFuncName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.domainName, Relation.IS_PART_OF, name)
                data.link(name.funcName, Relation.IS_PART_OF, name)
                data.link(name.nameType, Relation.KIND_OF, name)
                name.domainName.accept(this@Registrator.symbolicNameVisitor, data)
                name.funcName.accept(this@Registrator.symbolicNameVisitor, data)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitUnqualifiedDomainFuncName(
                name: UnqualifiedDomainFuncName, data: ShortNameResolver
            ) {
                data.addElement(name)
            }

            override fun visitListOfNames(
                name: ListOfNames<*>, data: ShortNameResolver
            ) {
                data.addElement(name)
                name.names.forEach {
                    data.link(
                        it, Relation.IS_PART_OF, name
                    ); it.accept(this@Registrator.symbolicNameVisitor, data)
                }
                data.link(name.nameType, Relation.KIND_OF, name)
                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
            }

            override fun visitFunctionTypeName(
                name: FunctionTypeName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.nameType, Relation.KIND_OF, name)
                data.link(name.args, Relation.IS_PART_OF, name)
                data.link(name.returns, Relation.IS_PART_OF, name)

                name.nameType.accept(this@Registrator.nameTypeVisitor, data)
                name.args.accept(this@Registrator.symbolicNameVisitor, data)
                name.returns.accept(this@Registrator.symbolicNameVisitor, data)
            }

            override fun visitPretypeName(
                name: PretypeName, data: ShortNameResolver
            ) {
                data.addElement(name)

            }

            override fun visitTypeName(
                name: TypeName, data: ShortNameResolver
            ) {
                data.addElement(name)
                data.link(name.pretype.name, Relation.IS_PART_OF, name)
                name.pretype.name.accept(this@Registrator.symbolicNameVisitor, data)

            }
        }
}
