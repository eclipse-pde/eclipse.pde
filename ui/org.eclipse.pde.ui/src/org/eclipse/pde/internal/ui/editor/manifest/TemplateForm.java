/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.update.ui.forms.internal.WebForm;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.update.ui.forms.internal.engine.*;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.launcher.RuntimeWorkbenchShortcut;
import org.eclipse.core.resources.*;
import org.eclipse.jdt.core.*;
import java.util.*;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.*;
import java.io.*;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.core.runtime.*;
import java.lang.reflect.InvocationTargetException;

public class TemplateForm extends WebForm {
	private static final String KEY_HEADING =
		"ManifestEditor.templatePage.heading";
	private static final String KEY_DONT_SHOW =
		"ManifestEditor.TemplatePage.dontShow";
	private ManifestTemplatePage page;
	private Button dontShowCheck;
	private boolean dontShow;
	private FormEngine text;
	private RuntimeWorkbenchShortcut launchShortcut;
	/**
	 * Constructor for TemplateForm.
	 */
	public TemplateForm(ManifestTemplatePage page) {
		this.page = page;
		launchShortcut = new RuntimeWorkbenchShortcut();
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	protected void createContents(final Composite parent) {
		HTMLTableLayout layout = new HTMLTableLayout();
		parent.setLayout(layout);
		layout.leftMargin = 10;
		layout.rightMargin = 0;
		layout.topMargin = 15;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 10;

		FormWidgetFactory factory = getFactory();

		SharedLabelProvider provider =
			PDEPlugin.getDefault().getLabelProvider();
		Image pageImage = provider.get(PDEPluginImages.DESC_PAGE_OBJ);
		Image runImage = provider.get(PDEPluginImages.DESC_RUN_EXC);
		Image debugImage = provider.get(PDEPluginImages.DESC_DEBUG_EXC);
		Image runWorkbenchImage =
			provider.get(PDEPluginImages.DESC_WORKBENCH_LAUNCHER_WIZ);

		HyperlinkAction pageAction = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				String pageId = link.getObjectId();
				((PDEFormPage) page).getEditor().showPage(pageId);
			}
		};
		HyperlinkAction debugAction = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				final String id = link.getObjectId();
				BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
					public void run() {
						if (id.equals("action.run")) {
							launchShortcut.run();
						} else if (id.equals("action.debug")) {
							launchShortcut.debug();
						}
					}
				});
			}
		};
		HyperlinkAction expandSource = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				expandSourceFolders();
			}
		};

		HyperlinkAction newExtensionWizard = new HyperlinkAction() {
			public void linkActivated(IHyperlinkSegment link) {
				openNewExtensionWizard();
			}
		};
		text = factory.createFormEngine(parent);
		text.setHyperlinkSettings(factory.getHyperlinkHandler());
		IFile file = page.getTemplateFile();
		try {
			InputStream is = file.getContents(true);
			text.load(is, true);
			is.close();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} catch (IOException e) {
			PDEPlugin.logException(e);
		}

		// register hyperlink actions
		text.registerTextObject("ExtensionsPage", pageAction);
		text.registerTextObject("OverviewPage", pageAction);
		text.registerTextObject("action.run", debugAction);
		text.registerTextObject("action.debug", debugAction);
		text.registerTextObject("action.expandSource", expandSource);
		text.registerTextObject("action.newExtension", newExtensionWizard);

		// register images
		text.registerTextObject("pageImage", pageImage);
		text.registerTextObject("runImage", runImage);
		text.registerTextObject("debugImage", debugImage);
		text.registerTextObject("runTimeWorkbenchImage", runWorkbenchImage);
		TableData td = new TableData();
		td.grabHorizontal = true;
		text.setLayoutData(td);

		Composite separator = new Composite(parent, SWT.NULL);
		separator.setBackground(factory.getBorderColor());
		td = new TableData();
		td.heightHint = 1;
		td.align = TableData.FILL;
		separator.setLayoutData(td);

		dontShowCheck =
			factory.createButton(
				parent,
				PDEPlugin.getResourceString(KEY_DONT_SHOW),
				SWT.CHECK);
		dontShowCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				dontShow = dontShowCheck.getSelection();
			}
		});
		
		WorkbenchHelp.setHelp(parent, IHelpContextIds.MANIFEST_WELCOME);
	}

	public void dispose() {
		if (dontShow)
			deleteTemplateFile(page.getTemplateFile());
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void deleteTemplateFile(final IFile file) {
		if (file.exists()) {
			ProgressMonitorDialog pmd =
				new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
			try {
				pmd.run(false, false, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
						throws InvocationTargetException {
						try {
							file.delete(true, monitor);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						} finally {
							monitor.done();
						}
					}
				});
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			} catch (InterruptedException e) {
			}
		}
	}

	private void expandSourceFolders() {
		IPluginModelBase model =
			(IPluginModelBase) ((PDEFormPage) page).getModel();
		IProject project = model.getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IClasspathEntry[] entries = javaProject.getRawClasspath();
			ArrayList sourceFolders = new ArrayList();
			for (int i = 0; i < entries.length; i++) {
				IClasspathEntry entry = entries[i];
				if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
					IFolder folder = project.getFolder(entry.getPath());
					if (folder.exists())
						sourceFolders.add(folder);
				}
			}
			if (sourceFolders.size() > 0) {
				StructuredSelection selection =
					new StructuredSelection(sourceFolders.toArray());
				IWorkbenchPage page = PDEPlugin.getActivePage();
				IWorkbenchPart part = page.getActivePart();
				if (part instanceof ISetSelectionTarget) {
					((ISetSelectionTarget) part).selectReveal(selection);
				}
			}
		} catch (JavaModelException e) {
		}
	}
	public void setFocus() {
		if (text != null)
			text.setFocus();
	}

	private void openNewExtensionWizard() {
		ManifestEditor editor =
			(ManifestEditor) ((PDEFormPage) page).getEditor();
		ManifestExtensionsPage exPage =
			(ManifestExtensionsPage) editor.getPage(
				ManifestEditor.EXTENSIONS_PAGE);
		editor.showPage(exPage);
		exPage.openNewExtensionWizard();
	}

	public void initialize(Object model) {
		super.initialize(model);
		IPluginModelBase modelBase = (IPluginModelBase) model;
		IPluginBase plugin = modelBase.getPluginBase();
		setHeadingText(
			PDEPlugin.getFormattedMessage(
				KEY_HEADING,
				plugin.getTranslatedName()));
		((Composite) getControl()).layout(true);
		updateSize();
	}
}