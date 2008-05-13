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
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.PDESourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
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
	private FormEntry fTypeEntry;
	private FormEntry fValueEntry;
	private PDESourceViewer fContentViewer;
	private Section fMainSection;
	private boolean fBlockEvents;

	/**
	 * @param section
	 */
	public DSPropertyDetails(IDSMaster section) {
		super(section, DSInputContext.CONTEXT_ID);

	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
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
		getPage().alignSectionHeaders(getMasterSection().getSection(),
				fMainSection);

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
		fTypeEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSPropertyDetails_typeEntry, SWT.NONE);

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
		GridData data = null;
		// Create the label
		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		Label label = getToolkit().createLabel(parent,
				Messages.DSPropertyDetails_bodyLabel, SWT.WRAP);
		label.setForeground(foreground);
		int style = GridData.VERTICAL_ALIGN_BEGINNING
				| GridData.HORIZONTAL_ALIGN_END;
		data = new GridData(style);
		label.setLayoutData(data);
		// Create the source viewer
		fContentViewer = new PDESourceViewer(getPage());
		fContentViewer.createUI(parent, 90, 60);
		// Needed to align vertically with form entry field and allow space
		// for a possible field decoration
		((GridData) fContentViewer.getViewer().getTextWidget().getLayoutData()).horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
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
		fTypeEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fProperty == null) {
					return;
				}
				fProperty.setPropertyType(fTypeEntry.getValue());
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

						if (fProperty.getPropertyElemBody() != null) {
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
		if (fProperty.getPropertyType() != null) {
			fTypeEntry.setValue(fProperty.getPropertyType(), true);
		} else {
			fTypeEntry.setValue("", true); //$NON-NLS-1$
		}
		fTypeEntry.setEditable(editable);

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
		fTypeEntry.commit();
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
