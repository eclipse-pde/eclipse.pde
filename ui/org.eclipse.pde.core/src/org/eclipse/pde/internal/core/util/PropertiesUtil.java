/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.io.PrintWriter;
import java.util.Enumeration;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class PropertiesUtil {
	private static final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	public static String createWritableName(String source) {
		if (source.indexOf(' ') >= 0) {
			// has blanks 
			StringBuffer writableName = new StringBuffer();
			for (int i = 0; i < source.length(); i++) {
				char c = source.charAt(i);
				if (c == ' ') {
					writableName.append("\\ "); //$NON-NLS-1$
				} else
					writableName.append(c);
			}
			source = writableName.toString();
		}
		return createEscapedValue(source);
	}

	public static String createEscapedValue(String value) {
		// if required, escape property values as \\uXXXX		
StringBuffer buf = new StringBuffer(value.length() * 2);
		// assume expansion by less than factor of 2
		for (int i = 0; i < value.length(); i++) {
			char character = value.charAt(i);
			if (character == '\\' || character == '\t' || character == '\r' || character == '\n' || character == '\f') {
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

	/*
	 * Copied from org.eclipse.jdt.internal.ui.refactoring.nls.ExternalizeWizardPage.windEscapeChars(String s)
	 */
	public static String windEscapeChars(String s) {
		if (s == null)
			return null;

		char aChar;
		int len = s.length();
		StringBuffer outBuffer = new StringBuffer(len);

		for (int x = 0; x < len;) {
			aChar = s.charAt(x++);
			if (aChar == '\\') {
				if (x >= len)
					break;
				aChar = s.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
						if (x >= len)
							break;
						aChar = s.charAt(x++);
						switch (aChar) {
							case '0' :
							case '1' :
							case '2' :
							case '3' :
							case '4' :
							case '5' :
							case '6' :
							case '7' :
							case '8' :
							case '9' :
								value = (value << 4) + aChar - '0';
								break;
							case 'a' :
							case 'b' :
							case 'c' :
							case 'd' :
							case 'e' :
							case 'f' :
								value = (value << 4) + 10 + aChar - 'a';
								break;
							case 'A' :
							case 'B' :
							case 'C' :
							case 'D' :
							case 'E' :
							case 'F' :
								value = (value << 4) + 10 + aChar - 'A';
								break;
							default :
								throw new IllegalArgumentException("Malformed \\uxxxx encoding."); //$NON-NLS-1$
						}
					}
					outBuffer.append((char) value);
				} else {
					if (aChar == 't') {
						outBuffer.append('\t');
					} else {
						if (aChar == 'r') {
							outBuffer.append('\r');
						} else {
							if (aChar == 'n') {
								outBuffer.append('\n');
							} else {
								if (aChar == 'f') {
									outBuffer.append('\f');
								} else {
									outBuffer.append(aChar);
								}
							}
						}
					}
				}
			} else
				outBuffer.append(aChar);
		}
		return outBuffer.toString();
	}

	public static void writeKeyValuePair(String indent, String name, String value, PrintWriter writer) {
		String writableName = createWritableName(name);
		writer.print(writableName + " = "); //$NON-NLS-1$

		writer.println(PropertiesUtil.createEscapedValue(value));
	}

	public static void writeKeyValuePair(String indent, String name, Enumeration tokens, PrintWriter writer) {
		String writableName = createWritableName(name);
		writer.print(writableName + " = "); //$NON-NLS-1$
		if (!tokens.hasMoreElements()) {
			writer.println();
			return;
		}
		int indentLength = name.length() + 3;
		for (; tokens.hasMoreElements();) {
			String token = tokens.nextElement().toString();
			writer.print(PropertiesUtil.createEscapedValue(token));
			if (tokens.hasMoreElements()) {
				writer.println(",\\"); //$NON-NLS-1$
				for (int j = 0; j < indentLength; j++) {
					writer.print(" "); //$NON-NLS-1$
				}
			} else
				writer.println(""); //$NON-NLS-1$
		}
	}

	public static int getInsertOffset(IDocument doc) {
		int offset = doc.getLength();
		for (int i = doc.getNumberOfLines() - 1; i >= 0; i--) {
			try {
				if (doc.get(doc.getLineOffset(i), doc.getLineLength(i)).trim().length() > 0) {
					break;
				}
				offset = doc.getLineOffset(i);
			} catch (BadLocationException e) {
			}
		}
		return offset;
	}

	public static boolean isNewlineNeeded(IDocument doc) throws BadLocationException {
		int line = doc.getLineOfOffset(getInsertOffset(doc));
		return doc.get(doc.getLineOffset(line), doc.getLineLength(line)).trim().length() > 0;
	}

}
