/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import java.util.*;

import org.eclipse.core.runtime.*;

/**
 * @author melhem
 *
 */
public class PdeJUnitPlugin extends Plugin {
	
	private static PdeJUnitPlugin inst;
	private ResourceBundle resourceBundle;
	
	public PdeJUnitPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle(
				"org.eclipse.pde.internal.junit.runtime.junitresources"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	
	public static PdeJUnitPlugin getDefault() {
		return inst;
	}
	
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	
	public static String getFormattedMessage(String key, String[] args) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, args);
	}
	
	static IPath getInstallLocation() {
		return new Path(inst.getDescriptor().getInstallURL().getFile());
	}
	
	public static String getPluginId() {
		return inst.getDescriptor().getUniqueIdentifier();
	}
	
	public static String getResourceString(String key) {
		ResourceBundle bundle = inst.getResourceBundle();
		if (bundle != null) {
			try {
				String bundleString = bundle.getString(key);
				return bundleString;
			} catch (MissingResourceException e) {
			}
		}
		return key;
	}
	
	public ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	
	
}
