/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.CompCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class CompCSDetails extends CSAbstractDetails {

	private ICompCS fDataCheatSheet;

	private Section fMainSection;

	private FormEntry fNameEntry;

	public CompCSDetails(ICSMaster masterSection) {
		super(masterSection, CompCSInputContext.CONTEXT_ID);
		fDataCheatSheet = null;

		fNameEntry = null;
		fMainSection = null;
	}

	public void setData(ICompCS object) {
		// Set data
		fDataCheatSheet = object;
	}

	@Override
	public void createDetails(Composite parent) {
		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fMainSection = getPage().createUISection(parent, DetailsMessages.CompCSDetails_sectionTitle, DetailsMessages.CompCSDetails_sectionDescription, style);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fMainSection);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(fMainSection, 2);
		// Create the name widget
		createUINameEntry(sectionClient);
		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fMainSection.setClient(sectionClient);
		markDetailsPart(fMainSection);
	}

	private void createUINameEntry(Composite parent) {
		fNameEntry = new FormEntry(parent, getManagedForm().getToolkit(), DetailsMessages.CompCSDetails_name, SWT.NONE);
	}

	@Override
	public void hookListeners() {
		// Create the listeners for the name entry
		createListenersNameEntry();
	}

	private void createListenersNameEntry() {
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fDataCheatSheet == null) {
					return;
				}
				fDataCheatSheet.setFieldName(fNameEntry.getValue());
			}
		});
	}

	@Override
	public void updateFields() {
		// Ensure data object is defined
		if (fDataCheatSheet == null) {
			return;
		}
		// Update name entry
		updateNameEntry(isEditableElement());
	}

	private void updateNameEntry(boolean editable) {
		fNameEntry.setValue(fDataCheatSheet.getFieldName(), true);
		fNameEntry.setEditable(editable);
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fNameEntry.commit();
		// No need to call for sub details, because they contain no form entries
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ICompCS) == false) {
			return;
		}
		// Set data
		setData((ICompCS) object);
		// Update the UI given the new data
		updateFields();
	}

}
