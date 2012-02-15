/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.datamodel;

import org.eclipse.ui.trace.internal.utils.DebugOptionsHandler;
import org.eclipse.ui.trace.internal.utils.TracingUtils;

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
		assert (path != null);
		assert (value != null);
		fOptionPath = path;
		fOptionPathValue = value;
		setParent(parentNode);
		setLabel(path);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("TracingComponentDebugOption [fOptionPath="); //$NON-NLS-1$
		builder.append(fOptionPath);
		builder.append(", fOptionPathValue="); //$NON-NLS-1$
		builder.append(fOptionPathValue);
		builder.append(", parent="); //$NON-NLS-1$
		if (getParent() != null) {
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
		result = prime * result + ((fOptionPath == null) ? 0 : fOptionPath.hashCode());
		result = prime * result + ((getParent() == null) ? 0 : getParent().hashCode());
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
		if (fOptionPath == null) {
			if (other.fOptionPath != null) {
				return false;
			}
		} else if (!fOptionPath.equals(other.fOptionPath)) {
			return false;
		}
		if (getParent() == null) {
			if (other.getParent() != null) {
				return false;
			}
		} else if (!getParent().equals(other.getParent())) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.trace.internal.datamodel.TracingNode#isEnabled()
	 */
	public boolean isEnabled() {
		boolean isEnabled = false;
		if (TracingUtils.isValueBoolean(fOptionPathValue)) {
			isEnabled = Boolean.parseBoolean(fOptionPathValue);
		} else {
			// a non-boolean debug option - enable it only if it exists in the DebugOptions
			String value = DebugOptionsHandler.getDebugOptions().getOption(this.fOptionPath);
			isEnabled = (value != null);
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
	public String getOptionPath() {
		return fOptionPath;
	}

	/**
	 * Accessor to the debug option value of this {@link TracingComponentDebugOption}
	 * 
	 * @return the debug option value of this {@link TracingComponentDebugOption}
	 */
	public String getOptionPathValue() {
		return fOptionPathValue;
	}

	/**
	 * Set the new option-path value
	 * 
	 * @param newValue
	 *            A non-null new {@link String} value of the option-path
	 */
	public void setOptionPathValue(final String newValue) {
		assert (newValue != null);
		fOptionPathValue = newValue;
	}

	/**
	 * Set the new option-path value to the specified boolean value
	 * 
	 * @param newValue
	 *            A new boolean value of the option-path
	 */
	public void setOptionPathValue(final boolean newValue) {
		fOptionPathValue = Boolean.toString(newValue);
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