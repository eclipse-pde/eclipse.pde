/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.details;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class DSPropertiesDetails extends DSAbstractDetails {
	private IDSProperties fProperties;

	private Section fMainSection;

	private FormEntry fEntry;

	public DSPropertiesDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
		fProperties = null;
		fMainSection = null;
		fEntry = null;

	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(Messages.DSPropertiesDetails_0);
		fMainSection.setDescription(Messages.DSPropertiesDetails_1);

		fMainSection.setLayout(FormLayoutFactory
				.createClearGridLayout(false, 1));

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(),
				fMainSection);

		// Create container for main section
		Composite mainSectionClient = getToolkit()
				.createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(false, 2));

		// Attribute: title
		fEntry = new FormEntry(mainSectionClient, getToolkit(), Messages.DSPropertiesDetails_2,
				SWT.NONE);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

	}

	public void hookListeners() {
		// Attribute: title
		fEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fProperties == null) {
					return;
				}
				fProperties.setEntry(fEntry.getValue());
			}
		});
	}

	// }

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fProperties == null) {
			return;
		}

		if (fProperties.getEntry() == null) {
			fEntry.setValue("", true); //$NON-NLS-1$
		} else {
			// Attribute: title
			fEntry.setValue(fProperties.getEntry(), true);
		}
		fEntry.setEditable(editable);

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSProperties) == false) {
			return;
		}
		// Set data
		setData((IDSProperties) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSProperties object) {
		// Set data
		fProperties = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fEntry.commit();
	}

}
