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
 *     Rafael Oliveira N�brega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.details;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class DSPropertiesDetails extends DSAbstractDetails {

	private IDSProperties fProperties;
	private Section fMainSection;
	private FormEntry fEntry;

	public DSPropertiesDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
		fProperties = null;
		fMainSection = null;
		fEntry = null;
	}

	public void createDetails(Composite parent) {
		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(Messages.DSPropertiesDetails_sectionTitle);
		fMainSection
				.setDescription(Messages.DSPropertiesDetails_sectionDescription);

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
				.createSectionClientGridLayout(false, 3));

		// Attribute: title
		fEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSPropertiesDetails_entry,
				Messages.DSPropertiesDetails_browse, isEditable(), 0);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

	}

	public void hookListeners() {
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite()
				.getActionBars();

		// Attribute: title
		fEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fProperties == null) {
					return;
				}
				fProperties.setEntry(fEntry.getValue());
			}

			public void linkActivated(HyperlinkEvent e) {
				String value = fEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fProperties.setEntry(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(fEntry);
			}
		});
	}

	private String handleLinkActivated(String value, boolean isInter) {
		try {
			IResource resource = getFile();
			if (resource != null) {
				IDE.openEditor(Activator.getActiveWorkbenchWindow()
						.getActivePage(), (IFile) resource, true);
			} 
		} catch (PartInitException e) {
		}
		return null;
	}

	private void doOpenSelectionDialog(FormEntry entry) {
		final IProject project = getPage().getPDEEditor().getCommonProject();
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				Activator.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setInput(project.getWorkspace());
		IResource resource = getFile();
		if (resource != null)
			dialog.setInitialSelection(resource);
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return true;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(Messages.DSPropertiesDetails_dialogTitle);
		dialog.setMessage(Messages.DSPropertiesDetails_dialogMessage);
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection != null
						&& selection.length > 0
						&& (selection[0] instanceof IFile || selection[0] instanceof IContainer))
					return new Status(IStatus.OK, Activator.PLUGIN_ID,
							IStatus.OK, "", null); //$NON-NLS-1$

				return new Status(IStatus.ERROR, Activator.PLUGIN_ID,
						IStatus.ERROR, "", null); //$NON-NLS-1$
			}
		});
		if (dialog.open() == Window.OK) {
			IResource res = (IResource) dialog.getFirstResult();
			IPath path = res.getProjectRelativePath();
			if (res instanceof IContainer)
				path = path.addTrailingSeparator();
			String value = path.toString();
			fEntry.setValue(value);
		}
	}

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fProperties == null) {
			return;
		}

		if (fProperties.getEntry() == null) {
			fEntry.setValue("", true); //$NON-NLS-1$
		} else {
			// Attribute: title
			fEntry.setValue(fProperties.getEntry(), true);
		}
		fEntry.setEditable(editable);

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSProperties) == false) {
			return;
		}
		// Set data
		setData((IDSProperties) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSProperties object) {
		// Set data
		fProperties = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fEntry.commit();
	}
	
	private IResource getFile() {
		String value = fEntry.getValue();
		if (value.length() == 0)
			return null;
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPath path = project.getFullPath().append(value);
		return project.getWorkspace().getRoot().findMember(path);
	}

}
