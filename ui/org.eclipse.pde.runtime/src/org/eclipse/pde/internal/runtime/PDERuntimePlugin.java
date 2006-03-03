/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


public class PDERuntimePlugin extends AbstractUIPlugin {
	
	private static PDERuntimePlugin inst;

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
	
	private IWorkbenchPage internalGetActivePage() {
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.fContext = context;
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
	}

}
