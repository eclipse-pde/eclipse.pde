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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.action.*;
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
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.update.ui.forms.internal.*;

public class JarsSection
	extends TableSection
	implements IModelChangedListener {
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
		
		class ContentProvider extends WorkbenchContentProvider {
			public boolean hasChildren(Object element) {
				Object[] children = getChildren(element);
				for (int i = 0; i < children.length; i++) {
					if (children[i] instanceof IFolder) {
						return true;
					}
				}
				return false;
			}
			
		
		}

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
			IAction renameAction = getRenameAction();
			renameAction.setEnabled(model.isEditable());
			manager.add(renameAction);
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
		Object object =
			((IStructuredSelection) entryTable.getSelection()).getFirstElement();
		if (object != null && object instanceof String) {
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
		final IProject project = file.getProject();
		FolderSelectionDialog dialog =
			new FolderSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(),
				new ContentProvider() {
		});
		dialog.setInput(project.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject) {
					return ((IProject)element).equals(project);
				}
				return element instanceof IFolder;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString("ManifestEditor.JarsSection.dialogTitle"));
		dialog.setMessage(PDEPlugin.getResourceString("ManifestEditor.JarsSection.dialogMessage"));
		
		
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				if (selection == null
					|| selection.length != 1
					|| !(selection[0] instanceof IFolder))
					return new Status(
						IStatus.ERROR,
						PDEPlugin.getPluginId(),
						IStatus.ERROR,
						"",
						null);
						
				IBuildModel buildModel = getBuildModel();
				String libKey = IBuildEntry.JAR_PREFIX + currentLibrary.getName();
				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
				
				String folderPath =
					((IFolder)selection[0]).getProjectRelativePath().addTrailingSeparator().toString();
				
				if (entry != null && entry.contains(folderPath))
					return new Status(
						IStatus.ERROR,
						PDEPlugin.getPluginId(),
						IStatus.ERROR,
						PDEPlugin.getResourceString("ManifestEditor.JarsSection.missingSource.duplicateFolder"),
						null);
						
				return new Status(
					IStatus.OK,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					"",
					null);
			}
		});
		
		if (dialog.open() == FolderSelectionDialog.OK) {
			try {
				IFolder folder = (IFolder) dialog.getFirstResult();
				String folderPath =
					folder.getProjectRelativePath().addTrailingSeparator().toString();
				IBuildModel buildModel = getBuildModel();
				String libKey = IBuildEntry.JAR_PREFIX + currentLibrary.getName();
				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
				if (entry == null) {
					entry = buildModel.getFactory().createEntry(libKey);
					buildModel.getBuild().add(entry);
				}
				entry.addToken(folderPath);
				entryTable.add(folderPath);
				((WorkspaceBuildModel) buildModel).save();
			} catch (CoreException e) {
			}
		}
	}
	
	private IBuildModel getBuildModel() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		return model.getBuildModel();
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

}
