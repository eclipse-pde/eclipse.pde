/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin.rows;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.core.search.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.IContextPart;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.ide.IDE;
public class ClassAttributeRow extends ReferenceAttributeRow {
	public ClassAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	protected boolean isReferenceModel() {
		return !part.getPage().getModel().isEditable();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#openReference()
	 */
	protected void openReference() {
		String name = text.getText();
		name = trimNonAlphaChars(name);
		String path = name.replace('.', '/') + ".java"; //$NON-NLS-1$
		IProject project = part.getPage().getPDEEditor().getCommonProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement result = javaProject.findElement(new Path(path));
				if (result != null) {
					JavaUI.openInEditor(result);
				} else {
					JavaAttributeValue value = createJavaAttributeValue();
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == WizardDialog.OK) {
						path = wizard.getClassName().replace('.', '/')
								+ ".java"; //$NON-NLS-1$
						text.setText(wizard.getClassNameWithArgs());
						result = javaProject.findElement(new Path(path));
						if (result != null)
							JavaUI.openInEditor(result);
					}
				}
			} else {
				IResource resource = project.findMember(new Path(path));
				if (resource != null && resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				} else {
					JavaAttributeValue value = createJavaAttributeValue();
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin
							.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == WizardDialog.OK) {
						String newValue = wizard.getClassName();
						path = newValue.replace('.', '/') + ".java"; //$NON-NLS-1$
						text.setText(newValue);
						resource = project.findMember(new Path(path));
						if (resource != null && resource instanceof IFile) {
							IWorkbenchPage page = PDEPlugin.getActivePage();
							IDE.openEditor(page, (IFile) resource, true);
						}
					}
				}
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#browse()
	 */
	protected void browse() {
		BusyIndicator.showWhile(text.getDisplay(), new Runnable() {
			public void run() {
				doOpenSelectionDialog();
			}
		});
	}
	private JavaAttributeValue createJavaAttributeValue() {
		IProject project = part.getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) part.getPage().getModel();
		String value = text.getText();
		return new JavaAttributeValue(project, model, getAttribute(), value);
	}
	private void doOpenSelectionDialog() {
		try {
			Shell shell = PDEPlugin.getActiveWorkbenchShell();
			IResource resource = getPluginBase().getModel()
					.getUnderlyingResource();
			IProject project = (resource == null) ? null : resource
					.getProject();
			if (project != null) {
				SelectionDialog dialog = JavaUI.createTypeDialog(shell,
						PlatformUI.getWorkbench().getProgressService(),
						getSearchScope(project),
						IJavaElementSearchConstants.CONSIDER_CLASSES, false,
						"*"); //$NON-NLS-1$
				dialog.setTitle(PDEPlugin.getResourceString("ClassAttributeRow.dialogTitle")); //$NON-NLS-1$
				if (dialog.open() == SelectionDialog.OK) {
					IType type = (IType) dialog.getResult()[0];
					text.setText(type.getFullyQualifiedName());
				}
			}
		} catch (CoreException e) {
		}
	}
	private IJavaSearchScope getSearchScope(IProject project) {
		IJavaProject jProject = JavaCore.create(project);
		return SearchEngine.createJavaSearchScope(getDirectRoots(jProject));
	}
	private IPackageFragmentRoot[] getDirectRoots(IJavaProject project) {
		ArrayList result = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = project.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[i].isArchive() && !roots[i].isExternal())) {
					result.add(roots[i]);
				}
			}
		} catch (JavaModelException e) {
		}
		return (IPackageFragmentRoot[]) result
				.toArray(new IPackageFragmentRoot[result.size()]);
	}
	private IPluginBase getPluginBase() {
		IBaseModel model = part.getPage().getPDEEditor().getAggregateModel();
		return ((IPluginModelBase) model).getPluginBase();
	}
	private String trimNonAlphaChars(String value) {
		value = value.trim();
		while (value.length() > 0 && !Character.isLetter(value.charAt(0)))
			value = value.substring(1, value.length());
		int loc = value.indexOf(":"); //$NON-NLS-1$
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = ""; //$NON-NLS-1$
		return value;
	}
}