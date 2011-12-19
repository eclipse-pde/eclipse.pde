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

import org.eclipse.ui.trace.internal.TracingUIActivator;
import org.eclipse.ui.trace.internal.utils.*;

/**
 * A debug option tracing component is a tree node that contains the option-path and value for a single debug option. A
 * debug option can have a {@link TracingComponent} or another {@link TracingComponentDebugOption} as a parent.
 */
public class TracingComponentDebugOption extends AbstractTracingNode {

	/**
	 * Construct a new {@link TracingComponentDebugOption} that does not have a parent node set. A parent node is
	 * required for all {@link TracingComponentDebugOption} instances but can be set at a later time via
	 * {@link TracingComponentDebugOption#setParent(TracingNode)}.
	 * 
	 * @param path
	 *            A non-null path for this debug option
	 * @param value
	 *            A non-null value for this debug option
	 */
	public TracingComponentDebugOption(final String path, final String value) {

		this(null, path, value);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Constructor for a new {@link TracingComponentDebugOption} for a specific parent node.
	 * 
	 * @param parentNode
	 *            The parent {@link TracingNode} for this {@link TracingComponentDebugOption}
	 * @param path
	 *            A non-null path for this debug option
	 * @param value
	 *            A non-null value for this debug option
	 */
	public TracingComponentDebugOption(final TracingNode parentNode, final String path, final String value) {

		super();
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, new Object[] {parentNode, path, value});
		}
		assert (path != null);
		assert (value != null);
		this.fOptionPath = path;
		this.fOptionPathValue = value;
		this.setParent(parentNode);
		this.setLabel(path);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	@Override
	public String toString() {

		final StringBuilder builder = new StringBuilder();
		builder.append("TracingComponentDebugOption [fOptionPath="); //$NON-NLS-1$
		builder.append(this.fOptionPath);
		builder.append(", fOptionPathValue="); //$NON-NLS-1$
		builder.append(this.fOptionPathValue);
		builder.append(", parent="); //$NON-NLS-1$
		if (this.getParent() != null) {
			builder.append(this.getParent());
		} else {
			builder.append("<unset>"); //$NON-NLS-1$
		}
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.fOptionPath == null) ? 0 : this.fOptionPath.hashCode());
		result = prime * result + ((this.getParent() == null) ? 0 : this.getParent().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {

		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TracingComponentDebugOption)) {
			return false;
		}
		TracingComponentDebugOption other = (TracingComponentDebugOption) obj;
		if (this.fOptionPath == null) {
			if (other.fOptionPath != null) {
				return false;
			}
		} else if (!this.fOptionPath.equals(other.fOptionPath)) {
			return false;
		}
		if (this.getParent() == null) {
			if (other.getParent() != null) {
				return false;
			}
		} else if (!this.getParent().equals(other.getParent())) {
			return false;
		}
		return true;
	}

	public boolean isEnabled() {

		boolean isEnabled = false;
		if (TracingUtils.isValueBoolean(this.fOptionPathValue)) {
			isEnabled = Boolean.parseBoolean(this.fOptionPathValue);
		} else {
			// a non-boolean debug option - enable it only if it exists in the DebugOptions
			String value = DebugOptionsHandler.getDebugOptions().getOption(this.fOptionPath);
			isEnabled = (value != null);
		}
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, String.valueOf(isEnabled));
		}
		return isEnabled;
	}

	@Override
	protected void populateChildren() {

		// empty implementation - all work is done in TracingComponent#populateChildren()
	}

	/**
	 * A {@link TracingComponentDebugOption} has no children
	 */
	@Override
	public final TracingComponentDebugOption[] getChildren() {
		return new TracingComponentDebugOption[0];
	}

	/**
	 * Accessor to the debug option path (i.e. bundle/option-path) of this {@link TracingComponentDebugOption}
	 * 
	 * @return the debug option path (i.e. bundle/option-path) of this {@link TracingComponentDebugOption}
	 */
	public final String getOptionPath() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.fOptionPath);
		}
		return this.fOptionPath;
	}

	/**
	 * Accessor to the debug option value of this {@link TracingComponentDebugOption}
	 * 
	 * @return the debug option value of this {@link TracingComponentDebugOption}
	 */
	public final String getOptionPathValue() {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING, this.fOptionPathValue);
		}
		return this.fOptionPathValue;
	}

	/**
	 * Set the new option-path value
	 * 
	 * @param newValue
	 *            A non-null new {@link String} value of the option-path
	 */
	public final void setOptionPathValue(final String newValue) {

		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, newValue);
		}
		assert (newValue != null);
		this.fOptionPathValue = newValue;
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	/**
	 * Set the new option-path value to the specified boolean value
	 * 
	 * @param newValue
	 *            A new boolean value of the option-path
	 */
	public final void setOptionPathValue(final boolean newValue) {

		String valueAsString = Boolean.toString(newValue);
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceEntry(TracingConstants.TRACE_MODEL_STRING, valueAsString);
		}
		this.fOptionPathValue = valueAsString;
		if (TracingUIActivator.DEBUG_MODEL) {
			TRACE.traceExit(TracingConstants.TRACE_MODEL_STRING);
		}
	}

	@Override
	public TracingComponentDebugOption clone() {
		return new TracingComponentDebugOption(getParent(), fOptionPath, fOptionPathValue);
	}

	/**
	 * The option-path - this value cannot change
	 */
	private final String fOptionPath;

	/**
	 * The value of the option-path - this value can change
	 */
	private String fOptionPathValue;

}