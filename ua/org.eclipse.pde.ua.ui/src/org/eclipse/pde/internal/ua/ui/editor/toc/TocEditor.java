/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ua.core.toc.text.TocMarkerManager;
import org.eclipse.pde.internal.ua.core.toc.text.TocModel;
import org.eclipse.pde.internal.ua.core.toc.text.TocObject;
import org.eclipse.pde.internal.ua.ui.IConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.wizards.toc.RegisterTocWizard;
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
 * Editor for table of contents xml files (for user assistance). Provides a
 * graphical way to view and edit the xml files, as well as a source viewer.
 */
public class TocEditor extends MultiSourceEditor {

	private ImageHyperlink fImageHyperlinkRegisterTOC;

	public TocEditor() {
		super();
	}

	@Override
	protected String getEditorID() {
		return IConstants.TABLE_OF_CONTENTS_EDITOR_ID;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (inUiThread() && isShowInApplicable()) {
			if (adapter == IShowInSource.class) {
				return adapter.cast(getShowInSource());
			}
		}

		if (adapter == IShowInTargetList.class) {
			return adapter.cast(getShowInTargetList());
		}

		return super.getAdapter(adapter);
	}

	private boolean inUiThread() {
		// get our workbench display
		Display display = getSite().getWorkbenchWindow().getWorkbench()
				.getDisplay();

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
			for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
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
	 *
	 * @return the <code>IShowInSource</code>
	 */
	private IShowInSource getShowInSource() {
		return () -> {
			ArrayList<IResource> resourceList = new ArrayList<>();
			IStructuredSelection selection = (IStructuredSelection) getSelection();
			IStructuredSelection resources;
			if (selection.isEmpty()) {
				resources = null;
			} else {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

				for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
					Object obj = iter.next();
					if (obj instanceof TocObject && ((TocObject) obj).getPath() != null) {
						Path resourcePath = new Path(((TocObject) obj).getPath());

						if (!resourcePath.isEmpty()) {
							TocModel model = (TocModel) getAggregateModel();
							IResource underlyingResource = model.getUnderlyingResource();
							if (underlyingResource != null) {
								IProject project = underlyingResource.getProject();
								if (project != null) {
									IPath pluginPath = project.getFullPath();
									IResource resource = root.findMember(pluginPath.append(resourcePath));
									if (resource != null) {
										resourceList.add(resource);
									}
								}
							}
						}
					}
				}

				resources = new StructuredSelection(resourceList);
			}

			return new ShowInContext(null, resources);
		};
	}

	/**
	 * Returns the <code>IShowInTargetList</code> for this section.
	 *
	 * @return the <code>IShowInTargetList</code>
	 */
	private IShowInTargetList getShowInTargetList() {
		return () -> new String[] { JavaUI.ID_PACKAGES, IPageLayout.ID_RES_NAV };
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	@Override
	public String getContextIDForSaveAs() {
		return TocInputContext.CONTEXT_ID;
	}

	@Override
	protected void addEditorPages() {
		try {
			addPage(new TocPage(this));
		} catch (PartInitException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
		// Add source page
		addSourcePage(TocInputContext.CONTEXT_ID);
	}

	@Override
	protected ISortableContentOutlinePage createContentOutline() {
		return new TocFormOutlinePage(this);
	}

	@Override
	protected InputContextManager createInputContextManager() {
		return new TocInputContextManager(this);
	}

	@Override
	protected void createResourceContexts(InputContextManager contexts,
			IFileEditorInput input) {
		contexts.putContext(input, new TocInputContext(this, input, true));
		contexts.monitorFile(input.getFile());
	}

	@Override
	protected void createStorageContexts(InputContextManager contexts,
			IStorageEditorInput input) {
		contexts.putContext(input, new TocInputContext(this, input, true));
	}

	@Override
	protected void createSystemFileContexts(InputContextManager contexts,
			FileStoreEditorInput input) {
		try {
			IFileStore store = EFS.getStore(input.getURI());
			IEditorInput in = new FileStoreEditorInput(store);
			contexts.putContext(in, new TocInputContext(this, in, true));
		} catch (CoreException e) {
			PDEUserAssistanceUIPlugin.logException(e);
		}
	}

	@Override
	public void editorContextAdded(InputContext context) {
		// Add the source page
		addSourcePage(context.getId());
	}

	@Override
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(TocInputContext.CONTEXT_ID);
	}

	@Override
	public void contextRemoved(InputContext context) {
		close(false);
	}

	@Override
	public void monitoredFileAdded(IFile monitoredFile) {
		// NO-OP
	}

	@Override
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	@Override
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

	@Override
	public boolean canCut(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection sel = (IStructuredSelection) selection;
			for (Iterator<?> iter = sel.iterator(); iter.hasNext();) {
				Object obj = iter.next();
				if (obj instanceof TocObject
						&& ((TocObject) obj).canBeRemoved()) {
					return canCopy(selection);
				}
			}
		}

		return false;
	}

