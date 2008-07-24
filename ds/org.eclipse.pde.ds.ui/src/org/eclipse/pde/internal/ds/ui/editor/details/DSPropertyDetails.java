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

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.ui.IConstants;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ds.ui.parts.ComboPart;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.PDESourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
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

public class DSPropertyDetails extends DSAbstractDetails {

	private IDSProperty fProperty;
	private FormEntry fNameEntry;
	private ComboPart fTypeCombo;
	private FormEntry fValueEntry;
	private PDESourceViewer fContentViewer;
	private Section fMainSection;
	private boolean fBlockEvents;
	private Label fTypeLabel;

	/**
	 * @param section
	 */
	public DSPropertyDetails(IDSMaster section) {
		super(section, DSInputContext.CONTEXT_ID);

	}

	public void createDetails(Composite parent) {

		GridData data = null;

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(Messages.DSPropertyDetails_mainSectionText);
		fMainSection
				.setDescription(Messages.DSPropertyDetails_mainSectionDescription);
		fMainSection.setLayout(FormLayoutFactory
				.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		alignSectionHeaders(fMainSection);

		// Create container for main section
		Composite mainSectionClient = getToolkit()
				.createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(false, 2));

		// Attribute: title
		fNameEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSPropertyDetails_nameEntry,
				SWT.NONE);

		// Attribute: value
		fValueEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSPropertyDetails_valueEntry,
				SWT.NONE);

		// Attribute: type
		fTypeLabel = getToolkit().createLabel(mainSectionClient,
				Messages.DSPropertyDetails_typeEntry, SWT.WRAP);
		fTypeCombo = new ComboPart();
		fTypeCombo
				.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);

		String[] itemsCard = new String[] { IConstants.PROPERTY_TYPE_BOOLEAN,
				IConstants.PROPERTY_TYPE_BYTE, IConstants.PROPERTY_TYPE_CHAR,
				IConstants.PROPERTY_TYPE_DOUBLE,
				IConstants.PROPERTY_TYPE_FLOAT,
				IConstants.PROPERTY_TYPE_INTEGER,
				IConstants.PROPERTY_TYPE_LONG, IConstants.PROPERTY_TYPE_SHORT,
				IConstants.PROPERTY_TYPE_STRING };
		fTypeCombo.setItems(itemsCard);
		fTypeCombo.getControl().setLayoutData(data);
		
		// description: Content (Element)
		createUIFieldContent(mainSectionClient);

		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		return fContentViewer.doGlobalAction(actionId);
	}

	/**
	 * @param parent
	 */
	private void createUIFieldContent(Composite parent) {
		// Create the label
		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		Label label = getToolkit().createLabel(parent,
				Messages.DSPropertyDetails_bodyLabel, SWT.WRAP);
		label.setForeground(foreground);
		fContentViewer = new PDESourceViewer(getPage());
		fContentViewer.createUI(parent, 90, 60);
		
		GridData gridData = (GridData) fContentViewer.getViewer().getTextWidget().getLayoutData();
		gridData.horizontalIndent = 4;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// description: Content (Element)
		createUIListenersContentViewer();
		// Attribute: name
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fProperty == null) {
					return;
				}
				fProperty.setPropertyName(fNameEntry.getValue());
			}
		});

		// Attribute: value
		fValueEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fProperty == null) {
					return;
				}
				fProperty.setPropertyValue(fValueEntry.getValue());
			}
		});

		// Attribute: type
		fTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fProperty == null) {
					return;
				}
				fProperty.setPropertyType(fTypeCombo.getSelection());
			}

		});

	}

	/**
	 * 
	 */
	private void createUIListenersContentViewer() {
		fContentViewer.createUIListeners();
		// Create document listener
		fContentViewer.getDocument().addDocumentListener(
				new IDocumentListener() {
					public void documentAboutToBeChanged(DocumentEvent event) {
						// NO-OP
					}

					public void documentChanged(DocumentEvent event) {
						// Check whether to handle this event
						if (fBlockEvents) {
							return;
						}
						// Ensure data object is defined
						if (fProperty == null) {
							return;
						}
						// Get the text from the event
						IDocument document = event.getDocument();
						if (document == null) {
							return;
						}
						// Get the text from the event
						String text = document.get().trim();

						if (text != null) {
							fProperty.setPropertyElemBody(text);
						}
					}
				});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fProperty == null) {
			return;
		}
		// Attribute: name
		if (fProperty.getPropertyName() != null) {
			fNameEntry.setValue(fProperty.getPropertyName(), true);
		} else {
			fNameEntry.setValue(""); //$NON-NLS-1$
		}
		fNameEntry.setEditable(editable);

		// Attribute: value
		if (fProperty.getPropertyValue() != null) {
			fValueEntry.setValue(fProperty.getPropertyValue(), true);
		} else {
			fValueEntry.setValue("", true); //$NON-NLS-1$
		}
		fValueEntry.setEditable(editable);

		// Attribute: type
		if (fProperty.getPropertyType() != null)
			fTypeCombo.setText(fProperty.getPropertyType());

		if (fProperty.getPropertyElemBody() == null) {
			fBlockEvents = true;
			fContentViewer.getDocument().set(""); //$NON-NLS-1$
			fBlockEvents = false;
		} else {
			// description: Content (Element)
			fBlockEvents = true;
			fContentViewer.getDocument().set(fProperty.getPropertyElemBody());
			fBlockEvents = false;
		}
		
		fContentViewer.getViewer().setEditable(editable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		// Set the context menu to null to prevent the editor context menu
		// from being disposed along with the source viewer
		if (fContentViewer != null) {
			fContentViewer.unsetMenu();
			fContentViewer = null;
		}

		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#canPaste(org.eclipse.swt.dnd.Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		return fContentViewer.canPaste();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fNameEntry.commit();
		fValueEntry.commit();
		// No need to call for sub details, because they contain no form entries
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSProperty) == false) {
			return;
		}
		// Set data
		setData((IDSProperty) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSProperty object) {
		// Set data
		fProperty = object;
	}

}
