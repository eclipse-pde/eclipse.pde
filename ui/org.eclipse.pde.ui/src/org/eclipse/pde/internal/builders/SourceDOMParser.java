/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.builders;

import org.apache.xerces.parsers.DOMParser;
import org.apache.xerces.utils.QName;
import org.apache.xerces.framework.XMLAttrList;
import org.w3c.dom.Node;
import java.util.Hashtable;
import org.xml.sax.Locator;
import org.xml.sax.SAXNotSupportedException;

/**
 * @version 	1.0
 * @author
 */
public class SourceDOMParser extends DOMParser {
	private Hashtable lines = new Hashtable();
	boolean notSupported;
	
	public SourceDOMParser() {
		try {
			setDeferNodeExpansion(false);
		}
		catch (Exception e) {
			notSupported = true;
		}
	}
	
	public void startElement(QName qname, XMLAttrList atts, int index) throws Exception {
		super.startElement(qname, atts, index);
		if (notSupported) return;
		Locator locator = getLocator();
		Integer lineValue = new Integer(locator.getLineNumber());
		Node elNode = getCurrentElementNode();
		if (elNode!=null)
			lines.put(elNode, lineValue);
	}
	
	public Hashtable getLineTable() { 
		return lines;
	}
}
