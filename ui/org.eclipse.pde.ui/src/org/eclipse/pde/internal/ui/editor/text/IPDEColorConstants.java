package org.eclipse.pde.internal.ui.editor.text;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.graphics.RGB;

public interface IPDEColorConstants {
	RGB XML_COMMENT =       new RGB(128,   0,   0);
	RGB PROC_INSTR =        new RGB(128, 128, 128);
	RGB STRING=             new RGB(  0, 128,   0);
	RGB DEFAULT=            new RGB(  0,   0,   0);
	RGB TAG=                new RGB(  0,   0, 128);
	
	
	String P_XML_COMMENT = "editor.color.xml_comment";
	String P_PROC_INSTR = "editor.color.instr";
	String P_STRING = "editor.color.string";
	String P_DEFAULT = "editor.color.default";
	String P_TAG = "editor.color.tag";
}
