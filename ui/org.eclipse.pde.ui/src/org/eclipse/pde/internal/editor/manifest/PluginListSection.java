package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.preferences.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.*;
import java.lang.reflect.*;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.swt.events.*;
import org.w3c.dom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.pde.internal.wizards.PluginPathUpdater;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.base.BuildPathUtil;
import org.eclipse.jdt.core.JavaModelException;

public class PluginListSection extends PDEFormSection implements IModelProviderListener {
	private FormWidgetFactory factory;
	private Composite pluginListParent;
	private Color separatorColor;
	public static final String SECTION_TITLE = "ManifestEditor.PluginListSection.title";
	public static final String SECTION_DESC = "ManifestEditor.PluginListSection.desc";
	public static final String KEY_EXTERNAL_PLUGINS = "ManifestEditor.PluginListSection.externalPlugins";
	public static final String KEY_EXTERNAL_PLUGINS_TOOLTIP = "ManifestEditor.PluginListSection.externalPlugins.tooltip";
	public static final String KEY_WORKSPACE_PLUGINS = "ManifestEditor.PluginListSection.workspacePlugins";
	public static final String KEY_UNRESOLVED_PLUGINS = "ManifestEditor.PluginListSection.unresolvedPlugins";
	public static final String KEY_UNRESOLVED_PLUGINS_TOOLTIP = "ManifestEditor.PluginListSection.unresolvedPlugins.tooltip";
	public static final String KEY_COMPUTE_BUILD_PATH = "ManifestEditor.PluginListSection.updateBuildPath";
	public static final String KEY_UPDATING_BUILD_PATH = "ManifestEditor.PluginListSection.updatingBuildPath";
	private boolean needsUpdate;

public PluginListSection(ManifestDependenciesPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public boolean addExternalPlugins(IPluginModel model) {
	IPlugin pluginObject = model.getPlugin();
	ExternalModelManager registry =
		PDEPlugin.getDefault().getExternalModelManager();
	IPluginModel[] models = registry.getModels();
	ArraySorter.INSTANCE.sortInPlace(models);
	boolean havePluginsToAdd = false;
	for (int i = 0; i < models.length; i++) {
		IPluginModel refmodel = models[i];
		if (refmodel.isEnabled()) {
			havePluginsToAdd = true;
			break;
		}
	}
	if (!havePluginsToAdd)
		return false;
	Label label = createHeader(pluginListParent, PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS), false);
	label.setToolTipText(PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS_TOOLTIP));
	factory.turnIntoHyperlink(label, new HyperlinkAdapter() {
		public void linkActivated(Control link) {
			openExternalPluginsDialog();
		}
	});

