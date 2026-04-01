package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.formver.common.SnaktInternalException
import org.jetbrains.kotlin.formver.viper.NameType
import org.jetbrains.kotlin.formver.viper.NamedEntity
import org.jetbrains.kotlin.formver.viper.SymbolicName
import org.jetbrains.kotlin.formver.viper.ast.*

interface NamedEntityVisitor<D, R> {
    val nameScopeVisitor: NameScopeVisitor<D, R>
    val nameTypeVisitor: NameTypeVisitor<D, R>
    val symbolicNameVisitor: SymbolicNameVisitor<D, R>

    fun visit(entity: NamedEntity, data: D): R = when (entity) {
        is NameScope -> entity.accept(nameScopeVisitor, data)
        is NameType -> entity.accept(nameTypeVisitor, data)
        is SymbolicName -> entity.accept(symbolicNameVisitor, data)
        else -> {
            throw SnaktInternalException(null, "Unknown NamedEntity type: ${entity::class.simpleName}")
        }
    }
}

interface NameScopeVisitor<D, R> {

    fun visit(scope: NameScope, data: D): R = when (scope) {
        is BadScope -> visitBadScope(scope, data)
        is ClassScope -> visitClassScope(scope, data)
        is FakeScope -> visitFakeScope(scope, data)
        is LocalScope -> visitLocalScope(scope, data)
        is PackageScope -> visitPackageScope(scope, data)
        is ParameterScope -> visitParameterScope(scope, data)
        is PrivateScope -> visitPrivateScope(scope, data)
        is PublicScope -> visitPublicScope(scope, data)
    }

    fun visitBadScope(scope: BadScope, data: D): R
    fun visitClassScope(scope: ClassScope, data: D): R
    fun visitFakeScope(scope: FakeScope, data: D): R
    fun visitLocalScope(scope: LocalScope, data: D): R
    fun visitPackageScope(scope: PackageScope, data: D): R
    fun visitParameterScope(scope: ParameterScope, data: D): R
    fun visitPrivateScope(scope: PrivateScope, data: D): R
    fun visitPublicScope(scope: PublicScope, data: D): R
}

interface NameTypeVisitor<D, R> {

    fun visit(type: NameType, data: D): R = when (type) {
        is NameType.Property -> visitProperty(type, data)
        is NameType.BackingField -> visitBackingField(type, data)
        is NameType.Getter -> visitGetter(type, data)
        is NameType.Setter -> visitSetter(type, data)
        is NameType.ExtensionSetter -> visitExtensionSetter(type, data)
        is NameType.ExtensionGetter -> visitExtensionGetter(type, data)
        is NameType.Type.Class -> visitTypeClass(type, data)
        is NameType.Constructor -> visitConstructor(type, data)
        is NameType.Function -> visitFunction(type, data)
        is NameType.Domain -> visitDomain(type, data)
        is NameType.DomainFunction -> visitDomainFunction(type, data)
        is NameType.Havoc -> visitHavoc(type, data)
        is NameType.Label.Break -> visitBreak(type, data)
        is NameType.Label.Catch -> visitCatch(type, data)
        is NameType.Label.Continue -> visitContinue(type, data)
        is NameType.Label.Return -> visitReturn(type, data)
        is NameType.Label.TryExit -> visitTryExit(type, data)
        is NameType.Predicate -> visitPredicate(type, data)
        is NameType.Special -> visitSpecial(type, data)
        is NameType.Type -> visitType(type, data)
        is NameType.Variables -> visitVariables(type, data)
    }

    // Property related
    fun visitProperty(type: NameType.Property, data: D): R
    fun visitBackingField(type: NameType.BackingField, data: D): R
    fun visitGetter(type: NameType.Getter, data: D): R
    fun visitSetter(type: NameType.Setter, data: D): R
    fun visitExtensionSetter(type: NameType.ExtensionSetter, data: D): R
    fun visitExtensionGetter(type: NameType.ExtensionGetter, data: D): R

