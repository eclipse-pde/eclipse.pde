/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
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
