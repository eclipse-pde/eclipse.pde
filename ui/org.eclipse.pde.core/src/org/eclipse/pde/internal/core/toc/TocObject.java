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
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * TocObject - All objects modeled in a Table of Contents subclass TocObject
 * This class contains functionality common to all TOC elements.
 */
public abstract class TocObject extends PlatformObject implements ITocConstants, Serializable, IWritable {

	//The model associated with this TocObject
	private transient TocModel fModel;
	
	//The TocObject's parent TocObject (can be null for root objects)
	private transient TocObject fParent;	

	private boolean hasEnablement;
	
	private TocEnablement fFieldEnablement;
	
	//Default XHTML elements that may be found in XML Text elements
	protected static final HashSet DEFAULT_TAG_EXCEPTIONS = new HashSet(12);
	
	//Characters that need to be substituted with escapes
	protected static final HashMap DEFAULT_SUBSTITUTE_CHARS = new HashMap(5);
	
	static {
		DEFAULT_TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("br/"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("p"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("/p"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("li"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("/li"); //$NON-NLS-1$		
		DEFAULT_TAG_EXCEPTIONS.add("a"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("/a"); //$NON-NLS-1$	
		DEFAULT_TAG_EXCEPTIONS.add("span"); //$NON-NLS-1$
		DEFAULT_TAG_EXCEPTIONS.add("/span"); //$NON-NLS-1$			
		DEFAULT_TAG_EXCEPTIONS.add("img"); //$NON-NLS-1$	
		
		DEFAULT_SUBSTITUTE_CHARS.put(new Character('&'), "&amp;"); //$NON-NLS-1$
		DEFAULT_SUBSTITUTE_CHARS.put(new Character('<'), "&lt;"); //$NON-NLS-1$
		DEFAULT_SUBSTITUTE_CHARS.put(new Character('>'), "&gt;"); //$NON-NLS-1$
		DEFAULT_SUBSTITUTE_CHARS.put(new Character('\''), "&apos;"); //$NON-NLS-1$
		DEFAULT_SUBSTITUTE_CHARS.put(new Character('\"'), "&quot;"); //$NON-NLS-1$
	}	
	
	/**
	 * Constructs the TocObject and initializes its attributes.
	 * 
	 * @param model The model associated with this TocObject.
	 * @param parent The parent of this TocObject.
	 */
	public TocObject(TocModel model, TocObject parent) {
		fModel = model;
		fParent = parent;
		hasEnablement = false;
		reset();
	}

	public void reconnect(TocModel model, TocObject parent) {
		fModel = model;
		fParent = parent;
		List children = getChildren();
		
		for(Iterator iter = children.iterator(); iter.hasNext();)
		{	TocObject child = (TocObject)iter.next();
			child.reconnect(model, this);
		}
	}

	/**
	 * @return the children of the object or an empty List if none exist.
	 */
	public abstract List getChildren();

	/**
	 * @return true iff this TOC object is capable of containing children.
	 */
	public abstract boolean canBeParent();
	
	/**
	 * @return the root TOC element that is an ancestor to this TocObject.
	 */
	public Toc getToc() {
		return fModel.getToc();
	}

	/**
	 * @return the model associated with this TocObject.
	 */
	public TocModel getModel() {
		return fModel;
	}

	/**
	 * @return the identifier for this TocObject.
	 */
	public abstract String getName();

	/**
	 * @return the path to the resource associated with this TOC object
	 * or <code>null</code> if one does not exist.
	 */
	public abstract String getPath();

	/**
	 * @return the parent of this TocObject, or <br />
	 * <code>null</code> if the TocObject has no parent.
	 */
	public TocObject getParent() {
		return fParent;
	}

	/**
	 * Change the parent of this TocObject. Usually
	 * used when the object is being moved from one
	 * part of the TOC to another
	 * 
	 * @param newParent the new parent of this TocObject
	 */
	void setParent(TocObject newParent) {
		fParent = newParent;
	}

	/**
	 * Check if the object is a direct or indirect descendant
	 * of the object parameter.
	 * 
	 * @param obj The TOC object to find in this object's ancestry
	 * @return true iff obj is an ancestor of this TOC object
	 */
	public boolean descendsFrom(TocObject obj)
	{	if(this.equals(obj))
		{	return true;
		}

		if(fParent != null && obj.canBeParent())
		{	return fParent.descendsFrom(obj);	
		}

		return false;
	}

	/**
	 * Get the concrete type of this TocObject.
	 */
	public abstract int getType();
	
	/**
	 * @return <code>true</code> iff the child parameter is the first
	 * of the TocObject's children.
	 */
	public boolean isFirstChildObject(TocObject tocObject) {
		//Returns false by default; subclasses that can have children
		//are expected to override this function
		return false;
	}

	/**
	 * @return <code>true</code> iff the child parameter is the last
	 * of the TocObject's children.
	 */
	public boolean isLastChildObject(TocObject tocObject) {
		//Returns false by default; subclasses that can have children
		//are expected to override this function
		return false;
	}
	
	/**
	 * @param tocObject the child used to locate a sibling
	 * @return the TocObject preceding the specified one
	 * in the list of children
	 */
	public TocObject getPreviousSibling(TocObject tocObject) {
		if(isFirstChildObject(tocObject))
		{	return null;
		}
		
		List children = getChildren();
		int position = children.indexOf(tocObject);
		if ((position == -1) ||
				(position == 0)) {
			// Either the item was not found or the item was found but it is 
			// at the first index
			return null;
		}
		
		return (TocObject)children.get(position - 1);
	}
	
	/**
	 * @param tocObject the child used to locate a sibling
	 * @return the TocObject proceeding the specified one
	 * in the list of children
	 */
	public TocObject getNextSibling(TocObject tocObject) {
		if(isLastChildObject(tocObject))
		{	return null;
		}

		List children = getChildren();
		int position = children.indexOf(tocObject);
		int lastIndex = children.size() - 1;
		if ((position == -1) ||
				(position == lastIndex)) {
			// Either the item was not found or the item was found but it is 
			// at the last index
			return null;
		}

		return (TocObject)children.get(position + 1);
	}


	/**
	 * @return true iff a child object can be removed
	 */
	public boolean canBeRemoved()
	{	if(getType() == TYPE_TOC)
		{	//Semantic Rule: The TOC root element can never be removed
			return false;
		}
		
		if(fParent != null)
		{	if (fParent.getType() == TYPE_TOC)
			{	//Semantic Rule: The TOC root element must always
				//have at least one child
				return fParent.getChildren().size() > 1;
			}
			
			return true;
		}
	
		return false;
	}
	
	/**
	 * Parses the given element's attributes and children.
	 * 
	 * @param element The XML element to parse.
	 */
	public void parse(Element element) {
		if (element.getNodeName().equals(getElement())) {
			parseAttributes(element);
			parseContent(element);
		}
	}

	/**
	 * Re-initializes the entire TocObject.
	 */
	public abstract void reset();

	/**
	 * Writes out the XML representation of this TocObject, and proceeds
	 * to write the elements of its children.
	 * 
	 * @param indent The indentation that will precede this element's data.
	 * @param writer The output stream to write the XML to.
	 */
	public void write(String indent, PrintWriter writer) {

		StringBuffer buffer = new StringBuffer();
		try {
			// Assemble start element
			buffer.append(getElement());
			// Assemble attributes
			writeAttributes(buffer);
			// Print start element and attributes
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print elements
			writeElements(indent, writer);
			// Print end element
			XMLPrintHandler.printEndElement(writer, getElement(), indent);
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 			
	}

	/**
	 * Signal that one of the TocObject's properties (attributes) has changed.
	 * 
	 * @param property The property that has changed.
	 * @param oldValue The old value of the property.
	 * @param newValue The current value of the property.
	 */
	protected void firePropertyChanged(String property, Object oldValue,
			Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
		
	/**
	 * Signal to the model that the object has changed.
	 * 
	 * @param object The object with a changed property.
	 * @param property The property that has changed.
	 * @param oldValue The old value of the property.
	 * @param newValue The current value of the property.
	 */
	private void firePropertyChanged(TocObject object, String property,
		Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
		
	/**
	 * Signals a change in the structure of the element, such as
	 * child addition or removal.
	 * 
	 * @param child The child changed in the TocObject.
	 * @param changeType The kind of change the child underwent.
	 */
	protected void fireStructureChanged(TocObject child, int changeType) {
		fireStructureChanged(new TocObject[] { child }, changeType);
	}
	
	/**
	 * Signal to the model that the TOC structure has changed.
	 * 
	 * @param children The children changed in the TocObject.
	 * @param changeType The kind of change the children underwent.
	 */
	private void fireStructureChanged(TocObject[] children,
			int changeType) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelChanged(new ModelChangedEvent(provider,
					changeType, children, null));
		}
	}
		
	/**
	 * @return true iff the model is not read-only.
	 */
	protected boolean isEditable() {
		return fModel.isEditable();
	}	
	
	/**
	 * Parse the attributes of this XML element.
	 * 
	 * @param element The element to parse.
	 */
	protected abstract void parseAttributes(Element element);
	
	/**
	 * Parse the contents of this XML element.
	 * 
	 * @param element the element to parse.
	 */
	protected void parseContent(Element element) {
		// Process children
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if(child.getNodeName().equals(ELEMENT_ENABLEMENT))
				{	parseEnablement((Element)child);
				}
				else
				{	parseElement((Element)child);
				}
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				parseText((Text)child);
			}
		}		
	}
	
	/**
	 * Parse the XML enablement of this element.
	 * 
	 * @param element the enablement to parse.
	 */
	private void parseEnablement(Element element)
	{	hasEnablement = true;
	
		fFieldEnablement = getModel().getFactory().createTocEnablement(this);
		fFieldEnablement.parse(element);
	}
	
	/**
	 * Parse an XML element child node of this element.
	 * 
	 * @param element the element to parse.
	 */
	protected abstract void parseElement(Element element);

	/**
	 * Parse the text contents of this element node.
	 * Currently, the TOC model contains no Text elements,
	 * and thus this is usually implemented as a no-op
	 * 
	 * @param text the element to parse.
	 */
	protected abstract void parseText(Text text);
	
	/**
	 * Write out the XML representations of the attributes
	 * associated with this TocObject.
	 * 
	 * @param buffer the buffer to which the attributes are written.
	 */
	protected abstract void writeAttributes(StringBuffer buffer);
	
	/**
	 * Writes child elements or child content.
	 * 
	 * @param indent The indentation that will precede the child data.
	 * @param writer The output stream to write the XML to.
	 */
	protected abstract void writeElements(String indent, PrintWriter writer);
	
	/**
	 * @return the name of the XML element associated with this TocObject
	 */
	public abstract String getElement();

	public boolean isHasEnablement() {
		return hasEnablement;
	}

	public void setHasEnablement(boolean hasEnablement) {
		this.hasEnablement = hasEnablement;
	}
}
