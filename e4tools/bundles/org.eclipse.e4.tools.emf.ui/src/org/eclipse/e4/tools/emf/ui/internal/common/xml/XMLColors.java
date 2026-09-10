/*******************************************************************************
 * Copyright (c) 2026 Vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.internal.common.xml;

import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.PlatformUI;

/**
 * The syntax colours of the XMI tab, taken from the active workbench theme.
 */
public class XMLColors {

	private XMLColors() {
	}

	public static Color get(String colorId) {
		return PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry().get(colorId);
	}
}
