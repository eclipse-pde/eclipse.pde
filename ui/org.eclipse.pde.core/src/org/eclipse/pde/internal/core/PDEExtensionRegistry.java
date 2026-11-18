/*******************************************************************************
 * Copyright (c) 2006, 2026 IBM Corporation and others.
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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.osgi.resource.Resource;

public class PDEExtensionRegistry {

	private final Object fMasterKey = new Object();
	private final Object fUserKey = new Object();
	private volatile IExtensionRegistry fRegistry = null;
	private PDERegistryStrategy fStrategy = null;

	private IPluginModelBase[] fModels = null;
	/**
	 * Incremented on every target reload, so that a registry is never created
	 * from models collected before that reload. Only modified while holding
	 * this object's monitor.
	 */
	private volatile int fTargetGeneration = 0;
	private final ArrayList<IRegistryChangeListener> fListeners = new ArrayList<>();

	private static final String EXTENSION_DIR = ".extensions"; //$NON-NLS-1$

	public PDEExtensionRegistry() {
		if (fStrategy == null) {
			File extensionsDir = new File(PDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR);
			// create the strategy without creating registry.  That way we create the registry at the last possible moment.
			// This way we can listen to events in PDE without creating the registry until we need it.
			fStrategy = new PDERegistryStrategy(new File[] {extensionsDir}, new boolean[] {false}, fMasterKey, this);
		}
	}

	public PDEExtensionRegistry(IPluginModelBase[] models) {
		fModels = models;
		if (fStrategy == null) {
			File extensionsDir = new File(PDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR);
			// Use TargetPDERegistryStrategy so we don't connect listeners to PluginModelManager.  This is used only in target so we don't need change events.
			fStrategy = new TargetPDERegistryStrategy(new File[] {extensionsDir}, new boolean[] {false}, fMasterKey, this);
		}
	}

	// Methods used to control information/status of Extension Registry

	protected IPluginModelBase[] getModels() {
		if (fModels == null) {
			// get all workspace and external models.  Make sure workspace models come first
			IPluginModelBase[] workspaceModels = PluginRegistry.getWorkspaceModels();
			IPluginModelBase[] externalModels = PluginRegistry.getExternalModels();
			IPluginModelBase[] allModels = new IPluginModelBase[workspaceModels.length + externalModels.length];
			System.arraycopy(workspaceModels, 0, allModels, 0, workspaceModels.length);
			System.arraycopy(externalModels, 0, allModels, workspaceModels.length, externalModels.length);
			return allModels;
		}
		return fModels;
	}

	public void stop() {
		if (fRegistry != null) {
			// the cache written on stop has to carry the timestamp of the current
			// models, not of those the registry was created from
			fStrategy.setModels(getModels());
			fRegistry.stop(fMasterKey);
		}
		dispose();
	}

	protected IExtensionRegistry getRegistry() {
		while (true) {
			IExtensionRegistry registry = fRegistry;
			if (registry != null) {
				return registry;
			}
			int generation = fTargetGeneration;
			// Collect the models before taking this object's monitor: collecting
			// them takes the lock of the PluginModelManager, whose model change
			// listeners ask for this registry while holding that lock. The
			// strategy, which the registry calls while holding the monitor, gets
			// these models handed over instead of reading them itself.
			IPluginModelBase[] models = getModels();
			synchronized (this) {
				if (fRegistry == null && generation == fTargetGeneration) {
					fRegistry = createRegistry(models);
				}
				if (fRegistry != null) {
					return fRegistry;
				}
			}
			// the target was reloaded while the models were collected, so they
			// may be outdated
		}
	}

	/** Has to be called while holding this object's monitor **/
	private IExtensionRegistry createRegistry(IPluginModelBase[] models) {
		fStrategy.setModels(models);
		IExtensionRegistry registry = RegistryFactory.createRegistry(fStrategy, fMasterKey, fUserKey);
		for (IRegistryChangeListener listener : fListeners) {
			registry.addRegistryChangeListener(listener);
		}
		return registry;
	}

	/**
	 * Discards the registry, which is created again from the reloaded target's
	 * models on next access.
	 */
	public synchronized void targetReloaded() {
		// stop old registry (which will write contents to FS) and delete the cache it creates
		// might see if we can dispose of a registry without writing to file system.  NOTE: Don't call stop() because we want to still reuse fStrategy
		// The cache is deleted right below, so stopping the registry computes its
		// timestamp from the models the registry was created from instead of
		// reading the current ones while holding this object's monitor (see
		// getRegistry()).
		if (fRegistry != null) {
			fRegistry.stop(fMasterKey);
		}
		CoreUtility.deleteContent(new File(PDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR));
		fRegistry = null;
		fTargetGeneration++;
		// release the models of the previous target
		fStrategy.setModels(new IPluginModelBase[0]);
	}

	// dispose of registry without writing contents.
	public void dispose() {
		fStrategy.dispose();
		fRegistry = null;
	}

	// Methods to access data in Extension Registry

	public IPluginModelBase[] findExtensionPlugins(String pointId, boolean activeOnly) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			// if extension point for extension does not exist, search all plug-ins manually
			return activeOnly ? PluginRegistry.getActiveModels() : PluginRegistry.getAllModels();
		}
		IExtension[] exts = point.getExtensions();
		HashSet<IPluginModelBase> plugins = new HashSet<>();
		for (IExtension ext : exts) {
			IPluginModelBase base = getPlugin(ext.getContributor(), false);
			if (base != null && !plugins.contains(base) && (!activeOnly || base.isEnabled())) {
				plugins.add(base);
			}
		}
		return plugins.toArray(new IPluginModelBase[plugins.size()]);
	}

	/*
	 * Returns IPluginModelBase even if the model is not enabled
	 */
	public IPluginModelBase findExtensionPointPlugin(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			return null;
		}
		IContributor contributor = point.getContributor();
		return getPlugin(contributor, true);
	}

	private IExtensionPoint getExtensionPoint(String pointId) {
		return getRegistry().getExtensionPoint(pointId);
	}

	/*
	 * Return true if the extension registry has any bundle (enabled/disabled) with the Extension Point specified
	 */
	public boolean hasExtensionPoint(String pointId) {
		//		IExtensionPoint point = getExtensionPoint(pointId);
		//		IPluginModelBase base = (point != null) ? getPlugin(point.getContributor(), false) : null;
		//		return (base != null) ? base.isEnabled() : false;
		return getExtensionPoint(pointId) != null;
	}

	/*
	 * Returns IPluginExtenionPoint for extension point id for any model (both enabled/disabled)
	 */
	public IPluginExtensionPoint findExtensionPoint(String pointId) {
		IExtensionPoint extPoint = getExtensionPoint(pointId);
		if (extPoint != null) {
			IPluginModelBase model = getPlugin(extPoint.getContributor(), true);
			if (model != null) {
				IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
				for (IPluginExtensionPoint point : points) {
					if (point.getFullId().equals(pointId)) {
						return point;
					}
				}
			}
		}
		return null;
	}

	public IPluginExtension[] findExtensionsForPlugin(IPluginModelBase base) {
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null) {
			return new IPluginExtension[0];
		}
		IExtension[] extensions = getRegistry().getExtensions(fStrategy.createContributor(base));
		ArrayList<PluginExtension> list = new ArrayList<>();
		for (IExtension ext : extensions) {
			PluginExtension extension = new PluginExtension(ext);
			extension.setModel(getExtensionsModel(base));
			extension.setParent(base.getExtensions());
			list.add(extension);
		}
		return list.toArray(new IPluginExtension[list.size()]);
	}

	public IPluginExtensionPoint[] findExtensionPointsForPlugin(IPluginModelBase base) {
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null) {
			return new IPluginExtensionPoint[0];
		}
		IExtensionPoint[] extensions = getRegistry().getExtensionPoints(fStrategy.createContributor(base));
		ArrayList<PluginExtensionPoint> list = new ArrayList<>();
		for (IExtensionPoint extension : extensions) {
			PluginExtensionPoint point = new PluginExtensionPoint(extension);
			point.setModel(getExtensionsModel(base));
			point.setParent(base.getExtensions());
			list.add(point);
		}
		return list.toArray(new IPluginExtensionPoint[list.size()]);
	}

	private ISharedPluginModel getExtensionsModel(IPluginModelBase base) {
		if (base instanceof IBundlePluginModelBase) {
			return ((IBundlePluginModelBase) base).getExtensionsModel();
		}
		return base;
	}

	public IExtension[] findExtensions(String extensionPointId, boolean activeOnly) {
		ArrayList<IExtension> list = new ArrayList<>();
		IExtensionPoint point = getExtensionPoint(extensionPointId);
		if (point != null) {
			IExtension[] extensions = point.getExtensions();
			if (!activeOnly) {
				return extensions;
			}
			for (IExtension extension : extensions) {
				IPluginModelBase base = getPlugin(extension.getContributor(), true);
				if (base != null && base.isEnabled()) {
					list.add(extension);
				}
			}
		} else {
			IPluginModelBase[] bases = activeOnly ? PluginRegistry.getActiveModels() : PluginRegistry.getAllModels();
			for (IPluginModelBase base : bases) {
				IContributor contributor = fStrategy.createContributor(base);
				if (contributor == null) {
					continue;
				}
				IExtension[] extensions = getRegistry().getExtensions(contributor);
				for (IExtension extension : extensions) {
					if (extension.getExtensionPointUniqueIdentifier().equals(extensionPointId)) {
						list.add(extension);
					}
				}
			}
		}
		return list.toArray(new IExtension[list.size()]);
	}

	// make sure we return the right IPluginModelBase when we have multiple versions of a plug-in Id
	private IPluginModelBase getPlugin(IContributor icontributor, boolean searchAll) {
		if (!(icontributor instanceof RegistryContributor contributor)) {
			return null;
		}
		long bundleId = Long.parseLong(contributor.getActualId());
		Resource desc = PDECore.getDefault().getModelManager().getState().getState()
				.getBundle(Long.parseLong(contributor.getActualId()));
		if (desc != null) {
			return PluginRegistry.findModel(desc);
		}
		// desc might be null if the workspace contains a plug-in with the same Bundle-SymbolicName
		ModelEntry entry = PluginRegistry.findEntry(contributor.getActualName());
		if (entry != null) {
			if (!searchAll && entry.getWorkspaceModels().length > 0) {
				return null;
			}
			IPluginModelBase externalModels[] = entry.getExternalModels();
			for (IPluginModelBase model : externalModels) {
				BundleDescription extDesc = model.getBundleDescription();
				if (extDesc != null && extDesc.getBundleId() == bundleId) {
					return model;
				}
			}
		}
		return null;
	}

	// Methods to add/remove listeners

	public void addListener(IRegistryChangeListener listener) {
		fRegistry.addRegistryChangeListener(listener);
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}
	}

	public void removeListener(IRegistryChangeListener listener) {
		fRegistry.removeRegistryChangeListener(listener);
		fListeners.remove(listener);
	}

}
