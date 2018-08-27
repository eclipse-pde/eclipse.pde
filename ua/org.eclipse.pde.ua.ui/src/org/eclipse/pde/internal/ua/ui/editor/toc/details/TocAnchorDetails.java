/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.toc.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.toc.text.TocAnchor;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.ui.editor.toc.TocInputContext;
import org.eclipse.pde.internal.ua.ui.editor.toc.TocTreeSection;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

public class TocAnchorDetails extends TocAbstractDetails {

	private TocAnchor fDataTOCAnchor;

	private FormEntry fAnchorIdEntry;

	public TocAnchorDetails(TocTreeSection masterSection) {
		super(masterSection, TocInputContext.CONTEXT_ID);
		fDataTOCAnchor = null;

		fAnchorIdEntry = null;
	}

	public void setData(TocAnchor object) {
		// Set data
		fDataTOCAnchor = object;
	}

	@Override
	protected TocObject getDataObject() {
		return fDataTOCAnchor;
	}

	@Override
	protected FormEntry getPathEntryField() {
		return null;
	}

	@Override
	public void createFields(Composite parent) {
		createAnchorIdWidget(parent);
	}

	private void createAnchorIdWidget(Composite parent) {
		fAnchorIdEntry = new FormEntry(parent, getManagedForm().getToolkit(), TocDetailsMessages.TocAnchorDetails_idText, SWT.NONE);
		// Ensure that the text field has proper width
		fAnchorIdEntry.getText().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}

	@Override
	protected String getDetailsTitle() {
		return TocDetailsMessages.TocAnchorDetails_title;
	}

	@Override
	protected String getDetailsDescription() {
		return TocDetailsMessages.TocAnchorDetails_idDesc;
	}

	@Override
	public void hookListeners() {
		createAnchorIdEntryListeners();
	}

	private void createAnchorIdEntryListeners() {
		fAnchorIdEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fDataTOCAnchor != null) {
					{
						fDataTOCAnchor.setFieldAnchorId(fAnchorIdEntry.getValue());
					}
				}
			}
		});
	}

	@Override
	public void updateFields() {
		// Ensure data object is defined
		if (fDataTOCAnchor != null) { // Update name entry
			updateAnchorIdEntry(isEditableElement());
		}
	}

	private void updateAnchorIdEntry(boolean editable) {
		fAnchorIdEntry.setValue(fDataTOCAnchor.getFieldAnchorId(), true);
		fAnchorIdEntry.setEditable(editable);
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fAnchorIdEntry.commit();
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if (object != null && object instanceof TocAnchor) {
			// Set data
			setData((TocAnchor) object);
			// Update the UI given the new data
			updateFields();
		}
	}
}
