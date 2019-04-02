/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
 *     Lucas Bullen (Red Hat Inc.) - Bug 520216 - Add generic editor as a tab
 *                                 - Bug 531226 - Update to reflect addition of source tab
 *                                 - Bug 531602 - formatting munged by editor
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 541067
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.shared.target.*;
import org.eclipse.pde.internal.ui.wizards.exports.TargetDefinitionExportWizard;
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
import org.eclipse.ui.internal.genericeditor.ExtensionBasedTextEditor;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.xml.sax.SAXException;

/**
 * Editor for target definition (*.target) files.  Interacts with the ITargetDefinition model
 * to modify target attributes.  Uses the target platform service to persist the modified model
 * to the backing file.
 *
 * @see ITargetDefinition
 * @see ITargetPlatformService
 */
public class TargetEditor extends FormEditor {

	private List<IManagedForm> fManagedFormPages = new ArrayList<>(2);
	private ExtensionBasedTextEditor fTextualEditor;
	private int fSourceTabIndex;
	private IDocument fTargetDocument;
	private IDocumentListener fTargetDocumentListener;

	private InputHandler fInputHandler = new InputHandler();
	private TargetChangedListener fTargetChangedListener;
	private boolean fDirty;

	private ImageHyperlink fLoadHyperlink;

	private EventHandler fEventHandler = e -> handleBrokerEvent(e);

	@Override
	protected FormToolkit createToolkit(Display display) {
		return new FormToolkit(PDEPlugin.getDefault().getFormColors(display));
	}

	@Override
	protected void addPages() {
		try {
			setActiveEditor(this);
			addPage(new DefinitionPage(this));
			addPage(new ContentPage(this));
			addPage(new EnvironmentPage(this));
			addTextualEditorPage();
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		BundleContext bundleContext = PDECore.getDefault().getBundleContext();
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.subscribe(TargetEvents.TOPIC_WORKSPACE_TARGET_CHANGED, fEventHandler);
	}

	@Override
	protected void pageChange(int newPageIndex) {
		try {
			if (newPageIndex != fSourceTabIndex && getCurrentPage() == fSourceTabIndex) {
				InputStream stream = new ByteArrayInputStream(fTargetDocument.get().getBytes(StandardCharsets.UTF_8));
				TargetDefinitionPersistenceHelper.initFromXML(getTarget(), stream);
				if (!getTarget().isResolved()) {
					getTargetChangedListener().contentsChanged(getTarget(), this, true, false);
				}
			}
			super.pageChange(newPageIndex);
		} catch (CoreException e) {
			setActivePage(fSourceTabIndex);
			showError(PDEUIMessages.TargetEditor_5, e);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			setActivePage(fSourceTabIndex);
			CoreException ce = new CoreException(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), e.getMessage(), e));
			showError(PDEUIMessages.TargetEditor_5, ce);
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		fInputHandler.setSaving(true);
		if (!isActiveTabTextualEditor()) {
			markStale();
			updateTextualEditor();
		}
		fTextualEditor.doSave(monitor);
		fDirty = false;
		editorDirtyStateChanged();
		fInputHandler.setSaving(false);
	}

