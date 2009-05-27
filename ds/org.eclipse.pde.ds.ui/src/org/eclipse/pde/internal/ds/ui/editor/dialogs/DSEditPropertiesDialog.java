/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 244997
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.dialogs;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSPropertiesSection;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class DSEditPropertiesDialog extends FormDialog {

	private IDSProperties fProperties;
	private DSPropertiesSection fPropertiesSection;
	private FormEntry fEntry;

	public DSEditPropertiesDialog(Shell parentShell,
			IDSProperties properties, DSPropertiesSection provideSection) {
		super(parentShell);
		fProperties = properties;
		fPropertiesSection = provideSection;

	}

	protected void createFormContent(IManagedForm mform) {
		mform.getForm().setText(Messages.DSEditPropertiesDialog_dialog_title);

		Composite container = mform.getForm().getBody();
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormToolkit toolkit = mform.getToolkit();
		toolkit.decorateFormHeading(mform.getForm().getForm());

		Composite entryContainer = toolkit.createComposite(container);
		entryContainer.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(false, 3));
		entryContainer.setLayoutData(new GridData(GridData.FILL_BOTH));


		// Attribute: title
		fEntry = new FormEntry(entryContainer, toolkit,
				Messages.DSPropertiesDetails_entry,
				Messages.DSPropertiesDetails_browse, false, 0);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 3; // FormLayoutFactory.CONTROL_HORIZONTAL_INDENT

		toolkit.paintBordersFor(entryContainer);
		updateFields();

		setEntryListeners();
	}

	public boolean isHelpAvailable() {
		return false;
	}

	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case 0:
			handleOKPressed();
			break;
		}
		super.buttonPressed(buttonId);
	}

	private void handleOKPressed() {
		fEntry.commit();
		if (!(fEntry.getValue().equals("") && fProperties.getEntry() == null)) { //$NON-NLS-1$
			if (!fEntry.getValue().equals(fProperties.getEntry())) {
				fProperties.setEntry(fEntry.getValue());
			}
		}

	}

	private void updateFields() {
		if (fProperties == null) {
			return;
		}

		if (fProperties.getEntry() == null) {
			fEntry.setValue(""); //$NON-NLS-1$
		} else {
			// Attribute: Interface
			fEntry.setValue(fProperties.getEntry(), true);
		}
		fEntry.setEditable(true);

	}

	public void setEntryListeners() {
		// Attribute: Interface
		fEntry.setFormEntryListener(new FormEntryAdapter(
				this.fPropertiesSection) {
			public void textValueChanged(FormEntry entry) {
				// no op due to OK Button
			}

			public void textDirty(FormEntry entry) {
				// no op due to OK Button
			}

			public void linkActivated(HyperlinkEvent e) {
				String value = fEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(fEntry);
			}

		});

	}

	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$',
						'.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					// TODO create our own wizard for reuse here
					DSNewClassCreationWizard wizard = new DSNewClassCreationWizard(
							project, isInter, value);
					WizardDialog dialog = new WizardDialog(Activator
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					if (dialog.open() == Window.OK) {
						return wizard.getQualifiedName();
					}
				}
			}
		} catch (PartInitException e1) {
		} catch (CoreException e1) {
		}
		return null;
	}

	private IProject getProject() {
		PDEFormEditor editor = (PDEFormEditor) this.fPropertiesSection
				.getPage().getEditor();
		return editor.getCommonProject();
	}

	private void doOpenSelectionDialog(FormEntry entry) {
		final IProject project = getProject();
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

	private IResource getFile() {
		String value = fEntry.getValue();
		if (value.length() == 0)
			return null;
		IProject project = getProject();
		IPath path = project.getFullPath().append(value);
		return project.getWorkspace().getRoot().findMember(path);
	}
	
}
