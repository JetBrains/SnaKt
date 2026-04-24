package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.embeddings.properties.FieldEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassEmbeddingDetails
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.viper.SymbolicName

class TypeResolver {

    private val classEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    private val interfaceEmbedding = mutableMapOf<SymbolicName, ClassTypeEmbedding>()

    private val embedding
        get() = classEmbedding + interfaceEmbedding

    private val superTypes = mutableMapOf<SymbolicName, MutableSet<SymbolicName>>()

    private val embeddingDetails = mutableMapOf<SymbolicName, ClassEmbeddingDetails>()

    private val classToFields = mutableMapOf<SymbolicName, MutableSet<Pair<SymbolicName, FieldEmbedding>>>()

    fun addSubtypeRelation(subtype: SymbolicName, supertype: SymbolicName) =
        superTypes.getOrPut(subtype) {
            mutableSetOf()
        }.add(supertype)


    fun addFieldToClass(klass: SymbolicName, fieldName: SymbolicName, fieldEmbedding: FieldEmbedding) {
        classToFields.getOrPut(klass) {
            mutableSetOf()
        }.add(fieldName to fieldEmbedding)
    }

    fun register(name: SymbolicName, typeEmbedding: ClassTypeEmbedding, isInterface: Boolean) = when (isInterface) {
        true -> interfaceEmbedding.putIfAbsent(name, typeEmbedding)
        false -> classEmbedding.putIfAbsent(name, typeEmbedding)
    }

    fun lookupSuperTypes(name: SymbolicName) = superTypes.getOrDefault(name, emptySet()).mapNotNull { embedding[it] }
    fun lookupClassType(name: SymbolicName) = embedding[name]
    fun classTypes() = classEmbedding.values


    fun embedding(name: SymbolicName) = embedding[name]
    fun allEmbeddings() = embedding.values

    fun isRegistered(name: SymbolicName) = embedding.containsKey(name)

    fun hasDetails(name: SymbolicName) = embeddingDetails.containsKey(name)

    fun allDetails(): List<ClassEmbeddingDetails> = embedding.values.map { details(it.name) }
    fun details(name: SymbolicName): ClassEmbeddingDetails = embeddingDetails.getOrPut(name)
    {
        val fields = classToFields[name] ?: emptySet()
        val fieldMapping = fields.associate { it.first to it.second }
        ClassEmbeddingDetails(
            type = embedding[name]!!,
            fields = fieldMapping,
            classSuperTypes = lookupSuperTypes(name),
            isInterface = interfaceEmbedding.containsKey(name)
        )
    }
}
