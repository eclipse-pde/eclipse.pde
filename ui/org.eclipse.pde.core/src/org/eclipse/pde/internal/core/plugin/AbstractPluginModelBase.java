/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginModelFactory;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;

public abstract class AbstractPluginModelBase
	extends AbstractModel
	implements IPluginModelBase, IPluginModelFactory {
	protected PluginBase fPluginBase;
	private boolean enabled;
	private BundleDescription fBundleDescription;
	
	public AbstractPluginModelBase() {
		super();
	}
	
	public abstract String getInstallLocation();
	
	public abstract IPluginBase createPluginBase();
	
	public IExtensions createExtensions() {
		return createPluginBase();
	}
	
	public IExtensionsModelFactory getFactory() {
		return this;
	}
	
	public IPluginModelFactory getPluginFactory() {
		return this;
	}

	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (fPluginBase == null && createIfMissing) {
			fPluginBase = (PluginBase) createPluginBase();
			setLoaded(true);
		}
		return fPluginBase;
	}
	
	public IExtensions getExtensions() {
		return getPluginBase();
	}
	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}
	public boolean isEnabled() {
		return enabled;
	}
	public boolean isFragmentModel() {
		return false;
	}

	public abstract URL getNLLookupLocation();

	protected URL[] getNLLookupLocations() {
		ArrayList list = new ArrayList();
		URL thisLocation = getNLLookupLocation();
		if (thisLocation != null)
			list.add(thisLocation);
		
		if (isFragmentModel()) {
			IFragment fragment = (IFragment)getPluginBase();
			String parentId = fragment.getPluginId();
			if (parentId != null) {
				IPlugin plugin = PDECore.getDefault().findPlugin(parentId);
				if (plugin != null) {
					try {
						list.add(new URL("file:" + plugin.getModel().getInstallLocation())); //$NON-NLS-1$
					} catch (MalformedURLException e) {
					}		
				}	
			}
		} else {
			if (fPluginBase != null) {
				String id = fPluginBase.getId();
				String version = fPluginBase.getVersion();
				addMatchingFragments(PDECore.getDefault().findFragmentsFor(id, version), list);
			}
		}		
		return (URL[])list.toArray(new URL[list.size()]);	
	}

	private void addMatchingFragments(IFragment[] fragments, List result) {
		for (int i = 0; i < fragments.length; i++) {
			IFragment fragment = fragments[i];
			URL location = ((IFragmentModel)fragment.getModel()).getNLLookupLocation();
			if (location == null) continue;
			IPluginLibrary libraries[] = fragment.getLibraries();
			for (int j = 0; j < libraries.length; j++) {
				try {
					result.add(new URL(location, libraries[j].getName()));
				} catch (MalformedURLException e) {
				}
			}
		}
	}
		
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { fPluginBase },
				null));
	}
	public void setEnabled(boolean newEnabled) {
		enabled = newEnabled;
	}
	public String toString() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase != null)
			return pluginBase.getTranslatedName();
		return super.toString();
	}

	protected abstract void updateTimeStamp();

	public IPluginAttribute createAttribute(IPluginElement element) {
		PluginAttribute attribute = new PluginAttribute();
		attribute.setModel(this);
		attribute.setParent(element);
		return attribute;
	}
	public IPluginElement createElement(IPluginObject parent) {
		PluginElement element = new PluginElement();
		element.setModel(this);
		element.setParent(parent);
		return element;
	}
	public IPluginExtension createExtension() {
		PluginExtension extension = new PluginExtension();
		extension.setParent(getPluginBase());
		extension.setModel(this);
		return extension;
	}
	public IPluginExtensionPoint createExtensionPoint() {
		PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
		extensionPoint.setModel(this);
		extensionPoint.setParent(getPluginBase());
		return extensionPoint;
	}
	public IPluginImport createImport() {
		PluginImport iimport = new PluginImport();
		iimport.setModel(this);
		iimport.setParent(getPluginBase());
		return iimport;
	}
	public IPluginLibrary createLibrary() {
		PluginLibrary library = new PluginLibrary();
		library.setModel(this);
		library.setParent(getPluginBase());
		return library;
	}
	
	public boolean isValid() {
		if (!isLoaded()) return false;
		if (fPluginBase==null) return false;
		return fPluginBase.isValid();	
	}

	public boolean isBundleModel() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#dispose()
	 */
	public void dispose() {
		fBundleDescription = null;
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#getBundleDescription()
	 */
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#setBundleDescription(org.eclipse.osgi.service.resolver.BundleDescription)
	 */
	public void setBundleDescription(BundleDescription description) {
		fBundleDescription = description;
	}

}
