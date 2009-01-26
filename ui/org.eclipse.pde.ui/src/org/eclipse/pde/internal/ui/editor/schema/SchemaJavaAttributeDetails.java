/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeFieldAssistDisposer;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class SchemaJavaAttributeDetails extends SchemaAttributeDetails {
	private FormEntry fClassEntry;
	private FormEntry fInterfaceEntry;
	private TypeFieldAssistDisposer fClassEntryFieldAssistDisposer;
	private TypeFieldAssistDisposer fInterfaceEntryFieldAssistDisposer;

	public SchemaJavaAttributeDetails(ElementSection section) {
		super(section);
	}

	protected void createTypeDetails(Composite parent, FormToolkit toolkit) {
		fClassEntry = new FormEntry(parent, toolkit, PDEUIMessages.SchemaAttributeDetails_extends, PDEUIMessages.SchemaAttributeDetails_browseButton, isEditable(), 13);
		fInterfaceEntry = new FormEntry(parent, toolkit, PDEUIMessages.SchemaAttributeDetails_implements, PDEUIMessages.SchemaAttributeDetails_browseButton, isEditable(), 13);
		fClassEntryFieldAssistDisposer = PDEJavaHelperUI.addTypeFieldAssistToText(fClassEntry.getText(), getPage().getPDEEditor().getCommonProject(), IJavaSearchConstants.CLASS);
		fInterfaceEntryFieldAssistDisposer = PDEJavaHelperUI.addTypeFieldAssistToText(fInterfaceEntry.getText(), getPage().getPDEEditor().getCommonProject(), IJavaSearchConstants.INTERFACE);
	}

	public void updateFields(ISchemaObject object) {
		if (!(object instanceof SchemaAttribute))
			return;
		super.updateFields(object);

		String basedOn = getAttribute().getBasedOn();
		if ((basedOn != null) && (basedOn.length() > 0)) {
			int index = basedOn.indexOf(":"); //$NON-NLS-1$
			if (index == -1) {
				String className = basedOn.substring(basedOn.lastIndexOf(".") + 1); //$NON-NLS-1$
				if ((className.length() > 1) && (className.charAt(0) == 'I')) {
					fClassEntry.setValue("", true); //$NON-NLS-1$
					fInterfaceEntry.setValue(basedOn, true);
				} else {
					fClassEntry.setValue(basedOn, true);
					fInterfaceEntry.setValue("", true); //$NON-NLS-1$
				}
			} else {
				fClassEntry.setValue(basedOn.substring(0, index), true);
				fInterfaceEntry.setValue(basedOn.substring(index + 1), true);
			}
		} else {
			fClassEntry.setValue("", true); //$NON-NLS-1$
			fInterfaceEntry.setValue("", true); //$NON-NLS-1$
		}

		boolean editable = isEditableElement();
		fClassEntry.setEditable(editable);
		fInterfaceEntry.setEditable(editable);
	}

	public void hookListeners() {
		super.hookListeners();
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fClassEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				setBasedOn();
			}

			public void linkActivated(HyperlinkEvent e) {
				if (blockListeners())
					return;
				String value = fClassEntry.getValue();
				value = handleLinkActivated(value, false);
				if (value != null)
					fClassEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				if (blockListeners())
					return;
				doOpenSelectionDialog(IJavaElementSearchConstants.CONSIDER_CLASSES, fClassEntry);
			}
		});
		fInterfaceEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				if (blockListeners())
					return;
				setBasedOn();
			}

			public void linkActivated(HyperlinkEvent e) {
				if (blockListeners())
					return;
				String value = fInterfaceEntry.getValue();
				value = handleLinkActivated(value, true);
				if (value != null)
					fInterfaceEntry.setValue(value);
			}

			public void browseButtonSelected(FormEntry entry) {
				if (blockListeners())
					return;
				doOpenSelectionDialog(IJavaElementSearchConstants.CONSIDER_INTERFACES, fInterfaceEntry);
			}
		});
	}

	private String handleLinkActivated(String value, boolean isInter) {
		IProject project = getPage().getPDEEditor().getCommonProject();
		try {
			if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement element = javaProject.findType(value.replace('$', '.'));
				if (element != null)
					JavaUI.openInEditor(element);
				else {
					NewClassCreationWizard wizard = new NewClassCreationWizard(project, isInter, value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
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

	private void setBasedOn() {
		String classEntry = fClassEntry.getValue().replaceAll(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		String interfaceEntry = fInterfaceEntry.getValue().replaceAll(":", ""); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuffer sb = new StringBuffer();
		if (classEntry.length() > 0)
			sb.append(classEntry);
		if (classEntry.length() > 0 || interfaceEntry.length() > 0)
			sb.append(":"); //$NON-NLS-1$
		if (interfaceEntry.length() > 0)
			sb.append(interfaceEntry);
		getAttribute().setBasedOn(sb.length() > 0 ? sb.toString() : null);
	}

	private void doOpenSelectionDialog(int scopeType, FormEntry entry) {
		try {
			String filter = entry.getValue();
			filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
			SelectionDialog dialog = JavaUI.createTypeDialog(PDEPlugin.getActiveWorkbenchShell(), PlatformUI.getWorkbench().getProgressService(), SearchEngine.createWorkspaceScope(), scopeType, false, filter); 
			dialog.setTitle(PDEUIMessages.GeneralInfoSection_selectionTitle);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				entry.setValue(type.getFullyQualifiedName('$'));
				entry.commit();
			}
		} catch (CoreException e) {
		}
	}

	public void commit(boolean onSave) {
		super.commit(onSave);
		// Only required for form entries
		fClassEntry.commit();
		fInterfaceEntry.commit();
	}

	public void dispose() {
		super.dispose();
		if (fClassEntryFieldAssistDisposer != null)
			fClassEntryFieldAssistDisposer.dispose();
		if (fInterfaceEntryFieldAssistDisposer != null)
			fInterfaceEntryFieldAssistDisposer.dispose();
	}
}
