/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.event.internal.model;

public enum Operator {
	NotSelected(Messages.Operator_Title, false), Equals(Messages.Operator_EqualsTo, true), NotEquals(Messages.Operator_NotEqualsTo,
			false), Contains(Messages.Operator_Contains, true), NotContains(Messages.Operator_NotContains, false), StartsWith(Messages.Operator_StartsWith,
					true), NotStartsWith(Messages.Operator_NotStartsWith, false);

	private final String text;

	private final boolean positive;

	private Operator(String text, boolean positive) {
		this.text = text;
		this.positive = positive;
	}

	@Override
	public String toString() {
		return text;
	}

	public boolean isPositive() {
		return positive;
	}

	public static Operator toOperator(String text) {
		for (Operator operator : values()) {
			if (operator.text.equals(text)) {
				return operator;
			}
		}
		throw new IllegalArgumentException(String.format(Messages.Operator_NotFoundFor, Operator.class.getSimpleName(), text));
	}
}
