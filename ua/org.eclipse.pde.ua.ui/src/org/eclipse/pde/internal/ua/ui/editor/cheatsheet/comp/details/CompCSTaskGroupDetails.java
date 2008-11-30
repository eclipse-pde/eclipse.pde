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

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.CompCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * CompCSTaskGroupDetails
 *
 */
public class CompCSTaskGroupDetails extends CSAbstractDetails {

	private Section fDefinitionSection;

	private FormEntry fNameEntry;

	private ComboPart fKindCombo;

	private Button fSkip;

	private ICompCSTaskGroup fDataTaskGroup;

	private CompCSEnclosingTextDetails fEnclosingTextSection;

	private static final String F_KIND_VALUE_SET = DetailsMessages.CompCSTaskGroupDetails_set;

	private static final String F_KIND_VALUE_CHOICE = DetailsMessages.CompCSTaskGroupDetails_choice;

	private static final String F_KIND_VALUE_SEQUENCE = DetailsMessages.CompCSTaskGroupDetails_sequence;

	/**
	 * @param section
	 */
	public CompCSTaskGroupDetails(ICSMaster section) {
		super(section, CompCSInputContext.CONTEXT_ID);
		fDataTaskGroup = null;

		fNameEntry = null;
		fKindCombo = null;
		fSkip = null;

		fDefinitionSection = null;
		fEnclosingTextSection = new CompCSEnclosingTextDetails(ICompCSConstants.TYPE_TASKGROUP, section);
	}

	/**
	 * @param object
	 */
	public void setData(ICompCSTaskGroup object) {
		// Set data
		fDataTaskGroup = object;
		// Set data on the enclosing text section
		fEnclosingTextSection.setData(object);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is 
		// registered directly.
		// Initialize managed form for enclosing text section
		fEnclosingTextSection.initialize(form);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		// Create the main section
		int style = Section.DESCRIPTION | ExpandableComposite.TITLE_BAR;
		fDefinitionSection = getPage().createUISection(parent, DetailsMessages.CompCSTaskGroupDetails_sectionTitle, DetailsMessages.CompCSTaskGroupDetails_sectionDescription, style);
		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fDefinitionSection);
		// Create the container for the main section
		Composite sectionClient = getPage().createUISectionContainer(fDefinitionSection, 2);
		// Create the name entry
		createUINameEntry(sectionClient);
		// Create the kind label
		createUIKindLabel(sectionClient);
		// Create the kind combo
		createUIKindCombo(sectionClient);
		// Create the skip button
		createUISkipButton(sectionClient);
		// Create the enclosing text section
		fEnclosingTextSection.createDetails(parent);
		// Bind widgets
		getManagedForm().getToolkit().paintBordersFor(sectionClient);
		fDefinitionSection.setClient(sectionClient);
		markDetailsPart(fDefinitionSection);

	}

	/**
	 * @param parent
	 */
	private void createUISkipButton(Composite parent) {
		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		fSkip = getToolkit().createButton(parent, DetailsMessages.CompCSTaskGroupDetails_optional, SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
	}

	/**
	 * @param parent
	 */
	private void createUIKindLabel(Composite parent) {
		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		Label label = getToolkit().createLabel(parent, DetailsMessages.CompCSTaskGroupDetails_type, SWT.WRAP);
		label.setForeground(foreground);
		label.setToolTipText(DetailsMessages.CompCSTaskGroupDetails_tooltip1);
	}

	/**
	 * @param parent
	 */
	private void createUIKindCombo(Composite parent) {
		fKindCombo = new ComboPart();
		fKindCombo.createControl(parent, getToolkit(), SWT.READ_ONLY);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		// Needed to align vertically with form entry field and allow space
		// for a possible field decoration
		data.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fKindCombo.getControl().setLayoutData(data);
		fKindCombo.add(F_KIND_VALUE_SET);
		fKindCombo.add(F_KIND_VALUE_SEQUENCE);
		fKindCombo.add(F_KIND_VALUE_CHOICE);
		fKindCombo.setText(F_KIND_VALUE_SET);
		fKindCombo.getControl().setToolTipText(DetailsMessages.CompCSTaskGroupDetails_tooltip2);
	}

	/**
	 * @param parent
	 */
	private void createUINameEntry(Composite parent) {
		fNameEntry = new FormEntry(parent, getManagedForm().getToolkit(), DetailsMessages.CompCSTaskGroupDetails_name, SWT.NONE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// Create listeners for the name entry
		createListenersNameEntry();
		// Create listeners for the kind combo
		createListenersKindCombo();
		// Create listeners for the skip button
		createListenersSkipButton();
		// Create listeners within the enclosing text section
		fEnclosingTextSection.hookListeners();
	}

	/**
	 * 
	 */
	private void createListenersNameEntry() {
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fDataTaskGroup == null) {
					return;
				}
				fDataTaskGroup.setFieldName(fNameEntry.getValue());
			}
		});
	}

	/**
	 * 
	 */
	private void createListenersKindCombo() {
		fKindCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fDataTaskGroup == null) {
					return;
				}
				String selection = fKindCombo.getSelection();
				if (selection.equals(F_KIND_VALUE_CHOICE)) {
					fDataTaskGroup.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_CHOICE);
				} else if (selection.equals(F_KIND_VALUE_SEQUENCE)) {
					fDataTaskGroup.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_SEQUENCE);
				} else if (selection.equals(F_KIND_VALUE_SET)) {
					fDataTaskGroup.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_SET);
				}
			}
		});
	}

	/**
	 * 
	 */
	private void createListenersSkipButton() {
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fDataTaskGroup == null) {
					return;
				}
				fDataTaskGroup.setFieldSkip(fSkip.getSelection());
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fDataTaskGroup == null) {
			return;
		}
		boolean editable = isEditableElement();
		// Update name entry
		updateNameEntry(editable);
		// Update kind combo
		updateKindCombo(editable);
		// Update skip button
		updateSkipButton(editable);
		// Update fields within enclosing text section
		fEnclosingTextSection.updateFields();
	}

	/**
	 * @param editable
	 */
	private void updateNameEntry(boolean editable) {
		fNameEntry.setValue(fDataTaskGroup.getFieldName(), true);
		fNameEntry.setEditable(editable);
	}

	/**
	 * @param editable
	 */
	private void updateKindCombo(boolean editable) {
		String kind = fDataTaskGroup.getFieldKind();

		if (kind == null) {
			// NO-OP
		} else if (kind.compareTo(ICompCSConstants.ATTRIBUTE_VALUE_SEQUENCE) == 0) {
			fKindCombo.setText(F_KIND_VALUE_SEQUENCE);
		} else if (kind.compareTo(ICompCSConstants.ATTRIBUTE_VALUE_CHOICE) == 0) {
			fKindCombo.setText(F_KIND_VALUE_CHOICE);
		} else {
			fKindCombo.setText(F_KIND_VALUE_SET);
		}

		fKindCombo.setEnabled(editable);
	}

	/**
	 * @param editable
	 */
	private void updateSkipButton(boolean editable) {
		fSkip.setSelection(fDataTaskGroup.getFieldSkip());
		fSkip.setEnabled(editable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fNameEntry.commit();
		// No need to call for sub details, because they contain no form entries
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ICompCSTaskGroup) == false) {
			return;
		}
		// Set data
		setData((ICompCSTaskGroup) object);
		// Update the UI given the new data
		updateFields();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Dispose of the enclosing text section
		if (fEnclosingTextSection != null) {
			fEnclosingTextSection.dispose();
			fEnclosingTextSection = null;
		}
		super.dispose();
	}

}
