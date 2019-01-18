/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.plugin;

import java.io.InputStream;
import javax.xml.parsers.SAXParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
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
import org.eclipse.pde.internal.core.AbstractNLModel;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;

public abstract class AbstractPluginModelBase extends AbstractNLModel implements IPluginModelBase, IPluginModelFactory {

	private static final long serialVersionUID = 1L;
	protected IPluginBase fPluginBase;
	private boolean enabled;
	private BundleDescription fBundleDescription;
	protected boolean fAbbreviated;

	public AbstractPluginModelBase() {
		super();
	}

	@Override
	public abstract String getInstallLocation();

	@Override
	public abstract IPluginBase createPluginBase();

	public IExtensions createExtensions() {
		return createPluginBase();
	}

	@Override
	public IExtensionsModelFactory getFactory() {
		return this;
	}

	@Override
	public IPluginModelFactory getPluginFactory() {
		return this;
	}

	@Override
	public IPluginBase getPluginBase() {
		return getPluginBase(true);
	}

	@Override
	public IPluginBase getPluginBase(boolean createIfMissing) {
		if (fPluginBase == null && createIfMissing) {
			fPluginBase = createPluginBase();
			setLoaded(true);
		}
		return fPluginBase;
	}

	@Override
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync, new PluginHandler(fAbbreviated));
	}

	public void load(InputStream stream, boolean outOfSync, PluginHandler handler) {
		if (fPluginBase == null) {
			fPluginBase = createPluginBase();
		}

		((PluginBase) fPluginBase).reset();
		setLoaded(false);
		try {
			SAXParser parser = getSaxParser();
			parser.parse(stream, handler);
			((PluginBase) fPluginBase).load(handler.getDocumentElement(), handler.getSchemaVersion());
			setLoaded(true);
			if (!outOfSync) {
				updateTimeStamp();
			}
		} catch (Exception e) {
			PDECore.log(e);
		}
	}

	public void load(BundleDescription description, PDEState state) {
		setBundleDescription(description);
		IPluginBase base = getPluginBase();
		if (base instanceof Plugin) {
			((Plugin) base).load(description, state);
		} else {
			((Fragment) base).load(description, state);
		}
		updateTimeStamp();
		setLoaded(true);
	}

	@Override
	public IExtensions getExtensions() {
		return getPluginBase();
	}

	@Override
	public IExtensions getExtensions(boolean createIfMissing) {
		return getPluginBase(createIfMissing);
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public boolean isFragmentModel() {
		return false;
	}

	@Override
	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {fPluginBase}, null));
	}

	@Override
	public void setEnabled(boolean newEnabled) {
		enabled = newEnabled;
	}

	@Override
	public String toString() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase != null) {
			return pluginBase.getId();
		}
		return super.toString();
	}

	@Override
	protected abstract void updateTimeStamp();

	@Override
	public IPluginAttribute createAttribute(IPluginElement element) {
		PluginAttribute attribute = new PluginAttribute();
		attribute.setModel(this);
		attribute.setParent(element);
		return attribute;
	}

	@Override
	public IPluginElement createElement(IPluginObject parent) {
		PluginElement element = new PluginElement();
		element.setModel(this);
		element.setParent(parent);
		return element;
	}

	@Override
	public IPluginExtension createExtension() {
		PluginExtension extension = new PluginExtension();
		extension.setParent(getPluginBase());
		extension.setModel(this);
		return extension;
	}

	@Override
	public IPluginExtensionPoint createExtensionPoint() {
		PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
		extensionPoint.setModel(this);
		extensionPoint.setParent(getPluginBase());
		return extensionPoint;
	}

	@Override
	public IPluginImport createImport() {
		PluginImport iimport = new PluginImport();
		iimport.setModel(this);
		iimport.setParent(getPluginBase());
		return iimport;
	}

	public IPluginImport createImport(String pluginId) {
		PluginImport iimport = new PluginImport(this, pluginId);
		iimport.setParent(getPluginBase());
		return iimport;
	}

	@Override
	public IPluginLibrary createLibrary() {
		PluginLibrary library = new PluginLibrary();
		library.setModel(this);
		library.setParent(getPluginBase());
		return library;
	}

	@Override
	public boolean isValid() {
		if (!isLoaded()) {
			return false;
		}
		if (fPluginBase == null) {
			return false;
		}
		return fPluginBase.isValid();
	}

	public boolean isBundleModel() {
		return false;
	}

	@Override
	public void dispose() {
		fBundleDescription = null;
		super.dispose();
	}

	@Override
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}

	@Override
	public void setBundleDescription(BundleDescription description) {
		fBundleDescription = description;
	}

}
