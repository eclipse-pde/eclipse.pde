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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask;
import org.w3c.dom.Element;

public class CompCSTask extends CompCSTaskObject implements ICompCSTask {

	private ArrayList fFieldParams;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public CompCSTask(ICompCSModel model, ICompCSObject parent) {
		super(model, parent);
		reset();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#getChildren
	 * ()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#getName()
	 */
	public String getName() {
		return fFieldName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#getType()
	 */
	public int getType() {
		return TYPE_TASK;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#getElement()
	 */
	public String getElement() {
		return ELEMENT_TASK;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSObject#parseElement
	 * (org.w3c.dom.Element)
	 */
	protected void parseElement(Element element) {
		super.parseElement(element);
		String name = element.getNodeName();
		ICompCSModelFactory factory = getModel().getFactory();

		if (name.equals(ELEMENT_PARAM)) {
			// Process param element
			ICompCSParam param = factory.createCompCSParam(this);
			fFieldParams.add(param);
			param.parse(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask#addFieldParam
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam)
	 */
	public void addFieldParam(ICompCSParam param) {
		fFieldParams.add(param);
		if (isEditable()) {
			fireStructureChanged(param, IModelChangedEvent.INSERT);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask#getFieldParams
	 * ()
	 */
	public ICompCSParam[] getFieldParams() {
		return (ICompCSParam[]) fFieldParams
				.toArray(new ICompCSParam[fFieldParams.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask#removeFieldParam
	 * (org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSParam)
	 */
	public void removeFieldParam(ICompCSParam param) {
		fFieldParams.remove(param);
		if (isEditable()) {
			fireStructureChanged(param, IModelChangedEvent.REMOVE);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#reset()
	 */
	public void reset() {
		super.reset();

		fFieldParams = new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSTaskObject#writeElements
	 * (java.lang.String, java.io.PrintWriter)
	 */
	protected void writeElements(String indent, PrintWriter writer) {
		super.writeElements(indent, writer);
		String newIndent = indent + XMLPrintHandler.XML_INDENT;
		// Print param elements
		Iterator iterator = fFieldParams.iterator();
		while (iterator.hasNext()) {
			ICompCSParam param = (ICompCSParam) iterator.next();
			param.write(newIndent, writer);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask#hasFieldParams
	 * ()
	 */
	public boolean hasFieldParams() {
		if (fFieldParams.isEmpty()) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTask#getFieldParam
	 * (java.lang.String)
	 */
	public ICompCSParam getFieldParam(String name) {
		if (fFieldParams.isEmpty()) {
			return null;
		}
		ListIterator iterator = fFieldParams.listIterator();
		while (iterator.hasNext()) {
			ICompCSParam parameter = (ICompCSParam) iterator.next();
			if (parameter.getFieldName().equals(name)) {
				return parameter;
			}
		}
		return null;
	}
}
