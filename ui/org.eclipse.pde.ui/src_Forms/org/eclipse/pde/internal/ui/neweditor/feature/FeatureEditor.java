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
package org.eclipse.pde.internal.ui.neweditor.feature;

import java.io.File;

import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.build.*;
import org.eclipse.pde.internal.ui.neweditor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;

public class FeatureEditor extends MultiSourceEditor {
	public static final String UNRESOLVED_TITLE =
		"FeatureEditor.Unresolved.title";
	public static final String VERSION_TITLE = "FeatureEditor.Version.title";
	public static final String VERSION_MESSAGE =
		"FeatureEditor.Version.message";
	public static final String VERSION_EXISTS = "FeatureEditor.Version.exists";
	public static final String UNRESOLVED_MESSAGE =
		"FeatureEditor.Unresolved.message";
	public static final String FEATURE_PAGE_TITLE =
		"FeatureEditor.FeaturePage.title";
	public static final String REFERENCE_PAGE_TITLE =
		"FeatureEditor.ReferencePage.title";
	public static final String ADVANCED_PAGE_TITLE =
		"FeatureEditor.AdvancedPage.title";
	public static final String INFO_PAGE_TITLE = "FeatureEditor.InfoPage.title";
	private boolean storageModel=false;

	public FeatureEditor() {
	}
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile buildFile = null;
		IFile featureFile = null;

		String name = file.getName().toLowerCase();
		if (name.equals("feature.xml")) {
			featureFile = file;
			buildFile = project.getFile("build.properties");
		} else if (name.equals("build.properties")) {
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
		FeatureInputContextManager manager =  new FeatureInputContextManager();
		//manager.setUndoManager(new PluginUndoManager(this));
		return manager;
	}
	
	public void monitoredFileAdded(IFile file) {
		String name = file.getName();
		 if (name.equalsIgnoreCase("feature.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new FeatureInputContext(this, in, false));						
		}
		else if (name.equalsIgnoreCase("build.properties")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new BuildInputContext(this, in, false));			
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}
	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
	}
	public void contextRemoved(InputContext context) {
		IFormPage page = findPage(context.getId());
		if (page!=null)
			removePage(context.getId());
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File buildFile = null;
		File featureFile = null;
		String name = file.getName().toLowerCase();
		if (name.equals("feature.xml")) {
			featureFile = file;
			File dir = file.getParentFile();
			buildFile = new File(dir, "build.properties");
		} else if (name.equals("build.properties")) {
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
		File pluginFile = new File(dir, "plugin.xml");
		return pluginFile;
	}
	private IFile createFeatureFile(IProject project) {
		IFile featureFile = project.getFile("feature.xml");
		return featureFile;
	}
	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		String name = input.getName().toLowerCase();
		if (name.equals("build.properties")) {
			manager.putContext(input, new BuildInputContext(this, input, true));
		} else if (name.equals("feature.xml")) {
			manager.putContext(input, new FeatureInputContext(this, input, true));
		}
	}

	public boolean canCopy(ISelection selection) {
		return true;
	}
	
	protected void addPages() {
		try {
			addPage(new FeatureFormPage(this, PDEPlugin.getResourceString(FEATURE_PAGE_TITLE)));
			addPage(new InfoFormPage(this, PDEPlugin.getResourceString(INFO_PAGE_TITLE)));
			if (inputContextManager.hasContext(BuildInputContext.CONTEXT_ID))
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
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary.getId().equals(FeatureInputContext.CONTEXT_ID))
				firstPageId = FeatureFormPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = FeatureFormPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createXMLSourcePage(PDEFormEditor editor, String title, String name) {
		return new FeatureSourcePage(editor, title, name);
	}
	
	protected IContentOutlinePage createContentOutline() {
		return new FeatureOutlinePage(this);
	}
	
	protected IPropertySheetPage getPropertySheet(PDEFormPage page) {
		return null;
	}

	public String getTitle() {
		if (!isModelCorrect(getAggregateModel()))
			return super.getTitle();
		IFeatureModel model = (IFeatureModel) getAggregateModel();
		String name = model.getFeature().getLabel();
		if (name == null)
			return super.getTitle();
		return model.getResourceString(name);
	}

	protected boolean isModelCorrect(Object model) {
		return model != null ? ((IFeatureModel) model).isValid() : false;
	}
	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers =
				new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
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

	public void updateTitle() {
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}
	public Object getAdapter(Class key) {
		//No property sheet needed - block super
		if (key.equals(IPropertySheetPage.class)) {
			return null;
		}
		return super.getAdapter(key);
	}	
}
