/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.datamodel;

import org.eclipse.osgi.service.debug.DebugOptions;

/**
 * The 'Product Tracing' page will display the tracing components as a tree. Each element in the tree will be
 * represented by a tracing node.
 */
public interface TracingNode {

	/**
	 * Accessor to the label for this tree node
	 * 
	 * @return Returns the label for this tree node.
	 */
	public String getLabel();

	/**
	 * Accessor for an array of {@link TracingNode} child nodes for this tree node.
	 * 
	 * @return Returns an array of {@link TracingNode} child nodes for this tree node.
	 */
	public TracingNode[] getChildren();

	/**
	 * Accessor for the parent object of this tree node.
	 * 
	 * @return Returns the parent {@link TracingNode} for this tree node or <code>null</code> if this tree node is the
	 *         root of the tree.
	 */
	public TracingNode getParent();

	/**
	 * Setter for the label for this tree node
	 * 
	 * @param newLabel
	 *            The new text for this tree node to display
	 */
	public void setLabel(String newLabel);

	/**
	 * Setter for the parent node of this tree node
	 * 
	 * @param newParent
	 *            The new parent {@link TracingNode} for this tree node.
	 */
	public void setParent(TracingNode newParent);

	/**
	 * Add a new {@link TracingNode} child node to this tree node.
	 * 
	 * @param newNode
	 *            The new {@link TracingNode} node to add as a child to this tree node.
	 */
	public void addChild(TracingNode newNode);

	/**
	 * Does this tree node have any child nodes?
	 * 
	 * @return Returns <code>true</code> if this tree node has child nodes; Otherwise, <code>false</code> is returned.
	 */
	public boolean hasChildren();

	/**
	 * Is the tracing value for this node enabled in the {@link DebugOptions}
	 * 
	 * @return Returns true if this tracing value is set in the {@link DebugOptions}; Otherwise, false is returned.
	 */
	public boolean isEnabled();
}