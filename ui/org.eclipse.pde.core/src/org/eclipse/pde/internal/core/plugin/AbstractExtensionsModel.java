/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;

public abstract class AbstractExtensionsModel
	extends AbstractModel
	implements IExtensionsModel, IExtensionsModelFactory {
	protected Extensions extensions;

	public AbstractExtensionsModel() {
		super();
	}
	
	public IExtensionsModelFactory getFactory() {
		return this;
	}
	
	protected Extensions createExtensions() {
		Extensions extensions = new Extensions();
		extensions.setModel(this);
		return extensions;
	}

	public IExtensions getExtensions() {
		return getExtensions(true);
	}
	public IExtensions getExtensions(boolean createIfMissing) {
		if (extensions == null && createIfMissing) {
			extensions = createExtensions();
			loaded = true;
		}
		return extensions;
	}

	public abstract URL getNLLookupLocation();

	protected URL[] getNLLookupLocations() {
		URL locations [] = { getNLLookupLocation() };
		return locations;
	}

	public synchronized void load(InputStream stream, boolean outOfSync)
		throws CoreException {

		if (extensions == null) {
			extensions = (Extensions) createExtensions();
			extensions.setModel(this);
		}
		extensions.reset();
		loaded = false;
		try {
			SAXParser parser = getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
			parser.parse(stream, handler);
			processDocument(handler.getDocument(), handler.getLineTable());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (Exception e) {
		}
	}
	private void processDocument(Document doc, Hashtable lineTable) {
		Node extensionsNode = doc.getDocumentElement();
		extensions.load(extensionsNode, lineTable);
	}

	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { extensions },
				null));
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
		if (!isLoaded()) return false;
		if (extensions==null) return false;
		return extensions.isValid();	
	}
	public boolean isReconcilingModel() {
		return false;
	}
}
