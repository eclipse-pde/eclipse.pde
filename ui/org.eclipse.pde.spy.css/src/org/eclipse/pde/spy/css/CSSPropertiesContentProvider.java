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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.e4.ui.css.core.dom.CSSStylableElement;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Widget;

@SuppressWarnings("restriction")
public class CSSPropertiesContentProvider implements IStructuredContentProvider {

	protected CSSEngine cssEngine;
	protected CSSStylableElement input;

	@Override
	public void dispose() {
		cssEngine = null;
		input = null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof CSSStylableElement) {
			this.input = (CSSStylableElement) newInput;
			this.cssEngine = CssSpyPart.getCSSEngine(input.getNativeWidget());
		} else if (newInput instanceof Widget) {
			this.cssEngine = CssSpyPart.getCSSEngine(newInput);
			this.input = (CSSStylableElement) cssEngine.getElement(newInput);
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		Collection<String> propertyNames = cssEngine.getCSSProperties(input);
		List<CSSPropertyProvider> properties = new ArrayList<>(propertyNames.size());
		for (String propertyName : propertyNames) {
			properties.add(new CSSPropertyProvider(propertyName, input, cssEngine));
		}
		return properties.toArray();
	}

}
