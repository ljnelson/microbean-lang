/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil; coding: utf-8-unix -*-
 *
 * Copyright © 2023–2024 microBean™.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.microbean.lang.visitor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import javax.lang.model.util.SimpleTypeVisitor14;

import org.microbean.lang.TypeAndElementSource;

import org.microbean.lang.element.DelegatingElement;

import org.microbean.lang.type.Types;

import static org.microbean.lang.type.Types.allTypeArguments;
import static org.microbean.lang.type.Types.asElement;

/**
 * Does something adapting-like. Looks to be related to type variable resolution.
 *
 * <p>Usage: call {@link #adapt(DeclaredType, DeclaredType)}, not {@link #visit(TypeMirror)}.</p>
 *
 * <p>This is a "one shot" visitor: because it mutates its internal data structures (!) (following the analogous
 * structures in the compiler), an instance of this class should not be reused.</p>
 */
// Not thread safe.
// Basically done.
// See https://github.com/openjdk/jdk/blob/jdk-20+13/src/jdk.compiler/share/classes/com/sun/tools/javac/code/Types.java#L4590-L4690
final class AdaptingVisitor extends SimpleTypeVisitor14<Void, TypeMirror> {

  /*
   * Ported mostly slavishly from the compiler.
   */

  // The compiler's implementation mutates this list.
  private final List<TypeVariable> from;

  // The compiler's implementation mutates this list.
  private final List<TypeMirror> to;

  private final TypeAndElementSource tes;

  private final Types types;

  // The compiler calls this "mapping"; the class is called "Adapting"; I think they are the same thing
  private final Map<DelegatingElement, TypeMirror> mapping;

  private final SameTypeVisitor sameTypeVisitor;

  private final SubtypeVisitor subtypeVisitor;

  private final Set<TypeMirrorPair> cache;

  AdaptingVisitor(final TypeAndElementSource tes,
                  final Types types,
                  final SameTypeVisitor sameTypeVisitor,
                  final SubtypeVisitor subtypeVisitor,
                  final List<TypeVariable> from, // mutated
                  final List<TypeMirror> to) { // mutated
    super();
    this.tes = Objects.requireNonNull(tes, "tes");
    this.types = Objects.requireNonNull(types, "types");
    this.sameTypeVisitor = Objects.requireNonNull(sameTypeVisitor, "sameTypeVisitor");
    this.subtypeVisitor = Objects.requireNonNull(subtypeVisitor, "subtypeVisitor");
    this.mapping = new HashMap<>();
    this.cache = new HashSet<>();
    this.from = Objects.requireNonNull(from, "from");
    this.to = Objects.requireNonNull(to, "to");
  }

  final void adaptSelf(final DeclaredType target) {
    this.adapt((DeclaredType)target.asElement().asType(), target);
  }

  final void adapt(final DeclaredType source, final DeclaredType target) {
    this.visitDeclared(source, target);
    final int fromSize = this.from.size();
    for (int i = 0; i < fromSize; i++) {
      final TypeMirror val = this.mapping.get(DelegatingElement.of(asElement(this.from.get(i), true), this.tes));
      if (this.to.get(i) != val) {
        this.to.set(i, val);
      }
    }
  }

  private final void adaptRecursive(final Collection<? extends TypeMirror> source, final Collection<? extends TypeMirror> target) {
    if (source.size() == target.size()) {
      final Iterator<? extends TypeMirror> sourceIterator = source.iterator();
      final Iterator<? extends TypeMirror> targetIterator = target.iterator();
      while (sourceIterator.hasNext()) {
        assert targetIterator.hasNext();
        this.adaptRecursive(sourceIterator.next(), targetIterator.next());
      }
    }
  }

  private final void adaptRecursive(final TypeMirror source, final TypeMirror target) {
    final TypeMirrorPair pair = new TypeMirrorPair(this.sameTypeVisitor, source, target);
    if (this.cache.add(pair)) {
      try {
        this.visit(source, target);
      } finally {
        this.cache.remove(pair);
      }
    }
  }