    // Class and Function related
    fun visitType(type: NameType.Type, data: D): R
    fun visitTypeClass(type: NameType.Type.Class, data: D): R
    fun visitConstructor(type: NameType.Constructor, data: D): R
    fun visitFunction(type: NameType.Function, data: D): R
    fun visitPredicate(type: NameType.Predicate, data: D): R
    fun visitHavoc(type: NameType.Havoc, data: D): R

    // Control Flow Labels
    fun visitReturn(type: NameType.Label.Return, data: D): R
    fun visitBreak(type: NameType.Label.Break, data: D): R
    fun visitContinue(type: NameType.Label.Continue, data: D): R
    fun visitCatch(type: NameType.Label.Catch, data: D): R
    fun visitTryExit(type: NameType.Label.TryExit, data: D): R

    // Logic/Domain related
    fun visitVariables(type: NameType.Variables, data: D): R
    fun visitDomain(type: NameType.Domain, data: D): R
    fun visitDomainFunction(type: NameType.DomainFunction, data: D): R
    fun visitSpecial(type: NameType.Special, data: D): R
}

interface SymbolicNameVisitor<D, R> {

    fun visit(name: SymbolicName, data: D): R = when (name) {
        is FreshName -> visitFreshName(name, data)
        is KotlinName -> visitKotlinName(name, data)
        is ScopedName -> visitScopedKotlinName(name, data)
        is DomainName -> visitDomainName(name, data)
        is NamedDomainAxiomLabel -> visitNamedDomainAxiomLabel(name, data)
        is QualifiedDomainFuncName -> visitQualifiedDomainFuncName(name, data)
        is UnqualifiedDomainFuncName -> visitUnqualifiedDomainFuncName(name, data)
        is HavocName -> visitHavocKotlinName(name, data)
        is PredicateName -> visitPredicateKotlinName(name, data)
        is NameOfType -> visitNameOfType(name, data)
        is RelatedDomainFuncName -> visitRelatedDomainFuncName(name, data)
        else -> {
            throw SnaktInternalException(null, "Unknown SymbolicName type: ${name::class.simpleName}")
        }
    }

    // Fresh names
    fun visitFreshName(name: FreshName, data: D): R = when (name) {
        is AnonymousBuiltinName -> visitAnonymousBuiltinName(name, data)
        is AnonymousName -> visitAnonymousName(name, data)
        is DispatchReceiverName -> visitDispatchReceiverName(name, data)
        is DomainFuncParameterName -> visitDomainFuncParameterName(name, data)
        is ExtensionReceiverName -> visitExtensionReceiverName(name, data)
        is FunctionResultVariableName -> visitFunctionResultVariableName(name, data)
        is NumberedLabelName -> visitNumberedLabelName(name, data)
        is PlaceholderArgumentName -> visitPlaceholderArgumentName(name, data)
        is ReturnVariableName -> visitReturnVariableName(name, data)
        is SpecialFieldName -> visitSpecialName(name, data)
        is SsaVariableName -> visitSsaVariableName(name, data)
        is PlaceholderReturnVariableName -> visitPlaceholderReturnVariableName(name, data)
        else -> {
            throw SnaktInternalException(null, "Unknown FreshName type: ${name::class.simpleName}")
        }
    }

    fun visitAnonymousBuiltinName(name: AnonymousBuiltinName, data: D): R
    fun visitAnonymousName(name: AnonymousName, data: D): R
    fun visitDispatchReceiverName(name: DispatchReceiverName, data: D): R
    fun visitDomainFuncParameterName(name: DomainFuncParameterName, data: D): R
    fun visitRelatedDomainFuncName(name: RelatedDomainFuncName, data: D): R
    fun visitExtensionReceiverName(name: ExtensionReceiverName, data: D): R
    fun visitPlaceholderReturnVariableName(name: PlaceholderReturnVariableName, data: D): R
    fun visitFunctionResultVariableName(name: FunctionResultVariableName, data: D): R
    fun visitNumberedLabelName(name: NumberedLabelName, data: D): R
    fun visitPlaceholderArgumentName(name: PlaceholderArgumentName, data: D): R
    fun visitReturnVariableName(name: ReturnVariableName, data: D): R
    fun visitSpecialName(name: SpecialFieldName, data: D): R
    fun visitSsaVariableName(name: SsaVariableName, data: D): R

