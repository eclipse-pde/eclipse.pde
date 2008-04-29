/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

public class StringHelper {
	private static final char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	public static String preparePropertiesString(String s, char[] newLine) {
		if (s == null)
			return null;
		int length = s.length();
		int nlLength = newLine.length;
		StringBuffer sb = new StringBuffer(length + nlLength);
		int i = 0;
		while (i < length) {
			char c = s.charAt(i);
			if (i + nlLength < length) {
				boolean notNewLine = false;
				for (int j = 0; j < nlLength; j++)
					if (s.charAt(i + j) != newLine[j])
						notNewLine = true;
				if (!notNewLine) {
					sb.append(unwindEscapeChars(new String(newLine)));
					// skip the nl chars
					i += nlLength;
					while (i < length && s.charAt(i) == ' ') {
						sb.append(' ');
						i++;
					}
					if (i < length) {
						sb.append("\\"); //$NON-NLS-1$
						sb.append(newLine);
					}
					continue;
				}
			}
			sb.append(unwindEscapeChars(Character.toString(c)));
			i++;
		}
		return sb.toString();
	}

	protected static String unwindEscapeChars(String s) {
		if (s != null) {
			int length = s.length();
			StringBuffer sb = new StringBuffer(length);
			for (int i = 0; i < length; i++) {
				char c = s.charAt(i);
				sb.append(getUnwoundString(c));
			}
			return sb.toString();
		}
		return null;
	}

	protected static String getUnwoundString(char c) {
		switch (c) {
			case '\b' :
				return "\\b";//$NON-NLS-1$
			case '\t' :
				return "\\t";//$NON-NLS-1$
			case '\n' :
				return "\\n";//$NON-NLS-1$
			case '\f' :
				return "\\f";//$NON-NLS-1$	
			case '\r' :
				return "\\r";//$NON-NLS-1$
			case '\\' :
				return "\\\\";//$NON-NLS-1$
			default :
				if (((c < 0x0020) || (c > 0x007e)))
					return new StringBuffer().append('\\').append('u').append(toHex((c >> 12) & 0xF)).append(toHex((c >> 8) & 0xF)).append(toHex((c >> 4) & 0xF)).append(toHex(c & 0xF)).toString();
		}
		return String.valueOf(c);
	}

	private static char toHex(int halfByte) {
		return HEX_DIGITS[(halfByte & 0xF)];
	}

	protected static String windEscapeChars(String s) {
		if (s == null)
			return null;

		char aChar;
		int len = s.length();
		StringBuffer outBuffer = new StringBuffer(len);

		for (int x = 0; x < len;) {
			aChar = s.charAt(x++);
			if (aChar == '\\') {
				aChar = s.charAt(x++);
				if (aChar == 'u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i < 4; i++) {
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

	protected static boolean isValidLocalization(String name) {
		if (name.length() <= 0) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if ((c < 'A' || 'Z' < c) && (c < 'a' || 'z' < c) && (c < '0' || '9' < c) && c != '_' && c != '-' && c != '/') {
				return false;
			}
		}
		return true;
	}
}
