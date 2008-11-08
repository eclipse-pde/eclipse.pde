/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
import javax.xml.parsers.SAXParser;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;

public abstract class AbstractPluginModelBase extends AbstractNLModel implements IPluginModelBase, IPluginModelFactory {

	private static final long serialVersionUID = 1L;
	protected IPluginBase fPluginBase;
	private boolean enabled;
	private BundleDescription fBundleDescription;
	protected boolean fAbbreviated;

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
			fPluginBase = createPluginBase();
			setLoaded(true);
		}
		return fPluginBase;
	}

	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync, new PluginHandler(fAbbreviated));
	}

	public void load(InputStream stream, boolean outOfSync, PluginHandler handler) {
		if (fPluginBase == null)
			fPluginBase = createPluginBase();

		((PluginBase) fPluginBase).reset();
		setLoaded(false);
		try {
			SAXParser parser = getSaxParser();
			parser.parse(stream, handler);
			((PluginBase) fPluginBase).load(handler.getDocumentElement(), handler.getSchemaVersion());
			setLoaded(true);
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
			PDECore.log(e);
		}
	}

	public void load(BundleDescription description, PDEState state) {
		setBundleDescription(description);
		IPluginBase base = getPluginBase();
		if (base instanceof Plugin)
			((Plugin) base).load(description, state);
		else
			((Fragment) base).load(description, state);
		updateTimeStamp();
		setLoaded(true);
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

	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {fPluginBase}, null));
	}

	public void setEnabled(boolean newEnabled) {
		enabled = newEnabled;
	}

	public String toString() {
		IPluginBase pluginBase = getPluginBase();
		if (pluginBase != null)
			return pluginBase.getId();
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

	public IPluginImport createImport(String pluginId) {
		PluginImport iimport = new PluginImport(this, pluginId);
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
		if (!isLoaded())
			return false;
		if (fPluginBase == null)
			return false;
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
