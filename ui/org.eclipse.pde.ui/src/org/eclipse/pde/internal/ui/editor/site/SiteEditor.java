/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 262977
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.site;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Locale;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.exports.SiteBuildOperation;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.core.site.WorkspaceSiteModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.ui.*;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;

public class SiteEditor extends MultiSourceEditor {

	private Action fBuildAllAction;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IPDEUIConstants.SITE_EDITOR_ID;
	}

	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		IFile file = input.getFile();
		IFile siteFile = null;
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals("site.xml")) { //$NON-NLS-1$
			siteFile = file;
			if (siteFile.exists()) {
				IEditorInput in = new FileEditorInput(siteFile);
				manager.putContext(in, new SiteInputContext(this, in, file == siteFile));
			}
			manager.monitorFile(siteFile);
		}
	}

	protected InputContextManager createInputContextManager() {
		SiteInputContextManager contextManager = new SiteInputContextManager(this);
		contextManager.setUndoManager(new SiteUndoManager(this));
		return contextManager;
	}

	public void monitoredFileAdded(IFile file) {
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}

	public void editorContextAdded(InputContext context) {
		addSourcePage(context.getId());
	}

	public void contextRemoved(InputContext context) {
		close(false);
	}

	protected void createSystemFileContexts(InputContextManager manager, FileStoreEditorInput input) {
		File file = new File(input.getURI());
		File siteFile = null;

		String name = file.getName().toLowerCase(Locale.ENGLISH);
		if (name.equals("site.xml")) { //$NON-NLS-1$
			siteFile = file;
			if (siteFile.exists()) {
				IFileStore store;
				try {
					store = EFS.getStore(siteFile.toURI());
					IEditorInput in = new FileStoreEditorInput(store);
					manager.putContext(in, new SiteInputContext(this, in, file == siteFile));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		String name = input.getName().toLowerCase(Locale.ENGLISH);
		if (name.startsWith("site.xml")) { //$NON-NLS-1$
			manager.putContext(input, new SiteInputContext(this, input, true));
		}
	}

	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}

	protected void addEditorPages() {
		try {
			addPage(new FeaturesPage(this));
			addPage(new ArchivePage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SiteInputContext.CONTEXT_ID);
	}

	protected String computeInitialPageId() {
		return FeaturesPage.PAGE_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new SiteSourcePage(editor, title, name);
	}

	protected ISortableContentOutlinePage createContentOutline() {
		return new SiteOutlinePage(this);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		InputContext context = null;
		if (object instanceof ISiteObject) {
			context = fInputContextManager.findContext(SiteInputContext.CONTEXT_ID);
		}
		return context;
	}

	public void contributeToToolbar(IToolBarManager manager) {
		manager.add(getBuildAllAction());
	}

	protected Action getBuildAllAction() {
		if (fBuildAllAction == null) {
			fBuildAllAction = new Action() {
				public void run() {
					handleBuild(((ISiteModel) getAggregateModel()).getSite().getFeatures());
				}
			};
			fBuildAllAction.setToolTipText(PDEUIMessages.CategorySection_buildAll);
			fBuildAllAction.setImageDescriptor(PDEPluginImages.DESC_BUILD_TOOL);
			updateActionEnablement();

			((ISiteModel) getAggregateModel()).addModelChangedListener(new IModelChangedListener() {
				public void modelChanged(IModelChangedEvent event) {
					updateActionEnablement();
				}
			});
		}
		return fBuildAllAction;
	}

	private void updateActionEnablement() {
		if (((ISiteModel) getAggregateModel()).getSite().getFeatures().length > 0)
			fBuildAllAction.setEnabled(true);
		else
			fBuildAllAction.setEnabled(false);
	}

	protected void handleBuild(ISiteFeature[] sFeatures) {
		if (sFeatures.length == 0)
			return;
		IFeatureModel[] models = getFeatureModels(sFeatures);
		if (models.length == 0)
			return;
		ensureContentSaved();
		ISiteModel buildSiteModel = new WorkspaceSiteModel((IFile) ((IModel) getAggregateModel()).getUnderlyingResource());
		try {
			buildSiteModel.load();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
			return;
		}

		Job job = new SiteBuildOperation(models, buildSiteModel, PDEUIMessages.BuildSiteJob_name);
		job.setUser(true);
		job.schedule();
	}

	private IFeatureModel[] getFeatureModels(ISiteFeature[] sFeatures) {
		ArrayList list = new ArrayList();
		for (int i = 0; i < sFeatures.length; i++) {
			IFeatureModel model = PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(sFeatures[i].getId(), sFeatures[i].getVersion());
			if (model != null)
				list.add(model);
		}
		return (IFeatureModel[]) list.toArray(new IFeatureModel[list.size()]);
	}

	private void ensureContentSaved() {
		if (isDirty()) {
			try {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						doSave(monitor);
					}
				};
				PlatformUI.getWorkbench().getProgressService().runInUI(PDEPlugin.getActiveWorkbenchWindow(), op, PDEPlugin.getWorkspace().getRoot());
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

}