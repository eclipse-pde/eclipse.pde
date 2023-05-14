/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.spi.IDynamicExtensionRegistry;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.runtime.spi.RegistryStrategy;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.util.tracker.ServiceTracker;

public class PDERegistryStrategy extends RegistryStrategy {

	/**
	 * Tracker for the XML parser service
	 */
	private ServiceTracker<SAXParserFactory, SAXParserFactory> xmlTracker = null;

	private Object fKey = null;

	private ModelListener fModelListener = null;
	private ExtensionListener fExtensionListener = null;
	private PDEExtensionRegistry fPDERegistry = null;

	class RegistryListener {
		IExtensionRegistry fRegistry;

		protected final void removeModels(IPluginModelBase[] bases, boolean onlyInactive) {
			for (IPluginModelBase base : bases) {
				//				resetModel(bases[i]);
				if (onlyInactive && base.isEnabled()) {
					continue;
				}
				removeBundle(fRegistry, base);
			}
		}

		public void setRegistry(IExtensionRegistry registry) {
			fRegistry = registry;
		}
	}

	class ModelListener extends RegistryListener implements IPluginModelListener {

		@Override
		public void modelsChanged(PluginModelDelta delta) {
			if (fRegistry == null) {
				createRegistry();
			}
			// can ignore removed models since the ModelEntries is empty
			ModelEntry[] entries = delta.getChangedEntries();
			for (int i = 0; i < entries.length; i++) {
				// If we have workspace models, we need to make sure they are registered before external models so when we search for extension points,
				// we find the workspace version
				IPluginModelBase[] workspaceModels = entries[i].getWorkspaceModels();
				if (workspaceModels.length > 0) {
					removeModels(entries[i].getExternalModels(), !entries[i].hasWorkspaceModels());
					removeModels(workspaceModels, true);
					addBundles(fRegistry, entries[i].getWorkspaceModels());
				}
				// make sure the external models are registered at all times
				addBundles(fRegistry, entries[i].getExternalModels());
			}
			entries = delta.getAddedEntries();
			ModelEntry[] removedEntries = delta.getRemovedEntries();
			if (removedEntries.length == entries.length && fRegistry instanceof IDynamicExtensionRegistry) {
				for (ModelEntry entry : removedEntries) {
					if (entry.getId() != null) {
						IDynamicExtensionRegistry registry = (IDynamicExtensionRegistry) fRegistry;
						IContributor[] contributors = registry.getAllContributors();
						for (IContributor contributor : contributors) {
							if (entry.getId().equals(contributor.getName())) {
								registry.removeContributor(contributor, fKey);
								break;
							}
						}
					}
				}
			}
			for (ModelEntry entry : entries) {
				addBundles(fRegistry, entry.getActiveModels());
			}
		}

	}

	class ExtensionListener extends RegistryListener implements IExtensionDeltaListener {

		@Override
		public void extensionsChanged(IExtensionDeltaEvent event) {
			if (fRegistry == null) {
				createRegistry();
			}
			IPluginModelBase[] bases = event.getRemovedModels();
			removeModels(bases, false);
			removeModels(event.getChangedModels(), false);
			addBundles(fRegistry, event.getChangedModels());
			addBundles(fRegistry, event.getAddedModels());
			// if we remove the last workspace model for a Bundle-SymbolicName, then refresh the external models by removing then adding them
			for (IPluginModelBase base : bases) {
				ModelEntry entry = PluginRegistry.findEntry(base.getPluginBase().getId());
				if (entry != null && entry.getWorkspaceModels().length == 0) {
					IPluginModelBase[] externalModels = entry.getExternalModels();
					removeModels(externalModels, false);
					addBundles(fRegistry, externalModels);
				}
			}
		}

	}

	public PDERegistryStrategy(File[] storageDirs, boolean[] cacheReadOnly, Object key, PDEExtensionRegistry registry) {
		super(storageDirs, cacheReadOnly);
		init();
		fKey = key;
		fPDERegistry = registry;
	}

	protected void init() {
		connectListeners();
	}

	@Override
	public void log(IStatus status) {
		// Because we are at development time, we create markers for registry problems and therefore do not log anything (bug 330648)
	}

	protected void connectListeners() {
		// Listen for model changes to register new bundles and unregister removed bundles
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.addPluginModelListener(fModelListener = new ModelListener());
		manager.addExtensionDeltaListener(fExtensionListener = new ExtensionListener());
	}

	protected void setListenerRegistry(IExtensionRegistry registry) {
		if (fModelListener != null) {
			fModelListener.setRegistry(registry);
		}
		if (fExtensionListener != null) {
			fExtensionListener.setRegistry(registry);
		}
	}

	@Override
	public void onStart(IExtensionRegistry registry, boolean loadedFromCache) {
		super.onStart(registry, loadedFromCache);
		setListenerRegistry(registry);
		if (!loadedFromCache) {
			processBundles(registry);
		}
	}

	@Override
	public void onStop(IExtensionRegistry registry) {
		super.onStop(registry);
		setListenerRegistry(null);
	}

	@Override
	public SAXParserFactory getXMLParser() {
		if (xmlTracker == null) {
			xmlTracker = new ServiceTracker<>(PDECore.getDefault().getBundleContext(), SAXParserFactory.class, null);
			xmlTracker.open();
		}
		return xmlTracker.getService();
	}

