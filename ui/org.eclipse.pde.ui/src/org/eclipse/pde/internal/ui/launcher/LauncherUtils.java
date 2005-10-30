/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.widgets.Display;

public class LauncherUtils {
	
	public static IPath getDefaultPath() {
		String userHome = System.getProperty("user.home"); //$NON-NLS-1$
		if (userHome != null && userHome.length() > 0)
			return new Path("${system_property:user.home}"); //$NON-NLS-1$
		return PDEPlugin.getWorkspace().getRoot().getLocation().removeLastSegments(1);
	}
	
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}
	
	
	public static void setDefaultSourceLocator(ILaunchConfiguration configuration)
			throws CoreException {
		ILaunchConfigurationWorkingCopy wc = null;
		if (configuration.isWorkingCopy()) {
			wc = (ILaunchConfigurationWorkingCopy) configuration;
		} else {
			wc = configuration.getWorkingCopy();
		}
		
		// set any old source locators to null.  Source locator is now declared in the plugin.xml
		String locator = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
		if (locator != null)
			wc.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID,(String) null);
		
		// set source path provider on pre-2.1 configurations
		String id = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, (String) null);
		if (id == null) 
			wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "org.eclipse.pde.ui.workbenchClasspathProvider"); //$NON-NLS-1$
		
		if (locator != null || id == null)
			wc.doSave();
	}
	
	public static boolean clearWorkspace(ILaunchConfiguration configuration, String workspace, IProgressMonitor monitor) throws CoreException {
		if (workspace.length() == 0)
			return false;
		
		File workspaceFile = new Path(workspace).toFile();
		if (configuration.getAttribute(IPDELauncherConstants.DOCLEAR, false) && workspaceFile.exists()) {
			boolean doClear = !configuration.getAttribute(IPDELauncherConstants.ASKCLEAR, true);
			if (!doClear) {
				int result = confirmDeleteWorkspace(workspaceFile);
				if (result == 2) {
					monitor.done();
					return false;
				}
				doClear = result == 0;
			}
			if (doClear) {
				CoreUtility.deleteContent(workspaceFile);
			}
		}
		monitor.done();
		return true;
	}
	
	private static int confirmDeleteWorkspace(final File workspaceFile) {
		final int[] result = new int[1];
		getDisplay().syncExec(new Runnable() {
			public void run() {
				String title = PDEUIMessages.LauncherUtils_title;
				String message =
					NLS.bind(PDEUIMessages.WorkbenchLauncherConfigurationDelegate_confirmDeleteWorkspace, workspaceFile.getPath());
				MessageDialog dialog = new MessageDialog(getDisplay().getActiveShell(), title, null,
						message, MessageDialog.QUESTION, new String[]{IDialogConstants.YES_LABEL,
								IDialogConstants.NO_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
				result[0] = dialog.open();
			}
		});
		return result[0];
	}

}
