/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.plugin.*;
import org.osgi.framework.*;


public class PDERuntimePlugin extends AbstractUIPlugin {
	
	private static PDERuntimePlugin inst;
	private ResourceBundle resourceBundle;
	private BundleContext fContext;

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
	
	public static String getPluginId() {
		return getDefault().getBundle().getSymbolicName();
	}
	
	public PDERuntimePlugin() {
		inst = this;
	}
	
	public java.util.ResourceBundle getResourceBundle() {
		try {
			if (resourceBundle == null)
				resourceBundle = ResourceBundle.getBundle("org.eclipse.pde.internal.runtime.pderuntimeresources"); //$NON-NLS-1$
		} catch (MissingResourceException x) {
			resourceBundle = null;
		}
		return resourceBundle;
	}
	
	public static String getResourceString(String key) {
		ResourceBundle bundle = PDERuntimePlugin.getDefault().getResourceBundle();
		try {
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e) {
			return key;
		}
	}
	
	public static String getFormattedMessage(String key, String arg) {
		String text = getResourceString(key);
		return java.text.MessageFormat.format(text, new Object[] { arg });
	}
	
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.fContext = context;
		IAdapterManager manager = Platform.getAdapterManager();
		RegistryPropertySourceFactory factory =
			new RegistryPropertySourceFactory();
		manager.registerAdapters(factory, PluginObjectAdapter.class);
	}
	
	public BundleContext getBundleContext() {
		return this.fContext;
	}
	
	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		inst = null;
		resourceBundle = null;
	}

}
