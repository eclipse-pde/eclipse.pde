/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class LibraryPluginJarsPage extends WizardPage {

	protected LibraryPluginFieldData fData;

	/**
	 * List of IFile and File of workspace and external Jars.
	 */
	protected ArrayList fJarPaths = new ArrayList();

	protected Button fRemove;

	protected TableViewer fTableViewer;

	public LibraryPluginJarsPage(String pageName, LibraryPluginFieldData data, Collection jarPaths) {
		super(pageName);
		fData = data;
		setTitle(PDEUIMessages.LibraryPluginJarsPage_title);
		setDescription(PDEUIMessages.LibraryPluginJarsPage_desc);
		if (jarPaths != null)
			fJarPaths.addAll(jarPaths);
	}

	private void chooseFile() {
		FileDialog dialog = new FileDialog(getShell(), SWT.OPEN | SWT.MULTI);
		dialog.setFilterExtensions(new String[] {"*.jar"}); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			String path = new File(res).getParent();
			String[] fileNames = dialog.getFileNames();
			for (int i = 0; i < fileNames.length; i++) {
				File newJarFile = new File(path, fileNames[i]);
				removeJar(fileNames[i]);
				fJarPaths.add(newJarFile);
				fTableViewer.add(newJarFile);
			}
			fRemove.setEnabled(fJarPaths.size() > 0);
			setPageComplete(fJarPaths.size() > 0);
		}
	}

	private void chooseWorkspaceFile() {
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider());
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(true);
		dialog.setTitle(PDEUIMessages.LibraryPluginJarsPage_SelectionDialog_title);
		dialog.setMessage(PDEUIMessages.LibraryPluginJarsPage_SelectionDialog_message);
		dialog.addFilter(new FileExtensionFilter("jar")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.JAR_SELECTION);

		if (dialog.open() == Window.OK) {
			Object[] files = dialog.getResult();
			for (int i = 0; i < files.length; i++) {
				IFile newJarFile = (IFile) files[i];
				removeJar(newJarFile.getName());
				fJarPaths.add(newJarFile);
				fTableViewer.add(newJarFile);
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
		l.setText(PDEUIMessages.LibraryPluginJarsPage_label);
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
			public String getText(Object obj) {
				String name;
				String location;
				if (obj instanceof IFile) {
					IFile jarFile = (IFile) obj;
					name = jarFile.getName();
					location = jarFile.getParent().getFullPath().toString().substring(1);
				} else {
					File jarFile = (File) obj;
					name = jarFile.getName();
					location = jarFile.getParent();
				}
				return name + " - " + location; //$NON-NLS-1$

			}

			public Image getImage(Object obj) {
				if (obj instanceof IFile) {
					return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_JAR_OBJ);
				}
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_JAR_LIB_OBJ);
			}
		});
		// should not sort, bug 98401
		//fTableViewer.setSorter(new ViewerSorter()); 
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
		browseWorkspace.setText(PDEUIMessages.LibraryPluginJarsPage_add);
		browseWorkspace.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(browseWorkspace);
		browseWorkspace.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseWorkspaceFile();
			}
		});

		Button browseFile = new Button(buttons, SWT.PUSH);
		browseFile.setText(PDEUIMessages.LibraryPluginJarsPage_addExternal);
		browseFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		SWTUtil.setButtonDimensionHint(browseFile);
		browseFile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				chooseFile();
			}
		});

		fRemove = new Button(buttons, SWT.PUSH);
		fRemove.setText(PDEUIMessages.LibraryPluginJarsPage_remove);
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
		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.NEW_LIBRARY_PROJECT_JAR_PAGE);
		setControl(control);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.LIBRARY_PLUGIN_JARS);
	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
		if (!selection.isEmpty()) {
			for (Iterator it = selection.iterator(); it.hasNext();) {
				Object file = it.next();
				fJarPaths.remove(file);
				fTableViewer.remove(file);
			}
			fRemove.setEnabled(fJarPaths.size() > 0);
			setPageComplete(fJarPaths.size() > 0);
		}
	}

	public boolean isPageComplete() {
		return fJarPaths.size() > 0;
	}

	private void removeJar(String fileName) {
		for (int i = 0; i < fJarPaths.size(); i++) {
			String name;
			if (fJarPaths.get(i) instanceof IFile) {
				IFile jarFile = (IFile) fJarPaths.get(i);
				name = jarFile.getName();
			} else {
				File jarFile = (File) fJarPaths.get(i);
				name = jarFile.getName();
			}
			if (name.equals(fileName)) {
				Object jarPath = fJarPaths.get(i);
				fJarPaths.remove(jarPath);
				fTableViewer.remove(jarPath);
			}
		}
	}

	public void updateData() {
		String[] jarPaths = new String[fJarPaths.size()];
		for (int i = 0; i < fJarPaths.size(); i++) {
			if (fJarPaths.get(i) instanceof IFile) {
				IFile jarFile = (IFile) fJarPaths.get(i);
				jarPaths[i] = jarFile.getLocation().toString();
			} else {
				File jarFile = (File) fJarPaths.get(i);
				jarPaths[i] = jarFile.toString();

			}
		}
		fData.setLibraryPaths(jarPaths);
	}
}
