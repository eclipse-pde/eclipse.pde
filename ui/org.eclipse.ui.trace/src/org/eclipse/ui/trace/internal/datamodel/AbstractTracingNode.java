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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.utils.TracingConstants;

/**
 * A abstract base class implementation of the {@link TracingNode} interface.
 */
public abstract class AbstractTracingNode implements TracingNode {

	/**
	 * Constructor to create the empty list of children
	 */
	public AbstractTracingNode() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);
		}
		this.children = new ArrayList<TracingNode>();
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * TODO
	 */
	protected abstract void populateChildren();

	public String getLabel() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.label);
		}
		return this.label;
	}

	public TracingNode getParent() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.parent);
		}
		return this.parent;
	}

	public TracingNode[] getChildren() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);
		}
		this.initialize();
		TracingNode[] results = this.children.toArray(new TracingNode[this.children.size()]);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.children);
		}
		return results;
	}

	public boolean hasChildren() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);
		}
		boolean hasChildren = false;
		this.initialize();
		if (this.children != null) {
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "There are no children for this node: " + this); //$NON-NLS-1$
			}
			hasChildren = this.children.size() > 0;
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, String.valueOf(hasChildren));
		}
		return hasChildren;
	}

	public void addChild(final TracingNode childNode) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, childNode);
		}
		if (!this.children.contains(childNode)) {
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, "Adding child node: " + childNode); //$NON-NLS-1$
			}
			this.children.add(childNode);
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Populate the list of children for this node if it has not been initialized yet.
	 */
	public void initialize() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING);
		}
		if (!this.childrenInitialized) {
			if (TracingUIActivator.DEBUG_MODEL) {
				TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, "First time population of the child nodes for '" + this); //$NON-NLS-1$
			}
			this.populateChildren();
			this.childrenInitialized = true;
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	public void setLabel(final String label) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, label);
		}
		this.label = label;
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	public void setParent(final TracingNode parent) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, parent);
		}
		if (this.parent == null) {
			this.parent = parent;
			if (this.parent != null) {
				// since a parent is being set then it should also be added as a child
				if (TracingUIActivator.DEBUG_MODEL) {
					TRACE.trace(TracingConstants.TRACE_MODEL_STRING, "Adding '" + this + "' to the parent node '" + this.parent + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				this.parent.addChild(this);
			}
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/** This nodes parent node */
	protected TracingNode parent = null;

	/** The label for this node */
	protected String label = null;

	/** The list of child nodes for this node */
	protected List<TracingNode> children = null;

	/** A flag to determine if the children have been initialized for this node */
	private boolean childrenInitialized = false;

	/** Trace object for this bundle */
	protected final static DebugTrace TRACE = TracingUIActivator.getDefault().getTrace();
}