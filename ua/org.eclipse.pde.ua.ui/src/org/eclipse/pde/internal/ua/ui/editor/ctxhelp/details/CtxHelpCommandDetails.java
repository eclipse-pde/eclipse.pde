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

	@Override
	public void createFields(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_labelDesc);
		fLabelEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_labelText, SWT.NONE);
		createSpace(parent);
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_commandDesc);
		fSerialEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpCommandDetails_commandText, SWT.NONE);
	}

	@Override
	protected String getDetailsTitle() {
		return CtxHelpDetailsMessages.CtxHelpCommandDetails_title;
	}

	@Override
	protected String getDetailsDescription() {
		return null;
	}

	@Override
	public void hookListeners() {
		fLabelEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				if (fCommand != null) {
					fCommand.setLabel(fLabelEntry.getValue());
				}
			}
		});
		fSerialEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) { // Ensure data object is defined
				if (fCommand != null) {
					fCommand.setSerialization(fSerialEntry.getValue());
				}
			}
		});
	}

	@Override
	public void updateFields() {
		if (fCommand != null) {
			fLabelEntry.setValue(fCommand.getLabel(), true);
			fLabelEntry.setEditable(isEditableElement());
			fSerialEntry.setValue(fCommand.getSerialization(), true);
			fSerialEntry.setEditable(isEditableElement());
		}
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fLabelEntry.commit();
		fSerialEntry.commit();

	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object object = getFirstSelectedObject(selection);
		if (object instanceof CtxHelpCommand) {
			fCommand = (CtxHelpCommand) object;
			updateFields();
		}
	}
}
