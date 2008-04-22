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
package org.eclipse.pde.internal.ds.ui.editor;


import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class DSImplementationDetails extends DSAbstractDetails {
	
	private IDSImplementation fImplementation;

	private Section fMainSection;

	private FormEntry fTitle;


	public DSImplementationDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
		fImplementation = null;
		fMainSection = null;
		fTitle = null;

	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText("Definition");
		fMainSection
				.setDescription("Specify the service's implementation class:");

		fMainSection.setLayout(FormLayoutFactory
				.createClearGridLayout(false, 1));

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
		fTitle = new FormEntry(mainSectionClient, getToolkit(), "Title*:",
				SWT.NONE);
		
		// Bind widgets
		 getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);


		
	}

	public void hookListeners() {
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fImplementation == null) {
					return;
				}
				fImplementation.setClassName(fTitle.getValue());
			}
		});
	}

	// }

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fImplementation == null) {
			return;
		}

		if (fImplementation.getClassName() == null) {
			return;
		}
		// Attribute: title
		fTitle.setValue(fImplementation.getClassName(), true);
		fTitle.setEditable(editable);


		
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSImplementation) == false) {
			return;
		}
		// Set data
		setData((IDSImplementation) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSImplementation object) {
		// Set data
		fImplementation = object;
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fTitle.commit();
	}
}
