package org.eclipse.pde.internal.editor.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.component.*;
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
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.graphics.*;


public class PluginSection
	extends PDEFormSection
	implements IModelProviderListener {
	public static final String PLUGIN_TITLE =
		"ComponentEditor.PluginSection.pluginTitle";
	public static final String FRAGMENT_TITLE = "ComponentEditor.PluginSection.fragmentTitle";
	public static final String FRAGMENT_DESC = "ComponentEditor.PluginSection.fragmentDesc";
	public static final String PLUGIN_DESC = "ComponentEditor.PluginSection.pluginDesc";
	private boolean updateNeeded;
	private Object[] references;
	private OpenReferenceAction openAction;
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
					return obj.toString() + " " + version;
				else
					return obj.toString();
			}
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return getReferenceImage(obj);
		}
	}
	private boolean fragmentSection;

public PluginSection(ComponentReferencePage page, boolean fragmentSection) {
	super(page);
	if (!fragmentSection) {
		setHeaderText(PDEPlugin.getResourceString(PLUGIN_TITLE));
		setDescription(PDEPlugin.getResourceString(PLUGIN_DESC));
		pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
	} else {
		setHeaderText(PDEPlugin.getResourceString(FRAGMENT_TITLE));
		setDescription(PDEPlugin.getResourceString(FRAGMENT_DESC));
		fragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
	}
	this.fragmentSection = fragmentSection;
}
public void commitChanges(boolean onSave) {
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.verticalSpacing = 9;
	container.setLayout(layout);

	pluginViewer = new CheckboxTableViewer(container, SWT.NULL);
	pluginViewer.setContentProvider(new PluginContentProvider());
	pluginViewer.setLabelProvider(new PluginLabelProvider());
	pluginViewer.setSorter(ListUtil.NAME_SORTER);

	pluginViewer.addCheckStateListener(new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent e) {
			handlePluginChecked((PluginReference) e.getElement(), e.getChecked());
		}
	});
	pluginViewer.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent e) {
			handleSelectionChanged(e);
		}
	});
	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener () {
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
	Menu menu=popupMenuManager.createContextMenu(table);
	table.setMenu(menu);
	GridData gd = new GridData(GridData.FILL_BOTH);
	table.setLayoutData(gd);
	factory.paintBordersFor(container);
	openAction = new OpenReferenceAction(pluginViewer);
	return container;
}
private Object[] createPluginReferences() {
	if (references == null) {
		WorkspaceModelManager manager =
			PDEPlugin.getDefault().getWorkspaceModelManager();
		if (fragmentSection) {
			IFragmentModel[] workspaceFragmentModels = manager.getWorkspaceFragmentModels();
			references = new Object[workspaceFragmentModels.length];
			for (int i = 0; i < workspaceFragmentModels.length; i++) {
				IFragmentModel model = workspaceFragmentModels[i];
				IComponentReference cref = findComponentFragment(model.getFragment().getId());
				PluginReference reference = new PluginReference(cref, model);
				reference.setFragment(true);
				references[i] = reference;
			}
		} else {
			IPluginModel[] workspaceModels = manager.getWorkspacePluginModels();
			references = new Object[workspaceModels.length];
			for (int i = 0; i < workspaceModels.length; i++) {
				IPluginModel model = workspaceModels[i];
				IComponentReference cref = findComponentPlugin(model.getPlugin().getId());
				PluginReference reference = new PluginReference(cref, model);
				references[i] = reference;

			}
		}
	}
	return references;
}
private Image createWarningImage(
	ImageDescriptor baseDescriptor,
	ImageDescriptor overlayDescriptor) {
	ImageDescriptor desc =
		new OverlayIcon(baseDescriptor, new ImageDescriptor[][] { { overlayDescriptor }
	});
	return desc.createImage();
}
public void dispose() {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	model.removeModelChangedListener(this);
	WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
	mng.removeModelProviderListener(this);
	if (pluginImage!=null) pluginImage.dispose();
	if (fragmentImage!=null) fragmentImage.dispose();
	if (warningPluginImage!=null) warningPluginImage.dispose();
	if (warningFragmentImage!=null) warningFragmentImage.dispose();
	super.dispose();
}
public void expandTo(Object object) {
	pluginViewer.setSelection(new StructuredSelection(object), true);
}
private void fillContextMenu(IMenuManager manager) {
	manager.add(openAction);
	manager.add(new Separator());
	getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
}
private IComponentFragment findComponentFragment(String id) {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	IComponent component = model.getComponent();
	IComponentFragment[] fragments = component.getFragments();
	for (int i = 0; i < fragments.length; i++) {
		if (fragments[i].getId().equals(id))
			return fragments[i];
	}
	return null;
}
private IComponentPlugin findComponentPlugin(String id) {
	IComponentModel model = (IComponentModel) getFormPage().getModel();
	IComponent component = model.getComponent();
	IComponentPlugin[] plugins = component.getPlugins();
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
		IComponentModel cmodel = (IComponentModel) getFormPage().getModel();
		IComponent component = cmodel.getComponent();
		if (checked) {
			IPluginModelBase model = reference.getModel();
			IPluginBase plugin = model.getPluginBase();
			IComponentReference cref = null;
			if (reference.isFragment())
				cref = cmodel.getFactory().createFragment();
			else
				cref = cmodel.getFactory().createPlugin();
			cref.setId(plugin.getId());
			cref.setLabel(plugin.getName());
			cref.setVersion(plugin.getVersion());
			if (reference.isFragment())
				component.addFragment((IComponentFragment) cref);
			else
				component.addPlugin((IComponentPlugin) cref);
			reference.setReference(cref);
		} else {
			if (reference.getReference() != null) {
				if (reference.isFragment())
					component.removeFragment((IComponentFragment) reference.getReference());
				else
					component.removePlugin((IComponentPlugin) reference.getReference());
				reference.setReference(null);
			}
		}
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
	IComponentModel model = (IComponentModel)input;
	update(input);
	if (model.isEditable()==false) {
		pluginViewer.getTable().setEnabled(false);
	}
	model.addModelChangedListener(this);
	WorkspaceModelManager mng = PDEPlugin.getDefault().getWorkspaceModelManager();
	mng.addModelProviderListener(this);
}
private void initializeOverlays() {
	if (fragmentSection)
		warningFragmentImage =
			createWarningImage(
				PDEPluginImages.DESC_FRAGMENT_OBJ,
				PDEPluginImages.DESC_ERROR_CO);
	else
		warningPluginImage =
			createWarningImage(
				PDEPluginImages.DESC_PLUGIN_OBJ,
				PDEPluginImages.DESC_ERROR_CO);
}
public boolean isFragmentSection() {
	return fragmentSection;
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
		updateNeeded = true;
		if (getFormPage().isVisible()) {
			update();
		}
	} else
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object obj = e.getChangedObjects()[0];
			if (isFragmentSection() && obj instanceof IComponentFragment)
				pluginViewer.refresh();
			if (!isFragmentSection() && obj instanceof IComponentPlugin)
				pluginViewer.refresh();
		}
}

public void modelsChanged(IModelProviderEvent event) {
	IModel model = event.getAffectedModel();
	if (model instanceof IFragmentModel && isFragmentSection() ||
	         model instanceof IPluginModel && !isFragmentSection()) {
	   updateNeeded=true;
	   update();
	}
}

public void setFocus() {
	if (pluginViewer != null)
		pluginViewer.getTable().setFocus();
}
public void setFragmentSection(boolean newFragmentSection) {
	fragmentSection = newFragmentSection;
}
public void update() {
	if (updateNeeded) {
		references = null;
		this.update(getFormPage().getModel());
	}
}
public void update(Object input) {
	IComponentModel model = (IComponentModel)input;
	IComponent component = model.getComponent();
	pluginViewer.setInput(model.getComponent());
	for (int i=0; i<references.length; i++) {
		PluginReference reference = (PluginReference)references[i];
		if (reference.getReference()!=null) {
			pluginViewer.setChecked(reference, true);
		}
	}
	updateNeeded=false;
}
}
