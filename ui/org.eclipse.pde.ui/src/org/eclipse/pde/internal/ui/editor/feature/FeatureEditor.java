/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.io.File;
import java.util.Locale;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.internal.core.build.IBuildObject;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureObject;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
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
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IShowEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class FeatureEditor extends MultiSourceEditor implements IShowEditorInput {

	public static void openFeatureEditor(IFeature feature) {
		if(feature!=null){
			IFeatureModel model = feature.getModel();
			openFeatureEditor(model);
		} else {
			Display.getCurrent().beep();
		}
	}

	public static void openFeatureEditor(IFeatureModel model) {
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			try {
				IEditorInput input = null;
				if (resource != null)
					input = new FileEditorInput((IFile) resource);
				else
					input = new SystemFileEditorInput(new File(model
							.getInstallLocation(), "feature.xml")); //$NON-NLS-1$
				IDE.openEditor(PDEPlugin.getActivePage(), input,
						IPDEUIConstants.FEATURE_EDITOR_ID, true);
			} catch (PartInitException e) {
			}
		} else {
			Display.getCurrent().beep();
		}

	}

	public FeatureEditor() {
	}

	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile buildFile = null;
		IFile featureFile = null;

		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals("feature.xml")) { //$NON-NLS-1$
			featureFile = file;
			buildFile = project.getFile("build.properties"); //$NON-NLS-1$
		} else if (name.equals("build.properties")) { //$NON-NLS-1$
			buildFile = file;
			featureFile = createFeatureFile(project);
		}
		if (featureFile.exists()) {
			FileEditorInput in = new FileEditorInput(featureFile);
			manager.putContext(in, new FeatureInputContext(this, in,
					file == featureFile));
		}
		if (buildFile.exists()) {
			FileEditorInput in = new FileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in,
					file == buildFile));
		}
		manager.monitorFile(featureFile);
		manager.monitorFile(buildFile);
	}

	protected InputContextManager createInputContextManager() {
		FeatureInputContextManager manager = new FeatureInputContextManager(
				this);
		manager.setUndoManager(new FeatureUndoManager(this));
		return manager;
	}

	public void monitoredFileAdded(IFile file) {
		String name = file.getName();
		if (name.equalsIgnoreCase("feature.xml")) { //$NON-NLS-1$
			/*
			 * IEditorInput in = new FileEditorInput(file);
			 * inputContextManager.putContext(in, new FeatureInputContext(this,
			 * in, false));
			 */
		} else if (name.equalsIgnoreCase("build.properties")) { //$NON-NLS-1$
			if (!fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID)) {
				IEditorInput in = new FileEditorInput(file);
				fInputContextManager.putContext(in, new BuildInputContext(this,
						in, false));
			}
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		// TODO may need to check with the user if there
		// are unsaved changes in the model for the
		// file that just got removed under us.
		return true;
	}

	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	public void contextRemoved(InputContext context) {
		if (context.isPrimary()) {
			close(true);
			return;
		}
		IFormPage page = findPage(context.getId());
		if (page != null)
			removePage(context.getId());
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File buildFile = null;
		File featureFile = null;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals("feature.xml")) { //$NON-NLS-1$
			featureFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, "build.properties"); //$NON-NLS-1$
		} else if (name.equals("build.properties")) { //$NON-NLS-1$
			buildFile = file;
			File dir = file.getParentFile();
			featureFile = createFeatureFile(dir);
		}
		if (featureFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(featureFile);
			manager.putContext(in, new FeatureInputContext(this, in,
					file == featureFile));
		}
		if (buildFile.exists()) {
			SystemFileEditorInput in = new SystemFileEditorInput(buildFile);
			manager.putContext(in, new BuildInputContext(this, in,
					file == buildFile));
		}
	}

	private File createFeatureFile(File dir) {
		File pluginFile = new File(dir, "plugin.xml"); //$NON-NLS-1$
		return pluginFile;
	}

	private IFile createFeatureFile(IProject project) {
		IFile featureFile = project.getFile("feature.xml"); //$NON-NLS-1$
		return featureFile;
	}

	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		String name = input.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals("build.properties")) { //$NON-NLS-1$
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.startsWith("feature.xml")) { //$NON-NLS-1$
			manager.putContext(input,
					new FeatureInputContext(this, input, true));
		}
	}

	protected void addPages() {
		try {
			addPage(new FeatureFormPage(this, PDEUIMessages.FeatureEditor_FeaturePage_title));
			addPage(new InfoFormPage(this, PDEUIMessages.FeatureEditor_InfoPage_title));
			addPage(new FeatureReferencePage(this, PDEUIMessages.FeatureEditor_ReferencePage_title));
			addPage(new FeatureIncludesPage(this, PDEUIMessages.FeatureEditor_IncludesPage_title));
			addPage(new FeatureDependenciesPage(this, PDEUIMessages.FeatureEditor_DependenciesPage_title));
			addPage(new FeatureAdvancedPage(this, PDEUIMessages.FeatureEditor_AdvancedPage_title));
			if (fInputContextManager.hasContext(BuildInputContext.CONTEXT_ID))
				addPage(new BuildPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(FeatureInputContext.CONTEXT_ID);
		addSourcePage(BuildInputContext.CONTEXT_ID);
	}

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor,
	 *      java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor,
			String title, String name, String contextId) {
		if (contextId.equals(FeatureInputContext.CONTEXT_ID))
			return new FeatureSourcePage(editor, title, name);
		if (contextId.equals(BuildInputContext.CONTEXT_ID))
			return new BuildSourcePage(editor, title, name);
		return super.createSourcePage(editor, title, name, contextId);
	}

	protected ISortableContentOutlinePage createContentOutline() {
		return new FeatureOutlinePage(this);
	}

	protected IPropertySheetPage getPropertySheet(PDEFormPage page) {
		return null;
	}

	public String getTitle() {
		if (!isModelCorrect(getAggregateModel()))
			return super.getTitle();
		IFeatureModel model = (IFeatureModel) getAggregateModel();
		String name = getTitleText(model.getFeature());
		if (name == null)
			return super.getTitle();
		return model.getResourceString(name);
	}

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
			return feature.getLabel();
		return feature.getId();
	}

	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IFeatureModel) model).isValid() : false;
	}

	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(),
					RTFTransfer.getInstance() };
			for (int i = 0; i < types.length; i++) {
				for (int j = 0; j < transfers.length; j++) {
					if (transfers[j].isSupportedType(types[i]))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}

	public Object getAdapter(Class key) {
		// No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof IBuildObject) {
			context = fInputContextManager
					.findContext(BuildInputContext.CONTEXT_ID);
		} else if (object instanceof IFeatureObject) {
			context = fInputContextManager
					.findContext(FeatureInputContext.CONTEXT_ID);
		}
		return context;
	}

	protected boolean isPatchEditor() {
		IBaseModel model = getAggregateModel();
		if(model==null || !(model instanceof IFeatureModel)){
			return false;
		}
		IFeature feature = ((IFeatureModel)model).getFeature();
		IFeatureImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isPatch()) {
				return true;
			}
		}
		return false;
	}

	public void showEditorInput(IEditorInput editorInput) {
    	String name = editorInput.getName();
    	if (name.equals("feature.xml")) { //$NON-NLS-1$
    		setActivePage(0);
    	} else {
    		setActivePage(getPageCount() - 3);
    	}
	}
}
