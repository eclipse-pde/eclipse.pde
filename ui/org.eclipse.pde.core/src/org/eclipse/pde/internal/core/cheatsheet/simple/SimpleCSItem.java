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

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject;
import org.w3c.dom.Node;

/**
 * SimpleCSItem
 *
 */
public class SimpleCSItem extends SimpleCSObject implements ISimpleCSItem {

	/**
	 * Element:  description
	 */
	private String fDescription;
	
	/**
	 * Attribute:  title
	 */
	private String fTitle;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSItem(ISimpleCSModel model) {
		super(model);
		// TODO: MP: Add a reset method
		fDescription = null;
		fTitle = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#addSubItems(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject[])
	 */
	public void addSubItems(ISimpleCSSubItemObject[] subitems) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getContextId()
	 */
	public String getContextId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getDescription()
	 */
	public String getDescription() {
		return fDescription;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getDialog()
	 */
	public boolean getDialog() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getExecutable()
	 */
	public ISimpleCSRunContainerObject getExecutable() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getHref()
	 */
	public String getHref() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSkip()
	 */
	public boolean getSkip() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getSubItems()
	 */
	public ISimpleCSSubItemObject[] getSubItems() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#getTitle()
	 */
	public String getTitle() {
		return fTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#removeSubItems(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSSubItemObject[])
	 */
	public void removeSubItems(ISimpleCSSubItemObject[] subitems) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setContextId(java.lang.String)
	 */
	public void setContextId(String contextId) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setDescription(java.lang.String)
	 */
	public void setDescription(String description) {
		fDescription = description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setDialog(boolean)
	 */
	public void setDialog(boolean dialog) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setExecutable(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunContainerObject)
	 */
	public void setExecutable(ISimpleCSRunContainerObject executable) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setHref(java.lang.String)
	 */
	public void setHref(String href) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setSkip(boolean)
	 */
	public void setSkip(boolean skip) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		fTitle = title;
	}

	public void parse(Node node) {
		// TODO Auto-generated method stub
		
	}

	public void write(String indent, PrintWriter writer) {
		// TODO: MP: Revisit
		// TODO: MP: Get rid of hardcodings
		// TODO: MP: Use XMLPrinter		
		writer.println(indent + "<!-- Item -->"); //$NON-NLS-1$
		writer.println();
		writer.print(indent + "<item"); //$NON-NLS-1$
		if ((fTitle != null) && 
				(fTitle.length() > 0)) {
			writer.print(" " + ATTRIBUTE_TITLE + "=\"" +  //$NON-NLS-1$ //$NON-NLS-2$
					getWritableString(fTitle) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		writer.println(">"); //$NON-NLS-1$

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
		writer.println(indent + "</item>"); //$NON-NLS-1$		
		
		
	}

}
