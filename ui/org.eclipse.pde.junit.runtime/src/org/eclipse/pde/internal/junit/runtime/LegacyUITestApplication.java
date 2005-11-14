/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

import junit.framework.Assert;

import org.eclipse.core.boot.IPlatformRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;

public class LegacyUITestApplication implements IPlatformRunnable {

	private static final String DEFAULT_APP_PRE_3_0 = "org.eclipse.ui.workbench"; //$NON-NLS-1$

	public Object run(final Object args) throws Exception {
		IPlatformRunnable object = getApplication((String[]) args);
		
		Assert.assertNotNull(object);
		Assert.assertTrue(object instanceof IWorkbench);

		final IWorkbench workbench = (IWorkbench) object;
		// the 'started' flag is used so that we only run tests when the window
		// is opened for the first time only.
		final boolean[] started = { false };
		workbench.addWindowListener(new IWindowListener() {
			public void windowOpened(IWorkbenchWindow w) {
				if (started[0])
					return;
				w.getShell().getDisplay().asyncExec(new Runnable() {
					public void run() {
						started[0] = true;
						LegacyRemotePluginTestRunner.main((String[]) args);
						workbench.close();
					}
				});
			}
			public void windowActivated(IWorkbenchWindow window) {
			}
			public void windowDeactivated(IWorkbenchWindow window) {
			}
			public void windowClosed(IWorkbenchWindow window) {
			}
		});
		return ((IPlatformRunnable) workbench).run(args);
	}
	
	
	private IPlatformRunnable getApplication(String[] args) throws CoreException {
		IExtension extension =
			Platform.getPluginRegistry().getExtension(
				Platform.PI_RUNTIME,
				Platform.PT_APPLICATIONS,
				DEFAULT_APP_PRE_3_0);

		Assert.assertNotNull(extension);

		// If the extension does not have the correct grammar, return null.
		// Otherwise, return the application object.
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run"); //$NON-NLS-1$
			if (runs.length > 0) {
				Object runnable = runs[0].createExecutableExtension("class"); //$NON-NLS-1$
				if (runnable instanceof IPlatformRunnable)
					return (IPlatformRunnable) runnable;
			}
		}
		return null;
	}
}
