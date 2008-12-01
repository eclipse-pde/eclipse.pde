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

import org.eclipse.pde.internal.core.text.IDocumentElementNode;

/**
 * Represents a context entry in context help. Contexts have a specific id that
 * allows UI elements to be associated with a list of commands and topics that
 * are stored in the context. Contexts may have one optional description element
 * and as many topic and command elements as needed.
 * 
 * @since 3.4
 * @see CtxHelpObject
 * @see CtxHelpModel
 * @see CtxHelpDocumentFactory
 */
public class CtxHelpContext extends CtxHelpObject {

	private static final long serialVersionUID = 1L;

	public CtxHelpContext(CtxHelpModel model) {
		super(model, ELEMENT_CONTEXT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#canBeParent()
	 */
	public boolean canBeParent() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#getType()
	 */
	public int getType() {
		return TYPE_CONTEXT;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#getName()
	 */
	public String getName() {
		return getId();
	}

	/**
	 * @return the value of the id attribute or <code>null</code> if the
	 *         attribute does not exist
	 */
	public String getId() {
		return getXMLAttributeValue(ATTRIBUTE_ID);
	}

	/**
	 * Set the id attribute, passing <code>null</code> will set the attribute to
	 * be empty.
	 * 
	 * @param id
	 *            new value
	 */
	public void setID(String id) {
		setXMLAttribute(ATTRIBUTE_ID, id);
	}

	/**
	 * @return the value of the title attribute or <code>null</code> if the
	 *         attribute does not exist
	 */
	public String getTitle() {
		return getXMLAttributeValue(ATTRIBUTE_TITLE);
	}

	/**
	 * Set the title attribute, passing <code>null</code> will set the attribute
	 * to be empty.
	 * 
	 * @param title
	 *            new value
	 */
	public void setTitle(String title) {
		setXMLAttribute(ATTRIBUTE_TITLE, title);
	}

	/**
	 * Searches for a child description node and returns the string contents of
	 * that node. Returns <code>null</code> if no description node exists.
	 * 
	 * @return description associated with this context or <code>null</code> no
	 *         description exists
	 */
	public String getDescription() {
		IDocumentElementNode node = getChildNode(CtxHelpDescription.class);
		if (node instanceof CtxHelpDescription) {
			return ((CtxHelpDescription) node).getDescription();
		}
		return null;
	}

	/**
	 * Set the description to be associated with this context. Searches for a
	 * child description node and updates the content with the given string. If
	 * a description node does not exist, one will be created. Passing
	 * <code>null</code> will remove any description node if one exists.
	 * 
	 * @param description
	 *            new value
	 */
	public void setDescription(String description) {
		IDocumentElementNode node = getChildNode(CtxHelpDescription.class);
		if (node instanceof CtxHelpDescription) {
			if (description == null) {
				removeChildNode(node, true);
			} else {
				((CtxHelpDescription) node).setDescription(description);
			}
		} else if (description != null) {
			CtxHelpDescription newDescription = getModel().getFactory()
					.createDescription();
			newDescription.setDescription(description);
			addChildNode(newDescription, 0, true);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject#canAddChild(
	 * org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject)
	 */
	public boolean canAddChild(int objectType) {
		return objectType == TYPE_TOPIC || objectType == TYPE_COMMAND;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.core.text.ctxhelp.CtxHelpObject#canAddSibling
	 * (int)
	 */
	public boolean canAddSibling(int objectType) {
		return objectType == TYPE_CONTEXT;
	}

}
