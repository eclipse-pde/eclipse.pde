/*
 * Created on Jan 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.texteditor.*;
/**
 * This class maintains objects associated with a single editor input.
 */
public abstract class InputContext {
	private PDEFormEditor editor;
	private IEditorInput input;
	private IModel model;
	private IModelChangedListener modelListener;
	private IDocumentProvider documentProvider;
	private IElementStateListener elementListener;
	private boolean validated;

	class ElementListener implements IElementStateListener {
		public void elementContentAboutToBeReplaced(Object element) {
		}
		public void elementContentReplaced(Object element) {
			//updateModel();
			//editor.fireSaveNeeded();
		}
		public void elementDeleted(Object element) {
		}
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
		}
		public void elementMoved(Object originalElement, Object movedElement) {
			editor.close(true);
		}
	}
	public InputContext(PDEFormEditor editor, IEditorInput input) {
		this.editor = editor;
		this.input = input;
		create();
	}
	public abstract String getId();

	public IEditorInput getInput() {
		return input;
	}
	public IModel getModel() {
		return model;
	}
	public IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}
	protected abstract IDocumentProvider createDocumentProvider(
			IEditorInput input);
	protected abstract IModel createModel(IEditorInput input);
	
	protected void create() {
		documentProvider = createDocumentProvider(input);
		if (documentProvider == null)
			return;
		model = createModel(input);
		if (model instanceof IModelChangeProvider) {
			modelListener = new IModelChangedListener() {
				public void modelChanged(IModelChangedEvent e) {
					if (e.getChangeType() != IModelChangedEvent.WORLD_CHANGED)
						editor.fireSaveNeeded(input);
				}
			};
			((IModelChangeProvider) model).addModelChangedListener(modelListener);
		}

		try {
			documentProvider.connect(input);
			IAnnotationModel amodel = documentProvider
					.getAnnotationModel(input);
			if (amodel != null)
				amodel.connect(documentProvider.getDocument(input));
			elementListener = new ElementListener();
			documentProvider.addElementStateListener(elementListener);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	public void validateEdit() {
		if (!validated) {
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				Shell shell = editor.getEditorSite().getShell();
				IStatus validateStatus = PDEPlugin.getWorkspace().validateEdit(
						new IFile[]{file}, shell);
				if (validateStatus.getCode() != IStatus.OK)
					ErrorDialog.openError(shell, editor.getTitle(), null,
							validateStatus);
			}
			validated = true;
		}
	}
	public void doSave(IProgressMonitor monitor) {
		WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
		public void execute(final IProgressMonitor monitor)
				throws CoreException {
				documentProvider.saveDocument(
					monitor,
					input,
					documentProvider.getDocument(input),
					true);
			}
		};

		try {
			documentProvider.aboutToChange(input);
			op.run(monitor);
			documentProvider.changed(input);
		} catch (InterruptedException x) {
		} catch (InvocationTargetException x) {
			PDEPlugin.logException(x);
		}
	}
	public boolean mustSave() {
		return documentProvider.mustSaveDocument(input);
	}
	public void dispose() {
		IAnnotationModel amodel = documentProvider.getAnnotationModel(input);
		if (amodel != null)
			amodel.disconnect(documentProvider.getDocument(input));
		documentProvider.disconnect(input);
		if (modelListener != null && model instanceof IModelChangeProvider) {
			((IModelChangeProvider) model)
					.removeModelChangedListener(modelListener);
			//if (undoManager != null)
			//undoManager.disconnect((IModelChangeProvider) model);
		}
	}
}
