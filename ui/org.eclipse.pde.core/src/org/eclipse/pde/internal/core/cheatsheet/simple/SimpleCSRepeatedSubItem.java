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

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCSRepeatedSubItem
 *
 */
public class SimpleCSRepeatedSubItem extends SimpleCSObject implements
		ISimpleCSRepeatedSubItem {

	/**
	 * Attribute:  values
	 */
	private String fValues;	
	
	/**
	 * Element:  subitem
	 */
	private ISimpleCSSubItem fSubItem;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSRepeatedSubItem(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#getSubItem()
	 */
	public ISimpleCSSubItem getSubItem() {
		return fSubItem;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#getValues()
	 */
	public String getValues() {
		return fValues;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#setSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void setSubItem(ISimpleCSSubItem subitem) {
		ISimpleCSObject old = fSubItem;
		fSubItem = subitem;

		if (isEditable()) {
			fireStructureChanged(subitem, old);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRepeatedSubItem#setValues(java.lang.String)
	 */
	public void setValues(String values) {
		String old = fValues;
		fValues = values;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_VALUES, old, fValues);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// Process values attribute
		fValues = PDETextHelper.translateReadText(element.getAttribute(ATTRIBUTE_VALUES));

		// Process subitem element
		NodeList children = element.getChildNodes();
		ISimpleCSModelFactory factory = getModel().getFactory();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				Element childElement = (Element)child;

				if (name.equals(ELEMENT_SUBITEM)) {
					fSubItem = factory.createSimpleCSSubItem(this);
					fSubItem.parse(childElement);
					// We are looking for only one subitem
					break;
				}
			}
		}			
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {

		StringBuffer buffer = new StringBuffer();
		String newIndent = indent + XMLPrintHandler.XML_INDENT;

		try {
			// Print repeated-subitem element
			buffer.append(ELEMENT_REPEATED_SUBITEM);
			// Print values attribute
			if ((fValues != null) && 
					(fValues.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_VALUES, PDETextHelper.translateWriteText(fValues)));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print subitem
			if (fSubItem != null) {
				fSubItem.write(newIndent, writer);
			}
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_REPEATED_SUBITEM, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 				
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fValues = null;
		fSubItem = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_REPEATED_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not supported in editor UI		
		return ELEMENT_REPEATED_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		// Add subitem
		if (fSubItem != null) {
			list.add(fSubItem);
		}
		return list;
	}

}