	SelectionListener listener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			IPlugin pluginInfo = (IPlugin) e.widget.getData();
			PluginListSection.this.fireSelectionNotification(pluginInfo);
			updateModel(pluginInfo, ((Button) e.widget).getSelection());
		}
	};
	for (int i = 0; i < models.length; i++) {
		IPluginModel refmodel = models[i];
		if (refmodel.isEnabled() == false)
			continue;
		IPlugin plugin = refmodel.getPlugin();
		Button button =
			factory.createButton(
				pluginListParent,
				plugin.getResourceString(plugin.getName()),
				SWT.CHECK);
		button.setData(plugin);
		button.setToolTipText(plugin.getId());
		button.setEnabled(model.isEditable());
		if (matches(pluginObject.getImports(), plugin.getId()))
			button.setSelection(true);
		button.addSelectionListener(listener);
	}
	return true;
}
public boolean addUnresolvedPlugins(IPluginModel model) {
	IPlugin pluginObject = model.getPlugin();
	IPluginImport[] imports = pluginObject.getImports();
	boolean havePluginsToAdd=false;

	Vector unresolved = new Vector();
	for (int i = 0; i < imports.length; i++) {
		IPluginImport iimport = imports[i];
		String refId = iimport.getId();
		IPlugin refPlugin = PDEPlugin.getDefault().findPlugin(refId);
		if (refPlugin != null)
			continue;
		// Unresolved reference
		unresolved.add(iimport);
	}

	havePluginsToAdd = unresolved.size() > 0;
	if (!havePluginsToAdd)
		return false;

	Label label = createHeader(pluginListParent, PDEPlugin.getResourceString(KEY_UNRESOLVED_PLUGINS), false);
	label.setToolTipText(PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS_TOOLTIP));
	factory.turnIntoHyperlink(label, new HyperlinkAdapter() {
		public void linkActivated(Control link) {
			openExternalPluginsDialog();
		}
	});

	SelectionListener listener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			IPluginImport iimport = (IPluginImport) e.widget.getData();
			PluginListSection.this.fireSelectionNotification(iimport);
			Button button = (Button) e.widget;
			boolean selection = button.getSelection();
			button.setForeground(
				selection
					? button.getDisplay().getSystemColor(SWT.COLOR_DARK_RED)
					: factory.getForegroundColor());
			updateModel(iimport, ((Button) e.widget).getSelection());
		}
	};
	IPluginImport [] unresolvedArray = new IPluginImport [unresolved.size()];
	unresolved.copyInto(unresolvedArray);
	ArraySorter.INSTANCE.sortInPlace(unresolvedArray);
	for (int i = 0; i < unresolvedArray.length; i++) {
		IPluginImport ref = unresolvedArray[i];
		Button button = factory.createButton(pluginListParent, ref.getId(), SWT.CHECK);
		button.setData(ref);
		button.setEnabled(model.isEditable());
		button.setSelection(true);
		button.setForeground(button.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
		button.addSelectionListener(listener);
	}
	return true;
}
public boolean addWorkspacePlugins(IPluginModel model, boolean hasExternal) {
	IPlugin pluginObject = model.getPlugin();
	WorkspaceModelManager manager =
		(WorkspaceModelManager) PDEPlugin.getDefault().getWorkspaceModelManager();
	IPluginModel[] models = manager.getWorkspacePluginModels();
	ArraySorter.INSTANCE.sortInPlace(models);
	boolean havePluginsToAdd = false;

	havePluginsToAdd = models.length > 1;
	if (!havePluginsToAdd)
		return false;

	if (hasExternal)
		createHeader(pluginListParent, PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS));

	SelectionListener listener = new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			IPlugin pluginInfo = (IPlugin) e.widget.getData();
			PluginListSection.this.fireSelectionNotification(pluginInfo);
			updateModel(pluginInfo, ((Button) e.widget).getSelection());
		}
	};
	for (int i = 0; i < models.length; i++) {
		IPluginModel refmodel = models[i];
		if (refmodel.getUnderlyingResource().equals(model.getUnderlyingResource())) {
			// this is us - cannot make self-reference
			continue;
		}
		//if (refmodel.isEnabled()==false) continue;
		IPlugin plugin = refmodel.getPlugin();

		Button button =
			factory.createButton(
				pluginListParent,
				plugin.getResourceString(plugin.getName()),
				SWT.CHECK);
		button.setData(plugin);
		button.setToolTipText(plugin.getId());
		button.setEnabled(model.isEditable());
		if (matches(pluginObject.getImports(), plugin.getId()))
			button.setSelection(true);
		button.addSelectionListener(listener);
	}
	return true;
}
public void commitChanges(boolean onSave) {
	if (onSave) {
		boolean shouldUpdate = BuildpathPreferencePage.isManifestUpdate();
		if (shouldUpdate)
		   updateBuildPath();
	}
	setDirty(false);
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);
	layout = new GridLayout();
	pluginListParent = factory.createComposite(container);
	pluginListParent.setLayout(layout);
	layout.verticalSpacing=0;
	layout.horizontalSpacing =0;
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	GridData gd = new GridData(GridData.FILL_BOTH);
	pluginListParent.setLayoutData(gd);
	return container;
}
private Label createHeader(Composite parent, String text) {
	return createHeader(parent, text, true);
}
private Label createHeader(
	Composite parent,
	String text,
	boolean addSeparator) {
	Composite header = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.marginWidth = 0;
	//layout.marginHeight = 0;
	layout.verticalSpacing = 2;
	header.setLayout(layout);
	Label label = factory.createLabel(header, text);
	GridData gd;
	if (addSeparator) {
		Control sep = factory.createCompositeSeparator(header);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 1;
		sep.setLayoutData(gd);
		sep.setBackground(separatorColor);
	}
	gd = new GridData();//GridData.FILL_HORIZONTAL);
	header.setLayoutData(gd);
	return label;
}
public void dispose() {
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
	PDEPlugin.getDefault().getExternalModelManager().removeModelProviderListener(this);
	PDEPlugin.getDefault().getWorkspaceModelManager().removeModelProviderListener(this);
	super.dispose();
}
public Iterator getSelectedPlugins() {
	Vector v = new Vector();
	Control[] controls = pluginListParent.getChildren();
	for (int i = 0; i < controls.length; i++) {
		Control control = controls[i];
		if (control instanceof Button) {
			Button button = (Button) control;
			if (button.getSelection()) {
				v.addElement(button.getData());
			}
		}
	}
	return v.iterator();
}
private boolean hasExternalPlugins() {
	return PDEPlugin.getDefault().getExternalModelManager().hasEnabledModels();
}
public void initialize(Object input) {
	update(input);
	fireSelectionNotification(input);
	IPluginModel model = (IPluginModel)input;
	model.addModelChangedListener(this);
	PDEPlugin.getDefault().getExternalModelManager().addModelProviderListener(this);
	PDEPlugin.getDefault().getWorkspaceModelManager().addModelProviderListener(this);
	separatorColor = factory.registerColor("pluginSeparatorColor", 212, 208, 200);
}
private boolean matches(IPluginImport [] imports, String id) {
	for (int i=0; i<imports.length; i++) {
		String sourceId = imports[i].getId();
		if (sourceId.equals(id))
			return true;
	}
	return false;
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED)
		needsUpdate = true;
}
public void modelsChanged(IModelProviderEvent e) {
	IModel model = e.getAffectedModel();
	if (model instanceof IPluginModel) {
		IPluginModel affModel = (IPluginModel)model;
		IPluginModel thisModel = (IPluginModel)getFormPage().getModel();
		// Skip ourselves
		if (affModel.getUnderlyingResource().equals(thisModel.getUnderlyingResource()))
		   return;
	    needsUpdate = true;
		if (getFormPage().isVisible()) {
			Display d = pluginListParent.getDisplay();
			d.asyncExec(new Runnable() {
				public void run() {
					update();
				}
			});
		}
	}
}

