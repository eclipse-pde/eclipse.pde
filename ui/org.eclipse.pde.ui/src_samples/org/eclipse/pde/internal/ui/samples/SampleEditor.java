/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.samples;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.ui.launcher.EclipseLaunchShortcut;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.EditorPart;

/**
 * @see EditorPart
 */
public class SampleEditor extends EditorPart {
	private FormToolkit toolkit;
	private ScrolledForm form;
	private FormText descText;
	private FormText instText;
	private final ILaunchShortcut defaultShortcut;
	private InputFileListener inputFileListener;

	class InputFileListener implements IResourceChangeListener, IResourceDeltaVisitor {
		@Override
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

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			if (resource instanceof IFile file) {
				if (file.equals(((IFileEditorInput) getEditorInput()).getFile())) {
					if (delta.getKind() == IResourceDelta.REMOVED || delta.getKind() == IResourceDelta.REPLACED)
						close();
					return false;
				}
			}
			return true;
		}
	}

	/**
	 *
	 */
	public SampleEditor() {
		defaultShortcut = new EclipseLaunchShortcut();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/**
	 * @see EditorPart#createPartControl
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createScrolledForm(parent);
		Properties properties = loadContent();
		form.setText(properties.getProperty("name")); //$NON-NLS-1$
		TableWrapLayout layout = new TableWrapLayout();
		layout.verticalSpacing = 10;
		layout.topMargin = 10;
		layout.bottomMargin = 10;
		layout.leftMargin = 10;
		layout.rightMargin = 10;
		form.getBody().setLayout(layout);

		final String launcher = properties.getProperty("launcher"); //$NON-NLS-1$
		final String launchTarget = properties.getProperty("launchTarget"); //$NON-NLS-1$

		descText = toolkit.createFormText(form.getBody(), true);
		descText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		String desc = properties.getProperty("description"); //$NON-NLS-1$
		String content = NLS.bind(PDEUIMessages.SampleEditor_desc, (desc != null ? desc : "")); //$NON-NLS-1$
		descText.setText(content, true, false);
		final String helpURL = properties.getProperty("helpHref"); //$NON-NLS-1$
		if (helpURL != null) {
			Hyperlink moreLink = toolkit.createHyperlink(form.getBody(), "Read More", SWT.NULL); //$NON-NLS-1$
			moreLink.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(HyperlinkEvent e) {
					PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpURL);
				}
			});
		}
		instText = toolkit.createFormText(form.getBody(), true);
		instText.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		StringBuilder buf = new StringBuilder();
		buf.append(PDEUIMessages.SampleEditor_content);
		instText.setText(buf.toString(), true, false);
		instText.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				Object href = e.getHref();
				if (href.equals("help")) { //$NON-NLS-1$
					PlatformUI.getWorkbench().getHelpSystem().displayHelpResource(helpURL);
				} else if (href.equals("run")) { //$NON-NLS-1$
					doRun(launcher, launchTarget, false);
				} else if (href.equals("debug")) { //$NON-NLS-1$
					doRun(launcher, launchTarget, true);
				}
			}
		});
		instText.setImage("run", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		instText.setImage("debug", PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		instText.setImage("help", PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK)); //$NON-NLS-1$
	}

	private void doRun(String launcher, String target, final boolean debug) {
		ILaunchShortcut shortcut = defaultShortcut;
		final ISelection selection;
		if (target != null) {
			selection = new StructuredSelection();
		} else
			selection = new StructuredSelection();
		final ILaunchShortcut fshortcut = shortcut;
		BusyIndicator.showWhile(form.getDisplay(),
				() -> fshortcut.launch(selection, debug ? ILaunchManager.DEBUG_MODE : ILaunchManager.RUN_MODE));
	}

	private Properties loadContent() {
		IStorageEditorInput input = (IStorageEditorInput) getEditorInput();
		Properties properties = new Properties();
		try {
			IStorage storage = input.getStorage();
			try (InputStream is = storage.getContents()) {
				properties.load(is);
			} catch (IOException e) {
				PDEPlugin.logException(e);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return properties;
	}

	@Override
	public void dispose() {
		if (inputFileListener != null) {
			PDEPlugin.getWorkspace().removeResourceChangeListener(inputFileListener);
			inputFileListener = null;
		}
		toolkit.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	/**
	 * @see EditorPart#setFocus
	 */
	@Override
	public void setFocus() {
		form.setFocus();
	}

	/**
	 * @see EditorPart#doSave
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	/**
	 * @see EditorPart#doSaveAs
	 */
	@Override
	public void doSaveAs() {
	}

	/**
	 * @see EditorPart#isDirty
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/**
	 * @see EditorPart#isSaveAsAllowed
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	/**
	 * @see EditorPart#init
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
		inputFileListener = new InputFileListener();
		PDEPlugin.getWorkspace().addResourceChangeListener(inputFileListener);
	}

	public void close() {
		Display display = getSite().getShell().getDisplay();
		display.asyncExec(() -> {
			if (toolkit != null) {
				getSite().getPage().closeEditor(SampleEditor.this, false);
			}
		});
	}
}
