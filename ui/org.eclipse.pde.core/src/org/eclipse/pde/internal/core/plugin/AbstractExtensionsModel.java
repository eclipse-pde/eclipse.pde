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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public abstract class AbstractExtensionsModel
	extends AbstractModel
	implements IExtensionsModel, IExtensionsModelFactory {
	protected Extensions extensions;
	private transient static SourceDOMParser parser;
	private transient static XMLErrorHandler errorHandler;
	//private DocumentModel documentModel;
	
	//static {
	//	if (!XMLCore.NEW_CODE_PATHS) {
	//		initializeParser();
	//	}
	//}

	public AbstractExtensionsModel() {
		super();
	}
	
	public IExtensionsModelFactory getFactory() {
		return this;
	}
	
//	public DocumentModel getDocumentModel() {
//		if (documentModel == null) {
//			documentModel= new  DocumentModel(this);
//		}
//		return documentModel;
//	}
	
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

	private static void initializeParser() {
		parser = new SourceDOMParser();
		errorHandler = new XMLErrorHandler();
		parser.setErrorHandler(errorHandler);
	}

	public synchronized void load(InputStream stream, boolean outOfSync)
		throws CoreException {
		//if (XMLCore.NEW_CODE_PATHS) {
		//	getDocumentModel().load(stream, outOfSync);
		//} else {
			loadOrig(stream, outOfSync);
		//}
	}
	
	private synchronized void loadOrig(InputStream stream, boolean outOfSync)
		throws CoreException {

		if (extensions == null) {
			extensions = (Extensions) createExtensions();
			extensions.setModel(this);
		}
		extensions.reset();
		loaded = false;
		try {
			InputSource source = new InputSource(stream);
			errorHandler.reset();
			parser.parse(source);
			//XMLErrorHandler errorHandler = new XMLErrorHandler();
			//SourceDOMParser parser = new SourceDOMParser();
			//parser.setErrorHandler(errorHandler);
			//parser.parse(source);
			if (errorHandler.getErrorCount() > 0
				|| errorHandler.getFatalErrorCount() > 0) {
				throwParseErrorsException();
			}
			processDocument(parser.getDocument(), parser.getLineTable());
			loaded = true;
			if (!outOfSync)
				updateTimeStamp();
		} catch (SAXException e) {
			throwParseErrorsException();
		} catch (IOException e) {
			throwParseErrorsException();
		}
	}
	private void processDocument(Document doc, Hashtable lineTable) {
		Node extensionsNode = doc.getDocumentElement();
		extensions.load(extensionsNode, lineTable);
	}
	public void reload(InputStream stream, boolean outOfSync)
		throws CoreException {
		//if (XMLCore.NEW_CODE_PATHS) {
		//	getDocumentModel().reload(stream, outOfSync);
		//} else {
			reloadOrig(stream, outOfSync);
		//}
	}
	private void reloadOrig(InputStream stream, boolean outOfSync)
		throws CoreException {
		load(stream, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[] { extensions },
				null));
	}
	public String toString() {
		return "extensions.xml";
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
}
