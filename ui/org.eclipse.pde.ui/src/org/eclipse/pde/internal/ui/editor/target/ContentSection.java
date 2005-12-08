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
package org.eclipse.pde.internal.ui.editor.target;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.wizards.FeatureSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ContentSection extends TableSection {
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (fLastTab == 0)
				return getTarget().getPlugins();
			return getTarget().getFeatures();
		}
	}
	
	private static final String[] TAB_LABELS = new String[2];
	static {
		TAB_LABELS[0] = "Plug-ins";
		TAB_LABELS[1] = "Features";
	}
	
	private static final String[] BUTTONS = new String[5];
	static {
		BUTTONS[0] = "Add...";
		BUTTONS[1] = "Remove";
		BUTTONS[2] = "Remove All";
		BUTTONS[3] = "Add Working Set...";
		BUTTONS[4] = "Add Required Plugins";
	}
	
	private TableViewer fContentViewer;
	private CTabFolder fTabFolder;
	private int fLastTab;
	private Button fUseAllPlugins;
	
	public ContentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, BUTTONS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 2;
		layout.verticalSpacing = 15;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fUseAllPlugins = toolkit.createButton(client, "Include all plug-ins from the specified target", SWT.CHECK);
		fUseAllPlugins.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getTarget().setUseAllPlugins(fUseAllPlugins.getSelection());
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fUseAllPlugins.setLayoutData(gd);
		
		fTabFolder = new CTabFolder(client, SWT.FLAT|SWT.TOP);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 2;
		gd.horizontalSpan = 2;
		fTabFolder.setLayoutData(gd);
		toolkit.adapt(fTabFolder, true, true);
		toolkit.getColors().initializeSectionToolBarColors();
		Color selectedColor1 = toolkit.getColors().getColor(FormColors.TB_BG);
		Color selectedColor2 = toolkit.getColors().getColor(FormColors.TB_GBG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor1,
				selectedColor2, toolkit.getColors().getBackground() },
				new int[] { 50, 100 }, true);
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				refresh();
			}
		});
		
		createTabs();
		
		createViewerPartControl(client, SWT.MULTI, 2, toolkit);
		
		TablePart tablePart = getTablePart();
		GridData data = (GridData) tablePart.getControl().getLayoutData();
		data.grabExcessVerticalSpace = true;
		data.grabExcessHorizontalSpace = true;
		fContentViewer = tablePart.getTableViewer();
		fContentViewer.setContentProvider(new ContentProvider());
		fContentViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fContentViewer.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof ITargetPlugin) {
					ITargetPlugin p1 = (ITargetPlugin) e1;
					ITargetPlugin p2 = (ITargetPlugin) e2;
					return super.compare(viewer, p1.getId(), p2.getId());
				} // else 
				ITargetFeature f1 = (ITargetFeature)e1;
				ITargetFeature f2 = (ITargetFeature)e2;
				return super.compare(viewer, f1.getId(), f2.getId());
			}
		});
		fContentViewer.setInput(PDECore.getDefault().getModelManager());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		section.setText("Target Content");
		section.setDescription("Select the plug-ins and features that make up the content of this profile.");
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		getModel().addModelChangedListener(this);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleAdd();
			break;
		case 1:
			handleDelete();
			break;
		case 2:
			handleRemoveAll();
			break;
		case 3:
			handleAddWorkingSet();
			break;
		case 4:
			handleAddRequired(getTarget().getPlugins());
		}
	}
	
	private void createTabs() {
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
		}
		fLastTab = 0;
		fTabFolder.setSelection(fLastTab);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fLastTab = fTabFolder.getSelectionIndex();
		fContentViewer.refresh();
		boolean useAllPlugins = getTarget().useAllPlugins();
		fUseAllPlugins.setSelection(useAllPlugins);
		fTabFolder.setEnabled(!useAllPlugins);
		getTablePart().setEnabled(!useAllPlugins);
		if (!useAllPlugins) {
			TablePart table = getTablePart();
			table.setButtonEnabled(0, isEditable());
			table.setButtonEnabled(1, isEditable());
			table.setButtonEnabled(2, isEditable());
			boolean pluginTab = (fLastTab == 0);
			table.setButtonEnabled(3, isEditable() && pluginTab);
			table.setButtonEnabled(4, isEditable() && pluginTab);
		}
		
		super.refresh();
	}
	
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ITargetPlugin && fLastTab == 0 ||
				objects[i] instanceof ITargetFeature && fLastTab == 1)
				return true;
		}
		return false;
	}
	
	private ITarget getTarget() {
		return getModel().getTarget();
	}
	
	private ITargetModel getModel() {
		return (ITargetModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	private void handleAdd() {
		if (fLastTab == 0)
			handleAddPlugin();
		else 
			handleAddFeature();
	}
	
	private void handleAddPlugin() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(), 
				PDEPlugin.getDefault().getLabelProvider());
		
		dialog.setElements(getBundles());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title); 
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] bundles = dialog.getResult();
			ITarget target = getTarget();
			ITargetModelFactory factory = getModel().getFactory();
			for (int i = 0; i < bundles.length; i++) {
				String id = ((BundleDescription)bundles[i]).getSymbolicName();
				ITargetPlugin plugin = factory.createPlugin();
				plugin.setId(id);
				target.addPlugin(plugin);
			}
		}
	}
	
	private BundleDescription[] getBundles() {
		TreeMap map = new TreeMap();
		ITarget target = getTarget();
		BundleDescription[] bundles = TargetPlatform.getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			String id = bundles[i].getSymbolicName();
			if (!target.containsPlugin(id)) {
				map.put(id, bundles[i]);
			}
		}
		return (BundleDescription[])map.values().toArray(new BundleDescription[map.size()]);
	}
	
	private void handleAddFeature() {
		IFeatureModel[] allModels = PDECore.getDefault()
			.getFeatureModelManager().getModels();
		ArrayList newModels = new ArrayList();
		ITarget target = getTarget();
		for (int i = 0; i < allModels.length; i++) {
			if (!target.containsFeature(allModels[i].getFeature().getId()))
				newModels.add(allModels[i]);
		}
		IFeatureModel[] candidateModels = (IFeatureModel[]) newModels
		.toArray(new IFeatureModel[newModels.size()]);
		FeatureSelectionDialog dialog = new FeatureSelectionDialog(
				getSection().getShell(),
				candidateModels, true);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			ITargetModelFactory factory = getModel().getFactory();
			for (int i = 0; i < models.length; ++i) {
				IFeature feature = ((IFeatureModel)models[i]).getFeature();
				String id = feature.getId();
				String version = feature.getVersion();
				ITargetFeature tfeature = factory.createFeature();
				tfeature.setId(id);
				tfeature.setVersion(version);
				target.addFeature(tfeature);
			}
		}
	}
	
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection)fContentViewer.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			ITarget target = getTarget();
			if (fLastTab == 0) {
				for (int i = 0; i < objects.length; i++) {
					target.removePlugin((ITargetPlugin)objects[i]);
				}	
			} else {
				for (int i = 0; i < objects.length; i++) {
					target.removeFeature((ITargetFeature)objects[i]);
				}
			}
		}
	}
	
	private void handleRemoveAll() {
		TableItem[] items = fContentViewer.getTable().getItems();
		ITarget target = getTarget();
		if (fLastTab == 0) {
			for (int i = 0; i < items.length; i++) {
				target.removePlugin((ITargetPlugin)items[i].getData());
			}
		} else {
			for (int i = 0; i < items.length; i++) {
				target.removeFeature((ITargetFeature)items[i].getData());
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TableSection#handleDoubleClick(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}
	
	private void handleOpen(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		if (object instanceof ITargetPlugin) {
			ManifestEditor.openPluginEditor(((ITargetPlugin)object).getId());
		} else if (object instanceof ITargetFeature) {
			handleOpenFeature((ITargetFeature)object);
		}
	}
	
	private void handleOpenFeature(ITargetFeature feature) {
		IFeatureModel model = PDECore.getDefault()
			.getFeatureModelManager().findFeatureModel(feature.getId());
		FeatureEditor.openFeatureEditor(model);
	}
	
	private void handleAddWorkingSet() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), true);
		if (dialog.open() == Window.OK) {
			IWorkingSet[] workingSets = dialog.getSelection();
			ITarget target = getTarget();
			ITargetModelFactory factory = target.getModel().getFactory();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IPluginModelBase model = findModel(elements[j]);
					if (model != null) {
						ITargetPlugin plugin = factory.createPlugin();
						plugin.setId(model.getPluginBase().getId());
						target.addPlugin(plugin);						
					}
				}
			}
		}
	}
	
	private IPluginModelBase findModel(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject)object).getProject();
		if (object instanceof IProject)
			return PDECore.getDefault().getModelManager().findModel((IProject)object);
		if (object instanceof PersistablePluginObject) {
			return PDECore.getDefault().getModelManager().findModel(((PersistablePluginObject)object).getPluginID());
		}
		return null;
	}
	
	public static void handleAddRequired(ITargetPlugin[] plugins) {
		if (plugins.length == 0)
			return;
		
		HashSet set = new HashSet();
		for (int i = 0; i < plugins.length; i++) {
			addDependencies(TargetPlatform.getState().getBundle(plugins[i].getId(), null), set);
		}
		
		ITarget target = plugins[0].getTarget();
		BundleDescription[] fragments = getAllFragments();
		for (int i = 0; i < fragments.length; i++) {
			String id = fragments[i].getSymbolicName();
			if (set.contains(id) || "org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
				continue;
			String host = fragments[i].getHost().getName();
			if (set.contains(host) || target.containsPlugin(host)) {
				addDependencies(fragments[i], set);
			}
		}
		ITargetModelFactory factory = target.getModel().getFactory();
		Iterator iter = set.iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			ITargetPlugin plugin = factory.createPlugin();
			plugin.setId(id);
			target.addPlugin(plugin);
		}
	}
	
	private static void addDependencies(BundleDescription desc, Set set) {
		if (desc == null)
			return;
		
		String id = desc.getSymbolicName();
		if (!set.add(id))
			return;

		
		if (desc.getHost() != null) {
			addDependencies((BundleDescription)desc.getHost().getSupplier(), set);
		} else {
			if (desc != null && !"org.eclipse.ui.workbench".equals(desc.getSymbolicName())) { //$NON-NLS-1$
				BundleDescription[] fragments = desc.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					addDependencies(fragments[i], set);
				}
			}
		}
		
		BundleSpecification[] requires = desc.getRequiredBundles();
		for (int i = 0; i < requires.length; i++) {
			addDependencies((BundleDescription)requires[i].getSupplier(), set);
		}
	}
	
	private static BundleDescription[] getAllFragments() {
		ArrayList list = new ArrayList();
		BundleDescription[] bundles = TargetPlatform.getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getHost() != null)
				list.add(bundles[i]);
		}
		return (BundleDescription[])list.toArray(new BundleDescription[list.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object[] objects = e.getChangedObjects();
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			for (int i = 0; i < objects.length; i++) {
				if ((objects[i] instanceof ITargetPlugin && fLastTab == 0) ||
						(objects[i] instanceof ITargetFeature && fLastTab == 1)) {
					fContentViewer.add(objects[i]);
				}
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			for (int i = 0; i < objects.length; i++) {
				if ((objects[i] instanceof ITargetPlugin && fLastTab == 0) ||
						(objects[i] instanceof ITargetFeature && fLastTab == 1)) {
					fContentViewer.remove(objects[i]);
				}
			}
		}
		if (e.getChangedProperty() == ITarget.P_ALL_PLUGINS)
			refresh();
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		} 	
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = (IStructuredSelection)fContentViewer.getSelection();
		if (ssel == null)
			return;
		
		Action openAction = new Action("Open") { 
			public void run() {
				handleDoubleClick((IStructuredSelection)fContentViewer.getSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);
		
		manager.add(new Separator());
		
		Action removeAction = new Action("Remove") { 
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);
		
		Action removeAll = new Action("Remove All") { 
			public void run() {
				handleRemoveAll();
			}
		};
		removeAll.setEnabled(isEditable());
		manager.add(removeAll);

		manager.add(new Separator());
		
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	protected void doPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof ITargetPlugin && fLastTab == 0)
				getTarget().addPlugin((ITargetPlugin)objects[i]);	
			else if (objects[i] instanceof ITargetFeature && fLastTab == 1)
				getTarget().addFeature((ITargetFeature)objects[i]);
		}
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
}
