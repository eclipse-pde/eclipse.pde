/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

/**
 * SimpleCSCommand
 *
 */
public class SimpleCSCommand extends SimpleCSObject implements ISimpleCSCommand {

	/**
	 * Attribute:  serialization
	 */
	private String fSerialization;
	
	/**
	 * Attribute:  returns
	 */
	private String fReturns;
	
	/**
	 * Attribute:  confirm
	 */
	private boolean fConfirm;
	
	/**
	 * Attribute:  when
	 */
	private String fWhen;
	
	/**
	 * Attribute:  translate
	 */
	private String fTranslate;	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 * @param parent
	 */
	public SimpleCSCommand(ISimpleCSModel model, ISimpleCSObject parent) {
		super(model, parent);
		reset();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#getConfirm()
	 */
	public boolean getConfirm() {
		return fConfirm;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#getReturns()
	 */
	public String getReturns() {
		return fReturns;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#getSerialization()
	 */
	public String getSerialization() {
		return fSerialization;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#getWhen()
	 */
	public String getWhen() {
		return fWhen;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getTranslate()
	 */
	public String getTranslate() {
		return fTranslate;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		Boolean old =  Boolean.valueOf(fConfirm);
		fConfirm = confirm;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_CONFIRM, old, Boolean.valueOf(fConfirm));
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setReturns(boolean)
	 */
	public void setReturns(String returns) {
		String old = fReturns;
		fReturns = returns;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_RETURNS, old, fReturns);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setSerialization(java.lang.String)
	 */
	public void setSerialization(String serialization) {
		String old = fSerialization;
		fSerialization = serialization;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_SERIALIZATION, old, fSerialization);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		String old = fWhen;
		fWhen = when;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_WHEN, old, fWhen);
		}		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setTranslate(java.lang.String)
	 */
	public void setTranslate(String translate) {
		String old = fTranslate;
		fTranslate = translate;
		if (isEditable()) {
			firePropertyChanged(ATTRIBUTE_TRANSLATE, old, fTranslate);
		}		
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Element element) {
		// Process serialization attribute
		// Read as is. Do not translate
		fSerialization = element.getAttribute(ATTRIBUTE_SERIALIZATION);
		// Process returns attribute
		// Read as is. Do not translate
		fReturns = element.getAttribute(ATTRIBUTE_RETURNS);
		// Process confirm attribute
		if (element.getAttribute(ATTRIBUTE_CONFIRM).compareTo(
				ATTRIBUTE_VALUE_TRUE) == 0) {
			fConfirm = true;
		}		
		// Process when attribute
		// Read as is. Do not translate
		fWhen = element.getAttribute(ATTRIBUTE_WHEN);
		// Process translate attribute
		// Read as is. Do not translate
		// Need to be able to write out the empty string
		Attr translateAttribute = element.getAttributeNode(ATTRIBUTE_TRANSLATE);
		if (translateAttribute == null) {
			fTranslate = null;
		} else {
			fTranslate = translateAttribute.getValue();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		
		StringBuffer buffer = new StringBuffer();
		
		try {
			// Print command element
			buffer.append(ELEMENT_COMMAND); 
			// Print serialization attribute
			if ((fSerialization != null) && 
					(fSerialization.length() > 0)) {
				// Write as is.  Do not translate
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_SERIALIZATION, fSerialization));
			}
			if ((fReturns != null) && 
					(fReturns.length() > 0)) {
				// Write as is.  Do not translate
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_RETURNS, fReturns));
			}
			// Print confirm attribute
			buffer.append(XMLPrintHandler.wrapAttribute(
					ATTRIBUTE_CONFIRM, new Boolean(fConfirm).toString()));
			// Print when attribute
			if ((fWhen != null) && 
					(fWhen.length() > 0)) {
				// Write as is.  Do not translate				
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_WHEN, fWhen));
			}
			// Print translate attribute
			if (fTranslate != null) {
				// Write as is.  Do not translate				
				buffer.append(XMLPrintHandler.wrapAttribute(
						ATTRIBUTE_TRANSLATE, fTranslate));
			}			
			// Start element
			XMLPrintHandler.printBeginElement(writer, buffer.toString(),
					indent, false);
			// End element
			XMLPrintHandler.printEndElement(writer, ELEMENT_COMMAND, indent);
			
		} catch (IOException e) {
			// Suppress
			//e.printStackTrace();
		} 	
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		fSerialization = null;
		fReturns = null;
		fConfirm = false;
		fWhen = null;
		fTranslate = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_COMMAND;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// Leave as is.  Not a separate node in tree view
		return ELEMENT_COMMAND;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

}
