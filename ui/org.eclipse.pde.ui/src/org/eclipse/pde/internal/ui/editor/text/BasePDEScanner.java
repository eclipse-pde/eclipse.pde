/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public abstract class BasePDEScanner extends BufferedRuleBasedScanner {

	private IColorManager fColorManager;

	protected BasePDEScanner() {
	}

	public void setColorManager(IColorManager manager) {
		fColorManager = manager;
	}

	public BasePDEScanner(IColorManager manager) {
		fColorManager = manager;
		initialize();
	}

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (affectsTextPresentation(property)) {
			Token token = getTokenAffected(event);
			if (property.endsWith(IPDEColorConstants.P_BOLD_SUFFIX))
				adaptToStyleChange(event, token, SWT.BOLD);
			else if (property.endsWith(IPDEColorConstants.P_ITALIC_SUFFIX))
				adaptToStyleChange(event, token, SWT.ITALIC);
			else
				adaptToColorChange(event, token);
		}
	}

	public abstract boolean affectsTextPresentation(String property);

	protected abstract Token getTokenAffected(PropertyChangeEvent event);

	protected abstract void initialize();

	protected void adaptToStyleChange(PropertyChangeEvent event, Token token, int styleAttribute) {
		if (token == null)
			return;

		boolean eventValue = false;
		Object value = event.getNewValue();
		if (value instanceof Boolean)
			eventValue = ((Boolean) value).booleanValue();

		TextAttribute attr = (TextAttribute) token.getData();
		boolean activeValue = (attr.getStyle() & styleAttribute) == styleAttribute;
		if (activeValue != eventValue) {
			Color foreground = attr.getForeground();
			Color background = attr.getBackground();
			int style = eventValue ? attr.getStyle() | styleAttribute : attr.getStyle() & ~styleAttribute;
			token.setData(new TextAttribute(foreground, background, style));
		}
	}

	protected void adaptToColorChange(PropertyChangeEvent event, Token token) {
		TextAttribute attr = (TextAttribute) token.getData();
		token.setData(new TextAttribute(fColorManager.getColor(event.getProperty()), attr.getBackground(), attr.getStyle()));
	}

	protected TextAttribute createTextAttribute(String property) {
		return createTextAttribute(fColorManager, property);
	}

	protected static TextAttribute createTextAttribute(IColorManager manager, String property) {
		Color color = manager.getColor(property);
		int style = SWT.NORMAL;
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(property + IPDEColorConstants.P_BOLD_SUFFIX))
			style |= SWT.BOLD;
		if (store.getBoolean(property + IPDEColorConstants.P_ITALIC_SUFFIX))
			style |= SWT.ITALIC;
		return new TextAttribute(color, null, style);
	}

}
