/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.toc;

import java.io.IOException;
import java.io.PrintWriter;
//import java.util.ArrayList;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.text.DocumentElementNode;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;

/**
 * Toc - represents the root element of a Table of Contents
 * The TOC root element, like TOC topics, can hold many child topics,
 * links and anchors. Aside from being the root element of the TOC,
 * the element differs from regular topics by having an optional
 * anchor attribute that determines which anchors this TOC will plug
 * into.
 */
public class Toc extends TocTopic {

	private static final long serialVersionUID = 1L;

	//The anchor field allows this TOC to be included in a specified
	//point of a TOC. An anchor link is written as:
	//{PATH TO TOC WITH ANCHOR}#{ANCHOR ID}
	//For example: ../com.example.abc/toc.xml#someID 
	private String fFieldAnchorTo;

	/**
	 * Constructs a new Toc. Only takes a model,
	 * since the root element cannot have a parent.
	 * 
	 * @param model The model associated with this TOC.
	 */
	public Toc(TocModel model) {
		super(model, null);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_TOC;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_TOC;
	}

	/**
	 * Parses the same attributes as topic elements, but also
	 * parses the anchor field, which is unique to the TOC root element.
	 * 
	 * @param element The XML element to parse
	 * 
	 * @see org.eclipse.pde.internal.core.toc.TocTopic#parseAttributes(org.w3c.dom.Element)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseAttributes(org.w3c.dom.Element)
	 */
	protected void parseAttributes(Element element) {
		super.parseAttributes(element);
		parseFieldAnchor(element);
	}
	
	/**
	 * Process the anchor attribute for TOC root elements.
	 * 
	 * @param element The XML element to parse
	 */
	protected void parseFieldAnchor(Element element) {
		// Process the anchor attribute ('link_to' attribute)
		// Trim leading and trailing whitespace
		fFieldAnchorTo = element.getAttribute(ATTRIBUTE_LINK_TO).trim();
	}	
	
	/**
	 * Override the topic behaviour for parsing the page link field,
	 * because the TOC root element uses a different attribute name
	 * ('topic' instead of 'href')
	 * 
	 * @param element The XML element to parse
	 * @see org.eclipse.pde.internal.core.toc.TocTopic#parseFieldLink(org.w3c.dom.Element)
	 */
	protected void parseFieldLink(Element element) {
		// Process link attribute ('topic' for toc elements)
		// Trim leading and trailing whitespace
		fFieldRef = element.getAttribute(ATTRIBUTE_TOPIC).trim();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#parseElement(org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		super.parseElement(element);
		// TODO: Add Enablement support
		/*String name = element.getNodeName();
		TocModelFactory factory = getModel().getFactory();
		
		if (name.equals(ELEMENT_ENABLEMENT)) {
			// Process topic element
			TocEnablement enablement = factory.createTocEnablement(this);
			fFieldEnablements.add(enablement);
			enablement.parse(element);
		}*/
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#reset()
	 */
	public void reset() {
		super.reset();
		
		fFieldAnchorTo = null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#write()
	 */
	public void write(String indent, PrintWriter writer) {
		//This is the first element that is written to file, since
		//this element is the root
		try {
			// Print XML declaration
			XMLPrintHandler.printHead(writer, DocumentElementNode.ATTRIBUTE_VALUE_ENCODING);
			super.write(indent, writer);
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 			
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#writeAttributes(java.lang.StringBuffer)
	 */
	protected void writeAttributes(StringBuffer buffer) {
		super.writeAttributes(buffer); 
		
		printAnchorAttribute(buffer);
	}

	/**
	 * Override the topic behaviour for writing out the page link field,
	 * because the TOC root element uses a different attribute name
	 * ('topic' instead of 'href')
	 * 
	 * @param buffer The buffer to write the attribute to
	 * @see org.eclipse.pde.internal.core.toc.TocTopic#printLinkAttribute(org.w3c.dom.Element)
	 */
	protected void printLinkAttribute(StringBuffer buffer)
	{	if ((fFieldRef != null) && 
				(fFieldRef.length() > 0)) {
			// Trim leading and trailing whitespace
			// Encode characters
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_TOPIC, 
					PDETextHelper.translateWriteText(
							fFieldRef.trim(), DEFAULT_SUBSTITUTE_CHARS)));
		}
	}
	
	/**
	 * Print the anchor attribute out to the buffer.
	 * 
	 * @param buffer
	 */
	private void printAnchorAttribute(StringBuffer buffer) {
		if ((fFieldAnchorTo != null) && 
				(fFieldAnchorTo.length() > 0)) {
			// No trim required
			// No encode required
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_LABEL, fFieldAnchorTo));
		}
	}

	/**
	 * @return the anchor path associated with this TOC
	 */
	public String getFieldAnchorTo() {
		return fFieldAnchorTo;
	}

	/**
	 * Change the value of the anchor field and 
	 * signal a model change if needed.
	 * 
	 * @param The new anchor path to associate with this TOC
	 */
	public void setFieldAnchorTo(String name) {
		String old = fFieldAnchorTo;
		fFieldAnchorTo = name;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_LINK_TO, old, fFieldAnchorTo);
		}
	}


}
