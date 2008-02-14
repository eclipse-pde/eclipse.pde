/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.toc;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.text.toc.TocModel;
import org.eclipse.pde.internal.core.text.toc.TocObject;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.wizards.toc.RegisterTocWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.part.*;

public class TocEditor extends MultiSourceEditor {

	private ImageHyperlink fImageHyperlinkRegisterTOC;

	public TocEditor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IPDEUIConstants.TABLE_OF_CONTENTS_EDITOR_ID;
	}

	public Object getAdapter(Class adapter) {
		if (inUiThread() && isShowInApplicable()) {
			if (adapter == IShowInSource.class) {
				return getShowInSource();
			} else if (adapter == IShowInTargetList.class) {
				return getShowInTargetList();
			}
		}

		return super.getAdapter(adapter);
	}

	private boolean inUiThread() {
		// get our workbench display
		Display display = getSite().getWorkbenchWindow().getWorkbench().getDisplay();

		// return true if we're in the UI thread
		if (display != null && !display.isDisposed()) {
			return display.getThread() == Thread.currentThread();
		}
		return false;
	}

	private boolean isShowInApplicable() {
		if (getSelection().isEmpty()) {
			return false;
		}

		if (getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) getSelection();
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (!(obj instanceof TocObject))
					return false;
				if (((TocObject) obj).getPath() == null)
					return false;
			}

			return true;
		}

		return false;
	}

	/**
	 * Returns the <code>IShowInSource</code> for this section.
	 * @return the <code>IShowInSource</code> 
	 */
	private IShowInSource getShowInSource() {
		return new IShowInSource() {
			public ShowInContext getShowInContext() {
				ArrayList resourceList = new ArrayList();
				IStructuredSelection selection = (IStructuredSelection) getSelection();
				IStructuredSelection resources;
				if (selection.isEmpty()) {
					resources = null;
				} else {
					IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();

					for (Iterator iter = selection.iterator(); iter.hasNext();) {
						Object obj = iter.next();
						if (obj instanceof TocObject && ((TocObject) obj).getPath() != null) {
							Path resourcePath = new Path(((TocObject) obj).getPath());

							if (!resourcePath.isEmpty()) {
								TocModel model = (TocModel) getAggregateModel();

								IPath pluginPath = model.getUnderlyingResource().getProject().getFullPath();
								IResource resource = root.findMember(pluginPath.append(resourcePath));

								if (resource != null) {
									resourceList.add(resource);
								}
							}
						}
					}

					resources = new StructuredSelection(resourceList);
				}

				return new ShowInContext(null, resources);
			}
		};
	}

	/**
	 * Returns the <code>IShowInTargetList</code> for this section.
	 * @return the <code>IShowInTargetList</code> 
	 */
	private IShowInTargetList getShowInTargetList() {
		return new IShowInTargetList() {
			public String[] getShowInTargetIds() {
				return new String[] {JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV};
			}
		};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getContextIDForSaveAs()
	 */
	public String getContextIDForSaveAs() {
		return TocInputContext.CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#addEditorPages()
	 */
	protected void addEditorPages() {
		try {
			addPage(new TocPage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		// Add source page
		addSourcePage(TocInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createContentOutline()
	 */
	protected ISortableContentOutlinePage createContentOutline() {
		return new TocFormOutlinePage(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createInputContextManager()
	 */
	protected InputContextManager createInputContextManager() {
		return new TocInputContextManager(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createResourceContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IFileEditorInput)
	 */
	protected void createResourceContexts(InputContextManager contexts, IFileEditorInput input) {
		contexts.putContext(input, new TocInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createStorageContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IStorageEditorInput)
	 */
	protected void createStorageContexts(InputContextManager contexts, IStorageEditorInput input) {
		contexts.putContext(input, new TocInputContext(this, input, true));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createSystemFileContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.pde.internal.ui.editor.SystemFileEditorInput)
	 */
	protected void createSystemFileContexts(InputContextManager contexts, SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		if (file != null) {
			IEditorInput in = new SystemFileEditorInput(file);
			contexts.putContext(in, new TocInputContext(this, in, true));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#editorContextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void editorContextAdded(InputContext context) {
		// Add the source page
		addSourcePage(context.getId());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(TocInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextRemoved(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void contextRemoved(InputContext context) {
		close(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileAdded(org.eclipse.core.resources.IFile)
	 */
	public void monitoredFileAdded(IFile monitoredFile) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getSelection()
	 */
	public ISelection getSelection() {
		IFormPage formPage = getActivePageInstance();
		if ((formPage != null) && (formPage instanceof TocPage)) {
			// Synchronizes the selection made in the master tree view with the
			// selection in the outline view when the link with editor button
			// is toggled on
			return ((TocPage) formPage).getSelection();
		}

		return super.getSelection();
	}

	public boolean canCut(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			for (Iterator iter = sel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof TocObject && ((TocObject) obj).canBeRemoved()) {
					return canCopy(selection);
				}
			}
		}

		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.MultiSourceEditor#createSourcePage(org.eclipse.pde.internal.ui.editor.PDEFormEditor, java.lang.String, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createSourcePage(PDEFormEditor editor, String title, String name, String contextId) {
		return new TocSourcePage(editor, title, name);
	}

	public void contributeToToolbar(IToolBarManager manager) {
		// Add the register cheat sheet link to the form title area
		if (getAggregateModel().isEditable())
			manager.add(createUIControlConRegisterCS());
	}

	/**
	 * @return
	 */
	private ControlContribution createUIControlConRegisterCS() {
		return new ControlContribution("Register") { //$NON-NLS-1$
			protected Control createControl(Composite parent) {
				// Create UI
				createUIImageHyperlinkRegisterToc(parent);
				// Create Listener
				createUIListenerImageHyperlinkRegisterToc();
				return fImageHyperlinkRegisterTOC;
			}
		};
	}

	/**
	 * @param parent
	 */
	private void createUIImageHyperlinkRegisterToc(Composite parent) {
		fImageHyperlinkRegisterTOC = new ImageHyperlink(parent, SWT.NONE);
		fImageHyperlinkRegisterTOC.setText(PDEUIMessages.TocPage_msgRegisterThisTOC);
		fImageHyperlinkRegisterTOC.setUnderlined(true);
		fImageHyperlinkRegisterTOC.setForeground(getToolkit().getHyperlinkGroup().getForeground());
	}

	/**
	 * 
	 */
	private void createUIListenerImageHyperlinkRegisterToc() {
		fImageHyperlinkRegisterTOC.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				handleLinkActivatedRegisterTOC();
			}

			public void linkEntered(HyperlinkEvent e) {
				handleLinkEnteredRegisterTOC(e.getLabel());
			}

			public void linkExited(HyperlinkEvent e) {
				handleLinkExitedRegisterTOC();
			}
		});
	}

	/**
	 * @param message
	 */
	private void handleLinkEnteredRegisterTOC(String message) {
		// Update colour
		fImageHyperlinkRegisterTOC.setForeground(getToolkit().getHyperlinkGroup().getActiveForeground());
		// Update IDE status line
		getEditorSite().getActionBars().getStatusLineManager().setMessage(message);
	}

	/**
	 *
	 */
	private void handleLinkExitedRegisterTOC() {
		// Update colour
		fImageHyperlinkRegisterTOC.setForeground(getToolkit().getHyperlinkGroup().getForeground());
		// Update IDE status line
		getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}

	/**
	 * 
	 */
	private void handleLinkActivatedRegisterTOC() {
		RegisterTocWizard wizard = new RegisterTocWizard((IModel) getAggregateModel());
		// Initialize the wizard
		wizard.init(PlatformUI.getWorkbench(), null);
		// Create the dialog for the wizard
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		dialog.getShell().setSize(400, 250);
		// Check the result
		if (dialog.open() == Window.OK) {
			// NO-OP
		}
	}
}
