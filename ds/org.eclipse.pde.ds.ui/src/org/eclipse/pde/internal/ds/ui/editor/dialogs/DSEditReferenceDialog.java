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
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSReferenceSection;
import org.eclipse.pde.internal.ds.ui.parts.ComboPart;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ds.ui.wizards.DSNewClassCreationWizard;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.FormDialog;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class DSEditReferenceDialog extends FormDialog {

	private IDSReference fReference;
	private FormEntry fNameEntry;
	private FormEntry fInterfaceEntry;
	private FormEntry fBindEntry;
	private FormEntry fUnBindEntry;
	private FormEntry fTargetEntry;
	private ComboPart fCardinality;
	private Label fLabelCardinality;
	private ComboPart fPolicy;
	private Label fLabelPolicy;
	private DSReferenceSection fReferenceSection;

	public DSEditReferenceDialog(Shell parentShell, IDSReference reference,
			DSReferenceSection referenceSection) {
		super(parentShell);
		fReference = reference;
		fReferenceSection = referenceSection;
	}

	protected void createFormContent(IManagedForm mform) {
		mform.getForm().setText(Messages.DSEditReferenceDialog_dialog_title);

		Composite container = mform.getForm().getBody();
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		FormToolkit toolkit = mform.getToolkit();
		toolkit.decorateFormHeading(mform.getForm().getForm());

		Composite entryContainer = toolkit.createComposite(container);
		entryContainer.setLayout(FormLayoutFactory.createSectionClientGridLayout(
				false, 3));
		entryContainer.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Attribute: name
		fNameEntry = new FormEntry(entryContainer, toolkit,
				Messages.DSReferenceDetails_nameEntry, SWT.NONE);

		// Attribute: Interface
		fInterfaceEntry = new FormEntry(entryContainer, toolkit,
				Messages.DSReferenceDetails_interfaceEntry,
				Messages.DSReferenceDetails_browse, false, 0);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 3;

		// Attribute: Cardinality
		addCardinalityEntry(toolkit, entryContainer, gd);

		// Attribute: Policy
		addPolicyEntry(toolkit, entryContainer, gd);

		// Attribute: Target
		fTargetEntry = new FormEntry(entryContainer, toolkit,
				Messages.DSReferenceDetails_targetEntry, SWT.NONE);

		// Attribute: Bind
		fBindEntry = new FormEntry(entryContainer, toolkit,
				Messages.DSReferenceDetails_bindEntry, SWT.NONE);

		// Attribute: UnBind
		fUnBindEntry = new FormEntry(entryContainer, toolkit,
				Messages.DSReferenceDetails_unbindEntry, SWT.NONE);

		// Update Fields with fReference`s attributes values
		toolkit.paintBordersFor(entryContainer);
		updateFields();

		setInterfaceEntryListeners();
		toolkit.paintBordersFor(entryContainer);
	}


	private void addCardinalityEntry(FormToolkit toolkit, Composite container3,
			GridData gd) {
		fLabelCardinality = toolkit.createLabel(container3,
				Messages.DSReferenceDetails_cardinalityLabel, SWT.WRAP);
		fLabelCardinality.setForeground(toolkit.getColors().getForeground());
		fCardinality = new ComboPart();
		fCardinality.createControl(container3, toolkit, SWT.READ_ONLY);

		String[] itemsCard = new String[] {
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_ONE,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ZERO_N,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_ONE,
				IDSConstants.VALUE_REFERENCE_CARDINALITY_ONE_N };
		fCardinality.setItems(itemsCard);
		fCardinality.getControl().setLayoutData(gd);
	}

	private void addPolicyEntry(FormToolkit toolkit, Composite container3,
			GridData gd) {
		fLabelPolicy = toolkit.createLabel(container3,
				Messages.DSReferenceDetails_policeLabel, SWT.WRAP);
		fLabelPolicy.setForeground(toolkit.getColors().getForeground());
		fPolicy = new ComboPart();
		fPolicy.createControl(container3, toolkit, SWT.READ_ONLY);
		String[] itemsPolicy = new String[] {
				IDSConstants.VALUE_REFERENCE_POLICY_STATIC,
				IDSConstants.VALUE_REFERENCE_POLICY_DYNAMIC };
		fPolicy.setItems(itemsPolicy);
		fPolicy.getControl().setLayoutData(gd);
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
		fNameEntry.commit();
		if (!(fNameEntry.getValue().equals("") && fReference.getReferenceName() == null)) { //$NON-NLS-1$
			if (!fNameEntry.getValue().equals(fReference.getReferenceName())) {
				fReference.setReferenceName(fNameEntry.getValue());
			}
		}

		fInterfaceEntry.commit();
		if (!(fInterfaceEntry.getValue().equals("") && fReference.getReferenceInterface() == null)) { //$NON-NLS-1$
			if (!fInterfaceEntry.getValue().equals(
					fReference.getReferenceInterface())) {
				fReference.setReferenceInterface(fInterfaceEntry.getValue());
			}
		}

		fBindEntry.commit();
		if (!(fBindEntry.getValue().equals("") && fReference.getReferenceBind() == null)) { //$NON-NLS-1$
			if (!fBindEntry.getValue().equals(fReference.getReferenceBind())) {
				fReference.setReferenceBind(fBindEntry.getValue());
			}
		}


		fUnBindEntry.commit();
		if (!(fUnBindEntry.getValue().equals("") && fReference.getReferenceUnbind() == null)) { //$NON-NLS-1$
			if (!fUnBindEntry.getValue()
					.equals(fReference.getReferenceUnbind())) {
				fReference.setReferenceUnbind(fUnBindEntry.getValue());
			}
		}

		fTargetEntry.commit();
		if ( !(fTargetEntry.getValue().equals("") && fReference.getReferenceTarget() == null)) { //$NON-NLS-1$
			if (!fTargetEntry.getValue().equals(fReference.getReferenceTarget())) {
				fReference.setReferenceTarget(fTargetEntry.getValue());
			}
		}

		if (fCardinality.getSelection() != null) {
			if (!fCardinality.getSelection().equals(
					fReference.getReferenceCardinality())) {
				fReference.setReferenceCardinality(fCardinality.getSelection());
			}
		}

		if (fPolicy.getSelection() != null) {
			if (!fPolicy.getSelection().equals(fReference.getReferencePolicy())) {
				fReference.setReferencePolicy(fPolicy.getSelection());
			}
		}
	}

	private void updateFields() {
		if (fReference == null) {
			return;
		}

		if (fReference.getReferenceName() == null) {
			fNameEntry.setValue(""); //$NON-NLS-1$
		} else {
			// Attribute: name
			fNameEntry.setValue(fReference.getReferenceName(), true);
		}
		fNameEntry.setEditable(true);

		if (fReference.getReferenceInterface() == null) {
			fInterfaceEntry.setValue(""); //$NON-NLS-1$
		} else {
			// Attribute: Interface
			fInterfaceEntry.setValue(fReference.getReferenceInterface(), true);
		}
		fInterfaceEntry.setEditable(true);

		// Attribute: Target
		fTargetEntry.setValue(fReference.getReferenceTarget(), true);
		fTargetEntry.setEditable(true);

		// Attribute: Bind
		fBindEntry.setValue(fReference.getReferenceBind(), true);
		fBindEntry.setEditable(true);

		// Attribute: Unbind
		fUnBindEntry.setValue(fReference.getReferenceUnbind(), true);
		fUnBindEntry.setEditable(true);

		// Attribute: Cardinality
		if (fReference.getReferenceCardinality() != null)
			fCardinality.setText(fReference.getReferenceCardinality());

		// Attribute: Policy
		if (fReference.getReferencePolicy() != null)
			fPolicy.setText(fReference.getReferencePolicy());

	}

	public void setInterfaceEntryListeners() {
		// Attribute: Interface
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(
				this.fReferenceSection) {
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
		PDEFormEditor editor = (PDEFormEditor) this.fReferenceSection.getPage()
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
			dialog.setTitle(Messages.DSReferenceDetails_selectType);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
				entry.commit();
			}
		} catch (CoreException e) {
		}
	}

}
