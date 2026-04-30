/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.core.names

import org.jetbrains.kotlin.name.FqName

class ScopeBuilder {
    private var scope: NameScope? = null

    fun complete(): NameScope {
        require(scope != null) { "Empty scope can not be built" }
        return scope!!
    }

    fun packageScope(packageName: FqName) {
        require(scope == null) { "Invalid scope combination: package after $scope" }
        scope = PackageScope(packageName)
    }

    fun packageScope(packageName: List<String>) {
        require(scope == null) { "Invalid scope combination: package after $scope" }
        scope = PackageScope(FqName.fromSegments(packageName))
    }

    fun classScope(className: ClassKotlinName) {
        require(scope != null) { "Class scope cannot be top-level" }
        scope = ClassScope(scope!!, className)
    }

    fun publicScope() {
        require(scope == null) { "Public scope cannot be nested." }
        scope = PublicScope
    }

    fun privateScope() {
        require(scope is ClassScope) { "Private scope must be in a class scope." }
        scope = PrivateScope(scope!!)
    }

    fun parameterScope() {
        require(scope == null) { "Parameter scope cannot be nested." }
        scope = ParameterScope
    }

    fun localScope(level: Int) {
        require(scope == null) { "Local scope cannot be nested." }
        scope = LocalScope(level)
    }

    fun badScope() {
        require(scope == null)
        scope = BadScope
    }

    fun fakeScope() {
        require(scope == null) { "Fake scope cannot be nested." }
        scope = FakeScope
    }
}


class ScopedNameBuilder {
    private val scopeBuilder = ScopeBuilder()

    fun complete(name: KotlinName): ScopedName {
        val scope = scopeBuilder.complete()
        return ScopedName(scope, name)
    }

    fun packageScope(packageName: FqName) = scopeBuilder.packageScope(packageName)

    fun packageScope(packageName: List<String>) = scopeBuilder.packageScope(packageName)
    fun classScope(className: ClassKotlinName) = scopeBuilder.classScope(className)

    fun publicScope() = scopeBuilder.publicScope()

    fun privateScope() = scopeBuilder.privateScope()


    fun parameterScope() = scopeBuilder.parameterScope()

    fun localScope(level: Int) = scopeBuilder.localScope(level)

    fun badScope() = scopeBuilder.badScope()

    fun fakeScope() = scopeBuilder.fakeScope()
}

// TODO: generalise this to work for all names.
fun buildName(init: ScopedNameBuilder.() -> KotlinName): ScopedName = ScopedNameBuilder().run { complete(init()) }

fun buildScope(init: ScopeBuilder.() -> Unit) = ScopeBuilder().apply { init() }.complete()
