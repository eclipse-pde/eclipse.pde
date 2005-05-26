/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class PluginGeneralInfoSection extends GeneralInfoSection {

	private FormEntry fClassEntry;

	public PluginGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_desc; //$NON-NLS-1$
	}
	
	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createClassEntry(parent, toolkit, actionBars);		
	}
	
	private void createClassEntry(Composite client, FormToolkit toolkit,IActionBars actionBars) {
		fClassEntry = new FormEntry(
							client,
							toolkit,
							PDEUIMessages.GeneralInfoSection_class,  //$NON-NLS-1$
							PDEUIMessages.GeneralInfoSection_browse, //$NON-NLS-1$ 
							isEditable());
		fClassEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				try {
					((IPlugin) getPluginBase()).setClassName(entry.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void linkActivated(HyperlinkEvent e) {
				String value = fClassEntry.getValue();
				IProject project = getPage().getPDEEditor().getCommonProject();
				try {
					if (project.hasNature(JavaCore.NATURE_ID)) {
						IJavaProject javaProject = JavaCore.create(project);
						IJavaElement element = javaProject.findType(value.replace('$', '.'));
						if (element != null)
							JavaUI.openInEditor(element);
						else {
							JavaAttributeWizard wizard = new JavaAttributeWizard(createJavaAttributeValue());
							WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
							dialog.create();
							SWTUtil.setDialogSize(dialog, 400, 500);
							if (dialog.open() == WizardDialog.OK) {
								fClassEntry.setValue(wizard.getClassNameWithArgs());
							}
						}
					}
				} catch (PartInitException e1) {
				} catch (CoreException e1) {
				}
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog();
			}
		});
		fClassEntry.setEditable(isEditable());
	}
	
	private void doOpenSelectionDialog() {
		try {
			IResource resource = getPluginBase().getModel().getUnderlyingResource();
			IProject project = (resource == null) ? null : resource.getProject();
			if (project != null) {
				SelectionDialog dialog = JavaUI.createTypeDialog(
						PDEPlugin.getActiveWorkbenchShell(),
						PlatformUI.getWorkbench().getProgressService(),
						getSearchScope(project),
						IJavaElementSearchConstants.CONSIDER_CLASSES, 
						false,
						""); //$NON-NLS-1$
				dialog.setTitle(PDEUIMessages.GeneralInfoSection_selectionTitle); //$NON-NLS-1$
				if (dialog.open() == SelectionDialog.OK) {
					IType type = (IType) dialog.getResult()[0];
					fClassEntry.setValue(type.getFullyQualifiedName('$'));
				}
			}
		} catch (CoreException e) {
		}
	}
	
	private IJavaSearchScope getSearchScope(IProject project) {
		return SearchEngine.createJavaSearchScope(getDirectRoots(JavaCore.create(project)));
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
		return (IPackageFragmentRoot[]) result.toArray(new IPackageFragmentRoot[result.size()]);
	}

	private JavaAttributeValue createJavaAttributeValue() {
		IProject project = getPage().getPDEEditor().getCommonProject();
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		return new JavaAttributeValue(project, model, null, fClassEntry.getValue());
	}
	
	public void cancelEdit() {
		fClassEntry.cancelEdit();
		super.cancelEdit();
	}
	
	public void commit(boolean onSave) {
		fClassEntry.commit();
		super.commit(onSave);
	}
	
	public void refresh() {
		IPluginModelBase model = (IPluginModelBase) getPage().getModel();
		IPlugin plugin = (IPlugin)model.getPluginBase();
		fClassEntry.setValue(plugin.getClassName(), true);
		super.refresh();
	}

}
