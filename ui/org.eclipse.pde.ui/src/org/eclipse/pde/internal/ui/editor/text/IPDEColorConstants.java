/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.swt.graphics.RGB;

public interface IPDEColorConstants {
	RGB XML_COMMENT =       new RGB(128,   0,   0);
	RGB PROC_INSTR =        new RGB(128, 128, 128);
	RGB STRING=             new RGB(  0, 128,   0);
	RGB DEFAULT=            new RGB(  0,   0,   0);
	RGB TAG=                new RGB(  0,   0, 128);
	
	
	String P_XML_COMMENT = "editor.color.xml_comment"; //$NON-NLS-1$
	String P_PROC_INSTR = "editor.color.instr"; //$NON-NLS-1$
	String P_STRING = "editor.color.string"; //$NON-NLS-1$
	String P_DEFAULT = "editor.color.default"; //$NON-NLS-1$
	String P_TAG = "editor.color.tag"; //$NON-NLS-1$
}
