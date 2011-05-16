/*******************************************************************************
 * Copyright (c) 2008, 2009 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/

package org.eclipse.pde.internal.ds.ui.editor.dialogs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSProvideSection;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DSEditProvideDialog extends FormDialog {

	private IDSProvide fProvide;
	private FormEntry fInterfaceEntry;
	private DSProvideSection fProvideSection;

	public DSEditProvideDialog(Shell parentShell, IDSProvide provide,
			DSProvideSection provideSection) {
		super(parentShell);
		fProvide = provide;
		fProvideSection = provideSection;

	}
	
	protected void createFormContent(IManagedForm mform) {
		mform.getForm().setText(Messages.DSEditProvideDialog_dialog_title);
		
		Composite container = mform.getForm().getBody();
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormToolkit toolkit = mform.getToolkit();
		toolkit.decorateFormHeading(mform.getForm().getForm());

		Composite composite = toolkit.createComposite(container);
		composite.setLayout(FormLayoutFactory
				.createSectionClientGridLayout(
				false, 3));
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Attribute: Interface
		fInterfaceEntry = new FormEntry(composite, toolkit,
				Messages.DSProvideDetails_interface,
				Messages.DSProvideDetails_browse, false, 0);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 3; // FormLayoutFactory.CONTROL_HORIZONTAL_INDENT

		// Bind widgets
		toolkit.paintBordersFor(composite);
		updateFields();

		setInterfaceEntryListeners();
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
		fInterfaceEntry.commit();
		if (!(fInterfaceEntry.getValue().equals("") && fProvide.getInterface() == null)) { //$NON-NLS-1$
			if (!fInterfaceEntry.getValue().equals(
					fProvide.getInterface())) {
				fProvide.setInterface(fInterfaceEntry.getValue());
			}
		}

	}

	private void updateFields() {
		if (fProvide == null) {
			return;
		}


		if (fProvide.getInterface() == null) {
			fInterfaceEntry.setValue(""); //$NON-NLS-1$
		} else {
			// Attribute: Interface
			fInterfaceEntry.setValue(fProvide.getInterface(), true);
		}
		fInterfaceEntry.setEditable(true);

	}

	public void setInterfaceEntryListeners() {
		// Attribute: Interface
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(
				this.fProvideSection) {
			public void textValueChanged(FormEntry entry) {
				// no op due to OK Button
			}
			
			public void textDirty(FormEntry entry) {
				// no op due to OK Button
			}

			public void linkActivated(HyperlinkEvent e) {
				String value = fInterfaceEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fInterfaceEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_CLASSES_AND_INTERFACES,
						fInterfaceEntry);
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
		PDEFormEditor editor = (PDEFormEditor) this.fProvideSection.getPage()
				.getEditor();
		return editor.getCommonProject();
	}

	private void doOpenSelectionDialog(int scopeType, FormEntry entry) {
		try {
			String filter = entry.getValue();
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
			SelectionDialog dialog = JavaUI.createTypeDialog(Activator
					.getActiveWorkbenchShell(), PlatformUI.getWorkbench()
					.getProgressService(), SearchEngine.createWorkspaceScope(),
					scopeType, false, filter);
			dialog.setTitle(Messages.DSProvideDetails_selectType);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
				entry.commit();
			}
		} catch (CoreException e) {
		}
	}
}