	@Override
	protected PDESourcePage createSourcePage(PDEFormEditor editor,
			String title, String name, String contextId) {
		return new TocSourcePage(editor, title, name);
	}

	@Override
	public void contributeToToolbar(IToolBarManager manager) {
		// Add the register cheat sheet link to the form title area
		if (WorkspaceModelManager.isPluginProject(getCommonProject())
				&& getAggregateModel().isEditable()) {
			manager.add(createUIControlConRegisterCS());
		}
	}

	private ControlContribution createUIControlConRegisterCS() {
		return new ControlContribution("Register") { //$NON-NLS-1$
			@Override
			protected Control createControl(Composite parent) {
				// Create UI
				createUIImageHyperlinkRegisterToc(parent);
				// Create Listener
				createUIListenerImageHyperlinkRegisterToc();
				return fImageHyperlinkRegisterTOC;
			}
		};
	}

	private void createUIImageHyperlinkRegisterToc(Composite parent) {
		fImageHyperlinkRegisterTOC = new ImageHyperlink(parent, SWT.NONE);
		fImageHyperlinkRegisterTOC.setText(TocMessages.TocEditor_link);
		fImageHyperlinkRegisterTOC.setUnderlined(true);
		fImageHyperlinkRegisterTOC.setForeground(getToolkit()
				.getHyperlinkGroup().getForeground());
	}

	private void createUIListenerImageHyperlinkRegisterToc() {
		fImageHyperlinkRegisterTOC
				.addHyperlinkListener(new IHyperlinkListener() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						handleLinkActivatedRegisterTOC();
					}

					@Override
					public void linkEntered(HyperlinkEvent e) {
						handleLinkEnteredRegisterTOC(e.getLabel());
					}

					@Override
					public void linkExited(HyperlinkEvent e) {
						handleLinkExitedRegisterTOC();
					}
				});
	}

	private void handleLinkEnteredRegisterTOC(String message) {
		// Update colour
		fImageHyperlinkRegisterTOC.setForeground(getToolkit()
				.getHyperlinkGroup().getActiveForeground());
		// Update IDE status line
		getEditorSite().getActionBars().getStatusLineManager()
				.setMessage(message);
	}

	private void handleLinkExitedRegisterTOC() {
		// Update colour
		fImageHyperlinkRegisterTOC.setForeground(getToolkit()
				.getHyperlinkGroup().getForeground());
		// Update IDE status line
		getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}

	private void handleLinkActivatedRegisterTOC() {
		RegisterTocWizard wizard = new RegisterTocWizard(
				(IModel) getAggregateModel());
		// Initialize the wizard
		wizard.init(PlatformUI.getWorkbench(), null);
		// Create the dialog for the wizard
		WizardDialog dialog = new WizardDialog(
				PDEUserAssistanceUIPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		dialog.getShell().setSize(400, 250);
		// Check the result
		if (dialog.open() == Window.OK) {
			// NO-OP
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {

		TocModel model = (TocModel) getAggregateModel();
		model.setMarkerRefreshNeeded(true);
		super.doSave(monitor);
		model.reconciled(model.getDocument()); // model recon occurs async so we
												// can proceed to save

	}

	@Override
	public void dispose() {
		// editor is closing, delete the markers
		TocMarkerManager.deleteMarkers((TocModel) getAggregateModel());
		super.dispose();
	}

	@Override
	protected void createInputContexts(InputContextManager contextManager) {
		super.createInputContexts(contextManager);

		// model is loaded, create markers if there were errors found
		TocMarkerManager.createMarkers((TocModel) getAggregateModel());
	}
}
