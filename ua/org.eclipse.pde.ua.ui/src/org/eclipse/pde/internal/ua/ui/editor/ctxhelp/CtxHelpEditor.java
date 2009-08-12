/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.ctxhelp;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpMarkerManager;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpModel;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpObject;
import org.eclipse.pde.internal.ua.core.ctxhelp.text.CtxHelpTopic;
import org.eclipse.pde.internal.ua.ui.IConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.wizards.ctxhelp.RegisterCtxHelpWizard;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ShowInContext;

/**
 * Main UI class for the context help editor.  This editor provides a convenient way to
 * explore and edit the xml files containing context help entries.  The editor will
 * have two pages, one displaying the xml source, the other displaying a form editor.
 * @since 3.4
 * @see CtxHelpSourcePage
 * @see CtxHelpPage
 */
public class CtxHelpEditor extends MultiSourceEditor {

	public CtxHelpEditor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IConstants.CONTEXT_HELP_EDITOR_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == IShowInSource.class) {
			if (inUiThread() && isShowInApplicable()) {
				return getShowInSource();
			}
		}
		if (adapter == IShowInTargetList.class) {
			return getShowInTargetList();
		}
		return super.getAdapter(adapter);
	}

	/**
	 * @return return whether the current thread is the UI thread
	 */
	private boolean inUiThread() {
		Display display = getSite().getWorkbenchWindow().getWorkbench().getDisplay();
		if (display != null && !display.isDisposed()) {
			return display.getThread() == Thread.currentThread();
		}
		return false;
	}

	/**
	 * @return whether there is a selection that requires the "Show In" menu to be available
	 */
	private boolean isShowInApplicable() {
		if (getSelection().isEmpty()) {
			return false;
		}
		if (getSelection() instanceof IStructuredSelection) {
			IStructuredSelection selection = (IStructuredSelection) getSelection();
			for (Iterator iter = selection.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof CtxHelpTopic && ((CtxHelpTopic) obj).getLocation() != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the <code>IShowInSource</code> for this editor.
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
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					for (Iterator iter = selection.iterator(); iter.hasNext();) {
						Object obj = iter.next();
						if (obj instanceof CtxHelpTopic) {
							IPath path = ((CtxHelpTopic) obj).getLocation();
							if (path != null && !path.isEmpty()) {
								CtxHelpModel model = (CtxHelpModel) getAggregateModel();
								IPath pluginPath = model.getUnderlyingResource().getProject().getFullPath();
								IResource resource = root.findMember(pluginPath.append(path));
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
	 * Returns the <code>IShowInTargetList</code> for this editor.
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
		return CtxHelpInputContext.CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#addEditorPages()
	 */
	protected void addEditorPages() {
		try {
			addPage(new CtxHelpPage(this));
		} catch (PartInitException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
		addSourcePage(CtxHelpInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createContentOutline()
	 */
	protected ISortableContentOutlinePage createContentOutline() {
		return new CtxHelpFormOutlinePage(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createInputContextManager()
	 */
	protected InputContextManager createInputContextManager() {
		return new CtxHelpInputContextManager(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createResourceContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IFileEditorInput)
	 */
	protected void createResourceContexts(InputContextManager contexts, IFileEditorInput input) {
		contexts.putContext(input, new CtxHelpInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createStorageContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IStorageEditorInput)
	 */
	protected void createStorageContexts(InputContextManager contexts, IStorageEditorInput input) {
		contexts.putContext(input, new CtxHelpInputContext(this, input, true));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createSystemFileContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.pde.internal.ui.editor.SystemFileEditorInput)
	 */
	protected void createSystemFileContexts(InputContextManager contexts, FileStoreEditorInput input) {
		try {
			IFileStore store = EFS.getStore(input.getURI());
			IEditorInput in = new FileStoreEditorInput(store);
			contexts.putContext(in, new CtxHelpInputContext(this, in, true));
		} catch (CoreException e) {
			PDEUserAssistanceUIPlugin.logException(e);
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
		return fInputContextManager.findContext(CtxHelpInputContext.CONTEXT_ID);
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
		if ((formPage != null) && (formPage instanceof CtxHelpPage)) {
			// Synchronizes the selection made in the master tree view with the
			// selection in the outline view when the link with editor button
			// is toggled on
			return ((CtxHelpPage) formPage).getSelection();
		}
		return super.getSelection();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#canCut(org.eclipse.jface.viewers.ISelection)
	 */
	public boolean canCut(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			for (Iterator iter = sel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof CtxHelpObject && ((CtxHelpObject) obj).canBeRemoved()) {
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
		return new CtxHelpSourcePage(editor, title, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#contributeToToolbar(org.eclipse.jface.action.IToolBarManager)
	 */
	public void contributeToToolbar(IToolBarManager manager) {
		if (getAggregateModel().isEditable()) {
			manager.add(new ControlContribution("Register") { //$NON-NLS-1$
						protected Control createControl(Composite parent) {
							ImageHyperlink fImageHyperlinkRegisterTOC = new ImageHyperlink(parent, SWT.NONE);
							fImageHyperlinkRegisterTOC.setText(CtxHelpMessages.CtxHelpEditor_text);
							fImageHyperlinkRegisterTOC.setUnderlined(true);
							fImageHyperlinkRegisterTOC.setForeground(getToolkit().getHyperlinkGroup().getForeground());
							fImageHyperlinkRegisterTOC.addHyperlinkListener(new IHyperlinkListener() {
								public void linkActivated(HyperlinkEvent e) {
									handleRegisterCtxHelpFile();
								}

								public void linkEntered(HyperlinkEvent e) {
									((ImageHyperlink) e.getSource()).setForeground(getToolkit().getHyperlinkGroup().getActiveForeground());
									getEditorSite().getActionBars().getStatusLineManager().setMessage(CtxHelpMessages.CtxHelpEditor_text);
								}

								public void linkExited(HyperlinkEvent e) {
									((ImageHyperlink) e.getSource()).setForeground(getToolkit().getHyperlinkGroup().getForeground());
									getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
								}
							});
							return fImageHyperlinkRegisterTOC;
						}
					});
		}
	}

	/**
	 * Opens the register context help wizard dialog.
	 */
	private void handleRegisterCtxHelpFile() {
		RegisterCtxHelpWizard wizard = new RegisterCtxHelpWizard((IModel) getAggregateModel());
		WizardDialog dialog = new WizardDialog(PDEUserAssistanceUIPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		dialog.getShell().setSize(400, 250);
		dialog.open();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor monitor) {
		CtxHelpModel model = (CtxHelpModel) getAggregateModel();
		model.setMarkerRefreshNeeded(true);

		super.doSave(monitor);
		model.reconciled(model.getDocument()); //model recon occurs async so we can proceed to save

	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#dispose()
	 */
	public void dispose() {
		//editor is closing, delete the markers
		CtxHelpMarkerManager.deleteMarkers((CtxHelpModel) getAggregateModel());
		super.dispose();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createInputContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager)
	 */
	protected void createInputContexts(InputContextManager contextManager) {
		super.createInputContexts(contextManager);

		// model is loaded, create markers if there were errors found
		CtxHelpMarkerManager.createMarkers((CtxHelpModel) getAggregateModel());
	}

}
