/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;

/**
 * Version identifier for @since tags. It consists of an optional bundle name followed by a 
 * version string that follows the format defined in the class {@link Version}
 *
 * @since 3.4
 */
public class SinceTagVersion {
	private String prefixString;
	private Version version;
	private String versionString;
	private String postfixString;
	private static final Pattern VERSION_PATTERN = Pattern.compile("([0-9]+\\.?[0-9]?\\.?[0-9]?\\.?[A-Za-z0-9]*)");; //$NON-NLS-1$

	/**
	 * Creates a new instance.
	 * 
	 * @param value the given since tag value
	 * @throws IllegalArgumentException if the given value is null
	 */
	public SinceTagVersion(String value) {
		if (value == null) {
			throw new IllegalArgumentException("The given value cannot be null"); //$NON-NLS-1$
		}
		char[] chars = value.toCharArray();
		final int READ_VERSION = 0;
		final int INSIDE_VERSION = 1;

		int mode = READ_VERSION;
		
		loop: for (int i = 0, max = chars.length; i < max; i++) {
			char currentChar = chars[i];
			switch(mode) {
			case READ_VERSION :
				switch(currentChar) {
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
						mode = INSIDE_VERSION;
						i--;
						break;
					default :
				}
				break;
			case INSIDE_VERSION :
				// read sequence of digits
				int start = i;
				loop2 : while (i < max) {
					if (Character.isWhitespace(currentChar)) {
						break loop2;
					}
					if (Character.isLetterOrDigit(currentChar)
							|| currentChar == '.') {
						currentChar = chars[i];
						i++;
					} else {
						break loop2;
					}
				}
				// extract "version string"
				String potentialVersion = null;
				if (i == max) {
					potentialVersion= value.substring(start, i);
				} else {
					potentialVersion= value.substring(start, i - 1);
				}
				// check if it matches
				Matcher m = VERSION_PATTERN.matcher(potentialVersion);
				if (m.find()) {
					this.versionString = potentialVersion;
					if (start != 0) {
						this.prefixString = value.substring(0, start);
						// if prefixString doesn't end with a space, this is a wrong version
						if (Character.isLetterOrDigit(value.charAt(start - 1))) {
							this.versionString = null;
							this.prefixString = null;
							continue loop;
						}
					}
					if (i != max) {
						this.postfixString = value.substring(i - 1);
					}
					try {
						this.version = new Version(this.versionString);
					} catch (IllegalArgumentException e) {
						// ignore - wrong format
					}
					return;
				}
				mode = READ_VERSION;
			}
		}
		if (this.versionString == null) {
			this.postfixString = value;
		}
	}

	/**
	 * Returns the version part of the @since tag as a string. null if the given version did not have the right format
	 *
	 * @return the version part of the @since tag
	 */
	public String getVersionString() {
		return this.versionString;
	}

	/**
	 * Returns the version part of the @since tag. null if the given version did not have the right format
	 *
	 * @return the version part of the @since tag
	 */
	public Version getVersion() {
		return this.version;
	}
	
	/**
	 * Returns the prefix part of the @since tag. It can be null.
	 *
	 * @return the prefix part of the @since tag
	 */
	public String prefixString() {
		return this.prefixString;
	}
	/**
	 * Returns the postfix part of the @since tag. It can be null.
	 *
	 * @return the postfix part of the @since tag
	 */
	public String postfixString() {
		return this.postfixString;
	}
}
