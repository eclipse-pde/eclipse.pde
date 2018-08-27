/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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

	@Override
	public boolean isRoot() {
		return true;
	}

	@Override
	public int getType() {
		return TYPE_ROOT;
	}

	@Override
	public boolean canBeParent() {
		return true;
	}

	@Override
	public String getName() {
		return ELEMENT_ROOT;
	}

	@Override
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_CONTEXT;
	}

	@Override
	public boolean canAddSibling(int objectType) {
		return false;
	}
}
