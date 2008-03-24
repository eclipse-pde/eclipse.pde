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
package org.eclipse.pde.internal.ds.core.text;

/**
 * TODO
 * 
 * @since 3.4
 * @see DSObject
 * @see DSModel
 * @see DSDocumentFactory
 */
public class DSRoot extends DSObject {

	private static final long serialVersionUID = 1L;

	public DSRoot(DSModel model) {
		super(model, ELEMENT_ROOT);
		setInTheModel(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#isRoot()
	 */
	public boolean isRoot() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_ROOT;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#getName()
	 */
	public String getName() {
		return ELEMENT_ROOT;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddChild(org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject)
	 */
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_CONTEXT;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddSibling(int)
	 */
	public boolean canAddSibling(int objectType) {
		return false;
	}
}
