package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.editor.PropertiesAction;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.pde.internal.model.feature.FeaturePlugin;
import java.util.Vector;

public class PluginSection
	extends PDEFormSection
	implements IModelProviderListener {
	private static final String PLUGIN_TITLE =
		"FeatureEditor.PluginSection.pluginTitle";
	private static final String FRAGMENT_TITLE =
		"FeatureEditor.PluginSection.fragmentTitle";
	private static final String FRAGMENT_DESC =
		"FeatureEditor.PluginSection.fragmentDesc";
	private static final String PLUGIN_DESC =
		"FeatureEditor.PluginSection.pluginDesc";
	private static final String KEY_SELECT_ALL = "ExternalPluginsBlock.selectAll";
	private static final String KEY_DESELECT_ALL =
		"ExternalPluginsBlock.deselectAll";
	private boolean updateNeeded;
	private Object[] references;
	private OpenReferenceAction openAction;
	private PropertiesAction propertiesAction;
	private CheckboxTableViewer pluginViewer;
	private Image pluginImage;
	private Image warningPluginImage;
	private Image fragmentImage;
	private Image warningFragmentImage;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return createPluginReferences();
		}
	}

	class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof PluginReference) {
				PluginReference ref = (PluginReference) obj;
				String version = ref.getModel().getPluginBase().getVersion();
				if (ref.isInSync())
					return obj.toString() + " (" + version + ")";
				else
					return obj.toString();
			}
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return getReferenceImage(obj);
		}
	}

	public PluginSection(FeatureReferencePage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(PLUGIN_TITLE));
		setDescription(PDEPlugin.getResourceString(PLUGIN_DESC));
		pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
		fragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
	}

	public void commitChanges(boolean onSave) {
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		pluginViewer = CheckboxTableViewer.newCheckList(container, SWT.NULL);
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(new PluginLabelProvider());
		pluginViewer.setSorter(ListUtil.NAME_SORTER);

		pluginViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent e) {
				BusyIndicator.showWhile(pluginViewer.getTable().getDisplay(), new Runnable() {
					public void run() {
						handlePluginChecked((PluginReference) e.getElement(), e.getChecked());
					}
				});
			}
		});
		pluginViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				handleSelectionChanged(e);
			}
		});
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		pluginViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				openAction.run();
			}
		});
		Table table = pluginViewer.getTable();
		popupMenuManager.setRemoveAllWhenShown(true);
		popupMenuManager.addMenuListener(listener);
		Menu menu = popupMenuManager.createContextMenu(table);
		table.setMenu(menu);
		GridData gd = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(gd);

		Composite buttonContainer = factory.createComposite(container);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonContainer.setLayout(layout);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		Button button =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_SELECT_ALL),
				SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(true);
			}
		});
		button =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(KEY_DESELECT_ALL),
				SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSelectAll(false);
			}
		});

		factory.paintBordersFor(container);
		openAction = new OpenReferenceAction(pluginViewer);
		propertiesAction = new PropertiesAction(getFormPage().getEditor());
		return container;
	}
	private Object[] createPluginReferences() {
		if (references == null) {
			WorkspaceModelManager manager =
				PDEPlugin.getDefault().getWorkspaceModelManager();
			IPluginModel[] workspaceModels = manager.getWorkspacePluginModels();
			IFragmentModel[] workspaceFragmentModels = manager.getWorkspaceFragmentModels();
			references =
				new Object[workspaceFragmentModels.length + workspaceModels.length];
			for (int i = 0; i < workspaceFragmentModels.length; i++) {
				IFragmentModel model = workspaceFragmentModels[i];
				IFeaturePlugin cref = findFeatureFragment(model.getFragment().getId());
				PluginReference reference = new PluginReference(cref, model);
				reference.setFragment(true);
				references[i] = reference;
				if (cref != null) {
					try {
						cref.setLabel(model.getFragment().getTranslatedName());
					} catch (CoreException e) {
					}
				}
			}
			int offset = workspaceFragmentModels.length;

			for (int i = 0; i < workspaceModels.length; i++) {
				IPluginModel model = workspaceModels[i];
				IFeaturePlugin cref = findFeaturePlugin(model.getPlugin().getId());
				PluginReference reference = new PluginReference(cref, model);
				references[offset + i] = reference;
				if (cref != null) {
					try {
						cref.setLabel(model.getPlugin().getTranslatedName());
					} catch (CoreException e) {
					}
				}
			}
		}
		return references;
	}

	private Image createWarningImage(
		ImageDescriptor baseDescriptor,
		ImageDescriptor overlayDescriptor) {
		ImageDescriptor desc =
			new OverlayIcon(baseDescriptor, new ImageDescriptor[][] { {
			}, {
			}, {
				overlayDescriptor }
		});
		return desc.createImage();
	}
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
		mng.removeModelProviderListener(this);
		if (warningPluginImage != null)
			warningPluginImage.dispose();
		if (warningFragmentImage != null)
			warningFragmentImage.dispose();
		super.dispose();
	}
	public void expandTo(Object object) {
		if (object instanceof IFeaturePlugin) {
			PluginReference reference = findReference((IFeaturePlugin) object);
			if (reference != null)
				pluginViewer.setSelection(new StructuredSelection(reference), true);
		}
	}
	private void fillContextMenu(IMenuManager manager) {
		manager.add(openAction);
		manager.add(propertiesAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	}
	private IFeaturePlugin findFeatureFragment(String id) {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			IFeaturePlugin plugin = plugins[i];
			if (plugin.getId().equals(id) && plugin.isFragment())
				return plugin;
		}
		return null;
	}
	private IFeaturePlugin findFeaturePlugin(String id) {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		IFeature feature = model.getFeature();
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (int i = 0; i < plugins.length; i++) {
			if (plugins[i].getId().equals(id))
				return plugins[i];
		}
		return null;
	}
	private Image getReferenceImage(Object obj) {
		if (!(obj instanceof PluginReference))
			return null;
		PluginReference reference = (PluginReference) obj;
		if (reference.isInSync()) {
			if (reference.isFragment())
				return fragmentImage;
			else
				return pluginImage;
		} else {
			if (warningFragmentImage == null)
				initializeOverlays();
			if (reference.isFragment())
				return warningFragmentImage;
			else
				return warningPluginImage;
		}
	}
	private void handlePluginChecked(PluginReference reference, boolean checked) {
		try {
			IFeatureModel fmodel = (IFeatureModel) getFormPage().getModel();
			IFeature feature = fmodel.getFeature();
			if (checked) {
				IPluginModelBase model = reference.getModel();
				IPluginBase plugin = model.getPluginBase();
				IFeaturePlugin fref = null;
				fref = fmodel.getFactory().createPlugin();
				((FeaturePlugin) fref).loadFrom(plugin);
				feature.addPlugin(fref);
				reference.setReference(fref);
			} else {
				if (reference.getReference() != null) {
					feature.removePlugin(reference.getReference());
					reference.setReference(null);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleSelectAll(boolean select) {
		pluginViewer.setAllChecked(select);
		IFeaturePlugin[] plugins = null;
		IFeatureModel fmodel = (IFeatureModel) getFormPage().getModel();
		IFeature feature = fmodel.getFeature();
		if (select) {
			Object[] refs = createPluginReferences();
			Vector frefs = new Vector();
			for (int i = 0; i < refs.length; i++) {
				PluginReference ref = (PluginReference) refs[i];
				IPluginModelBase model = ref.getModel();
				IPluginBase plugin = model.getPluginBase();
				IFeaturePlugin fref = null;
				fref = fmodel.getFactory().createPlugin();
				((FeaturePlugin) fref).loadFrom(plugin);
				ref.setReference(fref);
				frefs.add(fref);
			}
			plugins = (IFeaturePlugin[]) frefs.toArray(new IFeaturePlugin[frefs.size()]);
		}
		try {
			feature.setPlugins(plugins);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	private void handleSelectionChanged(SelectionChangedEvent e) {
		PluginReference reference =
			(PluginReference) ((IStructuredSelection) e.getSelection()).getFirstElement();

		StructuredSelection selection = null;

		if (reference != null && reference.getReference() != null) {
			selection = new StructuredSelection(reference.getReference());
		} else
			selection = new StructuredSelection();
		getFormPage().setSelection(selection);
	}
	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		if (model.isEditable() == false) {
			pluginViewer.getTable().setEnabled(false);
		}
		model.addModelChangedListener(this);
		WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
		mng.addModelProviderListener(this);
	}
	private void initializeOverlays() {
		warningFragmentImage =
			createWarningImage(
				PDEPluginImages.DESC_FRAGMENT_OBJ,
				PDEPluginImages.DESC_ERROR_CO);
		warningPluginImage =
			createWarningImage(
				PDEPluginImages.DESC_PLUGIN_OBJ,
				PDEPluginImages.DESC_ERROR_CO);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
			if (getFormPage().isVisible()) {
				update();
			}
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (obj instanceof IFeaturePlugin)
				pluginViewer.update(obj, null);
		}
	}

	public void modelsChanged(IModelProviderEvent event) {
		updateNeeded = true;
		update();
	}

	public void setFocus() {
		if (pluginViewer != null)
			pluginViewer.getTable().setFocus();
	}

	public void update() {
		if (updateNeeded) {
			references = null;
			this.update(getFormPage().getModel());
		}
	}

	private PluginReference findReference(IFeaturePlugin plugin) {
		for (int i = 0; i < references.length; i++) {
			PluginReference reference = (PluginReference) references[i];
			if (plugin.equals(reference.getReference()))
				return reference;
		}
		return null;
	}
	public void update(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		IFeature component = model.getFeature();
		pluginViewer.setInput(model.getFeature());
		for (int i = 0; i < references.length; i++) {
			PluginReference reference = (PluginReference) references[i];
			if (reference.getReference() != null) {
				pluginViewer.setChecked(reference, true);
			}
		}
		updateNeeded = false;
	}
}