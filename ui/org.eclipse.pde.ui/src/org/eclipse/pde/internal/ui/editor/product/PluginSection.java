/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Alexander Kurtakov <akurtako@redhat.com> - bug 415649
 *     Simon Scholz <simon.scholz@vogella.com> - bug 440275, 444808
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487988
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.*;
import java.util.List;
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
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.dependencies.DependencyCalculator;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.plugin.NewFragmentProjectWizard;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginSection extends TableSection implements IPluginModelListener {

	class ContentProvider implements IStructuredContentProvider {
		@Override
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

		@Override
		public void run() {
			handleNewPlugin();
		}
	}

	class NewFragmentAction extends Action {

		public NewFragmentAction() {
			super(PDEUIMessages.Product_PluginSection_newFragment, IAction.AS_PUSH_BUTTON);
			setImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_TOOL);
		}

		@Override
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

	@Override
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
			@Override
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
		final Cursor handCursor = Display.getCurrent().getSystemCursor(SWT.CURSOR_HAND);
		toolbar.setCursor(handCursor);
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
			fIncludeOptionalButton.addSelectionListener(widgetSelectedAdapter(e -> {
				if (input instanceof IFileEditorInput) {
					IFile file = ((IFileEditorInput) input).getFile();
					try {
						file.setPersistentProperty(OPTIONAL_PROPERTY, fIncludeOptionalButton.getSelection() ? "true" : null); //$NON-NLS-1$
					} catch (CoreException e1) {
					}
				}
			}));
		}
	}

	@Override
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
		IStructuredSelection ssel = fPluginTable.getStructuredSelection();
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

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
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
		return super.doGlobalAction(actionId);
	}

	@Override
	protected boolean canPaste(Object target, Object[] objects) {
		for (Object object : objects) {
			if (object instanceof IProductPlugin)
				return true;
		}
		return false;
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = fPluginTable.getStructuredSelection();
		if (ssel == null)
			return;

		Action openAction = new Action(PDEUIMessages.PluginSection_open) {
			@Override
			public void run() {
				handleDoubleClick(fPluginTable.getStructuredSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);

		manager.add(new Separator());

		Action removeAction = new Action(PDEUIMessages.PluginSection_remove) {
			@Override
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && !ssel.isEmpty());
		manager.add(removeAction);

		Action removeAll = new Action(PDEUIMessages.PluginSection_removeAll) {
			@Override
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

		ArrayList<BundleDescription> list = new ArrayList<>(plugins.length);
		for (IProductPlugin plugin : plugins) {
			list.add(TargetPlatformHelper.getState().getBundle(plugin.getId(), null));
		}
		DependencyCalculator calculator = new DependencyCalculator(includeOptional);
		calculator.findDependencies(list.toArray());

		BundleDescription[] bundles = TargetPlatformHelper.getState().getBundles();
		for (BundleDescription bundle : bundles) {
			HostSpecification host = bundle.getHost();
			if (host != null && calculator.containsPluginId(host.getName())) {
				calculator.findDependency(bundle);
			}
		}

		Collection<?> dependencies = calculator.getBundleIDs();

		IProduct product = plugins[0].getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductPlugin[] requiredPlugins = new IProductPlugin[dependencies.size()];
		int i = 0;
		Iterator<?> iter = dependencies.iterator();
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
			ArrayList<IProductPlugin> pluginList = new ArrayList<>();
			for (IWorkingSet workingSet : workingSets) {
				IAdaptable[] elements = workingSet.getElements();
				for (IAdaptable element : elements) {
					IPluginModelBase model = findModel(element);
					if (model != null) {
						IProductPlugin plugin = factory.createPlugin();
						IPluginBase base = model.getPluginBase();
						plugin.setId(base.getId());
						pluginList.add(plugin);
					}
				}
			}
			product.addPlugins(pluginList.toArray(new IProductPlugin[pluginList.size()]));
		}
	}

	private void handleRemoveAll() {
		IProduct product = getProduct();
		product.removePlugins(product.getPlugins());
	}

	private void handleDelete() {
		IStructuredSelection ssel = fPluginTable.getStructuredSelection();
		if (!ssel.isEmpty()) {
			Object[] objects = ssel.toArray();
			IProductPlugin[] plugins = new IProductPlugin[objects.length];
			System.arraycopy(objects, 0, plugins, 0, objects.length);
			getProduct().removePlugins(plugins);
			updateRemoveButtons(true, true);
		}
	}

	private void handleAdd() {
		PluginSelectionDialog pluginSelectionDialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getBundles(), true);
		if (pluginSelectionDialog.open() == Window.OK) {
			Object[] result = pluginSelectionDialog.getResult();
			for (Object object : result) {
				IPluginModelBase pluginModelBase = (IPluginModelBase) object;
				addPlugin(pluginModelBase.getPluginBase().getId(), ICoreConstants.DEFAULT_VERSION);
			}
		}
	}

	private IPluginModelBase[] getBundles() {
		List<IPluginModelBase> pluginModelBaseList = new ArrayList<>();
		IProduct product = getProduct();
		BundleDescription[] bundles = TargetPlatformHelper.getState().getBundles();
		for (BundleDescription bundleDescription : bundles) {
			if (!product.containsPlugin(bundleDescription.getSymbolicName())) {
				IPluginModelBase pluginModel = PluginRegistry.findModel(bundleDescription);
				if (pluginModel != null) {
					pluginModelBaseList.add(pluginModel);
				}
			}
		}

		return pluginModelBaseList.toArray(new IPluginModelBase[pluginModelBaseList.size()]);
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

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
			return;
		}
		Object[] objects = e.getChangedObjects();
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			for (Object object : objects) {
				if (object instanceof IProductPlugin)
					fPluginTable.add(object);
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {

			Table table = fPluginTable.getTable();
			int index = table.getSelectionIndex();

			for (Object object : objects) {
				if (object instanceof IProductPlugin)
					fPluginTable.remove(object);
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

	@Override
	public void refresh() {
		fPluginTable.refresh();
		updateRemoveButtons(true, true);
		super.refresh();
	}

	@Override
	public void modelsChanged(PluginModelDelta delta) {
		final Control control = fPluginTable.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(() -> {
				if (!control.isDisposed()) {
					fPluginTable.refresh();
					updateRemoveButtons(true, true);
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

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateRemoveButtons(true, false);
	}

	@Override
	public boolean setFormInput(Object input) {
		if (input instanceof IProductPlugin) {
			fPluginTable.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	@Override
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

	@Override
	protected boolean createCount() {
		return true;
	}

	public boolean includeOptionalDependencies() {
		return fIncludeOptionalButton.getSelection();
	}
}
