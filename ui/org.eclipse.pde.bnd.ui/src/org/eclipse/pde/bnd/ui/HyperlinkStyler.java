/*******************************************************************************
 * Copyright (c) 2013, 2019 bndtools project and others.
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

import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

public class HyperlinkStyler extends Styler {

	private final Color color;

	public HyperlinkStyler() {
		this(Display.getCurrent());
	}

	public HyperlinkStyler(Display display) {
		color = display.getSystemColor(SWT.COLOR_BLUE);
	}

	@Override
	public void applyStyles(TextStyle style) {
		style.foreground = color;

		style.underline = true;
		style.underlineColor = color;
		style.underlineStyle = SWT.UNDERLINE_SINGLE;
	}

}
