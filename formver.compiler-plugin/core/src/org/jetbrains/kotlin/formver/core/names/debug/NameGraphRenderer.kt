package org.jetbrains.kotlin.formver.core.names.debug

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.*
import kotlin.math.absoluteValue

class NameGraphRenderer(val shortNameResolver: ShortNameResolver) {

    /**
     * Relations are stored as triples of the form (name, relation, name).
     */
    private enum class Relation {

        /**
         * The following name
         * ``scopedName = ScopedName(scope, _)``
         * results in the relation
         * ``scope SCOPE_OF scopedName``
         **/
        SCOPE_OF,

        /**
         * The following name
         * ``scopedName = ScopedName(_, symbolicName)``
         * results in the relation
         * ``symbolicName SCOPE_OF scopedName``
         **/
        SCOPED_BY,

        /**
         * This relation is used when a name is associated with a type.
         * For example, the constructor name contains the function type name.
         * functionType TYPE_OF constructorName
         */
        TYPE_OF,

        /**
         * This relation is used when a name contains another name.
         * For example, an SsaVariable is a numbered version of the initial variable name.
         * initialVariable IS_PART_OF SsaVariable
         **/
        IS_PART_OF,

        /**
         * This relation is used when a name is associated with `NameType`.
         * For example, the name of a backing field is associated with the NameType backing field
         *
         * NameType KIND_OF name
         */
        KIND_OF,
    }

    /**
     * Stores all the relations between names.
     */
    private val tripleStore = mutableSetOf<Triple<AnyName, Relation, AnyName>>()

    /**
     * Adds a relation between two entities.
     */
    private fun link(a: AnyName, rel: Relation, b: AnyName) {
        tripleStore.add(Triple(a, rel, b))

    }


