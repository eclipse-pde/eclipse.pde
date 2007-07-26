/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Peter Friese <peter.friese@gentleware.com> - bug 191769
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleSymbolicNameHeader;
import org.eclipse.pde.internal.core.text.bundle.LazyStartHeader;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeFieldAssistDisposer;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.PDEJavaHelperUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.osgi.framework.Constants;

public class PluginGeneralInfoSection extends GeneralInfoSection {

	private FormEntry fClassEntry;
	private Button fLazyStart;
	private Button fSingleton;
	private TypeFieldAssistDisposer fTypeFieldAssistDisposer;

	public PluginGeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent);
	}
	
	protected String getSectionDescription() {
		return PDEUIMessages.ManifestEditor_PluginSpecSection_desc; 
	}
	
	protected void createSpecificControls(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		createClassEntry(parent, toolkit, actionBars);
		FormEditor formEditor = getPage().getEditor();
		if (isBundle() && (formEditor instanceof ManifestEditor)
				&& ((ManifestEditor) formEditor).isEquinox()) {
			createLazyStart(parent, toolkit, actionBars);
			createSingleton(parent, toolkit, actionBars);
		}
	}

	protected void addListeners() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.addModelChangedListener(this);
		}
		super.addListeners();
	}

	protected void removeListeners() {
		if (isBundle()) {
			IBundleModel model = getBundle().getModel();
			if (model != null)
				model.removeModelChangedListener(this);
		}
		super.removeListeners();
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
	
	private void createSingleton(Composite parent, FormToolkit toolkit, IActionBars actionBars) {
		fSingleton = toolkit.createButton(parent, PDEUIMessages.PluginGeneralInfoSection_singleton, SWT.CHECK);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		fSingleton.setLayoutData(td);
		fSingleton.setEnabled(isEditable());
		fSingleton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IManifestHeader header = getSingletonHeader();
				if (header instanceof BundleSymbolicNameHeader)
					((BundleSymbolicNameHeader)header).setSingleton(fSingleton.getSelection());
			}
		});
	}
	
	private void createClassEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		boolean isEditable = isEditable();
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
				value = PDEJavaHelperUI.createClass(value, project, createJavaAttributeValue(), false);
				if (value != null)
					fClassEntry.setValue(value);
			}
			public void browseButtonSelected(FormEntry entry) {
				doOpenSelectionDialog(entry.getValue());
			}
		});
		fClassEntry.setEditable(isEditable);
		
		if (isEditable) {
			fTypeFieldAssistDisposer = PDEJavaHelperUI.addTypeFieldAssistToText(
					fClassEntry.getText(), 
					getProject(),
					IJavaSearchConstants.CLASS);
		}
	}
	
	private void doOpenSelectionDialog(String className) {
		IResource resource = getPluginBase().getModel().getUnderlyingResource();
		String type = 
			PDEJavaHelperUI.selectType(resource, IJavaElementSearchConstants.CONSIDER_CLASSES, className);
		if (type != null)
			fClassEntry.setValue(type);
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
		// if we are refactoring, the Manifest moves before the editor closes.  This could cause the model to be null on a refresh()
		if (model == null)
			return;
		IPlugin plugin = (IPlugin)model.getPluginBase();
		// Only update this field if it already has not been modified
		// This will prevent the cursor from being set to position 0 after
		// accepting a field assist proposal using \r
		if (fClassEntry.isDirty() == false) {
			fClassEntry.setValue(plugin.getClassName(), true);
		}
		if (fLazyStart != null) {
			IManifestHeader header = getLazyStartHeader();
			fLazyStart.setSelection(header instanceof LazyStartHeader 
					&& ((LazyStartHeader)header).isLazyStart());
		}
		if (fSingleton != null) {
			IManifestHeader header = getSingletonHeader();
			fSingleton.setSelection(header instanceof BundleSymbolicNameHeader 
					&& ((BundleSymbolicNameHeader)header).isSingleton());
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
		if (TargetPlatformHelper.getTargetVersion() >= 3.2
				&& BundlePluginBase.getBundleManifestVersion(getBundle()) >= 2)
			return ICoreConstants.ECLIPSE_LAZYSTART;
		return ICoreConstants.ECLIPSE_AUTOSTART;
	}

	private IManifestHeader getSingletonHeader() {
		IBundle bundle = getBundle();
		if (bundle instanceof Bundle) {
			IManifestHeader header = bundle.getManifestHeader(Constants.BUNDLE_SYMBOLICNAME);
			return header;
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.plugin.GeneralInfoSection#dispose()
	 */
	public void dispose() {
		super.dispose();
		if (fTypeFieldAssistDisposer != null) {
			fTypeFieldAssistDisposer.dispose();
		}
	}
	
}
