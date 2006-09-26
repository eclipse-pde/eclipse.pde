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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCSPerformWhen
 *
 */
public class SimpleCSPerformWhen extends SimpleCSObject implements
		ISimpleCSPerformWhen {

	/**
	 * Attribute:  condition
	 */
	private String fCondition;	
	
	/**
	 * Elements:  action, command
	 */
	private ArrayList fExecutables;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSPerformWhen(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#getCondition()
	 */
	public String getCondition() {
		return fCondition;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#getExecutables()
	 */
	public ISimpleCSRunObject[] getExecutables() {
		return (ISimpleCSRunObject[]) fExecutables.toArray(
				new ISimpleCSRunObject[fExecutables.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#setCondition(java.lang.String)
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
		// Read as is. Do not translate
		fCondition = element.getAttribute(ATTRIBUTE_CONDITION);
		
		// Process children

		NodeList children = element.getChildNodes();
		ISimpleCSModelFactory factory = getModel().getFactory();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				Element childElement = (Element)child;

				if (name.equals(ELEMENT_COMMAND)) {
					ISimpleCSCommand command = factory.createSimpleCSCommand(this);
					fExecutables.add(command);
					command.parse(childElement);
				} else if (name.equals(ELEMENT_ACTION)) {
					ISimpleCSAction action = factory.createSimpleCSAction(this);
					fExecutables.add(action);
					action.parse(childElement);
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
			// Print perform-when element
			buffer.append(ELEMENT_PERFORM_WHEN);
			// Print condition attribute
			if ((fCondition != null) && 
					(fCondition.length() > 0)) {
				// Write as is.  Do not translate
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_CONDITION, fCondition));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print executables
			Iterator iterator = fExecutables.iterator();
			while (iterator.hasNext()) {
				ISimpleCSRunObject executable = (ISimpleCSRunObject)iterator.next();
				executable.write(newIndent, writer);
			}
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_PERFORM_WHEN, indent);
			
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
		fExecutables = new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_PERFORM_WHEN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not supported in editor UI
		return ELEMENT_PERFORM_WHEN;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#addExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject)
	 */
	public void addExecutable(ISimpleCSRunObject executable) {
		fExecutables.add(executable);
		
		if (isEditable()) {
			fireStructureChanged(executable, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSPerformWhen#removeExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject)
	 */
	public void removeExecutable(ISimpleCSRunObject executable) {
		fExecutables.remove(executable);
		
		if (isEditable()) {
			fireStructureChanged(executable, IModelChangedEvent.REMOVE);
		}			
	}

}
