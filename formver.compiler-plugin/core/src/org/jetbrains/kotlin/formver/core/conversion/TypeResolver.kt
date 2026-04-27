package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassPredicateBuilder
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeInvariantEmbedding
import org.jetbrains.kotlin.formver.core.names.PropertyKotlinName
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.PermExp
import org.jetbrains.kotlin.formver.viper.ast.Predicate

class TypeResolver {

    private val classEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    private val interfaceEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    private val embedding
        get() = classEmbedding + interfaceEmbedding

    private val superTypes = mutableMapOf<SymbolicName, MutableSet<SymbolicName>>()

    private val classToFields = mutableMapOf<ClassPropertyPair, FieldEmbedding>()

    fun addSubtypeRelation(subtype: SymbolicName, supertype: SymbolicName) = superTypes.getOrPut(subtype) {
        mutableSetOf()
    }.add(supertype)


    fun addFieldToClass(name: ClassPropertyPair, fieldEmbedding: FieldEmbedding) {
        classToFields.getOrPut(name) {
            fieldEmbedding
        }
    }

    fun register(name: SymbolicName, typeEmbedding: ClassTypeEmbedding, isInterface: Boolean) = when (isInterface) {
        true -> interfaceEmbedding.putIfAbsent(name, typeEmbedding)
        false -> classEmbedding.putIfAbsent(name, typeEmbedding)
    }

    fun lookupSuperTypes(name: SymbolicName) = superTypes.getOrDefault(name, emptySet()).mapNotNull { embedding[it] }
    fun lookupClassType(name: SymbolicName) = embedding[name]
    fun lookupField(name: ClassPropertyPair) = classToFields[name]
    fun lookupClassFields(name: SymbolicName) = classToFields.filterKeys { it.first == name }.values.toList()
    fun lookupClassFieldsWithNames(name: SymbolicName) =
        classToFields.filterKeys { it.first == name }.map { Pair(it.key.second, it.value) }


    fun embedding(name: SymbolicName) = embedding[name]
    fun allEmbeddings() = embedding.values

    fun isRegistered(name: SymbolicName) = embedding.containsKey(name)

    private fun collectFields(className: SymbolicName): Set<Pair<PropertyKotlinName, FieldEmbedding>> {
        return lookupSuperTypes(className).fold(lookupClassFieldsWithNames(className).toSet()) { acc, type ->
            acc + collectFields(type.name)
        }
    }

    fun <R> flatMapUniqueFields(
        className: SymbolicName,
        action: (PropertyKotlinName, FieldEmbedding) -> List<R>
    ): List<R> = collectFields(className).flatMap { action(it.first, it.second) }


    fun accessInvariantsOfClass(className: SymbolicName): List<TypeInvariantEmbedding> =
        flatMapUniqueFields(className) { _, field ->
            field.accessInvariantsForParameter()
        }


    fun hierarchyPathTo(
        classType: ClassTypeEmbedding,
        field: FieldEmbedding,
        ctx: TypeResolver
    ): Sequence<ClassTypeEmbedding> = sequence {
        val className = field.containingClass?.name
        require(className != null) { "Cannot find hierarchy unfold path of a field with no class information" }
        if (className == classType.name) {
            yield(classType)
        } else {
            val sup = lookupSuperTypes(classType.name).firstOrNull { !interfaceEmbedding.containsKey(it.name) }
                ?: throw IllegalArgumentException("Reached top of the hierarchy without finding the field")

            yield(classType)
            yieldAll(hierarchyPathTo(sup, field, ctx))
        }
    }

    fun sharedPredicate(classType: ClassTypeEmbedding): Predicate =
        ClassPredicateBuilder.build(classType.name, classType.sharedPredicateName, this) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                    forType {
                        addAccessToSharedPredicate(this@TypeResolver)
                        includeSubTypeInvariants()
                    }
                }
            }
            forEachSuperType {
                addAccessToSharedPredicate(this@TypeResolver)
            }
        }

    fun uniquePredicate(classType: ClassTypeEmbedding): Predicate =
        ClassPredicateBuilder.build(classType.name, classType.uniquePredicateName, this) {
            forEachField {
                if (isAlwaysReadable) {
                    addAccessPermissions(PermExp.WildcardPerm())
                } else {
                    addAccessPermissions(PermExp.FullPerm())
                }
                forType {
                    addAccessToSharedPredicate(this@TypeResolver)
                    if (isUnique) {
                        addAccessToUniquePredicate(this@TypeResolver)
                    }
                    includeSubTypeInvariants()
                }
            }
            forEachSuperType {
                addAccessToUniquePredicate(this@TypeResolver)
            }
        }

    fun allClassAccessPredicates(): List<Predicate> =
        allEmbeddings().flatMap { listOf(sharedPredicate(it), uniquePredicate(it)) }
}
