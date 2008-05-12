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
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223739
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.details;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.IConstants;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.editor.DSInputContext;
import org.eclipse.pde.internal.ds.ui.editor.IDSMaster;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.schema.NewClassCreationWizard;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;

public class DSReferenceDetails extends DSAbstractDetails {

	private IDSReference fReference;
	private Section fMainSection;
	private FormEntry fNameEntry;
	private FormEntry fInterfaceEntry;
	private FormEntry fBindEntry;
	private FormEntry fUnBindEntry;
	private FormEntry fTargetEntry;
	private ComboPart fCardinality;
	private Label fLabelCardinality;
	private ComboPart fPolicy;
	private Label fLabelPolicy;

	public DSReferenceDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);
	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(Messages.DSReferenceDetails_title);
		fMainSection.setDescription(Messages.DSReferenceDetails_description);

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

		// Attribute: name
		fNameEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSReferenceDetails_nameEntry, SWT.NONE);

		// Attribute: Interface
		fInterfaceEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSReferenceDetails_interfaceEntry,
				Messages.DSReferenceDetails_browse, isEditable(), 0);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 3; // FormLayoutFactory.CONTROL_HORIZONTAL_INDENT

		// Attribute: Cardinality
		fLabelCardinality = getToolkit().createLabel(mainSectionClient,
				Messages.DSReferenceDetails_cardinalityLabel, SWT.WRAP);
		fLabelCardinality.setForeground(foreground);
		fCardinality = new ComboPart();
		fCardinality.createControl(mainSectionClient, getToolkit(),
				SWT.READ_ONLY);

		String[] itemsCard = new String[] { IConstants.CARDINALITY_ZERO_ONE,
				IConstants.CARDINALITY_ZERO_N, IConstants.CARDINALITY_ONE_ONE,
				IConstants.CARDINALITY_ONE_N };
		fCardinality.setItems(itemsCard);
		fCardinality.getControl().setLayoutData(gd);

		// Attribute: Target
		fTargetEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSReferenceDetails_targetEntry, SWT.NONE);

		// Attribute: Bind
		fBindEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSReferenceDetails_bindEntry, SWT.NONE);

		// Attribute: UnBind
		fUnBindEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSReferenceDetails_unbindEntry, SWT.NONE);

		// Attribute: Policy
		fLabelPolicy = getToolkit().createLabel(mainSectionClient,
				Messages.DSReferenceDetails_policeLabel, SWT.WRAP);
		fLabelPolicy.setForeground(foreground);
		fPolicy = new ComboPart();
		fPolicy.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);
		String[] itemsPolicy = new String[] { IConstants.REFERENCE_STATIC,
				IConstants.REFERENCE_DYNAMIC };
		fPolicy.setItems(itemsPolicy);
		fPolicy.getControl().setLayoutData(gd);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

	}

	public void hookListeners() {
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite()
				.getActionBars();

		// Attribute: name
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceName(fNameEntry.getValue());
			}

		});

		// Attribute: Interface
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceInterface(fInterfaceEntry.getValue());
			}
			

			public void linkActivated(HyperlinkEvent e) {
				String value = fInterfaceEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fInterfaceEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_INTERFACES,
						fInterfaceEntry);
			}

		});

		// Attribute: Target
		fTargetEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceTarget(fTargetEntry.getValue());
			}
		});

		// Attribute: Bind
		fBindEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceBind(fBindEntry.getValue());
			}
		});

		// Attribute: UnBind
		fUnBindEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferenceUnbind(fUnBindEntry.getValue());
			}
		});

		// Attribute: Cardinality
		fCardinality.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}

				fReference.setReferenceCardinality(fCardinality.getSelection());
			}

		});

		// Attribute: Policy
		fPolicy.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fReference == null) {
					return;
				}
				fReference.setReferencePolicy(fPolicy.getSelection());
			}

		});

	}

	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$',
						'.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					// TODO create our own wizard for reuse here
					NewClassCreationWizard wizard = new NewClassCreationWizard(
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
			dialog.setTitle(Messages.DSReferenceDetails_selectType);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
				entry.commit();
			}
		} catch (CoreException e) {
		}
	}

	public void updateFields() {

		boolean editable = isEditableElement();
		// Ensure data object is defined
		if (fReference == null) {
			return;
		}

		if (fReference.getReferenceName() == null) {
			fNameEntry.setValue(""); //$NON-NLS-1$
		} else {
			// Attribute: name
			fNameEntry.setValue(fReference.getReferenceName(), true);
		}
		fNameEntry.setEditable(editable);

		if (fReference.getReferenceInterface() == null) {
			fInterfaceEntry.setValue(""); //$NON-NLS-1$
		} else {
			// Attribute: Interface
			fInterfaceEntry.setValue(fReference.getReferenceInterface(), true);
		}
		fInterfaceEntry.setEditable(editable);

		// Attribute: Target
		fTargetEntry.setValue(fReference.getReferenceTarget(), true);
		fTargetEntry.setEditable(editable);

		// Attribute: Bind
		fBindEntry.setValue(fReference.getReferenceBind(), true);
		fBindEntry.setEditable(editable);

		// Attribute: Unbind
		fUnBindEntry.setValue(fReference.getReferenceUnbind(), true);
		fUnBindEntry.setEditable(editable);

		// Attribute: Cardinality
		if (fReference.getReferenceCardinality() != null)
			fCardinality.setText(fReference.getReferenceCardinality());

		// Attribute: Policy
		if (fReference.getReferencePolicy() != null)
			fPolicy.setText(fReference.getReferencePolicy());

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSReference) == false) {
			return;
		}
		// Set data
		setData((IDSReference) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSReference object) {
		// Set data
		fReference = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fNameEntry.commit();
		fInterfaceEntry.commit();
		fBindEntry.commit();
		fUnBindEntry.commit();
		fTargetEntry.commit();
	}
}