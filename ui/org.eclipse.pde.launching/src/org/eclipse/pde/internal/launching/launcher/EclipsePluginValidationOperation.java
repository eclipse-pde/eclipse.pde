/*******************************************************************************
 * Copyright (c) 2007, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.launching.IPDEConstants;
import org.eclipse.pde.internal.launching.PDELaunchingPlugin;
import org.eclipse.pde.internal.launching.PDEMessages;

public class EclipsePluginValidationOperation extends LaunchValidationOperation {
	public static final int CREATE_EXTENSION_ERROR_CODE = 1000;

	private static final Object[] EMPTY = new Object[0];
	private final Map<Object, Object[]> fExtensionErrors = new HashMap<>(2);

	public EclipsePluginValidationOperation(ILaunchConfiguration configuration, Set<IPluginModelBase> models) {
		this(configuration, models, null);
	}

	public EclipsePluginValidationOperation(ILaunchConfiguration configuration, Set<IPluginModelBase> models, String launchMode) {
		super(configuration, models, launchMode);
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		super.run(monitor);
		if (!fExtensionErrors.isEmpty())
			fExtensionErrors.clear();
		validateExtensions();
	}

	private void validateExtensions() {
		try {
			List<String> required = RequirementHelper.getApplicationLaunchRequirements(fLaunchConfiguration);
			for (String element : required) {
				BundleDescription bundle = getState().getBundle(element, null);
				if (bundle == null) {
					String message = NLS.bind(PDEMessages.EclipsePluginValidationOperation_pluginMissing, element);
					Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, CREATE_EXTENSION_ERROR_CODE, message, null);
					IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
					Object extensionError = null;
					if (statusHandler == null)
						extensionError = status.getMessage();
					else
						extensionError = statusHandler.handleStatus(status, element);
					fExtensionErrors.put(extensionError, EMPTY);
				}
			}
		} catch (CoreException e) {
			PDELaunchingPlugin.log(e);
		}
	}

	@Override
	public boolean hasErrors() {
		return super.hasErrors() || fExtensionErrors.size() >= 1;
	}

	@Override
	public Map<Object, Object[]> getInput() {
		Map<Object, Object[]> map = super.getInput();
		map.putAll(fExtensionErrors);
		return map;
	}

}
