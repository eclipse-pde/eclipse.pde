package org.eclipse.pde.internal.editor.component;

import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.ant.internal.ui.*;
import org.eclipse.ant.core.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.*;
import org.eclipse.pde.internal.core.*;
import java.io.*;
import java.lang.reflect.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.swt.events.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.component.*;
import org.eclipse.pde.internal.model.component.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.launcher.*;
import org.eclipse.pde.internal.preferences.*;
import org.apache.tools.ant.*;

public class SynchronizeVersionsWizardPage extends WizardPage {
	public static final int USE_COMPONENT = 1;
	public static final int USE_PLUGINS = 2;
	public static final int USE_REFERENCES = 3;
	private ComponentEditor componentEditor;
	private Button useComponentButton;
	private Button usePluginsButton;
	private Button useReferencesButton;

	private static final String PREFIX =
		PDEPlugin.getDefault().getPluginId() + ".synchronizeVersions.";
	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode";
	public static final String PAGE_TITLE = "VersionSyncWizard.title";
	public static final String KEY_GROUP = "VersionSyncWizard.group";
	public static final String KEY_USE_COMPONENT = "VersionSyncWizard.useComponent";
	public static final String KEY_USE_PLUGINS = "VersionSyncWizard.usePlugins";
	public static final String KEY_USE_REFERENCES = "VersionSyncWizard.useReferences";
	public static final String KEY_SYNCHRONIZING = "VersionSyncWizard.synchronizing";
	public static final String PAGE_DESC = "VersionSyncWizard.desc";

public SynchronizeVersionsWizardPage(ComponentEditor componentEditor) {
	super("componentJar");
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	this.componentEditor = componentEditor;
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	container.setLayout(layout);

	Group group = new Group(container, SWT.SHADOW_ETCHED_IN);
	GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	layout = new GridLayout();
	group.setLayout(layout);
	group.setLayoutData(gd);
	group.setText(PDEPlugin.getResourceString(KEY_GROUP));

	useComponentButton = new Button(group, SWT.RADIO);
	useComponentButton.setText(PDEPlugin.getResourceString(KEY_USE_COMPONENT));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	useComponentButton.setLayoutData(gd);

	usePluginsButton = new Button(group, SWT.RADIO);
	usePluginsButton.setText(PDEPlugin.getResourceString(KEY_USE_PLUGINS));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	usePluginsButton.setLayoutData(gd);
	
	useReferencesButton = new Button(group, SWT.RADIO);
	useReferencesButton.setText(PDEPlugin.getResourceString(KEY_USE_REFERENCES));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	useReferencesButton.setLayoutData(gd);  

	setControl(container);
	loadSettings();
}
private WorkspacePluginModelBase findFragment(String id) {
	IPluginModelBase[] models =
		PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
	return findWorkspaceModelBase(models, id);
}
private WorkspacePluginModelBase findModel(String id) {
	IPluginModelBase [] models = PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
	return findWorkspaceModelBase(models, id);
}
private IComponentReference findPluginReference(String id) {
	IComponentModel model = (IComponentModel) componentEditor.getModel();
	IComponentPlugin[] references = model.getComponent().getPlugins();
	for (int i = 0; i<references.length; i++) {
		if (references[i].getId().equals(id))
			return references[i];
	}
	return null;
}
private WorkspacePluginModelBase findWorkspaceModelBase(
	IPluginModelBase[] models,
	String id) {
	for (int i = 0; i < models.length; i++) {
		IPluginModelBase modelBase = models[i];
		if (modelBase instanceof WorkspacePluginModelBase
			&& modelBase.getPluginBase().getId().equals(id))
			return (WorkspacePluginModelBase) modelBase;
	}
	return null;
}
public boolean finish() {
	final int mode = saveSettings();

	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				runOperation(mode, monitor);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} finally {
				monitor.done();
			}
		}
	};
	try {
		getContainer().run(false, true, operation);
	} catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	} catch (InterruptedException e) {
		return false;
	}
	return true;
}
private void forceVersion(String targetVersion, IPluginModelBase modelBase)
	throws CoreException {
	IFile file = (IFile) modelBase.getUnderlyingResource();
	IModelProvider modelProvider =
		PDEPlugin.getDefault().getWorkspaceModelManager();
	modelProvider.connect(file, componentEditor);
	WorkspacePluginModelBase model =
		(WorkspacePluginModelBase) modelProvider.getModel(file, componentEditor);
	model.load();
	if (model.isLoaded()) {
		IPluginBase base = model.getPluginBase();
		base.setVersion(targetVersion);
		if (base instanceof IFragment) {
			// also fix target plug-in version
			IFragment fragment = (IFragment) base;
			IComponentReference ref = findPluginReference(fragment.getPluginId());
			if (ref != null)
				fragment.setPluginVersion(ref.getVersion());
		}
		model.save();
	}
	modelProvider.disconnect(file, componentEditor);
}
private void loadSettings() {
	IDialogSettings settings = getDialogSettings();
	if (settings.get(PROP_SYNCHRO_MODE) != null) {
		int mode = settings.getInt(PROP_SYNCHRO_MODE);
		switch (mode) {
			case USE_COMPONENT :
				useComponentButton.setSelection(true);
				break;
			case USE_PLUGINS :
				usePluginsButton.setSelection(true);
				break;
			case USE_REFERENCES :
				useReferencesButton.setSelection(true);
				break;
		}
	}
}
private void runOperation(int mode, IProgressMonitor monitor)
	throws CoreException, InvocationTargetException {
	WorkspaceComponentModel model =
		(WorkspaceComponentModel) componentEditor.getModel();
	IComponent component = model.getComponent();
	IComponentReference[] plugins = component.getPlugins();
	IComponentReference[] fragments = component.getFragments();
	int size = plugins.length + fragments.length;
	monitor.beginTask(PDEPlugin.getResourceString(KEY_SYNCHRONIZING), size);
	for (int i = 0; i < plugins.length; i++) {
		synchronizeVersion(mode, component.getVersion(), plugins[i], monitor);
	}
	for (int i = 0; i < fragments.length; i++) {
		synchronizeVersion(mode, component.getVersion(), fragments[i], monitor);
	}
	model.fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.WORLD_CHANGED, null, null));
}
private int saveSettings() {
	IDialogSettings settings = getDialogSettings();

	int mode = USE_COMPONENT;

	if (usePluginsButton.getSelection())
		mode = USE_PLUGINS;
	else
		if (useReferencesButton.getSelection())
			mode = USE_REFERENCES;
	settings.put(PROP_SYNCHRO_MODE, mode);
	return mode;
}
private void synchronizeVersion(
	int mode,
	String componentVersion,
	IComponentReference ref,
	IProgressMonitor monitor)
	throws CoreException {
	String id = ref.getId();
	WorkspacePluginModelBase modelBase = null;
	if (ref instanceof IComponentPlugin) {
		modelBase = findModel(id);

	} else {
		modelBase = findFragment(id);
	}
	if (modelBase == null)
		return;
	if (mode == USE_PLUGINS) {
		String baseVersion = modelBase.getPluginBase().getVersion();
		if (ref.getVersion().equals(baseVersion) == false) {
			ref.setVersion(baseVersion);
		}
	} else {
		String targetVersion = componentVersion;
		if (mode == USE_REFERENCES)
			targetVersion = ref.getVersion();
		else
			ref.setVersion(targetVersion);
		String baseVersion = modelBase.getPluginBase().getVersion();
		if (targetVersion.equals(baseVersion) == false) {
			forceVersion(targetVersion, modelBase);
		}
	}
	monitor.worked(1);
}
}
