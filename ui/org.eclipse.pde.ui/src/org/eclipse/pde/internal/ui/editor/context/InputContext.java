/*
 * Created on Jan 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.context;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.StorageDocumentProvider;
import org.eclipse.pde.internal.ui.editor.SystemFileDocumentProvider;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.model.IEditingModel;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MoveSourceEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
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
	private boolean mustSynchronize;

	class ElementListener implements IElementStateListener {
		public void elementContentAboutToBeReplaced(Object element) {
		}
		public void elementContentReplaced(Object element) {
			doRevert();
		}
		public void elementDeleted(Object element) {
		}
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
			mustSynchronize=true;
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
	protected IDocumentProvider createDocumentProvider(IEditorInput input) {
		IDocumentProvider documentProvider = null;
		if (input instanceof IFileEditorInput) {
			documentProvider = new FileDocumentProvider() {
				public IDocument createDocument(Object element) throws CoreException {
					IDocument document = super.createDocument(element);
					if (document != null) {
						IDocumentPartitioner partitioner = createDocumentPartitioner();
						if (partitioner != null) {
							partitioner.connect(document);
							document.setDocumentPartitioner(partitioner);
						}
					}
					return document;
				}
			};
		} else if (input instanceof SystemFileEditorInput) {
			return new SystemFileDocumentProvider(createDocumentPartitioner(), getDefaultCharset());
		} else if (input instanceof IStorageEditorInput) {
			documentProvider = new StorageDocumentProvider(createDocumentPartitioner(), getDefaultCharset());
		}
		return documentProvider;
	}
	
	protected IDocumentPartitioner createDocumentPartitioner() {
		return null;
	}
	
	protected abstract String getDefaultCharset();
	
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
							if (!editor.getLastDirtyState())
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
	
	public synchronized boolean validateEdit() {
		if (!validated) {
			if (input instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) input).getFile();
				if (file.isReadOnly()) {
					Shell shell = editor.getEditorSite().getShell();
					IStatus validateStatus = PDEPlugin.getWorkspace().validateEdit(
						new IFile[]{file}, shell);
					validated=true; // to prevent loops
					if (validateStatus.getSeverity() != IStatus.OK)
						ErrorDialog.openError(shell, editor.getTitle(), null,
							validateStatus);
					return validateStatus.getSeverity() == IStatus.OK;
				}
			}
		}
		return true;
	}
	public void doSave(IProgressMonitor monitor) {
		/*
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
		*/
		//Removed unnecessary usage of workspace modify operation
		// as per defect #62225
		try {
			IDocument doc = documentProvider.getDocument(input);
			documentProvider.aboutToChange(input);
			flushModel(doc);			
			documentProvider.saveDocument(monitor, input, doc, true);
			documentProvider.changed(input);
			validated=false;
		}
		catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	protected abstract void addTextEditOperation(ArrayList ops, IModelChangedEvent event);
	
	protected void flushModel(IDocument doc) {
		if (fEditOperations.size() > 0) {
			try {
				MultiTextEdit edit = new MultiTextEdit();
				for (int i = 0; i < fEditOperations.size(); i++) {
					insert(edit, (TextEdit)fEditOperations.get(i));
				}
				if (model instanceof IEditingModel)
					((IEditingModel)model).setStale(true);				
				edit.apply(doc);
				fEditOperations.clear();
				if (model instanceof IEditable)
					((IEditable)model).setDirty(false);
			} catch (MalformedTreeException e) {
				PDEPlugin.logException(e);
			} catch (BadLocationException e) {
				PDEPlugin.logException(e);
			}
		}	
	}
	
	protected static void insert(TextEdit parent, TextEdit edit) {
		if (!parent.hasChildren()) {
			parent.addChild(edit);
			if (edit instanceof MoveSourceEdit) {
				parent.addChild(((MoveSourceEdit)edit).getTargetEdit());
			}
			return;
		}
		TextEdit[] children= parent.getChildren();
		// First dive down to find the right parent.
		for (int i= 0; i < children.length; i++) {
			TextEdit child= children[i];
			if (covers(child, edit)) {
				insert(child, edit);
				return;
			}
		}
		// We have the right parent. Now check if some of the children have to
		// be moved under the new edit since it is covering it.
		for (int i= children.length - 1; i >= 0; i--) {
			TextEdit child= children[i];
			if (covers(edit, child)) {
				parent.removeChild(i);
				edit.addChild(child);
			}
		}
		parent.addChild(edit);
		if (edit instanceof MoveSourceEdit) {
			parent.addChild(((MoveSourceEdit)edit).getTargetEdit());
		}
	}
	
	protected static boolean covers(TextEdit thisEdit, TextEdit otherEdit) {
		if (thisEdit.getLength() == 0)	// an insertion point can't cover anything
			return false;
		
		int thisOffset= thisEdit.getOffset();
		int thisEnd= thisEdit.getExclusiveEnd();	
		if (otherEdit.getLength() == 0) {
			int otherOffset= otherEdit.getOffset();
			return thisOffset < otherOffset && otherOffset < thisEnd;
		} else {
			int otherOffset= otherEdit.getOffset();
			int otherEnd= otherEdit.getExclusiveEnd();
			return thisOffset <= otherOffset && otherEnd <= thisEnd;
		}
	}		

	public boolean mustSave() {
		if (!fIsSourceMode) {
			if (model instanceof IEditable) {
				if (((IEditable)model).isDirty()) {
					return true;
				}
			}
		}
		return documentProvider.canSaveDocument(input);
	}
	
	public void dispose() {
		IAnnotationModel amodel = documentProvider.getAnnotationModel(input);
		if (amodel != null)
			amodel.disconnect(documentProvider.getDocument(input));
		documentProvider.removeElementStateListener(elementListener);
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
	
	public boolean setSourceEditingMode(boolean sourceMode) {
		fIsSourceMode = sourceMode;
		if (sourceMode) {
			// entered source editing mode; in this mode,
			// this context's document will be edited directly
			// in the source editor. All changes in the model
			// are caused by reconciliation and should not be 
			// fired to the world.
			flushModel(documentProvider.getDocument(input));
			mustSynchronize=true;
			return true;
		}
		else {
			// leaving source editing mode; if the document
			// has been modified while in this mode,
			// fire the 'world changed' event from the model
			// to cause all the model listeners to become stale.
			return synchronizeModelIfNeeded();
		}
	}
	
	private boolean synchronizeModelIfNeeded() {
		if (mustSynchronize) {
			boolean result = synchronizeModel(documentProvider.getDocument(input));
			mustSynchronize=false;
			return result;
		}
		return true;
	}

	public void doRevert() {
		mustSynchronize=true;
		synchronizeModelIfNeeded();
		/*
		if (model instanceof IEditable) {
			((IEditable)model).setDirty(false);
		}
		*/
	}

	public boolean isInSourceMode() {
		return fIsSourceMode;
	}

	public boolean isModelCorrect() {
		synchronizeModelIfNeeded();
		return model!=null ? model.isValid() : false;
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		return true;
	}
	public boolean matches(IResource resource) {
		if (input instanceof IFileEditorInput) {
			IFileEditorInput finput = (IFileEditorInput)input;
			IFile file = finput.getFile();
			if (file.equals(resource))
				return true;
		}
		return false;
	}
	/**
	 * @return Returns the validated.
	 */
	public boolean isValidated() {
		return validated;
	}
	/**
	 * @param validated The validated to set.
	 */
	public void setValidated(boolean validated) {
		this.validated = validated;
	}
}