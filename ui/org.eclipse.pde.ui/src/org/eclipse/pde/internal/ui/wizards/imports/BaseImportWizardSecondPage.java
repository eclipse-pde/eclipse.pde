/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModelProviderEvent;
import org.eclipse.pde.core.IModelProviderListener;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

public abstract class BaseImportWizardSecondPage extends WizardPage implements IModelProviderListener {
	
	protected static final String SETTINGS_ADD_FRAGMENTS = "addFragments"; //$NON-NLS-1$
	protected static final String SETTINGS_AUTOBUILD = "autobuild"; //$NON-NLS-1$
	
	protected PluginImportWizardFirstPage fPage1;
	protected IPluginModelBase[] fModels = new IPluginModelBase[0];
	private String fLocation;
	protected Button fAddFragmentsButton;
	private Button fAutoBuildButton;
	protected TableViewer fImportListViewer;
	private boolean fRefreshNeeded = true;

	class ContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object element) {
			return new Object[0];
		}
	}
	
	public BaseImportWizardSecondPage(String pageName, PluginImportWizardFirstPage page) {
		super(pageName);
		fPage1 = page;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		PDECore.getDefault().getExternalModelManager().addModelProviderListener(this);
	}

	protected Composite createImportList(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Label label = new Label(container, SWT.NONE);
		label.setText(PDEUIMessages.ImportWizard_DetailedPage_importList); 

		Table table = new Table(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 225;
		table.setLayoutData(gd);

		fImportListViewer = new TableViewer(table);
		fImportListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fImportListViewer.setContentProvider(new ContentProvider());
		fImportListViewer.setInput(PDECore.getDefault().getExternalModelManager());
		fImportListViewer.setSorter(ListUtil.PLUGIN_SORTER);
		return container;
	}
	
	protected Composite createComputationsOption(Composite parent, int span) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		composite.setLayoutData(gd);
		
		fAddFragmentsButton = new Button(composite, SWT.CHECK);
		fAddFragmentsButton.setText(PDEUIMessages.ImportWizard_SecondPage_addFragments); 
		fAddFragmentsButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		if (getDialogSettings().get(SETTINGS_ADD_FRAGMENTS) != null)
			fAddFragmentsButton.setSelection(getDialogSettings().getBoolean(SETTINGS_ADD_FRAGMENTS));
		else 
			fAddFragmentsButton.setSelection(true);
		
		if (!PDEPlugin.getWorkspace().isAutoBuilding()) {
			fAutoBuildButton = new Button(composite, SWT.CHECK);
			fAutoBuildButton.setText(PDEUIMessages.BaseImportWizardSecondPage_autobuild);
			fAutoBuildButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fAutoBuildButton.setSelection(getDialogSettings().getBoolean(SETTINGS_AUTOBUILD));
		}
		return composite;
		
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		PDECore.getDefault().getExternalModelManager().removeModelProviderListener(this);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && isRefreshNeeded()) {
			fModels = fPage1.getModels();
			refreshPage();
		}
	}

	protected abstract void refreshPage();

	protected boolean isRefreshNeeded() {
		if (fRefreshNeeded) {
			fRefreshNeeded = false;
			fLocation = fPage1.getDropLocation();
			return true;	
		}			
		String currLocation = fPage1.getDropLocation();
		if (fLocation == null || !fLocation.equals(currLocation)) {
			fLocation = fPage1.getDropLocation();
			return true;
		}
		return false;	
	}
	
	private IPluginModelBase findModel(String id) {
		for (int i = 0; i < fModels.length; i++) {
			String modelId = fModels[i].getPluginBase().getId();
			if (modelId != null && modelId.equals(id))
				return fModels[i];
		}
		return null;
	}

	private IFragmentModel[] findFragments(IPlugin plugin) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < fModels.length; i++) {
			if (fModels[i] instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) fModels[i]).getFragment();
				if (plugin.getId().equalsIgnoreCase(fragment.getPluginId())) {
					result.add(fModels[i]);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}

	protected void addPluginAndDependencies(
		IPluginModelBase model,
		ArrayList selected,
		boolean addFragments) {
			
		boolean containsVariable = false;
		if (!selected.contains(model)) {
			selected.add(model);
			boolean hasextensibleAPI = ClasspathUtilCore.hasExtensibleAPI(model);
			if (!addFragments && !hasextensibleAPI && model instanceof IPluginModel) {
				IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
				for (int i = 0; i < libraries.length; i++) {
					if (ClasspathUtilCore.containsVariables(libraries[i].getName())) {
						containsVariable = true;
						break;
					}
				}
			}
			addDependencies(model, selected, addFragments || containsVariable || hasextensibleAPI);
		}
	}
	
	protected void addDependencies(
		IPluginModelBase model,
		ArrayList selected,
		boolean addFragments) {
		
		IPluginImport[] required = model.getPluginBase().getImports();
		if (required.length > 0) {
			for (int i = 0; i < required.length; i++) {
				IPluginModelBase found = findModel(required[i].getId());
				if (found != null) {
					addPluginAndDependencies(found, selected, addFragments);
				}
			}
		}
		
		if (addFragments) {
			if (model instanceof IPluginModel) {	
				IFragmentModel[] fragments = findFragments(((IPluginModel)model).getPlugin());
				for (int i = 0; i < fragments.length; i++) {
					addPluginAndDependencies(fragments[i], selected, addFragments);
				}
			} else {
				IFragment fragment = ((IFragmentModel) model).getFragment();
				IPluginModelBase found = findModel(fragment.getPluginId());
				if (found != null) {
					addPluginAndDependencies(found, selected, addFragments);
				}
			}
		}
	}
	
	public IPluginModelBase[] getModelsToImport() {
		TableItem[] items = fImportListViewer.getTable().getItems();
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			result.add(items[i].getData());
		}
		return (IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
	}
	
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(SETTINGS_ADD_FRAGMENTS, fAddFragmentsButton.getSelection());
		if (fAutoBuildButton != null)
			settings.put(SETTINGS_AUTOBUILD, fAutoBuildButton.getSelection());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModelProviderListener#modelsChanged(org.eclipse.pde.core.IModelProviderEvent)
	 */
	public void modelsChanged(IModelProviderEvent event) {
		fRefreshNeeded = true;
	}
	
	public boolean forceAutoBuild() {
		return fAutoBuildButton != null && getDialogSettings().getBoolean(SETTINGS_AUTOBUILD);
	}

}
