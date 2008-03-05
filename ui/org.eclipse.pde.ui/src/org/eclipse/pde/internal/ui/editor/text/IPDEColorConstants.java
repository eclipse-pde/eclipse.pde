/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.swt.graphics.RGB;

public interface IPDEColorConstants {
	RGB XML_COMMENT = new RGB(128, 0, 0);
	RGB PROC_INSTR = new RGB(128, 128, 128);
	RGB STRING = new RGB(0, 128, 0);
	RGB EXTERNALIZED_STRING = new RGB(128, 0, 128);
	RGB DEFAULT = new RGB(0, 0, 0);
	RGB DEFAULT_HIGH_CONTRAST = new RGB(255, 255, 255);
	RGB TAG = new RGB(0, 0, 128);

	RGB HEADER_KEY = new RGB(128, 0, 0);
	RGB HEADER_VALUE = new RGB(0, 0, 0);
	RGB HEADER_VALUE_HIGH_CONTRAST = new RGB(255, 255, 255);
	RGB HEADER_ASSIGNMENT = new RGB(0, 0, 0);
	RGB HEADER_ASSIGNMENT_HIGH_CONTRAST = new RGB(255, 255, 255);
	RGB HEADER_OSGI = new RGB(128, 0, 0);
	RGB HEADER_ATTRIBUTES = new RGB(128, 128, 0);

	String P_BOLD_SUFFIX = "_bold"; //$NON-NLS-1$
	String P_ITALIC_SUFFIX = "_italic"; //$NON-NLS-1$

	String P_XML_COMMENT = "editor.color.xml_comment"; //$NON-NLS-1$
	String P_PROC_INSTR = "editor.color.instr"; //$NON-NLS-1$
	String P_STRING = "editor.color.string"; //$NON-NLS-1$
	String P_EXTERNALIZED_STRING = "editor.color.externalized_string"; //$NON-NLS-1$
	String P_DEFAULT = "editor.color.default"; //$NON-NLS-1$
	String P_TAG = "editor.color.tag"; //$NON-NLS-1$

	String P_HEADER_KEY = "editor.color.header_key"; //$NON-NLS-1$
	String P_HEADER_VALUE = "editor.color.header_value"; //$NON-NLS-1$
	String P_HEADER_ASSIGNMENT = "editor.color.header_assignment"; //$NON-NLS-1$
	String P_HEADER_OSGI = "editor.color.header_osgi"; //$NON-NLS-1$
	String P_HEADER_ATTRIBUTES = "editor.color.header_attributes"; //$NON-NLS-1$
}
