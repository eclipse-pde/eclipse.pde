/*******************************************************************************
 * Copyright (c)  Lacherp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lacherp - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.adapter.viewer;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.pde.spy.adapter.model.AdapterData;
import org.eclipse.swt.SWT;

public class AdapterDataComparator extends ViewerComparator {

	private int columnIndex;
	private int direction;

	public AdapterDataComparator(int columnIndex) {
		this.columnIndex = columnIndex;
		direction = SWT.UP;
	}

	/** Called when click on table header, reverse order */
	public void setColumn(int column) {
		if (column == columnIndex) {
			// Same column as last sort; toggle the direction
			direction = (direction == SWT.UP) ? SWT.DOWN : SWT.UP;
		} else {
			// New column; do a descending sort
			columnIndex = column;
			direction = SWT.DOWN;
		}
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		// Now can compare the text from label provider.
		if (e1 instanceof AdapterData && e2 instanceof AdapterData) {
			int rc = ((AdapterData) e1).compareTo((AdapterData) e2);
			// If descending order, flip the direction
			return (direction == SWT.DOWN) ? -rc : rc;
		}
		return -1;
	}
	
	public int getDirection() {
		return direction;
	}
}
