/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2022–2023 microBean™.
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;

import com.sun.tools.javac.model.JavacTypes;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.microbean.lang.ElementSource;

import org.microbean.lang.type.Types;

import org.microbean.lang.visitor.AsSuperVisitor;
import org.microbean.lang.visitor.CaptureVisitor;
import org.microbean.lang.visitor.ContainsTypeVisitor;
import org.microbean.lang.visitor.EraseVisitor;
import org.microbean.lang.visitor.SameTypeVisitor;
import org.microbean.lang.visitor.MemberTypeVisitor;
import org.microbean.lang.visitor.PrecedesPredicate;
import org.microbean.lang.visitor.SubtypeVisitor;
import org.microbean.lang.visitor.SupertypeVisitor;
import org.microbean.lang.visitor.TypeClosureVisitor;
import org.microbean.lang.visitor.Visitors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(AnnotationProcessingInterceptor.class)
final class TestSameTypeVisitor {

  private TestSameTypeVisitor() {
    super();
  }

  @Test
  final void testSameTypeVisitor(final ProcessingEnvironment env) {
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

    // Set up the fundamentals.
    final Visitors visitors = new Visitors((m, n) -> elements.getTypeElement(elements.getModuleElement(m), n));

    // Should be ready to go.
    
  }

}