/*
 * Created on Jan 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.context;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.PDEFormEditor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.texteditor.*;
/**
 * This class maintains objects associated with a single editor input.
 */
public abstract class InputContext {
	private PDEFormEditor editor;
	private IEditorInput input;
	private IBaseModel model;
	private IModelChangedListener modelListener;
	private IDocumentProvider documentProvider;
	private IElementStateListener elementListener;
	private boolean validated;
	private boolean primary;
	protected ArrayList fEditOperations = new ArrayList();
	private boolean fIsSourceMode;

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
	public InputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		this.editor = editor;
		this.input = input;
		setPrimary(primary);
	}
	public abstract String getId();

	public IEditorInput getInput() {
		return input;
	}
	public PDEFormEditor getEditor() {
		return editor;
	}
	public IBaseModel getModel() {
		return model;
	}
	public IDocumentProvider getDocumentProvider() {
		return documentProvider;
	}
	protected abstract IDocumentProvider createDocumentProvider(
			IEditorInput input);
	
	protected abstract IBaseModel createModel(IEditorInput input) throws CoreException;
	
	protected void create() {
		documentProvider = createDocumentProvider(input);
		if (documentProvider == null)
			return;
		try {
			documentProvider.connect(input);
			model = createModel(input);
			if (model instanceof IModelChangeProvider) {
				modelListener = new IModelChangedListener() {
					public void modelChanged(IModelChangedEvent e) {
						if (e.getChangeType() != IModelChangedEvent.WORLD_CHANGED) {
							editor.fireSaveNeeded(input, true);
							if (!fIsSourceMode) {
								addTextEditOperation(fEditOperations, e);
							}
						} 
					}
				};
				((IModelChangeProvider) model).addModelChangedListener(modelListener);
			}

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
			flushModel(documentProvider.getDocument(input));
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
	
	protected abstract void addTextEditOperation(ArrayList ops, IModelChangedEvent event);
	
	protected void flushModel(IDocument doc) {
		if (fEditOperations.size() > 0) {
			try {
				MultiTextEdit edit = new MultiTextEdit();
				for (int i = 0; i < fEditOperations.size(); i++) {
					edit.addChild((TextEdit)fEditOperations.get(i));
				}
				edit.apply(doc);
				fEditOperations.clear();
			} catch (MalformedTreeException e) {
				e.printStackTrace();
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}
	
	public boolean mustSave() {
		if (!fIsSourceMode) {
			if (model instanceof IEditable) {
				if (((IEditable)model).isDirty())
					return true;
			}
		}
		return documentProvider.canSaveDocument(input);
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
		if (model!=null)
			model.dispose();
	}
	/**
	 * @return Returns the primary.
	 */
	public boolean isPrimary() {
		return primary;
	}
	/**
	 * @param primary The primary to set.
	 */
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
	
	public void setSourceEditingMode(boolean sourceMode) {
		fIsSourceMode = sourceMode;
		if (sourceMode) {
			// entered source editing mode; in this mode,
			// this context's document will be edited directly
			// in the source editor. All changes in the model
			// are caused by reconciliation and should not be 
			// fired to the world.
			flushModel(documentProvider.getDocument(input));
		}
		else {
			// leaving source editing mode; if the document
			// has been modified while in this mode,
			// fire the 'world changed' event from the model
			// to cause all the model listeners to become stale.
			boolean cleanSource = synchronizeModel(documentProvider.getDocument(input));
			if (!cleanSource) {
				// should go back to the source mode
			}
		}
	}
	
	public boolean isInSourceMode() {
		return fIsSourceMode;
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		return true;
	}
}
