/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.swt.graphics.RGB;

public interface IPDEColorConstants {
	RGB HEADER_KEY = new RGB(128, 0, 0);
	RGB HEADER_VALUE = new RGB(0, 0, 0);
	RGB HEADER_VALUE_HIGH_CONTRAST = new RGB(255, 255, 255);
	RGB HEADER_ASSIGNMENT = new RGB(0, 0, 0);
	RGB HEADER_ASSIGNMENT_HIGH_CONTRAST = new RGB(255, 255, 255);
	RGB HEADER_OSGI = new RGB(128, 0, 0);
	RGB HEADER_ATTRIBUTES = new RGB(128, 128, 0);

	String P_BOLD_SUFFIX = "_bold"; //$NON-NLS-1$
	String P_ITALIC_SUFFIX = "_italic"; //$NON-NLS-1$

	String P_HEADER_KEY = "editor.color.header_key"; //$NON-NLS-1$
	String P_HEADER_VALUE = "editor.color.header_value"; //$NON-NLS-1$
	String P_HEADER_ASSIGNMENT = "editor.color.header_assignment"; //$NON-NLS-1$
	String P_HEADER_OSGI = "editor.color.header_osgi"; //$NON-NLS-1$
	String P_HEADER_ATTRIBUTES = "editor.color.header_attributes"; //$NON-NLS-1$
}
