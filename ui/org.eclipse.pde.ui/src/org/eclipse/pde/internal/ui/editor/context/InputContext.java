/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.context;
import java.util.ArrayList;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextUtilities;
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
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IElementStateListener;
/**
 * This class maintains objects associated with a single editor input.
 */
public abstract class InputContext {
	
	private PDEFormEditor fEditor;
	private IEditorInput fEditorInput;
	private IBaseModel fModel;
	private IModelChangedListener fModelListener;
	private IDocumentProvider fDocumentProvider;
	private IElementStateListener fElementListener;
	protected ArrayList fEditOperations = new ArrayList();
	
	private boolean fValidated;
	private boolean fPrimary;
	private boolean fIsSourceMode;
	private boolean fMustSynchronize;

	class ElementListener implements IElementStateListener {
		public void elementContentAboutToBeReplaced(Object element) {
		}
		public void elementContentReplaced(Object element) {
			doRevert();
		}
		public void elementDeleted(Object element) {
            dispose();
		}
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
			fMustSynchronize=true;
		}
		public void elementMoved(Object originalElement, Object movedElement) {
            dispose();
			fEditor.close(true);
		}
	}
	public InputContext(PDEFormEditor editor, IEditorInput input, boolean primary) {
		this.fEditor = editor;
		this.fEditorInput = input;
		setPrimary(primary);
	}
	public abstract String getId();

	public IEditorInput getInput() {
		return fEditorInput;
	}
	public PDEFormEditor getEditor() {
		return fEditor;
	}
	public IBaseModel getModel() {
		return fModel;
	}
	public IDocumentProvider getDocumentProvider() {
		return fDocumentProvider;
	}
	private IDocumentProvider createDocumentProvider(IEditorInput input) {
		if (input instanceof IFileEditorInput) {
			return createFileDocumentProvider();
		} else if (input instanceof SystemFileEditorInput) {
			return new SystemFileDocumentProvider(createDocumentPartitioner(), getDefaultCharset());
		} else if (input instanceof IStorageEditorInput) {
			return new StorageDocumentProvider(createDocumentPartitioner(), getDefaultCharset());
		}
		return null;
	}
	
	private IDocumentProvider createFileDocumentProvider() {
		return new ForwardingDocumentProvider(getPartitionName(),
				getDocumentSetupParticipant(), new TextFileDocumentProvider());
	}
	
	protected IDocumentSetupParticipant getDocumentSetupParticipant() {
		return new IDocumentSetupParticipant() {
			public void setup(IDocument document) {
			}			
		};
	}
	
	protected abstract String getPartitionName();
		
	protected IDocumentPartitioner createDocumentPartitioner() {
		return null;
	}
	
	protected abstract String getDefaultCharset();
	
	protected abstract IBaseModel createModel(IEditorInput input) throws CoreException;
	
	protected void create() {
		fDocumentProvider = createDocumentProvider(fEditorInput);
		if (fDocumentProvider == null)
			return;
		try {
			fDocumentProvider.connect(fEditorInput);
			fModel = createModel(fEditorInput);
			if (fModel instanceof IModelChangeProvider) {
				fModelListener = new IModelChangedListener() {
					public void modelChanged(IModelChangedEvent e) {
						if (e.getChangeType() != IModelChangedEvent.WORLD_CHANGED) {
							if (!fEditor.getLastDirtyState())
								fEditor.fireSaveNeeded(fEditorInput, true);
							if (!fIsSourceMode) {
								IModelChangeProvider provider = e.getChangeProvider();
								if (provider instanceof IEditingModel) {
									// this is to guard against false notifications
									// when a revert operation is performed, focus is taken away from a FormEntry
									// and a text edit operation is falsely requested
									if (((IEditingModel)provider).isDirty())
										addTextEditOperation(fEditOperations, e);
								}
							}
						} 
					}
				};
				((IModelChangeProvider) fModel).addModelChangedListener(fModelListener);
			}

			IAnnotationModel amodel = fDocumentProvider
					.getAnnotationModel(fEditorInput);
			if (amodel != null)
				amodel.connect(fDocumentProvider.getDocument(fEditorInput));
			fElementListener = new ElementListener();
			fDocumentProvider.addElementStateListener(fElementListener);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		
	}
	
	public synchronized boolean validateEdit() {
		if (!fValidated) {
			if (fEditorInput instanceof IFileEditorInput) {
				IFile file = ((IFileEditorInput) fEditorInput).getFile();
				if (file.isReadOnly()) {
					Shell shell = fEditor.getEditorSite().getShell();
					IStatus validateStatus = PDEPlugin.getWorkspace().validateEdit(
						new IFile[]{file}, shell);
					fValidated=true; // to prevent loops
					if (validateStatus.getSeverity() != IStatus.OK)
						ErrorDialog.openError(shell, fEditor.getTitle(), null,
							validateStatus);
					return validateStatus.getSeverity() == IStatus.OK;
				}
			}
		}
		return true;
	}
	public void doSave(IProgressMonitor monitor) {
		try {
			IDocument doc = fDocumentProvider.getDocument(fEditorInput);
			fDocumentProvider.aboutToChange(fEditorInput);
			flushModel(doc);			
			fDocumentProvider.saveDocument(monitor, fEditorInput, doc, true);
			fDocumentProvider.changed(fEditorInput);
			fValidated=false;
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
				if (fModel instanceof IEditingModel)
					((IEditingModel)fModel).setStale(true);				
				edit.apply(doc);
				fEditOperations.clear();
				if (fModel instanceof IEditable)
					((IEditable)fModel).setDirty(false);
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
		}
		int otherOffset= otherEdit.getOffset();
		int otherEnd= otherEdit.getExclusiveEnd();
		return thisOffset <= otherOffset && otherEnd <= thisEnd;
	}		

	public boolean mustSave() {
		if (!fIsSourceMode) {
			if (fModel instanceof IEditable) {
				if (((IEditable)fModel).isDirty()) {
					return true;
				}
			}
		}
		return fDocumentProvider.canSaveDocument(fEditorInput);
	}
	
	public void dispose() {
		IAnnotationModel amodel = fDocumentProvider.getAnnotationModel(fEditorInput);
		if (amodel != null)
			amodel.disconnect(fDocumentProvider.getDocument(fEditorInput));
		fDocumentProvider.removeElementStateListener(fElementListener);
		fDocumentProvider.disconnect(fEditorInput);
		if (fModelListener != null && fModel instanceof IModelChangeProvider) {
			((IModelChangeProvider) fModel)
					.removeModelChangedListener(fModelListener);
			//if (undoManager != null)
			//undoManager.disconnect((IModelChangeProvider) model);
		}
		if (fModel!=null)
			fModel.dispose();
	}
	/**
	 * @return Returns the primary.
	 */
	public boolean isPrimary() {
		return fPrimary;
	}
	/**
	 * @param primary The primary to set.
	 */
	public void setPrimary(boolean primary) {
		this.fPrimary = primary;
	}
	
	public boolean setSourceEditingMode(boolean sourceMode) {
		fIsSourceMode = sourceMode;
		if (sourceMode) {
			// entered source editing mode; in this mode,
			// this context's document will be edited directly
			// in the source editor. All changes in the model
			// are caused by reconciliation and should not be 
			// fired to the world.
			flushModel(fDocumentProvider.getDocument(fEditorInput));
			fMustSynchronize=true;
			return true;
		}
		// leaving source editing mode; if the document
		// has been modified while in this mode,
		// fire the 'world changed' event from the model
		// to cause all the model listeners to become stale.
		return synchronizeModelIfNeeded();
	}
	
	private boolean synchronizeModelIfNeeded() {
		if (fMustSynchronize) {
			boolean result = synchronizeModel(fDocumentProvider.getDocument(fEditorInput));
			fMustSynchronize=false;
			return result;
		}
		return true;
	}

	public void doRevert() {
		fMustSynchronize=true;
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
		return fModel!=null ? fModel.isValid() : false;
	}
	
	protected boolean synchronizeModel(IDocument doc) {
		return true;
	}
	public boolean matches(IResource resource) {
		if (fEditorInput instanceof IFileEditorInput) {
			IFileEditorInput finput = (IFileEditorInput)fEditorInput;
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
		return fValidated;
	}
	/**
	 * @param validated The validated to set.
	 */
	public void setValidated(boolean validated) {
		this.fValidated = validated;
	}
	
	public String getLineDelimiter() {
		if (fDocumentProvider != null) {
			IDocument document = fDocumentProvider.getDocument(fEditorInput);
			if (document != null) {
				return TextUtilities.getDefaultLineDelimiter(document);
			}
		}
		return System.getProperty("line.separator"); //$NON-NLS-1$
	}
}
