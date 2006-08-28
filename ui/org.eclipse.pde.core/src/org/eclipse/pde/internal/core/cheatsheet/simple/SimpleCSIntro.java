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

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.w3c.dom.Node;

/**
 * SimpleCSIntro
 *
 */
public class SimpleCSIntro extends SimpleCSObject implements ISimpleCSIntro {

	/**
	 * Element:  description
	 */
	private String fDescription;	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSIntro(ISimpleCSModel model) {
		super(model);
		// TODO: MP: create reset method
		fDescription = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getContextId()
	 */
	public String getContextId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getDescription()
	 */
	public String getDescription() {
		return fDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#getHref()
	 */
	public String getHref() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		fDescription = description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro#setHref(java.lang.String)
	 */
	public void setHref(String href) {
		// TODO Auto-generated method stub

	}

	public void parse(Node node) {
		// TODO Auto-generated method stub
		
	}

	public void write(String indent, PrintWriter writer) {
		// TODO: MP: Revisit
		// TODO: MP: Get rid of hardcodings
		// TODO: MP: Use XMLPrinter		
		writer.println(indent + "<!-- Introduction -->"); //$NON-NLS-1$
		writer.println();
		writer.println(indent + "<intro>"); //$NON-NLS-1$

		if (fDescription != null) {
			writer.print(indent + "   " + "<description>"); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println();
			writer.print(indent + "      " + getWritableString(fDescription)); //$NON-NLS-1$
			writer.println();
			writer.print(indent + "   " + "</description>"); //$NON-NLS-1$ //$NON-NLS-2$
			
			// TODO: MP: Create a description class
			//fDescription.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println(indent + "</intro>"); //$NON-NLS-1$		
		
	}

}
