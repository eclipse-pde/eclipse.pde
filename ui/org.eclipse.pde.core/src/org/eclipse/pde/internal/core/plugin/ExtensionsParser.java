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
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		if ("eclipse".equals(target)) {
			fIsLegacy = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (fOpenElements == null) {
			if (qName.equals("plugin") || qName.equals("fragment")) {
				fOpenElements = new Stack();
			}
		} else if (fOpenElements.size() == 0) {
			if (qName.equals("extension")) {
				createExtension(attributes);
			} else if (qName.equals("extension-point")) {
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
		PluginExtension extension = new PluginExtension();
		if (extension.load(attributes, fLocator.getLineNumber())) {
			extension.setModel(fModel);
			extension.setInTheModel(true);
			fExtensions.add(extension);
			if (extension.getPoint().equals("org.eclipse.pde.core.source"))
				fOpenElements.push(extension);
		}
	}

	/**
	 * @param attributes
	 */
	private void createExtensionPoint(Attributes attributes) {
		PluginExtensionPoint extPoint = new PluginExtensionPoint();
		if (extPoint.load(attributes, fLocator.getLineNumber())) {
			extPoint.setModel(fModel);
			extPoint.setInTheModel(true);
			fExtensionPoints.add(extPoint);
		}
	}
	
	private void createElement(String tagName, Attributes attributes) {
		PluginElement element = new PluginElement();
		PluginParent parent = (PluginParent)fOpenElements.peek();
		element.setParent(parent);
		element.setInTheModel(true);
		element.setModel(fModel);
		element.load(tagName, attributes);
		parent.appendChild(element);
		fOpenElements.push(element);
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (fOpenElements != null && !fOpenElements.isEmpty())
			fOpenElements.pop();
	}

	/* (non-Javadoc)
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
