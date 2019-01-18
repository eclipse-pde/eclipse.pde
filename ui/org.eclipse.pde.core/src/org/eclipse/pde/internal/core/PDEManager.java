/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.osgi.framework.Constants;

public class PDEManager {

	public static IFragmentModel[] findFragmentsFor(IPluginModelBase model) {
		ArrayList<IPluginModelBase> result = new ArrayList<>();
		BundleDescription desc = getBundleDescription(model);
		if (desc != null) {
			BundleDescription[] fragments = desc.getFragments();
			for (BundleDescription fragment : fragments) {
				IPluginModelBase candidate = PluginRegistry.findModel(fragment);
				if (candidate instanceof IFragmentModel) {
					result.add(candidate);
				}
			}
		}
		return result.toArray(new IFragmentModel[result.size()]);
	}

	public static IPluginModel findHostFor(IFragmentModel fragment) {
		BundleDescription desc = getBundleDescription(fragment);
		if (desc != null) {
			HostSpecification spec = desc.getHost();
			if (spec != null) {
				IPluginModelBase host = PluginRegistry.findModel(spec.getName());
				if (host instanceof IPluginModel) {
					return (IPluginModel) host;
				}
			}
		}
		return null;
	}

	private static BundleDescription getBundleDescription(IPluginModelBase model) {
		BundleDescription desc = model.getBundleDescription();

		if (desc == null && model.getUnderlyingResource() != null) {
			// the model may be an editor model.
			// editor models don't carry a bundle description
			// get the core model counterpart.
			IProject project = model.getUnderlyingResource().getProject();
			IPluginModelBase coreModel = PluginRegistry.findModel(project);
			if (coreModel != null) {
				desc = coreModel.getBundleDescription();
			}
		}
		return desc;
	}

	public static URL[] getNLLookupLocations(IPluginModelBase model) {
		ArrayList<URL> urls = new ArrayList<>();
		addNLLocation(model, urls);
		if (model instanceof IPluginModel) {
			IFragmentModel[] fragments = findFragmentsFor(model);
			for (IFragmentModel fragment : fragments) {
				addNLLocation(fragment, urls);
			}
		} else if (model instanceof IFragmentModel) {
			IPluginModel host = findHostFor((IFragmentModel) model);
			if (host != null) {
				addNLLocation(host, urls);
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	private static void addNLLocation(IPluginModelBase model, ArrayList<URL> urls) {
		// We should use model.getNLLookupLocation(), but it doesn't return an encoded url (Bug 403512)
		if (model.getInstallLocation() != null) {
			try {
				URI encodedURI = URIUtil.toURI(model.getInstallLocation(), true);
				urls.add(encodedURI.toURL());
			} catch (MalformedURLException e) {
			}
		}
	}

	/**
	 * Returns the bundle localization file specified by the manifest header or the default location.  The
	 * file may not exist.
	 *
	 * @param model the plug-in to lookup the localization for
	 * @return the bundle localization file location or the default location
	 */
	public static String getBundleLocalization(IPluginModelBase model) {
		if (model instanceof IBundlePluginModelBase && model.getUnderlyingResource() != null) {
			return ((IBundlePluginModelBase) model).getBundleLocalization();
		}

		if (model instanceof ExternalPluginModelBase) {
			return ((ExternalPluginModelBase) model).getLocalization();
		}

		return Constants.BUNDLE_LOCALIZATION_DEFAULT_BASENAME;
	}

}
