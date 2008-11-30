/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.core.toc.text;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.toc.ITocConstants;

/**
 * TocObject - All objects modeled in a Table of Contents subclass TocObject
 * This class contains functionality common to all TOC elements.
 */
public abstract class TocObject extends DocumentObject implements
		ITocConstants, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs the TocObject and initializes its attributes.
	 * 
	 * @param model
	 *            The model associated with this TocObject.
	 * @param parent
	 *            The parent of this TocObject.
	 */
	public TocObject(TocModel model, String tagName) {
		super(model, tagName);
	}

	protected String getAttributeIndent() {
		return " "; //$NON-NLS-1$
	}

	/**
	 * @return the children of the object or an empty List if none exist.
	 */
	public List getChildren() { // Create a copy of the child list instead of
		// returning the list itself. That way, our list
		// of children cannot be altered from outside
		ArrayList list = new ArrayList();

		// Add children of this topic
		IDocumentElementNode[] childNodes = getChildNodes();
		if (childNodes.length > 0) {
			for (int i = 0; i < childNodes.length; ++i) {
				if (childNodes[i] instanceof TocObject) {
					list.add(childNodes[i]);
				}
			}
		}

		return list;
	}

	public boolean isLeafNode() {
		return !canBeParent();
	}

	/**
	 * @return true iff this TOC object is capable of containing children.
	 */
	public abstract boolean canBeParent();

	/**
	 * @return the root TOC element that is an ancestor to this TocObject.
	 */
	public TocModel getModel() {
		final IModel sharedModel = getSharedModel();
		if (sharedModel instanceof TocModel) {
			return (TocModel) sharedModel;
		}

		return null;
	}

	/**
	 * @return the root TOC element that is an ancestor to this TocObject.
	 */
	public Toc getToc() {
		final TocModel model = getModel();

		if (model != null) {
			return model.getToc();
		}

		return null;
	}

	/**
	 * @return the identifier for this TocObject.
	 */
	public abstract String getName();

	/**
	 * @return the path to the resource associated with this TOC object or
	 *         <code>null</code> if one does not exist.
	 */
	public abstract String getPath();

	/**
	 * @return the parent of this TocObject, or <br />
	 *         <code>null</code> if the TocObject has no parent.
	 */
	public TocObject getParent() {
		IDocumentElementNode parent = getParentNode();
		return parent instanceof TocObject ? (TocObject) parent : null;
	}

	/**
	 * Check if the object is a direct or indirect descendant of the object
	 * parameter.
	 * 
	 * @param obj
	 *            The TOC object to find in this object's ancestry
	 * @return true iff obj is an ancestor of this TOC object
	 */
	public boolean descendsFrom(TocObject obj) {
		if (this.equals(obj)) {
			return true;
		}

		if (getParent() != null && obj.canBeParent()) {
			return getParent().descendsFrom(obj);
		}

		return false;
	}

	/**
	 * Get the concrete type of this TocObject.
	 */
	public abstract int getType();

	/**
	 * @param tocObject
	 *            the child used to locate a sibling
	 * @return the TocObject preceding the specified one in the list of children
	 */
	public TocObject getPreviousSibling(TocObject tocObject) {
		return (TocObject) getPreviousSibling(tocObject, TocObject.class);
	}

	/**
	 * @param tocObject
	 *            the child used to locate a sibling
	 * @return the TocObject proceeding the specified one in the list of
	 *         children
	 */
	public TocObject getNextSibling(TocObject tocObject) {
		return (TocObject) getNextSibling(tocObject, TocObject.class);
	}

	/**
	 * @return true iff a child object can be removed
	 */
	public boolean canBeRemoved() {
		if (getType() == TYPE_TOC) { // Semantic Rule: The TOC root element can
										// never be removed
			return false;
		}

		TocObject parent = getParent();
		if (parent != null) {
			if (parent.getType() == TYPE_TOC) { // Semantic Rule: The TOC root
												// element must always
				// have at least one child
				return parent.getChildren().size() > 1;
			}

			return true;
		}

		return false;
	}
}
