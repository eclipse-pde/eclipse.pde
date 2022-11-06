/*******************************************************************************
 * Copyright (c) 2005, 2023 IBM Corporation and others.
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
 *     Hannes Wellmann - Bug 570760 - Option to automatically add requirements to product-launch
 *     Hannes Wellmann - Unify and clean-up Product Editor's PluginSection and FeatureSection
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.DependencyManager;
import org.eclipse.pde.internal.core.DependencyManager.Options;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.plugin.NewFragmentProjectWizard;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * Section of the product editor on the {@code Contents} page that lists all
 * required Plug-ins of this Plug-in-based product.
 */
public class PluginSection extends AbstractProductContentSection<PluginSection> {

	private static final List<String> BUTTON_LABELS;
	private static final List<Consumer<PluginSection>> BUTTON_HANDLERS;

	private static final int BTN_ADD;
	private static final int BTN_ADD_WORKING_SET;
	private static final int BTN_ADD_REQUIRED;
	private static final int BTN_REMOVE;
	private static final int BTN_REMOVE_ALL;
	private static final int BTN_PROPS;

	static {
		List<String> labels = new ArrayList<>();
		List<Consumer<PluginSection>> handlers = new ArrayList<>();

		BTN_ADD = addButton(PDEUIMessages.Product_PluginSection_add, PluginSection::handleAdd, labels, handlers);
		BTN_ADD_WORKING_SET = addButton(PDEUIMessages.Product_PluginSection_working, PluginSection::handleAddWorkingSet,
				labels, handlers);
		BTN_ADD_REQUIRED = addButton(PDEUIMessages.Product_PluginSection_required,
				s -> handleAddRequired(s.getProduct().getPlugins(), s.fIncludeOptionalButton.getSelection()), labels,
				handlers);
		BTN_REMOVE = addButton(PDEUIMessages.PluginSection_remove, PluginSection::handleRemove, labels, handlers);
		BTN_REMOVE_ALL = addButton(PDEUIMessages.Product_PluginSection_removeAll, PluginSection::handleRemoveAll,
				labels, handlers);
		BTN_PROPS = addButton(PDEUIMessages.Product_FeatureSection_properties, PluginSection::handleProperties, labels,
				handlers);

		BUTTON_LABELS = List.copyOf(labels);
		BUTTON_HANDLERS = List.copyOf(handlers);
	}

	private Button fIncludeOptionalButton;
	private static final QualifiedName OPTIONAL_PROPERTY = new QualifiedName(IPDEUIConstants.PLUGIN_ID,
			"product.includeOptional"); //$NON-NLS-1$

