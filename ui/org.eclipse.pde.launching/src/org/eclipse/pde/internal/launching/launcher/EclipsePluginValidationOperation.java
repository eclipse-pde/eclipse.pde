/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.*;

public class EclipsePluginValidationOperation extends LaunchValidationOperation {
	public static final int CREATE_EXTENSION_ERROR_CODE = 1000;

	private Map fExtensionErrors = new HashMap(2);
	private static Object[] EMPTY = new Object[0];

	public EclipsePluginValidationOperation(ILaunchConfiguration configuration) {
		super(configuration);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation#getModels()
	 */
	protected IPluginModelBase[] getModels() throws CoreException {
		return BundleLauncherHelper.getMergedBundles(fLaunchConfiguration, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		super.run(monitor);
		if (fExtensionErrors.size() > 0)
			fExtensionErrors.clear();
		validateExtensions();
	}

	private void validateExtensions() {
		try {
			String[] required = RequirementHelper.getApplicationRequirements(fLaunchConfiguration);
			for (int i = 0; i < required.length; i++) {
				BundleDescription bundle = getState().getBundle(required[i], null);
				if (bundle == null) {
					String message = NLS.bind(PDEMessages.EclipsePluginValidationOperation_pluginMissing, required[i]);
					Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, CREATE_EXTENSION_ERROR_CODE, message, null);
					IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
					Object extensionError = null;
					if (statusHandler == null)
						extensionError = status.getMessage();
					else
						extensionError = statusHandler.handleStatus(status, required[i]);
					fExtensionErrors.put(extensionError, EMPTY);
				}
			}
		} catch (CoreException e) {
			PDELaunchingPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation#hasErrors()
	 */
	public boolean hasErrors() {
		return super.hasErrors() || fExtensionErrors.size() >= 1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.launching.launcher.LaunchValidationOperation#getInput()
	 */
	public Map getInput() {
		Map map = super.getInput();
		map.putAll(fExtensionErrors);
		return map;
	}

}
