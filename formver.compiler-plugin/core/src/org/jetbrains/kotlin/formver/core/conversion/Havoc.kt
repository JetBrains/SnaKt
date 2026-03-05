package org.jetbrains.kotlin.formver.core.conversion

import org.jetbrains.kotlin.formver.core.domains.MethodBuilder
import org.jetbrains.kotlin.formver.core.domains.RuntimeTypeDomain
import org.jetbrains.kotlin.formver.core.embeddings.expression.PlaceholderVariableEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.ClassTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PretypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.PrimitivePreTypeEmbedding
import org.jetbrains.kotlin.formver.core.embeddings.types.TypeEmbeddingFlags
import org.jetbrains.kotlin.formver.core.embeddings.types.asTypeEmbedding
import org.jetbrains.kotlin.formver.core.linearization.pureToViper
import org.jetbrains.kotlin.formver.core.names.*
import org.jetbrains.kotlin.formver.viper.ast.Exp
import org.jetbrains.kotlin.formver.viper.ast.Method
import org.jetbrains.kotlin.formver.viper.ast.Stmt
import org.jetbrains.kotlin.formver.viper.ast.Type

/**
 * The Havoc object should be used for everything that is related to havoc. This includes:
 * - Creating havoc methods
 * - Creating havoc method calls
 *
 * The Havoc object is a factory creating ``HavocMethodCallBuilder``. These builders are used to perform a havoc call.
 * There are currently two kind of builders:
 * - ``PrimitiveHavocMethodCallBuilder`` used for types which do not require a predicate to be accessed (this includes int, char, strings)
 * - `ClassHavocMethodCallBuilder` used for class types.
 *
 * There are two different types of havoc methods.
 * 1. For primitive types, this method is stored in the `Havoc` object.
 * 2. For classes. For each class there are two havoc methods, one for the case where the class might be null
 * and the other for when the class is not null.
 * The class havoc methods are stored in the corresponding ``pretype`` details.
 * Nevertheless, they should be called through the Builder infrastructure.
 *
 */
object Havoc {

    // Start of the Factory section
    /**
     * The ``HavocMethodCallBuilder`` is used to create havoc calls.
     * They can be created using the `Havoc` object.
     * First the flags and the target (the variable to which the result should be written to) must be set, before calling `.build()`
     */
    abstract class HavocMethodCallBuilder {
        val target: Exp.LocalVar
            get() = _target ?: throw IllegalStateException("Target not set")

        val flags: TypeEmbeddingFlags
            get() = _flags ?: throw IllegalStateException("Flags not set")

        private var _target: Exp.LocalVar? = null
        private var _flags: TypeEmbeddingFlags? = null

        fun withTarget(target: Exp.LocalVar): HavocMethodCallBuilder {
            _target = target
            return this
        }

        fun withFlags(flags: TypeEmbeddingFlags): HavocMethodCallBuilder {
            _flags = flags
            return this
        }

        abstract fun build(): Stmt.MethodCall
    }

    class PrimitiveHavocMethodCallBuilder(private val preType: PretypeEmbedding) : HavocMethodCallBuilder() {
        override fun build(): Stmt.MethodCall =
            getPrimitiveMethod().toMethodCall(listOf(flags.adjustRuntimeType(preType.runtimeType)), listOf(target))
    }

    class ClassHavocMethodCallBuilder(private val preType: ClassTypeEmbedding) : HavocMethodCallBuilder() {
        override fun build(): Stmt.MethodCall {
            val method = if (flags.nullable) {
                preType.details.havocMethodNullable
            } else {
                preType.details.havocMethod
            }
            return method.toMethodCall(emptyList(), listOf(target))
        }
    }

    fun getCallBuilder(preType: PrimitivePreTypeEmbedding): HavocMethodCallBuilder = PrimitiveHavocMethodCallBuilder(preType)

    fun getCallBuilder(preType: ClassTypeEmbedding): HavocMethodCallBuilder = ClassHavocMethodCallBuilder(preType)

    // End of the Factory section



    // Start of Method creation
    private val primitiveHavocMethod = MethodBuilder.build(
        SpecialName("havoc")
    ) {
        argument {
            RuntimeTypeDomain.RuntimeType
        }
        returns {
            Type.Ref
        }
        postcondition {
            RuntimeTypeDomain.isSubtype(
                RuntimeTypeDomain.typeOf(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref)),
                args[0]
            )
        }
    }

    fun getClassMethod(classType: ClassTypeEmbedding, nullable: Boolean): Method {
        val result = PlaceholderVariableEmbedding(PlaceholderReturnVariableName, classType.asTypeEmbedding())
        val methodName = if (nullable) {
            ScopedKotlinName(classType.name.asScope(), HavocKotlinName("havocNullable"))
        } else {
            ScopedKotlinName(classType.name.asScope(), HavocKotlinName("havoc"))
        }

        val accessPredicate = classType.sharedPredicateAccessInvariant().fillHole(result).pureToViper(toBuiltin = true)
        val accessPostcondition = if (nullable) {
            Exp.Implies(
                Exp.NeCmp(Exp.LocalVar(PlaceholderReturnVariableName, Type.Ref), RuntimeTypeDomain.nullValue()),
                accessPredicate
            )
        } else {
            accessPredicate
        }

        val isSubtypeType = classType.runtimeType.run {
            if (nullable) {
                RuntimeTypeDomain.nullable(this)
            } else {
                this
            }
        }

        return MethodBuilder.build(methodName) {
            returns {
                Type.Ref
            }
            postcondition {
                accessPostcondition
            }
            postcondition {
                RuntimeTypeDomain.isSubtype(
                    RuntimeTypeDomain.typeOf(
                        Exp.LocalVar(
                            PlaceholderReturnVariableName, Type.Ref
                        )
                    ), isSubtypeType
                )
            }
        }
    }

    fun getPrimitiveMethod(): Method = primitiveHavocMethod

    // End of Method creation

}