	public PluginSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, BUTTON_LABELS, BUTTON_HANDLERS, IProductPlugin.class::isInstance);
	}

	@Override
	void populateSection(Section section, Composite container, FormToolkit toolkit) {

		createAutoIncludeRequirementsButton(container, PDEUIMessages.Product_PluginSection_autoIncludeRequirements);

		new Label(container, SWT.NONE); // fills column 2
		createOptionalDependenciesButton(container);

		configureTable(IProduct::getPlugins, new ViewerComparator());

		enableTableButtons(BTN_ADD, BTN_ADD_WORKING_SET, BTN_ADD_REQUIRED, BTN_PROPS);
		// remove buttons will be updated on refresh

		section.setText(PDEUIMessages.Product_PluginSection_title);
		section.setDescription(PDEUIMessages.Product_PluginSection_desc);
	}

	private void createOptionalDependenciesButton(Composite container) {
		if (isEditable()) {
			fIncludeOptionalButton = new Button(container, SWT.CHECK);
			fIncludeOptionalButton.setText(PDEUIMessages.PluginSection_includeOptional);
			// initialize value
			IEditorInput input = getPage().getEditorInput();
			if (input instanceof IFileEditorInput fileEditorInput) {
				IFile file = fileEditorInput.getFile();
				try {
					fIncludeOptionalButton.setSelection("true".equals(file.getPersistentProperty(OPTIONAL_PROPERTY))); //$NON-NLS-1$
				} catch (CoreException e) {
				}
			}
			// create listener to save value when the checkbox is changed
			fIncludeOptionalButton.addSelectionListener(widgetSelectedAdapter(e -> {
				if (input instanceof IFileEditorInput fileEditorInput) {
					IFile file = fileEditorInput.getFile();
					try {
						file.setPersistentProperty(OPTIONAL_PROPERTY,
								fIncludeOptionalButton.getSelection() ? "true" : null); //$NON-NLS-1$
					} catch (CoreException e1) {
					}
				}
			}));
		}
	}

	@Override
	List<Action> getToolbarActions() {
		Action newPluginAction = createPushAction(PDEUIMessages.Product_PluginSection_newPlugin,
				PDEPluginImages.DESC_NEWPPRJ_TOOL, () -> handleNewPlugin());
		Action newFragmentAction = createPushAction(PDEUIMessages.Product_PluginSection_newFragment,
				PDEPluginImages.DESC_NEWFRAGPRJ_TOOL, () -> handleNewFragment());
		return List.of(newPluginAction, newFragmentAction);
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
		IStructuredSelection ssel = getTableSelection();
		if (ssel.size() == 1 && ssel.getFirstElement() instanceof IProductPlugin plugin) {
			VersionDialog dialog = new VersionDialog(PDEPlugin.getActiveWorkbenchShell(), isEditable(),
					plugin.getVersion());
			dialog.create();
			SWTUtil.setDialogSize(dialog, 400, 200);
			if (dialog.open() == Window.OK) {
				plugin.setVersion(dialog.getVersion());
			}
		}
	}

	@Override
	protected void handleDoubleClick(IStructuredSelection selection) {
		if (selection.getFirstElement() instanceof IProductPlugin plugin) {
			ManifestEditor.openPluginEditor(plugin.getId());
		}
	}

	public static void handleAddRequired(IProductPlugin[] plugins, boolean includeOptional) {
		if (plugins.length == 0) {
			return;
		}
		List<BundleDescription> list = Stream.of(plugins).map(plugin -> {
			String version = VersionUtil.isEmptyVersion(plugin.getVersion()) ? null : plugin.getVersion();
			return PluginRegistry.findModel(plugin.getId(), version, IMatchRules.PERFECT, null);
		}).filter(Objects::nonNull).map(IPluginModelBase::getBundleDescription).toList();

		DependencyManager.Options[] options = includeOptional
				? new Options[] { Options.INCLUDE_NON_TEST_FRAGMENTS, Options.INCLUDE_OPTIONAL_DEPENDENCIES }
				: new Options[] { Options.INCLUDE_NON_TEST_FRAGMENTS };
		Set<BundleDescription> dependencies = DependencyManager.findRequirementsClosure(list, options);

		IProduct product = plugins[0].getProduct();
		addPluginsWithSymbolicName(product, dependencies.stream().map(BundleDescription::getSymbolicName));
	}

	private void handleAddWorkingSet() {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = manager.createWorkingSetSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				true);
		if (dialog.open() == Window.OK) {
			IWorkingSet[] workingSets = dialog.getSelection();
			Stream<String> plugins = Stream.of(workingSets).flatMap(ws -> Stream.of(ws.getElements()))
					.map(this::findModel).filter(Objects::nonNull).map(model -> model.getPluginBase().getId());
			addPluginsWithSymbolicName(getProduct(), plugins);
		}
	}

	private static void addPluginsWithSymbolicName(IProduct product, Stream<String> pluginIds) {
		IProductModelFactory factory = product.getModel().getFactory();
		IProductPlugin[] plugins = pluginIds.map(symbolicName -> {
			IProductPlugin plugin = factory.createPlugin();
			plugin.setId(symbolicName);
			return plugin;
		}).toArray(IProductPlugin[]::new);
		product.addPlugins(plugins);
	}

	@Override
	void handleRemoveAll() {
		IProduct product = getProduct();
		product.removePlugins(product.getPlugins());
	}

	@Override
	protected void doPaste(Object target, Object[] objects) {
		IProductPlugin[] plugins = filterToArray(Stream.of(objects), IProductPlugin.class);
		getProduct().addPlugins(plugins);
	}

	@Override
	void removeElements(IProduct product, List<Object> elements) {
		IProductPlugin[] plugins = filterToArray(elements.stream(), IProductPlugin.class);
		getProduct().removePlugins(plugins);
	}

	private void handleAdd() {
		PluginSelectionDialog pluginSelectionDialog = new PluginSelectionDialog(PDEPlugin.getActiveWorkbenchShell(),
				getBundles(getProduct()), true);
		if (pluginSelectionDialog.open() == Window.OK) {
			Object[] result = pluginSelectionDialog.getResult();
			for (Object object : result) {
				IPluginModelBase pluginModelBase = (IPluginModelBase) object;
				addPlugin(pluginModelBase.getPluginBase().getId(), ICoreConstants.DEFAULT_VERSION);
			}
		}
	}

	private static IPluginModelBase[] getBundles(IProduct product) {
		List<IPluginModelBase> pluginModelBaseList = new ArrayList<>();
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
		product.addPlugins(new IProductPlugin[] { plugin });
		getTableViewer().setSelection(new StructuredSelection(plugin));
	}

	private IPluginModelBase findModel(IAdaptable object) {
		if (object instanceof IJavaProject javaProject) {
			object = javaProject.getProject();
		}
		if (object instanceof IProject project) {
			return PluginRegistry.findModel(project);
		} else if (object instanceof PersistablePluginObject pluginObject) {
			return PluginRegistry.findModel(pluginObject.getPluginID());
		}
		return null;
	}

	@Override
	void updateButtons(boolean updateRemove, boolean updateRemoveAll) {

		updateRemoveButtons(updateRemove ? BTN_REMOVE : -1, updateRemoveAll ? BTN_REMOVE_ALL : -1);

		TablePart tablePart = getTablePart();
		Table table = getTable();

		tablePart.setButtonEnabled(BTN_PROPS, isEditable() && table.getSelection().length == 1);
		tablePart.setButtonEnabled(BTN_ADD_REQUIRED, isEditable() && table.getItemCount() > 0);
	}

	public boolean includeOptionalDependencies() {
		return fIncludeOptionalButton.getSelection();
	}
}
