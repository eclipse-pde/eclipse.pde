/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.cheatsheet.simple;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCSIntro
 *
 */
public class SimpleCSIntro extends SimpleCSObject implements ISimpleCSIntro {

	/**
	 * Element:  description
	 */
	private ISimpleCSDescription fDescription;	
	
	/**
	 * Attribute:  contextId
	 */
	private String fContextId;
	
	/**
	 * Attribute:  href
	 */
	private String fHref;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSIntro(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getContextId()
	 */
	public String getContextId() {
		return fContextId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getDescription()
	 */
	public ISimpleCSDescription getDescription() {
		return fDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getHref()
	 */
	public String getHref() {
		return fHref;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		String old = fContextId;
		fContextId = contextId;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_CONTEXTID, old, fContextId);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setDescription(java.lang.String)
	 */
	public void setDescription(ISimpleCSDescription description) {
		ISimpleCSObject old = fDescription;		
		fDescription = description;

		if (isEditable()) {
			fireStructureChanged(description, old);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setHref(java.lang.String)
	 */
	public void setHref(String href) {
		String old = fHref;
		fHref = href;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_HREF, old, fHref);
		}		
	}

	/**
	 * @param node
	 */
	public void parse(Element element) {
		// Process contextId attribute
		fContextId = element.getAttribute(ATTRIBUTE_CONTEXTID).trim();
		// Process href attribute
		fHref = element.getAttribute(ATTRIBUTE_HREF).trim();
		// Process children
		NodeList children = element.getChildNodes();
		ISimpleCSModelFactory factory = getModel().getFactory();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element)child;
				String name = child.getNodeName();
				if (name.equals(ELEMENT_DESCRIPTION)) {
					fDescription = factory.createSimpleCSDescription(this);
					fDescription.parse(childElement);
				}
			}
		}
	}

	public void write(String indent, PrintWriter writer) {

		StringBuffer buffer = new StringBuffer();
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		
		try {
			// Print intro element
			buffer.append(ELEMENT_INTRO); //$NON-NLS-1$
			// Print contextId attribute
			// Print href attribute
			if ((fContextId != null) &&
					(fContextId.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_CONTEXTID, 
						PDETextHelper.translateWriteText(
								fContextId.trim(), SUBSTITUTE_CHARS)));
			} else if ((fHref != null) &&
							(fHref.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_HREF, 
						PDETextHelper.translateWriteText(
								fHref.trim(), SUBSTITUTE_CHARS)));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print description element
			if (fDescription != null) {
				fDescription.write(newIndent, writer);
			}
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_INTRO, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#reset()
	 */
	public void reset() {
		fDescription = null;
		fContextId = null;
		fHref = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_INTRO;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		return PDECoreMessages.SimpleCSIntro_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

}
