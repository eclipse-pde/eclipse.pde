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

import java.util.HashSet;

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.util.PDETextHelper;

/**
 * SimpleCSDocumentTextNode
 *
 */
public class SimpleCSDocumentTextNode extends DocumentTextNode {

	private static final long serialVersionUID = 1L;

	private static final HashSet TAG_EXCEPTIONS = new HashSet(3);
	
	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("br/"); //$NON-NLS-1$
	}	
	
	/**
	 * 
	 */
	public SimpleCSDocumentTextNode() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.DocumentTextNode#write()
	 */
	public String write() {
		String content = getText().trim();
		return PDETextHelper.translateWriteText(content,
				TAG_EXCEPTIONS, SUBSTITUTE_CHARS);
	}
	
}
