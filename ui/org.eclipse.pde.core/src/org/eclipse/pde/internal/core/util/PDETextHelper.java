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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;



/**
 * PDETextHelper
 *
 */
public class PDETextHelper {

	public static final String F_DOTS = "..."; //$NON-NLS-1$
	
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

			if ((currentChar == '\r') || (currentChar == '\n') ||
					(currentChar == '\t')) {
				// Convert newlines, carriage returns and tabs to spaces
				currentChar = ' ';
			}
			
			if (currentChar == ' ') {
				// Skip multiple spaces
				if (previousChar != ' ') {
					buffer.append(currentChar);
					previousChar = currentChar;
				}
			} else {
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
	 * @param text
	 * @param substituteChars
	 * @return
	 */
	public static String translateWriteText(String text, HashMap substituteChars) {
		return translateWriteText(text, null, substituteChars);
	}
	
	/**
	 * @param text
	 * @param tagExceptions
	 * @param substituteChars
	 * @return
	 */
	public static String translateWriteText(String text, HashSet tagExceptions, 
			HashMap substituteChars) {

		// Ensure not null
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		
		// Process tag exceptions if provided
		boolean processTagExceptions = false;
		int scanLimit = 0;		
		if ((tagExceptions != null) && 
				(tagExceptions.isEmpty() == false)) {
			processTagExceptions = true;
			// Use the biggest entry in the set as the limit
			scanLimit = determineMaxLength(tagExceptions);
		}
		
		// Process substitute characters if provided
		boolean processSubstituteChars = false;
		if ((substituteChars != null) && 
				(substituteChars.isEmpty() == false)) {
			processSubstituteChars = true;
		}
		
		StringBuffer buffer = new StringBuffer(text.length());
		
		// Visit each character in text
		for (IntegerPointer index = new IntegerPointer(0); 
				index.getInteger() < text.length(); 
				index.increment()) {
			
			char currentChar = text.charAt(index.getInteger());
			boolean processed = false;
			
			if ((processed == false) && 
					(processTagExceptions == true)) {
				processed = processTagExceptions(currentChar, tagExceptions, 
						buffer, scanLimit, text, index);
			}

			if ((processed == false) && 
					(processSubstituteChars == true)) {
				processed = processSubstituteChars(currentChar, substituteChars,
						buffer);
			}			
			
			if (processed == false) {
				buffer.append(currentChar);					
			}
		}

		return buffer.toString();
	}

	/**
	 * @param currentChar
	 * @param substituteChars
	 * @param buffer
	 * @return
	 */
	private static boolean processSubstituteChars(char currentChar, 
			HashMap substituteChars, StringBuffer buffer) {
		Character character = new Character(currentChar);
		if (substituteChars.containsKey(character)) {
			String value = (String)substituteChars.get(character);
			if (isDefined(value)) {
				// Append the value if defined
				buffer.append(value);
			}
			// If the value was not defined, then we will strip the character
			return true;
		}
		return false;
	}
	
	/**
	 * @param currentChar
	 * @param tagExceptions
	 * @param buffer
	 * @param scanLimit
	 * @param inputText
	 * @param index
	 * @return
	 */
	private static boolean processTagExceptions(char currentChar, 
			HashSet tagExceptions, StringBuffer buffer, int scanLimit, 
			String text, IntegerPointer index) {

		if (currentChar == '<') {
			// Determine whether this bracket is part of a tag that is a
			// valid tag exception
			// Respect character array boundaries. Adjust accordingly
			int limit = scanLimit + index.getInteger() + 2;
			if (text.length() < limit) {
				limit = text.length();
			}
			StringBuffer parsedText = new StringBuffer();
			// Scan ahead in text to parse out a possible element tag name
			for (int j = index.getInteger() + 1; j < limit; j++) {
				char futureChar = text.charAt(j);
				if (futureChar == '>') {
					// An ending bracket was found
					// This is indeed a element tag
					// Determine if the element tag we found is a valid 
					// tag exception
					String futureBuffer = parsedText.toString();
					if (tagExceptions.contains(futureBuffer)) {
						// The element tag is a valid tag exception
						buffer.append('<' + futureBuffer + '>');
						// Fast forward the current index to the scanned ahead
						// index to skip what we just found
						index.setInteger(j);
						return true;
					}					
					return false;
				}
				// Accumulate the possible element tag name
				parsedText.append(futureChar);
			}
		}
		return false;
	}	
	
	/**
	 * @param set
	 * @return
	 */
	private static int determineMaxLength(HashSet set) {
		Iterator iterator = set.iterator();
		int maxLength = -1;
		while (iterator.hasNext()) {
			// Has to be a String
			String object = (String)iterator.next();
			if (object.length() > maxLength) {
				maxLength = object.length();
			}
		}
		return maxLength;
	}
	
	/**
	 * IntegerPointer
	 *
	 */
	private static class IntegerPointer {

		private int fInteger;
		
		/**
		 * 
		 */
		public IntegerPointer(int integer) {
			fInteger = integer;
		}
		
		/**
		 * @return
		 */
		public int getInteger() {
			return fInteger;
		}
		
		/**
		 * @param integer
		 */
		public void setInteger(int integer) {
			fInteger = integer;
		}
		
		/**
		 * 
		 */
		public void increment() {
			fInteger++;
		}
	}
	
}
