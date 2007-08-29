/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListIterator;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginExtension;
import org.eclipse.pde.internal.core.plugin.PluginExtensionPoint;
import org.eclipse.pde.internal.core.util.CoreUtility;

public class PDEExtensionRegistry {
	
	private Object fMasterKey = new Object();
	private Object fUserKey = new Object();
	private IExtensionRegistry fRegistry = null;
	private PDERegistryStrategy fStrategy = null;
	
	private IPluginModelBase[] fModels = null;
	private ArrayList fListeners = new ArrayList();
	
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
		return (fModels == null) ? PluginRegistry.getActiveModels() : fModels;
	}
	
	public void stop() {
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
		dispose();
	}
	
	protected synchronized IExtensionRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry = createRegistry();
			for (ListIterator li = fListeners.listIterator(); li.hasNext();)
				fRegistry.addRegistryChangeListener((IRegistryChangeListener)li.next());
		}
		return fRegistry;
	}
	
	private IExtensionRegistry createRegistry() {
		return RegistryFactory.createRegistry(fStrategy, fMasterKey, fUserKey);
	}

	public void targetReloaded() {		
		// stop old registry (which will write contents to FS) and delete the cache it creates
		// might see if we can dispose of a registry without writing to file system.  NOTE: Don't call stop() because we want to still reuse fStrategy
		if (fRegistry != null)
			fRegistry.stop(fMasterKey);
		CoreUtility.deleteContent(new File(PDECore.getDefault().getStateLocation().toFile(), EXTENSION_DIR));
		fRegistry = null;
	}
	
	// dispose of registry without writing contents.
	public void dispose() {
		fStrategy.dispose();
		fRegistry = null;
	}
	
	// Methods to access data in Extension Registry
	
	public IPluginModelBase[] findExtensionPlugins(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			// if extension point for extension does not exist, search all plug-ins manually
			return PluginRegistry.getAllModels();
		}
		IExtension[] exts = point.getExtensions();
		HashMap plugins = new HashMap();
		for (int i = 0; i < exts.length; i++) {
			String pluginId = exts[i].getContributor().getName();
			if (plugins.containsKey(pluginId))
				continue;
			IPluginModelBase base = PluginRegistry.findModel(pluginId);
			if (base != null)
				plugins.put(pluginId, base);
		}
		java.util.Collection values = plugins.values();
		return (IPluginModelBase[])values.toArray(new IPluginModelBase[values.size()]);
	}
	
	public IPluginModelBase findExtensionPointPlugin(String pointId) {
		IExtensionPoint point = getExtensionPoint(pointId);
		if (point == null) {
			return null;
		}
		IContributor contributor = point.getContributor();
		return PluginRegistry.findModel(contributor.getName());
	}
	
	private IExtensionPoint getExtensionPoint(String pointId) {
		return getRegistry().getExtensionPoint(pointId);
	}
	
	public boolean hasExtensionPoint(String pointId) {
		return getExtensionPoint(pointId) != null;
	}
	
	public IPluginExtensionPoint findExtensionPoint(String pointId) {
		IExtensionPoint extPoint = getExtensionPoint(pointId);
		if (extPoint != null) {
			IPluginModelBase model = PluginRegistry.findModel(extPoint.getContributor().getName());
			if (model != null) {
				IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
				for (int i = 0; i < points.length; i++) {
					IPluginExtensionPoint point = points[i];
					if (points[i].getFullId().equals(pointId)) {
						return point;
					}
				}
			}
		}
		return null;
	}
	
	public IPluginExtension[] findExtensionsForPlugin(String pluginId) {
		IPluginModelBase base = PluginRegistry.findModel(pluginId);
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null)
			return new IPluginExtension[0];
		IExtension[] extensions = getRegistry().getExtensions(fStrategy.createContributor(base));
		ArrayList list = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			PluginExtension extension = new PluginExtension(extensions[i]);
			extension.setModel(getExtensionsModel(base));
			extension.setParent(base.getExtensions());
			list.add(extension);
		}
		return (IPluginExtension[]) list.toArray(new IPluginExtension[list.size()]);
	}
	
	public IPluginExtensionPoint[] findExtensionPointsForPlugin(String pluginId) {
		IPluginModelBase base = PluginRegistry.findModel(pluginId);
		IContributor contributor = fStrategy.createContributor(base);
		if (contributor == null) 
			return new IPluginExtensionPoint[0];
		IExtensionPoint[] extensions = getRegistry().getExtensionPoints(fStrategy.createContributor(base));
		ArrayList list = new ArrayList();
		for (int i = 0; i < extensions.length; i++) {
			PluginExtensionPoint point = new PluginExtensionPoint(extensions[i]);
			point.setModel(getExtensionsModel(base));
			point.setParent(base.getExtensions());
			list.add(point);
		}
		return (IPluginExtensionPoint[]) list.toArray(new IPluginExtensionPoint[list.size()]);
	}
	
	private ISharedPluginModel getExtensionsModel(IPluginModelBase base) {
		if (base instanceof IBundlePluginModelBase) 
			return ((IBundlePluginModelBase)base).getExtensionsModel();
		return base;
	}
	
	public IExtension[] findExtensions(String extensionPointId) {
		IExtensionPoint point = getExtensionPoint(extensionPointId);
		if (point != null) 
			return point.getExtensions();
		ArrayList list = new ArrayList();
		IPluginModelBase[] bases = PluginRegistry.getActiveModels();
		for (int i = 0; i < bases.length; i++) {
			IContributor contributor = fStrategy.createContributor(bases[i]);
			if (contributor == null)
				continue;
			IExtension[] extensions = getRegistry().getExtensions(contributor);
			for (int j = 0; j < extensions.length; j++) {
				if (extensions[j].getExtensionPointUniqueIdentifier().equals(extensionPointId))
					list.add(extensions[j]);
			}
		}
		return (IExtension[]) list.toArray(new IExtension[list.size()]);
	}
	
	// Methods to add/remove listeners
	
	public void addListener(IRegistryChangeListener listener) {
		fRegistry.addRegistryChangeListener(listener);
		if (!fListeners.contains(listener))
			fListeners.add(listener);
	}
	
	public void removeListener(IRegistryChangeListener listener) {
		fRegistry.removeRegistryChangeListener(listener);
		fListeners.remove(listener);
	}

}
