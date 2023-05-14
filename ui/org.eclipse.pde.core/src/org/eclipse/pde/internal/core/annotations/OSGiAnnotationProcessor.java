/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.annotations;

import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.pde.core.IBaseModel;

public interface OSGiAnnotationProcessor {
	/**
	 * process the given annotation of the given type
	 *
	 * @param annotation
	 *            the annotation to process
	 * @param type
	 *            the fully qualified type name of the annotation to process
	 */
	void processAnnotation(Annotation annotation, String type);

	/**
	 * applies the processed annotation actions (if any) to the given model
	 *
	 * @param model
	 */
	void apply(IBaseModel model);

	/**
	 * Optionally converts the given Expression to the string literal value
	 *
	 * @param expression
	 *            the expression to convert
	 * @return an Optional holding the string literal value or an empty optional
	 *         if it could not be converted
	 */
	static Optional<String> stringValue(Expression expression) {
		return Optional.ofNullable(expression).filter(StringLiteral.class::isInstance).map(StringLiteral.class::cast)
				.map(StringLiteral::getLiteralValue);
	}

	/**
	 * Explodes an expression into a stream of child expressions. If the
	 * expression could not be exploded, it is returned as a single item.
	 *
	 * @param expression
	 *            the expression
	 * @return a stream of exploded expressions, or the expression itself
	 */
	static Stream<Expression> expressions(Expression expression) {
		Expression unwrap = value(expression).orElse(expression);
		if (unwrap instanceof ArrayInitializer) {
			ArrayInitializer arrayInitializer = (ArrayInitializer) unwrap;
			return arrayInitializer.expressions().stream().filter(Expression.class::isInstance)
					.map(Expression.class::cast);
		}
		return Stream.of(expression);
	}

	/**
	 * Optionally retrieves the 'value' of an annotation
	 *
	 * @param annotation
	 *            the annotation to retrieve the value from
	 * @return the Expression that represents the value or an empty optional if
	 *         the value is not found
	 */
	static Optional<Expression> value(Expression annotation) {
		return member(annotation, "value"); //$NON-NLS-1$
	}

	/**
	 * Optionally retrieves the given member from an annotation
	 *
	 * @param annotation
	 *            the annotation to retrieve the member from
	 * @param memberName
	 *            the name of the member to retrieve
	 * @return the retrieved value of the member with the given name or an empty
	 *         optional if no such member exits.
	 */
	static Optional<Expression> member(Expression annotation, String memberName) {
		if (annotation instanceof NormalAnnotation) {
			NormalAnnotation normalAnnotation = (NormalAnnotation) annotation;
			for (Object value : normalAnnotation.values()) {
				if (value instanceof MemberValuePair) {
					MemberValuePair pair = (MemberValuePair) value;
					SimpleName name = pair.getName();
					if (name != null && name.toString().equals(memberName)) {
						return Optional.ofNullable(pair.getValue());
					}
				}
			}
		}
		if (annotation instanceof SingleMemberAnnotation && "value".equals(memberName)) { //$NON-NLS-1$
			SingleMemberAnnotation singleMemberAnnotation = (SingleMemberAnnotation) annotation;
			return Optional.ofNullable(singleMemberAnnotation.getValue());
		}
		return Optional.empty();
	}

}
