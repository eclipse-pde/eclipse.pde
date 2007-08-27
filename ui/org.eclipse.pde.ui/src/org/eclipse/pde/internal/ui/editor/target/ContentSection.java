/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 187646
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetFeature;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.itarget.ITargetModelFactory;
import org.eclipse.pde.internal.core.itarget.ITargetPlugin;
import org.eclipse.pde.internal.core.plugin.ExternalPluginModelBase;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.ConditionalListSelectionDialog;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyCalculator;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.wizards.FeatureSelectionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ContentSection extends TableSection {
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			ITarget target = getTarget();
			if (target.useAllPlugins())
				return new Object[0];
			if (fLastTab == 0)
				return target.getPlugins();
			return target.getFeatures();
		}
	}
	
	private static final String[] TAB_LABELS = new String[2];
	static {
		TAB_LABELS[0] = PDEUIMessages.ContentSection_plugins;
		TAB_LABELS[1] = PDEUIMessages.ContentSection_features;
	}
	
	private static final String[] BUTTONS = new String[5];
	static {
		BUTTONS[0] = PDEUIMessages.ContentSection_add;
		BUTTONS[1] = PDEUIMessages.ContentSection_remove;
		BUTTONS[2] = PDEUIMessages.ContentSection_removeAll;
		BUTTONS[3] = PDEUIMessages.ContentSection_workingSet;
		BUTTONS[4] = PDEUIMessages.ContentSection_required;
	}
	
	private TableViewer fContentViewer;
	private CTabFolder fTabFolder;
	private int fLastTab;
	private Button fUseAllPlugins;
	private Image[] fTabImages;
	private Button fIncludeOptionalButton;
	public static final QualifiedName OPTIONAL_PROPERTY = new QualifiedName(IPDEUIConstants.PLUGIN_ID, "target.includeOptional"); //$NON-NLS-1$
	
	public ContentSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, BUTTONS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fUseAllPlugins = toolkit.createButton(client, PDEUIMessages.ContentSection_allTarget, SWT.CHECK);
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
		Color selectedColor = toolkit.getColors().getColor(IFormColors.TB_BG);
		fTabFolder.setSelectionBackground(new Color[] { selectedColor,
				toolkit.getColors().getBackground() },
				new int[] { 100 }, true);
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
		fContentViewer.setComparator(new ViewerComparator() {
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
		fContentViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		
		createOptionalDependenciesButton(client);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		section.setText(PDEUIMessages.ContentSection_targetContent);
		section.setDescription(PDEUIMessages.ContentSection_targetContentDesc);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		updateButtons();
		getModel().addModelChangedListener(this);
	}
	
	private void createOptionalDependenciesButton(Composite client) {
		if (isEditable()) {
			fIncludeOptionalButton = new Button(client, SWT.CHECK);
			fIncludeOptionalButton.setText(PDEUIMessages.ContentSection_includeOptional);
			// initialize value
			IEditorInput input = getPage().getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput)input).getFile();
				try {
					fIncludeOptionalButton.setSelection("true".equals(file.getPersistentProperty(OPTIONAL_PROPERTY))); //$NON-NLS-1$
				} catch (CoreException e) {
				}
			}
			fIncludeOptionalButton.setEnabled(!getTarget().useAllPlugins());
			// create listener to save value when the checkbox is changed
			fIncludeOptionalButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					IEditorInput input = getPage().getEditorInput();
					if (input instanceof IFileEditorInput) {
						IFile file = ((IFileEditorInput)input).getFile();
						try {
							file.setPersistentProperty(OPTIONAL_PROPERTY, fIncludeOptionalButton.getSelection() ? "true" : null); //$NON-NLS-1$
						} catch (CoreException e1) {
						}
					}
				}
			});
		}
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
			handleAddRequired(getTarget().getPlugins(), fIncludeOptionalButton.getSelection());
		}
	}
	
	private void createTabs() {
		fTabImages = new Image[] {PDEPluginImages.DESC_PLUGIN_OBJ.createImage(), 
				PDEPluginImages.DESC_FEATURE_OBJ.createImage()
		};
		for (int i = 0; i < TAB_LABELS.length; i++) {
			CTabItem item = new CTabItem(fTabFolder, SWT.NULL);
			item.setText(TAB_LABELS[i]);
			item.setImage(fTabImages[i]);
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
		updateButtons();
		super.refresh();
	}
	
	protected void updateButtons(){
		boolean useAllPlugins = getTarget().useAllPlugins();
		fUseAllPlugins.setSelection(useAllPlugins);
		fTabFolder.setEnabled(!useAllPlugins);
		TablePart table = getTablePart();
		boolean itemsSelected = !fContentViewer.getSelection().isEmpty();
		boolean hasItems = fContentViewer.getTable().getItemCount() > 0;
		table.setButtonEnabled(0, isEditable() && !useAllPlugins);
		table.setButtonEnabled(1, isEditable() && !useAllPlugins && itemsSelected);
		table.setButtonEnabled(2, isEditable() && !useAllPlugins && hasItems);
		boolean pluginTab = (fLastTab == 0);
		table.setButtonEnabled(3, isEditable() && pluginTab && !useAllPlugins);
		table.setButtonEnabled(4, isEditable() && pluginTab && !useAllPlugins && hasItems);
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
		updateButtons();
	}
	
	private void handleAddPlugin() {
		ConditionalListSelectionDialog dialog = new ConditionalListSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(), 
				PDEPlugin.getDefault().getLabelProvider(),
				PDEUIMessages.ContentSection_addDialogButtonLabel);
		
		TreeMap map = getBundles();
		dialog.setElements(map.values().toArray());
		dialog.setConditionalElements(getWorkspaceBundles(map).values().toArray());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title); 
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] bundles = dialog.getResult();
			ITarget target = getTarget();
			ITargetModelFactory factory = getModel().getFactory();
			ITargetPlugin[] plugins = new ITargetPlugin[bundles.length];
			for (int i = 0; i < bundles.length; i++) {
				String id = ((BundleDescription)bundles[i]).getSymbolicName();
				ITargetPlugin plugin = factory.createPlugin();
				plugin.setId(id);
				plugins[i] = plugin;
			}
			target.addPlugins(plugins);
			fContentViewer.setSelection(new StructuredSelection(plugins[plugins.length - 1]));
		}
	}
	
	private TreeMap getBundles() {
		TreeMap map = new TreeMap();
		ITarget target = getTarget();
		IPluginModelBase[] models = PluginRegistry.getExternalModels();
		for (int i = 0; i < models.length; i++) {
			BundleDescription desc = ((ExternalPluginModelBase)models[i]).getBundleDescription();
			String id = desc.getSymbolicName();
			if (!target.containsPlugin(id))
				map.put(id, desc);
		}
		return map;
	}
	
	protected TreeMap getWorkspaceBundles(TreeMap used) {
		TreeMap map = new TreeMap();
		ITarget target = getTarget();
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		for (int i = 0; i < models.length; i++) {
			BundleDescription desc = models[i].getBundleDescription();
			String id = desc.getSymbolicName();
			if (id != null && !target.containsPlugin(id) && !used.containsKey(id))
				map.put(id, desc);
		}
		return map;
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
			ITargetFeature [] features = new ITargetFeature[models.length];
			for (int i = 0; i < models.length; ++i) {
				IFeature feature = ((IFeatureModel)models[i]).getFeature();
				String id = feature.getId();
				ITargetFeature tfeature = factory.createFeature();
				tfeature.setId(id);
				features[i] = tfeature;
			}
			target.addFeatures(features);
			fContentViewer.setSelection(new StructuredSelection(features[features.length - 1]));
		}
	}
	
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection)fContentViewer.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			ITarget target = getTarget();
			if (fLastTab == 0) {
				ITargetPlugin[] plugins = new ITargetPlugin[objects.length];
				System.arraycopy(objects, 0, plugins, 0, objects.length);
				target.removePlugins(plugins);
			} else {
				ITargetFeature[] features = new ITargetFeature[objects.length];
				System.arraycopy(objects, 0, features, 0, objects.length);
				target.removeFeatures(features);
			}
		}
		updateButtons();
	}
	
	private void handleRemoveAll() {
		TableItem[] items = fContentViewer.getTable().getItems();
		ITarget target = getTarget();
		if (fLastTab == 0) {
			ITargetPlugin[] plugins = new ITargetPlugin[items.length];
			for (int i = 0; i < plugins.length; i++)
				plugins[i] = (ITargetPlugin)items[i].getData();
			target.removePlugins(plugins);
		} else {
			ITargetFeature[] features = new ITargetFeature[items.length];
			for (int i = 0; i < features.length; i++)
				features[i] = (ITargetFeature)items[i].getData();
			target.removeFeatures(features);
		}
		updateButtons();
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
			HashSet plugins = new HashSet();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IPluginModelBase model = findModel(elements[j]);
					if (model != null) {
						ITargetPlugin plugin = factory.createPlugin();
						plugin.setId(model.getPluginBase().getId());
						plugins.add(plugin);						
					}
				}
			}
			target.addPlugins((ITargetPlugin[]) plugins.toArray(new ITargetPlugin[plugins.size()]));
		}
		updateButtons();
	}
	
	private IPluginModelBase findModel(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject)object).getProject();
		if (object instanceof IProject)
			return PluginRegistry.findModel((IProject)object);
		if (object instanceof PersistablePluginObject) {
			return PluginRegistry.findModel(((PersistablePluginObject)object).getPluginID());
		}
		return null;
	}
	
	public static void handleAddRequired(ITargetPlugin[] plugins, boolean includeOptional) {
		if (plugins.length == 0)
			return;
		
		ArrayList list = new ArrayList(plugins.length);
		for (int i = 0; i < plugins.length; i++) {
			list.add(TargetPlatformHelper.getState().getBundle(plugins[i].getId(), null));
		}
		DependencyCalculator calculator = new DependencyCalculator(includeOptional);
		calculator.findDependencies(list.toArray());
		Collection dependencies = calculator.getBundleIDs();
		
		ITarget target = plugins[0].getTarget();
		ITargetModelFactory factory = target.getModel().getFactory();
		ITargetPlugin[] pluginsToAdd = new ITargetPlugin[dependencies.size()];
		int i = 0;
		Iterator iter = dependencies.iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			ITargetPlugin plugin = factory.createPlugin();
			plugin.setId(id);
			pluginsToAdd[i++] = plugin;
		}
		target.addPlugins(pluginsToAdd);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
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
			
			Table table = fContentViewer.getTable();
			int index = table.getSelectionIndex();			
			
			for (int i = 0; i < objects.length; i++) {
				if ((objects[i] instanceof ITargetPlugin && fLastTab == 0) ||
						(objects[i] instanceof ITargetFeature && fLastTab == 1)) {
					fContentViewer.remove(objects[i]);
				}
			}
			
			// Update Selection

			int count = table.getItemCount();
				
			if ( count == 0 ) {
				// Nothing to select
			} else if ( index < count ) {
				table.setSelection( index );
			} else {
				table.setSelection( count - 1 );
			}	
			
		}
		if (e.getChangedProperty() == ITarget.P_ALL_PLUGINS) {
			refresh();
			fIncludeOptionalButton.setEnabled(!((Boolean)e.getNewValue()).booleanValue());
		}
	}
	
	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// Reload input
		fContentViewer.setInput(PDECore.getDefault().getModelManager());
		// Perform the refresh
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
		if (actionId.equals(ActionFactory.SELECT_ALL.getId())) {
			handleSelectAll();
			return true;
		}
		return false;
	}

	private void handleSelectAll() {
		fContentViewer.getTable().selectAll();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = (IStructuredSelection)fContentViewer.getSelection();
		if (ssel == null)
			return;
		
		Action openAction = new Action(PDEUIMessages.ContentSection_open) { 
			public void run() {
				handleDoubleClick((IStructuredSelection)fContentViewer.getSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);
		
		manager.add(new Separator());
		
		Action removeAction = new Action(PDEUIMessages.ContentSection_remove) { 
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);
		
		Action removeAll = new Action(PDEUIMessages.ContentSection_removeAll) { 
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
	
	public boolean setFormInput(Object input) {
		if (input instanceof ITargetPlugin) {
			if (fTabFolder.getSelectionIndex() != 0) {
				fTabFolder.setSelection(0);
				refresh();
			}
			fContentViewer.setSelection(new StructuredSelection(input), true);
			return true;
		} else if (input instanceof ITargetFeature) {
			if (fTabFolder.getSelectionIndex() != 1) {
				fTabFolder.setSelection(1);
				refresh();
			}
			fContentViewer.setSelection(new StructuredSelection(input), true);
			return true;			
		}
		return super.setFormInput(input);
	}

	
	public void dispose() {
		ITargetModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		if (fTabImages != null)
			for (int i = 0; i < fTabImages.length; i++) 
				fTabImages[i].dispose();
		super.dispose();
	}
	
	protected boolean createCount() { return true; }
}