  // Adapts the component type of source.
  @Override
  public final Void visitArray(final ArrayType source, final TypeMirror target) {
    assert source.getKind() == TypeKind.ARRAY;
    if (target.getKind() == TypeKind.ARRAY) {
      this.adaptRecursive(source.getComponentType(), ((ArrayType)target).getComponentType());
    }
    return null;
  }

  // Adapts type arguments of source. "Recursive" means something like "go into type parameters etc."
  // Adapting a type argument takes effect on wildcard-extending type variables only, it looks like.
  @Override
  public final Void visitDeclared(final DeclaredType source, final TypeMirror target) {
    assert source.getKind() == TypeKind.DECLARED;
    if (target.getKind() == TypeKind.DECLARED) {
      this.adaptRecursive(allTypeArguments(source), allTypeArguments(target));
    }
    return null;
  }

  @Override
  public final Void visitTypeVariable(final TypeVariable source, final TypeMirror target) {
    assert source.getKind() == TypeKind.TYPEVAR;
    final DelegatingElement sourceElement = DelegatingElement.of(source.asElement(), this.tes);
    TypeMirror val = this.mapping.get(sourceElement);
    if (val == null) {
      val = target;
      this.from.add(source);
      this.to.add(target);
    } else if (val.getKind() == TypeKind.WILDCARD && target.getKind() == TypeKind.WILDCARD) {
      // This block is the only place in this entire class where the magic happens.
      final WildcardType valWc = (WildcardType)val;
      final TypeMirror valSuperBound = valWc.getSuperBound();
      final TypeMirror valExtendsBound = valWc.getExtendsBound();
      final WildcardType targetWc = (WildcardType)target;
      final TypeMirror targetSuperBound = targetWc.getSuperBound();
      final TypeMirror targetExtendsBound = targetWc.getExtendsBound();
      if (valSuperBound == null) {
        if (valExtendsBound == null) {
          // valWc is lower-bounded (and upper-bounded)
          if (targetExtendsBound == null &&
              this.subtypeVisitor.withCapture(true).visit(this.types.superBound(val), this.types.superBound(target))) {
            // targetWc is lower-bounded (and maybe unbounded)
            val = target;
          }
        } else if (targetSuperBound == null &&
                   !this.subtypeVisitor.withCapture(true).visit(this.types.extendsBound(val), this.types.extendsBound(target))) {
          // valWc is upper-bounded
          // targetWc is upper-bounded (and maybe unbounded)
          val = target;
        }
      } else if (valExtendsBound == null) {
        // valWc is lower-bounded
        if (targetExtendsBound == null &&
            this.subtypeVisitor.withCapture(true).visit(this.types.superBound(val), this.types.superBound(target))) {
          // targetWc is lower-bounded (and maybe unbounded)
          val = target;
        }
      } else {
        throw new IllegalStateException("val: " + val);
      }
    } else if (!this.sameTypeVisitor.visit(val, target)) {
      throw new IllegalStateException();
    }
    this.mapping.put(sourceElement, val);
    return null;
  }

  // Adapts the bounds of a wildcard.
  @Override
  public final Void visitWildcard(final WildcardType source, final TypeMirror target) {
    assert source.getKind() == TypeKind.WILDCARD;
    final TypeMirror extendsBound = source.getExtendsBound();
    final TypeMirror superBound = source.getSuperBound();
    if (extendsBound == null) {
      if (superBound == null) {
        this.adaptRecursive(this.types.extendsBound(source), this.types.extendsBound(target));
      } else {
        this.adaptRecursive(this.types.superBound(source), this.types.superBound(target));
      }
    } else if (superBound == null) {
      this.adaptRecursive(this.types.extendsBound(source), this.types.extendsBound(target));
    } else {
      throw new IllegalArgumentException("source: " + source);
    }
    return null;
  }


}
