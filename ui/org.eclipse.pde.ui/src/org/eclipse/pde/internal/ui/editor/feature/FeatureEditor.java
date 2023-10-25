/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *     Christoph Läubrich - Bug 576610 - FeatureEditor should support display of non-file-based feature models
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.io.File;
import java.util.Locale;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureObject;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildPage;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class FeatureEditor extends MultiSourceEditor implements IShowEditorInput {

	private Action fExportAction;

	@Override
	protected String getEditorID() {
		return IPDEUIConstants.FEATURE_EDITOR_ID;
	}

	public static void openFeatureEditor(IFeature feature) {
		if (feature != null) {
			IFeatureModel model = feature.getModel();
			openFeatureEditor(model);
		} else {
			Display.getCurrent().beep();
		}
	}

	/**
	 * Opens Feature Editor on "Included Plug-ins" page and preselects the
	 * featurePlugin.
	 *
	 * @param featurePlugin
	 *            is included plug-in in feature
	 */
	public static void openFeatureEditor(final IFeaturePlugin featurePlugin) {
		if (featurePlugin != null) {
			IEditorPart editor = openFeatureEditor(featurePlugin.getModel());
			// activate the page with plug-ins and preselect the requested
			// featurePlugin
			if (editor instanceof FeatureEditor) {
				IFormPage page = ((FeatureEditor) editor).setActivePage(FeatureReferencePage.PAGE_ID);
				page.selectReveal(featurePlugin);
			}
		} else {
			Display.getCurrent().beep();
		}
	}

	public static IEditorPart openFeatureEditor(IFeatureModel model) {
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			try {
				IEditorInput input = null;
				if (resource != null)
					input = new FileEditorInput((IFile) resource);
				else {
					String installLocation = model.getInstallLocation();
					if (installLocation == null) {
						input = new FeatureModelEditorInput(model);
					} else {
						File file = new File(installLocation, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
						IFileStore store = EFS.getStore(file.toURI());
						input = new FileStoreEditorInput(store);
					}
				}
				return IDE.openEditor(PDEPlugin.getActivePage(), input, IPDEUIConstants.FEATURE_EDITOR_ID, true);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		} else {
			Display.getCurrent().beep();
		}
		return null;
	}

	public FeatureEditor() {
	}

	@Override
	protected void createInputContexts(InputContextManager contextManager) {
		IEditorInput editorInput = getEditorInput();
		if (editorInput instanceof FeatureModelEditorInput) {
			contextManager.putContext(editorInput, new FeatureInputContext(this, editorInput, true));
		} else {
			super.createInputContexts(contextManager);
		}
	}

	@Override
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile buildFile = null;
		IFile featureFile = null;

		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)) {
			featureFile = file;
			buildFile = PDEProject.getBuildProperties(project);
		} else if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			buildFile = file;
			featureFile = createFeatureFile(project);
		}
		if (featureFile != null && featureFile.exists()) {
			FileEditorInput in = new FileEditorInput(featureFile);
			manager.putContext(in, new FeatureInputContext(this, in, file == featureFile));
		}
		if (buildFile.exists()) {
			FileEditorInput in = new FileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in, file == buildFile));
		}
		manager.monitorFile(featureFile);
		manager.monitorFile(buildFile);
	}

	@Override
	protected InputContextManager createInputContextManager() {
		FeatureInputContextManager manager = new FeatureInputContextManager(this);
		manager.setUndoManager(new FeatureUndoManager(this));
		return manager;
	}

	@Override
	public void monitoredFileAdded(IFile file) {
		if (fInputContextManager == null)
			return;
		String name = file.getName();
		if (name.equalsIgnoreCase(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)) {
			/*
			 * IEditorInput in = new FileEditorInput(file);
			 * inputContextManager.putContext(in, new FeatureInputContext(this,
			 * in, false));
			 */
		} else if (name.equalsIgnoreCase(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			if (!fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BuildInputContext(this, in, false));
			}
		}
	}

	@Override
	public boolean monitoredFileRemoved(IFile file) {
		// TODO may need to check with the user if there
		// are unsaved changes in the model for the
		// file that just got removed under us.
		return true;
	}

	@Override
	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	@Override
	public void contextRemoved(InputContext context) {
		if (context.isPrimary()) {
			close(true);
			return;
		}
		IFormPage page = findPage(context.getId());
		if (page != null)
			removePage(context.getId());
	}

	@Override
	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		File file = new File(input.getURI());
		File buildFile = null;
		File featureFile = null;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)) {
			featureFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
		} else if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			buildFile = file;
			File dir = file.getParentFile();
			featureFile = createFeatureFile(dir);
		}
		try {
			if (featureFile.exists()) {
				IFileStore store = EFS.getStore(featureFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new FeatureInputContext(this, in, file == featureFile));
			}
			if (buildFile.exists()) {
				IFileStore store = EFS.getStore(buildFile.toURI());
				IEditorInput in = new FileStoreEditorInput(store);
				manager.putContext(in, new BuildInputContext(this, in, file == buildFile));
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private File createFeatureFile(File dir) {
		File pluginFile = new File(dir, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		return pluginFile;
	}

	private IFile createFeatureFile(IProject project) {
		return PDEProject.getFeatureXml(project);
	}

	@Override
	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		String name = input.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals(ICoreConstants.BUILD_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.startsWith(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)) {
			manager.putContext(input, new FeatureInputContext(this, input, true));
		}
	}

	@Override
	protected void addEditorPages() {
		try {
			addPage(new FeatureFormPage(this, PDEUIMessages.FeatureEditor_FeaturePage_title));
			addPage(new InfoFormPage(this, PDEUIMessages.FeatureEditor_InfoPage_title));
			addPage(new FeatureReferencePage(this, PDEUIMessages.FeatureEditor_ReferencePage_title));
			addPage(new FeatureIncludesPage(this, PDEUIMessages.FeatureEditor_IncludesPage_title));
			addPage(new FeatureDependenciesPage(this, PDEUIMessages.FeatureEditor_DependenciesPage_title));
			if (fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID))
				addPage(new BuildPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		if (getEditorInput() instanceof FeatureModelEditorInput) {
			// a model input has no source!
			return;
		}
		addSourcePage(FeatureInputContext.CONTEXT_ID);
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}

	@Override
	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = fInputContextManager.getPrimaryContext();
			if (primary != null && FeatureInputContext.CONTEXT_ID.equals(primary.getId()))
				firstPageId = FeatureFormPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = FeatureFormPage.PAGE_ID;
		}
		return firstPageId;
	}

	@Override
	protected IEditorPart createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		if (contextId.equals(FeatureInputContext.CONTEXT_ID))
			return new FeatureSourcePage(editor, title, name);
		if (contextId.equals(BuildInputContext.CONTEXT_ID))
			return new BuildSourcePage(editor, title, name);
		return super.createSourcePage(editor, title, name, contextId);
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new FeatureOutlinePage(this);
	}

	protected IPropertySheetPage getPropertySheet(PDEFormPage page) {
		return null;
	}

	@Override
	public String getTitle() {
		IFeatureModel model = (IFeatureModel) getAggregateModel();
		if (!isModelCorrect(model)) {
			return super.getTitle();
		}
		String name = getTitleText(model.getFeature());
		if (name == null) {
			return super.getTitle();
		}
		return model.getResourceString(name);
	}

	@Override
	public String getTitleProperty() {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref != null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES))
			return IFeatureObject.P_LABEL;
		return IIdentifiable.P_ID;
	}

	private String getTitleText(IFeature feature) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		String pref = store.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
		if (pref != null && pref.equals(IPreferenceConstants.VALUE_USE_NAMES))
			return feature.getTranslatableLabel();
		return feature.getId();
	}

	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IFeatureModel) model).isValid() : false;
	}

	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance() };
			for (TransferData type : types) {
				for (Transfer transfer : transfers) {
					if (transfer.isSupportedType(type))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}

	@Override
	public <T> T getAdapter(Class<T> key) {
		// No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}

	@Override
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IBuildObject) {
			context = fInputContextManager.findContext(BuildInputContext.CONTEXT_ID);
		} else if (object instanceof IFeatureObject) {
			context = fInputContextManager.findContext(FeatureInputContext.CONTEXT_ID);
		}
		return context;
	}

	protected boolean isPatchEditor() {
		IBaseModel model = getAggregateModel();
		if (model == null || !(model instanceof IFeatureModel)) {
			return false;
		}
		IFeature feature = ((IFeatureModel) model).getFeature();
		IFeatureImport[] imports = feature.getImports();
		for (IFeatureImport featureImport : imports) {
			if (featureImport.isPatch()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void showEditorInput(IEditorInput editorInput) {
		String name = editorInput.getName();
		if (name.equals(ICoreConstants.FEATURE_FILENAME_DESCRIPTOR)) {
			setActivePage(0);
		} else {
			setActivePage(getPageCount() - 3);
		}
	}

	protected Action getFeatureExportAction() {
		if (fExportAction == null) {
			fExportAction = new Action() {
				@Override
				public void run() {
					doSave(null);
					FeatureEditorContributor contributor = (FeatureEditorContributor) getContributor();
					contributor.getBuildAction().run();
				}
			};
			fExportAction.setToolTipText(PDEUIMessages.FeatureEditor_exportTooltip);
			fExportAction.setImageDescriptor(PDEPluginImages.DESC_EXPORT_FEATURE_TOOL);
		}
		return fExportAction;
	}

	@Override
	public void contributeToToolbar(IToolBarManager manager) {
		manager.add(getFeatureExportAction());
	}
}
