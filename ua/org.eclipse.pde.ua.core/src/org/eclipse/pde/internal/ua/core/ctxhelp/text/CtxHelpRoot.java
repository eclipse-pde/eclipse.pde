/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.ctxhelp.text;

/**
 * Represents the root "contexts" entry in a context help xml file. There may be
 * only one root node in the file and all other nodes must be inside the root.
 * The root may contain many context elements.
 * 
 * @since 3.4
 * @see CtxHelpObject
 * @see CtxHelpModel
 * @see CtxHelpDocumentFactory
 */
public class CtxHelpRoot extends CtxHelpObject {

	private static final long serialVersionUID = 1L;

	public CtxHelpRoot(CtxHelpModel model) {
		super(model, ELEMENT_ROOT);
		setInTheModel(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#getType()
	 */
	public int getType() {
		return TYPE_ROOT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#getName()
	 */
	public String getName() {
		return ELEMENT_ROOT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#canAddChild
	 * (org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject)
	 */
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_CONTEXT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#canAddSibling
	 * (int)
	 */
	public boolean canAddSibling(int objectType) {
		return false;
	}
}
