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
package org.eclipse.pde.internal.ui.editor.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.IPluginModelListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelDelta;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.plugin.NewFragmentProjectWizard;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;


public class PluginSection extends TableSection implements IPluginModelListener{
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return getProduct().getPlugins();
		}
	}

	private TableViewer fPluginTable;

	public PluginSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels());
	}
	
	private static String[] getButtonLabels() {
		String[] labels = new String[9];
		labels[0] = PDEUIMessages.Product_PluginSection_add; //$NON-NLS-1$
		labels[1] = PDEUIMessages.Product_PluginSection_working; //$NON-NLS-1$
		labels[2] = PDEUIMessages.Product_PluginSection_required; //$NON-NLS-1$
		labels[3] = PDEUIMessages.PluginSection_remove; //$NON-NLS-1$
		labels[4] = PDEUIMessages.Product_PluginSection_removeAll; //$NON-NLS-1$
		labels[5] = null;
		labels[6] = null;
		labels[7] = PDEUIMessages.Product_PluginSection_newPlugin; //$NON-NLS-1$
		labels[8] = PDEUIMessages.Product_PluginSection_newFragment; //$NON-NLS-1$
		return labels;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TablePart tablePart = getTablePart();
		fPluginTable = tablePart.getTableViewer();
		fPluginTable.setContentProvider(new ContentProvider());
		fPluginTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginTable.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IProductPlugin p1 = (IProductPlugin)e1;
				IProductPlugin p2 = (IProductPlugin)e2;
				return super.compare(viewer, p1.getId(), p2.getId());
			}
		});
		fPluginTable.setInput(PDECore.getDefault().getModelManager());
		
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(2, isEditable());
		tablePart.setButtonEnabled(3, isEditable());
		tablePart.setButtonEnabled(6, isEditable());
		tablePart.setButtonEnabled(7, isEditable());
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
		section.setText(PDEUIMessages.Product_PluginSection_title); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.Product_PluginSection_desc); //$NON-NLS-1$
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
			handleAddWorkingSet();
			break;
		case 2:
			handleAddRequired();
			break;
		case 3:
			handleDelete();
			break;
		case 4:
			handleRemoveAll();
			break;
		case 7:
			handleNewPlugin();
			break;
		case 8:
			handleNewFragment();
		}
	}
	
	private void handleNewFragment() {
		NewFragmentProjectWizard wizard = new NewFragmentProjectWizard();
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == WizardDialog.OK) {
			addPlugin(wizard.getFragmentId());
		}
	}

	private void handleNewPlugin() {
		NewPluginProjectWizard wizard = new NewPluginProjectWizard();
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == WizardDialog.OK) {
			addPlugin(wizard.getPluginId());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TableSection#handleDoubleClick(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
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
	
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IProductPlugin)
				return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = (IStructuredSelection)fPluginTable.getSelection();
		if (ssel == null)
			return;
		
		Action openAction = new Action(PDEUIMessages.PluginSection_open) { //$NON-NLS-1$
			public void run() {
				handleDoubleClick((IStructuredSelection)fPluginTable.getSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);
		
		manager.add(new Separator());
		
		Action removeAction = new Action(PDEUIMessages.PluginSection_remove) { //$NON-NLS-1$
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);
		
		Action removeAll = new Action(PDEUIMessages.PluginSection_removeAll) { //$NON-NLS-1$
			public void run() {
				handleRemoveAll();
			}
		};
		removeAll.setEnabled(isEditable());
		manager.add(removeAll);

		manager.add(new Separator());
		
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleOpen(IStructuredSelection selection) {
		Object object = selection.getFirstElement();
		if (object instanceof IProductPlugin) {
			ManifestEditor.openPluginEditor(((IProductPlugin)object).getId());
		}
	}

	private void handleAddRequired() {
		IProductPlugin[] plugins = getProduct().getPlugins();
		HashSet set = new HashSet();
		for (int i = 0; i < plugins.length; i++) {
			addDependencies(TargetPlatform.getState().getBundle(plugins[i].getId(), null), set);
		}
		BundleDescription[] fragments = getAllFragments();
		for (int i = 0; i < fragments.length; i++) {
			String id = fragments[i].getSymbolicName();
			if (set.contains(id) || "org.eclipse.ui.workbench.compatibility".equals(id))
				continue;
			String host = fragments[i].getHost().getName();
			if (set.contains(host) || getProduct().containsPlugin(host)) {
				addDependencies(fragments[i], set);
			}
		}
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		Iterator iter = set.iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			IProductPlugin plugin = factory.createPlugin();
			plugin.setId(id);
			product.addPlugin(plugin);
		}
	}
	
	private void addDependencies(BundleDescription desc, Set set) {
		if (desc == null)
			return;
		
		String id = desc.getSymbolicName();
		if (getProduct().containsPlugin(id) || !set.add(id))
			return;

		
		if (desc.getHost() != null) {
			addDependencies((BundleDescription)desc.getHost().getSupplier(), set);
		} else {
			if (desc != null && !"org.eclipse.ui.workbench".equals(desc.getSymbolicName())) {
				BundleDescription[] fragments = desc.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					addDependencies(fragments[i], set);
				}
			}
		}
		
		BundleDescription[] requires = desc.getResolvedRequires();
		for (int i = 0; i < requires.length; i++) {
			addDependencies(requires[i], set);
		}
	}
	
	private BundleDescription[] getAllFragments() {
		ArrayList list = new ArrayList();
		BundleDescription[] bundles = TargetPlatform.getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].getHost() != null)
				list.add(bundles[i]);
		}
		return (BundleDescription[])list.toArray(new BundleDescription[list.size()]);
	}
	
	private void handleAddWorkingSet() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), true);
		if (dialog.open() == Window.OK) {
			IWorkingSet[] workingSets = dialog.getSelection();
			IProduct product = getProduct();
			IProductModelFactory factory = product.getModel().getFactory();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IPluginModelBase model = findModel(elements[j]);
					if (model != null) {
						IProductPlugin plugin = factory.createPlugin();
						plugin.setId(model.getPluginBase().getId());
						product.addPlugin(plugin);						
					}
				}
			}
		}
	}
	
	private void handleRemoveAll() {
		TableItem[] items = fPluginTable.getTable().getItems();
		IProduct product = getProduct();
		for (int i = 0; i < items.length; i++) {
			product.removePlugin((IProductPlugin)items[i].getData());
		}
	}
	
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection)fPluginTable.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			IProduct product = getProduct();
			for (int i = 0; i < objects.length; i++) {
				product.removePlugin((IProductPlugin)objects[i]);
			}
		}
	}

	private void handleAdd() {	
		PluginSelectionDialog dialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getAvailableChoices(), true);
		if (dialog.open() == PluginSelectionDialog.OK) {
			Object[] models = dialog.getResult();
			for (int i = 0; i < models.length; i++) {
				addPlugin(((IPluginModelBase)models[i]).getPluginBase().getId());
			}
		}
	}
	
	private void addPlugin(String id) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductPlugin plugin = factory.createPlugin();
		plugin.setId(id);
		product.addPlugin(plugin);
	}
	
	private IPluginModelBase[] getAvailableChoices() {
		IPluginModelBase[] models = PDECore.getDefault().getModelManager ().getPlugins();
		IProduct product = getProduct();
		ArrayList list = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getPluginBase().getId();
			if (id != null && !product.containsPlugin(id)) {
				list.add(models[i]);
			}
		}
		return (IPluginModelBase[])list.toArray(new IPluginModelBase[list.size()]);
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();	
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
				if (objects[i] instanceof IProductPlugin)
					fPluginTable.add(objects[i]);
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IProductPlugin)
					fPluginTable.remove(objects[i]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fPluginTable.refresh();
		super.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
	 */
	public void modelsChanged(PluginModelDelta delta) {
		final Control control = fPluginTable.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						fPluginTable.refresh();
				}
			});
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
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
	}
	
	public boolean setFormInput(Object input) {
		if (input instanceof IProductPlugin) {
			fPluginTable.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}
	
	protected void doPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IProductPlugin)
				getProduct().addPlugin((IProductPlugin)objects[i]);		
		}
	}
	
}
