/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

import java.util.HashSet;



/**
 * PDETextHelper
 *
 */
public class PDETextHelper {

	public static final String F_DOTS = "..."; //$NON-NLS-1$

	public static final String F_AMPERSAND = "&amp;"; //$NON-NLS-1$

	public static final String F_LESS_THAN = "&lt;"; //$NON-NLS-1$

	public static final String F_GREATER_THAN = "&gt;"; //$NON-NLS-1$

	public static final String F_APOSTROPHE = "&apos;"; //$NON-NLS-1$

	public static final String F_QUOTE = "&quot;"; //$NON-NLS-1$

	
	/**
	 * @param text
	 * @return
	 */
	public static String truncateAndTrailOffText(String text, int limit) {
		String trimmed = text.trim();
		int dotsLength = F_DOTS.length();
		int trimmedLength = trimmed.length();
		int limitWithDots = limit - dotsLength;
		
		if (limit >= trimmedLength) {
			return trimmed;
		}
		// limit <= trimmedLength
		if (limit <= dotsLength) {
			return ""; //$NON-NLS-1$
		}
		// dotsLength < limit < trimmedLength
		return trimmed.substring(0, limitWithDots) + F_DOTS;
	}
	
	/**
	 * @param text
	 * @return
	 */
	public static boolean isDefined(String text) {
		if ((text == null) || 
				(text.length() == 0)) {
			return false;
		}
		return true;
	}
	
	/**
	 * Strips \n, \r, \t
	 * Strips leading spaces, trailing spaces, duplicate spaces
	 * @param text
	 * @return never null
	 */
	public static String translateReadText(String text) {
		// Ensure not null
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		String result = ""; //$NON-NLS-1$
		// Get rid of leading and trailing whitespace
		String inputText = text.trim();
		int length = inputText.length();
		char previousChar = ' ';
		StringBuffer buffer = new StringBuffer(length);
		// Visit each character in text
		for (int i = 0; i < length; i++) {
			char currentChar = inputText.charAt(i);
			
			if (currentChar == ' ') {
				// Skip multiple spaces
				if (previousChar != ' ') {
					buffer.append(currentChar);
					previousChar = currentChar;
				}
			} else if ((currentChar != '\r') && (currentChar != '\n') &&
						(currentChar != '\t')) {
				// Skip carriage returns, newlines and tabs
				buffer.append(currentChar);
				previousChar = currentChar;
			}
		}
		result = buffer.toString();
		if (PDEHTMLHelper.isAllWhitespace(result)) {
			return ""; //$NON-NLS-1$
		}
		return result;
	}	


	/**
	 * Strips \n, \r, \t
	 * Strips leading spaces, trailing spaces, duplicate spaces
	 * Translates &, <, >, ', "
	 * To &amp;, &lt;, &gt;, &apos;, &quot;
	 * @param text
	 * @return
	 */
	public static String translateWriteText(String text) {
		return translateWriteText(text, null, 0);
	}
	
	/**
	 * Strips \n, \r, \t
	 * Strips leading spaces, trailing spaces, duplicate spaces
	 * Translates &, <, >, ', "
	 * To &amp;, &lt;, &gt;, &apos;, &quot;
	 * Leaves tag exceptions specified intact
	 * @param text
	 * @param exceptions
	 * @param scanLimit
	 * @return
	 */
	public static String translateWriteText(String text, HashSet exceptions, 
			int scanLimit) {
		// Ensure not null
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		// Process exceptions if provided
		boolean processExceptions = false;
		if ((exceptions != null) && 
				(exceptions.isEmpty() == false)) {
			processExceptions = true;
		}
		String result = ""; //$NON-NLS-1$
		// Trim leading and trailing whitespace
		String inputText = text.trim();
		int length = inputText.length();
		char previousChar = ' ';
		StringBuffer buffer = new StringBuffer(length);
		// Visit each character in text
		for (int i = 0; i < length; i++) {
			char currentChar = inputText.charAt(i);
			
			if ((processExceptions == true) && 
					(currentChar == '<')) {
				// Determine whether this bracket is part of a tag that is a
				// valid tag exception
				// Respect character array boundaries. Adjust accordingly
				int limit = scanLimit + i + 2;
				if (length < limit) {
					limit = length;
				}
				boolean foundMatch = false;
				StringBuffer parsedText = new StringBuffer();
				// Scan ahead in text to parse out a possible element tag name
				for (int j = i + 1; j < limit; j++) {
					char futureChar = inputText.charAt(j);
					if (futureChar == '>') {
						// An ending bracket was found
						// This is indeed a element tag
						// Determine if the element tag we found is a valid 
						// tag exception
						String futureBuffer = parsedText.toString();
						if (exceptions.contains(futureBuffer)) {
							// The element tag is a valid tag exception
							buffer.append('<' + futureBuffer + '>');
							// Fast forward the current index to the scanned ahead
							// index to skip what we just found
							i = j;
							foundMatch = true;
						}					
						break;
					}
					// Accumulate the possible element tag name
					parsedText.append(futureChar);
				}
				if (foundMatch ==  false) {
					// No match to a valid tag exception was found
					// Escape the bracket
					buffer.append(F_LESS_THAN); 
				}
			} else if (currentChar == ' ') {
				// Skip multiple spaces
				if (previousChar != ' ') {
					buffer.append(currentChar);
					previousChar = currentChar;
				}
			} else if ((currentChar != '\r') && (currentChar != '\n') &&
						(currentChar != '\t')) {
				// Skip carriage returns, newlines and tabs
				buffer.append(encode(currentChar));
				previousChar = currentChar;
			}
		}
		result = buffer.toString();
		if (PDEHTMLHelper.isAllWhitespace(result)) {
			return ""; //$NON-NLS-1$
		}
		return result;
	}
	
	/**
	 * @param value
	 * @return
	 */
	public static String encode(char value) {
		String result = null;
		switch (value) {
			case '&':
				result = F_AMPERSAND; 
				break;
			case '<':
				result = F_LESS_THAN; 
				break;
			case '>':
				result = F_GREATER_THAN; 
				break;
			case '\'':
				result = F_APOSTROPHE; 
				break;
			case '\"':
				result = F_QUOTE; 
				break;
			default:
				result = (new Character(value)).toString();
				break;
		}
		return result;
	}
	
}
