/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class LibraryPluginJarsPage extends WizardPage {

	protected LibraryPluginFieldData fData;

	protected ArrayList fJarPaths = new ArrayList();

	protected Button fRemove;

	protected TableViewer fTableViewer;

	public LibraryPluginJarsPage(String pageName, LibraryPluginFieldData data) {
		super(pageName);
		fData = data;
		setTitle(PDEPlugin.getResourceString("LibraryPluginJarsPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin
				.getResourceString("LibraryPluginJarsPage.desc")); //$NON-NLS-1$
	}

	private void addJarPath(String jarPath) {
		String jarFileName = new File(jarPath).getName();
		for (int i = 0; i < fJarPaths.size(); i++) {
			String jarFile = (String) fJarPaths.get(i);
			String fileName = new File(jarFile).getName();
			if (fileName.equals(jarFileName)) {
				fJarPaths.remove(jarFile);
				fTableViewer.remove(jarFile);
			}
		}
		fJarPaths.add(jarPath);
		fTableViewer.add(jarPath);
	}

	private void chooseFile() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] { "*.jar" }); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			String path = new File(res).getParent();
			String[] fileNames = dialog.getFileNames();
			for (int i = 0; i < fileNames.length; i++) {
				String jarPath = path + File.separator + fileNames[i];
				addJarPath(jarPath);
			}
			fRemove.setEnabled(fJarPaths.size() > 0);
			setPageComplete(fJarPaths.size() > 0);
		}
	}

	private void chooseWorkspaceFile() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
				getShell(), new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());

		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(true);
		dialog.setTitle(PDEPlugin.getResourceString("LibraryPluginJarsPage.SelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("LibraryPluginJarsPage.SelectionDialog.message")); //$NON-NLS-1$
		dialog.addFilter(new FileExtensionFilter("jar")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			for (int i = 0; i < files.length; i++) {
				IFile file = (IFile) files[i];
				String path = file.getLocation().toString();
				addJarPath(path);
			}
			fRemove.setEnabled(fJarPaths.size() > 0);
			setPageComplete(fJarPaths.size() > 0);
		}
	}

	public void createControl(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		// layout.verticalSpacing = 10;
		control.setLayout(layout);

		Label l = new Label(control, SWT.WRAP);
		l.setText(PDEPlugin.getResourceString("LibraryPluginJarsPage.label")); //$NON-NLS-1$
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		l.setLayoutData(data);
		fTableViewer = new TableViewer(control, SWT.MULTI | SWT.BORDER);
		fTableViewer.setContentProvider(new DefaultTableProvider() {
			public Object[] getElements(Object inputElement) {
				return fJarPaths.toArray();
			}
		});
		fTableViewer.setLabelProvider(new LabelProvider() {
			public Image getImage(Object obj) {
				return PDEPlugin.getDefault().getLabelProvider().get(
						PDEPluginImages.DESC_JAVA_LIB_OBJ);
			}
		});
		fTableViewer.setSorter(new ViewerSorter());
		data = new GridData(GridData.FILL_BOTH);
		fTableViewer.getControl().setLayoutData(data);
		fTableViewer.setInput(fJarPaths);
		fTableViewer.getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				if (event.character == SWT.DEL && event.stateMask == 0) {
					handleRemove();
				}
			}
		});

		Composite buttons = new Composite(control, SWT.NONE);
		layout = new GridLayout();
		layout.verticalSpacing = 5;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);
		data = new GridData(GridData.FILL_VERTICAL);
		data.grabExcessVerticalSpace = true;
		buttons.setLayoutData(data);

		Button browseWorkspace = new Button(buttons, SWT.PUSH);
		browseWorkspace.setText(PDEPlugin
				.getResourceString("LibraryPluginJarsPage.add")); //$NON-NLS-1$
		browseWorkspace.setLayoutData(new GridData(
				GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(browseWorkspace);
		browseWorkspace.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseWorkspaceFile(); //$NON-NLS-1$
			}
		});

		Button browseFile = new Button(buttons, SWT.PUSH);
		browseFile.setText(PDEPlugin
				.getResourceString("LibraryPluginJarsPage.addExternal")); //$NON-NLS-1$
		browseFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(browseFile);
		browseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile(); //$NON-NLS-1$
			}
		});

		fRemove = new Button(buttons, SWT.PUSH);
		fRemove.setText(PDEPlugin
				.getResourceString("LibraryPluginJarsPage.remove")); //$NON-NLS-1$
		fRemove.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(fRemove);
		fRemove.setEnabled(fJarPaths.size() > 0);
		setPageComplete(fJarPaths.size() > 0);
		fRemove.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});

		Dialog.applyDialogFont(control);
		WorkbenchHelp.setHelp(control,
				IHelpContextIds.NEW_LIBRARY_PROJECT_JAR_PAGE);
		setControl(control);
	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer
				.getSelection();
		if (!selection.isEmpty()) {
			for (Iterator it = selection.iterator(); it.hasNext();) {
				String s = (String) it.next();
				fJarPaths.remove(s);
				fTableViewer.remove(s);
			}
			fRemove.setEnabled(fJarPaths.size() > 0);
			setPageComplete(fJarPaths.size() > 0);
		}
	}

	public boolean isPageComplete() {
		return fJarPaths.size() > 0;
	}

	public void updateData() {
		fData.setLibraryPaths((String[]) fJarPaths.toArray(new String[fJarPaths
				.size()]));
	}
}
