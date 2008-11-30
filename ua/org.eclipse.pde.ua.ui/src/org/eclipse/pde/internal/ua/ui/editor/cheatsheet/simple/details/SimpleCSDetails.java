/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSDetails
 *
 */
public class SimpleCSDetails extends CSAbstractDetails {

	private ISimpleCS fCheatSheet;

	private FormEntry fTitle;

	private Section fMainSection;

	/**
	 * @param section
	 */
	public SimpleCSDetails(ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fCheatSheet = null;

		fTitle = null;
		fMainSection = null;
	}

	/**
	 * @param object
	 */
	public void setData(ISimpleCS object) {
		// Set data
		fCheatSheet = object;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ISimpleCS) == false) {
			return;
		}
		// Set data
		setData((ISimpleCS) object);
		// Update the UI given the new data
		updateFields();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		FormToolkit toolkit = getManagedForm().getToolkit();
		GridData data = null;

		// Create main section
		fMainSection = toolkit.createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(SimpleDetailsMessages.SimpleCSDetails_mainSetionTitle);
		fMainSection.setDescription(SimpleDetailsMessages.SimpleCSDetails_mainSectionDesc);
		fMainSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fMainSection);

		// Create container for main section
		Composite mainSectionClient = toolkit.createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));

		// Attribute: title
		fTitle = new FormEntry(mainSectionClient, toolkit, SimpleDetailsMessages.SimpleCSDetails_attrTitle, SWT.NONE);

		// Bind widgets
		toolkit.paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fCheatSheet == null) {
					return;
				}
				fCheatSheet.setTitle(fTitle.getValue());
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fCheatSheet == null) {
			return;
		}

		boolean editable = isEditableElement();
		// Attribute: title
		fTitle.setValue(fCheatSheet.getTitle(), true);
		fTitle.setEditable(editable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fTitle.commit();
		// No need to call for sub details, because they contain no form entries
	}

}
