package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.pde.model.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.elements.*;
import java.util.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jface.window.*;
import org.eclipse.pde.model.build.*;
import org.eclipse.pde.internal.model.build.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.internal.parts.TablePart;

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
		if (!(getFormPage().getModel() instanceof IEditable))
			return;
		ISelection selection = entryTable.getSelection();

		if (currentLibrary != null) {
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_FOLDER)) {
				public void run() {
					handleNew();
				}
			});
		}

		if (!selection.isEmpty()) {
			manager.add(new Separator());
			manager.add(new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			});
		}
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
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
					}
					else {
						if (entry.contains(folder)) {
							String message = PDEPlugin.getFormattedMessage(DUPLICATE_FOLDER_MESSAGE, folder);
							throw new CoreException(
								new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.OK, message, null));
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
		return verifyFolderIsOnBuildPath(project, folder);
	}
	private boolean verifyFolderIsOnBuildPath(IProject project, IFolder folder) {
		IJavaProject javaProject = JavaCore.create(project);
		try {
			IClasspathEntry[] entries = javaProject.getRawClasspath();
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
			IClasspathEntry sourceEntry = JavaCore.newSourceEntry(folder.getFullPath());
			IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length] = sourceEntry;
			javaProject.setRawClasspath(newEntries, null);
			return true;
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
			return false;
		}
	}
}