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
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
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
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.LazyStartHeader;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class PluginGeneralInfoSection extends GeneralInfoSection {

	private FormEntry fClassEntry;
	private Button fLazyStart;

	public PluginGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_desc; 
	}
	
	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createClassEntry(parent, toolkit, actionBars);		
		if (isBundle()) {
			createLazyStart(parent, toolkit, actionBars);
		}
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.addModelChangedListener(this);
		}
	}
	
	public void dispose() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.removeModelChangedListener(this);
		}
		super.dispose();
	}
	
	private void createLazyStart(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		fLazyStart = toolkit.createButton(parent, PDEUIMessages.PluginGeneralInfoSection_lazyStart, SWT.CHECK);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		fLazyStart.setLayoutData(td);
		fLazyStart.setEnabled(isEditable());
		fLazyStart.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IManifestHeader header = getLazyStartHeader();
				if (header instanceof LazyStartHeader)
					((LazyStartHeader)header).setLazyStart(fLazyStart.getSelection());
				else
					getBundle().setHeader(getLazyStartHeaderName(), 
							Boolean.toString(fLazyStart.getSelection()));
			}
		});
	}
	
	private void createClassEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fClassEntry = new FormEntry(
							client,
							toolkit,
							PDEUIMessages.GeneralInfoSection_class,  
							PDEUIMessages.GeneralInfoSection_browse, // 
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
							if (dialog.open() == Window.OK) {
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
				dialog.setTitle(PDEUIMessages.GeneralInfoSection_selectionTitle); 
				if (dialog.open() == Window.OK) {
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
				IClasspathEntry entry = roots[i].getRawClasspathEntry();
				if (entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER 
					|| entry.getPath().equals(new Path(PDECore.CLASSPATH_CONTAINER_ID)))
				result.add(roots[i]);
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
		if (fLazyStart != null) {
			IManifestHeader header = getLazyStartHeader();
			fLazyStart.setSelection(header instanceof LazyStartHeader 
					&& ((LazyStartHeader)header).isLazyStart());
		}
		super.refresh();
	}
	
	private IManifestHeader getLazyStartHeader() {
		IBundle bundle = getBundle();
		if (bundle instanceof Bundle) {
			IManifestHeader header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_LAZYSTART);
			if (header == null)
				header = bundle.getManifestHeader(ICoreConstants.ECLIPSE_AUTOSTART);
			return header;
		}
		return null;
	}
	
	private String getLazyStartHeaderName() {
		if (TargetPlatform.getTargetVersion() >= 3.2
				&& BundlePluginBase.getBundleManifestVersion(getBundle()) >= 2)
			return ICoreConstants.ECLIPSE_LAZYSTART;
		return ICoreConstants.ECLIPSE_AUTOSTART;
	}

}
