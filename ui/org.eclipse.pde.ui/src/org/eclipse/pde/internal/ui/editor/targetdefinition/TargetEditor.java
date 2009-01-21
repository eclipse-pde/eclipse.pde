/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Editor for target definition (*.target) files.  Interacts with the ITargetDefinition model
 * to modify target attributes.  Uses the target platform service to persist the modified model
 * to the backing file.
 * 
 * @see ITargetDefinition
 * @see ITargetPlatformService
 */
public class TargetEditor extends FormEditor {

	private ITargetDefinition fTarget;

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#createToolkit(org.eclipse.swt.widgets.Display)
	 */
	protected FormToolkit createToolkit(Display display) {
		return new FormToolkit(PDEPlugin.getDefault().getFormColors(display));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	protected void addPages() {
		try {
			setActiveEditor(this);
			setPartName(getEditorInput().getName());
			addPage(new DefinitionPage(this));
			addPage(new EnvironmentPage(this));
		} catch (PartInitException e) {
			PDEPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		// TODO Better error handling
		commitPages(true);
		ITargetDefinition target = getTarget();
		if (target != null) {
			ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (service != null) {
				try {
					service.saveTargetDefinition(target);
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
			}
		}
		editorDirtyStateChanged();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		// TODO Auto-generated method stub
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Returns the target model backing this editor
	 * @return target model
	 */
	public ITargetDefinition getTarget() {
		// TODO Better error handling
		if (fTarget == null) {
			ITargetPlatformService service = (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
			if (service != null) {
				IEditorInput input = getEditorInput();
				if (input instanceof IFileEditorInput) {
					ITargetHandle fileHandle = service.getTarget(((IFileEditorInput) input).getFile());
					try {
						fTarget = fileHandle.getTargetDefinition();
					} catch (CoreException e) {
						PDEPlugin.log(e);
					}
				}
				// TODO Support storage editor input?
				if (fTarget == null) {
					fTarget = service.newTarget();
				}
			}
		}
		return fTarget;
	}

	/**
	 * Handles the revert action
	 */
	public void doRevert() {
		try {
			init(getEditorSite(), getEditorInput());
		} catch (PartInitException e) {
			PDEPlugin.log(e);
		}
	}

}
