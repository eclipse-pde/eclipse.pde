/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\s([0-9]+\\.?[0-9]?\\.?[0-9]?\\.?[A-Za-z0-9]*)\\s");; //$NON-NLS-1$
	private static final Pattern NO_SPACE_VERSION_PATTERN = Pattern.compile("([0-9]+\\.?[0-9]?\\.?[0-9]?\\.?[A-Za-z0-9]*)");; //$NON-NLS-1$
	private static final Pattern NO_LEADING_SPACE_VERSION_PATTERN = Pattern.compile("([0-9]+\\.?[0-9]?\\.?[0-9]?\\.?[A-Za-z0-9]*)\\s");; //$NON-NLS-1$

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
		Matcher m = VERSION_PATTERN.matcher(value);
		if (m.find()) {
			this.versionString = m.group(1);
			// check for a version that would start the value string
			m = NO_LEADING_SPACE_VERSION_PATTERN.matcher(value);
			if (m.find()) {
				this.versionString = m.group(1);
			}
		} else {
			m = NO_SPACE_VERSION_PATTERN.matcher(value);
			if (m.find()) {
				this.versionString = m.group(1);
			}
		}
		if (this.versionString != null) {
			int prefixIndex = value.indexOf(this.versionString);
			if (versionString.length() != value.length()) {
				if (prefixIndex != 0) {
					this.prefixString = value.substring(0, prefixIndex);
				}
			}
			// extract postfix
			int beginIndex = prefixIndex + this.versionString.length();
			if (beginIndex != value.length()) {
				this.postfixString = value.substring(beginIndex);
			}
			try {
				this.version = new Version(this.versionString);
			} catch (IllegalArgumentException e) {
				// ignore - wrong format
			}
		} else if (value.length() != 0) {
			// we consider the actual string as the postfix value
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
