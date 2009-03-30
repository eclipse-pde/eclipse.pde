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

import java.util.*;
import java.util.List;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.impl.WorkspaceFileTargetHandle;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.part.FileEditorInput;

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
	private List fManagedFormPages = new ArrayList(2);
	private FileInputListener fInputListener;

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
		ITargetDefinition target = getTarget();
		WorkspaceFileTargetHandle currentTargetHandle = (WorkspaceFileTargetHandle) target.getHandle();

		SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
		dialog.create();
		dialog.setMessage(PDEUIMessages.TargetEditor_0, IMessageProvider.NONE);
		dialog.setOriginalFile(currentTargetHandle.getTargetFile());
		dialog.open();

		IPath path = dialog.getResult();

		if (path == null) {
			return;
		}
		if (!"target".equalsIgnoreCase(path.getFileExtension())) { //$NON-NLS-1$
			path.addFileExtension("target"); //$NON-NLS-1$
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IFile file = workspace.getRoot().getFile(path);

		if (target != null) {
			try {
				WorkspaceFileTargetHandle newFleTarget = new WorkspaceFileTargetHandle(file);
				newFleTarget.save(target);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}

		PDEPlugin.getWorkspace().removeResourceChangeListener(fInputListener);
		fInputListener = new FileInputListener(file);
		PDEPlugin.getWorkspace().addResourceChangeListener(fInputListener);
		setInput(new FileEditorInput(file));
		setPartName(getEditorInput().getName());
		commitPages(true);
		editorDirtyStateChanged();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.init(site, input);
		if (input instanceof IFileEditorInput) {
			fInputListener = new FileInputListener(((IFileEditorInput) input).getFile());
			PDEPlugin.getWorkspace().addResourceChangeListener(fInputListener);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#dispose()
	 */
	public void dispose() {
		if (fInputListener != null) {
			PDEPlugin.getWorkspace().removeResourceChangeListener(fInputListener);
			fInputListener = null;
		}
		super.dispose();
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
				} /*else if (input instanceof IURIEditorInput){
									IFileStore store = EFS.getStore(((IURIEditorInput) input).getURI());
									store.
									is = store.openInputStream(EFS.CACHE, new NullProgressMonitor());
									model = createStorageModel(is);
								}
								ResourcesPlugin.getPlugin().getWorkspace().getRoot().findFilesForLocationURI(URI)
								
								ITargetHandle fileHandle = service.getTarget(file)
							}*/
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
		fTarget = null;
		for (Iterator iterator = fManagedFormPages.iterator(); iterator.hasNext();) {
			IFormPart[] parts = ((IManagedForm) iterator.next()).getParts();
			for (int i = 0; i < parts.length; i++) {
				if (parts[i] instanceof AbstractFormPart) {
					((AbstractFormPart) parts[i]).markStale();
				}
			}
		}
		setActivePage(getActivePage());
		editorDirtyStateChanged();
	}

	public void contributeToToolbar(final ScrolledForm form, String helpURL) {
		ControlContribution setAsTarget = new ControlContribution("Set") { //$NON-NLS-1$
			protected Control createControl(Composite parent) {
				final ImageHyperlink hyperlink = new ImageHyperlink(parent, SWT.NONE);
				hyperlink.setText(PDEUIMessages.AbstractTargetPage_setTarget);
				hyperlink.setUnderlined(true);
				hyperlink.setForeground(getToolkit().getHyperlinkGroup().getForeground());
				hyperlink.addHyperlinkListener(new IHyperlinkListener() {
					public void linkActivated(HyperlinkEvent e) {
						LoadTargetDefinitionJob.load(getTarget());
					}

					public void linkEntered(HyperlinkEvent e) {
						hyperlink.setForeground(getToolkit().getHyperlinkGroup().getActiveForeground());
					}

					public void linkExited(HyperlinkEvent e) {
						hyperlink.setForeground(getToolkit().getHyperlinkGroup().getForeground());
					}
				});
				return hyperlink;
			}
		};
		final String href = ""; //$NON-NLS-1$
		Action help = new Action("help") { //$NON-NLS-1$
			public void run() {
				BusyIndicator.showWhile(form.getForm().getDisplay(), new Runnable() {
					public void run() {
						PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(href);
					}
				});
			}
		};
		help.setToolTipText(PDEUIMessages.PDEFormPage_help);
		help.setImageDescriptor(PDEPluginImages.DESC_HELP);
		form.getToolBarManager().add(setAsTarget);
		form.getToolBarManager().add(help);
		form.updateToolBar();
	}

	/**
	 * Adds the given form to the list of forms to be refreshed when reverting
	 * @param managedForm
	 */
	public void addForm(IManagedForm managedForm) {
		fManagedFormPages.add(managedForm);
	}

	/**
	 * Resource change listener for the file input to this editor in case it is deleted. 
	 */
	class FileInputListener implements IResourceChangeListener, IResourceDeltaVisitor {
		IFile fInput;

		public FileInputListener(IFile input) {
			fInput = input;
		}

		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				IResourceDelta delta = event.getDelta();
				try {
					delta.accept(this);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}

		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				if (file.equals(fInput)) {
					if (delta.getKind() == IResourceDelta.REMOVED || delta.getKind() == IResourceDelta.REPLACED) {
						Display display = getSite().getShell().getDisplay();
						display.asyncExec(new Runnable() {
							public void run() {
								getSite().getPage().closeEditor(TargetEditor.this, false);
							}
						});
					}
					return false;
				}
			}
			return true;
		}
	}

}
