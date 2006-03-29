/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.builders.PDEMarkerFactory;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.text.AbstractEditingModel;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeWizard;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.ide.IDE;

public abstract class AbstractPDEMarkerResolution implements IMarkerResolution2 {

	public static final int CREATE_TYPE = 1;
	public static final int RENAME_TYPE = 2;
	public static final int REMOVE_TYPE = 3;
	
	protected int fType;
	protected IResource fResource;
	private PDEFormEditor fOpenEditor;

	public AbstractPDEMarkerResolution(int type) {
		fType = type;
	}
	
	public Image getImage() {
		return null;
	}

	public int getType() {
		return fType;
	}

	public String getDescription() {
		return getLabel();
	}
	
	public void run(IMarker marker) {
		fResource = marker.getResource();
		IBaseModel model = findModelFromEditor(marker);
		
		if (model != null) {
			// directly modify model from open editor and save
			createChange(model);
			fOpenEditor.doSave(null);
			fOpenEditor = null;
		} else {
			// create text edits and apply them to textbuffer
			try {
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(fResource.getFullPath(), null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(fResource.getFullPath());
				if (buffer.isDirty())
					buffer.commit(null, true);
				IDocument document = buffer.getDocument();	
				IModel editModel = loadModel(document);
				IModelTextChangeListener listener = createListener(document);
				if (!editModel.isLoaded() || listener == null)
					return;
				if (editModel instanceof IModelChangeProvider)
					((IModelChangeProvider)editModel).addModelChangedListener(listener);
				createChange(editModel);
				TextEdit[] edits = listener.getTextOperations();
				if (edits.length > 0) {
					MultiTextEdit multi = new MultiTextEdit();
					multi.addChildren(edits);
					multi.apply(document);
					buffer.commit(null, true);
				}
			} catch (CoreException e) {
				PDEPlugin.log(e);
			} catch (MalformedTreeException e) {
				PDEPlugin.log(e);
			} catch (BadLocationException e) {
				PDEPlugin.log(e);
			} finally {
				try {
					FileBuffers.getTextFileBufferManager().disconnect(fResource.getFullPath(), null);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
	}
	
	private IBaseModel findModelFromEditor(IMarker marker) {
		IProject project = fResource.getProject();
		String name = fResource.getName();
		if (name.equals("plugin.xml") || name.equals("fragment.xml")) { //$NON-NLS-1$ //$NON-NLS-2$
			fOpenEditor = EditorUtilities.getOpenManifestEditor(project);
			if (fOpenEditor != null) {
				IBaseModel model = fOpenEditor.getAggregateModel();
				if (model instanceof IBundlePluginModelBase)
					model = ((IBundlePluginModelBase)model).getExtensionsModel();
				if (model instanceof AbstractEditingModel)
					return model;
			}
		} else if (name.equals("build.properties")) { //$NON-NLS-1$
			fOpenEditor = EditorUtilities.getOpenBuildPropertiesEditor(project);
			if (fOpenEditor != null) {
				IBaseModel model = fOpenEditor.getAggregateModel();
				if (model instanceof AbstractEditingModel)
					return model;
			} else {
				fOpenEditor = EditorUtilities.getOpenManifestEditor(project);
				if (fOpenEditor == null)
					return null;
				IFormPage page = fOpenEditor.findPage(BuildInputContext.CONTEXT_ID);
				if (page instanceof BuildSourcePage) {
					IBaseModel model = ((BuildSourcePage)page).getInputContext().getModel();
					if (model instanceof AbstractEditingModel)
						return model;
				}
			}
		} else if (name.equals("MANIFEST.MF")) { //$NON-NLS-1$
			fOpenEditor = EditorUtilities.getOpenManifestEditor(project);
			if (fOpenEditor == null)
				return null;
			IBaseModel model = fOpenEditor.getAggregateModel();
			if (model instanceof IBundlePluginModel) {
				try {
					if (marker.getAttribute(PDEMarkerFactory.MPK_LOCATION_PATH) != null)
						return model;
				} catch (CoreException e) {
				}
				model = ((IBundlePluginModel)model).getBundleModel();
				if (model instanceof AbstractEditingModel)
					return model;
			}
		}
		return null;
	}
	
	protected abstract void createChange(IBaseModel model);
	
	protected abstract IModel loadModel(IDocument doc);
	
	protected abstract IModelTextChangeListener createListener(IDocument doc);
	
	protected String selectType() {
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(
					PDEPlugin.getActiveWorkbenchShell(),
					PlatformUI.getWorkbench().getProgressService(),
					SearchEngine.createWorkspaceScope(),
					IJavaElementSearchConstants.CONSIDER_ALL_TYPES, 
			        false, ""); //$NON-NLS-1$
			dialog.setTitle(PDEUIMessages.ClassAttributeRow_dialogTitle); 
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (JavaModelException e) {
		}
		return null;
	}
	
	protected String createClass(String name, IPluginModelBase model, JavaAttributeValue value) {
		IProject project = model.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement result = null;
				if (name.length() > 0)
					result = javaProject.findType(name);
				if (result != null)
					JavaUI.openInEditor(result);
				else {
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK) {
						name = wizard.getClassNameWithArgs();
						result = javaProject.findType(name);
						if (result != null)
							JavaUI.openInEditor(result);
					}
				}
			} else {
				IResource resource = project.findMember(new Path(name));
				if (resource != null && resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				} else {
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK) {
						String newValue = wizard.getClassName();
						name = newValue.replace('.', '/') + ".java"; //$NON-NLS-1$
						resource = project.findMember(new Path(name));
						if (resource != null && resource instanceof IFile) {
							IWorkbenchPage page = PDEPlugin.getActivePage();
							IDE.openEditor(page, (IFile) resource, true);
						}
					}
				}
			}
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return name;
	}
	
	protected String trimNonAlphaChars(String value) {
		value = value.trim();
		while (value.length() > 0 && !Character.isLetter(value.charAt(0)))
			value = value.substring(1, value.length());
		int loc = value.indexOf(":"); //$NON-NLS-1$
		if (loc != -1 && loc > 0)
			value = value.substring(0, loc);
		else if (loc == 0)
			value = ""; //$NON-NLS-1$
		return value;
	}
}
