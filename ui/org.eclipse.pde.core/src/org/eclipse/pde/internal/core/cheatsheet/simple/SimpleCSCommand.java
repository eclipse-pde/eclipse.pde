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

import org.eclipse.pde.internal.core.XMLPrintHandler;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
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
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSCommand(ISimpleCSModel model) {
		super(model);
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
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		fConfirm = confirm;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setReturns(boolean)
	 */
	public void setReturns(String returns) {
		fReturns = returns;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setSerialization(java.lang.String)
	 */
	public void setSerialization(String serialization) {
		fSerialization = serialization;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSCommand#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		fWhen = when;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Element element) {
		// Process serialization attribute
		fSerialization = element.getAttribute(ATTRIBUTE_SERIALIZATION);
		// Process returns attribute
		fReturns = element.getAttribute(ATTRIBUTE_RETURNS);
		// Process confirm attribute
		if (element.getAttribute(ATTRIBUTE_CONFIRM).compareTo(
				ATTRIBUTE_VALUE_TRUE) == 0) {
			fConfirm = true;
		}		
		// Process when attribute
		fWhen = element.getAttribute(ATTRIBUTE_WHEN);
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
				buffer.append(XMLPrintHandler.wrapAttributeForPrint(
						ATTRIBUTE_SERIALIZATION, fSerialization));
			}
			if ((fReturns != null) && 
					(fReturns.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttributeForPrint(
						ATTRIBUTE_RETURNS, fReturns));
			}
			// Print skip attribute
			buffer.append(XMLPrintHandler.wrapAttributeForPrint(
					ATTRIBUTE_CONFIRM, new Boolean(fConfirm).toString()));
			// Print when attribute
			if ((fWhen != null) && 
					(fWhen.length() > 0)) {
				buffer.append(XMLPrintHandler.wrapAttributeForPrint(
						ATTRIBUTE_WHEN, fWhen));
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_COMMAND;
	}

}
