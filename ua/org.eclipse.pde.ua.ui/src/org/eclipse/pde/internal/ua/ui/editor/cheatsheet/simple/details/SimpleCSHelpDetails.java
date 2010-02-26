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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.ISimpleCSHelpObject;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.CSAbstractSubDetails;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.ICSMaster;
import org.eclipse.pde.internal.ua.ui.editor.cheatsheet.simple.SimpleCSInputContext;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.util.FileExtensionsFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * SimpleCSHelpDetailsSection
 * 
 */
public class SimpleCSHelpDetails extends CSAbstractSubDetails {

	private Text fHelpText;

	private ComboPart fHelpCombo;

	private Label fHelpLabel;

	private Button fHelpBrowse;

	private ISimpleCSHelpObject fHelpObject;

	private Section fHelpSection;

	private boolean fBlockListeners;

	private static final String F_NO_HELP = SimpleDetailsMessages.SimpleCSHelpDetails_none;

	private static final String F_HELP_CONTEXT_ID = SimpleDetailsMessages.SimpleCSHelpDetails_helpContextID;

	private static final String F_HELP_DOCUMENT_LINK = SimpleDetailsMessages.SimpleCSHelpDetails_helpDocumentLink;

	/**
	 * @param section
	 */
	public SimpleCSHelpDetails(ICSMaster section) {
		super(section, SimpleCSInputContext.CONTEXT_ID);
		fHelpObject = null;
		fBlockListeners = false;

		fHelpText = null;
		fHelpCombo = null;
		fHelpLabel = null;
		fHelpBrowse = null;

		fHelpSection = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#setData
	 * (java.lang.Object)
	 */
	public void setData(ISimpleCSHelpObject object) {
		// Set data
		fHelpObject = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails
	 * #createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {

		int columnSpan = 3;
		FormToolkit toolkit = getToolkit();

		GridData data = null;
		Label label = null;
		Color foreground = toolkit.getColors().getColor(IFormColors.TITLE);

		// Create help section
		fHelpSection = toolkit.createSection(parent, Section.DESCRIPTION
				| ExpandableComposite.TITLE_BAR | ExpandableComposite.TWISTIE);
		fHelpSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fHelpSection
				.setText(SimpleDetailsMessages.SimpleCSHelpDetails_helpSectionText);
		fHelpSection
				.setDescription(SimpleDetailsMessages.SimpleCSHelpDetails_helpSectionDesc);
		fHelpSection.setLayout(FormLayoutFactory
				.createClearGridLayout(false, 1));
		data = new GridData(GridData.FILL_HORIZONTAL);
		fHelpSection.setLayoutData(data);

		// Create container for help section
		Composite helpSectionClient = toolkit.createComposite(fHelpSection);
		helpSectionClient.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(false, columnSpan));

		// Attribute: href
		// Attribute: contextId
		label = toolkit.createLabel(helpSectionClient,
				SimpleDetailsMessages.SimpleCSHelpDetails_attrType, SWT.WRAP);
		label.setForeground(foreground);

		// Attribute: href
		// Attribute: contextId
		fHelpCombo = new ComboPart();
		fHelpCombo.createControl(helpSectionClient, toolkit, SWT.READ_ONLY);
		GridData comboData = new GridData(GridData.FILL_HORIZONTAL);
		comboData.horizontalSpan = columnSpan - 1;
		fHelpCombo.getControl().setLayoutData(comboData);
		fHelpCombo.add(F_NO_HELP);
		fHelpCombo.add(F_HELP_CONTEXT_ID);
		fHelpCombo.add(F_HELP_DOCUMENT_LINK);
		fHelpCombo.setText(F_NO_HELP);

		// Attribute: href
		// Attribute: contextId
		fHelpLabel = toolkit.createLabel(helpSectionClient,
				SimpleDetailsMessages.SimpleCSHelpDetails_attrValue, SWT.WRAP);
		fHelpLabel.setForeground(foreground);

		// Attribute: href
		// Attribute: contextId
		fHelpText = toolkit.createText(helpSectionClient, null);
		data = new GridData(GridData.FILL_HORIZONTAL);
		fHelpText.setLayoutData(data);
		// Browse Button
		fHelpBrowse = toolkit.createButton(helpSectionClient,
				SimpleDetailsMessages.SimpleCSHelpDetails_browse, SWT.PUSH);

		// Bind widgets
		toolkit.paintBordersFor(helpSectionClient);
		fHelpSection.setClient(helpSectionClient);
		// Mark as a details part to enable cut, copy, paste, etc.
		markDetailsPart(fHelpSection);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails
	 * #hookListeners()
	 */
	public void hookListeners() {

		// Attribute: href
		// Attribute: contextId
		fHelpCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fHelpObject == null) {
					return;
				}
				String selection = fHelpCombo.getSelection();
				if (selection.equals(F_NO_HELP) == false) {
					// Help was selected
					if (selection.equals(F_HELP_CONTEXT_ID)) {
						// Help context ID was selected, clear the help
						// document link value
						fHelpObject.setHref(null);
						fHelpBrowse.setEnabled(false);
					} else {
						// Help document link was selected, clear the help
						// context ID value
						fHelpObject.setContextId(null);
						fHelpBrowse.setEnabled(true);
					}
					// Make the label and text field visible
					fHelpLabel.setVisible(true);
					fHelpText.setVisible(true);
					fHelpBrowse.setVisible(true);
					// Set the focus on the text field
					fHelpText.setFocus();
					// Clear the previous contents of the text field
					// (Will cause field to become dirty)
					fHelpText.setText(""); //$NON-NLS-1$
					fHelpText.setToolTipText(""); //$NON-NLS-1$
				} else {
					// No help was selected
					// Make the label and text field invisible
					fHelpLabel.setVisible(false);
					fHelpText.setVisible(false);
					fHelpBrowse.setVisible(false);
					// Clear values for help in model
					fHelpObject.setContextId(null);
					fHelpObject.setHref(null);
				}
			}
		});
		// Attribute: href
		// Attribute: contextId
		fHelpText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				// Block UI updates
				if (fBlockListeners) {
					return;
				}
				// Ensure data object is defined
				if (fHelpObject == null) {
					return;
				}
				String selection = fHelpCombo.getSelection();
				if (selection.equals(F_HELP_CONTEXT_ID)) {
					// Help context ID was selected, save the field contents
					// as such
					fHelpObject.setContextId(fHelpText.getText());
				} else {
					// Help document link was selected, save the field contents
					// as such
					fHelpObject.setHref(fHelpText.getText());
				}
				// Update tooltip
				fHelpText.setToolTipText(fHelpText.getText());
			}
		});

		fHelpBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleButtonSelectedEventBrowse(e);
			}
		});
	}

	/**
	 * @param event
	 */
	private void handleButtonSelectedEventBrowse(SelectionEvent event) {
		// Create the dialog
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getManagedForm().getForm().getShell(),
				new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		// Disable multiple selection
		dialog.setAllowMultiple(false);
		// Title
		dialog.setTitle(SimpleDetailsMessages.SimpleCSHelpDetails_dialogTitle);
		// Message
		dialog.setMessage(SimpleDetailsMessages.SimpleCSHelpDetails_dialogMessage);
		// Add valid file extensions to filter by
		FileExtensionsFilter filter = new FileExtensionsFilter();
		filter.addFileExtension("htm"); //$NON-NLS-1$
		filter.addFileExtension("html"); //$NON-NLS-1$
		filter.addFileExtension("shtml"); //$NON-NLS-1$
		filter.addFileExtension("xhtml"); //$NON-NLS-1$
		dialog.addFilter(filter);
		// Set the input as all workspace projects
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		dialog.setInput(root);
		// Set the initial selection using the existing path (if any)
		Path path = new Path(fHelpText.getText());
		// Path must be non-empty, absolute and have at least two segments
		if ((path.isEmpty() == false) && path.isAbsolute()
				&& (path.segmentCount() > 1)) {
			IFile helpDocumentFile = root.getFile(path);
			dialog.setInitialSelection(helpDocumentFile);
		}
		// Open the dialog
		if (dialog.open() == Window.OK) {
			// Get the selection
			Object result = dialog.getFirstResult();
			// Ensure a file was selected
			if (!(result instanceof IFile)) {
				return;
			}
			IFile file = (IFile) result;
			// Get the absolute path
			String absolutePath = file.getFullPath().toPortableString();
			// Update the field
			fHelpText.setText(absolutePath);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.pde.internal.ui.editor.cheatsheet.simple.details.ISimpleCSDetails
	 * #updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fHelpObject == null) {
			return;
		}

		boolean editable = isEditableElement();
		boolean expanded = false;

		// Block model updates
		fBlockListeners = true;
		// Attribute: contextId
		// Attribute: href
		if (PDETextHelper.isDefined(fHelpObject.getContextId())) {
			fHelpText.setText(fHelpObject.getContextId());
			fHelpText.setToolTipText(fHelpObject.getContextId());
			fHelpCombo.setText(F_HELP_CONTEXT_ID);
			expanded = true;
		} else if (PDETextHelper.isDefined(fHelpObject.getHref())) {
			fHelpText.setText(fHelpObject.getHref());
			fHelpText.setToolTipText(fHelpObject.getHref());
			fHelpCombo.setText(F_HELP_DOCUMENT_LINK);
			expanded = true;
		} else {
			fHelpCombo.setText(F_NO_HELP);
		}
		// Unblock model updates
		fBlockListeners = false;

		fHelpSection.setExpanded(expanded);
		fHelpText.setEnabled(editable);
		fHelpText.setVisible(expanded);
		fHelpLabel.setVisible(expanded);
		fHelpBrowse.setVisible(expanded);
		fHelpCombo.setEnabled(editable);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// NO-OP
		// No form entries
	}

}
