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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.DocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.ua.core.ctxhelp.ICtxHelpConstants;

/**
 * All modeled objects of a context help xml file must extend from this abstract
 * class.
 *
 * @since 3.4
 * @see CtxHelpModel
 * @see CtxHelpDocumentFactory
 */
public abstract class CtxHelpObject extends DocumentObject implements
		ICtxHelpConstants {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs the CtxHelpObject and initializes its attributes.
	 *
	 * @param model
	 *            The model to associate with this CtxHelpObject
	 * @param tagName
	 *            The xml tag name for this object
	 */
	public CtxHelpObject(CtxHelpModel model, String tagName) {
		super(model, tagName);
	}

	/**
	 * @return the children of the object or an empty List if none exist.
	 */
	public List<CtxHelpObject> getChildren() {
		// Create a copy of the child list instead of
		// returning the list itself. That way, our list
		// of children cannot be altered from outside
		List<CtxHelpObject> list = new ArrayList<>();

		// Add children of this topic
		IDocumentElementNode[] childNodes = getChildNodes();
		if (childNodes.length > 0) {
			for (IDocumentElementNode childNode : childNodes) {
				if (childNode instanceof CtxHelpObject) {
					list.add((CtxHelpObject) childNode);
				}
			}
		}

		return list;
	}

	@Override
	protected String getAttributeIndent() {
		return " "; //$NON-NLS-1$
	}

	@Override
	protected String getContentIndent() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean isLeafNode() {
		return !canBeParent();
	}

	/**
	 * @return true if this object is capable of containing children.
	 */
	public abstract boolean canBeParent();

	public abstract boolean canAddChild(int objectType);

	public abstract boolean canAddSibling(int objectType);

	// public abstract boolean canAddSibling(int objectType);

	public void addChild(CtxHelpObject newObject, CtxHelpObject targetSibling,
			boolean insertBefore) {
		if (canAddChild(newObject.getType())) {
			int currentIndex = indexOf(targetSibling);
			if (!insertBefore) {
				currentIndex++;
			}
			addChildNode(newObject, currentIndex, true);
		}
	}

	public void addChild(CtxHelpObject newObject) {
		if (canAddChild(newObject.getType())) {
			addChildNode(newObject, true);
		}
	}

	/**
	 * @return the root model object that is an ancestor to this object.
	 */
	public CtxHelpModel getModel() {
		final IModel sharedModel = getSharedModel();
		if (sharedModel instanceof CtxHelpModel) {
			return (CtxHelpModel) sharedModel;
		}
		return null;
	}

	/**
	 * @return the root element that is an ancestor to this object.
	 */
	public CtxHelpRoot getRoot() {
		final CtxHelpModel model = getModel();
		if (model != null) {
			return model.getCtxHelpRoot();
		}
		return null;
	}

	/**
	 * @return the identifier for this object to be used when displaying the
	 *         element to the user
	 */
	public abstract String getName();

	/**
	 * Get the concrete type of this object, must be one of the TYPE constants
	 * defined in ICtxHelpConstants.
	 *
	 * @see ICtxHelpConstants
	 */
	public abstract int getType();

	/**
	 * @return the parent of this object, or <code>null</code> if there is no
	 *         parent.
	 */
	public CtxHelpObject getParent() {
		IDocumentElementNode parent = getParentNode();
		return parent instanceof CtxHelpObject ? (CtxHelpObject) parent : null;
	}

	/**
	 * Check if the object is a direct or indirect descendant of the object
	 * parameter.
	 *
	 * @param obj
	 *            The object to find in this object's ancestry
	 * @return true iff obj is an ancestor of this object
	 */
	public boolean descendsFrom(CtxHelpObject obj) {
		if (this.equals(obj)) {
			return true;
		}
		if (getParent() != null && obj.canBeParent()) {
			return getParent().descendsFrom(obj);
		}
		return false;
	}

	/**
	 * @param ctxHelpObject
	 *            the child used to locate a sibling
	 * @return the object preceding the specified one in the list of children
	 */
	public CtxHelpObject getPreviousSibling(CtxHelpObject ctxHelpObject) {
		return (CtxHelpObject) getPreviousSibling(ctxHelpObject,
				CtxHelpObject.class);
	}

	/**
	 * @param ctxHelpObject
	 *            the child used to locate a sibling
	 * @return the object proceeding the specified one in the list of children
	 */
	public CtxHelpObject getNextSibling(CtxHelpObject ctxHelpObject) {
		return (CtxHelpObject) getNextSibling(ctxHelpObject,
				CtxHelpObject.class);
	}

	/**
	 * @return true iff a this object can be removed
	 */
	public boolean canBeRemoved() {
		if (getType() == TYPE_ROOT) { // Semantic Rule: The root element can
										// never be removed
			return false;
		}
		return true;
	}

	public void removeChild(CtxHelpObject object) {
		if (object.canBeRemoved()) {
			removeChildNode(object, true);
		}
	}

	public void moveChild(CtxHelpObject object, int newRelativeIndex) {
		moveChildNode(object, newRelativeIndex, true);
	}

}
