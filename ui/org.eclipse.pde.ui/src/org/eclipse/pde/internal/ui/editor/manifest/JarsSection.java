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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.update.ui.forms.internal.*;

public class JarsSection
	extends TableSection
	implements IModelChangedListener {
	private FormWidgetFactory factory;
	private TableViewer entryTable;
	private Image entryImage;
	private IPluginLibrary currentLibrary;
	public static final String SECTION_TITLE = "ManifestEditor.JarsSection.title";
	public static final String SECTION_RTITLE = "ManifestEditor.JarsSection.rtitle";
	public static final String SECTION_DIALOG_TITLE =
		"ManifestEditor.JarsSection.dialogTitle";
	public static final String POPUP_NEW_FOLDER =
		"ManifestEditor.JarsSection.newFolder";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String SECTION_NEW = "ManifestEditor.JarsSection.new";
	public static final String SECTION_DESC = "ManifestEditor.JarsSection.desc";
	public static final String SOURCE_DIALOG_TITLE =
		"ManifestEditor.JarsSection.missingSource.title";
	public static final String SOURCE_DIALOG_MESSAGE =
		"ManifestEditor.JarsSection.missingSource.message";
	public static final String DUPLICATE_FOLDER_MESSAGE =
		"ManifestEditor.JarsSection.missingSource.duplicateFolder";

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IPluginLibrary) {
				IPluginLibrary library = (IPluginLibrary) parent;
				IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
				IBuildModel buildModel = model.getBuildModel();
				String libKey = IBuildEntry.JAR_PREFIX + library.getName();
				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
				if (entry != null) {
					return entry.getTokens();
				}
			}
			return new Object[0];
		}
	}

	class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return entryImage;
		}
	}

	public JarsSection(ManifestRuntimePage page) {
		super(page, new String[] { PDEPlugin.getResourceString(SECTION_NEW)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		initializeImages();
		Composite container = createClientContainer(parent, 2, factory);
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);
		TablePart tablePart = getTablePart();
		entryTable = tablePart.getTableViewer();
		entryTable.setContentProvider(new TableContentProvider());
		entryTable.setLabelProvider(new TableLabelProvider());
		factory.paintBordersFor(container);
		return container;
	}
	
	// defect 19550
	protected StructuredViewerPart createViewerPart(String [] buttonLabels) {
		EditableTablePart tablePart = (EditableTablePart)super.createViewerPart(buttonLabels);
		tablePart.setEditable(false);
		return tablePart;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(selection);
	}

	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		return false;
	}

	protected void fillContextMenu(IMenuManager manager) {
		IModel model = (IModel) getFormPage().getModel();

		ISelection selection = entryTable.getSelection();

		if (currentLibrary != null) {
			Action newAction = new Action(PDEPlugin.getResourceString(POPUP_NEW_FOLDER)) {
				public void run() {
					handleNew();
				}
			};
			newAction.setEnabled(model.isEditable());
			manager.add(newAction);
		}

		if (!selection.isEmpty()) {
			manager.add(new Separator());
			Action deleteAction = new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			};
			deleteAction.setEnabled(model.isEditable());
			manager.add(deleteAction);
		}
		manager.add(new Separator());
		// defect 19550
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager,false);
	}
	private void handleDelete() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IBuildModel buildModel = model.getBuildModel();
		if (buildModel.isEditable() == false)
			return;
		IFile file = (IFile) model.getUnderlyingResource();
		IProject project = file.getProject();
		Object object =
			((IStructuredSelection) entryTable.getSelection()).getFirstElement();
		if (object != null && object instanceof String) {
			String folderName = object.toString();
			IFolder folder = project.getFolder(folderName);
			removeIfOnBuildPath(project, folder);
			String libKey = IBuildEntry.JAR_PREFIX + currentLibrary.getName();
			IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
			if (entry != null) {
				try {
					entry.removeToken(object.toString());
					entryTable.remove(object);
					((WorkspaceBuildModel) buildModel).save();
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}
	private void handleNew() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IFile file = (IFile) model.getUnderlyingResource();
		IProject project = file.getProject();
		ContainerSelectionDialog dialog =
			new ContainerSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				project,
				true,
				PDEPlugin.getResourceString(SECTION_DIALOG_TITLE));
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				IPath path = (IPath) result[0];
				IPath projectPath = project.getFullPath();
				if (!projectPath.isPrefixOf(path))
					return;
				int matching = path.matchingFirstSegments(projectPath);
				path = path.removeFirstSegments(matching);
				String folder = path.toString();
				if (!verifyFolderExists(project, folder))
					return;
				try {
					if (!folder.endsWith("/"))
						folder = folder + "/";
					IBuildModel buildModel = model.getBuildModel();
					String libKey = IBuildEntry.JAR_PREFIX + currentLibrary.getName();
					IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
					if (entry == null) {
						entry = buildModel.getFactory().createEntry(libKey);
						buildModel.getBuild().add(entry);
					} else {
						if (entry.contains(folder)) {
							String message =
								PDEPlugin.getFormattedMessage(DUPLICATE_FOLDER_MESSAGE, folder);
							MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),"", message);
							return;
						}
					}
					entry.addToken(folder);
					entryTable.add(folder);
					((WorkspaceBuildModel) buildModel).save();
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}
	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		IBuildModel buildModel = model.getBuildModel();
		boolean editable = model.isEditable() && buildModel.isEditable();
		setReadOnly(!editable);
		getTablePart().setButtonEnabled(0, false);
		model.addModelChangedListener(this);
		if (buildModel.isEditable() == false) {
			String header = getHeaderText();
			setHeaderText(PDEPlugin.getFormattedMessage(SECTION_RTITLE, header));
		}
	}
	private void initializeImages() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		ISharedImages sharedImages = workbench.getSharedImages();
		entryImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
	}
	public void modelChanged(IModelChangedEvent event) {
	}
	public void sectionChanged(
		FormSection source,
		int changeType,
		Object changeObject) {
		IPluginLibrary library = (IPluginLibrary) changeObject;
		update(library);
	}
	public void setFocus() {
		entryTable.getTable().setFocus();
	}
	private void update(IPluginLibrary library) {
		currentLibrary = library;
		entryTable.setInput(currentLibrary);
		getTablePart().setButtonEnabled(0, !isReadOnly() && library != null);
	}
	private boolean verifyFolderExists(IProject project, String folderName) {
		IPath path = project.getFullPath().append(folderName);
		IFolder folder = project.getWorkspace().getRoot().getFolder(path);
		if (folder.exists() == false) {
			boolean result =
				MessageDialog.openQuestion(
					PDEPlugin.getActiveWorkbenchShell(),
					PDEPlugin.getResourceString(SOURCE_DIALOG_TITLE),
					PDEPlugin.getFormattedMessage(
						SOURCE_DIALOG_MESSAGE,
						folder.getFullPath().toString()));
			if (result) {
				try {
					folder.create(false, true, null);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
					return false;
				}
			} else
				return false;
		}
		return addIfNotOnBuildPath(project, folder);
	}
	private boolean addIfNotOnBuildPath(IProject project, IFolder folder) {
		final IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] entries = null;
		try {
			entries = javaProject.getRawClasspath();
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
			return false;
		}
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath path = entry.getPath();
				if (path.equals(folder.getFullPath())) {
					// found
					return true;
				}
			}
		}
		// it is not, so add it
		IClasspathEntry sourceEntry = JavaCore.newSourceEntry(folder.getFullPath().makeAbsolute());
		final IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = sourceEntry;
		return executeNewClasspathOperation(javaProject, newEntries);
	}
	private boolean removeIfOnBuildPath(IProject project, IFolder folder) {
		final IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] entries = null;
		try {
			entries = javaProject.getRawClasspath();
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
			return false;
		}
		ArrayList newList = new ArrayList();
		boolean found = false;
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath path = entry.getPath();
				if (path.equals(folder.getFullPath())) {
					// found
					found = true;
					continue;
				}
			}
			newList.add(entry);
		}
		if (!found)
			return false;
		// it is, so remove it
		IClasspathEntry[] newEntries =
			(IClasspathEntry[]) newList.toArray(new IClasspathEntry[newList.size()]);
		return executeNewClasspathOperation(javaProject, newEntries);
	}

	private boolean executeNewClasspathOperation(
		final IJavaProject javaProject,
		final IClasspathEntry[] newEntries) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					javaProject.setRawClasspath(newEntries, monitor);
				} catch (JavaModelException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		ProgressMonitorDialog pmd =
			new ProgressMonitorDialog(entryTable.getTable().getShell());
		try {
			pmd.run(true, false, op);
			return true;
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		}
	}
}
