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
package org.eclipse.pde.bnd.ui;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.TextStyle;

public class BoldStyler extends Styler {

	public static BoldStyler	INSTANCE_DEFAULT	= new BoldStyler(JFaceResources.DEFAULT_FONT, null, null);
	public static BoldStyler	INSTANCE_COUNTER	= new BoldStyler(JFaceResources.DEFAULT_FONT,
		JFacePreferences.COUNTER_COLOR, null);

	private final String		fontName;
	private final String		fForegroundColorName;
	private final String		fBackgroundColorName;

	public BoldStyler(String fontName, String foregroundColorName, String backgroundColorName) {
		this.fontName = fontName;
		fForegroundColorName = foregroundColorName;
		fBackgroundColorName = backgroundColorName;
	}

	@Override
	public void applyStyles(TextStyle textStyle) {
		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		Font font = JFaceResources.getFontRegistry()
			.getBold(fontName);
		if (fForegroundColorName != null) {
			textStyle.foreground = colorRegistry.get(fForegroundColorName);
		}
		if (fBackgroundColorName != null) {
			textStyle.background = colorRegistry.get(fBackgroundColorName);
		}
		textStyle.font = font;
	}
}
