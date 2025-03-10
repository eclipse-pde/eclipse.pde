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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.IModelUndoManager;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

public abstract class InputContextManager implements IResourceChangeListener {
	private final PDEFormEditor editor;
	private final Map<IEditorInput, InputContext> inputContexts = new HashMap<>();
	private final Set<IFile> monitoredFiles = ConcurrentHashMap.newKeySet();
	private final ArrayList<IInputContextListener> listeners = new ArrayList<>();
	private IModelUndoManager undoManager;

	public InputContextManager(PDEFormEditor editor) {
		this.editor = editor;
		PDEPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	public void addInputContextListener(IInputContextListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeInputContextListener(IInputContextListener listener) {
		listeners.remove(listener);
	}

	public void dispose() {
		PDEPlugin.getWorkspace().removeResourceChangeListener(this);
		// dispose input contexts
		for (InputContext context : inputContexts.values()) {
			unhookUndo(context);
			context.dispose();
		}
		inputContexts.clear();
		undoManager = null;
		monitoredFiles.clear();
	}

	/**
	 * Saves dirty contexts.
	 */
	public void save(IProgressMonitor monitor) {
		Collection<InputContext> values = inputContexts.values();
		SubMonitor subMon = SubMonitor.convert(monitor, values.size());
		for (InputContext context : values) {
			if (context.mustSave()) {
				context.doSave(subMon.newChild(1));
			}
		}
	}

	public IProject getCommonProject() {
		for (InputContext context : inputContexts.values()) {
			IEditorInput input = context.getInput();
			if (input instanceof IFileEditorInput) {
				return ((IFileEditorInput) input).getFile().getProject();
			}
		}
		return null;
	}

	public boolean hasContext(String id) {
		return findContext(id) != null;
	}

	public InputContext findContext(String id) {
		for (InputContext context : inputContexts.values()) {
			if (context.getId().equals(id)) {
				return context;
			}
		}
		return null;
	}

	public InputContext findContext(IResource resource) {
		for (InputContext context : inputContexts.values()) {
			if (context.matches(resource)) {
				return context;
			}
		}
		return null;
	}

	public abstract IBaseModel getAggregateModel();

	public InputContext getContext(IEditorInput input) {
		return inputContexts.get(input);
	}

	public void putContext(IEditorInput input, InputContext context) {
		inputContexts.put(input, context);
		fireContextChange(context, true);
	}

	/**
	 * Update the key (the editor input in this case) associated with the
	 * input context without firing a context change event.
	 * Used for save as operations.
	 */
	private void updateInputContext(IEditorInput newInput, IEditorInput oldInput) throws Exception {
		InputContext value = null;
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
		for (InputContext context : inputContexts.values()) {
			if (context.isPrimary()) {
				return context;
			}
		}
		return null;
	}

	public InputContext[] getInvalidContexts() {
		ArrayList<InputContext> result = new ArrayList<>();
		for (InputContext context : inputContexts.values()) {
			if (context.isModelCorrect() == false) {
				result.add(context);
			}
		}
		return result.toArray(new InputContext[result.size()]);
	}

	public boolean isDirty() {
		for (InputContext context : inputContexts.values()) {
			if (context.mustSave()) {
				return true;
			}
		}
		return false;
	}

	public void monitorFile(IFile file) {
		if (file == null) {
			return;
		}
		monitoredFiles.add(file);
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (monitoredFiles.isEmpty()) {
			return;
		}

		IResourceDelta delta = event.getDelta();

		try {
			List<IFile> added = new ArrayList<>();
			List<IFile> removed = new ArrayList<>();
			delta.accept(delta1 -> {
				int kind = delta1.getKind();
				if (kind != IResourceDelta.ADDED && kind != IResourceDelta.REMOVED) {
					return true;
				}
				IResource resource = delta1.getResource();
				if (monitoredFiles.contains(resource)) {
					if (kind == IResourceDelta.ADDED) {
						added.add((IFile) resource);
					} else {
						removed.add((IFile) resource);
					}
					return false;
				}
				return true;
			});
			if (!added.isEmpty() || !removed.isEmpty()) {
				asyncStructureChanged(added, removed);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void asyncStructureChanged(List<IFile> added, List<IFile> removed) {
		try {
			IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
			progressService.runInUI(progressService, monitor -> {
				for (IFile file : added) {
					if (!monitor.isCanceled()) {
						structureChanged(file, true);
					}
				}
				for (IFile file : removed) {
					if (!monitor.isCanceled()) {
						structureChanged(file, false);
					}
				}
			}, null);
		} catch (InvocationTargetException | InterruptedException e) {
			PDEPlugin.logException(e);
		}
	}

	protected void structureChanged(IFile file, boolean added) {
		if (monitoredFiles.contains(file)) {
			if (added) {
				fireStructureChange(file, true);
			} else {
				fireStructureChange(file, false);
				removeContext(file);
			}
		}
	}

	private void removeContext(IFile file) {
		for (InputContext context : inputContexts.values()) {
			IEditorInput input = context.getInput();
			if (input instanceof IFileEditorInput fileInput) {
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
			if (added) {
				listener.monitoredFileAdded(file);
			} else {
				listener.monitoredFileRemoved(file);
			}
		}
	}

	protected void fireContextChange(InputContext context, boolean added) {
		for (int i = 0; i < listeners.size(); i++) {
			IInputContextListener listener = listeners.get(i);
			if (added) {
				listener.contextAdded(context);
			} else {
				listener.contextRemoved(context);
			}
		}
		if (added) {
			hookUndo(context);
		} else {
			unhookUndo(context);
		}
	}

	public void undo() {
		if (undoManager != null && undoManager.isUndoable()) {
			undoManager.undo();
		}
	}

	public void redo() {
		if (undoManager != null && undoManager.isRedoable()) {
			undoManager.redo();
		}
	}

	private void hookUndo(InputContext context) {
		if (undoManager == null) {
			return;
		}
		IBaseModel model = context.getModel();
		if (model instanceof IModelChangeProvider) {
			undoManager.connect((IModelChangeProvider) model);
		}
	}

	private void unhookUndo(InputContext context) {
		if (undoManager == null) {
			return;
		}
		IBaseModel model = context.getModel();
		if (model instanceof IModelChangeProvider) {
			undoManager.disconnect((IModelChangeProvider) model);
		}
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
