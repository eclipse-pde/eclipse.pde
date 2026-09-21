/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel <Lars.Vogel@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;

/**
 * The syntax colors of the PDE XML source viewers, taken from the current
 * workbench theme.
 */
public final class XMLSyntaxColors {

	/** Color of {@code %key} values that refer to a localization file. */
	public static final String EXTERNALIZED_STRING_COLOR = "org.eclipse.pde.ui.externalizedStringColor"; //$NON-NLS-1$

	private XMLSyntaxColors() {
	}

	/**
	 * Returns the theme color with the given id, or {@code null} to draw in the
	 * viewer's foreground when no workbench is running.
	 */
	public static Color get(String colorId) {
		if (!PlatformUI.isWorkbenchRunning()) {
			return null;
		}
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(colorId);
	}
}
