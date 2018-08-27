/*******************************************************************************
 * Copyright (c) 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

public class ProblemReporter {

	private final ValidationErrorLevel errorLevel;

	private final Set<DSAnnotationProblem> problems;

	public ProblemReporter(ValidationErrorLevel errorLevel, Set<DSAnnotationProblem> problems) {
		this.errorLevel = errorLevel;
		this.problems = problems;
	}

	public void reportProblem(Annotation annotation, String member, String message, String... args) {
		reportProblem(annotation, member, -1, message, args);
	}

	public void reportProblem(Annotation annotation, String member, ValidationErrorLevel errorLevel, String message, String... args) {
		reportProblem(annotation, member, -1, errorLevel, message, args);
	}

	public void reportProblem(Annotation annotation, String member, int valueIndex, String message, String... args) {
		reportProblem(annotation, member, valueIndex, errorLevel, message, args);
	}

	public void reportProblem(Annotation annotation, String member, int valueIndex, ValidationErrorLevel errorLevel, String message, String... args) {
		reportProblem(annotation, member, valueIndex, false, errorLevel, message, args);
	}

	public void reportProblem(Annotation annotation, String member, int valueIndex, boolean fullPair, ValidationErrorLevel errorLevel, String message, String... args) {
		if (errorLevel.isIgnore()) {
			return;
		}

		ASTNode element = annotation;
		if (annotation.isNormalAnnotation() && member != null) {
			NormalAnnotation na = (NormalAnnotation) annotation;
			for (Object value : na.values()) {
				MemberValuePair pair = (MemberValuePair) value;
				if (member.equals(pair.getName().getIdentifier())) {
					element = fullPair ? pair : pair.getValue();
					break;
				}
			}
		} else if (annotation.isSingleMemberAnnotation()) {
			SingleMemberAnnotation sma = (SingleMemberAnnotation) annotation;
			element = sma.getValue();
		}

		int start = element.getStartPosition();
		int length = element.getLength();

		if (valueIndex >= 0 && element instanceof ArrayInitializer) {
			ArrayInitializer ai = (ArrayInitializer) element;
			if (valueIndex < ai.expressions().size()) {
				Expression expression = (Expression) ai.expressions().get(valueIndex);
				start = expression.getStartPosition();
				length = expression.getLength();
			}
		}

		if (start >= 0) {
			DSAnnotationProblem problem = new DSAnnotationProblem(errorLevel.isError(), message, args);
			problem.setSourceStart(start);
			problem.setSourceEnd(start + length - 1);
			problems.add(problem);
		}
	}
}
