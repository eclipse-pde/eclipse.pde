/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.pde.ds.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	protected void initializeImageRegistry(ImageRegistry registry) {
		registry.put(SharedImages.DESC_IMPLEMENTATION,
				createImageDescriptor(SharedImages.DESC_IMPLEMENTATION));
		registry.put(SharedImages.DESC_PROPERTIES,
				createImageDescriptor(SharedImages.DESC_PROPERTIES));
		registry.put(SharedImages.DESC_PROPERTY,
				createImageDescriptor(SharedImages.DESC_PROPERTY));
		registry.put(SharedImages.DESC_PROVIDE,
				createImageDescriptor(SharedImages.DESC_PROVIDE));
		registry.put(SharedImages.DESC_REFERENCE,
				createImageDescriptor(SharedImages.DESC_REFERENCE));
		registry.put(SharedImages.DESC_REFERENCE_ZERO_N,
				createImageDescriptor(SharedImages.DESC_REFERENCE_ZERO_N));
		registry.put(SharedImages.DESC_REFERENCE_ZERO_ONE,
				createImageDescriptor(SharedImages.DESC_REFERENCE_ZERO_ONE));
		registry.put(SharedImages.DESC_REFERENCE_ONE_N,
				createImageDescriptor(SharedImages.DESC_REFERENCE_ONE_N));
		registry.put(SharedImages.DESC_ROOT,
				createImageDescriptor(SharedImages.DESC_ROOT));
		registry.put(SharedImages.DESC_SERVICE,
				createImageDescriptor(SharedImages.DESC_SERVICE));
		registry.put(SharedImages.DESC_SERVICES,
				createImageDescriptor(SharedImages.DESC_SERVICES));
		registry.put(SharedImages.DESC_DS,
				createImageDescriptor(SharedImages.DESC_DS));
		registry.put(SharedImages.DESC_ATTR,
				createImageDescriptor(SharedImages.DESC_ATTR));
		registry.put(SharedImages.OVR_DYNAMIC,
				createImageDescriptor(SharedImages.OVR_DYNAMIC));
		registry.put(SharedImages.DESC_DETAILS,
				createImageDescriptor(SharedImages.DESC_DETAILS));
		registry.put(SharedImages.DESC_DS_WIZ,
				createImageDescriptor(SharedImages.DESC_DS_WIZ));
	}
	
	private ImageDescriptor createImageDescriptor(String id) {
		return imageDescriptorFromPlugin(PLUGIN_ID, id);
	}
	
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return getDefault().getWorkbench().getActiveWorkbenchWindow();
	}
	
	public static void logException(Throwable e, final String title,
			String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException)
			status = ((CoreException) e).getStatus();
		else {
			if (message == null)
				message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK,
					message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
		Display display = Display.getCurrent() == null ? Display.getCurrent()
				: Display.getDefault();
		final IStatus fstatus = status;
		display.asyncExec(new Runnable() {
			public void run() {
				ErrorDialog.openError(null, title, null, fstatus);
			}
		});
	}
	
	public static void logException(Throwable e) {
		logException(e, null, null);
	}


}
