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
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.BundleHelper;
import org.osgi.framework.hooks.resolver.ResolverHook;

public class BundleValidationOperation implements IWorkspaceRunnable {

	private static StateObjectFactory FACTORY;

	private final Set<IPluginModelBase> fModels;
	private final Dictionary<String, String>[] fProperties;
	private final ResolverHook fResolverHook;
	private State fState;

	@SuppressWarnings("unchecked")
	public BundleValidationOperation(Set<IPluginModelBase> models) {
		this(models, new Dictionary[] { TargetPlatformHelper.getTargetEnvironment() });
	}

	public BundleValidationOperation(Set<IPluginModelBase> models, Dictionary<String, String>[] properties) {
		this(models, properties, null);
	}

	public BundleValidationOperation(Set<IPluginModelBase> models, Dictionary<String, String>[] properties, ResolverHook resolverHook) {
		fModels = models;
		fProperties = properties;
		fResolverHook = resolverHook;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		if (FACTORY == null) {
			FACTORY = BundleHelper.getPlatformAdmin().getFactory();
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, fModels.size() + 1);
		fState = FACTORY.createState(true);
		if (fResolverHook != null) {
			fState.setResolverHookFactory(c -> fResolverHook);
		}
		long id = 1;
		for (IPluginModelBase fModel : fModels) {
			BundleDescription bundle = fModel.getBundleDescription();
			if (bundle != null) {
				fState.addBundle(FACTORY.createBundleDescription(id++, bundle));
			}
			subMonitor.split(1);
		}
		fState.setPlatformProperties(fProperties);
		fState.resolve(false);
		subMonitor.split(1);
	}

	public Map<Object, Object[]> getResolverErrors() {
		Set<String> alreadyDuplicated = new HashSet<>();
		Map<Object, Object[]> map = new LinkedHashMap<>();
		BundleDescription[] bundles = fState.getBundles();
		for (BundleDescription bundle : bundles) {
			if (!bundle.isResolved()) {
				map.put(bundle, fState.getResolverErrors(bundle));
			} else if (bundle.isSingleton() && !alreadyDuplicated.contains(bundle.getSymbolicName())) {
				BundleDescription[] dups = fState.getBundles(bundle.getSymbolicName());
				if (dups.length > 1) {
					// more than 1 singleton present
					alreadyDuplicated.add(bundle.getSymbolicName());
					MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, NLS.bind(PDECoreMessages.BundleValidationOperation_multiple_singletons, new String[] {Integer.toString(dups.length), bundle.getSymbolicName()}), null);
					for (BundleDescription dup : dups) {
						status.add(Status.error(dup.getLocation()));
					}
					map.put(bundle, new Object[] {status});
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
			for (BundleDescription bundle : bundles) {
				if (!bundle.isResolved()) {
					return true;
				} else if (bundle.isSingleton()) {
					BundleDescription[] dups = fState.getBundles(bundle.getSymbolicName());
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
