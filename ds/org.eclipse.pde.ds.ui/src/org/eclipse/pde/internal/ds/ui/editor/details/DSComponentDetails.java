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
import org.eclipse.pde.internal.ds.core.IDSComponent;
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
import org.eclipse.swt.widgets.Control;
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

public class DSComponentDetails extends DSAbstractDetails {

	private IDSComponent fComponent;
	private Section fMainSection;
	private FormEntry fNameEntry;
	private FormEntry fFactoryEntry;
	private ComboPart fEnabledCombo;
	private Label fLabelEnabled;
	private ComboPart fImmediateCombo;
	private Label fLabelImmediate;

	public DSComponentDetails(IDSMaster masterSection) {
		super(masterSection, DSInputContext.CONTEXT_ID);

	}

	public void createDetails(Composite parent) {

		Color foreground = getToolkit().getColors().getColor(IFormColors.TITLE);

		// Create main section
		fMainSection = getToolkit().createSection(parent,
				Section.DESCRIPTION | ExpandableComposite.TITLE_BAR);
		fMainSection.clientVerticalSpacing = FormLayoutFactory.SECTION_HEADER_VERTICAL_SPACING;
		fMainSection.setText(Messages.DSComponentDetails_mainSectionTitle);
		fMainSection.setDescription(Messages.DSComponentDetails_mainSectionDescription);

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
				Messages.DSComponentDetails_nameEntry, SWT.NONE);

		// Attribute: factory
		fFactoryEntry = new FormEntry(mainSectionClient, getToolkit(),
				Messages.DSComponentDetails_factoryEntry,
				Messages.DSComponentDetails_browse, isEditable(), 0);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		gd.horizontalSpan = 2;
		gd.horizontalIndent = 3; // FormLayoutFactory.CONTROL_HORIZONTAL_INDENT
		
		// Attribute: Enabled
		fLabelEnabled = getToolkit().createLabel(mainSectionClient,
				Messages.DSComponentDetails_enabledLabel, SWT.WRAP);
		fLabelEnabled.setForeground(foreground);
		fEnabledCombo = new ComboPart();
		fEnabledCombo.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);
		Control control = fEnabledCombo.getControl();
		String[] items = new String[] { IConstants.TRUE, IConstants.FALSE }; //$NON-NLS-1$ //$NON-NLS-2$
		fEnabledCombo.setItems(items);
		fEnabledCombo.getControl().setLayoutData(gd);

		// Attribute: Immediate
		fLabelImmediate = getToolkit().createLabel(mainSectionClient,
				Messages.DSComponentDetails_immediateLabel, SWT.WRAP);
		fLabelImmediate.setForeground(foreground);
		fImmediateCombo = new ComboPart();
		fImmediateCombo
				.createControl(mainSectionClient, getToolkit(), SWT.READ_ONLY);
		fImmediateCombo.setItems(items);
		fImmediateCombo.getControl().setLayoutData(gd);

		// Bind widgets
		getToolkit().paintBordersFor(mainSectionClient);
		fMainSection.setClient(mainSectionClient);
		markDetailsPart(fMainSection);

	}

	public void hookListeners() {
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
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite()
				.getActionBars();
		// Attribute: factory
		fFactoryEntry.setFormEntryListener(new FormEntryAdapter(this,
				actionBars) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setFactory(fFactoryEntry.getValue());
			}
			
			public void linkActivated(HyperlinkEvent e) {
				String value = fFactoryEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fFactoryEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_ALL_TYPES,
						fFactoryEntry);
			}
			
			
		});

		// Attribute: Enabled
		fEnabledCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}

				fComponent.setEnabled(fEnabledCombo.getSelectionIndex() == 0);
			}

		});

		// Attribute: Immediate
		fImmediateCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setImmediate(fImmediateCombo.getSelectionIndex() == 0);
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
			dialog.setTitle(Messages.DSImplementationDetails_selectType);
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
		if (fComponent == null) {
			return;
		}

		if (fComponent.getAttributeName() == null) {
			// Attribute: name
			fNameEntry.setValue("", true); //$NON-NLS-1$
		} else {
			// Attribute: name
			fNameEntry.setValue(fComponent.getAttributeName(), true);
		}
		fNameEntry.setEditable(editable);

		if (fComponent.getFactory() == null) {
			fFactoryEntry.setValue("", true); //$NON-NLS-1$
		} else {
			// Attribute: name
			fFactoryEntry.setValue(fComponent.getFactory(), true);
		}
		fFactoryEntry.setEditable(editable);

		// Attribute: Enabled
		fEnabledCombo.select(fComponent.getEnabled() ? 0 : 1);

		// Attribute: Immediate
		fImmediateCombo.select(fComponent.getImmediate() ? 0 : 1);

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		// Get the first selected object
		Object object = getFirstSelectedObject(selection);
		// Ensure we have the right type
		if ((object == null) || (object instanceof IDSComponent) == false) {
			return;
		}
		// Set data
		setData((IDSComponent) object);
		// Update the UI given the new data
		updateFields();
	}

	/**
	 * @param object
	 */
	public void setData(IDSComponent object) {
		// Set data
		fComponent = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		super.commit(onSave);
		fNameEntry.commit();
		fFactoryEntry.commit();
	}
}