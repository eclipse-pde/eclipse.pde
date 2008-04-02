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

import org.osgi.framework.Version;

/**
 * Version identifier for @since tags. It consists of an optional bundle name followed by a 
 * version string that follows the format defined in the class {@link Version}
 *
 * @since 3.4
 */
public class SinceTagVersion {
	private String pluginName;
	private Version version;

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
		int index = value.indexOf(' ');
		if (index == -1) {
			// should be only a version; no plugin name
			try {
				this.version = new Version(value);
			} catch (IllegalArgumentException e) {
				// ignore - wrong format
			}
		} else {
			// plugin name followed by a version
			this.pluginName = value.substring(0, index);
			try {
				this.version = new Version(value.substring(index + 1, value.length()).trim());
			} catch(IllegalArgumentException e) {
				// ignore - wrong format
			}
		}
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
	 * Returns the plugin name part of the @since tag. It can be null.
	 *
	 * @return the plugin name part of the @since tag
	 */
	public String pluginName() {
		return this.pluginName;
	}
}
