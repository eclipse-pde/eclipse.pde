/*******************************************************************************
 *  Copyright (c) 2003, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.context;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.IModelUndoManager;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

public abstract class InputContextManager implements IResourceChangeListener {
	private PDEFormEditor editor;
	private Hashtable<IEditorInput, Object> inputContexts;
	private ArrayList<IFile> monitoredFiles;
	private ArrayList<IInputContextListener> listeners;
	private IModelUndoManager undoManager;

	/**
	 *
	 */
	public InputContextManager(PDEFormEditor editor) {
		this.editor = editor;
		inputContexts = new Hashtable<>();
		listeners = new ArrayList<>();
		PDEPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public void addInputContextListener(IInputContextListener listener) {
		if (!listeners.contains(listener))
			listeners.add(listener);
	}

	public void removeInputContextListener(IInputContextListener listener) {
		listeners.remove(listener);
	}

	/**
	 *
	 *
	 */
	public void dispose() {
		PDEPlugin.getWorkspace().removeResourceChangeListener(this);
		// dispose input contexts
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			unhookUndo(context);
			context.dispose();
		}
		inputContexts.clear();
		undoManager = null;
	}

	/**
	 * Saves dirty contexts.
	 * @param monitor
	 */
	public void save(IProgressMonitor monitor) {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			if (context.mustSave())
				context.doSave(monitor);
		}
	}

	public IProject getCommonProject() {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			IEditorInput input = context.getInput();
			if (input instanceof IFileEditorInput)
				return ((IFileEditorInput) input).getFile().getProject();
		}
		return null;
	}

	public boolean hasContext(String id) {
		return findContext(id) != null;
	}

	public InputContext findContext(String id) {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			if (context.getId().equals(id))
				return context;
		}
		return null;
	}

	public InputContext findContext(IResource resource) {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			if (context.matches(resource))
				return context;
		}
		return null;
	}

	public abstract IBaseModel getAggregateModel();

	public InputContext getContext(IEditorInput input) {
		return (InputContext) inputContexts.get(input);
	}

	public void putContext(IEditorInput input, InputContext context) {
		inputContexts.put(input, context);
		fireContextChange(context, true);
	}

	/**
	 * Update the key (the editor input in this case) associated with the
	 * input context without firing a context change event.
	 * Used for save as operations.
	 * @param newInput
	 * @param oldInput
	 * @throws Exception
	 */
	private void updateInputContext(IEditorInput newInput, IEditorInput oldInput) throws Exception {
		Object value = null;
		// Retrieve the input context referenced by the old editor input and
		// remove it from the context manager
		if (inputContexts.containsKey(oldInput)) {
			value = inputContexts.remove(oldInput);
		} else {
			throw new Exception(PDEUIMessages.InputContextManager_errorMessageInputContextNotFound);
		}
		// Re-insert the input context back into the context manager using the
		// new editor input as its key
		inputContexts.put(newInput, value);
	}

	/**
	 * @param monitor
	 * @param contextID
	 * @throws Exception
	 */
	public void saveAs(IProgressMonitor monitor, String contextID) throws Exception {
		// Find the existing context
		InputContext inputContext = findContext(contextID);
		if (inputContext != null) {
			// Keep the old editor input
			IEditorInput oldInput = editor.getEditorInput();
			// Perform the save as operation
			inputContext.doSaveAs(monitor);
			// Get the new editor input
			IEditorInput newInput = inputContext.getInput();
			// Update the context manager accordingly
			updateInputContext(newInput, oldInput);
		} else {
			throw new Exception(PDEUIMessages.InputContextManager_errorMessageInputContextNotFound);
		}
	}

	public InputContext getPrimaryContext() {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			if (context.isPrimary())
				return context;
		}
		return null;
	}

	public InputContext[] getInvalidContexts() {
		ArrayList<InputContext> result = new ArrayList<>();
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			if (context.isModelCorrect() == false)
				result.add(context);
		}
		return result.toArray(new InputContext[result.size()]);
	}

	public boolean isDirty() {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			if (context.mustSave())
				return true;
		}
		return false;
	}

	public void monitorFile(IFile file) {
		if (monitoredFiles == null)
			monitoredFiles = new ArrayList<>();
		monitoredFiles.add(file);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();

		try {
			delta.accept(delta1 -> {
				int kind = delta1.getKind();
				IResource resource = delta1.getResource();
				if (resource instanceof IFile) {
					if (kind == IResourceDelta.ADDED)
						asyncStructureChanged((IFile) resource, true);
					else if (kind == IResourceDelta.REMOVED)
						asyncStructureChanged((IFile) resource, false);
					return false;
				}
				return true;
			});
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void asyncStructureChanged(final IFile file, final boolean added) {
		if (editor == null || editor.getEditorSite() == null)
			return;
		Shell shell = editor.getEditorSite().getShell();
		Display display = shell != null ? shell.getDisplay() : Display.getDefault();

		display.asyncExec(() -> structureChanged(file, added));
	}

	protected void structureChanged(IFile file, boolean added) {
		if (monitoredFiles == null)
			return;
		for (int i = 0; i < monitoredFiles.size(); i++) {
			IFile ifile = monitoredFiles.get(i);
			if (ifile.equals(file)) {
				if (added) {
					fireStructureChange(file, true);
				} else {
					fireStructureChange(file, false);
					removeContext(file);
				}
			}
		}
	}

	private void removeContext(IFile file) {
		for (Enumeration<Object> contexts = inputContexts.elements(); contexts.hasMoreElements();) {
			InputContext context = (InputContext) contexts.nextElement();
			IEditorInput input = context.getInput();
			if (input instanceof IFileEditorInput) {
				IFileEditorInput fileInput = (IFileEditorInput) input;
				if (file.equals(fileInput.getFile())) {
					inputContexts.remove(input);
					fireContextChange(context, false);
					return;
				}
			}
		}
	}

	protected void fireStructureChange(IFile file, boolean added) {
		for (int i = 0; i < listeners.size(); i++) {
			IInputContextListener listener = listeners.get(i);
			if (added)
				listener.monitoredFileAdded(file);
			else
				listener.monitoredFileRemoved(file);
		}
	}

	protected void fireContextChange(InputContext context, boolean added) {
		for (int i = 0; i < listeners.size(); i++) {
			IInputContextListener listener = listeners.get(i);
			if (added)
				listener.contextAdded(context);
			else
				listener.contextRemoved(context);
		}
		if (added)
			hookUndo(context);
		else
			unhookUndo(context);
	}

	public void undo() {
		if (undoManager != null && undoManager.isUndoable())
			undoManager.undo();
	}

	public void redo() {
		if (undoManager != null && undoManager.isRedoable())
			undoManager.redo();
	}

	private void hookUndo(InputContext context) {
		if (undoManager == null)
			return;
		IBaseModel model = context.getModel();
		if (model instanceof IModelChangeProvider)
			undoManager.connect((IModelChangeProvider) model);
	}

	private void unhookUndo(InputContext context) {
		if (undoManager == null)
			return;
		IBaseModel model = context.getModel();
		if (model instanceof IModelChangeProvider)
			undoManager.disconnect((IModelChangeProvider) model);
	}

	/**
	 * @return Returns the undoManager.
	 */
	public IModelUndoManager getUndoManager() {
		return undoManager;
	}

	/**
	 * @param undoManager The undoManager to set.
	 */
	public void setUndoManager(IModelUndoManager undoManager) {
		this.undoManager = undoManager;
	}
}
