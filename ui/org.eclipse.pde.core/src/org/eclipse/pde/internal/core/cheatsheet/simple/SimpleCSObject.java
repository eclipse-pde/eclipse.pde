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

import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * SimpleCSObject
 *
 */
public abstract class SimpleCSObject extends PlatformObject implements ISimpleCSObject {

	private transient ISimpleCSModel fModel;
	
	private transient ISimpleCSObject fParent;
	
	private static final long serialVersionUID = 1L;

	protected static final HashSet TAG_EXCEPTIONS = new HashSet(3);
	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("br/"); //$NON-NLS-1$
	}
	
	/**
	 * 
	 */
	public SimpleCSObject(ISimpleCSModel model, ISimpleCSObject parent) {
		fModel = model;
		fParent = parent;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getModel()
	 */
	public ISimpleCSModel getModel() {
		return fModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getSimpleCS()
	 */
	public ISimpleCS getSimpleCS() {
		return fModel.getSimpleCS();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#setModel(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel)
	 */
	public void setModel(ISimpleCSModel model) {
		fModel = model;
	}

	/**
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
		
	/**
	 * @param object
	 * @param property
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChanged(ISimpleCSObject object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
		
	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject child, int changeType) {
		fireStructureChanged(new ISimpleCSObject[] { child }, changeType);
	}
	
	/**
	 * @param newValue
	 * @param oldValue
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject newValue,
			ISimpleCSObject oldValue) {

		int changeType = -1;
		ISimpleCSObject object = null;
		if (newValue == null) {
			changeType = IModelChangedEvent.REMOVE;
			object = oldValue;
		} else {
			changeType = IModelChangedEvent.INSERT;
			object = newValue;
		}
		fireStructureChanged(object, changeType);
	}
	
	/**
	 * @param children
	 * @param changeType
	 */
	protected void fireStructureChanged(ISimpleCSObject[] children,
			int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider,
					changeType, children, null));
		}
	}
		
	/**
	 * @return
	 */
	protected boolean isEditable() {
		return getModel().isEditable();
	}
		
	/**
	 * @param source
	 * @return
	 */
	public String getWritableString(String source) {
		// TODO: MP: Probably don't need this anymore since using xmlprinthandler
		return CoreUtility.getWritableString(source);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getParent()
	 */
	public ISimpleCSObject getParent() {
		return fParent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public abstract int getType();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getName()
	 */
	public abstract String getName();
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getChildren()
	 */
	public abstract List getChildren();
	
	/**
	 * Recursively finds and acculates all element's text and element 
	 * children into a String in raw XML form 
	 * @param element
	 * @return 
	 */
	protected String parseElementText(Element element) {
		// Puts all Text nodes in the full depth of the sub-tree 
		// underneath this Node
		// i.e., there are neither adjacent Text nodes nor empty Text nodes. 
		element.normalize();
		// Process only if there are children
		if (element.getChildNodes().getLength() > 0) {
			NodeList children = element.getChildNodes();
			StringBuffer buffer = new StringBuffer();
			// Traverse over each childe
			for (int i = 0; i < children.getLength(); i++) {
				Node node = children.item(i);
				if (node.getNodeType() == Node.TEXT_NODE) {
					// Accumulate the text children
					buffer.append(((Text)node).getData());
				} else if (node.getNodeType() == Node.ELEMENT_NODE) {
					Element subElement = (Element)node;
					// Recursively accumulate element children
					String value = parseElementText(subElement);
					if (value.length() > 0) {
						// The element had children
						// Enclose the accumulated children with start and end tags
						buffer.append('<' + subElement.getNodeName() + '>');
						buffer.append(value);
						buffer.append("</" + subElement.getNodeName() + '>'); //$NON-NLS-1$
					} else {
						// The element had no children
						// Generate an abbreviated element tag
						buffer.append('<' + subElement.getNodeName() + "/>"); //$NON-NLS-1$
					}
				}
			}
			// Return all accumulated children under the input element as a raw
			// XML string
			return buffer.toString();
		}
		return ""; //$NON-NLS-1$
	}
	
}
