package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.lang.reflect.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.feature.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.jface.action.*;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.core.runtime.*;

public class EditorBuildFeatureAction extends Action {
	public static final String LABEL = "FeatureEditor.BuildAction.label";
	BuildFeatureAction buildDelegate;
	FeatureEditor activeEditor;

public EditorBuildFeatureAction() {
	setText(PDEPlugin.getResourceString(LABEL));
	buildDelegate = new BuildFeatureAction();

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
	buildDelegate.run(this);
}
public void setActiveEditor(FeatureEditor editor) {
	this.activeEditor = editor;
	buildDelegate.setActivePart(this, editor);
	IFeatureModel model = (IFeatureModel) editor.getModel();
	buildDelegate.setFeatureFile((IFile) model.getUnderlyingResource());
	setEnabled(model.isEditable());
}
}
