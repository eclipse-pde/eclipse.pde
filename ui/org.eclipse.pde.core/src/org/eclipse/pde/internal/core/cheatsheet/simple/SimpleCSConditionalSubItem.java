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
import java.util.Iterator;
import java.util.List;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCSConditionalSubItem
 *
 */
public class SimpleCSConditionalSubItem extends SimpleCSObject implements
		ISimpleCSConditionalSubItem {

	/**
	 * Attribute:  condition
	 */
	private String fCondition;

	/**
	 * Elements:  subitem
	 */
	private ArrayList fSubItems;	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSConditionalSubItem(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#getCondition()
	 */
	public String getCondition() {
		return fCondition;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#getSubItems()
	 */
	public ISimpleCSSubItem[] getSubItems() {
		return (ISimpleCSSubItem[]) fSubItems.toArray(
				new ISimpleCSSubItemObject[fSubItems.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#setCondition(java.lang.String)
	 */
	public void setCondition(String condition) {
		String old = fCondition;
		fCondition = condition;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_CONDITION, old, fCondition);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {

		// Process condition attribute
		// Read as is.  Do not translate
		fCondition = element.getAttribute(ATTRIBUTE_CONDITION);
		
		// Process children

		NodeList children = element.getChildNodes();
		ISimpleCSModelFactory factory = getModel().getFactory();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				Element childElement = (Element)child;

				if (name.equals(ELEMENT_SUBITEM)) {
					ISimpleCSSubItem subitem = factory.createSimpleCSSubItem(this);
					fSubItems.add(subitem);
					subitem.parse(childElement);
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
			// Print conditional-subitem element
			buffer.append(ELEMENT_CONDITIONAL_SUBITEM);
			// Print condition attribute
			// Write as is.  Do not translate
			if ((fCondition != null) && 
					(fCondition.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_CONDITION, fCondition));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print subitems
			Iterator iterator = fSubItems.iterator();
			while (iterator.hasNext()) {
				ISimpleCSSubItem subitem = (ISimpleCSSubItem)iterator.next();
				subitem.write(newIndent, writer);
			}
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_CONDITIONAL_SUBITEM, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 				
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fCondition = null;
		fSubItems = new ArrayList();		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_CONDITIONAL_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not supported in editor UI
		return ELEMENT_CONDITIONAL_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		// Add subitems
		if (fSubItems.size() > 0) {
			list.addAll(fSubItems);
		}
		return list;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#addSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void addSubItem(ISimpleCSSubItem subitem) {
		fSubItems.add(subitem);
		
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConditionalSubItem#removeSubItem(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem)
	 */
	public void removeSubItem(ISimpleCSSubItem subitem) {
		fSubItems.remove(subitem);
		
		if (isEditable()) {
			fireStructureChanged(subitem, IModelChangedEvent.REMOVE);
		}	
	}

}
