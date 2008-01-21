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
package org.eclipse.pde.internal.core.text.ctxhelp;

/**
 * Represents a description entry in context help.  There may be one description
 * element for each context.  A description element will contain a CDATA string
 * explaining the context that the user will see in dynamic help.  Descriptions
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.toc.TocObject#canBeParent()
	 */
	public boolean canBeParent() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getType()
	 */
	public int getType() {
		return TYPE_DESCRIPTION;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.toc.TocObject#getName()
	 */
	public String getName() {
		return getDescription();
	}

	/**
	 * @return the XML content of this element containing the description or <code>null</code>
	 */
	public String getDescription() {
		return getXMLContent();
	}

	/**
	 * Sets the XML content of this element to the given description string.
	 * Passing <code>null</code> will set the content to be empty.
	 * @param description new content
	 */
	public void setDescription(String description) {
		setXMLContent(description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddChild(org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject)
	 */
	public boolean canAddChild(int objectType) {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddSibling(int)
	 */
	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_COMMAND || objectType == TYPE_TOPIC;
	}
}
