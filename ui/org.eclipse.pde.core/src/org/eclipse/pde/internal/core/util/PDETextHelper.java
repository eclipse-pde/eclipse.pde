/*******************************************************************************
 *  Copyright (c) 2006, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDETextHelper {

	public static final String F_DOTS = "..."; //$NON-NLS-1$

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

	public static boolean isDefined(String text) {
		if ((text == null) || (text.length() == 0)) {
			return false;
		}
		return true;
	}

	public static boolean isDefinedAfterTrim(String text) {
		if (text == null) {
			return false;
		}
		String trimmedText = text.trim();
		if (trimmedText.length() == 0) {
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

			if ((currentChar == '\r') || (currentChar == '\n') || (currentChar == '\t')) {
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

	public static String translateWriteText(String text, HashMap substituteChars) {
		return translateWriteText(text, null, substituteChars);
	}

	public static String translateWriteText(String text, HashSet tagExceptions, HashMap substituteChars) {
		// Ensure not null
		if (text == null) {
			return ""; //$NON-NLS-1$
		}
		// Process tag exceptions if provided
		boolean processTagExceptions = false;
		int scanLimit = 0;
		if ((tagExceptions != null) && (tagExceptions.isEmpty() == false)) {
			processTagExceptions = true;
			// Use the biggest entry in the set as the limit
			scanLimit = determineMaxLength(tagExceptions);
		}
		// Process substitute characters if provided
		boolean processSubstituteChars = false;
		if ((substituteChars != null) && (substituteChars.isEmpty() == false)) {
			processSubstituteChars = true;
		}
		// Translated buffer
		StringBuffer buffer = new StringBuffer(text.length());
		// Visit each character in text
		for (IntegerPointer index = new IntegerPointer(0); index.getInteger() < text.length(); index.increment()) {
			// Process the current character
			char currentChar = text.charAt(index.getInteger());
			boolean processed = false;
			// If we are processing tag exceptions, check to see if this
			// character is part of a tag exception and process it accordingly
			// if it is
			if ((processed == false) && (processTagExceptions == true)) {
				processed = processTagExceptions(currentChar, substituteChars, tagExceptions, buffer, scanLimit, text, index);
			}
			// If the character was not part of a tag exception and we are
			// processing substitution characters, check to see if this 
			// character needs to be translated and process it accordingly if
			// it is
			if ((processed == false) && (processSubstituteChars == true)) {
				processed = processSubstituteChars(currentChar, substituteChars, buffer);
			}
			// If the character did not need to be translated, just append it
			// as is to the buffer
			if (processed == false) {
				buffer.append(currentChar);
			}
		}
		// Return the translated buffer
		return buffer.toString();
	}

	private static boolean processSubstituteChars(char currentChar, HashMap substituteChars, StringBuffer buffer) {
		Character character = new Character(currentChar);
		if (substituteChars.containsKey(character)) {
			String value = (String) substituteChars.get(character);
			if (isDefined(value)) {
				// Append the value if defined
				buffer.append(value);
			}
			// If the value was not defined, then we will strip the character
			return true;
		}
		return false;
	}

	private static boolean processTagExceptions(char currentChar, HashMap substituteChars, HashSet tagExceptions, StringBuffer buffer, int scanLimit, String text, IntegerPointer index) {
		// If the current character is an open angle bracket, then it may be
		// part of a valid tag exception
		if (currentChar == '<') {
			// Determine whether this bracket is part of a tag that is a
			// valid tag exception
			// Respect character array boundaries. Adjust accordingly
			int limit = text.length();
			// Scan ahead buffer
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
					if (isValidTagException(tagExceptions, futureBuffer)) {
						// The element tag is a valid tag exception
						// Process the tag exception characters
						processTagExceptionCharacters(substituteChars, buffer, futureBuffer);
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

	private static void processTagExceptionCharacters(HashMap substituteChars, StringBuffer buffer, String text) {
		// Get the tag name
		String tagName = getTagName(text);
		// Determine if there is a trailing forward slash
		boolean trailingSlash = text.endsWith("/"); //$NON-NLS-1$
		// Extract the attribute list of the element tag content
		// It may contain trailing spaces or a trailing '/'
		String attributeList = text.substring(tagName.length());
		// If the attribute list is not valid, discard the attribute list
		if ((isValidTagAttributeList(attributeList) == false)) {
			buffer.append('<');
			buffer.append(tagName);
			// Since, this tag has an attribute list and we are discarding it,
			// we have to make sure to replace the trailing slash if it had one
			if (trailingSlash) {
				buffer.append('/');
			}
			buffer.append('>');
			return;
		} else if (attributeList.length() == 0) {
			// If the tag has no attribute list then just return the tag
			// as is (trailing slash is already including in the tag name)
			buffer.append('<');
			buffer.append(tagName);
			buffer.append('>');
			return;
		}
		boolean inQuote = false;
		// Append the opening element bracket
		buffer.append('<');
		// Traverse the tag element content character by character
		// Translate any substitution characters required only inside attribute
		// value double-quotes
		for (int i = 0; i < text.length(); i++) {
			boolean processed = false;
			char currentChar = text.charAt(i);
			boolean onQuote = (currentChar == '"');
			// Determine whether we are currently processing the quote character
			if (onQuote) {
				if (inQuote) {
					// Quote encountered is an end quote
					inQuote = false;
				} else {
					// Quote encountered is a begin quote
					inQuote = true;
				}
			}
			// If we are currently within an attribute value double-quotes and
			// not on a quote character, translate this character if necessary
			if (inQuote && !onQuote) {
				processed = processSubstituteChars(currentChar, substituteChars, buffer);
			}
			// If the character did not require translation, just append it 
			// as-is
			if (processed == false) {
				buffer.append(currentChar);
			}
		}
		// Append the closing element bracket
		buffer.append('>');
	}

	private static boolean isValidTagException(HashSet tagExceptions, String buffer) {
		// Sample buffer format:
		// NO '<'
		// tagName att1="value" att2="value"
		// NO '>'
		// Parse tag name and ignore attributes (if any specified)
		String tagName = getTagName(buffer);
		// Check to see if the tag name is a tag exception
		if (tagExceptions.contains(tagName)) {
			return true;
		}
		return false;
	}

	private static boolean isValidTagAttributeList(String text) {
		// Determine whether the given attribute list is formatted in the
		// valid name="value" XML format
		// Sample formats:
		// " att1="value1" att2="value2"
		// " att1="value1" att2="value2 /"
		// " att1="value1"

		//			              space  attributeName      space = space  "attributeValue" space /
		String patternString = "^([\\s]+[A-Za-z0-9_:\\-\\.]+[\\s]?=[\\s]?\".+?\")*[\\s]*[/]?$"; //$NON-NLS-1$
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(text);
		// Determine whether the given attribute list matches the pattern
		return matcher.find();
	}

	private static String getTagName(String buffer) {
		// Sample buffer format:
		// NO '<'
		// tagName att1="value" att2="value"
		// NO '>'		
		StringBuffer tagName = new StringBuffer();
		// The tag name is every non-whitespace character in the buffer until
		// a whitespace character is encountered
		for (int i = 0; i < buffer.length(); i++) {
			char character = buffer.charAt(i);
			if (Character.isWhitespace(character)) {
				break;
			}
			tagName.append(character);
		}
		return tagName.toString();
	}

	private static int determineMaxLength(HashSet set) {
		Iterator iterator = set.iterator();
		int maxLength = -1;
		while (iterator.hasNext()) {
			// Has to be a String
			String object = (String) iterator.next();
			if (object.length() > maxLength) {
				maxLength = object.length();
			}
		}
		return maxLength;
	}

	private static class IntegerPointer {

		private int fInteger;

		public IntegerPointer(int integer) {
			fInteger = integer;
		}

		public int getInteger() {
			return fInteger;
		}

		public void setInteger(int integer) {
			fInteger = integer;
		}

		public void increment() {
			fInteger++;
		}
	}

}
