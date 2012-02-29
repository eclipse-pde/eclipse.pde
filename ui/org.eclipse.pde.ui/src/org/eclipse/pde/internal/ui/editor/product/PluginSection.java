/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyCalculator;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.plugin.NewFragmentProjectWizard;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginSection extends TableSection implements IPluginModelListener {

	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return getProduct().getPlugins();
		}
	}

	private TableViewer fPluginTable;
	private Button fIncludeOptionalButton;
	private Action fNewPluginAction;
	private Action fNewFragmentAction;
	public static final QualifiedName OPTIONAL_PROPERTY = new QualifiedName(IPDEUIConstants.PLUGIN_ID, "product.includeOptional"); //$NON-NLS-1$

	class NewPluginAction extends Action {

		public NewPluginAction() {
			super(PDEUIMessages.Product_PluginSection_newPlugin, IAction.AS_PUSH_BUTTON);
			setImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_TOOL);
		}

		public void run() {
			handleNewPlugin();
		}
	}

	class NewFragmentAction extends Action {

		public NewFragmentAction() {
			super(PDEUIMessages.Product_PluginSection_newFragment, IAction.AS_PUSH_BUTTON);
			setImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_TOOL);
		}

		public void run() {
			handleNewFragment();
		}
	}

	public PluginSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels());
	}

	private static String[] getButtonLabels() {
		String[] labels = new String[6];
		labels[0] = PDEUIMessages.Product_PluginSection_add;
		labels[1] = PDEUIMessages.Product_PluginSection_working;
		labels[2] = PDEUIMessages.Product_PluginSection_required;
		labels[3] = PDEUIMessages.PluginSection_remove;
		labels[4] = PDEUIMessages.Product_PluginSection_removeAll;
		labels[5] = PDEUIMessages.Product_FeatureSection_properties;
		return labels;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData sectionData = new GridData(GridData.FILL_BOTH);
		sectionData.verticalSpan = 2;
		section.setLayoutData(sectionData);

		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		createOptionalDependenciesButton(container);

		TablePart tablePart = getTablePart();
		fPluginTable = tablePart.getTableViewer();
		fPluginTable.setContentProvider(new ContentProvider());
		fPluginTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		fPluginTable.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				IProductPlugin p1 = (IProductPlugin) e1;
				IProductPlugin p2 = (IProductPlugin) e2;
				return super.compare(viewer, p1.getId(), p2.getId());
			}
		});
		GridData data = (GridData) tablePart.getControl().getLayoutData();
		data.minimumWidth = 200;
		fPluginTable.setInput(getProduct());

		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(2, isEditable());

		// remove buttons will be updated on refresh

		tablePart.setButtonEnabled(5, isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);

		section.setText(PDEUIMessages.Product_PluginSection_title);
		section.setDescription(PDEUIMessages.Product_PluginSection_desc);
		getModel().addModelChangedListener(this);
		createSectionToolbar(section, toolkit);
	}

	private void createSectionToolbar(Section section, FormToolkit toolkit) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(section);
		final Cursor handCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
		// Cursor needs to be explicitly disposed
		toolbar.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				if ((handCursor != null) && (handCursor.isDisposed() == false)) {
					handCursor.dispose();
				}
			}
		});
		fNewPluginAction = new NewPluginAction();
		fNewFragmentAction = new NewFragmentAction();
		toolBarManager.add(fNewPluginAction);
		toolBarManager.add(fNewFragmentAction);

		toolBarManager.update(true);
		section.setTextClient(toolbar);
	}

	private void createOptionalDependenciesButton(Composite container) {
		if (isEditable()) {
			fIncludeOptionalButton = new Button(container, SWT.CHECK);
			fIncludeOptionalButton.setText(PDEUIMessages.PluginSection_includeOptional);
			// initialize value
			IEditorInput input = getPage().getEditorInput();
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				try {
					fIncludeOptionalButton.setSelection("true".equals(file.getPersistentProperty(OPTIONAL_PROPERTY))); //$NON-NLS-1$
				} catch (CoreException e) {
				}
			}
			// create listener to save value when the checkbox is changed
			fIncludeOptionalButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					IEditorInput input = getPage().getEditorInput();
					if (input instanceof IFileEditorInput) {
						IFile file = ((IFileEditorInput) input).getFile();
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
			case 0 :
				handleAdd();
				break;
			case 1 :
				handleAddWorkingSet();
				break;
			case 2 :
				handleAddRequired(getProduct().getPlugins(), fIncludeOptionalButton.getSelection());
				break;
			case 3 :
				handleDelete();
				break;
			case 4 :
				handleRemoveAll();
				break;
			case 5 :
				handleProperties();
				break;
		}
	}

	private void handleNewFragment() {
		NewFragmentProjectWizard wizard = new NewFragmentProjectWizard();
		wizard.init(PDEPlugin.getActiveWorkbenchWindow().getWorkbench(), null);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == Window.OK) {
			addPlugin(wizard.getFragmentId(), wizard.getFragmentVersion());
		}
	}

	private void handleNewPlugin() {
		NewPluginProjectWizard wizard = new NewPluginProjectWizard();
		wizard.init(PDEPlugin.getActiveWorkbenchWindow().getWorkbench(), null);
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == Window.OK) {
			addPlugin(wizard.getPluginId(), wizard.getPluginVersion());
		}
	}

	private void handleProperties() {
		IStructuredSelection ssel = (IStructuredSelection) fPluginTable.getSelection();
		if (ssel.size() == 1) {
			IProductPlugin plugin = (IProductPlugin) ssel.toArray()[0];
			VersionDialog dialog = new VersionDialog(PDEPlugin.getActiveWorkbenchShell(), isEditable(), plugin.getVersion());
			dialog.create();
			SWTUtil.setDialogSize(dialog, 400, 200);
			if (dialog.open() == Window.OK) {
				plugin.setVersion(dialog.getVersion());
			}
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
		IStructuredSelection ssel = (IStructuredSelection) fPluginTable.getSelection();
		if (ssel == null)
			return;

		Action openAction = new Action(PDEUIMessages.PluginSection_open) {
			public void run() {
				handleDoubleClick((IStructuredSelection) fPluginTable.getSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);

		manager.add(new Separator());

		Action removeAction = new Action(PDEUIMessages.PluginSection_remove) {
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);

		Action removeAll = new Action(PDEUIMessages.PluginSection_removeAll) {
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
			ManifestEditor.openPluginEditor(((IProductPlugin) object).getId());
		}
	}

	public static void handleAddRequired(IProductPlugin[] plugins, boolean includeOptional) {
		if (plugins.length == 0)
			return;

		ArrayList list = new ArrayList(plugins.length);
		for (int i = 0; i < plugins.length; i++) {
			list.add(TargetPlatformHelper.getState().getBundle(plugins[i].getId(), null));
		}
		DependencyCalculator calculator = new DependencyCalculator(includeOptional);
		calculator.findDependencies(list.toArray());

		BundleDescription[] bundles = TargetPlatformHelper.getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			HostSpecification host = bundles[i].getHost();
			if (host != null && !("org.eclipse.ui.workbench.compatibility".equals(bundles[i].getSymbolicName())) //$NON-NLS-1$
					&& calculator.containsPluginId(host.getName())) {
				calculator.findDependency(bundles[i]);
			}
		}

		Collection dependencies = calculator.getBundleIDs();

		IProduct product = plugins[0].getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductPlugin[] requiredPlugins = new IProductPlugin[dependencies.size()];
		int i = 0;
		Iterator iter = dependencies.iterator();
		while (iter.hasNext()) {
			String id = iter.next().toString();
			IProductPlugin plugin = factory.createPlugin();
			plugin.setId(id);
			requiredPlugins[i++] = plugin;
		}
		product.addPlugins(requiredPlugins);
	}

	private void handleAddWorkingSet() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), true);
		if (dialog.open() == Window.OK) {
			IWorkingSet[] workingSets = dialog.getSelection();
			IProduct product = getProduct();
			IProductModelFactory factory = product.getModel().getFactory();
			ArrayList pluginList = new ArrayList();
			for (int i = 0; i < workingSets.length; i++) {
				IAdaptable[] elements = workingSets[i].getElements();
				for (int j = 0; j < elements.length; j++) {
					IPluginModelBase model = findModel(elements[j]);
					if (model != null) {
						IProductPlugin plugin = factory.createPlugin();
						IPluginBase base = model.getPluginBase();
						plugin.setId(base.getId());
						pluginList.add(plugin);
					}
				}
			}
			product.addPlugins((IProductPlugin[]) pluginList.toArray(new IProductPlugin[pluginList.size()]));
		}
	}

	private void handleRemoveAll() {
		IProduct product = getProduct();
		product.removePlugins(product.getPlugins());
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) fPluginTable.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			IProductPlugin[] plugins = new IProductPlugin[objects.length];
			System.arraycopy(objects, 0, plugins, 0, objects.length);
			getProduct().removePlugins(plugins);
			updateRemoveButtons(true, true);
		}
	}

	private void handleAdd() {
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin.getDefault().getLabelProvider());
		dialog.setElements(getBundles());
		dialog.setTitle(PDEUIMessages.PluginSelectionDialog_title);
		dialog.setMessage(PDEUIMessages.PluginSelectionDialog_message);
		dialog.setMultipleSelection(true);
		if (dialog.open() == Window.OK) {
			Object[] bundles = dialog.getResult();
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription desc = (BundleDescription) bundles[i];
				addPlugin(desc.getSymbolicName(), "0.0.0"); //$NON-NLS-1$
				//addPlugin(desc.getSymbolicName(), desc.getVersion().toString());
			}
		}
	}

	private BundleDescription[] getBundles() {
		TreeMap map = new TreeMap();
		IProduct product = getProduct();
		BundleDescription[] bundles = TargetPlatformHelper.getState().getBundles();
		for (int i = 0; i < bundles.length; i++) {
			String id = bundles[i].getSymbolicName();
			if (!product.containsPlugin(id)) {
				map.put(id, bundles[i]);
			}
		}
		return (BundleDescription[]) map.values().toArray(new BundleDescription[map.size()]);
	}

	private void addPlugin(String id, String version) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductPlugin plugin = factory.createPlugin();
		plugin.setId(id);
		plugin.setVersion(version);
		product.addPlugins(new IProductPlugin[] {plugin});
		fPluginTable.setSelection(new StructuredSelection(plugin));
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
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
			return;
		}
		Object[] objects = e.getChangedObjects();
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IProductPlugin)
					fPluginTable.add(objects[i]);
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {

			Table table = fPluginTable.getTable();
			int index = table.getSelectionIndex();

			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IProductPlugin)
					fPluginTable.remove(objects[i]);
			}

			// Update Selection

			int count = table.getItemCount();

			if (count == 0) {
				// Nothing to select
			} else if (index < count) {
				table.setSelection(index);
			} else {
				table.setSelection(count - 1);
			}

		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			fPluginTable.refresh();
		}
		updateRemoveButtons(false, true);
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		// This section can get disposed if the configuration is changed from
		// plugins to features or vice versa.  Subsequently, the configuration
		// page is removed and readded.  In this circumstance, abort the
		// refresh		
		if (fPluginTable.getTable().isDisposed()) {
			return;
		}
		// Reload the input
		fPluginTable.setInput(getProduct());
		// Perform the refresh
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fPluginTable.refresh();
		updateRemoveButtons(true, true);
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
					if (!control.isDisposed()) {
						fPluginTable.refresh();
						updateRemoveButtons(true, true);
					}
				}
			});
		}
	}

	private IPluginModelBase findModel(IAdaptable object) {
		if (object instanceof IJavaProject)
			object = ((IJavaProject) object).getProject();
		if (object instanceof IProject)
			return PluginRegistry.findModel((IProject) object);
		if (object instanceof PersistablePluginObject) {
			return PluginRegistry.findModel(((PersistablePluginObject) object).getPluginID());
		}
		return null;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateRemoveButtons(true, false);
	}

	public boolean setFormInput(Object input) {
		if (input instanceof IProductPlugin) {
			fPluginTable.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	protected void doPaste(Object target, Object[] objects) {
		IProductPlugin[] plugins;
		if (objects instanceof IProductPlugin[])
			plugins = (IProductPlugin[]) objects;
		else {
			plugins = new IProductPlugin[objects.length];
			for (int i = 0; i < objects.length; i++)
				if (objects[i] instanceof IProductPlugin)
					plugins[i] = (IProductPlugin) objects[i];
		}
		getProduct().addPlugins(plugins);
	}

	private void updateRemoveButtons(boolean updateRemove, boolean updateRemoveAll) {
		TablePart tablePart = getTablePart();
		Table table = tablePart.getTableViewer().getTable();
		TableItem[] tableSelection = table.getSelection();
		if (updateRemove) {
			ISelection selection = getViewerSelection();
			tablePart.setButtonEnabled(3, isEditable() && !selection.isEmpty() && selection instanceof IStructuredSelection && ((IStructuredSelection) selection).getFirstElement() instanceof IProductPlugin);
		}
		int count = fPluginTable.getTable().getItemCount();
		if (updateRemoveAll)
			tablePart.setButtonEnabled(4, isEditable() && count > 0);
		tablePart.setButtonEnabled(2, isEditable() && count > 0);
		tablePart.setButtonEnabled(5, isEditable() && tableSelection.length == 1);
	}

	protected boolean createCount() {
		return true;
	}

	public boolean includeOptionalDependencies() {
		return fIncludeOptionalButton.getSelection();
	}
}
