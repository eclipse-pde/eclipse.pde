/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.registry.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class PDERuntimePlugin extends AbstractUIPlugin {
	
	private static PDERuntimePlugin inst;
	private ResourceBundle resourceBundle;

	public PDERuntimePlugin(IPluginDescriptor descriptor) {
		super(descriptor);
		inst = this;
		try {
			resourceBundle =
				ResourceBundle.getBundle(
					"org.eclipse.pde.internal.runtime.pderuntimeresources");
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
	}
	public static IWorkbenchPage getActivePage() {
		return getDefault().internalGetActivePage();
	}
	public static Shell getActiveWorkbenchShell() {
		return getActiveWorkbenchWindow().getShell();
	}
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	public static PDERuntimePlugin getDefault() {
		return inst;
	}
	/* package */
	static IPath getInstallLocation() {
		return new Path(getDefault().getDescriptor().getInstallURL().getFile());
	}
	public static String getPluginId() {
		return getDefault().getDescriptor().getUniqueIdentifier();
	}
	public java.util.ResourceBundle getResourceBundle() {
		return resourceBundle;
	}
	public static String getResourceString(String key) {
		ResourceBundle bundle = inst.getResourceBundle();
		if (bundle != null) {
			try {
				String bundleString = bundle.getString(key);
				//return "$"+bundleString;
				return bundleString;
			} catch (MissingResourceException e) {
				// default actions is to return key, which is OK
			}
		}
		return key;
	}
	
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	public void startup() throws CoreException {
		IAdapterManager manager = Platform.getAdapterManager();
		RegistryPropertySourceFactory factory =
			new RegistryPropertySourceFactory();
		manager.registerAdapters(factory, PluginObjectAdapter.class);
	}
	
}
