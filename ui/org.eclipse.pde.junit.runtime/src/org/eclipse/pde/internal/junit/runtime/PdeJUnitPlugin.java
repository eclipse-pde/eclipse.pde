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

public class PdeJUnitPlugin extends Plugin {
	
	private static PdeJUnitPlugin inst;
	private ResourceBundle resourceBundle;
	
	public PdeJUnitPlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
	}
	
	public static PdeJUnitPlugin getDefault() {
		return inst;
	}
	
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	
	public static String getResourceString(String key) {
		ResourceBundle bundle = PdeJUnitPlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	public ResourceBundle getResourceBundle() {
		try {
			resourceBundle =
				ResourceBundle.getBundle(
				"org.eclipse.pde.internal.junit.runtime.junitresources"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}
	
	
}
