package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.plugin.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;


public class PluginSection extends TableSection implements IPluginModelListener{
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return getProduct().getPlugins();
		}
	}

	private TableViewer fPluginTable;
	private Button fIncludeFragments;

	public PluginSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels());
	}
	
	private static String[] getButtonLabels() {
		String[] labels = new String[9];
		labels[0] = PDEPlugin.getResourceString("Product.PluginSection.add"); //$NON-NLS-1$
		labels[1] = PDEPlugin.getResourceString("Product.PluginSection.working"); //$NON-NLS-1$
		labels[2] = PDEPlugin.getResourceString("Product.PluginSection.required"); //$NON-NLS-1$
		labels[3] = PDEPlugin.getResourceString("PluginSection.remove"); //$NON-NLS-1$
		labels[4] = PDEPlugin.getResourceString("Product.PluginSection.removeAll"); //$NON-NLS-1$
		labels[5] = null;
		labels[6] = null;
		labels[7] = PDEPlugin.getResourceString("Product.PluginSection.newPlugin"); //$NON-NLS-1$
		labels[8] = PDEPlugin.getResourceString("Product.PluginSection.newFragment"); //$NON-NLS-1$
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
		
		fIncludeFragments = toolkit.createButton(container, PDEPlugin.getResourceString("Product.PluginSection.includeFragments"), SWT.CHECK); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fIncludeFragments.setLayoutData(gd);
		fIncludeFragments.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getProduct().setIncludeFragments(fIncludeFragments.getSelection());
			}
		});
		fIncludeFragments.setEnabled(isEditable());
		
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(2, isEditable());
		tablePart.setButtonEnabled(3, isEditable());
		tablePart.setButtonEnabled(6, isEditable());
		tablePart.setButtonEnabled(7, isEditable());
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
		section.setText(PDEPlugin.getResourceString("Product.PluginSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("Product.PluginSection.desc")); //$NON-NLS-1$
		getProduct().getModel().addModelChangedListener(this);
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
		getProduct().getModel().removeModelChangedListener(this);
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
		
		Action openAction = new Action(PDEPlugin.getResourceString("PluginSection.open")) { //$NON-NLS-1$
			public void run() {
				handleDoubleClick((IStructuredSelection)fPluginTable.getSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);
		
		manager.add(new Separator());
		
		Action removeAction = new Action(PDEPlugin.getResourceString("PluginSection.remove")) { //$NON-NLS-1$
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);
		
		Action removeAll = new Action(PDEPlugin.getResourceString("PluginSection.removeAll")) { //$NON-NLS-1$
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
			addDependencies(plugins[i].getId(), set);
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
	
	private void addDependencies(String pluginId, Set set) {
		IProduct product = getProduct();
		
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model == null) {
			set.remove(pluginId);
			return;
		}
		
		if (model instanceof IFragmentModel) {
			String hostId = ((IFragmentModel) model).getFragment().getPluginId();
			if (!product.containsPlugin(hostId) && set.add(hostId))
				addDependencies(hostId, set);
		} else {
			boolean addFragments = fIncludeFragments.getSelection()
					|| ((IPlugin)model.getPluginBase()).hasExtensibleAPI();
			if (!addFragments) {
				IPluginLibrary[] libs = model.getPluginBase().getLibraries();
				for (int i = 0; i < libs.length; i++) {
					if (ClasspathUtilCore.containsVariables(libs[i].getName())) {
						addFragments = true;
						break;
					}
				}
			}
			if (addFragments) {
				IFragment[] fragments = PDECore.getDefault().findFragmentsFor(pluginId, model.getPluginBase().getVersion());
				for (int i = 0; i < fragments.length; i++) {
					String fragmentId = fragments[i].getId();
					if (!product.containsPlugin(fragmentId) && set.add(fragmentId))
						addDependencies(fragmentId, set);
				}
			}
		}
		
		IPluginImport[] imports = model.getPluginBase().getImports();
		for (int i = 0; i < imports.length; i++) {
			String id = imports[i].getId();
			if (!product.containsPlugin(id) && set.add(id)) {
				addDependencies(id, set);
			}
		}
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
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
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
		fIncludeFragments.setSelection(getProduct().includeFragments());
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
