package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jdt.core.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.model.jars.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.ui.*;
import org.eclipse.jface.resource.*;
import org.eclipse.core.resources.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.wizards.extension.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
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
import org.eclipse.pde.internal.base.model.build.*;
import org.eclipse.pde.internal.model.build.*;
import org.eclipse.jface.dialogs.*;

public class JarsSection
	extends PDEFormSection
	implements IModelChangedListener {
	private FormWidgetFactory factory;
	private TableViewer entryTable;
	private Button newButton;
	private Image entryImage;
	private IPluginLibrary currentLibrary;
	public static final String SECTION_TITLE = "ManifestEditor.JarsSection.title";
	public static final String SECTION_RTITLE = "ManifestEditor.JarsSection.rtitle";
	public static final String SECTION_DIALOG_TITLE = "ManifestEditor.JarsSection.dialogTitle";
	public static final String POPUP_NEW_FOLDER = "ManifestEditor.JarsSection.newFolder";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String SECTION_NEW = "ManifestEditor.JarsSection.new";
	public static final String SECTION_DESC = "ManifestEditor.JarsSection.desc";
	public static final String SOURCE_DIALOG_TITLE = "ManifestEditor.JarsSection.missingSource.title";
	public static final String SOURCE_DIALOG_MESSAGE = "ManifestEditor.JarsSection.missingSource.message";

	class NameModifier implements ICellModifier {
		public boolean canModify(Object object, String property) {
			return true;
		}
		public void modify(Object object, String property, Object value) {
			Item item = (Item) object;
			final String entry = (String) item.getData();
			IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
			IBuildModel buildModel = model.getBuildModel();
			String buildKey = IBuildEntry.JAR_PREFIX + currentLibrary.getName();
			IBuildEntry buildEntry = buildModel.getBuild().getEntry(buildKey);

			try {
				buildEntry.renameToken(entry, value.toString());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}

			entryTable.getTable().getDisplay().asyncExec(new Runnable() {
				public void run() {
					entryTable.update(entry, null);
				}
			});
			((WorkspaceBuildModel) buildModel).save();
		}
		public Object getValue(Object object, String property) {
			return object.toString();
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
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	setDescription(PDEPlugin.getResourceString(SECTION_DESC));
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	this.factory = factory;
	initializeImages();
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;

	container.setLayout(layout);
	final Table table = factory.createTable(container, SWT.FULL_SELECTION);
	TableLayout tlayout = new TableLayout();

	TableColumn tableColumn = new TableColumn(table, SWT.NULL);
	tableColumn.setText("Point Name");
	ColumnLayoutData cLayout = new ColumnWeightData(100, true);
	tlayout.addColumnData(cLayout);

	//table.setLinesVisible(true);
	//table.setHeaderVisible(true);
	table.setLayout(tlayout);

	MenuManager popupMenuManager = new MenuManager();
	IMenuListener listener = new IMenuListener() {
		public void menuAboutToShow(IMenuManager mng) {
			fillContextMenu(mng);
		}
	};
	popupMenuManager.addMenuListener(listener);
	popupMenuManager.setRemoveAllWhenShown(true);
	Menu menu = popupMenuManager.createContextMenu(table);
	table.setMenu(menu);

	entryTable = new TableViewer(table);
	entryTable.setContentProvider(new TableContentProvider());
	entryTable.setLabelProvider(new TableLabelProvider());
	factory.paintBordersFor(container);
/*
	if (getFormPage().getModel() instanceof IEditable) {

		CellEditor[] editors = new CellEditor[] { new ModifiedTextCellEditor(table)};
		String[] properties = { "name" };
		entryTable.setCellEditors(editors);
		entryTable.setCellModifier(new NameModifier());
		entryTable.setColumnProperties(properties);
	}
*/

	entryTable.addSelectionChangedListener(new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			Object item = ((IStructuredSelection) event.getSelection()).getFirstElement();
			fireSelectionNotification(item);
			getFormPage().setSelection(event.getSelection());
		}
	});

	GridData gd = new GridData(GridData.FILL_BOTH);
	table.setLayoutData(gd);

	Composite buttonContainer = factory.createComposite(container);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	buttonContainer.setLayout(layout);

	newButton = factory.createButton(buttonContainer, PDEPlugin.getResourceString(SECTION_NEW), SWT.PUSH);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.verticalAlignment = GridData.BEGINNING;
	newButton.setLayoutData(gd);
	newButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			handleNew();
		}
	});
	return container;
}
public void dispose() {
	IPluginModelBase model = (IPluginModelBase)getFormPage().getModel();
	model.removeModelChangedListener(this);
}
public void doGlobalAction(String actionId) {
	if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
		handleDelete();
	}
}
private void fillContextMenu(IMenuManager manager) {
	if (!(getFormPage().getModel() instanceof IEditable)) return;
	ISelection selection = entryTable.getSelection();

	manager.add(new Action(PDEPlugin.getResourceString(POPUP_NEW_FOLDER)) {
		public void run() {
			handleNew();
		}
	});

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
			String folder = path.lastSegment();
			verifyFolderExists(project, folder);
			try {
				IBuildModel buildModel = model.getBuildModel();
				String libKey = IBuildEntry.JAR_PREFIX + currentLibrary.getName();
				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
				if (entry == null) {
					entry = buildModel.getFactory().createEntry(libKey);
					buildModel.getBuild().add(entry);
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
	IPluginModelBase model = (IPluginModelBase)input;
	IBuildModel buildModel = model.getBuildModel();
	boolean editable = model.isEditable() && buildModel.isEditable();
	setReadOnly(!editable);
	newButton.setEnabled(editable);
	model.addModelChangedListener(this);
	if (buildModel.isEditable()==false) {
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
}
private void verifyFolderExists(IProject project, String folderName) {
	IPath path = project.getFullPath().append(folderName);
	IFolder folder = project.getWorkspace().getRoot().getFolder(path);
	if (folder.exists() == false) {
		boolean result =
			MessageDialog.openQuestion(
				PDEPlugin.getActiveWorkbenchShell(),
				PDEPlugin.getResourceString(SOURCE_DIALOG_TITLE),
				PDEPlugin.getFormattedMessage(SOURCE_DIALOG_MESSAGE, folder.getFullPath().toString()));
		if (result) {
			try {
				folder.create(false, true, null);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	verifyFolderIsOnBuildPath(project, folder);
}
private void verifyFolderIsOnBuildPath(IProject project, IFolder folder) {
	IJavaProject javaProject = JavaCore.create(project);
	try {
		IClasspathEntry[] entries = javaProject.getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			IClasspathEntry entry = entries[i];
			if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				IPath path = entry.getPath();
				if (path.equals(folder.getFullPath())) {
					// found
					return;
				}
			}
		}
		// it is not, so add it
		IClasspathEntry sourceEntry = JavaCore.newSourceEntry(folder.getFullPath());
		IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);
		newEntries[entries.length] = sourceEntry;
		javaProject.setRawClasspath(newEntries, null);
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	}
}
}
