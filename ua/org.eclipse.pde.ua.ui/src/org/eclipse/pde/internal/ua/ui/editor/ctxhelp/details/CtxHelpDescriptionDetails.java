/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpDescription;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpInputContext;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpTreeSection;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

/**
 * Details section for description entries.
 * @since 3.4
 * @see CtxHelpAbstractDetails
 * @see CtxHelpDescription
 */
public class CtxHelpDescriptionDetails extends CtxHelpAbstractDetails {

	private CtxHelpDescription fDescription;
	private FormEntry fDescEntry;

	public CtxHelpDescriptionDetails(CtxHelpTreeSection masterSection) {
		super(masterSection, CtxHelpInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#createFields(org.eclipse.swt.widgets.Composite)
	 */
	public void createFields(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpDescriptionDetails_descDesc);
		fDescEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpDescriptionDetails_descLabel, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 50;
		fDescEntry.getText().setLayoutData(data);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fDescEntry.getLabel().setLayoutData(data);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#getDetailsTitle()
	 */
	protected String getDetailsTitle() {
		return CtxHelpDetailsMessages.CtxHelpDescriptionDetails_title;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#getDetailsDescription()
	 */
	protected String getDetailsDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		fDescEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fDescription != null) {
					fDescription.setDescription(fDescEntry.getValue());
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#updateFields()
	 */
	public void updateFields() {
		if (fDescription != null) {
			fDescEntry.setValue(fDescription.getDescription(), true);
			fDescEntry.setEditable(isEditableElement());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fDescEntry.commit();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object object = getFirstSelectedObject(selection);
		if (object instanceof CtxHelpDescription) {
			fDescription = (CtxHelpDescription) object;
			updateFields();
		}
	}
}
