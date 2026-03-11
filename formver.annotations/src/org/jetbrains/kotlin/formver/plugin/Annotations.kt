/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

/**
 * Excludes the annotated function from Viper conversion entirely.
 *
 * Neither conversion nor verification is performed for this function. Use this annotation on functions
 * that contain side effects, I/O, or other constructs irrelevant to the formal verification of
 * surrounding code.
 *
 * This annotation takes precedence over project-level `conversionTargetsSelection` and
 * `verificationTargetsSelection` settings.
 */
annotation class NeverConvert

/**
 * Converts the annotated function to Viper but skips the verification step.
 *
 * The generated Viper program is produced (and can be inspected with `logLevel`) but the Silicon
 * verifier is not invoked. Useful for inspecting the generated Viper without waiting for the SMT
 * solver, or for temporarily disabling verification of a known-failing function.
 *
 * This annotation takes precedence over the project-level `verificationTargetsSelection` setting.
 */
annotation class NeverVerify

/**
 * Always submits the annotated function to the Viper verifier, regardless of project-level settings.
 *
 * Functions that would normally be excluded from verification (for example, because they have no
 * Kotlin `contract { }` block and the project uses `targets_with_contract` selection) are verified
 * when annotated with this annotation.
 *
 * This annotation takes precedence over the project-level `verificationTargetsSelection` setting.
 */
annotation class AlwaysVerify

/**
 * Debug annotation: causes the plugin to emit the intermediate `ExpEmbedding` tree for the annotated
 * function as a compiler info diagnostic.
 *
 * Useful when investigating incorrect Viper output and you want to inspect the intermediate IR stage
 * between FIR and the Viper AST. The embedding tree is printed in a human-readable indented format.
 */
annotation class DumpExpEmbeddings

/**
 * Marks a value as having unique (exclusive) ownership.
 *
 * A `@Unique` value may be *consumed* (moved) exactly once. Passing a `@Unique` value to a function
 * that expects a `@Unique` parameter transfers ownership to the callee. After consumption the original
 * binding may not be used again; doing so produces a `UNIQUENESS_VIOLATION` diagnostic.
 *
 * When applied to a property, the backing field uses exclusive Viper predicates instead of shared
 * read access.
 *
 * Use together with [@Borrowed] to allow a function to use a unique value without consuming it.
 *
 * @see Borrowed
 */
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class Unique

/**
 * Marks a `@Unique` parameter as borrowed: the function uses the value without consuming it.
 *
 * After a call to a `@Borrowed` function the caller retains ownership of the unique value and may
 * continue to use it. A `@Borrowed` value may **not** be passed to a consuming (non-borrowed) `@Unique`
 * parameter; doing so produces a `UNIQUENESS_VIOLATION` diagnostic.
 *
 * @see Unique
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Borrowed

/**
 * Declares that the annotated function has no observable side effects.
 *
 * The plugin verifies this claim at compile time: if the function body contains any impure constructs
 * (heap writes, calls to non-pure functions, loops, etc.) an `INTERNAL_ERROR` diagnostic is produced.
 *
 * Pure functions are translated to Viper **function** declarations rather than **method** declarations,
 * which allows them to appear inside specifications: `preconditions { }`, `postconditions { }`,
 * `loopInvariants { }`, and `verify()`.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Pure

/**
 * Applied as `@property:Manual`. Opts a specific property out of the automatic Viper permission system.
 *
 * The programmer is responsible for inserting the correct Viper access predicates manually. Files that
 * use `@Manual` properties typically include `// NEVER_VALIDATE` to skip end-to-end permission checking,
 * since the automatic consistency check cannot verify manually managed heap permissions.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Manual