private void openExternalPluginsDialog() {
	ExternalPluginsWizard wizard = new ExternalPluginsWizard();
	WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
	dialog.create();
	dialog.getShell().setText(PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS));
	dialog.open();
}
public void setFocus() {
	if (pluginListParent != null) {
		Control[] children = pluginListParent.getChildren();
		if (children.length > 0)
			children[0].setFocus();
	}
}
public void update() {
	if (needsUpdate) {
		Control [] children = pluginListParent.getChildren();
		for (int i=0; i<children.length; i++) {
			children[i].dispose();
		}
		update(getFormPage().getModel());
	}
}
public void update(Object input) {
	IPluginModel model = (IPluginModel) input;

	boolean hasUnresolved = addUnresolvedPlugins(model);
	if (hasUnresolved) {
			new Label(pluginListParent, SWT.NONE);
	}
	boolean hasExternal = hasExternalPlugins();
	boolean added = addWorkspacePlugins(model, hasExternal || hasUnresolved);
	if (hasExternal) {
		if (added) {
			new Label(pluginListParent, SWT.NONE);
		}
		addExternalPlugins(model);
	}
	pluginListParent.layout(true);
	pluginListParent.getDisplay().asyncExec(new Runnable() {
		public void run() {
			fireSelectionNotification(null);
			((ScrollableForm)getFormPage().getForm()).update();
		}
	});
	needsUpdate = false;
}
private void updateBuildPath() {
	computeBuildPath((IPluginModel)getFormPage().getModel(), false);
}
private void updateModel(IPlugin info, boolean add) {
	IPluginModel model = (IPluginModel) getFormPage().getModel();
	if (!model.isEditable())
		return;
	IPlugin plugin = model.getPlugin();

	IPluginImport matchingNode = null;
	IPluginImport[] imports = plugin.getImports();

	for (int i = 0; i < imports.length; i++) {
		IPluginImport importNode = imports[i];
		if (importNode.getId().equals(info.getId())) { // this one
			matchingNode = importNode;
			break;
		}
	}

	try {
		if (add) {
			if (matchingNode == null) {
				IPluginImport importNode = model.getFactory().createImport();
				importNode.setId(info.getId());
				plugin.add(importNode);
			}
		} else
			if (matchingNode != null) {
				plugin.remove(matchingNode);
			}
	} catch (CoreException e) {
	}
}
private void updateModel(IPluginImport unresolved, boolean add) {
	IPluginModel model = (IPluginModel) getFormPage().getModel();
	if (!model.isEditable())
		return;
	IPlugin plugin = model.getPlugin();

	try {
		if (add)
			plugin.add(unresolved);
		else
			plugin.remove(unresolved);
	} catch (CoreException e) {
		PDEPlugin.logException(e);
	}
}

public boolean fillContextMenu(IMenuManager manager) {
	Action action = new Action() {
		public void run() {
			IPluginModel model = (IPluginModel) getFormPage().getModel();
			computeBuildPath(model, true);
		}
	};
	action.setText(PDEPlugin.getResourceString(KEY_COMPUTE_BUILD_PATH));
	manager.add(action);
	manager.add(new Separator());
	return true;
}

private void computeBuildPath(final IPluginModel model, final boolean save) {
	IRunnableWithProgress op = new IRunnableWithProgress() {
		public void run(IProgressMonitor monitor) {
			monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATING_BUILD_PATH), 1);
			try {
			   if (save && getFormPage().getEditor().isDirty()) {
			      getFormPage().getEditor().doSave(monitor);
			   }
			   BuildPathUtil.setBuildPath(model, monitor);
			   monitor.worked(1);
			}
			catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			finally {
			   monitor.done();
			}
		}
	};
	
	ProgressMonitorDialog pm = new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
	try {
	   pm.run(false, false, op);
	}
	catch (InterruptedException e) {
		PDEPlugin.logException(e);
	}
	catch (InvocationTargetException e) {
		PDEPlugin.logException(e.getTargetException());
	}
}
}
