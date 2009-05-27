/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IExtensionsModel;
import org.eclipse.pde.core.plugin.IExtensionsModelFactory;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.AbstractNLModel;
import org.eclipse.pde.internal.core.PDEState;
import org.xml.sax.SAXException;

public abstract class AbstractExtensionsModel extends AbstractNLModel implements IExtensionsModel, IExtensionsModelFactory {

	private static final long serialVersionUID = 1L;
	protected Extensions fExtensions;

	public IExtensionsModelFactory getFactory() {
		return this;
	}

	protected Extensions createExtensions() {
		Extensions extensions = new Extensions(!isEditable());
		extensions.setModel(this);
		return extensions;
	}

	public IExtensions getExtensions() {
		return getExtensions(true);
	}

	public IExtensions getExtensions(boolean createIfMissing) {
		if (fExtensions == null && createIfMissing) {
			fExtensions = createExtensions();
			setLoaded(true);
		}
		return fExtensions;
	}

	public abstract URL getNLLookupLocation();

	protected URL[] getNLLookupLocations() {
		URL locations[] = {getNLLookupLocation()};
		return locations;
	}

	public synchronized void load(InputStream stream, boolean outOfSync) throws CoreException {

		if (fExtensions == null) {
			fExtensions = createExtensions();
			fExtensions.setModel(this);
		}
		fExtensions.reset();
		setLoaded(false);
		try {
			// TODO: possibly remove this work.
			// Need a good way to "setLoaded()" value
			// With the way we do it, we might be able to claim it is always loaded.
			SAXParser parser = getSaxParser();
			PluginHandler handler = new PluginHandler(true);
			parser.parse(stream, handler);
			fExtensions.load(handler.getSchemaVersion());
			setLoaded(true);
			if (!outOfSync)
				updateTimeStamp();
		} catch (ParserConfigurationException e) {
		} catch (SAXException e) {
		} catch (FactoryConfigurationError e) {
		} catch (IOException e) {
		}
	}

	// loaded from workspace when creating workspace model
	public void load(BundleDescription desc, PDEState state) {
		fExtensions = createExtensions();
		fExtensions.setModel(this);
		updateTimeStamp();
		setLoaded(true);
	}

	public void reload(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(new ModelChangedEvent(this, IModelChangedEvent.WORLD_CHANGED, new Object[] {fExtensions}, null));
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
		extension.setParent(getExtensions());
		extension.setModel(this);
		return extension;
	}

	public IPluginExtensionPoint createExtensionPoint() {
		PluginExtensionPoint extensionPoint = new PluginExtensionPoint();
		extensionPoint.setModel(this);
		extensionPoint.setParent(getExtensions());
		return extensionPoint;
	}

	public boolean isValid() {
		if (!isLoaded())
			return false;
		if (fExtensions == null)
			return false;
		return fExtensions.isValid();
	}
}