	@Override
	public void doSaveAs() {
		commitPages(true);
		ITargetDefinition target = getTarget();

		SaveAsDialog dialog = new SaveAsDialog(getSite().getShell());
		dialog.create();
		dialog.setMessage(PDEUIMessages.TargetEditor_0, IMessageProvider.NONE);
		if (target.getHandle() instanceof WorkspaceFileTargetHandle) {
			WorkspaceFileTargetHandle currentTargetHandle = (WorkspaceFileTargetHandle) target.getHandle();
			dialog.setOriginalFile(currentTargetHandle.getTargetFile());
		}
		dialog.open();

		IPath path = dialog.getResult();

		if (path == null) {
			return;
		}
		if (!"target".equalsIgnoreCase(path.getFileExtension())) { //$NON-NLS-1$
			path = path.addFileExtension("target"); //$NON-NLS-1$
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IFile file = workspace.getRoot().getFile(path);

		if (workspace.validateEdit(new IFile[] {file}, getSite().getShell()).isOK()) {
			try {
				WorkspaceFileTargetHandle newFileTarget = new WorkspaceFileTargetHandle(file);
				newFileTarget.save(target);
				setInput(new FileEditorInput(file));
			} catch (CoreException e) {
				PDEPlugin.log(e);
				showError(PDEUIMessages.TargetEditor_3, e);
			}
		}
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	protected void setDirty(boolean dirty) {
		fDirty = fDirty || dirty;
		editorDirtyStateChanged();
		if (fDirty && isActiveTabTextualEditor()) {
			updateTextualEditor();
		}
	}

	@Override
	public boolean isDirty() {
		return fDirty || super.isDirty();
	}

	public void markStale() {
		for (IManagedForm form : fManagedFormPages) {
			IFormPart[] parts = form.getParts();
			for (IFormPart part : parts) {
				if (part instanceof AbstractFormPart) {
					((AbstractFormPart) part).markStale();
				}
			}
		}
		editorDirtyStateChanged();
	}

	/*
	 * @see org.eclipse.ui.forms.editor.FormEditor#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 * @since 3.7
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof IFileEditorInput) && !(input instanceof IURIEditorInput))
			throw new PartInitException(NLS.bind(PDEUIMessages.TargetEditor_6, input.getClass().getName()));
		super.init(site, input);
	}

	@Override
	protected void setInput(IEditorInput input) {
		super.setInput(input);
		setPartName(getEditorInput().getName());
		fInputHandler.setInput(input);
	}

	@Override
	public void dispose() {
		BundleContext bundleContext = PDECore.getDefault().getBundleContext();
		IEclipseContext context = EclipseContextFactory.getServiceContext(bundleContext);
		IEventBroker eventBroker = context.get(IEventBroker.class);
		eventBroker.unsubscribe(fEventHandler);

		// Cancel any resolution jobs that are runnning
		Job.getJobManager().cancel(getTargetChangedListener().getJobFamily());
		getTargetChangedListener().setContentTree(null);
		getTargetChangedListener().setLocationTree(null);

		fInputHandler.dispose();
		super.dispose();
	}

	/**
	 * Returns the target model backing this editor
	 * @return target model
	 */
	public ITargetDefinition getTarget() {
		return fInputHandler.getTarget();
	}

	/**
	 * @return a shared listener that will refresh UI components when the target is modified
	 */
	public TargetChangedListener getTargetChangedListener() {
		if (fTargetChangedListener == null) {
			fTargetChangedListener = new TargetChangedListener();
		}
		return fTargetChangedListener;
	}

	/**
	 * Handles the revert action
	 */
	public void doRevert() {
		fInputHandler.reset();
		for (IManagedForm form : fManagedFormPages) {
			IFormPart[] parts = form.getParts();
			for (IFormPart part : parts) {
				if (part instanceof AbstractFormPart) {
					((AbstractFormPart) part).markStale();
				}
			}
		}
		setActivePage(getActivePage());
		editorDirtyStateChanged();
	}

	public void contributeToToolbar(final ScrolledForm form, String contextID) {
		ControlContribution setAsTarget = new ControlContribution("Set") { //$NON-NLS-1$

			@Override
			protected Control createControl(Composite parent) {
				PDEPreferencesManager preferences = PDECore.getDefault().getPreferencesManager();
				String mementoPref = null;
				String memento = null;
				String hyperLinkText = PDEUIMessages.AbstractTargetPage_setTarget;
				mementoPref = preferences.getString(ICoreConstants.WORKSPACE_TARGET_HANDLE);
				try {
					memento = getTarget().getHandle().getMemento();
				} catch (CoreException e) {
				}
				if (mementoPref != null && memento != null) {
					if (memento.equals(mementoPref)) {
						hyperLinkText = PDEUIMessages.AbstractTargetPage_reloadTarget;
					}
				}
				fLoadHyperlink = new ImageHyperlink(parent, SWT.NONE | SWT.NO_FOCUS);
				fLoadHyperlink.setText(hyperLinkText);
				fLoadHyperlink.setUnderlined(true);
				fLoadHyperlink.setForeground(getToolkit().getHyperlinkGroup().getForeground());
				fLoadHyperlink.addHyperlinkListener(new IHyperlinkListener() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						IEditorPart editorPart = TargetEditor.this;
						IWorkbenchPage page = editorPart.getSite().getPage();
						if (TargetEditor.this.isDirty()) {
							page.saveEditor(editorPart, true);
						}
						ITargetDefinition target = getTarget();
						LoadTargetDefinitionJob.load(target);
					}

					@Override
					public void linkEntered(HyperlinkEvent e) {
						HyperlinkGroup hyperlinkGroup = getHyperlinkGroup();

						if (hyperlinkGroup != null) {
							fLoadHyperlink.setForeground(hyperlinkGroup.getActiveForeground());
						}
					}

					@Override
					public void linkExited(HyperlinkEvent e) {
						HyperlinkGroup hyperlinkGroup = getHyperlinkGroup();

						if (hyperlinkGroup != null) {
							fLoadHyperlink.setForeground(hyperlinkGroup.getForeground());
						}
					}

					private HyperlinkGroup getHyperlinkGroup() {
						FormToolkit toolkit = getToolkit();
						HyperlinkGroup hyperlinkGroup = null;
						if (toolkit != null) {
							hyperlinkGroup = toolkit.getHyperlinkGroup();
						}
						return hyperlinkGroup;
					}
				});
				return fLoadHyperlink;
			}
		};

		final String helpContextID = contextID;
		Action help = new Action("help") { //$NON-NLS-1$
			@Override
			public void run() {
				BusyIndicator.showWhile(form.getForm().getDisplay(), () -> PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContextID));
			}
		};
		help.setToolTipText(PDEUIMessages.PDEFormPage_help);
		help.setImageDescriptor(PDEPluginImages.DESC_HELP);

