/*******************************************************************************
 * Copyright (c) 2008, 2019 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 549441, Bug 489181
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
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

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
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

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		registerImageDescriptor(registry, SharedImages.DESC_IMPLEMENTATION);
		registerImageDescriptor(registry, SharedImages.DESC_PROPERTIES);
		registerImageDescriptor(registry, SharedImages.DESC_PROPERTY);
		registerImageDescriptor(registry, SharedImages.DESC_PROVIDE);
		registerImageDescriptor(registry, SharedImages.DESC_REFERENCE);
		registerImageDescriptor(registry, SharedImages.DESC_REFERENCE_ZERO_N);
		registerImageDescriptor(registry, SharedImages.DESC_REFERENCE_ZERO_ONE);
		registerImageDescriptor(registry, SharedImages.DESC_REFERENCE_ONE_N);
		registerImageDescriptor(registry, SharedImages.DESC_ROOT);
		registerImageDescriptor(registry, SharedImages.DESC_SERVICE);
		registerImageDescriptor(registry, SharedImages.DESC_SERVICES);
		registerImageDescriptor(registry, SharedImages.DESC_DS);
		registerImageDescriptor(registry, SharedImages.DESC_ATTR);
		registerImageDescriptor(registry, SharedImages.OVR_DYNAMIC);
		registerImageDescriptor(registry, SharedImages.DESC_DETAILS);
		registerImageDescriptor(registry, SharedImages.DESC_DS_WIZ);
	}

	private void registerImageDescriptor(ImageRegistry registry, String id) {
		ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, id).ifPresent(d -> registry.put(id, d));
	}

	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	public static void logException(Throwable e, final String title,
			String message) {
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}
		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			if (message == null) {
				message = e.getMessage();
			}
			if (message == null) {
				message = e.toString();
			}
			status = Status.error(message, e);
		}
		ResourcesPlugin.getPlugin().getLog().log(status);
		Display display = Display.getCurrent() != null ? Display.getCurrent()
				: Display.getDefault();
		final IStatus fstatus = status;
		display.asyncExec(() -> ErrorDialog.openError(null, title, null, fstatus));
	}

	public static void logException(Throwable e) {
		logException(e, null, null);
	}


}
