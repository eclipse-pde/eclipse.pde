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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSSubItem;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class SimpleCSSubItemDetails extends CSAbstractDetails {

	private ISimpleCSSubItem fSubItem;

	private FormEntry fLabel;

	private Button fSkip;

	private Section fMainSection;

	private SimpleCSCommandDetails fCommandSection;

	public SimpleCSSubItemDetails(ICSMaster masterTreeSection) {
		super(masterTreeSection, SimpleCSInputContext.CONTEXT_ID);
		fSubItem = null;

		fLabel = null;
		fSkip = null;
		fMainSection = null;
		fCommandSection = new SimpleCSCommandDetails(masterTreeSection);
	}

	public void setData(ISimpleCSSubItem object) {
		// Set data
		fSubItem = object;
		// Set data on command section
		fCommandSection.setData(object);
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ISimpleCSSubItem) == false) {
			return;
		}
		// Set data
		setData((ISimpleCSSubItem) object);
		// Update the UI given the new data
		updateFields();
	}

	@Override
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is
		// registered directly.
		// Initialize managed form for command section
		fCommandSection.initialize(form);
	}

	@Override
	public void createDetails(Composite parent) {

		GridData data = null;

		// Create main section
		fMainSection = getToolkit().createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(SimpleDetailsMessages.SimpleCSSubItemDetails_mainSectionText);
		fMainSection.setDescription(SimpleDetailsMessages.SimpleCSSubItemDetails_mainSectionDesc);
		fMainSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fMainSection);

		// Create container for main section
		Composite mainSectionClient = getToolkit().createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));

		// Attribute: label
		fLabel = new FormEntry(mainSectionClient, getToolkit(), SimpleDetailsMessages.SimpleCSSubItemDetails_attrBody, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 50;
		fLabel.getText().setLayoutData(data);
		data = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fLabel.getLabel().setLayoutData(data);

		// Attribute: skip
		fSkip = getToolkit().createButton(mainSectionClient, SimpleDetailsMessages.SimpleCSSubItemDetails_attrSkip, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

		fCommandSection.createDetails(parent);
	}

	@Override
	public void hookListeners() {
		// Attribute: label
		fLabel.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fSubItem == null) {
					return;
				}
				fSubItem.setLabel(fLabel.getValue());
			}
		});
		// Attribute: skip
		fSkip.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fSubItem == null) {
					return;
				}
				fSubItem.setSkip(fSkip.getSelection());
			}
		});

		fCommandSection.hookListeners();
	}

	@Override
	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fSubItem == null) {
			return;
		}
		// Attribute: label
		fLabel.setValue(fSubItem.getLabel(), true);
		fLabel.setEditable(editable);

		// Attribute: skip
		fSkip.setSelection(fSubItem.getSkip());
		fSkip.setEnabled(editable);

		fCommandSection.updateFields();
	}

	@Override
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fLabel.commit();
		// No need to call for sub details, because they contain no form entries
	}

}
