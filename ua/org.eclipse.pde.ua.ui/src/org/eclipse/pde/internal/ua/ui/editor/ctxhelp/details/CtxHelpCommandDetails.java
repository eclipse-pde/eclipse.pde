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
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpCommand;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpInputContext;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpTreeSection;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;

/**
 * Details section for command entries.
 * @since 3.4
 * @see CtxHelpAbstractDetails
 * @see CtxHelpCommand
 */
public class CtxHelpCommandDetails extends CtxHelpAbstractDetails {

	private CtxHelpCommand fCommand;
	private FormEntry fLabelEntry;
	private FormEntry fSerialEntry;

	public CtxHelpCommandDetails(CtxHelpTreeSection masterSection) {
		super(masterSection, CtxHelpInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#createFields(org.eclipse.swt.widgets.Composite)
	 */
	public void createFields(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_labelDesc);
		fLabelEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_labelText, SWT.NONE);
		createSpace(parent);
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_commandDesc);
		fSerialEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_commandText, SWT.NONE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#getDetailsTitle()
	 */
	protected String getDetailsTitle() {
		return CtxHelpDetailsMessages.CtxHelpCommandDetails_title;
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
		fLabelEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fCommand != null) {
					fCommand.setLabel(fLabelEntry.getValue());
				}
			}
		});
		fSerialEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) { // Ensure data object is defined
				if (fCommand != null) {
					fCommand.setSerialization(fSerialEntry.getValue());
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#updateFields()
	 */
	public void updateFields() {
		if (fCommand != null) {
			fLabelEntry.setValue(fCommand.getLabel(), true);
			fLabelEntry.setEditable(isEditableElement());
			fSerialEntry.setValue(fCommand.getSerialization(), true);
			fSerialEntry.setEditable(isEditableElement());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fLabelEntry.commit();
		fSerialEntry.commit();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object object = getFirstSelectedObject(selection);
		if (object instanceof CtxHelpCommand) {
			fCommand = (CtxHelpCommand) object;
			updateFields();
		}
	}
}
