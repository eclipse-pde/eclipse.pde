/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc.details;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ua.core.toc.text.TocLink;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.ui.editor.toc.HelpEditorUtil;
import org.eclipse.pde.internal.ua.ui.editor.toc.TocFileValidator;
import org.eclipse.pde.internal.ua.ui.editor.toc.TocInputContext;
import org.eclipse.pde.internal.ua.ui.editor.toc.TocTreeSection;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class TocLinkDetails extends TocAbstractDetails {

	private TocLink fDataTOCLink;

	private FormEntry fTocPathEntry;

	/**
	 * @param masterSection
	 */
	public TocLinkDetails(TocTreeSection masterSection) {
		super(masterSection, TocInputContext.CONTEXT_ID);
		fDataTOCLink = null;

		fTocPathEntry = null;
	}

	/**
	 * @param object
	 */
	public void setData(TocLink object) {
		// Set data
		fDataTOCLink = object;
	}

	protected TocObject getDataObject() {
		return fDataTOCLink;
	}

	protected FormEntry getPathEntryField() {
		return fTocPathEntry;
	}

	protected boolean isTocPath() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.CSAbstractDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createFields(Composite parent) {
		createTocPathWidget(parent);
	}

	/**
	 * @param parent
	 */
	private void createTocPathWidget(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), TocDetailsMessages.TocLinkDetails_locationDesc);

		fTocPathEntry = new FormEntry(parent, getManagedForm().getToolkit(), TocDetailsMessages.TocLinkDetails_locationText, TocDetailsMessages.TocLinkDetails_browse, isEditable());
	}

	protected String getDetailsTitle() {
		return TocDetailsMessages.TocLinkDetails_title;
	}

	protected String getDetailsDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.toc.TocAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		createTocPathEntryListeners();
	}

	/**
	 * 
	 */
	private void createTocPathEntryListeners() {
		fTocPathEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fDataTOCLink != null) {
					fDataTOCLink.setFieldTocPath(fTocPathEntry.getValue());
				}
			}

			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}

			public void linkActivated(HyperlinkEvent e) {
				handleOpen();
			}
		});
	}

	private void handleBrowse() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getPage().getSite().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new TocFileValidator(fDataTOCLink.getModel()));
		dialog.setAllowMultiple(false);
		dialog.setTitle(TocDetailsMessages.TocLinkDetails_dialogTitle);
		dialog.setMessage(TocDetailsMessages.TocLinkDetails_dialogMessage);
		dialog.addFilter(new FileExtensionFilter(HelpEditorUtil.tocExtension));

		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			setPathEntry(file);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.toc.TocAbstractDetails#updateFields()
	 */
	public void updateFields() {
		// Ensure data object is defined
		if (fDataTOCLink != null) { // Update name entry
			updateTocPathEntry(isEditableElement());
		}
	}

	/**
	 * @param editable
	 */
	private void updateTocPathEntry(boolean editable) {
		fTocPathEntry.setValue(fDataTOCLink.getFieldTocPath(), true);
		fTocPathEntry.setEditable(editable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fTocPathEntry.commit();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.IPartSelectionListener#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if (object != null && object instanceof TocLink) {
			// Set data
			setData((TocLink) object);
			// Update the UI given the new data
			updateFields();
		}
	}
}
