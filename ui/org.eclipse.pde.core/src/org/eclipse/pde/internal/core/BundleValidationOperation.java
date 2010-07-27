/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.*;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class BundleValidationOperation implements IWorkspaceRunnable {

	private static StateObjectFactory FACTORY;

	private IPluginModelBase[] fModels;
	private Dictionary[] fProperties;
	private State fState;

	public BundleValidationOperation(IPluginModelBase[] models) {
		this(models, new Dictionary[] {TargetPlatformHelper.getTargetEnvironment()});
	}

	public BundleValidationOperation(IPluginModelBase[] models, Dictionary[] properties) {
		fModels = models;
		fProperties = properties;
	}

	public void run(IProgressMonitor monitor) throws CoreException {
		if (FACTORY == null)
			FACTORY = Platform.getPlatformAdmin().getFactory();
		monitor.beginTask("", fModels.length + 1); //$NON-NLS-1$
		fState = FACTORY.createState(true);
		for (int i = 0; i < fModels.length; i++) {
			BundleDescription bundle = fModels[i].getBundleDescription();
			if (bundle != null)
				fState.addBundle(FACTORY.createBundleDescription(bundle));
			monitor.worked(1);
		}
		fState.setPlatformProperties(fProperties);
		fState.resolve(false);
		monitor.done();
	}

	public Map getResolverErrors() {
		Set alreadyDuplicated = new HashSet();
		Map map = new HashMap();
		BundleDescription[] bundles = fState.getBundles();
		for (int i = 0; i < bundles.length; i++) {
			BundleDescription desc = bundles[i];
			if (!desc.isResolved()) {
				map.put(desc, fState.getResolverErrors(desc));
			} else if (desc.isSingleton() && !alreadyDuplicated.contains(desc.getSymbolicName())) {
				BundleDescription[] dups = fState.getBundles(desc.getSymbolicName());
				if (dups.length > 1) {
					// more than 1 singleton present
					alreadyDuplicated.add(desc.getSymbolicName());
					MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, NLS.bind(PDECoreMessages.BundleValidationOperation_multiple_singletons, new String[] {Integer.toString(dups.length), desc.getSymbolicName()}), null);
					for (int j = 0; j < dups.length; j++) {
						status.add(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, dups[j].getLocation()));
					}
					map.put(desc, new Object[] {status});
				}
			}
		}
		return map;
	}

	public State getState() {
		return fState;
	}

	public boolean hasErrors() {
		if (fState.getHighestBundleId() > -1) {
			BundleDescription[] bundles = fState.getBundles();
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription desc = bundles[i];
				if (!desc.isResolved()) {
					return true;
				} else if (desc.isSingleton()) {
					BundleDescription[] dups = fState.getBundles(desc.getSymbolicName());
					if (dups.length > 1) {
						// more than one singleton
						return true;
					}
				}
			}
		}
		return false;
	}

}
