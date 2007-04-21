/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.graphics.Image;

public class EclipsePluginValidationOperation extends LaunchValidationOperation {
	
	private Map fExtensionErrors = new HashMap(2);
	private static Object[] EMPTY = new Object[0];
	

	public EclipsePluginValidationOperation(ILaunchConfiguration configuration) {
		super(configuration);
	}

	protected IPluginModelBase[] getModels() throws CoreException{
		return LaunchPluginValidator.getPluginList(fLaunchConfiguration);
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
				String product = fLaunchConfiguration.getAttribute(IPDELauncherConstants.PRODUCT, (String)null);
				if (product != null) {
					validateExtension(product);
					String application = getApplication(product);
					if (application != null)
						validateExtension(application);
				}
			} else {
				String configType = fLaunchConfiguration.getType().getName();
				String attribute = configType.equals(EclipseLaunchShortcut.CONFIGURATION_TYPE)
									? IPDELauncherConstants.APPLICATION : IPDELauncherConstants.APP_TO_TEST;
				String application = fLaunchConfiguration.getAttribute(attribute, TargetPlatform.getDefaultApplication());
				if (!IPDEUIConstants.CORE_TEST_APPLICATION.equals(application)) {
					validateExtension(application);
				}						
			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
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
					if ("org.eclipse.core.runtime.products".equals(point)  //$NON-NLS-1$
							&& product.equals(IdUtil.getFullId(ext))) { 
						if (ext.getChildCount() == 1) {
							IPluginElement prod = (IPluginElement)ext.getChildren()[0];
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
		
	private void validateExtension(String id) {
		String bundleID = id.substring(0, id.lastIndexOf('.'));
		BundleDescription bundle = getState().getBundle(bundleID, null);
		if (bundle == null) {
			String name = NLS.bind(PDEUIMessages.EclipsePluginValidationOperation_pluginMissing, bundleID);
			PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
			Image image = provider.get(PDEPluginImages.DESC_PLUGIN_OBJ);
			fExtensionErrors.put(new NamedElement(name, image), EMPTY);
		}
	}
	
	public boolean hasErrors() {
		return super.hasErrors() || fExtensionErrors.size() > 1;
	}
	
	public Map getInput() {
		Map map = super.getInput();
		map.putAll(fExtensionErrors);
		return map;
	}

}
