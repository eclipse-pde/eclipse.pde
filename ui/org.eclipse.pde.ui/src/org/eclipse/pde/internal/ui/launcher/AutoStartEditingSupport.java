/*******************************************************************************
 *  Copyright (c) 2019 Julian Honnen
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Julian Honnen <julian.honnen@vector.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.Arrays;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.pde.internal.ui.shared.CachedCheckboxTreeViewer;
import org.eclipse.swt.SWT;

class AutoStartEditingSupport extends EditingSupport {

	private final CachedCheckboxTreeViewer fViewer;
	private final CellEditor fCellEditor;
	private final String[] fItems;

	public AutoStartEditingSupport(CachedCheckboxTreeViewer viewer) {
		super(viewer);
		fViewer = viewer;
		fItems = new String[] { "default", Boolean.toString(true), Boolean.toString(false) }; //$NON-NLS-1$
		fCellEditor = new ComboBoxCellEditor(viewer.getTree(), fItems, SWT.BORDER | SWT.READ_ONLY);
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return fCellEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return fViewer.isCheckedLeafElement(element) && element instanceof IHasAutoStart;
	}

	@Override
	protected Object getValue(Object element) {
		String autoStart = ((IHasAutoStart) element).getAutoStart();
		int index = Arrays.asList(fItems).indexOf(autoStart);
		return (index < 0) ? 0 : index;
	}

	@Override
	protected void setValue(Object element, Object value) {
		int index = (int) value;
		if (index < 0) {
			index = 0;
		}
		((IHasAutoStart) element).setAutoStart(fItems[index]);
		fViewer.update(element, null);
	}

	public interface IHasAutoStart {
		String getAutoStart();

		void setAutoStart(String autoStart);
	}

}
