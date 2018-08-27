/*******************************************************************************
 * Copyright (c) 2009, 2017 Anyware Technologies and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

	private static final HashSet<String> TAG_EXCEPTIONS = new HashSet<>(2);

	static {
		TAG_EXCEPTIONS.add("b"); //$NON-NLS-1$
		TAG_EXCEPTIONS.add("/b"); //$NON-NLS-1$
	}

	public CtxHelpDescriptionTextNode() {
		super();
	}

	@Override
	public String write() {
		String content = getText().trim();
		return PDETextHelper.translateWriteText(content, TAG_EXCEPTIONS,
				SUBSTITUTE_CHARS);
	}

}