		Action export = new Action("export") { //$NON-NLS-1$
			@Override
			public void run() {
				TargetDefinitionExportWizard wizard = new TargetDefinitionExportWizard(getTarget());
				wizard.setWindowTitle(PDEUIMessages.ExportActiveTargetDefinition);
				WizardDialog dialog = new WizardDialog(getSite().getShell(), wizard);
				dialog.open();
			}
		};
		export.setToolTipText("Export"); //$NON-NLS-1$
		export.setImageDescriptor(PDEPluginImages.DESC_EXPORT_TARGET_TOOL);

		form.getToolBarManager().add(setAsTarget);
		form.getToolBarManager().add(export);
		form.getToolBarManager().add(help);
		form.updateToolBar();

	}

	/**
	 * Adds the given form to the list of forms to be refreshed when reverting
	 * @param managedForm
	 */
	public void addForm(IManagedForm managedForm) {
		fManagedFormPages.add(managedForm);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(managedForm.getForm().getBody(), IHelpContextIds.TARGET_EDITOR);
	}

	/**
	 * Opens an error dialog using the editor's shell.
	 * @param message error message to display
	 * @param exception exception to display in details section
	 */
	public void showError(final String message, final CoreException exception) {
		Display display = getSite().getShell().getDisplay();
		display.asyncExec(() -> ErrorDialog.openError(getSite().getShell(), PDEUIMessages.TargetEditor_4, message, exception.getStatus()));
	}

	/**
	 * The InputHandler serves as a bridge between the input file and the TargetDefinition model.
	 */
	private class InputHandler implements IResourceChangeListener {
		private IEditorInput fInput;
		private ITargetDefinition fTarget;
		private IFile fTargetFileInWorkspace;
		private boolean fSaving = false;

		public void dispose() {
			PDEPlugin.getWorkspace().removeResourceChangeListener(this);
		}

		public void reset() {
			setInput(fInput);
		}

		public void setSaving(boolean saving) {
			this.fSaving = saving;
		}

		public void setInput(IEditorInput input) {
			fInput = input;
			fTargetFileInWorkspace = null;
			fTarget = null;
			File targetFile = null;
			if (input instanceof IFileEditorInput) {
				fTargetFileInWorkspace = ((IFileEditorInput) input).getFile();
				targetFile = fTargetFileInWorkspace.getLocation().toFile();
			} else if (input instanceof IURIEditorInput) {
				String part = ((IURIEditorInput) input).getURI().getSchemeSpecificPart();
				Path path = new Path(part);
				fTargetFileInWorkspace = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				targetFile = path.toFile();
			}

			//If the file is not available or invalid, close the target editor
			if (fTargetFileInWorkspace == null || targetFile == null || !targetFile.exists()) {
				TargetEditor.this.close(false);
			}
			PDEPlugin.getWorkspace().addResourceChangeListener(this);
		}

		/**
		 * @return the target definition that is the input to the editor
		 */
		public ITargetDefinition getTarget() {
			if (fTarget == null) {
				try {
					loadTargetDefinition();
					TargetEditor.this.fDirty = false;
					TargetEditor.this.editorDirtyStateChanged();
				} catch (CoreException e) {
					PDEPlugin.log(e);
					setActivePage(fSourceTabIndex);
				}
			}
			return fTarget;
		}

		private ITargetDefinition loadTargetDefinition() throws CoreException {
			ITargetPlatformService service = getTargetPlatformService();
			try {
				if (fInput instanceof IFileEditorInput) {
					ITargetHandle fileHandle = service.getTarget(((IFileEditorInput) fInput).getFile());
					fTarget = fileHandle.getTargetDefinition();
				} else if (fInput instanceof IURIEditorInput) {
					ITargetHandle externalTarget = service.getTarget(((IURIEditorInput) fInput).getURI());
					fTarget = externalTarget.getTargetDefinition();
				}
			} catch (CoreException e) {
				fTarget = service.newTarget();
				throw e;
			}
			TargetEditor.this.getTargetChangedListener().contentsChanged(fTarget, this, true, false);
			return fTarget;
		}

		private ITargetPlatformService getTargetPlatformService() throws CoreException {
			ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (service == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "ITargetPlatformService not available")); //$NON-NLS-1$
			}
			return service;
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
				IResourceDelta delta = event.getDelta().findMember(fTargetFileInWorkspace.getFullPath());
				if (delta != null) {
					if (delta.getKind() == IResourceDelta.REMOVED) {
						TargetEditor.this.close(false);
					} else if (delta.getKind() == IResourceDelta.CHANGED || delta.getKind() == IResourceDelta.REPLACED) {
						if (!fSaving) {
							Display display = getSite().getShell().getDisplay();
							display.asyncExec(() -> {
								if (getActivePage() != -1)
									TargetEditor.this.doRevert();
							});
						}
					}
				}
			}
		}
	}

	/**
	 * initializes fTargetDocument and fTargetDocumentListener
	 *
	 * @throws PartInitException
	 */
	private void addTextualEditorPage() throws PartInitException {
		fTextualEditor = new ExtensionBasedTextEditor();
		fSourceTabIndex = addPage(fTextualEditor, getEditorInput());
		Control editorControl = fTextualEditor.getAdapter(Control.class);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(editorControl, IHelpContextIds.TARGET_EDITOR_SOURCE_PAGE);
		setPageText(fSourceTabIndex, PDEUIMessages.GenericEditorTab_title);

		fTargetDocument = fTextualEditor.getDocumentProvider().getDocument(getEditorInput());

		fTargetDocumentListener = new IDocumentListener() {
			@Override
			public void documentChanged(DocumentEvent event) {
				markStale();
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				// documentChanged used instead
			}
		};
		fTargetDocument.addDocumentListener(fTargetDocumentListener);
	}

	private void updateTextualEditor() {
		fTargetDocument.removeDocumentListener(fTargetDocumentListener);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		try {
			TargetDefinitionPersistenceHelper.persistXML(getTarget(), byteArrayOutputStream);
			fTargetDocument.set(new String(byteArrayOutputStream.toByteArray()));
		} catch (CoreException | ParserConfigurationException | TransformerException | IOException | SAXException e) {
			PDEPlugin.log(e);
		}
		fTargetDocument.addDocumentListener(fTargetDocumentListener);
	}

	private boolean isActiveTabTextualEditor() {
		return getActivePage() == getPageCount() - 1;
	}

	/**
	 * When changes are noticed in the target, this listener will resolve the
	 * target and update the necessary pages in the editor.
	 */
	class TargetChangedListener implements ITargetChangedListener {
		private TargetLocationsGroup fLocationTree;
		private TargetContentsGroup fContentTree;
		private Object fJobFamily = new Object();

		public void setLocationTree(TargetLocationsGroup locationTree) {
			fLocationTree = locationTree;
		}

		public void setContentTree(TargetContentsGroup contentTree) {
			fContentTree = contentTree;
		}

		/**
		 * @return non-null object identifier for any jobs created by this listener
		 */
		public Object getJobFamily() {
			return fJobFamily;
		}

		@Override
		public void contentsChanged(ITargetDefinition definition, Object source, boolean resolve, boolean forceResolve) {
			if (!forceResolve && (!resolve || definition.isResolved())) {
				if (fContentTree != null && source != fContentTree) {
					ITargetDefinition target = getTarget();
					// Check to see if we are resolved, resolving, or cancelled
					if (target != null && target.isResolved()) {
						fContentTree.setInput(getTarget());
					} else if (Job.getJobManager().find(getJobFamily()).length > 0) {
						fContentTree.setInput(null);
					} else {
						fContentTree.setCancelled();
					}

				}
				if (fLocationTree != null && source != fLocationTree) {
					fLocationTree.setInput(getTarget());
				}
			} else {
				if (fContentTree != null) {
					fContentTree.setInput(null);
				}
				if (fLocationTree != null) {
					fLocationTree.setInput(getTarget());
				}
				Job.getJobManager().cancel(getJobFamily());
				// delete profile
				try {
					P2TargetUtils.forceCheckTarget(getTarget());
					P2TargetUtils.deleteProfile(getTarget().getHandle());
				} catch (CoreException e) {
					PDEPlugin.log(e);
				}
				String name = getTarget().getName();
				if (name == null)
					name = ""; //$NON-NLS-1$
				Job resolveJob = new Job(NLS.bind(PDEUIMessages.TargetEditor_1, name)) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						getTarget().resolve(monitor);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						// Don't return any problems because we don't want an error dialog
						return Status.OK_STATUS;
					}

					@Override
					public boolean belongsTo(Object family) {
						return family.equals(getJobFamily());
					}
				};
				resolveJob.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(org.eclipse.core.runtime.jobs.IJobChangeEvent event) {
						final IStatus status = event.getResult();
						UIJob job = new UIJob(PDEUIMessages.TargetEditor_2) {
							@Override
							public IStatus runInUIThread(IProgressMonitor monitor) {
								if (fContentTree != null) {
									if (status.getSeverity() == IStatus.CANCEL) {
										fContentTree.setCancelled();
									} else {
										fContentTree.setInput(getTarget());
									}
								}
								if (fLocationTree != null) {
									fLocationTree.setInput(getTarget());
								}
								return Status.OK_STATUS;
							}
						};
						job.setSystem(true);
						job.schedule();
					}
				});
				resolveJob.schedule();
			}
		}
	}

	private void updateHyperlinkText(String s) {
		if (fLoadHyperlink != null && !fLoadHyperlink.isDisposed()) {
			fLoadHyperlink.setText(s);
		}
		ITextViewer viewer = fTextualEditor.getAdapter(ITextViewer.class);
		if (viewer instanceof ISourceViewerExtension5) {
			ISourceViewerExtension5 extension5 = (ISourceViewerExtension5) viewer;
			extension5.updateCodeMinings();
		}
	}

	private void handleBrokerEvent(Event event) {
		String topic = event.getTopic();
		if (TargetEvents.TOPIC_WORKSPACE_TARGET_CHANGED.equals(topic)) {
			ITargetDefinition workspaceTarget = extractTargetDefinition(event);
			handleWorkspaceTargetChanged(workspaceTarget);
		}
	}

	private void handleWorkspaceTargetChanged(ITargetDefinition workspaceTarget) {
		ITargetDefinition editorTarget = getTarget();
		if (editorTarget == null) {
			return;
		}
		ITargetHandle editorHandle = editorTarget.getHandle();
		ITargetHandle changedHandle = workspaceTarget.getHandle();
		try {
			final boolean isCurrent = Objects.equals(editorHandle.getMemento(), changedHandle.getMemento());
			final String label = isCurrent ? PDEUIMessages.AbstractTargetPage_reloadTarget
					: PDEUIMessages.AbstractTargetPage_setTarget;
			Display.getDefault().asyncExec(() -> updateHyperlinkText(label));
		} catch (CoreException e) {
			PDECore.log(e.getStatus());
		}
	}

	private ITargetDefinition extractTargetDefinition(Event event) {
		Object data = event.getProperty(IEventBroker.DATA);
		if (data instanceof ITargetDefinition) {
			return (ITargetDefinition) data;
		}
		return null;
	}

}
