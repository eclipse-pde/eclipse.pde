/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSModel;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSOnCompletion;

public class SimpleCSOnCompletion extends SimpleCSObject implements
		ISimpleCSOnCompletion {

	private static final long serialVersionUID = 1L;

	/**
	 * @param model
	 */
	public SimpleCSOnCompletion(ISimpleCSModel model) {
		super(model, ELEMENT_ONCOMPLETION);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSOnCompletion
	 * #getContent()
	 */
	public String getContent() {
		return getXMLContent();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.icheatsheet.simple.ISimpleCSOnCompletion
	 * #setContent(java.lang.String)
	 */
	public void setContent(String content) {
		setXMLContent(content);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#
	 * getChildren()
	 */
	public List getChildren() {
		return new ArrayList();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getName
	 * ()
	 */
	public String getName() {
		return ELEMENT_ONCOMPLETION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.text.cheatsheet.simple.SimpleCSObject#getType
	 * ()
	 */
	public int getType() {
		return TYPE_ON_COMPLETION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#
	 * isContentCollapsed()
	 */
	public boolean isContentCollapsed() {
		return true;
	}

}
