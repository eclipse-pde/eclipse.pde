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
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.w3c.dom.Node;

/**
 * SimpleCS
 *
 */
public class SimpleCS extends SimpleCSObject implements ISimpleCS {

	/**
	 * Element:  intro
	 */
	private ISimpleCSIntro fIntro;
	
	/**
	 * Attribute:  title
	 */
	private String fTitle;
	
	/**
	 * Element:  item
	 */
	private ArrayList fItems;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCS(ISimpleCSModel model) {
		super(model);
		reset();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#addItems(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem[])
	 */
	public void addItems(ISimpleCSItem[] items) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getIntro()
	 */
	public ISimpleCSIntro getIntro() {
		return fIntro;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getItems()
	 */
	public ISimpleCSItem[] getItems() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#getTitle()
	 */
	public String getTitle() {
		return fTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#removeItems(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem[])
	 */
	public void removeItems(ISimpleCSItem[] items) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#reset()
	 */
	public void reset() {
		fIntro = null;
		fTitle = null;
		fItems = new ArrayList();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#setIntro(org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro)
	 */
	public void setIntro(ISimpleCSIntro intro) {
		fIntro = intro;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS#setTitle(java.lang.String)
	 */
	public void setTitle(String title) {
		fTitle = title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// TODO: MP: Get rid of hardcodings
		// TODO: MP: Use XMLPrinter
		// TODO: MP: Revisit
		writer.print(indent + "<cheatsheet"); //$NON-NLS-1$
		if ((fTitle != null) && 
				(fTitle.length() > 0)) {
			writer.print(" " + ATTRIBUTE_TITLE + "=\"" +  //$NON-NLS-1$ //$NON-NLS-2$
					getWritableString(fTitle) + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		writer.println(">"); //$NON-NLS-1$

		if (fIntro != null) {
			writer.println();
			fIntro.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		Iterator iterator = fItems.iterator();
		while (iterator.hasNext()) {
			ISimpleCSItem item = (ISimpleCSItem)iterator.next();
			item.write(indent + "   ", writer); //$NON-NLS-1$
		}
		
		writer.println();
		writer.println("</cheatsheet>"); //$NON-NLS-1$		

	}

	public void addItem(ISimpleCSItem item) {
		// TODO: MP: Revisit
		fItems.add(item);
	}

	public void removeItem(ISimpleCSItem item) {
		// TODO Auto-generated method stub
		
	}

}
