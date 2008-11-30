/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp.details;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpTopic;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpInputContext;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpTreeSection;
import org.eclipse.pde.internal.ua.ui.editor.toc.details.HelpEditorFilter;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Details section for topic entries.
 * @since 3.4
 * @see CtxHelpAbstractDetails
 * @see CtxHelpTopic
 */
public class CtxHelpTopicDetails extends CtxHelpAbstractDetails {

	private CtxHelpTopic fTopic;
	private FormEntry fLabelEntry;
	private FormEntry fLinkEntry;

	public CtxHelpTopicDetails(CtxHelpTreeSection masterSection) {
		super(masterSection, CtxHelpInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#createFields(org.eclipse.swt.widgets.Composite)
	 */
	public void createFields(Composite parent) {
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpTopicDetails_label);
		fLabelEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpTopicDetails_location, SWT.NONE);
		createSpace(parent);
		createLabel(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpTopicDetails_locationHTML);
		fLinkEntry = new FormEntry(parent, getManagedForm().getToolkit(), CtxHelpDetailsMessages.CtxHelpTopicDetails_locationTitle, CtxHelpDetailsMessages.CtxHelpTopicDetails_browseTitle, isEditable());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#getDetailsTitle()
	 */
	protected String getDetailsTitle() {
		return CtxHelpDetailsMessages.CtxHelpTopicDetails_detailsTitle;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#getDetailsDescription()
	 */
	protected String getDetailsDescription() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#hookListeners()
	 */
	public void hookListeners() {
		fLabelEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				if (fTopic != null) {
					fTopic.setLabel(fLabelEntry.getValue());
				}
			}
		});
		fLinkEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) { // Ensure data object is defined
				if (fTopic != null) {
					// TODO Do we need better testing for path validity?
					fTopic.setLocation(new Path(fLinkEntry.getValue()));
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

	/**
	 * Handle when the browse button is pressed.  Open up a file selection dialog.
	 */
	private void handleBrowse() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getPage().getSite().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(CtxHelpDetailsMessages.CtxHelpTopicDetails_dialogTitle);
		dialog.setMessage(CtxHelpDetailsMessages.CtxHelpTopicDetails_dialogMessage);
		dialog.addFilter(new HelpEditorFilter());

		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile) dialog.getFirstResult();
			setPathEntry(file);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#updateFields()
	 */
	public void updateFields() {
		if (fTopic != null) {
			fLabelEntry.setValue(fTopic.getLabel(), true);
			fLabelEntry.setEditable(isEditableElement());
			if (fTopic.getLocation() == null) {
				fLinkEntry.setValue("", true); //$NON-NLS-1$
			} else {
				fLinkEntry.setValue(fTopic.getLocation().toPortableString(), true);
			}
			fLinkEntry.setEditable(isEditableElement());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fLabelEntry.commit();
		fLinkEntry.commit();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.ctxhelp.details.CtxHelpAbstractDetails#selectionChanged(org.eclipse.ui.forms.IFormPart, org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IFormPart part, ISelection selection) {
		Object object = getFirstSelectedObject(selection);
		if (object instanceof CtxHelpTopic) {
			fTopic = (CtxHelpTopic) object;
			updateFields();
		}
	}

	/**
	 * Sets the text of the form entry used to supply a link
	 * @param file
	 */
	protected void setPathEntry(IFile file) {
		IPath path = file.getFullPath();
		if (file.getProject().equals(fTopic.getModel().getUnderlyingResource().getProject())) {
			fLinkEntry.setValue(path.removeFirstSegments(1).toString());
		} else {
			fLinkEntry.setValue(".." + path.toString()); //$NON-NLS-1$
		}
	}

	/**
	 * Handle when the link is pressed.  Try opening the file at the specified path.
	 */
	protected void handleOpen() {
		getMasterSection().open(fTopic);
		// TODO Consider having a wizard open to create a new file if the path does not exist
	}

}
