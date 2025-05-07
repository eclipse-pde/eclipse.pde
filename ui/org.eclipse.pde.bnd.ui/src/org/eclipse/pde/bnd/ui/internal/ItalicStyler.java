/*******************************************************************************
 * Copyright (c) 2016, 2019 bndtools project and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
*******************************************************************************/
package org.eclipse.pde.bnd.ui.internal;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

public class ItalicStyler extends Styler {

	public static final Styler	INSTANCE_DEFAULT	= new ItalicStyler(JFaceResources.DEFAULT_FONT, null, null);
	public static final Styler	INSTANCE_QUALIFIER	= new ItalicStyler(JFaceResources.DEFAULT_FONT,
		JFacePreferences.QUALIFIER_COLOR, null);
	public static final Styler	INSTANCE_ERROR		= new ItalicStyler(JFaceResources.DEFAULT_FONT,
		JFacePreferences.ERROR_COLOR, null);

	private final String		fontName;
	private final String		fForegroundColorName;
	private final String		fBackgroundColorName;

	public ItalicStyler(String fontName, String foregroundColorName, String backgroundColorName) {
		this.fontName = fontName;
		fForegroundColorName = foregroundColorName;
		fBackgroundColorName = backgroundColorName;
	}

	@Override
	public void applyStyles(TextStyle textStyle) {
		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		Font font = JFaceResources.getFontRegistry()
			.getItalic(fontName);
		if (fForegroundColorName != null) {
			textStyle.foreground = colorRegistry.get(fForegroundColorName);
		}
		if (fBackgroundColorName != null) {
			textStyle.background = colorRegistry.get(fBackgroundColorName);
		}
		textStyle.font = font;
	}
}
