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
package org.eclipse.pde.internal.core.build;

import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.IBuildEntry;

public class BuildEntry extends BuildObject implements IBuildEntry {
	private Vector tokens = new Vector();
	private String name;
	private static final char[] HEX =
		{
			'0',
			'1',
			'2',
			'3',
			'4',
			'5',
			'6',
			'7',
			'8',
			'9',
			'A',
			'B',
			'C',
			'D',
			'E',
			'F' };

	public BuildEntry(String name) {
		this.name = name;
	}
	public void addToken(String token) throws CoreException {
		ensureModelEditable();
		tokens.add(token);
		getModel().fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.INSERT,
				new Object[] { token },
				null));
	}
	public String getName() {
		return name;
	}
	public String[] getTokens() {
		String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}

	public boolean contains(String token) {
		return tokens.contains(token);
	}
	void processEntry(String value) {
		StringTokenizer stok = new StringTokenizer(value, ",");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			token = token.trim();
			tokens.add(token);
		}
	}
	public void removeToken(String token) throws CoreException {
		ensureModelEditable();
		tokens.remove(token);
		getModel().fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.REMOVE,
				new Object[] { token },
				null));
	}
	public void renameToken(String oldName, String newName)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.elementAt(i).toString().equals(oldName)) {
				tokens.setElementAt(newName, i);
				break;
			}
		}
		getModel().fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.CHANGE,
				new Object[] { oldName },
				null));
	}
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		String oldValue = this.name;
		this.name = name;
		getModel().fireModelObjectChanged(this, P_NAME, oldValue, name);
	}
	public String toString() {
		return name;
	}

	private String createWritableName(String source) {
		if (source.indexOf(' ') == -1)
			return source;
		// has blanks 
		StringBuffer writableName = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == ' ') {
				writableName.append("\\ ");
			} else
				writableName.append(c);
		}
		return writableName.toString();
	}

	private String createEscapedValue(String value) {
		// if required, escape property values as \\uXXXX		
		StringBuffer buf = new StringBuffer(value.length() * 2);
		// assume expansion by less than factor of 2
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (character == '\\'
				|| character == '\t'
				|| character == '\r'
				|| character == '\n'
				|| character == '\f') {
				// handle characters requiring leading \
				buf.append('\\');
				buf.append(character);
			} else if ((character < 0x0020) || (character > 0x007e)) {
				// handle characters outside base range (encoded)
				buf.append('\\');
				buf.append('u');
				buf.append(HEX[(character >> 12) & 0xF]); // first nibble
				buf.append(HEX[(character >> 8) & 0xF]); // second nibble
				buf.append(HEX[(character >> 4) & 0xF]); // third nibble
				buf.append(HEX[character & 0xF]); // fourth nibble
			} else {
				// handle base characters
				buf.append(character);
			}
		}
		return buf.toString();
	}

	public void write(String indent, PrintWriter writer) {
		String writableName = createWritableName(name);
		writer.print(writableName + " = ");
		if (tokens.size() == 0) {
			writer.println();
			return;
		}
		int indentLength = name.length() + 3;
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.elementAt(i).toString();
			writer.print(createEscapedValue(token));
			if (i < tokens.size() - 1) {
				writer.println(",\\");
				for (int j = 0; j < indentLength; j++) {
					writer.print(" ");
				}
			} else
				writer.println("");
		}
	}
}
