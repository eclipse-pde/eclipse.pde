/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.core.cheatsheet.simple.text;

import java.util.HashSet;

import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.util.PDETextHelper;

public class SimpleCSDocumentTextNode extends DocumentTextNode {

	private static final long serialVersionUID = 1L;

	private static final HashSet<String> TAG_EXCEPTIONS = new HashSet<>(3);

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

	@Override
	public String write() {
		String content = getText().trim();
		return PDETextHelper.translateWriteText(content, TAG_EXCEPTIONS,
				SUBSTITUTE_CHARS);
	}

}
