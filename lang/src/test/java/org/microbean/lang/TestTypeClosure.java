/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.microbean.lang;

import java.lang.reflect.Field;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;

import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import com.sun.tools.javac.code.Type;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestTypeClosure {

  private TestTypeClosure() {
    super();
  }

  @Test
  final void testArray(final ProcessingEnvironment env) {
    final javax.lang.model.util.Elements elements = env.getElementUtils();
    final javax.lang.model.util.Types javacModelTypes = env.getTypeUtils();
    assertTrue(javacModelTypes instanceof JavacTypes);
    com.sun.tools.javac.code.Types javacTypes = null;
    try {
      final Field f = JavacTypes.class.getDeclaredField("types");
      assertTrue(f.trySetAccessible());
      javacTypes = (com.sun.tools.javac.code.Types)f.get(javacModelTypes);
    } catch (final ReflectiveOperationException reflectiveOperationException) {
      fail(reflectiveOperationException);
    }
    assertNotNull(javacTypes);

    final ArrayType integerArrayType = javacModelTypes.getArrayType(elements.getTypeElement("java.lang.Integer").asType());
    final List<Type> integerArrayClosure = javacTypes.closure((Type)integerArrayType);

    // This is weird and we don't support it.  See
    // https://stackoverflow.com/questions/73683649/in-the-javac-source-code-why-does-closuretype-return-a-non-empty-list-for-non.
    assertEquals(1, integerArrayClosure.size());
    assertSame(integerArrayType, integerArrayClosure.get(0));

    // Let's prove we don't support it.
    final TypeAndElementSource tes = new TypeAndElementSource() {
        @Override
        public final ArrayType arrayTypeOf(final TypeMirror componentType) {
          return javacModelTypes.getArrayType(componentType);
        }
        @Override
        public boolean assignable(final TypeMirror payload, final TypeMirror receiver) {
          return javacModelTypes.isAssignable(payload, receiver);
        }
        @Override
        public final TypeElement boxedClass(final PrimitiveType t) {
          return javacModelTypes.boxedClass(t);
        }
        @Override
        public final boolean contains(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.contains(t, s);
        }
        @Override
        public final DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... arguments) {
          return javacModelTypes.getDeclaredType(typeElement, arguments);
        }
        @Override
        public final DeclaredType declaredType(final DeclaredType enclosingType, final TypeElement typeElement, final TypeMirror... arguments) {
          return javacModelTypes.getDeclaredType(enclosingType, typeElement, arguments);
        }
        @Override
        public final List<? extends TypeMirror> directSupertypes(final TypeMirror t) {
          return javacModelTypes.directSupertypes(t);
        }
        @Override
        @SuppressWarnings("unchecked")
        public final <T extends TypeMirror> T erasure(final T t) {
          return (T)javacModelTypes.erasure(t);
        }
        @Override
        public final ModuleElement moduleElement(final CharSequence moduleName) {
          return elements.getModuleElement(moduleName);
        }
        @Override
        public final NoType noType(final TypeKind k) {
          return javacModelTypes.getNoType(k);
        }
        @Override
        public final NullType nullType() {
          return javacModelTypes.getNullType();
        }
        @Override
        public final PrimitiveType primitiveType(final TypeKind k) {
          return javacModelTypes.getPrimitiveType(k);
        }
        @Override
        public boolean sameType(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.isSameType(t, s);
        }
        @Override
        public boolean subtype(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.isSubtype(t, s);
        }
        @Override
        public final TypeElement typeElement(final CharSequence n) {
          return elements.getTypeElement(n);
        }
        @Override
        public final TypeElement typeElement(final ModuleElement m, final CharSequence n) {
          return elements.getTypeElement(m, n);
        }
        @Override
        public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
          throw new UnsupportedOperationException(); // NOTE
        }
        @Override
        public final WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
          return javacModelTypes.getWildcardType(extendsBound, superBound);
        }
      };
    final Visitors visitors = new Visitors(tes); // (m, n) -> elements.getTypeElement(elements.getModuleElement(m), n));
    try {
      visitors.typeClosureVisitor().visit(integerArrayType);
      fail();
    } catch (final IllegalArgumentException expected) {
      // OK
    }
  }

  @Test
  final void testTypeClosure(final ProcessingEnvironment env) {
    final javax.lang.model.util.Elements elements = env.getElementUtils();
    final javax.lang.model.util.Types javacModelTypes = env.getTypeUtils();
    assertTrue(javacModelTypes instanceof JavacTypes);
    com.sun.tools.javac.code.Types javacTypes = null;
    try {
      final Field f = JavacTypes.class.getDeclaredField("types");
      assertTrue(f.trySetAccessible());
      javacTypes = (com.sun.tools.javac.code.Types)f.get(javacModelTypes);
    } catch (final ReflectiveOperationException reflectiveOperationException) {
      fail(reflectiveOperationException);
    }
    assertNotNull(javacTypes);

    final TypeElement integerElement = elements.getTypeElement("java.lang.Integer");
    final DeclaredType integerElementType = (DeclaredType)integerElement.asType();
    assertSame(TypeKind.DECLARED, integerElementType.getKind());

    // https://github.com/openjdk/jdk/blob/jdk-20+14/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L3702-L3703
    //
    // "…(that is, subclasses come first, arbitrary but fixed otherwise)."

    final List<Type> closure = javacTypes.closure((Type)integerElementType);
    // 0: java.lang.Integer
    // 1: java.lang.Number (superclass of java.lang.Integer)
    // 2: java.io.Serializable (declared interface)
    // 3: java.lang.Comparable<java.lang.Integer> (declared interface)
    // 4: java.lang.constant.Constable (declared interface)
    // 5: java.lang.constant.ConstantDesc (declared interface)
    // 6: java.lang.Object (superclass of java.lang.Number)
    assertEquals(7, closure.size());

    // Let's try it with our visitor.
    final TypeAndElementSource tes = new TypeAndElementSource() {
        @Override
        public final ArrayType arrayTypeOf(final TypeMirror componentType) {
          return javacModelTypes.getArrayType(componentType);
        }
        @Override
        public boolean assignable(final TypeMirror payload, final TypeMirror receiver) {
          return javacModelTypes.isAssignable(payload, receiver);
        }
        // @Override
        public final TypeElement boxedClass(final PrimitiveType t) {
          return javacModelTypes.boxedClass(t);
        }
        @Override
        public final boolean contains(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.contains(t, s);
        }
        @Override
        public final DeclaredType declaredType(final TypeElement typeElement, final TypeMirror... arguments) {
          return javacModelTypes.getDeclaredType(typeElement, arguments);
        }
        @Override
        public final DeclaredType declaredType(final DeclaredType enclosingType, final TypeElement typeElement, final TypeMirror... arguments) {
          return javacModelTypes.getDeclaredType(enclosingType, typeElement, arguments);
        }
        @Override
        public final List<? extends TypeMirror> directSupertypes(final TypeMirror t) {
          return javacModelTypes.directSupertypes(t);
        }
        @Override
        @SuppressWarnings("unchecked")
        public final <T extends TypeMirror> T erasure(final T t) {
          return (T)javacModelTypes.erasure(t);
        }
        @Override
        public final ModuleElement moduleElement(final CharSequence moduleName) {
          return elements.getModuleElement(moduleName);
        }
        @Override
        public final NoType noType(final TypeKind k) {
          return javacModelTypes.getNoType(k);
        }
        @Override
        public final NullType nullType() {
          return javacModelTypes.getNullType();
        }
        @Override
        public final PrimitiveType primitiveType(final TypeKind k) {
          return javacModelTypes.getPrimitiveType(k);
        }
        @Override
        public boolean sameType(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.isSameType(t, s);
        }
        @Override
        public boolean subtype(final TypeMirror t, final TypeMirror s) {
          return javacModelTypes.isSubtype(t, s);
        }
        /*
        @Override
        public final TypeElement typeElement(final CharSequence m, final CharSequence n) {
          return elements.getTypeElement(elements.getModuleElement(m), n);
        }
        */
        @Override
        public final TypeElement typeElement(final CharSequence n) {
          return elements.getTypeElement(n);
        }
        @Override
        public final TypeElement typeElement(final ModuleElement m, final CharSequence n) {
          return elements.getTypeElement(m, n);
        }
        @Override
        public final TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t) {
          throw new UnsupportedOperationException(); // NOTE
        }
        @Override
        public final WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound) {
          return javacModelTypes.getWildcardType(extendsBound, superBound);
        }
      };
    final Visitors visitors = new Visitors(tes); // (m, n) -> elements.getTypeElement(elements.getModuleElement(m), n));
    final List<? extends TypeMirror> list = visitors.typeClosureVisitor().visit(integerElementType).toList();
    assertEquals(7, list.size(), "Unexpected type closure list: " + list);
  }

}
