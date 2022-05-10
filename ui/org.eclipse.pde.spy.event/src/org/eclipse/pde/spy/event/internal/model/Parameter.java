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

import org.eclipse.pde.spy.event.internal.util.MultilineFormatter;

public class Parameter implements IEventItem {
	private static final String EMPTY_VALUE = ""; //$NON-NLS-1$

	private final String name;
	private final Object value;

	private String formattedValue;

	public Parameter(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public String getParam1() {
		if (value == null) {
			return SpecialValue.Null.toString();
		}
		if (formattedValue == null) {
			formattedValue = MultilineFormatter.format(value.toString(), 70);
		}
		return formattedValue;
	}

	@Override
	public String getParam2() {
		return EMPTY_VALUE;
	}
}
