package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.*;
import org.eclipse.update.core.*;

public class PreviewAction extends Action {
	public static final String LABEL = "FeatureEditor.previewAction.label";
	private FeatureEditor activeEditor;

	public PreviewAction() {
		setText(PDEPlugin.getResourceString(LABEL));
	}
	private void ensureContentSaved() {
		if (activeEditor.isDirty()) {
			ProgressMonitorDialog monitor =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			try {
				monitor.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						activeEditor.doSave(monitor);
					}
				});
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

	public void run() {
		ensureContentSaved();
		IWorkbenchPage page = PDEPlugin.getActivePage();
		IFeature feature = createUpdateFeature();
		if (feature == null)
			return;

		try {
			IViewPart view = page.showView("org.eclipse.update.ui.DetailsView");
			StructuredSelection selection = new StructuredSelection(feature);
			((ISelectionListener) view).selectionChanged(activeEditor, selection);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	private IFeature createUpdateFeature() {
		IFileEditorInput input = (IFileEditorInput) activeEditor.getEditorInput();
		IFile file = input.getFile();
		IPath fullPath = Platform.getLocation().append(file.getFullPath());
		try {
			File systemFile = fullPath.toFile();
			URL url = systemFile.toURL();
			FeatureReference fref = new FeatureReference();
			fref.setURL(url);
			
			fref.setType(ISite.DEFAULT_INSTALLED_FEATURE_TYPE);
			return fref.getFeature(null);
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}

	public void setActiveEditor(FeatureEditor editor) {
		this.activeEditor = editor;
	}
}