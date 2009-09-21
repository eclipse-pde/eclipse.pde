/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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

import org.eclipse.pde.launching.IPDELauncherConstants;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.launching.*;

public class EclipsePluginValidationOperation extends LaunchValidationOperation {
	public static final int CREATE_EXTENSION_ERROR_CODE = 1000;

	private Map fExtensionErrors = new HashMap(2);
	private static Object[] EMPTY = new Object[0];

	public EclipsePluginValidationOperation(ILaunchConfiguration configuration) {
		super(configuration);
	}

	protected IPluginModelBase[] getModels() throws CoreException {
		return BundleLauncherHelper.getMergedBundles(fLaunchConfiguration, false);
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		super.run(monitor);
		if (fExtensionErrors.size() > 0)
			fExtensionErrors.clear();
		validateExtensions();
	}

	private void validateExtensions() {
		try {
			if (fLaunchConfiguration.getAttribute(IPDELauncherConstants.USE_PRODUCT, false)) {
				String product = fLaunchConfiguration.getAttribute(IPDELauncherConstants.PRODUCT, (String) null);
				if (product != null) {
					validateExtension(product);
					String application = getApplication(product);
					if (application != null)
						validateExtension(application);
				}
			} else {
				String configType = fLaunchConfiguration.getType().getIdentifier();
				String attribute = configType.equals(IPDELauncherConstants.ECLIPSE_APPLICATION_LAUNCH_CONFIGURATION_TYPE) ? IPDELauncherConstants.APPLICATION : IPDELauncherConstants.APP_TO_TEST;
				String application = fLaunchConfiguration.getAttribute(attribute, TargetPlatform.getDefaultApplication());
				if (!IPDEConstants.CORE_TEST_APPLICATION.equals(application)) {
					validateExtension(application);
				}
			}
		} catch (CoreException e) {
			PDELaunchingPlugin.log(e);
		}
	}

	private String getApplication(String product) {
		String bundleID = product.substring(0, product.lastIndexOf('.'));
		BundleDescription bundle = getState().getBundle(bundleID, null);
		if (bundle != null) {
			IPluginModelBase model = PluginRegistry.findModel(bundle);
			if (model != null) {
				IPluginExtension[] extensions = model.getPluginBase().getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					IPluginExtension ext = extensions[i];
					String point = ext.getPoint();
					if ("org.eclipse.core.runtime.products".equals(point) //$NON-NLS-1$
							&& product.equals(IdUtil.getFullId(ext))) {
						if (ext.getChildCount() == 1) {
							IPluginElement prod = (IPluginElement) ext.getChildren()[0];
							if (prod.getName().equals("product")) { //$NON-NLS-1$
								IPluginAttribute attr = prod.getAttribute("application"); //$NON-NLS-1$
								return attr != null ? attr.getValue() : null;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private void validateExtension(String id) throws CoreException {
		int index = id.lastIndexOf('.');
		if (index == -1)
			return;
		String bundleID = id.substring(0, index);
		BundleDescription bundle = getState().getBundle(bundleID, null);
		if (bundle == null) {
			String message = NLS.bind(PDEMessages.EclipsePluginValidationOperation_pluginMissing, bundleID);
			Status status = new Status(IStatus.ERROR, IPDEConstants.PLUGIN_ID, CREATE_EXTENSION_ERROR_CODE, message, null);
			IStatusHandler statusHandler = DebugPlugin.getDefault().getStatusHandler(status);
			Object extensionError = null;
			if (statusHandler == null)
				extensionError = status.getMessage();
			else
				extensionError = statusHandler.handleStatus(status, id);
			fExtensionErrors.put(extensionError, EMPTY);
		}
	}

	public boolean hasErrors() {
		return super.hasErrors() || fExtensionErrors.size() >= 1;
	}

	public Map getInput() {
		Map map = super.getInput();
		map.putAll(fExtensionErrors);
		return map;
	}

}
