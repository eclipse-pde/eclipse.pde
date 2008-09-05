/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

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
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSServiceComponentSection extends PDESection {

	private IDSComponent fComponent;
	private IDSImplementation fImplementation;
	private FormEntry fClassEntry;
	private FormEntry fNameEntry;
	private IDSModel fModel;
	private Button fImmediateButton;
	private Button fEnabledButton;

	public DSServiceComponentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {

		initializeAttributes();

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);
		section.setText(Messages.DSSection_title);
		section.setDescription(Messages.DSSection_description);

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Attribute: name
		fNameEntry = new FormEntry(client, toolkit,
				Messages.DSComponentDetails_nameEntry, SWT.NONE);

		// Attribute: title
		fClassEntry = new FormEntry(client, toolkit,
				Messages.DSImplementationDetails_classEntry,
				Messages.DSImplementationDetails_browse, isEditable(), 0);

		createButtons(client, toolkit);

		setListeners();
		updateUIFields();

		toolkit.paintBordersFor(client);
		section.setClient(client);

	}

	private void createButtons(Composite parent, FormToolkit toolkit) {
		fEnabledButton = toolkit.createButton(parent,
				Messages.DSServiceComponentSection_enabledButtonMessage,
				SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fEnabledButton.setLayoutData(data);
		fEnabledButton.setEnabled(isEditable());
		fEnabledButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fModel.getDSComponent().setEnabled(
						fEnabledButton.getSelection());
			}
		});

		fImmediateButton = toolkit.createButton(parent,
				Messages.DSServiceComponentSection_immediateButtonMessage,
				SWT.CHECK);
		fImmediateButton.setLayoutData(data);
		fImmediateButton.setEnabled(isEditable());
		fImmediateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fModel.getDSComponent().setImmediate(
						fImmediateButton.getSelection());
			}
		});
	}

	private void initializeAttributes() {
		fModel = (IDSModel) getPage().getModel();
		fModel.addModelChangedListener(this);

		fComponent = fModel.getDSComponent();
		if (fComponent != null) {
			fImplementation = fComponent.getImplementation();
		}
	}

	public void commit(boolean onSave) {
		fClassEntry.commit();
		fNameEntry.commit();
		super.commit(onSave);
	}

	public void modelChanged(IModelChangedEvent e) {
		fComponent = fModel.getDSComponent();
		if (fComponent != null)
			fImplementation = fComponent.getImplementation();

		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}

		if (fNameEntry != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					updateUIFields();
				}
			});
		}

	}

	public void updateUIFields() {

		if (fComponent != null) {
			if (fComponent.getAttributeName() == null) {
				// Attribute: name
				fNameEntry.setValue("", true); //$NON-NLS-1$
			} else {
				// Attribute: name
				fNameEntry.setValue(fComponent.getAttributeName(), true);
			}

			if (fComponent.getEnabled()) {
				fEnabledButton.setSelection(true);
			}

			if (fComponent.getImmediate()) {
				fImmediateButton.setSelection(true);
			}
			fNameEntry.setEditable(isEditable());

		}

		// Ensure data object is defined
		if (fImplementation != null) {
			if (fImplementation.getClassName() == null) {
				fClassEntry.setValue("", true); //$NON-NLS-1$
			} else {
				// Attribute: title
				fClassEntry.setValue(fImplementation.getClassName(), true);

			}
			fClassEntry.setEditable(isEditable());
		}

	}

	public void setListeners() {
		// Attribute: name
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setAttributeName(fNameEntry.getValue());
			}
		});
		IActionBars actionBars = this.getPage().getEditor().getEditorSite()
				.getActionBars();
		// Attribute: title
		fClassEntry
				.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
					public void textValueChanged(FormEntry entry) {
						// Ensure data object is defined
						if (fImplementation == null) {
							return;
						}
						fImplementation.setClassName(fClassEntry.getValue());
					}

					public void linkActivated(HyperlinkEvent e) {
						String value = fClassEntry.getValue();
						value = handleLinkActivated(value, false);
						if (value != null)
							fClassEntry.setValue(value);
					}

					public void browseButtonSelected(FormEntry entry) {
						doOpenSelectionDialog(
								IJavaElementSearchConstants.CONSIDER_CLASSES,
								fClassEntry);
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

	private void doOpenSelectionDialog(int scopeType, FormEntry entry) {
		try {
			String filter = entry.getValue();
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
			SelectionDialog dialog = JavaUI.createTypeDialog(Activator
					.getActiveWorkbenchShell(), PlatformUI.getWorkbench()
					.getProgressService(), SearchEngine.createWorkspaceScope(),
					scopeType, false, filter);
			dialog.setTitle(Messages.DSImplementationDetails_selectType);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
				entry.commit();
			}
		} catch (CoreException e) {
		}
	}

}
