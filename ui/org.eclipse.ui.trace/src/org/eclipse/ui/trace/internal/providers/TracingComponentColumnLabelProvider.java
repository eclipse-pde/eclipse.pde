/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.ui.trace.internal.providers;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.ui.trace.internal.utils.TracingConstants;

/**
 * A label provide object for the tracing UI viewers columns.
 */
public class TracingComponentColumnLabelProvider extends ColumnLabelProvider {

	/**
	 * Construct a new {@link TracingComponentColumnLabelProvider} for the specified index.
	 *
	 * @param index
	 *            The column index. One of either {@link TracingConstants#LABEL_COLUMN_INDEX} for the label column or
	 *            {@link TracingConstants#VALUE_COLUMN_INDEX} for the value column.
	 */
	public TracingComponentColumnLabelProvider(final int index) {

		super();
		this.columnIndex = index;
	}

	@Override
	public String getText(final Object element) {

		return TracingComponentLabelProvider.getLabel(this.columnIndex, element);
	}

	/**
	 * The column index. One of {@link TracingConstants#LABEL_COLUMN_INDEX} or
	 * {@link TracingConstants#VALUE_COLUMN_INDEX}
	 */
	private int columnIndex;
}