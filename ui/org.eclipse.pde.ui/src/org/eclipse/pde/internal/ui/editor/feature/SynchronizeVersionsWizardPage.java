/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.model.IDocumentAttribute;
import org.eclipse.pde.internal.ui.model.IEditingModel;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginBaseNode;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.help.WorkbenchHelp;

public class SynchronizeVersionsWizardPage extends WizardPage {
	public static final int USE_FEATURE = 1;
	public static final int USE_PLUGINS = 2;
	public static final int USE_REFERENCES = 3;
	private FeatureEditor fFeatureEditor;
	private Button fUseComponentButton;
	private Button fUsePluginsButton;
	private Button fUseReferencesButton;

	private static final String PREFIX =
		PDEPlugin.getPluginId() + ".synchronizeVersions."; //$NON-NLS-1$
	private static final String PROP_SYNCHRO_MODE = PREFIX + "mode"; //$NON-NLS-1$
	public static final String PAGE_TITLE = "VersionSyncWizard.title"; //$NON-NLS-1$
	public static final String KEY_GROUP = "VersionSyncWizard.group"; //$NON-NLS-1$
	public static final String KEY_USE_COMPONENT = "VersionSyncWizard.useComponent"; //$NON-NLS-1$
	public static final String KEY_USE_PLUGINS = "VersionSyncWizard.usePlugins"; //$NON-NLS-1$
	public static final String KEY_USE_REFERENCES = "VersionSyncWizard.useReferences"; //$NON-NLS-1$
	public static final String KEY_SYNCHRONIZING = "VersionSyncWizard.synchronizing"; //$NON-NLS-1$
	public static final String PAGE_DESC = "VersionSyncWizard.desc"; //$NON-NLS-1$

public SynchronizeVersionsWizardPage(FeatureEditor featureEditor) {
	super("featureJar"); //$NON-NLS-1$
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	this.fFeatureEditor = featureEditor;
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

	fUseComponentButton = new Button(group, SWT.RADIO);
	fUseComponentButton.setText(PDEPlugin.getResourceString(KEY_USE_COMPONENT));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	fUseComponentButton.setLayoutData(gd);

	fUsePluginsButton = new Button(group, SWT.RADIO);
	fUsePluginsButton.setText(PDEPlugin.getResourceString(KEY_USE_PLUGINS));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	fUsePluginsButton.setLayoutData(gd);
	
	fUseReferencesButton = new Button(group, SWT.RADIO);
	fUseReferencesButton.setText(PDEPlugin.getResourceString(KEY_USE_REFERENCES));
	gd = new GridData(GridData.FILL_HORIZONTAL);
	fUseReferencesButton.setLayoutData(gd);  

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
			} catch (BadLocationException e) {
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

	/**
	 * Forces a version into plugin/fragment .xml
	 * 
	 * @param targetVersion
	 * @param modelBase
	 * @throws CoreException
	 */
	private void forceVersion(String targetVersion, IPluginModelBase modelBase,
			IProgressMonitor monitor) throws CoreException,
			BadLocationException {
		IFile file = (IFile) modelBase.getUnderlyingResource();
		if (file == null) {
			return;
		}
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file
					.getFullPath());

			IDocument document = buffer.getDocument();
			PluginModelBase model = getEditingModel(document,
					"fragment.xml".equals(file.getName())); //$NON-NLS-1$
			model.load();
			if (!model.isLoaded())
				throw new CoreException(
						new Status(
								IStatus.ERROR,
								IPDEUIConstants.PLUGIN_ID,
								IStatus.ERROR,
								"The synchronize version operation cannot proceed because plug-in '" + modelBase.getPluginBase().getId() + "' has a malformed manifest file.", null)); //$NON-NLS-1$ //$NON-NLS-2$

			TextEdit edit = modifyVersion(model, targetVersion);
			if (edit != null) {
				edit.apply(document);
				buffer.commit(monitor, true);
			}
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}
	}

	private TextEdit modifyVersion(PluginModelBase model, String version)
			throws CoreException, MalformedTreeException, BadLocationException {
		IPluginBase pluginBase = model.getPluginBase();

		PluginBaseNode element = (PluginBaseNode) pluginBase;// (PluginElementNode)extension.getChildren()[0];
		IDocumentAttribute versionAttr = element
				.getDocumentAttribute("version"); //$NON-NLS-1$
		if (versionAttr != null)
			return new ReplaceEdit(versionAttr.getValueOffset(), versionAttr
					.getValueLength(), version);
		// insert new version attribute (after name, or id attribute)
		IDocumentAttribute attr = element.getDocumentAttribute("name"); //$NON-NLS-1$
		if (attr == null) {
			attr = element.getDocumentAttribute("id"); //$NON-NLS-1$			
		}
		if (attr != null) {
			String newLine = TextUtilities
					.getDefaultLineDelimiter(((IEditingModel) model)
							.getDocument());
			return new ReplaceEdit(attr.getValueOffset()
					+ attr.getValueLength() + 1, 0, newLine + "   version=\"" //$NON-NLS-1$
					+ version + "\""); //$NON-NLS-1$
		}
		return null;
	}

	private PluginModelBase getEditingModel(IDocument document,
			boolean isFragment) {
		if (isFragment)
			return new FragmentModel(document, false);
		return new PluginModel(document, false);
	}

private void loadSettings() {
	IDialogSettings settings = getDialogSettings();
	if (settings.get(PROP_SYNCHRO_MODE) != null) {
		int mode = settings.getInt(PROP_SYNCHRO_MODE);
		switch (mode) {
			case USE_FEATURE :
				fUseComponentButton.setSelection(true);
				break;
			case USE_PLUGINS :
				fUsePluginsButton.setSelection(true);
				break;
			case USE_REFERENCES :
				fUseReferencesButton.setSelection(true);
				break;
		}
	}
	else 
	   fUseComponentButton.setSelection(true);
}
private void runOperation(int mode, IProgressMonitor monitor)
	throws CoreException, BadLocationException {
	WorkspaceFeatureModel model =
		(WorkspaceFeatureModel) fFeatureEditor.getAggregateModel();
	IFeature feature = model.getFeature();
	IFeaturePlugin[] plugins = feature.getPlugins();
	int size = plugins.length;
	monitor.beginTask(PDEPlugin.getResourceString(KEY_SYNCHRONIZING), size);
	for (int i = 0; i < plugins.length; i++) {
		synchronizeVersion(mode, feature.getVersion(), plugins[i], monitor);
	}
}
private int saveSettings() {
	IDialogSettings settings = getDialogSettings();

	int mode = USE_FEATURE;

	if (fUsePluginsButton.getSelection())
		mode = USE_PLUGINS;
	else
		if (fUseReferencesButton.getSelection())
			mode = USE_REFERENCES;
	settings.put(PROP_SYNCHRO_MODE, mode);
	return mode;
}
private void synchronizeVersion(
	int mode,
	String featureVersion,
	IFeaturePlugin ref,
	IProgressMonitor monitor)
	throws CoreException,
	BadLocationException{
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
		if (!ref.getVersion().equals(baseVersion)) {
			ref.setVersion(baseVersion);
		}
	} else {
		String targetVersion = featureVersion;
		if (mode == USE_REFERENCES)
			targetVersion = ref.getVersion();
		else
			ref.setVersion(targetVersion);
		
		String baseVersion = modelBase.getPluginBase().getVersion();
		if (!targetVersion.equals(baseVersion)) {
			forceVersion(targetVersion, modelBase, monitor);
		}
	}
	monitor.worked(1);
}
}
