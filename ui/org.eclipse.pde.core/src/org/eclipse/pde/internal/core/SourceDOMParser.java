/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.core;

import java.util.Hashtable;
import org.apache.xerces.parsers.DOMParser;

/**
 * @version 	1.0
 * @author
 */
public class SourceDOMParser extends DOMParser {
	private Hashtable lines = new Hashtable();
	boolean notSupported;
	
	public SourceDOMParser() {
		try {
			//setDeferNodeExpansion(false);
		}
		catch (Exception e) {
			notSupported = true;
		}
	}
/*
 * This code does not work in XML4J 4.x.x
 * Must find another way to map line numbers and model objects.
 * 
	public void startElement(QName qname, XMLAttrList atts, int index) throws Exception {
		super.startElement(qname, atts, index);
		if (notSupported) return;
		Locator locator = getLocator();
		Integer lineValue = new Integer(locator.getLineNumber());
		Node elNode = getCurrentElementNode();
		if (elNode!=null)
			lines.put(elNode, lineValue);
	}
*/
	public Hashtable getLineTable() { 
		return lines;
	}
}
