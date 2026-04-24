package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassEmbeddingDetails
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName

class TypeResolver {

    private val classTypes = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    private val interfaceTypes = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    private val types
        get() = classTypes + interfaceTypes

    private val superTypes = mutableMapOf<SymbolicName, Set<SymbolicName>>()

    private val details = mutableMapOf<SymbolicName, ClassEmbeddingDetails>()

    private val classToFields = mutableMapOf<SymbolicName, Set<Pair<SymbolicName, FieldEmbedding>>>()


    /**
     * Maps the name of a field to the name of a class it belongs to.
     */
    val fieldToClassMapping = mutableMapOf<SymbolicName, SymbolicName>()


    fun extendsTypeHierarchy(subtype: SymbolicName, supertype: List<SymbolicName>) {
        superTypes.merge(subtype, supertype.toSet()) { a, b -> a + b }
    }

    fun addFieldToClass(klass: SymbolicName, fieldName: SymbolicName, fieldEmbedding: FieldEmbedding) {
        classToFields.merge(klass, setOf(Pair(fieldName, fieldEmbedding))) { a, b -> (a + b) }
    }

    fun register(name: SymbolicName, typeEmbedding: ClassTypeEmbedding, isInterface: Boolean) = when (isInterface) {
        true -> interfaceTypes.putIfAbsent(name, typeEmbedding)
        false -> classTypes.putIfAbsent(name, typeEmbedding)

    }

    fun lookupSuperTypes(name: SymbolicName) = superTypes.getOrDefault(name, emptySet()).mapNotNull { types[it] }
    fun lookupClassType(name: SymbolicName) = types[name]
    fun classTypes() = classTypes.values

    fun finish(name: SymbolicName) {
        require(!details.containsKey(name)) { "Class type details for $name have already been finalized" }
        details[name] = details(name)
    }

    fun isRegistered(name: SymbolicName) = types.containsKey(name)

    fun hasDetails(name: SymbolicName) = details.containsKey(name)


    fun allDetails(): List<ClassEmbeddingDetails> = types.values.map { details(it.name) }
    fun details(name: SymbolicName): ClassEmbeddingDetails = details.getOrPut(name)
    {
        ClassEmbeddingDetails(
            type = types[name]!!,
            fields = (classToFields[name] ?: emptySet()).associate { it.first to it.second },
            classSuperTypes = lookupSuperTypes(name).map {
                details(it.name)
            },
            isInterface = interfaceTypes.containsKey(name),
            preType = types[name]!!,
            this
        )
    }
}
