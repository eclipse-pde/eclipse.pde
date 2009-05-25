/*******************************************************************************
 *  Copyright (c) 2000, 2007 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;

/**
 * A simple XML writer.
 */
public class XMLWriter extends PrintWriter {
	protected int tab;

	/* constants */
	protected static final String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

	public XMLWriter(OutputStream output) throws UnsupportedEncodingException {
		super(new OutputStreamWriter(output, "UTF8")); //$NON-NLS-1$
		tab = 0;
		println(XML_VERSION);
	}

	public void endTag(String name) {
		tab--;
		printTag('/' + name, null);
	}

	public void printSimpleTag(String name, Object value) {
		if (value == null)
			return;
		printTag(name, null, true, false, false);
		print(getEscaped(String.valueOf(value)));
		printTag('/' + name, null, false, true, false);
	}

	public void printTabulation() {
		for (int i = 0; i < tab; i++)
			super.print('\t');
	}

	public void printTag(String name, Map parameters) {
		printTag(name, parameters, true, true, false);
	}

	public void printTag(String name, Map parameters, boolean shouldTab, boolean newLine, boolean close) {
		StringBuffer sb = new StringBuffer();
		sb.append("<"); //$NON-NLS-1$
		sb.append(name);
		if (parameters != null)
			for (Enumeration enumeration = Collections.enumeration(parameters.keySet()); enumeration.hasMoreElements();) {
				sb.append(" "); //$NON-NLS-1$
				String key = (String) enumeration.nextElement();
				if (parameters.get(key) != null) {
					sb.append(key);
					sb.append("=\""); //$NON-NLS-1$
					sb.append(getEscaped(String.valueOf(parameters.get(key))));
					sb.append("\""); //$NON-NLS-1$
				}
			}
		if (close)
			sb.append("/>"); //$NON-NLS-1$
		else 
			sb.append(">"); //$NON-NLS-1$
		if (shouldTab)
			printTabulation();
		if (newLine)
			println(sb.toString());
		else
			print(sb.toString());
	}

	public void startTag(String name, Map parameters) {
		startTag(name, parameters, true);
	}

	public void startTag(String name, Map parameters, boolean newLine) {
		printTag(name, parameters, true, newLine, false);
		tab++;
	}

	private static void appendEscapedChar(StringBuffer buffer, char c) {
		buffer.append(getReplacement(c));
	}

	public static String getEscaped(String s) {
		StringBuffer result = new StringBuffer(s.length() + 10);
		for (int i = 0; i < s.length(); ++i)
			appendEscapedChar(result, s.charAt(i));
		return result.toString();
	}

	private static String getReplacement(char c) {
		// Encode special XML characters into the equivalent character references.
		// These five are defined by default for all XML documents.
		switch (c) {
			case '<' :
				return "&lt;"; //$NON-NLS-1$
			case '>' :
				return "&gt;"; //$NON-NLS-1$
			case '"' :
				return "&quot;"; //$NON-NLS-1$
			case '\'' :
				return "&apos;"; //$NON-NLS-1$
			case '&' :
				return "&amp;"; //$NON-NLS-1$
			default :
				return String.valueOf(c);
		}
	}
	
	public void printlnEscaped(String s) {
		if (s != null)
			println(getEscaped(s));
		else
			println();
	}
}
