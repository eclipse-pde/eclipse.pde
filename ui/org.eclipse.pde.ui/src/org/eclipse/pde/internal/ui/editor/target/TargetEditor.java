/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.target;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.internal.core.LoadTargetOperation;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.progress.IProgressService;

public class TargetEditor extends PDEFormEditor {

	protected static String LAST_PATH;

	static {
		Location installLoc = Platform.getInstallLocation();
		if (installLoc == null) {
			LAST_PATH = ""; //$NON-NLS-1$
		}
		URL url = installLoc.getURL();
		LAST_PATH = new Path(url.getPath()).toOSString();
	}

	public TargetEditor() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getEditorID()
	 */
	protected String getEditorID() {
		return IPDEUIConstants.TARGET_EDITOR_ID;
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
		return TargetInputContext.CONTEXT_ID;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createInputContextManager()
	 */
	protected InputContextManager createInputContextManager() {
		return new TargetInputContextManager(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createResourceContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IFileEditorInput)
	 */
	protected void createResourceContexts(InputContextManager manager, IFileEditorInput input) {
		manager.putContext(input, new TargetInputContext(this, input, true));
		manager.monitorFile(input.getFile());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createSystemFileContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.pde.internal.ui.editor.SystemFileEditorInput)
	 */
	protected void createSystemFileContexts(InputContextManager manager, SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		if (file != null) {
			String name = file.getName();
			if (name.endsWith(".target")) { //$NON-NLS-1$
				IEditorInput in = new SystemFileEditorInput(file);
				manager.putContext(in, new TargetInputContext(this, in, true));
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createStorageContexts(org.eclipse.pde.internal.ui.editor.context.InputContextManager, org.eclipse.ui.IStorageEditorInput)
	 */
	protected void createStorageContexts(InputContextManager manager, IStorageEditorInput input) {
		if (input.getName().endsWith(".target")) { //$NON-NLS-1$
			manager.putContext(input, new TargetInputContext(this, input, true));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#createContentOutline()
	 */
	protected ISortableContentOutlinePage createContentOutline() {
		return new TargetOutlinePage(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormEditor#getInputContext(java.lang.Object)
	 */
	protected InputContext getInputContext(Object object) {
		return fInputContextManager.findContext(TargetInputContext.CONTEXT_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
	 */
	protected void addEditorPages() {
		try {
			addPage(new OverviewPage(this));
			addPage(new ContentPage(this));
			addPage(new EnvironmentPage(this));
			addPageChangedListener(new IPageChangedListener() {

				public void pageChanged(PageChangedEvent event) {
					Object o = event.getSelectedPage();
					if (o instanceof EnvironmentPage)
						((EnvironmentPage) o).updateChoices();
				}

			});
		} catch (PartInitException e) {
			PDEPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#contextAdded(org.eclipse.pde.internal.ui.editor.context.InputContext)
	 */
	public void editorContextAdded(InputContext context) {
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
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.context.IInputContextListener#monitoredFileRemoved(org.eclipse.core.resources.IFile)
	 */
	public boolean monitoredFileRemoved(IFile monitoredFile) {
		return true;
	}

	public void contributeToToolbar(IToolBarManager manager) {
		ControlContribution save = new ControlContribution("Set") { //$NON-NLS-1$
			protected Control createControl(Composite parent) {
				final ImageHyperlink hyperlink = new ImageHyperlink(parent, SWT.NONE);
				hyperlink.setText(PDEUIMessages.AbstractTargetPage_setTarget);
				hyperlink.setUnderlined(true);
				hyperlink.setForeground(getToolkit().getHyperlinkGroup().getForeground());
				hyperlink.addHyperlinkListener(new IHyperlinkListener() {
					public void linkActivated(HyperlinkEvent e) {
						doLoadTarget();
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
		manager.add(save);
	}

	private void doLoadTarget() {
		IRunnableWithProgress run = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					ITargetModel model = getTargetModel();
					if (!model.isLoaded()) {
						MessageDialog.openError(getActiveEditor().getSite().getShell(), PDEUIMessages.TargetPlatformPreferencePage_invalidTitle, PDEUIMessages.TargetPlatformPreferencePage_invalidDescription);
						monitor.done();
						return;
					}
					LoadTargetOperation op = new LoadTargetOperation(getTarget(), getFilePath());
					PDEPlugin.getWorkspace().run(op, monitor);
					Object[] features = op.getMissingFeatures();
					Object[] plugins = op.getMissingPlugins();
					if (plugins.length + features.length > 0)
						TargetErrorDialog.showDialog(getActiveEditor().getSite().getShell(), features, plugins);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} catch (OperationCanceledException e) {
					throw new InterruptedException(e.getMessage());
				} finally {
					monitor.done();
				}
			}
		};
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {
			service.runInUI(service, run, PDEPlugin.getWorkspace().getRoot());
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}

	private ITarget getTarget() {
		return getTargetModel().getTarget();
	}

	private ITargetModel getTargetModel() {
		return ((ITargetModel) getAggregateModel());
	}

	private IPath getFilePath() {
		IEditorInput input = getEditorInput();
		if (input instanceof IFileEditorInput) {
			IFile file = ((IFileEditorInput) input).getFile();
			if (file != null)
				return file.getFullPath();
		}
		return null;
	}

}