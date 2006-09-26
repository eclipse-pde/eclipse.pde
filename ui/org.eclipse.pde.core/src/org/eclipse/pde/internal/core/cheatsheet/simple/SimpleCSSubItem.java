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
import java.util.HashMap;
import java.util.List;

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * SimpleCSSubItem
 *
 */
public class SimpleCSSubItem extends SimpleCSObject implements ISimpleCSSubItem {

	
	/**
	 * Attribute:  label
	 */
	private String fLabel;
	
	/**
	 * Attribute:  skip
	 */
	private boolean fSkip;
	
	/**
	 * Attribute:  when
	 */
	private String fWhen;
	
	/**
	 * Elements:  action, command, perform-when
	 */
	private ISimpleCSRunContainerObject fExecutable;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final HashMap LABEL_SUBSTITUTE_CHARS = new HashMap(7);
	
	static {
		LABEL_SUBSTITUTE_CHARS.putAll(SUBSTITUTE_CHARS);
		LABEL_SUBSTITUTE_CHARS.put(new Character('\n'), " "); //$NON-NLS-1$
		LABEL_SUBSTITUTE_CHARS.put(new Character('\r'), ""); //$NON-NLS-1$
	}		
	
	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSSubItem(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#getExecutable()
	 */
	public ISimpleCSRunContainerObject getExecutable() {
		return fExecutable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#getLabel()
	 */
	public String getLabel() {
		return fLabel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#getSkip()
	 */
	public boolean getSkip() {
		return fSkip;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#getWhen()
	 */
	public String getWhen() {
		return fWhen;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#setExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject)
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		ISimpleCSObject old = fExecutable;		
		fExecutable = executable;

		if (isEditable()) {
			fireStructureChanged(executable, old);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#setLabel(java.lang.String)
	 */
	public void setLabel(String label) {
		String old = fLabel;
		fLabel = label;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_LABEL, old, fLabel);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#setSkip(boolean)
	 */
	public void setSkip(boolean skip) {
		Boolean old = Boolean.valueOf(fSkip);
		fSkip = skip;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_SKIP, old, Boolean.valueOf(fSkip));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItem#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		String old = fWhen;
		fWhen = when;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_WHEN, old, fWhen);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Element element) {
		// Process label attribute
		fLabel = element.getAttribute(ATTRIBUTE_LABEL).trim();
		// Process skip attribute
		if (element.getAttribute(ATTRIBUTE_SKIP).compareTo(
				ATTRIBUTE_VALUE_TRUE) == 0) {
			fSkip = true;
		}
		// Process when attribute
		// Read as is. Do not translate
		fWhen = element.getAttribute(ATTRIBUTE_WHEN);
		// Process children
		NodeList children = element.getChildNodes();
		ISimpleCSModelFactory factory = getModel().getFactory();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String name = child.getNodeName();
				Element childElement = (Element)child;

				if (name.equals(ELEMENT_ACTION)) {
					fExecutable = factory.createSimpleCSAction(this);
					fExecutable.parse(childElement);
				} else if (name.equals(ELEMENT_COMMAND)) {
					fExecutable = factory.createSimpleCSCommand(this);
					fExecutable.parse(childElement);
				} else if (name.equals(ELEMENT_PERFORM_WHEN)) {
					fExecutable = factory.createSimpleCSPerformWhen(this);
					fExecutable.parse(childElement);
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
			// Print subitem element
			buffer.append(ELEMENT_SUBITEM); //$NON-NLS-1$
			// Print label attribute
			if ((fLabel != null) && 
					(fLabel.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_LABEL, 
						PDETextHelper.translateWriteText(
								fLabel.trim(), LABEL_SUBSTITUTE_CHARS)));
			}
			// Print skip attribute
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_SKIP, new Boolean(fSkip).toString()));
			// Print when attribute
			if ((fWhen != null) && 
					(fWhen.length() > 0)) {
				// Write as is.  Do not translate
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_WHEN, fWhen));
			}
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// Print action | command | perform-when element
			if (fExecutable != null) {
				fExecutable.write(newIndent, writer);
			}
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_SUBITEM, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 				
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fLabel = null;
		fSkip = false;
		fWhen = null;
		fExecutable = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_SUBITEM;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		return fLabel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		ArrayList list = new ArrayList();
		if ((fExecutable != null) &&
				(fExecutable.getType() == ISimpleCSConstants.TYPE_PERFORM_WHEN)) {
			list.add(fExecutable);
		}
		return list;
	}

}
