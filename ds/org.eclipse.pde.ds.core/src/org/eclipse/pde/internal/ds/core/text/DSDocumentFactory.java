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

import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;
import org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpRoot;

/**
 * Handles the creation of document nodes representing the types of elements that
 * can exist in a declarative services xml file.
 * 
 * @since 3.4
 * @see DSModel
 * @see DSDocumentHandler
 */
public class DSDocumentFactory extends DocumentNodeFactory implements IDocumentNodeFactory {
	private DSModel fModel;

	public DSDocumentFactory(DSModel model) {
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentElementNode)
	 */
	public IDocumentElementNode createDocumentNode(String name, IDocumentElementNode parent) {
		if (isRoot(name)) { // Root
			return createRoot();
		}
		return super.createDocumentNode(name, parent);
	}

	public IDocumentElementNode createRoot() {
		return new DSRoot(fModel);
	}

	private boolean isRoot(String name) {
		return name.equals("<component>"); 
	}

}
