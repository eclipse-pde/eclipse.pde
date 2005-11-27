/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.service.prefs.BackingStoreException;

public class SecondaryBundlesSection extends TableSection implements IModelChangedListener {
	
	private TableViewer fAdditionalTable;
	private Vector fAdditionalBundles;
	private Action fNewAction;
	private Action fRemoveAction;
	private Button fRequireBundleButton;
	private Button fImportPackageButton;
	private Button fManageDepsButton;
	private Label fResolveDepsLabel;
	private IProject fProject;
	
	private static String ADD = PDEUIMessages.RequiresSection_add;
	private static String REMOVE = PDEUIMessages.RequiresSection_delete; 
	
	class ContentProvider extends DefaultTableProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if (fAdditionalBundles == null)
				return createAdditionalBundles();	
			return fAdditionalBundles.toArray();
		}
		
		private IBuildEntry getBuildInfo() {
			IBuildEntry entry = null;
			IBuildModel model = getBuildModel();
			if (model == null)
				return null;
			IBuild buildObject = model.getBuild();
			entry = buildObject.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			return entry;
		}
		
		private Object[] createAdditionalBundles() {
			IBuildEntry entry = getBuildInfo(); 
			try {
				if (entry != null) {
					String [] tokens = entry.getTokens();
					fAdditionalBundles = new Vector(tokens.length);
					for (int i = 0; i < tokens.length; i++) {
						fAdditionalBundles.add(tokens[i].trim());
					}
					return fAdditionalBundles.toArray();
				}
				return new Object[0];
			} catch (Exception e) {
				PDEPlugin.logException( e );
				return new Object[0]; //If exception happen while getting bundles, return an empty table
			}
		}
	}
	
	class SecondaryTableLabelProvider extends SharedLabelProvider
	implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		
		public Image getColumnImage(Object obj, int index) {
			String pluginID = obj.toString();
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginID);
			if (model == null)
			{	
				return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, F_ERROR);
			}
			else if (model instanceof IBundlePluginModel  || model instanceof WorkspacePluginModel)
			{
				return get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ);
			}
			else if (model instanceof ExternalPluginModel)
			{
				return get(PDEPluginImages.DESC_PLUGIN_OBJ, F_EXTERNAL);
			}
			return null;
		}
	}

	public SecondaryBundlesSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION|Section.TWISTIE|Section.COMPACT, new String[] { ADD, REMOVE});
		getSection().setText(PDEUIMessages.SecondaryBundlesSection_title); 
		getSection().setDescription(PDEUIMessages.SecondaryBundlesSection_desc);
	}

	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		TablePart tablePart = getTablePart();
		fAdditionalTable = tablePart.getTableViewer();

		fAdditionalTable.setContentProvider(new ContentProvider());
		fAdditionalTable.setLabelProvider(new SecondaryTableLabelProvider());
		
		fManageDepsButton = toolkit.createButton(container, PDEUIMessages.SecondaryBundlesSection_check, SWT.CHECK);
		fManageDepsButton.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				enableButtons(fManageDepsButton.getSelection());
				savePreferences();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				enableButtons(fManageDepsButton.getSelection());
				savePreferences();
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fManageDepsButton.setLayoutData(gd);
		
		fResolveDepsLabel = toolkit.createLabel(container, PDEUIMessages.SecondaryBundlesSection_resolve, SWT.LEFT);
		gd = new GridData();
		gd.horizontalSpan = 2;
		fResolveDepsLabel.setLayoutData(gd);
		
		Composite comp = toolkit.createComposite(container);
		comp.setLayout(new GridLayout(2, false));
		gd = new GridData();
		gd.horizontalSpan = 2;
		comp.setLayoutData(gd);
		
		fRequireBundleButton = toolkit.createButton(comp, "Require-Bundle", SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fRequireBundleButton.setLayoutData(gd);
		fRequireBundleButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				savePreferences();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				savePreferences();
			}			
		});
		
		fImportPackageButton = toolkit.createButton(comp, "Import-Package", SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalIndent = 20;
		fImportPackageButton.setLayoutData(gd);
		fImportPackageButton.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				savePreferences();
			}
			public void widgetDefaultSelected(SelectionEvent e) {
				savePreferences();
			}			
		});
		
		toolkit.paintBordersFor(container);
		makeActions();
		section.setClient(container);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		initialize();
	}
	
	private void enableButtons(boolean value) {
		fRequireBundleButton.setEnabled(value);
		fImportPackageButton.setEnabled(value);
		fResolveDepsLabel.setEnabled(value);
	}
	
	private void savePreferences() {
		if (fProject == null) {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			IResource resource = model.getUnderlyingResource();
			if (resource == null)
				return;
			fProject = resource.getProject();
		}
		IEclipsePreferences pref = new ProjectScope(fProject).getNode(PDECore.PLUGIN_ID);
		
		if (!fManageDepsButton.getSelection())
			pref.putBoolean(ICoreConstants.SECONDARY_DEPENDENCIES, false);
		else 
			pref.remove(ICoreConstants.SECONDARY_DEPENDENCIES);
		if (fImportPackageButton.getSelection())
			pref.putBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, false);
		else
			pref.remove(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private void initialize() {
		try {
			IPluginModelBase model = (IPluginModelBase) getPage().getModel();
			fAdditionalTable.setInput(model.getPluginBase());
			getTablePart().setButtonEnabled(0, model.isEditable());
			getTablePart().setButtonEnabled(1, false);
			
			IBuildModel build = getBuildModel();
			if (build != null) 
				build.addModelChangedListener(this);
			
			IResource resource = model.getUnderlyingResource();
			if (resource == null)
				return;
			fProject = resource.getProject();
			IEclipsePreferences pref = new ProjectScope(fProject).getNode(PDECore.PLUGIN_ID);
			if (pref != null) {
				fManageDepsButton.setSelection(pref.getBoolean(ICoreConstants.SECONDARY_DEPENDENCIES, true));
				boolean useRequireBundle = pref.getBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, true);
				fRequireBundleButton.setSelection(useRequireBundle);
				fImportPackageButton.setSelection(!useRequireBundle);
				enableButtons(fManageDepsButton.getSelection());
			}	
		} catch (Exception e){
			PDEPlugin.logException( e );
		}
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fAdditionalTable.getSelection();
		manager.add(fNewAction);
		manager.add(new Separator());
		getPage().contextMenuAboutToShow(manager);
		
		if (!selection.isEmpty())
			manager.add(fRemoveAction);
	}
	
	public void refresh() {
		fAdditionalBundles = null;
		fAdditionalTable.refresh();
		super.refresh();
	}
	
	protected void buttonSelected(int index) {
		switch (index){
			case 0:
				handleNew();
				break;
			case 1:
				handleRemove();
				break;
		}
	}
	
	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
			.findContext(BuildInputContext.CONTEXT_ID);
		if (context == null)
			return null;
		return (IBuildModel) context.getModel();
	}

	private void makeActions() {
		fNewAction = new Action( ADD ){
			public void run() { 
				handleNew();
			}
		};
		
		fRemoveAction = new Action (REMOVE) {
			public void run() {
				handleRemove();
			}
		};
	}

	private void handleNew(){
		PluginSelectionDialog dialog =
			new PluginSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				getAvailablePlugins(),
				true);
		dialog.create();
		if (dialog.open() == PluginSelectionDialog.OK) {
		    IBuild build = getBuildModel().getBuild();
			IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			try {
			    if (entry == null) {
			        entry = getBuildModel().getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
			        build.add(entry);
			    }
			    Object[] models = dialog.getResult();
			
				for (int i = 0; i < models.length; i++) {
					IPluginModel pmodel = (IPluginModel) models[i];
					entry.addToken(pmodel.getPlugin().getId());
				}
			}catch (CoreException e) {
				PDEPlugin.logException( e );
			}
		}
	}
	
	private IPluginModelBase[] getAvailablePlugins() {
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPluginsOnly();
		HashSet currentPlugins = new HashSet( 
				 (fAdditionalBundles == null) ? new Vector(1) : fAdditionalBundles);
		IProject currentProj = getPage().getPDEEditor().getCommonProject();
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(currentProj);
		String project_id = entry.getId();
		currentPlugins.add(project_id);
		
		ArrayList result = new ArrayList();
		for (int i = 0; i < plugins.length; i++){
			if (!currentPlugins.contains(plugins[i].getPluginBase().getId())) 
					result.add(plugins[i]);
		}
		return (IPluginModelBase []) result.toArray(new IPluginModelBase[result.size()]);
	}
	
	private void handleRemove(){
		IStructuredSelection ssel = (IStructuredSelection) fAdditionalTable.getSelection();
		
		IBuildEntry entry = getBuildModel().getBuild().getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
		Iterator it = ssel.iterator();
		try {
			while (it.hasNext()) {
				String pluginName = (String) it.next();
				entry.removeToken(pluginName);
			}
		} catch (CoreException e){
			PDEPlugin.logException( e );
		}
		refresh();
	}

	protected void selectionChanged(IStructuredSelection sel) {
		Object item = sel.getFirstElement();
		if (item == null) {
			getTablePart().setButtonEnabled(1, false);
		} else 
			getTablePart().setButtonEnabled(1, true); 
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changedObject = event.getChangedObjects()[0];
		if ((changedObject instanceof IBuildEntry && 
				((IBuildEntry) changedObject).getName().equals(IBuildEntry.SECONDARY_DEPENDENCIES))) {
			refresh();
		}
	}
	
	public boolean doGlobalAction(String actionId) {
		// hit delete key
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		return false;
	}
}
