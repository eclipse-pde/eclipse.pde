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
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;
import org.w3c.dom.Element;

/**
 * SimpleCSAction
 *
 */
public class SimpleCSAction extends SimpleCSObject implements ISimpleCSAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSAction(ISimpleCSModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#addParams(java.lang.String[])
	 */
	public void addParams(String[] params) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getClazz()
	 */
	public String getClazz() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getConfirm()
	 */
	public boolean getConfirm() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParams()
	 */
	public String[] getParams() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getPluginId()
	 */
	public String getPluginId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#removeParams(java.lang.String[])
	 */
	public void removeParams(String[] subitems) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setClazz(java.lang.String)
	 */
	public void setClazz(String clazz) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setPluginId(java.lang.String)
	 */
	public void setPluginId(String pluginId) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#parse(org.w3c.dom.Element)
	 */
	public void parse(Element element) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#reset()
	 */
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSObject#getType()
	 */
	public int getType() {
		return TYPE_ACTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getWhen()
	 */
	public String getWhen() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getName()
	 */
	public String getName() {
		// TODO: MP: Update name
		return ELEMENT_ACTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSObject#getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

}
