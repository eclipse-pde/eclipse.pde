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
import org.eclipse.pde.internal.ds.core.IDSRoot;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class DSComponentDetails extends DSAbstractDetails {

	private IDSRoot fComponent;

	private Section fMainSection;

	private FormEntry fName;

	private FormEntry fFactory;

	private ComboPart fEnabled;

	private Label fLabelEnabled;

	private ComboPart fImmediate;

	private Label fLabelImmediate;

	public DSComponentDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
		fComponent = null;
		fMainSection = null;
		fName = null;

	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText("Definition");
		fMainSection
				.setDescription("Specify the service's component attributes:");

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

		// Attribute: name
		fName = new FormEntry(mainSectionClient, getToolkit(), "Name*:",
				SWT.NONE);

		// Attribute: factory
		fFactory = new FormEntry(mainSectionClient, getToolkit(), "Factory:",
				SWT.NONE);

		// Attribute: Enabled
		fLabelEnabled = getToolkit().createLabel(mainSectionClient, "Enabled:",
				SWT.WRAP);
		fLabelEnabled.setForeground(foreground);
		fEnabled = new ComboPart();
		fEnabled.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);
		Control control = fEnabled.getControl();
		String[] items = new String[] { "true", "false" };
		fEnabled.setItems(items);

		// Attribute: Immediate
		fLabelImmediate = getToolkit().createLabel(mainSectionClient,
				"Immediate:", SWT.WRAP);
		fLabelImmediate.setForeground(foreground);
		fImmediate = new ComboPart();
		fImmediate
				.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);
		fImmediate.setItems(items);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

	}

	public void hookListeners() {
		// Attribute: name
		fName.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setAttributeName(fName.getValue());
			}
		});

		// Attribute: factory
		fFactory.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setFactory(fFactory.getValue());
			}
		});

		// Attribute: Enabled
		fEnabled.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}

				fComponent.setEnabled(fEnabled.getSelectionIndex() == 0);
			}

		});

		// Attribute: Immediate
		fImmediate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setImmediate(fImmediate.getSelectionIndex() == 0);
			}

		});

	}

	// }

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fComponent == null) {
			return;
		}

		if (fComponent.getAttributeName() == null) {
			return;
		}
		// Attribute: name
		fName.setValue(fComponent.getAttributeName(), true);
		fName.setEditable(editable);

		if (fComponent.getFactory() == null) {
			return;
		}
		// Attribute: name
		fFactory.setValue(fComponent.getFactory(), true);
		fFactory.setEditable(editable);

		// Attribute: Enabled
		fEnabled.select(fComponent.getEnabled() ? 0 : 1);

		// Attribute: Immediate
		fImmediate.select(fComponent.getImmediate() ? 0 : 1);

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSRoot) == false) {
			return;
		}
		// Set data
		setData((IDSRoot) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSRoot object) {
		// Set data
		fComponent = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fName.commit();
		fFactory.commit();
	}
}