/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;

public class RuntimeInfoSection extends PDESection
		implements
			IModelChangedListener {

	public static final String SECTION_TITLE = "BuildEditor.RuntimeInfoSection.title";
	public static final String SECTION_DESC = "BuildEditor.RuntimeInfoSection.desc";
	public static final String SECTION_NEW = "BuildEditor.RuntimeInfoSection.addLibrary";
	public static final String SECTION_UP = "ManifestEditor.LibrarySection.up";
	public static final String SECTION_DOWN = "ManifestEditor.LibrarySection.down";
	public static final String POPUP_NEW_LIBRARY = "BuildEditor.RuntimeInfoSection.popupAdd";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String JSECTION_NEW = "BuildEditor.RuntimeInfoSection.addFolder";
	public static final String POPUP_NEW_FOLDER = "BuildEditor.RuntimeInfoSection.popupFolder";
	public static final String JAR_INCLUDE = "BuildEditor.RuntimeInfoSection.buildInclude";
	protected TableViewer fLibraryViewer;
	protected TableViewer fFolderViewer;

	protected StructuredViewerPart fLibraryPart;
	protected StructuredViewerPart fFolderPart;
	private IBuildEntry fCurrentLibrary;
	private IStructuredSelection fCurrentSelection;

	private Button fIncludeLibraryButton;
	private boolean fEnabled = true;

	class RenameAction extends Action {

		public RenameAction() {
			super(PDEPlugin.getResourceString("EditableTablePart.renameAction")); //$NON-NLS-1$
		}

		public void run() {
			doRename();
		}
	}

	class PartAdapter extends TablePart {

		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}

		public void selectionChanged(IStructuredSelection selection) {
			if (selection.size() != 0)
				RuntimeInfoSection.this.selectionChanged(selection);
		}

		public void handleDoubleClick(IStructuredSelection selection) {
			RuntimeInfoSection.this.handleDoubleClick(selection);
		}

		public void buttonSelected(Button button, int index) {
			if (getViewer() == fLibraryPart.getViewer()) {
				switch (index) {
					case 0 :
						handleNew();
						break;
					case 2 :
						handleUp();
						break;
					case 3 :
						handleDown();
						break;
				}
			} else if (getViewer() == fFolderPart.getViewer()) {
				if (index == 0)
					handleNewFolder();
			} else {
				button.getShell().setDefaultButton(null);
			}
		}
	}

	public class LibraryContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {

		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel) parent).getBuild();
				IBuildEntry jarOrderEntry = build
						.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
				IBuildEntry[] libraries = BuildUtil.getBuildLibraries(build
						.getBuildEntries());
				if (jarOrderEntry == null) {
					return libraries;
				}

				Vector libList = new Vector();
				String[] tokens = jarOrderEntry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IBuildEntry entry = build.getEntry(IBuildEntry.JAR_PREFIX
							+ tokens[i]);
					if (entry != null)
						libList.add(entry);
				}
				for (int i = 0; i < libraries.length; i++) {
					if (!libList.contains(libraries[i]))
						libList.add(libraries[i]);
				}
				return (IBuildEntry[]) libList.toArray(new IBuildEntry[libList
						.size()]);
			}
			return new Object[0];
		}
	}

	public class LibraryLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {

		public String getColumnText(Object obj, int index) {
			String name = ((IBuildEntry) obj).getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX))
				return name.substring(IBuildEntry.JAR_PREFIX.length());
			return name;
		}

		public Image getColumnImage(Object obj, int index) {
			PDELabelProvider provider = PDEPlugin.getDefault()
					.getLabelProvider();
			return provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
		}
	}

	class JarsNewContentProvider extends WorkbenchContentProvider {

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

	public class FolderContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {

		public Object[] getElements(Object parent) {
			return (parent instanceof IBuildEntry) ? ((IBuildEntry) parent)
					.getTokens() : new Object[0];
		}
	}

	public class FolderLabelProvider extends LabelProvider
			implements
				ITableLabelProvider {

		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			ISharedImages sharedImages = PlatformUI.getWorkbench()
					.getSharedImages();
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
	}

	public RuntimeInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getBuildModel().addModelChangedListener(this);
		createClient(getSection(), page.getManagedForm().getToolkit());
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
				.findContext(BuildInputContext.CONTEXT_ID);
		return (IBuildModel) context.getModel();
	}

	protected void handleLibInBinBuild(boolean isSelected, String libName) {
		IBuildModel model = getBuildModel();
		IBuildEntry binIncl = model.getBuild().getEntry(
				IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IProject project = model.getUnderlyingResource().getProject();
		IPath libPath = project.getFile(libName).getProjectRelativePath();

		try {
			if (binIncl == null && !isSelected)
				return;
			if (binIncl == null) {
				binIncl = model.getFactory().createEntry(
						IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
				model.getBuild().add(binIncl);
			}
			if (!isSelected && libPath.segmentCount() == 1
					&& binIncl.contains("*.jar")) {
				addAllJarsToBinIncludes(binIncl, project, model);
			}
			if (isSelected && !binIncl.contains(libName)) {
				binIncl.addToken(libName);
			} else if (!isSelected && binIncl.contains(libName)) {
				binIncl.removeToken(libName);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	protected void addAllJarsToBinIncludes(IBuildEntry binIncl,
			IProject project, IBuildModel model) {
		try {
			IResource[] members = project.members();
			for (int i = 0; i < members.length; i++) {
				if (!(members[i] instanceof IFolder)
						&& members[i].getFileExtension().equals("jar")) {
					binIncl.addToken(members[i].getName());
				}
			}

			IBuildEntry[] libraries = BuildUtil.getBuildLibraries(model
					.getBuild().getBuildEntries());
			if (libraries.length != 0) {
				for (int j = 0; j < libraries.length; j++) {
					String libraryName = libraries[j].getName().substring(7);
					IPath path = project.getFile(libraryName)
							.getProjectRelativePath();
					if (path.segmentCount() == 1
							&& !binIncl.contains(libraryName))
						binIncl.addToken(libraryName);
				}
			}
			binIncl.removeToken("*.jar");
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setOutputEntryTokens(Set outputFolders, IBuildEntry outputEntry) {
		Iterator iter = outputFolders.iterator();
		try {
			while (iter.hasNext()) {
				String outputFolder = iter.next().toString();
				if (!outputFolder.endsWith("" + Path.SEPARATOR))
					outputFolder = outputFolder.concat("" + Path.SEPARATOR);
				if (!outputEntry.contains(outputFolder.toString()))
					outputEntry.addToken(outputFolder.toString());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private IPackageFragmentRoot[] computeSourceFolders() {
		ArrayList folders = new ArrayList();
		IBuildModel buildModel = getBuildModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots = jProject
						.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE) {
						folders.add(roots[i]);
					}
				}
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return (IPackageFragmentRoot[]) folders
				.toArray(new IPackageFragmentRoot[folders.size()]);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		layout.makeColumnsEqualWidth = true;
		container.setLayout(layout);

		createLeftSection(container, toolkit);
		createRightSection(container, toolkit);

		fIncludeLibraryButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(JAR_INCLUDE), SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fIncludeLibraryButton.setLayoutData(gd);
		fIncludeLibraryButton.setVisible(false);
		fIncludeLibraryButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				handleLibInBinBuild(fIncludeLibraryButton.getSelection(),
						fCurrentLibrary.getName().substring(7));
			}
		});
		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	private void createLeftSection(Composite parent, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		layout.numColumns = 2;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		container.setLayoutData(gd);

		fLibraryPart = new PartAdapter(new String[]{
				PDEPlugin.getResourceString(SECTION_NEW), null,
				PDEPlugin.getResourceString(SECTION_UP),
				PDEPlugin.getResourceString(SECTION_DOWN)});
		fLibraryPart.createControl(container, SWT.FULL_SELECTION, 2, toolkit);
		fLibraryViewer = (TableViewer) fLibraryPart.getViewer();
		fLibraryViewer.setContentProvider(new LibraryContentProvider());
		fLibraryViewer.setLabelProvider(new LibraryLabelProvider());
		fLibraryPart.setButtonEnabled(2, false);
		fLibraryPart.setButtonEnabled(3, false);
		fLibraryViewer.setInput(getBuildModel());
		toolkit.paintBordersFor(container);

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				fillLibraryContextMenu(manager);
			}
		});

		fLibraryViewer.getControl().setMenu(
				menuMgr.createContextMenu(fLibraryViewer.getControl()));
	}

	private void createRightSection(Composite parent, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 2;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		container.setLayoutData(gd);

		fFolderPart = new PartAdapter(new String[]{PDEPlugin
				.getResourceString(JSECTION_NEW)});
		fFolderPart.createControl(container, SWT.FULL_SELECTION, 2, toolkit);
		fFolderViewer = (TableViewer) fFolderPart.getViewer();
		fFolderViewer.setContentProvider(new FolderContentProvider());
		fFolderViewer.setLabelProvider(new FolderLabelProvider());
		toolkit.paintBordersFor(container);

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {

			public void menuAboutToShow(IMenuManager manager) {
				fillFolderViewerContextMenu(manager);
			}
		});
		fFolderViewer.getControl().setMenu(
				menuMgr.createContextMenu(fFolderViewer.getControl()));
	}

	protected void fillFolderViewerContextMenu(IMenuManager manager) {
		ISelection selection = fFolderViewer.getSelection();
		if (fCurrentLibrary != null) {
			Action newAction = new Action(PDEPlugin
					.getResourceString(POPUP_NEW_FOLDER)) {

				public void run() {
					handleNewFolder();
				}
			};
			newAction.setEnabled(fEnabled);
			manager.add(newAction);
		}

		manager.add(new Separator());
		Action deleteAction = new Action(PDEPlugin
				.getResourceString(POPUP_DELETE)) {

			public void run() {
				handleDeleteFolder();
			}
		};
		deleteAction.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(deleteAction);

		// defect 19550
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager, false);

	}

	protected void fillLibraryContextMenu(IMenuManager manager) {
		ISelection selection = fLibraryViewer.getSelection();
		Action newAction = new Action(PDEPlugin
				.getResourceString(POPUP_NEW_LIBRARY)) {

			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(fEnabled);
		manager.add(newAction);

		manager.add(new Separator());
		IAction renameAction = new RenameAction();
		renameAction.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(renameAction);

		Action deleteAction = new Action(PDEPlugin
				.getResourceString(POPUP_DELETE)) {

			public void run() {
				handleDelete();
			}
		};
		deleteAction.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(deleteAction);

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
				manager, false);
	}

	protected void entryModified(IBuildEntry oldEntry, String newValue) {
		final IBuildEntry entry = oldEntry;
		IBuildModel buildModel = getBuildModel();
		IBuild build = buildModel.getBuild();
		String oldName = entry.getName().substring(7);

		try {
			if (newValue.equals(entry.getName()))
				return;
			if (!newValue.startsWith(IBuildEntry.JAR_PREFIX))
				newValue = IBuildEntry.JAR_PREFIX + newValue;
			if (!newValue.endsWith(".jar"))
				newValue = newValue + ".jar";

			// jars.compile.order
			IBuildEntry tempEntry = build
					.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
			if (tempEntry != null && tempEntry.contains(oldName)){
				tempEntry.renameToken(oldName, newValue.substring(7));
			}

			// output.{source folder}.jar
			tempEntry = build
					.getEntry(IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX
							+ oldName);
			if (tempEntry != null) {
				tempEntry
						.setName(IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX
								+ newValue.substring(7));
				
			}
			// bin.includes
			tempEntry = build
					.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
			if (tempEntry != null && tempEntry.contains(oldName)){
				tempEntry.renameToken(oldName, newValue.substring(7));
			}

			// bin.excludes
			tempEntry = build
					.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
			if (tempEntry != null && tempEntry.contains(oldName)){
				tempEntry.renameToken(oldName, newValue.substring(7));
				
			}

			// rename
			entry.setName(newValue);
			
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void expandTo(Object object) {
		fLibraryViewer.setSelection(new StructuredSelection(object), true);
	}

	public void handleDoubleClick(IStructuredSelection selection) {
		doRename();
	}

	public void enableSection(boolean enable) {
		fEnabled = enable;
		fLibraryPart.setButtonEnabled(0, enable);
		fLibraryPart.setButtonEnabled(2, false);
		fLibraryPart.setButtonEnabled(3, false);
		fIncludeLibraryButton.setEnabled(enable);

		fFolderPart.setButtonEnabled(0, enable
				&& !fLibraryViewer.getSelection().isEmpty());
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			if (fEnabled) {
				if (fLibraryViewer.getControl().isFocusControl()) {
					handleDelete();
				} else {
					handleDeleteFolder();
				}
			}
			return true;
		}
		return false;
	}

	private void doRename() {
		IStructuredSelection selection = (IStructuredSelection) fLibraryViewer
				.getSelection();
		if (selection.size() == 1) {
			IBuildEntry entry = (IBuildEntry) selection.getFirstElement();
			String oldName = entry.getName().substring(7);
			RenameDialog dialog = new RenameDialog(fLibraryViewer.getControl()
					.getShell(), oldName);
			dialog.create();
			dialog
					.getShell()
					.setText(
							PDEPlugin
									.getResourceString("EditableTablePart.renameTitle")); //$NON-NLS-1$
			dialog.getShell().setSize(300, 150);
			if (dialog.open() == Dialog.OK) {
				entryModified(entry, dialog.getNewName());
			}
		}
	}

	public void dispose() {
		getBuildModel().removeModelChangedListener(this);
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void refreshOutputKeys() {
		if (!isJavaProject())
			return;
		IPackageFragmentRoot[] sourceFolders = computeSourceFolders();

		String[] jarFolders = fCurrentLibrary.getTokens();
		IPackageFragmentRoot sourceFolder;
		IClasspathEntry entry;
		IPath outputPath;
		Set outputFolders;

		try {
			outputFolders = new HashSet();
			for (int j = 0; j < jarFolders.length; j++) {
				sourceFolder = getSourceFolder(jarFolders[j], sourceFolders);
				if (sourceFolder != null) {
					entry = sourceFolder.getRawClasspathEntry();
					outputPath = entry.getOutputLocation();
					if (outputPath == null) {
						outputFolders.add("bin");
					} else {
						outputPath = outputPath.removeFirstSegments(1);
						outputFolders.add(outputPath.toString());
					}
				}
			}
			if (outputFolders.size() != 0) {
				String libName = fCurrentLibrary.getName().substring(7);
				IBuildModel buildModel = getBuildModel();
				IBuild build = buildModel.getBuild();
				String outputName = IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX
						+ libName;

				IBuildEntry outputEntry = build.getEntry(outputName);

				if (outputEntry == null) {
					outputEntry = buildModel.getFactory().createEntry(
							outputName);
					build.add(outputEntry);
				}
				setOutputEntryTokens(outputFolders, outputEntry);

			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	private boolean isJavaProject() {
		try {
			IBuildModel buildModel = getBuildModel();
			IProject project = buildModel.getUnderlyingResource().getProject();
			return project.hasNature(JavaCore.NATURE_ID);
		} catch (CoreException e) {
		}
		return false;
	}

	private boolean isReadOnly() {
		IBuildModel model = getBuildModel();
		if (model instanceof IEditable)
			return !((IEditable) model).isEditable();
		return true;
	}

	private void update(IBuildEntry variable) {
		fCurrentLibrary = variable;
		fFolderViewer.setInput(fCurrentLibrary);
		fFolderPart.setButtonEnabled(0, !isReadOnly() && fEnabled
				&& variable != null);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getPage().getPDEEditor().setSelection(selection);
		if (item instanceof IBuildEntry) {
			update((IBuildEntry) item);
			updateDirectionalButtons();
			fIncludeLibraryButton.setVisible(true);
			String name = ((IBuildEntry) item).getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX))
				name = name.substring(IBuildEntry.JAR_PREFIX.length());
			fIncludeLibraryButton.setSelection(isJarIncluded(name));
		}
	}

	protected void updateDirectionalButtons() {
		Table table = fLibraryViewer.getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		boolean canMove = table.getItemCount() > 1;
		fLibraryPart.setButtonEnabled(2, canMove && hasSelection
				&& table.getSelectionIndex() > 0);
		fLibraryPart.setButtonEnabled(3, canMove && hasSelection
				&& table.getSelectionIndex() < table.getItemCount() - 1);
	}

	private boolean isJarIncluded(String libName) {
		IBuildModel model = getBuildModel();
		IProject project = model.getUnderlyingResource().getProject();
		IPath libPath = project.getFile(libName).getProjectRelativePath();
		IBuildEntry binIncl = model.getBuild().getEntry(
				IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry binExcl = model.getBuild().getEntry(
				IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
		if (binIncl == null)
			return false;

		if (libPath.segmentCount() == 1) {
			return binIncl.contains(libName) || binIncl.contains("*.jar");
		} else if (binIncl.contains(libName)) {
			return true;
		} else if (binExcl != null && binExcl.contains(libName)) {
			return false;
		} else {
			return isParentIncluded(libPath, binIncl, binExcl);
		}
	}

	protected boolean isParentIncluded(IPath libPath, IBuildEntry binIncl,
			IBuildEntry binExcl) {
		while (libPath.segmentCount() > 1) {
			libPath = libPath.removeLastSegments(1);
			if (binIncl.contains(libPath.toString() + Path.SEPARATOR))
				return true;
			else if (binExcl != null
					&& binExcl.contains(libPath.toString() + Path.SEPARATOR))
				return false;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fLibraryViewer.refresh();
		fFolderViewer.refresh();
		fLibraryViewer.setSelection(null);
		fFolderViewer.setInput(null);
		fFolderPart.setButtonEnabled(0, false);
		fIncludeLibraryButton.setVisible(false);
		updateDirectionalButtons();
		super.refresh();
	}

	protected String[] getLibraryNames() {
		String[] libNames = new String[fLibraryViewer.getTable().getItemCount()];
		for (int i = 0; i < libNames.length; i++)
			libNames[i] = fLibraryViewer.getTable().getItem(i).getText();
		return libNames;
	}

	protected void handleNew() {
		final String[] libNames = getLibraryNames();
		IBaseModel pmodel = getPage().getModel();
		final IPluginModelBase pluginModelBase = (pmodel instanceof IPluginModelBase)
				? (IPluginModelBase) pmodel
				: null;

		BusyIndicator.showWhile(fLibraryViewer.getTable().getDisplay(),
				new Runnable() {

					public void run() {
						IBuildModel buildModel = getBuildModel();
						IBuild build = buildModel.getBuild();
						AddLibraryDialog dialog = new AddLibraryDialog(
								getSection().getShell(), libNames,
								pluginModelBase);
						dialog.create();
						dialog.getShell().setText("Add Entry"); //$NON-NLS-1$
						dialog.getShell().setSize(300, 350);

						try {
							if (dialog.open() == Dialog.OK) {

								String name = dialog.getNewName();

								if (!name.endsWith(".jar"))
									name = name + ".jar";

								String keyName = name;
								if (!keyName.startsWith(IBuildEntry.JAR_PREFIX))
									keyName = IBuildEntry.JAR_PREFIX + name;
								if (name.startsWith(IBuildEntry.JAR_PREFIX))
									name = name.substring(7);

								handleLibInBinBuild(true, name);

								// add library to jars compile order
								IBuildEntry jarOrderEntry = build
										.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
								if (jarOrderEntry == null) {
									jarOrderEntry = getBuildModel()
											.getFactory()
											.createEntry(
													IBuildPropertiesConstants.PROPERTY_JAR_ORDER);

									jarOrderEntry.addToken(name);
									build.add(jarOrderEntry);
								} else {
									jarOrderEntry.addToken(name);
								}
								// end of jars compile order addition

								IBuildEntry library = buildModel.getFactory()
										.createEntry(keyName);
								build.add(library);

							}
						} catch (CoreException e) {
							PDEPlugin.logException(e);
						}
					}
				});
	}

	private IPackageFragmentRoot getSourceFolder(String folderName,
			IPackageFragmentRoot[] sourceFolders) {
		for (int i = 0; i < sourceFolders.length; i++) {
			if (sourceFolders[i].getPath().removeFirstSegments(1).equals(
					new Path(folderName))) {
				return sourceFolders[i];
			}
		}
		return null;
	}

	protected void handleDelete() {
		int index = fLibraryViewer.getTable().getSelectionIndex();
		if (index != -1) {
			String libName = fLibraryViewer.getTable().getItem(index).getText();
			IBuild build = getBuildModel().getBuild();

			try {
				// jars.compile.order
				IBuildEntry entry = build
						.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
				if (entry != null) {
					entry.removeToken(libName);
				}
				// output.{source folder}.jar
				entry = build
						.getEntry(IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX
								+ libName);
				if (entry != null)
					build.remove(entry);

				// bin.includes
				entry = build
						.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
				if (entry != null && entry.contains(libName))
					entry.removeToken(libName);

				// bin.excludes
				entry = build
						.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
				if (entry != null && entry.contains(libName))
					entry.removeToken(libName);

				build.remove(build.getEntry(IBuildEntry.JAR_PREFIX + libName));
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private void handleDeleteFolder() {
		int index = fFolderViewer.getTable().getSelectionIndex();
		Object object = ((IStructuredSelection) fFolderViewer.getSelection())
				.getFirstElement();
		if (object != null) {
			String libKey = fCurrentLibrary.getName();
			IBuildEntry entry = getBuildModel().getBuild().getEntry(libKey);
			if (entry != null) {
				try {
					String[] tokens = entry.getTokens();
					if (tokens.length > index + 1)
						fCurrentSelection = new StructuredSelection(
								tokens[index + 1]);
					entry.removeToken((String) object);

				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private void handleNewFolder() {
		IFile file = (IFile) getBuildModel().getUnderlyingResource();
		final IProject project = file.getProject();

		FolderSelectionDialog dialog = new FolderSelectionDialog(PDEPlugin
				.getActiveWorkbenchShell(), new WorkbenchLabelProvider(),
				new JarsNewContentProvider() {
				});

		dialog.setInput(project.getWorkspace());
		dialog.addFilter(new ViewerFilter() {

			public boolean select(Viewer viewer, Object parentElement,
					Object element) {
				if (element instanceof IProject) {
					return ((IProject) element).equals(project);
				}
				return element instanceof IFolder;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin
				.getResourceString("ManifestEditor.JarsSection.dialogTitle"));
		dialog.setMessage(PDEPlugin
				.getResourceString("ManifestEditor.JarsSection.dialogMessage"));

		dialog.setValidator(new ISelectionStatusValidator() {

			public IStatus validate(Object[] selection) {
				if (selection == null || selection.length != 1
						|| !(selection[0] instanceof IFolder))
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
							IStatus.ERROR, "", null);

				String libKey = fCurrentLibrary.getName();

				IBuildEntry entry = getBuildModel().getBuild().getEntry(libKey);

				String folderPath = ((IFolder) selection[0])
						.getProjectRelativePath().addTrailingSeparator()
						.toString();

				if (entry != null && entry.contains(folderPath))
					return new Status(

							IStatus.ERROR,
							PDEPlugin.getPluginId(),
							IStatus.ERROR,
							PDEPlugin
									.getResourceString("BuildEditor.RuntimeInfoSection.missingSource.duplicateFolder"),
							null);

				return new Status(IStatus.OK, PDEPlugin.getPluginId(),
						IStatus.OK, "", null);
			}
		});

		if (dialog.open() == FolderSelectionDialog.OK) {
			try {
				IFolder folder = (IFolder) dialog.getFirstResult();
				String folderPath = folder.getProjectRelativePath()
						.addTrailingSeparator().toString();
				IBuildModel buildModel = getBuildModel();
				String libKey = fCurrentLibrary.getName();
				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);

				fCurrentSelection = new StructuredSelection(folderPath);

				entry.addToken(folderPath);
				refreshOutputKeys();

			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	protected void handleDown() {
		int index = fLibraryViewer.getTable().getSelectionIndex();
		String item1 = ((IBuildEntry) fLibraryViewer.getElementAt(index))
				.getName().substring(7);
		String item2 = ((IBuildEntry) fLibraryViewer.getElementAt(index + 1))
				.getName().substring(7);

		updateJarsCompileOrder(item1, item2);
	}

	protected void handleUp() {
		int index = fLibraryViewer.getTable().getSelectionIndex();
		String item1 = ((IBuildEntry) fLibraryViewer.getElementAt(index))
				.getName().substring(7);
		String item2 = ((IBuildEntry) fLibraryViewer.getElementAt(index - 1))
				.getName().substring(7);
		
		updateJarsCompileOrder(item1, item2);
	}

	public void updateJarsCompileOrder(String library1, String library2) {
		IBuildModel model = getBuildModel();
		IBuild build = model.getBuild();
		IBuildEntry jarOrderEntry = build
				.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
		try {
			if (jarOrderEntry == null) {
				jarOrderEntry = model.getFactory().createEntry(
						IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
				build.add(jarOrderEntry);
			} else {
				String tokens[] = jarOrderEntry.getTokens();
				for (int i =0; i<tokens.length; i++){
					jarOrderEntry.removeToken(tokens[i]);
				}
			}

			
			int numLib = fLibraryViewer.getTable().getItemCount();
			String[] names = new String[numLib];
			for (int i = 0; i < numLib; i++) {
				String name = ((IBuildEntry) fLibraryViewer
						.getElementAt(i)).getName().substring(7);
				if (name.equals(library1))
					name = library2;
				else if (name.equals(library2))
					name = library1;
				names[i] = name;
			}
			
			for (int i = 0; i < numLib; i++)
				jarOrderEntry.addToken(names[i]);

		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void modelChanged(IModelChangedEvent event) {
	
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
		Object changeObject = event.getChangedObjects()[0];
		String keyName = event.getChangedProperty();
		int type = event.getChangeType();
		// check if model change applies to this section
		if (!(changeObject instanceof IBuildEntry)
				|| (!((IBuildEntry)changeObject).getName().startsWith(IBuildEntry.JAR_PREFIX) 
						&& !((IBuildEntry)changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_JAR_ORDER)
						&& !((IBuildEntry)changeObject).getName().equals(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES)))
			return;
		
		final IBuildEntry entry = (IBuildEntry)changeObject;
		
		if (keyName!= null && keyName.equals(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES)){
			if (fCurrentLibrary == null)
				return;
			if (event.getOldValue() == null || event.getNewValue() == null ){ // added/removed token
				boolean isInBinBuild = entry.contains(fCurrentLibrary.getName().substring(7));
				fIncludeLibraryButton.setSelection(isInBinBuild);
			}
			return;
		}
		
		
		
		if (type == IModelChangedEvent.INSERT){
//			 account for new key
			fLibraryViewer.refresh();
			if (fCurrentSelection != null) {
				fLibraryViewer.setSelection(fCurrentSelection);
				fIncludeLibraryButton.setSelection(true);
				updateDirectionalButtons();
			} else {
				fFolderPart.setButtonEnabled(0, false);
				fLibraryViewer.setSelection(null);
				fFolderViewer.setInput(null);
				fIncludeLibraryButton.setVisible(false);
			}
		} else if (type == IModelChangedEvent.REMOVE){
			// account for key removal
			fLibraryViewer.remove(entry);
			fLibraryViewer.refresh();
			fFolderPart.setButtonEnabled(0, false);
			fLibraryViewer.setSelection(null);
			fFolderViewer.setInput(null);
			fIncludeLibraryButton.setVisible(false);
		} else if (keyName!=null && keyName.startsWith(IBuildEntry.JAR_PREFIX)){ 
			// modification to source.{libname}.jar
			// renaming token
			if (event.getOldValue() != null && event.getNewValue() != null){
				fLibraryViewer.update(entry, null);
				return;
			} else { // add/remove source folder
				fFolderViewer.refresh();
				if (fCurrentSelection != null) {
					fFolderViewer.setSelection(fCurrentSelection);
					updateDirectionalButtons();
				} else {
					fFolderPart.setButtonEnabled(0, false);
					fLibraryViewer.setSelection(null);
					fFolderViewer.setInput(null);
					fIncludeLibraryButton.setVisible(false);
				}
			}
		} else if (keyName!= null && keyName.equals(IBuildPropertiesConstants.PROPERTY_JAR_ORDER)){
			// account for change in jars compile order
			if (event.getNewValue() == null && event.getOldValue() != null){
				// removing token from jars compile order : do nothing
				return;
			}
			if (event.getOldValue() != null && event.getNewValue() != null){
				// renaming token from jars compile order : do nothing
				return;
			}

			fLibraryViewer.refresh();
			if (fCurrentLibrary != null) {
				fLibraryViewer.setSelection(new StructuredSelection(
						fCurrentLibrary));
			}
			updateDirectionalButtons();
		}
	}

}