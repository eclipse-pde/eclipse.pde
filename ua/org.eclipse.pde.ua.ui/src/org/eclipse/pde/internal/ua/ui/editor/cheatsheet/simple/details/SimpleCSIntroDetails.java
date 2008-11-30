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

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.PDESourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SimpleCSIntroDetails
 */
public class SimpleCSIntroDetails extends CSAbstractDetails {

	private ISimpleCSIntro fIntro;

	private PDESourceViewer fContentViewer;

	private Section fMainSection;

	private SimpleCSHelpDetails fHelpSection;

	private boolean fBlockEvents;

	/**
	 * @param elementSection
	 */
	public SimpleCSIntroDetails(ICSMaster elementSection) {
		super(elementSection, SimpleCSInputContext.CONTEXT_ID);
		fIntro = null;

		fContentViewer = null;
		fMainSection = null;
		fHelpSection = new SimpleCSHelpDetails(elementSection);
		fBlockEvents = false;
	}

	/**
	 * @param object
	 */
	public void setData(ISimpleCSIntro object) {
		// Set data
		fIntro = object;
		// Set data on help section
		fHelpSection.setData(object);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof ISimpleCSIntro) == false) {
			return;
		}
		// Set data
		setData((ISimpleCSIntro) object);
		// Update the UI given the new data
		updateFields();
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		GridData data = null;

		// Create main section
		fMainSection = getToolkit().createSection(parent, Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(SimpleDetailsMessages.SimpleCSIntroDetails_mainSectionText);
		fMainSection.setDescription(SimpleDetailsMessages.SimpleCSIntroDetails_mainSectionDesc);
		fMainSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fMainSection.setLayoutData(data);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		getPage().alignSectionHeaders(getMasterSection().getSection(), fMainSection);

		// Create container for main section
		Composite mainSectionClient = getToolkit().createComposite(fMainSection);
		mainSectionClient.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));

		// description:  Content (Element)
		createUIFieldContent(mainSectionClient);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

		fHelpSection.createDetails(parent);
	}

	/**
	 * @param parent
	 */
	private void createUIFieldContent(Composite parent) {
		GridData data = null;
		// Create the label
		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);
		Label label = getToolkit().createLabel(parent, SimpleDetailsMessages.SimpleCSIntroDetails_attrBody, SWT.WRAP);
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEDetails#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		return fContentViewer.doGlobalAction(actionId);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		// description: Content (Element)
		createUIListenersContentViewer();
		fHelpSection.hookListeners();
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
				if (fIntro == null) {
					return;
				}
				// Get the text from the event
				IDocument document = event.getDocument();
				if (document == null) {
					return;
				}
				// Get the text from the event
				String text = document.get().trim();

				if (fIntro.getDescription() != null) {
					fIntro.getDescription().setContent(text);
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.simple.SimpleCSAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fIntro == null) {
			return;
		}

		fHelpSection.updateFields();

		if (fIntro.getDescription() == null) {
			return;
		}

		// description:  Content (Element)
		fBlockEvents = true;
		fContentViewer.getDocument().set(fIntro.getDescription().getContent());
		fBlockEvents = false;

		boolean editable = isEditableElement();
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		// No need to call for sub details, because they contain no form entries
	}

}