    // Kotlin Specific Names
    fun visitKotlinName(name: KotlinName, data: D): R = when (name) {
        is ConstructorKotlinName -> visitConstructorKotlinName(name, data)
        is SimpleKotlinName -> visitSimpleKotlinName(name, data)
        is TypedKotlinNameWithType -> visitTypedKotlinNameWithType(name, data)
        is ClassKotlinName -> visitClassKotlinName(name, data)
        is TypedKotlinName -> visitTypedKotlinName(name, data)
    }

    fun visitConstructorKotlinName(name: ConstructorKotlinName, data: D): R
    fun visitHavocKotlinName(name: HavocName, data: D): R
    fun visitPredicateKotlinName(name: PredicateName, data: D): R
    fun visitSimpleKotlinName(name: SimpleKotlinName, data: D): R
    fun visitTypedKotlinNameWithType(name: TypedKotlinNameWithType, data: D): R
    fun visitClassKotlinName(name: ClassKotlinName, data: D): R
    fun visitTypedKotlinName(name: TypedKotlinName, data: D): R

    // Various Names
    fun visitScopedKotlinName(name: ScopedName, data: D): R
    fun visitDomainName(name: DomainName, data: D): R
    fun visitNamedDomainAxiomLabel(name: NamedDomainAxiomLabel, data: D): R
    fun visitQualifiedDomainFuncName(name: QualifiedDomainFuncName, data: D): R
    fun visitUnqualifiedDomainFuncName(name: UnqualifiedDomainFuncName, data: D): R

    // Type Names
    fun visitNameOfType(name: NameOfType, data: D): R = when (name) {
        is FunctionTypeName -> visitFunctionTypeName(name, data)
        is ListOfNames<*> -> visitListOfNames(name, data)
        is PretypeName -> visitPretypeName(name, data)
        is TypeName -> visitTypeName(name, data)
    }

    fun visitListOfNames(name: ListOfNames<*>, data: D): R
    fun visitFunctionTypeName(name: FunctionTypeName, data: D): R
    fun visitPretypeName(name: PretypeName, data: D): R
    fun visitTypeName(name: TypeName, data: D): R
}

// Name Scope
fun <D, R> NameScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visit(this, data)
fun <D, R> BadScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitBadScope(this, data)
fun <D, R> ClassScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitClassScope(this, data)
fun <D, R> FakeScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitFakeScope(this, data)
fun <D, R> LocalScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitLocalScope(this, data)
fun <D, R> PackageScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitPackageScope(this, data)
fun <D, R> ParameterScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitParameterScope(this, data)
fun <D, R> PrivateScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitPrivateScope(this, data)
fun <D, R> PublicScope.accept(visitor: NameScopeVisitor<D, R>, data: D): R = visitor.visitPublicScope(this, data)

// Name Type
fun <D, R> NameType.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visit(this, data)
fun <D, R> NameType.Property.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitProperty(this, data)
fun <D, R> NameType.BackingField.accept(visitor: NameTypeVisitor<D, R>, data: D): R =
    visitor.visitBackingField(this, data)

fun <D, R> NameType.Getter.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitGetter(this, data)
fun <D, R> NameType.Setter.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitSetter(this, data)
fun <D, R> NameType.ExtensionSetter.accept(visitor: NameTypeVisitor<D, R>, data: D): R =
    visitor.visitExtensionSetter(this, data)

fun <D, R> NameType.ExtensionGetter.accept(visitor: NameTypeVisitor<D, R>, data: D): R =
    visitor.visitExtensionGetter(this, data)

