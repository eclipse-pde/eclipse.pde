/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.details;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.parts.PDESourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
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
 * SimpleCSItemDetails
 *
 */
public class SimpleCSItemDetails extends CSAbstractDetails {

	private ISimpleCSItem fItem;

	private FormEntry fTitle;

	private Button fSkip;

	private PDESourceViewer fContentViewer;

	private Section fMainSection;

	private SimpleCSHelpDetails fHelpSection;

	private SimpleCSCommandDetails fCommandSection;

	private ControlDecoration fSkipInfoDecoration;

	private boolean fBlockEvents;

	/**
	 * @param section
	 */
	public SimpleCSItemDetails(ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fItem = null;

		fTitle = null;
		fSkip = null;
		fSkipInfoDecoration = null;
		fContentViewer = null;
		fMainSection = null;
		fBlockEvents = false;

		fHelpSection = new SimpleCSHelpDetails(section);
		fCommandSection = new SimpleCSCommandDetails(section);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		super.initialize(form);
		// Unfortunately this has to be explicitly called for sub detail
		// sections through its main section parent; since, it never is 
		// registered directly.
		// Initialize managed form for help section
		fHelpSection.initialize(form);
		// Initialized managed form for command section
		fCommandSection.initialize(form);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		GridData data = null;

		// Create main section
		fMainSection = getToolkit().createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(SimpleDetailsMessages.SimpleCSItemDetails_mainSectionText);
		fMainSection.setDescription(SimpleDetailsMessages.SimpleCSItemDetails_mainSectionDesc);
		fMainSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fMainSection);

		// Create container for main section
		Composite mainSectionClient = getToolkit().createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));

		// Attribute: title
		fTitle = new FormEntry(mainSectionClient, getToolkit(), SimpleDetailsMessages.SimpleCSItemDetails_attrTitle, SWT.NONE);

		// description: Content (Element)
		createUIFieldContent(mainSectionClient);

		// Attribute: skip
		fSkip = getToolkit().createButton(mainSectionClient, SimpleDetailsMessages.SimpleCSItemDetails_attrSkip, SWT.CHECK);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fSkip.setLayoutData(data);
		fSkip.setForeground(foreground);
		createSkipInfoDecoration();
		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

		fCommandSection.createDetails(parent);

		fHelpSection.createDetails(parent);
	}

	/* (non-Javadoc)
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
		Label label = getToolkit().createLabel(parent, SimpleDetailsMessages.SimpleCSItemDetails_label, SWT.WRAP);
		label.setForeground(foreground);
		int style = GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END;
		data = new GridData(style);
		label.setLayoutData(data);
		// Create the source viewer
		fContentViewer = new PDESourceViewer(getPage());
		fContentViewer.createUI(parent, 90, 60);
		// Needed to align vertically with form entry field and allow space
		// for a possible field decoration			
		((GridData) fContentViewer.getViewer().getTextWidget().getLayoutData()).horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
	}

	/**
	 * 
	 */
	private void createSkipInfoDecoration() {
		// Skip info decoration
		int bits = SWT.TOP | SWT.LEFT;
		fSkipInfoDecoration = new ControlDecoration(fSkip, bits);
		fSkipInfoDecoration.setMarginWidth(1);
		fSkipInfoDecoration.setDescriptionText(SimpleDetailsMessages.SimpleCSItemDetails_disabled);
		updateSkipInfoDecoration(false);
		fSkipInfoDecoration.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// description: Content (Element)
		createUIListenersContentViewer();
		// Attribute: title
		fTitle.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fItem == null) {
					return;
				}
				fItem.setTitle(fTitle.getValue());
			}
		});
		// Attribute: skip
		fSkip.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fItem == null) {
					return;
				}
				fItem.setSkip(fSkip.getSelection());
				getMasterSection().updateButtons();
			}
		});

		fHelpSection.hookListeners();

		fCommandSection.hookListeners();
	}

	/**
	 * 
	 */
	private void createUIListenersContentViewer() {
		fContentViewer.createUIListeners();
		// Create document listener
		fContentViewer.getDocument().addDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
				// NO-OP
			}

			public void documentChanged(DocumentEvent event) {
				// Check whether to handle this event
				if (fBlockEvents) {
					return;
				}
				// Ensure data object is defined
				if (fItem == null) {
					return;
				}
				// Get the text from the event
				IDocument document = event.getDocument();
				if (document == null) {
					return;
				}
				// Get the text from the event
				String text = document.get().trim();

				if (fItem.getDescription() != null) {
					fItem.getDescription().setContent(text);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fItem == null) {
			return;
		}
		// Attribute: title
		fTitle.setValue(fItem.getTitle(), true);
		fTitle.setEditable(editable);

		// Attribute: skip
		fSkip.setSelection(fItem.getSkip());
		updateSkipEnablement();
		// TODO: MP: SimpleCS:  Revist all parameters and check we are simply looking for null - okay for non-String types
		// TODO: MP: SimpleCS:  Reevaluate write methods and make sure not writing empty string

		fHelpSection.updateFields();

		fCommandSection.updateFields();

		if (fItem.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		fBlockEvents = true;
		String content = fItem.getDescription().getContent();
		fContentViewer.getDocument().set(content == null ? "" : content);
		fBlockEvents = false;
		fContentViewer.getViewer().setEditable(editable);
	}

	/* (non-Javadoc)
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#canPaste(org.eclipse.swt.dnd.Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		return fContentViewer.canPaste();
	}

	/**
	 * 
	 */
	private void updateSkipEnablement() {
		// Ensure data object is defined
		if (fItem == null) {
			return;
		}
		boolean editable = isEditableElement();
		// Preserve cheat sheet validity
		// Semantic Rule:  Specifying whether an item can be skipped or not has
		// no effect when subitems are present (because the item delegates the
		// control to the subitem to skip).
		if (fItem.hasSubItems()) {
			editable = false;
			updateSkipInfoDecoration(true);
		} else {
			updateSkipInfoDecoration(false);
		}
		fSkip.setEnabled(editable);
	}

	/**
	 * @param show
	 */
	private void updateSkipInfoDecoration(boolean show) {
		if (show) {
			fSkipInfoDecoration.show();
		} else {
			fSkipInfoDecoration.hide();
		}
		fSkipInfoDecoration.setShowHover(show);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ISimpleCSItem) == false) {
			return;
		}
		// Set data
		setData((ISimpleCSItem) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(ISimpleCSItem object) {
		// Set data
		fItem = object;
		// Set data on commands section
		fCommandSection.setData(object);
		// Set data on help section
		fHelpSection.setData(object);
	}
}
