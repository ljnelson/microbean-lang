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
package org.microbean.lang;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.List;
import java.util.Optional;

import javax.lang.model.AnnotatedConstruct;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Parameterizable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;

import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import static java.lang.constant.ConstantDescs.NULL;

// In general: behavior could be undefined for anything accepting a type or element that isn't "from" this source.
public interface TypeAndElementSource {

  public ArrayType arrayTypeOf(final TypeMirror componentType);

  // Note the strange positioning of payload and receiver.
  public boolean assignable(final TypeMirror payload, final TypeMirror receiver);

  public default TypeElement boxedClass(final PrimitiveType t) {
    return switch (t.getKind()) { // won't cause symbol completion
    case BOOLEAN -> this.typeElement("java.lang.Boolean");
    case BYTE -> this.typeElement("java.lang.Byte");
    case CHAR -> this.typeElement("java.lang.Character");
    case DOUBLE -> this.typeElement("java.lang.Double");
    case FLOAT -> this.typeElement("java.lang.Float");
    case INT -> this.typeElement("java.lang.Integer");
    case LONG -> this.typeElement("java.lang.Long");
    case SHORT -> this.typeElement("java.lang.Short");
    default -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public boolean contains(final TypeMirror t, final TypeMirror s);

  public DeclaredType declaredType(final DeclaredType enclosingType,
                                   final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  public DeclaredType declaredType(final TypeElement typeElement,
                                   final TypeMirror... typeArguments);

  public List<? extends TypeMirror> directSupertypes(final TypeMirror t);

  public <T extends TypeMirror> T erasure(final T t);

  public ModuleElement moduleElement(final CharSequence canonicalName);

  public NoType noType(final TypeKind k);

  public NullType nullType();

  public PrimitiveType primitiveType(final TypeKind k);

  public boolean sameType(final TypeMirror t, final TypeMirror s);

  public boolean subtype(final TypeMirror t, final TypeMirror s);

  public TypeElement typeElement(final CharSequence canonicalName);

  // Note that Elements#getTypeElement(ModuleElement, CharSequence), to which this basically ultimately delegates, says
  // that the ModuleElement argument represents the "module relative to which the lookup should happen". That's not
  // necessarily obvious.
  public TypeElement typeElement(final ModuleElement module, final CharSequence canonicalName);

  public TypeVariable typeVariable(final java.lang.reflect.TypeVariable<?> t);

  public WildcardType wildcardType(final TypeMirror extendsBound, final TypeMirror superBound);


  /*
   * Default methods.
   */


  public default ArrayType arrayType(final Type t) {
    return switch (t) {
    case null                        -> throw new NullPointerException("t");
    case Class<?> c when c.isArray() -> this.arrayTypeOf(this.type(c.getComponentType()));
    case GenericArrayType g          -> this.arrayTypeOf(this.type(g.getGenericComponentType()));
    default                          -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public default DeclaredType declaredType(final CharSequence canonicalName) {
    return this.declaredType(this.typeElement(canonicalName));
  }

  public default DeclaredType declaredType(final Type t) {
    return switch (t) {
    case null                -> throw new NullPointerException("t");
    case Class<?> c when c.isArray() || c.isPrimitive() || c.isLocalClass() || c.isAnonymousClass()
                             -> throw new IllegalArgumentException("t: " + t);
    case Class<?> c          -> {
      final Class<?> ec = c.getEnclosingClass();
      yield ec == null ? this.declaredType(this.typeElement(c)) : this.declaredType(this.declaredType(ec), this.typeElement(c));
    }
    case ParameterizedType p -> this.declaredType(switch (p.getOwnerType()) {
      case null                 -> null;
      case Class<?> c           -> this.declaredType(c);
      case ParameterizedType pt -> this.declaredType(pt);
      default                   -> throw new IllegalArgumentException("t: " + t);
      },
      this.typeElement(p.getRawType()),
      this.typeArray(p.getActualTypeArguments()));
    default                  -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public default Optional<? extends ConstantDesc> describeConstable(final AnnotatedConstruct a) {
    return switch (a) {
    case null            -> Optional.of(NULL);
    case Constable c     -> c.describeConstable();
    case ConstantDesc cd -> Optional.of(cd); // future proofing
    case TypeMirror t    -> Optional.empty();
    case Element e       -> Optional.empty();
    default              -> throw new IllegalArgumentException("a: " + a);
    };
  }

  public default PrimitiveType primitiveType(final Class<?> c) {
    if (!c.isPrimitive()) {
      throw new IllegalArgumentException("c: " + c);
    }
    return this.primitiveType(TypeKind.valueOf(c.getName().toUpperCase()));
  }

  public default TypeMirror type(final Type t) {
    return switch (t) {
    case null                                                     -> throw new NullPointerException();
    case Class<?> c when c == void.class                          -> this.noType(TypeKind.VOID);
    case Class<?> c when c.isArray()                              -> this.arrayType(c);
    case Class<?> c when c.isPrimitive()                          -> this.primitiveType(c);
    case Class<?> c when c.isLocalClass() || c.isAnonymousClass() -> throw new IllegalArgumentException("t: " + t);
    case Class<?> c                                               -> this.declaredType(c);
    case ParameterizedType p                                      -> this.declaredType(p);
    case GenericArrayType g                                       -> this.arrayType(g);
    case java.lang.reflect.TypeVariable<?> tv                     -> this.typeVariable(tv);
    case java.lang.reflect.WildcardType w                         -> this.wildcardType(w);
    default                                                       -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public default TypeMirror[] typeArray(final Type[] ts) {
    final TypeMirror[] rv = new TypeMirror[ts.length];
    for (int i = 0; i < ts.length; i++) {
      rv[i] = this.type(ts[i]);
    }
    return rv;
  }

  public default TypeElement typeElement(final Type t) {
    return switch (t) {
    case null                -> throw new NullPointerException("t");
    case Class<?> c when c.isArray() || c.isPrimitive() || c.isLocalClass() || c.isAnonymousClass()
                             -> throw new IllegalArgumentException("t: " + t);
    case Class<?> c          -> this.typeElement(c.getCanonicalName());
    case ParameterizedType p -> this.typeElement(p.getRawType());
    default                  -> throw new IllegalArgumentException("t: " + t);
    };
  }

  public default WildcardType wildcardType(final java.lang.reflect.WildcardType t) {
    final Type[] lowerBounds = t.getLowerBounds();
    return this.wildcardType(this.type(t.getUpperBounds()[0]), this.type(lowerBounds.length <= 0 ? null : lowerBounds[0]));
  }

}
