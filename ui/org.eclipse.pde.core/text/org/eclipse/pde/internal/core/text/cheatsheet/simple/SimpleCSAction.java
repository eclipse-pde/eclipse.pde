/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.cheatsheet.simple;

import java.io.PrintWriter;
import java.util.List;

import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModel;

/**
 * SimpleCSAction
 *
 */
public class SimpleCSAction extends SimpleCSObject implements ISimpleCSAction {

	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public SimpleCSAction(ISimpleCSModel model) {
		super(model);
		// TODO: MP: CURRENT: IMPLEMENT
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getClazz()
	 */
	public String getClazz() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParam(int)
	 */
	public String getParam(int index) {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getParams()
	 */
	public String[] getParams() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#getPluginId()
	 */
	public String getPluginId() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setClazz(java.lang.String)
	 */
	public void setClazz(String clazz) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setParam(java.lang.String, int)
	 */
	public void setParam(String param, int index) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSAction#setPluginId(java.lang.String)
	 */
	public void setPluginId(String pluginId) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getConfirm()
	 */
	public boolean getConfirm() {
		// TODO: MP: CURRENT: IMPLEMENT
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getTranslate()
	 */
	public String getTranslate() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#getWhen()
	 */
	public String getWhen() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setConfirm(boolean)
	 */
	public void setConfirm(boolean confirm) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setTranslate(java.lang.String)
	 */
	public void setTranslate(String translate) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSRunObject#setWhen(java.lang.String)
	 */
	public void setWhen(String when) {
		// TODO: MP: CURRENT: IMPLEMENT

	}

	public List getChildren() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	public String getName() {
		// TODO: MP: CURRENT: IMPLEMENT
		return null;
	}

	public int getType() {
		// TODO: MP: CURRENT: IMPLEMENT
		return 0;
	}

	public void write(String indent, PrintWriter writer) {
		// TODO: MP: CURRENT: IMPLEMENT
		
	}

}
