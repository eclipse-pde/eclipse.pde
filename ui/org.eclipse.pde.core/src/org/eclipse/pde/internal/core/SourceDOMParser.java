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
package org.eclipse.pde.internal.core;

import java.util.Hashtable;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.xni.*;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @version 	1.0
 * @author
 */
public class SourceDOMParser extends DOMParser {
	private Hashtable lines = new Hashtable();
	boolean notSupported;
	private XMLLocator locator;

	public SourceDOMParser() {
		try {
			setFeature(DEFER_NODE_EXPANSION, false);
		} catch (Exception e) {
			notSupported = true;
		}
	}
	
	public void reset() {
		super.reset();
		lines.clear();
	}

	public void startDocument(
		XMLLocator locator,
		String encoding,
		Augmentations augs)
		throws XNIException {
		super.startDocument(locator, encoding, augs);
		this.locator = locator;
	}

	public void startElement(
		QName element,
		XMLAttributes atts,
		Augmentations augs)
		throws XNIException {
		super.startElement(element, atts, augs);
		if (notSupported)
			return;
		try {
			Integer [] range = new Integer[2];
			range[0] = new Integer(locator.getLineNumber());
			Node elNode = (Node) getProperty(CURRENT_ELEMENT_NODE);
			if (elNode != null)
				lines.put(elNode, range);
		} catch (SAXException e) {
		}
	}

	public void endElement(QName element, Augmentations augs)
		throws XNIException {
		if (notSupported)
			return;
		try {
			Node elNode = (Node) getProperty(CURRENT_ELEMENT_NODE);
			if (elNode != null) {
				Integer[] range = (Integer[]) lines.get(elNode);
				if (range != null) {
					Integer endValue = new Integer(locator.getLineNumber());
					range[1] = endValue;
				}
			}
		} catch (SAXException e) {
		}
		super.endElement(element, augs);
	}

	public Hashtable getLineTable() {
		return lines;
	}
}
