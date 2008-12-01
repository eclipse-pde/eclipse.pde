/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.comp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public abstract class CompCSObject extends PlatformObject implements
		ICompCSObject {

	private static final long serialVersionUID = 1L;

	private transient ICompCSModel fModel;

	private transient ICompCSObject fParent;

	protected static final HashSet DEFAULT_TAG_EXCEPTIONS = new HashSet(12);

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
	 * @param model
	 * @param parent
	 */
	public CompCSObject(ICompCSModel model, ICompCSObject parent) {
		fModel = model;
		fParent = parent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getChildren
	 * ()
	 */
	public abstract List getChildren();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getCompCS()
	 */
	public ICompCS getCompCS() {
		return fModel.getCompCS();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getModel()
	 */
	public ICompCSModel getModel() {
		return fModel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getName()
	 */
	public abstract String getName();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getParent()
	 */
	public ICompCSObject getParent() {
		return fParent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getType()
	 */
	public abstract int getType();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#parse(org
	 * .w3c.dom.Element)
	 */
	public void parse(Element element) {
		if (element.getNodeName().equals(getElement())) {
			parseAttributes(element);
			parseContent(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#reset()
	 */
	public abstract void reset();

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#setModel
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel)
	 */
	public void setModel(ICompCSModel model) {
		fModel = model;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 * java.io.PrintWriter)
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
			// e.printStackTrace();
		}
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
	private void firePropertyChanged(ICompCSObject object, String property,
			Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			IModelChangeProvider provider = fModel;
			provider.fireModelObjectChanged(object, property, oldValue,
					newValue);
		}
	}

	/**
	 * @param child
	 * @param changeType
	 */
	protected void fireStructureChanged(ICompCSObject child, int changeType) {
		fireStructureChanged(new ICompCSObject[] { child }, changeType);
	}

	/**
	 * @param newValue
	 * @param oldValue
	 * @param changeType
	 */
	protected void fireStructureChanged(ICompCSObject newValue,
			ICompCSObject oldValue) {

		int changeType = -1;
		ICompCSObject object = null;
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
	private void fireStructureChanged(ICompCSObject[] children, int changeType) {
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
	 * @param element
	 */
	protected abstract void parseAttributes(Element element);

	/**
	 * @param element
	 */
	protected void parseContent(Element element) {
		// Process children
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				parseElement((Element) child);
			} else if (child.getNodeType() == Node.TEXT_NODE) {
				parseText((Text) child);
			}
		}
	}

	/**
	 * @param element
	 */
	protected abstract void parseElement(Element element);

	/**
	 * @param element
	 */
	protected abstract void parseText(Text text);

	/**
	 * @param buffer
	 */
	protected abstract void writeAttributes(StringBuffer buffer);

	/**
	 * Writes child elements or child content
	 * 
	 * @param indent
	 * @param writer
	 */
	protected abstract void writeElements(String indent, PrintWriter writer);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject#getElement()
	 */
	public abstract String getElement();
}