    /**
     * This method generates a dot graph of the current state of the short name resolver.
     *
     * The graph shows the current candidate for each name, and the dependencies between them.
     *
     * If `showCollisions` is true, the graph will show which names collide with each other.
     *
     * If `showCurrent` is true, the graph will also show the current candidate for each name.
     */
    fun render(showCollisions: Boolean = true, showCurrent: Boolean = true): String {

        if (tripleStore.isEmpty()) registerRelation()

        val nl = "\\n"

        /**
         * Returns the candidate Name, but not recursively resolved. Recursive Names are replaced with
         * placeholders.
         */
        fun candidateNameTemplate(candidate: CandidateName): String = candidate.parts.joinToString() {
            when (it) {
                is NamePart.Basic -> it.name
                is NamePart.Dependent -> when (it.name) {
                    is NameScope -> "<scope>"
                    is NameType -> "<type>"
                    is SymbolicName -> when (it.name) {
                        is FreshName -> "<fresh>"
                        is KotlinName -> "<kotlin>"
                        is ScopedName -> "<Kscope>"
                        is DomainName -> "<domain>"
                        is PretypeName -> "<typeName>"
                        else -> "<other>"
                    }

                    else -> it.name.toString()
                }

                NamePart.Separator -> SEPARATOR
            }
        }

        fun candidates(entity: AnyName): String = entity.candidates().joinToString(nl) { candidateNameTemplate(it) }

        fun id(entity: AnyName): String {
            val hashCode = entity.hashCode() + entity::class.qualifiedName.hashCode()
            return "n${hashCode.absoluteValue}"
        }

        fun nodeColor(entity: AnyName): String = if (entity.inViper) {
            "lime"
        } else {
            "black"
        }


        val sb = StringBuilder()
        sb.appendLine("digraph NameSystem {")
        sb.appendLine("node [shape=box];")


        fun nodeLabel(entity: AnyName): String {
            var res = ""
            if (showCurrent) res += shortNameResolver.current(entity) + "$nl---$nl"
            res += candidates(entity)
            return res
        }

        // Helper for labels
        fun label(entity: AnyName): String = when (entity) {
            is NameScope -> "Scope: "
            is NameType -> "Type: "
            is SymbolicName -> {
                if (entity.nameType != null) {
                    entity.nameType!!.fullName() + ": "
                } else {
                    when (entity) {
                        is FreshName -> "Fresh: "
                        is KotlinName -> "Kotlin: "
                        is ScopedName -> "Scoped: "
                        is PretypeName -> "PretypeName: "
                        is DomainName -> "Domain: "
                        is NamedDomainAxiomLabel -> "Axiom: "
                        is QualifiedDomainFuncName -> "Qual Func: "
                        is UnqualifiedDomainFuncName -> "Unqual Func: "
                        else -> "Unknown: "
                    }
                }
            }

            is ViperKeyword -> "Viper Keyword: "
            else -> throw SnaktInternalException(null, "Unsupported entity type: ${entity.javaClass.name}")
        } + entity.fullName() + nl + nodeLabel(entity)


        fun relationLabel(relation: Relation) = when (relation) {
            Relation.SCOPE_OF -> "[color=blue]"
            Relation.IS_PART_OF -> "[color=black]"
            Relation.TYPE_OF -> "[color=pink]"
            Relation.SCOPED_BY -> "[color=lightblue]"
            Relation.KIND_OF -> "[color=green]"
        }


        // add all elements
        shortNameResolver.elements().forEach {
            sb.appendLine("\"${id(it)}\" [label=\"${label(it)}\" color=\"${nodeColor(it)}\"];\n")
        }

        if (showCollisions) {
            val collisions = shortNameResolver.collisions()
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

    private fun registerRelation() {
        shortNameResolver.elements().forEach { registerRelationEntity(it) }
    }

    private fun registerRelationFreshName(entity: FreshName) {
        when (entity) {
            is SsaVariableName -> {
                link(entity.baseName, Relation.IS_PART_OF, entity)
            }

            is HavocName -> {
                link(entity.type.name, Relation.IS_PART_OF, entity)
            }
            // No else branch, because FreshName is sealed. When adding a new name, the compiler will complain here.
            FunctionResultVariableName, PlaceholderReturnVariableName, ExtensionReceiverName, DispatchReceiverName,
            is AnonymousBuiltinName, is AnonymousName, is PlaceholderArgumentName,
            is ReturnVariableName, is BreakLabelName, is CatchLabelName,
            is ContinueLabelName, is TryExitLabelName, is ReturnLabelName,
            is DomainAssociatedFuncName, is DomainFuncParameterName, is PredicateName, is SpecialFieldName -> {
            }
        }
    }

    private fun registerRelationEntity(entity: AnyName) {
        when (entity) {
            is NameScope -> {
                entity.parent?.let {
                    link(it, Relation.SCOPE_OF, entity)
                }
                if (entity is ClassScope) {
                    link(entity.className, Relation.SCOPED_BY, entity)
                }
            }

            // Handled by addElement above
            is NameType -> {}

            is SymbolicName -> {
                entity.nameType?.let {
                    link(it, Relation.KIND_OF, entity)
                }
                when (entity) {
                    is FreshName -> registerRelationFreshName(entity)
                    is ScopedName -> {
                        link(entity.name, Relation.SCOPED_BY, entity)
                        link(entity.scope, Relation.SCOPE_OF, entity)
                    }

                    is ConstructorKotlinName -> {
                        link(entity.type.name, Relation.TYPE_OF, entity)
                    }

                    is TypedKotlinNameWithType -> {
                        link(entity.type.name, Relation.TYPE_OF, entity)
                    }

                    is NamedDomainAxiomLabel -> {
                        link(entity.domainName, Relation.IS_PART_OF, entity)
                    }

                    is QualifiedDomainFuncName -> {
                        link(entity.domainName, Relation.IS_PART_OF, entity)
                        link(entity.funcName, Relation.IS_PART_OF, entity)
                    }

                    is ListOfNames<*> -> {
                        entity.names.forEach {
                            link(it, Relation.IS_PART_OF, entity)
                        }
                    }

                    is FunctionTypeName -> {
                        link(entity.args, Relation.IS_PART_OF, entity)
                        link(entity.returns, Relation.IS_PART_OF, entity)
                    }

                    is TypeName -> {
                        link(entity.pretype.name, Relation.IS_PART_OF, entity)
                    }

                    is SimpleKotlinName, is ClassKotlinName, is TypedKotlinName, is DomainName, is UnqualifiedDomainFuncName, is PretypeName, is FunctionResultVariableName -> {}
                    else -> {
                        throw SnaktInternalException(null, "Unsupported entity type: ${entity.javaClass.name}")
                    }
                }
            }
        }
    }


}
