/*******************************************************************************
 * Copyright (c) 2009 Anyware Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Anyware Technologies - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.ctxhelp.text;

import java.util.HashSet;

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.util.PDETextHelper;

public class CtxHelpDescriptionTextNode extends DocumentTextNode {

	private static final long serialVersionUID = 1L;

	private static final HashSet TAG_EXCEPTIONS = new HashSet(2);

	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
	}

	public CtxHelpDescriptionTextNode() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.core.text.DocumentTextNode#write()
	 */
	public String write() {
		String content = getText().trim();
		return PDETextHelper.translateWriteText(content, TAG_EXCEPTIONS,
				SUBSTITUTE_CHARS);
	}

}
