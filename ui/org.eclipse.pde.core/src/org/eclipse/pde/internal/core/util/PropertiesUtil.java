/*
 * Created on Sep 26, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.util;

import java.io.*;
import java.util.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class PropertiesUtil {
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

	public static String createWritableName(String source) {
		if (source.indexOf(' ') == -1)
			return source;
		// has blanks 
		StringBuffer writableName = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == ' ') {
				writableName.append("\\ "); //$NON-NLS-1$
			} else
				writableName.append(c);
		}
		return writableName.toString();
	}

	public static String createEscapedValue(String value) {
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
		for (;tokens.hasMoreElements();) {
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
	
	public static String writeKeyValuePair(String name, String[] tokens) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(createWritableName(name));
		buffer.append(" = "); //$NON-NLS-1$
		int indentLength = name.length() + 3;
		for (int i = 0; i < tokens.length; i++) {
			buffer.append(createEscapedValue(tokens[i]));
			if (i < tokens.length - 1) {
				buffer.append(",\\" + System.getProperty("line.separator")); //$NON-NLS-1$ //$NON-NLS-2$
				for (int j = 0; j < indentLength; j++) {
					buffer.append(" "); //$NON-NLS-1$
				}
			}
		}	
		buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
		return buffer.toString();
	}
}
