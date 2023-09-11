/*******************************************************************************
 * Copyright (c) 2011 Manumitting Technologies, Inc.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Brian de Alwis (MT) - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.css;

import org.eclipse.e4.ui.css.core.dom.CSSStylableElement;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.w3c.dom.css.CSSValue;

/**
 * A getter and setter of a particular CSS property for a particular element.
 */
@SuppressWarnings("restriction")
public class CSSPropertyProvider {

	private final String propertyName;
	private final CSSStylableElement element;
	private final CSSEngine engine;

	public CSSPropertyProvider(String propertyName, CSSStylableElement element, CSSEngine engine) {
		this.propertyName = propertyName;
		this.element = element;
		this.engine = engine;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getValue() throws Exception {
		return engine.retrieveCSSProperty(element, propertyName, "");
	}

	public void setValue(String value) throws Exception {
		CSSValue v = engine.parsePropertyValue(value);
		engine.applyCSSProperty(element, propertyName, v, "");
	}

	@Override
	public String toString() {
		return propertyName;
	}

}
