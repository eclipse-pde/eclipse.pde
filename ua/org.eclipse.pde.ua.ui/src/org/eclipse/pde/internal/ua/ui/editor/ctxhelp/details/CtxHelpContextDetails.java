/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpContext;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpInputContext;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpTreeSection;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

/**
 * Details section for context entries.
 * @since 3.4
 * @see CtxHelpAbstractDetails
 * @see CtxHelpContext
 */
public class CtxHelpContextDetails extends CtxHelpAbstractDetails {

	private CtxHelpContext fContext;
	private FormEntry fIdEntry;
	private FormEntry fTitleEntry;
	private FormEntry fDescEntry;

	public CtxHelpContextDetails(CtxHelpTreeSection masterSection) {
		super(masterSection, CtxHelpInputContext.CONTEXT_ID);
	}

	@Override
	public void createFields(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpContextDetails_idDesc);
		fIdEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpContextDetails_idLabel, SWT.NONE);
		createSpace(parent);
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpContextDetails_descDesc);
		fDescEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpContextDetails_descText, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 50;
		fDescEntry.getText().setLayoutData(data);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fDescEntry.getLabel().setLayoutData(data);
		createSpace(parent);
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpContextDetails_titleDesc);
		fTitleEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpContextDetails_titleTitle, SWT.NONE);
	}

	@Override
	protected String getDetailsTitle() {
		return CtxHelpDetailsMessages.CtxHelpContextDetails_title;
	}

	@Override
	protected String getDetailsDescription() {
		return null;
	}

	@Override
	public void hookListeners() {
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				if (fContext != null) {
					fContext.setID(fIdEntry.getValue());
				}
			}
		});
		fDescEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				if (fContext != null) {
					if (fDescEntry.getValue().trim().length() > 0) {
						fContext.setDescription(fDescEntry.getValue());
					} else {
						// Pass null to delete the description node because it is empty
						fContext.setDescription(null);
					}
				}
			}
		});
		fTitleEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) { // Ensure data object is defined
				if (fContext != null) {
					fContext.setTitle(fTitleEntry.getValue());
				}
			}
		});
	}

	@Override
	public void updateFields() {
		if (fContext != null) {
			fIdEntry.setValue(fContext.getId(), true);
			fIdEntry.setEditable(isEditableElement());
			fDescEntry.setValue(fContext.getDescription(), true);
			fDescEntry.setEditable(isEditableElement());
			fTitleEntry.setValue(fContext.getTitle(), true);
			fTitleEntry.setEditable(isEditableElement());
		}
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fIdEntry.commit();
		fDescEntry.commit();
		fTitleEntry.commit();

	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object object = getFirstSelectedObject(selection);
		if (object instanceof CtxHelpContext) {
			fContext = (CtxHelpContext) object;
			updateFields();
		}
	}
}
