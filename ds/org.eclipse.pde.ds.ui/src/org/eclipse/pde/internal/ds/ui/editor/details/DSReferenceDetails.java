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
import org.eclipse.pde.internal.ds.core.IDSReference;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class DSReferenceDetails extends DSAbstractDetails {

	private IDSReference fReference;

	private Section fMainSection;

	private FormEntry fName;

	private FormEntry fInterface;

	private FormEntry fBind;

	private FormEntry fUnBind;

	private FormEntry fTarget;

	private ComboPart fCardinality;

	private Label fLabelCardinality;

	private ComboPart fPolicy;

	private Label fLabelPolicy;

	public DSReferenceDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
		fReference = null;
		fMainSection = null;
		fName = null;
		fInterface = null;
		fPolicy = null;
		fLabelCardinality = null;
		fLabelPolicy = null;
		fTarget = null;
		fBind = null;
		fUnBind = null;

	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText("Definition");
		fMainSection
				.setDescription("Specify the reference's component attributes:");

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

		// Attribute: Interface
		fInterface = new FormEntry(mainSectionClient, getToolkit(),
				"Interface*:", SWT.NONE);

		// Attribute: Cardinality
		fLabelCardinality = getToolkit().createLabel(mainSectionClient,
				"Cardinality:", SWT.WRAP);
		fLabelCardinality.setForeground(foreground);
		fCardinality = new ComboPart();
		fCardinality.createControl(mainSectionClient, getToolkit(),
				SWT.READ_ONLY);

		String[] itemsCard = new String[] { "0..1", "0..n", "1..1", "1..n" };
		fCardinality.setItems(itemsCard);

		// Attribute: Target
		fTarget = new FormEntry(mainSectionClient, getToolkit(), "Target:",
				SWT.NONE);

		// Attribute: Bind
		fBind = new FormEntry(mainSectionClient, getToolkit(), "Bind:",
				SWT.NONE);

		// Attribute: UnBind
		fUnBind = new FormEntry(mainSectionClient, getToolkit(), "Unbind:",
				SWT.NONE);

		// Attribute: Policy
		fLabelPolicy = getToolkit().createLabel(mainSectionClient, "Policy:",
				SWT.WRAP);
		fLabelPolicy.setForeground(foreground);
		fPolicy = new ComboPart();
		fPolicy.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);
		String[] itemsPolicy = new String[] { "static", "dynamic" };
		fPolicy.setItems(itemsPolicy);

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
				if (fReference == null) {
					return;
				}
				fReference.setReferenceName(fName.getValue());
			}
		});

		// Attribute: Interface
		fInterface.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceInterface(fInterface.getValue());
			}
		});

		// Attribute: Target
		fTarget.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceTarget(fTarget.getValue());
			}
		});

		// Attribute: Bind
		fBind.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceBind(fBind.getValue());
			}
		});

		// Attribute: UnBind
		fUnBind.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceUnbind(fUnBind.getValue());
			}
		});

		// Attribute: Cardinality
		fCardinality.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}

				fReference.setReferenceCardinality(fCardinality.getSelection());
			}

		});

		// Attribute: Policy
		fPolicy.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferencePolicy(fPolicy.getSelection());
			}

		});

	}

	// }

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fReference == null) {
			return;
		}

		if (fReference.getReferenceName() == null) {
			fName.setValue("");
		} else {
			// Attribute: name
			fName.setValue(fReference.getReferenceName(), true);
		}
		fName.setEditable(editable);

		if (fReference.getReferenceInterface() == null) {
			fInterface.setValue("");
		} else {
			// Attribute: Interface
			fInterface.setValue(fReference.getReferenceInterface(), true);
		}
		fInterface.setEditable(editable);

		// Attribute: Target
		fTarget.setValue(fReference.getReferenceTarget(), true);
		fTarget.setEditable(editable);

		// Attribute: Bind
		fBind.setValue(fReference.getReferenceBind(), true);
		fBind.setEditable(editable);

		// Attribute: Unbind
		fUnBind.setValue(fReference.getReferenceUnbind(), true);
		fUnBind.setEditable(editable);

		// Attribute: Cardinality
		if(fReference.getReferenceCardinality() != null)
			fCardinality.setText(fReference.getReferenceCardinality());

		// Attribute: Policy
		if (fReference.getReferencePolicy() != null)
			fPolicy.setText(fReference.getReferencePolicy());

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSReference) == false) {
			return;
		}
		// Set data
		setData((IDSReference) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSReference object) {
		// Set data
		fReference = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fName.commit();
		fInterface.commit();
		fBind.commit();
		fUnBind.commit();
		fTarget.commit();
	}
}