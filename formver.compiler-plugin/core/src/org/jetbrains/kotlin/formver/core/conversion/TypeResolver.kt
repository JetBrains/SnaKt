package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.names.NameMatcher
import org.jetbrains.kotlin.formver.viper.SymbolicName

class TypeResolver {
    /**
     * Collection of all ClassTypeEmbeddings and InterfaceTypeEmbeddings
     */
    private val classEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()
    private val interfaceEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()
    private val embedding
        get() = classEmbedding + interfaceEmbedding


    /**
     * Supertype relation. Key is a subtype, value is a set of supertypes.
     * The transitive closure is not included.
     */
    private val superTypes = mutableMapOf<SymbolicName, MutableSet<SymbolicName>>()

    /**
     * All the fields. Key is the pair of the class name and the field name.
     */
    private val fields = mutableMapOf<ClassPropertyPair, FieldEmbedding>()

    /**
     * Register a class or interface type embedding.
     * This is needed to know which classes were already registered.
     */
    fun register(typeEmbedding: ClassTypeEmbedding, isInterface: Boolean) = when (isInterface) {
        true -> interfaceEmbedding.putIfAbsent(typeEmbedding.name, typeEmbedding)
        false -> classEmbedding.putIfAbsent(typeEmbedding.name, typeEmbedding)
    }

    fun isRegistered(name: SymbolicName) = embedding.containsKey(name)


    fun embeddings() = embedding.values.toList()

    fun lookupEmbedding(name: SymbolicName) = embedding[name]

    /**
     * Extends the subtype relation with [subtype] <: [supertype]
     */
    fun addSubtypeRelation(subtype: SymbolicName, supertype: SymbolicName) = superTypes.getOrPut(subtype) {
        mutableSetOf()
    }.add(supertype)


    /**
     * Returns the set of super types of a given class or interface.
     * These are only the direct super types, transitive closure is not included.
     */
    fun lookupSuperTypes(name: SymbolicName) = superTypes.getOrDefault(name, emptySet()).mapNotNull { embedding[it] }

    /**
     * Adds a field to the class.
     */
    fun addFieldToClass(name: ClassPropertyPair, fieldEmbedding: FieldEmbedding) {
        fields.getOrPut(name) {
            fieldEmbedding
        }
    }

    fun fields() = fields.map { it.value }.toList()

    /**
     * Returns the field embedding for a given [ClassPropertyPair]
     */
    fun lookupField(name: ClassPropertyPair) = fields[name]

    /**
     * Returns all the fields belonging directly to a class.
     */
    fun lookupClassFields(name: SymbolicName) = fields.filterKeys { it.first == name }.values.toList()


    /**
     * Collects all the fields belonging to a class and its super types.
     * They are unique with regard to their name.
     */
    private fun collectFields(className: SymbolicName): List<FieldEmbedding> {
        return lookupSuperTypes(className).fold(lookupClassFields(className)) { acc, type ->
            acc + collectFields(type.name)
        }.distinctBy { it.name }
    }

    fun <R> flatMapUniqueFields(
        className: SymbolicName,
        action: (FieldEmbedding) -> List<R>
    ): List<R> = collectFields(className).flatMap { action(it) }


    /**
     * Returns the sequence of class types that are between the [typeEmbedding] and the [field].
     */
    fun hierarchyPathTo(
        typeEmbedding: PretypeEmbedding,
        field: FieldEmbedding
    ): Sequence<ClassTypeEmbedding> = sequence {
        val classType = (typeEmbedding as? ClassTypeEmbedding) ?: return@sequence
        val className = field.containingClass?.name
        require(className != null) { "Cannot find hierarchy unfold path of a field with no class information" }
        if (className == typeEmbedding.name) {
            yield(classType)
        } else {
            val sup = lookupSuperTypes(classType.name).firstOrNull { !interfaceEmbedding.containsKey(it.name) }
                ?: throw IllegalArgumentException("Reached top of the hierarchy without finding the field")

            yield(classType)
            yieldAll(hierarchyPathTo(sup, field))
        }
    }

    /**
     * Returns true if the [pretypeEmbedding] is a subtype of Collection with [name]
     */
    fun isInheritorOfCollectionTypeNamed(pretypeEmbedding: PretypeEmbedding, name: String): Boolean {
        val classEmbedding = pretypeEmbedding as? ClassTypeEmbedding ?: return false
        return classEmbedding.isCollectionTypeNamed(name) || lookupSuperTypes(classEmbedding.name).any {
            isInheritorOfCollectionTypeNamed(it, name)
        }
    }

    fun isCollectionInheritor(pretype: PretypeEmbedding) = isInheritorOfCollectionTypeNamed(pretype, "Collection")

    fun PretypeEmbedding.isCollectionTypeNamed(name: String): Boolean {
        val classEmbedding = this as? ClassTypeEmbedding ?: return false
        NameMatcher.matchGlobalScope(classEmbedding.name) {
            ifInCollectionsPkg {
                ifClassName(name) {
                    return true
                }
            }
            return false
        }
    }

}