	private void processBundles(IExtensionRegistry registry) {
		addBundles(registry, fPDERegistry.getModels());
	}

	private void addBundles(IExtensionRegistry registry, IPluginModelBase[] bases) {
		for (IPluginModelBase base : bases) {
			addBundle(registry, base);
		}
	}

	private void addBundle(IExtensionRegistry registry, IPluginModelBase base) {
		IContributor contributor = createContributor(base);
		if (contributor == null) {
			return;
		}
		if (((IDynamicExtensionRegistry) registry).hasContributor(contributor)) {
			return;
		}

		File input = getFile(base);
		if (input == null) {
			return;
		}
		InputStream is = null;
		ZipFile jfile = null;

		try {
			if (new File(base.getInstallLocation()).isDirectory()) {
				// Directory bundle, access the extensions file directly
				is = new FileInputStream(input);
			} else {
				// Archived bundle, need to extract the file
				jfile = new ZipFile(input, ZipFile.OPEN_READ);
				String fileName = (base.isFragmentModel()) ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR
						: ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
				ZipEntry entry = jfile.getEntry(fileName);
				if (entry != null) {
					is = jfile.getInputStream(entry);
				}
			}
			if (is != null) {
				registry.addContribution(new BufferedInputStream(is), contributor, true, input.getPath(), null, fKey);
			}
		} catch (IOException e) {
		} finally {
			if (jfile != null) {
				try {
					jfile.close();
				} catch (IOException e) {
				}
			}
		}
	}

	private void removeBundle(IExtensionRegistry registry, IPluginModelBase base) {
		if (registry instanceof IDynamicExtensionRegistry) {
			IContributor contributor = createContributor(base);
			if (contributor != null && ((IDynamicExtensionRegistry) registry).hasContributor(contributor)) {
				((IDynamicExtensionRegistry) registry).removeContributor(createContributor(base), fKey);
			}
		}
	}

	//	added for releasing cached information from IPluginModelBase
	//	private void resetModel(IPluginModelBase model) {
	//		IPluginBase base = model.getPluginBase();
	//		if (base instanceof BundlePluginBase) {
	//			IExtensions ext = ((BundlePluginBase)base).getExtensionsRoot();
	//			if (ext != null && ext instanceof AbstractExtensions) {
	//				((AbstractExtensions)ext).reset();
	//			}
	//		} else if (base instanceof AbstractExtensions){
	//			((AbstractExtensions)base).resetExtensions();
	//		}
	//	}

	private File getFile(IPluginModelBase base) {
		String loc = base.getInstallLocation();
		if (loc == null) {
			return null;
		}
		File file = new File(loc);
		if (!file.exists()) {
			return null;
		}
		if (file.isFile()) {
			return file;
		}
		String fileName = (base.isFragmentModel()) ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR : ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR;
		File inputFile = new File(file, fileName);
		return (inputFile.exists()) ? inputFile : null;
	}

	public IContributor createContributor(IPluginModelBase base) {
		BundleDescription desc = base == null ? null : base.getBundleDescription();
		// return null if the IPluginModelBase does not have a BundleDescription (since then we won't have a valid 'id')
		if (desc == null) {
			return null;
		}
		String name = desc.getSymbolicName();
		String id = Long.toString(desc.getBundleId());
		String hostName = null;
		String hostId = null;

		HostSpecification host = desc.getHost();
		// make sure model is a singleton.  If it is a fragment, make sure host is singleton
		if (host != null && host.getBundle() != null && !host.getBundle().isSingleton() || host == null && !desc.isSingleton()) {
			return null;
		}
		if (host != null) {
			BundleDescription[] hosts = host.getHosts();
			if (hosts.length != 1) {
				return null;
			}
			BundleDescription hostDesc = hosts[0];
			hostName = hostDesc.getSymbolicName();
			hostId = Long.toString(hostDesc.getBundleId());
		}
		return new RegistryContributor(id, name, hostId, hostName);
	}

	public void dispose() {
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		manager.removePluginModelListener(fModelListener);
		manager.removeExtensionDeltaListener(fExtensionListener);
		if (xmlTracker != null) {
			xmlTracker.close();
			xmlTracker = null;
		}
	}

	private void createRegistry() {
		fPDERegistry.getRegistry();
	}

	// Same timestamp calculations as PDEState.computeTimestamp(URL[] urls, long timestamp)
	@Override
	public long getContributionsTimestamp() {
		IPluginModelBase[] bases = fPDERegistry.getModels();
		long timeStamp = 0;
		for (IPluginModelBase base : bases) {
			String loc = base.getInstallLocation();
			if (loc == null) {
				continue;
			}

			File location = new File(loc);
			if (location.exists()) {
				if (location.isFile()) {
					timeStamp ^= location.lastModified();
				} else {
					File manifest = new File(location, ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
					if (manifest.exists()) {
						timeStamp ^= manifest.lastModified();
					}
					manifest = new File(location, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
					if (manifest.exists()) {
						timeStamp ^= manifest.lastModified();
					}
					manifest = new File(location, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
					if (manifest.exists()) {
						timeStamp ^= manifest.lastModified();
					}
				}
				timeStamp ^= location.getAbsolutePath().hashCode();
			}
		}
		return timeStamp;
	}

}