fun <D, R> NameType.Type.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitType(this, data)
fun <D, R> NameType.Type.Class.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitTypeClass(this, data)
fun <D, R> NameType.Constructor.accept(visitor: NameTypeVisitor<D, R>, data: D): R =
    visitor.visitConstructor(this, data)

fun <D, R> NameType.Function.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitFunction(this, data)
fun <D, R> NameType.Predicate.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitPredicate(this, data)
fun <D, R> NameType.Havoc.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitHavoc(this, data)

fun <D, R> NameType.Label.Return.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitReturn(this, data)
fun <D, R> NameType.Label.Break.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitBreak(this, data)
fun <D, R> NameType.Label.Continue.accept(visitor: NameTypeVisitor<D, R>, data: D): R =
    visitor.visitContinue(this, data)

fun <D, R> NameType.Label.Catch.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitCatch(this, data)
fun <D, R> NameType.Label.TryExit.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitTryExit(this, data)

fun <D, R> NameType.Variables.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitVariables(this, data)
fun <D, R> NameType.Domain.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitDomain(this, data)
fun <D, R> NameType.DomainFunction.accept(visitor: NameTypeVisitor<D, R>, data: D): R =
    visitor.visitDomainFunction(this, data)

fun <D, R> NameType.Special.accept(visitor: NameTypeVisitor<D, R>, data: D): R = visitor.visitSpecial(this, data)

// Symbolic Name
fun <D, R> SymbolicName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visit(this, data)
fun <D, R> FreshName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visitFreshName(this, data)
fun <D, R> AnonymousBuiltinName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitAnonymousBuiltinName(this, data)

fun <D, R> AnonymousName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visitAnonymousName(this, data)
fun <D, R> DispatchReceiverName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitDispatchReceiverName(this, data)

fun <D, R> DomainFuncParameterName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitDomainFuncParameterName(this, data)

fun <D, R> RelatedDomainFuncName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitRelatedDomainFuncName(this, data)

fun <D, R> ExtensionReceiverName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitExtensionReceiverName(this, data)

fun <D, R> FunctionResultVariableName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitFunctionResultVariableName(this, data)

fun <D, R> NumberedLabelName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitNumberedLabelName(this, data)

fun <D, R> PlaceholderArgumentName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitPlaceholderArgumentName(this, data)

fun <D, R> PlaceholderReturnVariableName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitPlaceholderReturnVariableName(this, data)

fun <D, R> ReturnVariableName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitReturnVariableName(this, data)

fun <D, R> SpecialFieldName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitSpecialName(this, data)
fun <D, R> SsaVariableName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitSsaVariableName(this, data)

fun <D, R> ConstructorKotlinName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitConstructorKotlinName(this, data)

fun <D, R> HavocName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitHavocKotlinName(this, data)

fun <D, R> PredicateName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitPredicateKotlinName(this, data)

fun <D, R> SimpleKotlinName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitSimpleKotlinName(this, data)

fun <D, R> TypedKotlinNameWithType.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitTypedKotlinNameWithType(this, data)

fun <D, R> ScopedName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitScopedKotlinName(this, data)

fun <D, R> NamedDomainAxiomLabel.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitNamedDomainAxiomLabel(this, data)

fun <D, R> QualifiedDomainFuncName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitQualifiedDomainFuncName(this, data)

fun <D, R> UnqualifiedDomainFuncName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitUnqualifiedDomainFuncName(this, data)

fun <D, R> NameOfType.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visitNameOfType(this, data)
fun <D, R> FunctionTypeName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R =
    visitor.visitFunctionTypeName(this, data)

fun <D, R> ListOfNames<*>.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visitListOfNames(this, data)
fun <D, R> PretypeName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visitPretypeName(this, data)
fun <D, R> TypeName.accept(visitor: SymbolicNameVisitor<D, R>, data: D): R = visitor.visitTypeName(this, data)