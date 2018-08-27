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

import org.eclipse.pde.internal.core.text.IDocumentTextNode;

/**
 * Represents a description entry in context help. There may be one description
 * element for each context. A description element will contain a CDATA string
 * explaining the context that the user will see in dynamic help. Descriptions
 * are leaf objects.
 *
 * @since 3.4
 * @see CtxHelpObject
 * @see CtxHelpModel
 * @see CtxHelpDocumentFactory
 */
public class CtxHelpDescription extends CtxHelpObject {

	private static final long serialVersionUID = 1L;

	public CtxHelpDescription(CtxHelpModel model) {
		super(model, ELEMENT_DESCRIPTION);
	}

	@Override
	protected IDocumentTextNode createDocumentTextNode() {
		return new CtxHelpDescriptionTextNode();
	}

	@Override
	public boolean canBeParent() {
		return false;
	}

	@Override
	public int getType() {
		return TYPE_DESCRIPTION;
	}

	@Override
	public String getName() {
		return getDescription();
	}

	/**
	 * @return the XML content of this element containing the description or
	 *         <code>null</code>
	 */
	public String getDescription() {
		return getXMLContent();
	}

	/**
	 * Sets the XML content of this element to the given description string.
	 * Passing <code>null</code> will set the content to be empty.
	 *
	 * @param description
	 *            new content
	 */
	public void setDescription(String description) {
		setXMLContent(description);
	}

	@Override
	public boolean canAddChild(int objectType) {
		return false;
	}

	@Override
	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_COMMAND || objectType == TYPE_TOPIC;
	}

	@Override
	protected String getTerminateIndent() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean isContentCollapsed() {
		return true;
	}
}
