package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name


fun Name.basicName(): BasicName = BasicName(asString())

// Classes
fun ClassId.toClassName(): ClassName =
    when (val outer = outerClassId) {
        null -> ClassName(packageFqName.toPackageName(), shortClassName.basicName()).register()
        else -> ClassName(outer.toClassName(), shortClassName.basicName()).register()
    }

// Fields
fun FirPropertySymbol.toUserFieldName(): UserFieldName {
    return UserFieldName(callableId!!.classId!!.toClassName(), callableId!!.callableName.basicName()).register()
}

fun CallableId.toUserFieldName(): UserFieldName {
    return UserFieldName(classId!!.toClassName(), callableName.basicName()).register()
}

fun CallableId.embedGetter() {
    FieldGetter(toUserFieldName()).register()
}

fun CallableId.embedSetter() {
    FieldSetter(toUserFieldName()).register()
}

// Constructor

fun FirConstructorSymbol.toConstructorName() {
    val className = callableId.classId!!.toClassName()
    val parameters = valueParameterSymbols.map { it.resolvedReturnType.toTypeName() }
    Constructor(className, FunctionType(parameters, className).register()).register()
}

// Package
fun FqName.toPackageName(): ScopeName {
    if (isRoot) {
        return PackageRoot.register()
    }
    return PackageName(parent().toPackageName(), shortName().basicName()).register()
}


// Types
fun ClassId.toTypeName(): TypeName {
    return PrimitiveType(shortClassName.basicName()).register()
}

fun ConeKotlinType.toTypeName(): ComplexType {
    val primitiveType = when (this) {
        is ConeClassLikeTypeImpl -> lookupTag.classId.toTypeName()
        else -> PrimitiveType(BasicName("Unknown")).register()
    }
    return ComplexType(primitiveType, false).register() // todo
}

