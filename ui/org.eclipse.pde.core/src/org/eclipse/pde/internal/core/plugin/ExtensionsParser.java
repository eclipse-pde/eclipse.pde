/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import java.util.*;

import org.eclipse.pde.core.plugin.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * @author melhem
 *  
 */
public class ExtensionsParser extends DefaultHandler {

	private Vector fExtensions;

	private Vector fExtensionPoints;

	private Stack fOpenElements;

	private Locator fLocator;

	private boolean fIsLegacy = true;

	private ISharedPluginModel fModel;

	/**
	 *  
	 */
	public ExtensionsParser(ISharedPluginModel model) {
		super();
		fExtensionPoints = new Vector();
		fExtensions = new Vector();
		fModel = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String,
	 *      java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		if ("eclipse".equals(target)) { //$NON-NLS-1$
			fIsLegacy = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (fOpenElements == null) {
			if (qName.equals("plugin") || qName.equals("fragment")) { //$NON-NLS-1$ //$NON-NLS-2$
				fOpenElements = new Stack();
			}
		} else if (fOpenElements.size() == 0) {
			if (qName.equals("extension")) { //$NON-NLS-1$
				createExtension(attributes);
			} else if (qName.equals("extension-point")) { //$NON-NLS-1$
				createExtensionPoint(attributes);
			}
		} else {
			createElement(qName, attributes);
		}
	}

	/**
	 * @param attributes
	 */
	private void createExtension(Attributes attributes) {
		PluginExtension extension = (PluginExtension) fModel.getFactory()
				.createExtension();
		extension.point = attributes.getValue("point"); //$NON-NLS-1$
		extension.id = attributes.getValue("id"); //$NON-NLS-1$
		extension.name = attributes.getValue("name"); //$NON-NLS-1$
		extension.range = new int[] { fLocator.getLineNumber(),
				fLocator.getLineNumber() };
		if (extension.isValid()) {
			extension.setInTheModel(true);
			fExtensions.add(extension);
			if ("org.eclipse.pde.core.source".equals(extension.point) || "org.eclipse.core.runtime.products".equals(extension.point)) //$NON-NLS-1$ //$NON-NLS-2$
				fOpenElements.push(extension);
		}
	}

	/**
	 * @param attributes
	 */
	private void createExtensionPoint(Attributes attributes) {
		PluginExtensionPoint extPoint = (PluginExtensionPoint) fModel
				.getFactory().createExtensionPoint();
		extPoint.id = attributes.getValue("id"); //$NON-NLS-1$
		extPoint.name = attributes.getValue("name"); //$NON-NLS-1$
		extPoint.schema = attributes.getValue("schema"); //$NON-NLS-1$
		extPoint.range = new int[] {fLocator.getLineNumber(), fLocator.getLineNumber()};
		if (extPoint.isValid()) {
			extPoint.setInTheModel(true);
			fExtensionPoints.add(extPoint);
		}
	}

	private void createElement(String tagName, Attributes attributes) {
		PluginElement element = new PluginElement();
		PluginParent parent = (PluginParent) fOpenElements.peek();
		element.setParent(parent);
		element.setInTheModel(true);
		element.setModel(fModel);
		element.load(tagName, attributes);
		parent.appendChild(element);
		fOpenElements.push(element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (fOpenElements != null && !fOpenElements.isEmpty())
			fOpenElements.pop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.helpers.DefaultHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		fLocator = locator;
	}

	public boolean isLegacy() {
		return fIsLegacy;
	}

	public Vector getExtensions() {
		return fExtensions;
	}

	public Vector getExtensionPoints() {
		return fExtensionPoints;
	}
}
