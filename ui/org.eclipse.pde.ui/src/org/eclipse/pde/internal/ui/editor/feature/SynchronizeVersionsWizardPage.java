/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.help.*;

public class SynchronizeVersionsWizardPage extends WizardPage {
	public static final int USE_FEATURE = 1;
	public static final int USE_PLUGINS = 2;
	public static final int USE_REFERENCES = 3;
	private FeatureEditor featureEditor;
	private Button useComponentButton;
	private Button usePluginsButton;
	private Button useReferencesButton;

	private static final String PREFIX =
		PDEPlugin.getPluginId() + ".synchronizeVersions.";
	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode";
	public static final String PAGE_TITLE = "VersionSyncWizard.title";
	public static final String KEY_GROUP = "VersionSyncWizard.group";
	public static final String KEY_USE_COMPONENT = "VersionSyncWizard.useComponent";
	public static final String KEY_USE_PLUGINS = "VersionSyncWizard.usePlugins";
	public static final String KEY_USE_REFERENCES = "VersionSyncWizard.useReferences";
	public static final String KEY_SYNCHRONIZING = "VersionSyncWizard.synchronizing";
	public static final String PAGE_DESC = "VersionSyncWizard.desc";

public SynchronizeVersionsWizardPage(FeatureEditor featureEditor) {
	super("featureJar");
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	this.featureEditor = featureEditor;
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
	Dialog.applyDialogFont(container);
	loadSettings();
	WorkbenchHelp.setHelp(container, IHelpContextIds.FEATURE_SYNCHRONIZE_VERSIONS);
}
private WorkspacePluginModelBase findFragment(String id) {
	IPluginModelBase[] models =
		PDECore.getDefault().getWorkspaceModelManager().getFragmentModels();
	return findWorkspaceModelBase(models, id);
}
private WorkspacePluginModelBase findModel(String id) {
	IPluginModelBase [] models = PDECore.getDefault().getWorkspaceModelManager().getPluginModels();
	return findWorkspaceModelBase(models, id);
}
private IFeaturePlugin findPluginReference(String id) {
	IFeatureModel model = (IFeatureModel) featureEditor.getModel();
	IFeaturePlugin[] references = model.getFeature().getPlugins();
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
	NewWorkspaceModelManager modelProvider =
		PDECore.getDefault().getWorkspaceModelManager();
	WorkspacePluginModelBase model =
		(WorkspacePluginModelBase) modelProvider.getModel(file);
	model.load();
	if (model.isLoaded()) {
		IPluginBase base = model.getPluginBase();
		base.setVersion(targetVersion);
		if (base instanceof IFragment) {
			// also fix target plug-in version
			IFragment fragment = (IFragment) base;
			IFeaturePlugin ref = findPluginReference(fragment.getPluginId());
			if (ref != null)
				fragment.setPluginVersion(targetVersion);
		}
		model.save();
		if (base instanceof IPlugin) {
		   IPlugin local = PDECore.getDefault().findPlugin(base.getId());
		   if (local!=null && 
		       local.getModel().getUnderlyingResource()!=null &&
		       local.getModel().getUnderlyingResource().equals(file)) {
		      ((PluginBase)local).internalSetVersion(base.getVersion());
		   }
		}
	}
}
private void loadSettings() {
	IDialogSettings settings = getDialogSettings();
	if (settings.get(PROP_SYNCHRO_MODE) != null) {
		int mode = settings.getInt(PROP_SYNCHRO_MODE);
		switch (mode) {
			case USE_FEATURE :
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
	else 
	   useComponentButton.setSelection(true);
}
private void runOperation(int mode, IProgressMonitor monitor)
	throws CoreException, InvocationTargetException {
	WorkspaceFeatureModel model =
		(WorkspaceFeatureModel) featureEditor.getModel();
	IFeature feature = model.getFeature();
	IFeaturePlugin[] plugins = feature.getPlugins();
	int size = plugins.length;
	monitor.beginTask(PDEPlugin.getResourceString(KEY_SYNCHRONIZING), size);
	for (int i = 0; i < plugins.length; i++) {
		synchronizeVersion(mode, feature.getVersion(), plugins[i], monitor);
	}
	model.fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.WORLD_CHANGED, null, null));
}
private int saveSettings() {
	IDialogSettings settings = getDialogSettings();

	int mode = USE_FEATURE;

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
	String featureVersion,
	IFeaturePlugin ref,
	IProgressMonitor monitor)
	throws CoreException {
	String id = ref.getId();
	WorkspacePluginModelBase modelBase = null;
	if (ref.isFragment()) {
		modelBase = findFragment(id);
	} else {
		modelBase = findModel(id);
	}
	if (modelBase == null)
		return;
	if (mode == USE_PLUGINS) {
		String baseVersion = modelBase.getPluginBase().getVersion();
		if (ref.getVersion().equals(baseVersion) == false) {
			ref.setVersion(baseVersion);
		}
	} else {
		String targetVersion = featureVersion;
		if (mode == USE_REFERENCES)
			targetVersion = ref.getVersion();
		else
			ref.setVersion(targetVersion);
		String baseVersion = modelBase.getPluginBase().getVersion();
		if (targetVersion.equals(baseVersion) == false) {
			forceVersion(targetVersion, modelBase);
		}
		if (mode == USE_FEATURE)
			ref.setVersion(targetVersion);
	}
	monitor.worked(1);
}
}
