/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.pde.internal.ui.dialogs.FolderSelectionDialog;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.RenameDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class RuntimeInfoSection extends PDESection implements IModelChangedListener, IBuildPropertiesConstants {

	private static final int F_NEW_INDEX = 0;
	private static final int F_UP_UNDEX = 2;
	private static final int F_DOWN_INDEX = 3;

	protected TableViewer fLibraryViewer;
	protected TableViewer fFolderViewer;

	protected StructuredViewerPart fLibraryPart;
	protected StructuredViewerPart fFolderPart;

	private boolean fEnabled = true;

	class PartAdapter extends TablePart {

		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}

		public void selectionChanged(IStructuredSelection selection) {
			getPage().getPDEEditor().setSelection(selection);
			Object item = selection.getFirstElement();
			if (item instanceof IBuildEntry) {
				update((IBuildEntry) item);
			} else if (selection == null || selection.isEmpty())
				update(null);
			updateDirectionalButtons();
		}

		public void handleDoubleClick(IStructuredSelection selection) {
			Object element = selection.getFirstElement();
			if (getLibrarySelection() == element)
				doRename();
			else if (element instanceof String)
				handleRenameFolder((String) element);
		}

		public void buttonSelected(Button button, int index) {
			if (getViewer() == fLibraryPart.getViewer()) {
				switch (index) {
					case F_NEW_INDEX :
						handleNew();
						break;
					case F_UP_UNDEX : // move up
						updateJarsCompileOrder(true);
						break;
					case F_DOWN_INDEX : // move down
						updateJarsCompileOrder(false);
						break;
				}
			} else if (getViewer() == fFolderPart.getViewer() && index == F_NEW_INDEX)
				handleNewFolder();
			else
				button.getShell().setDefaultButton(null);
		}
	}

	public class LibraryContentProvider extends DefaultContentProvider implements IStructuredContentProvider {

		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel) parent).getBuild();
				IBuildEntry jarOrderEntry = build.getEntry(PROPERTY_JAR_ORDER);
				IBuildEntry[] libraries = BuildUtil.getBuildLibraries(build.getBuildEntries());
				if (jarOrderEntry == null)
					return libraries;

				Vector libList = new Vector();
				String[] tokens = jarOrderEntry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IBuildEntry entry = build.getEntry(IBuildEntry.JAR_PREFIX + tokens[i]);
					if (entry != null)
						libList.add(entry);
				}
				for (int i = 0; i < libraries.length; i++)
					if (!libList.contains(libraries[i]))
						libList.add(libraries[i]);
				return libList.toArray();
			}
			return new Object[0];
		}
	}

	public class LibraryLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			String name = ((IBuildEntry) obj).getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX))
				return name.substring(IBuildEntry.JAR_PREFIX.length());
			return name;
		}

		public Image getColumnImage(Object obj, int index) {
			PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
			return provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);
		}
	}

	class JarsNewContentProvider extends WorkbenchContentProvider {
		public boolean hasChildren(Object element) {
			Object[] children = getChildren(element);
			for (int i = 0; i < children.length; i++)
				if (children[i] instanceof IFolder)
					return true;
			return false;
		}
	}

	public class FolderContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return (parent instanceof IBuildEntry) ? ((IBuildEntry) parent).getTokens() : new Object[0];
		}
	}

	public class FolderLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}

		public Image getColumnImage(Object obj, int index) {
			ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
	}

	public RuntimeInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.BuildEditor_RuntimeInfoSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_RuntimeInfoSection_desc);
		getBuildModel().addModelChangedListener(this);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager().findContext(BuildInputContext.CONTEXT_ID);
		if (context == null)
			return null;
		return (IBuildModel) context.getModel();
	}

	protected void handleLibInBinBuild(boolean isSelected, String libName) {
		IBuildModel model = getBuildModel();
		IBuildEntry binIncl = model.getBuild().getEntry(PROPERTY_BIN_INCLUDES);
		IProject project = model.getUnderlyingResource().getProject();
		IPath libPath;
		if (libName.equals(".")) //$NON-NLS-1$
			libPath = null;
		else
			libPath = project.getFile(libName).getProjectRelativePath();
		try {
			if (binIncl == null && !isSelected)
				return;
			if (binIncl == null) {
				binIncl = model.getFactory().createEntry(PROPERTY_BIN_INCLUDES);
				model.getBuild().add(binIncl);
			}
			if (libPath != null) {
				if (!isSelected && libPath.segmentCount() == 1 && binIncl.contains("*.jar")) { //$NON-NLS-1$
					addAllJarsToBinIncludes(binIncl, project, model);
				} else if (!isSelected && libPath.segmentCount() > 1) {
					IPath parent = libPath.removeLastSegments(1);
					String parentPath = parent.toString() + IPath.SEPARATOR;
					if (binIncl.contains(parentPath) && !project.exists(parent)) {
						binIncl.removeToken(parentPath);
					} else if (parent.segmentCount() > 1) {
						parent = parent.removeLastSegments(1);
						parentPath = parent.toString() + IPath.SEPARATOR;
						if (binIncl.contains(parentPath) && !project.exists(parent))
							binIncl.removeToken(parentPath);
					}
				}
			}
			if (isSelected && !binIncl.contains(libName))
				binIncl.addToken(libName);
			else if (!isSelected && binIncl.contains(libName))
				binIncl.removeToken(libName);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

	}

	protected void addAllJarsToBinIncludes(IBuildEntry binIncl, IProject project, IBuildModel model) {
		try {
			IResource[] members = project.members();
			for (int i = 0; i < members.length; i++)
				if (!(members[i] instanceof IFolder) && members[i].getFileExtension().equals("jar")) //$NON-NLS-1$
					binIncl.addToken(members[i].getName());

			IBuildEntry[] libraries = BuildUtil.getBuildLibraries(model.getBuild().getBuildEntries());
			if (libraries.length != 0) {
				for (int j = 0; j < libraries.length; j++) {
					String libraryName = libraries[j].getName().substring(7);
					IPath path = project.getFile(libraryName).getProjectRelativePath();
					if (path.segmentCount() == 1 && !binIncl.contains(libraryName))
						binIncl.addToken(libraryName);
				}
			}
			binIncl.removeToken("*.jar"); //$NON-NLS-1$
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void setOutputEntryTokens(Set outputFolders, IBuildEntry outputEntry) {
		Iterator iter = outputFolders.iterator();
		try {
			while (iter.hasNext()) {
				String outputFolder = iter.next().toString();
				if (!outputFolder.endsWith("" + IPath.SEPARATOR)) //$NON-NLS-1$
					outputFolder = outputFolder.concat("" + IPath.SEPARATOR); //$NON-NLS-1$
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
				IPackageFragmentRoot[] roots = jProject.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++)
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE)
						folders.add(roots[i]);
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return (IPackageFragmentRoot[]) folders.toArray(new IPackageFragmentRoot[folders.size()]);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));

		createLeftSection(container, toolkit);
		createRightSection(container, toolkit);

		toolkit.paintBordersFor(container);
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);
		section.setClient(container);
	}

	private void createLeftSection(Composite parent, FormToolkit toolkit) {
		Composite container = createContainer(parent, toolkit);

		fLibraryPart = new PartAdapter(new String[] {PDEUIMessages.BuildEditor_RuntimeInfoSection_addLibrary, null, PDEUIMessages.ManifestEditor_LibrarySection_up, PDEUIMessages.ManifestEditor_LibrarySection_down});
		fLibraryPart.createControl(container, SWT.FULL_SELECTION, 2, toolkit);
		fLibraryViewer = (TableViewer) fLibraryPart.getViewer();
		fLibraryViewer.setContentProvider(new LibraryContentProvider());
		fLibraryViewer.setLabelProvider(new LibraryLabelProvider());
		fLibraryPart.setButtonEnabled(F_UP_UNDEX, false);
		fLibraryPart.setButtonEnabled(F_DOWN_INDEX, false);
		fLibraryViewer.setInput(getBuildModel());
		toolkit.paintBordersFor(container);

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillLibraryContextMenu(manager);
			}
		});
		fLibraryViewer.getControl().setMenu(menuMgr.createContextMenu(fLibraryViewer.getControl()));
	}

	private void createRightSection(Composite parent, FormToolkit toolkit) {
		Composite container = createContainer(parent, toolkit);

		fFolderPart = new PartAdapter(new String[] {PDEUIMessages.BuildEditor_RuntimeInfoSection_addFolder}) {
			public void selectionChanged(IStructuredSelection selection) {
				// folder selection ignored
			}
		};
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
		fFolderViewer.getControl().setMenu(menuMgr.createContextMenu(fFolderViewer.getControl()));
	}

	private Composite createContainer(Composite parent, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		container.setLayoutData(gd);
		return container;
	}

	protected void fillFolderViewerContextMenu(IMenuManager manager) {
		final ISelection selection = fFolderViewer.getSelection();
		ISelection libSelection = fLibraryViewer.getSelection();
		if (libSelection != null && !libSelection.isEmpty()) {
			Action newAction = new Action(PDEUIMessages.BuildEditor_RuntimeInfoSection_popupFolder) {
				public void run() {
					handleNewFolder();
				}
			};
			newAction.setEnabled(fEnabled);
			manager.add(newAction);
		}

		manager.add(new Separator());

		Action replace = new Action(PDEUIMessages.RuntimeInfoSection_replace) {
			public void run() {
				handleRenameFolder(((IStructuredSelection) selection).getFirstElement().toString());
			}
		};
		replace.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(replace);

		Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
			public void run() {
				handleDeleteFolder();
			}
		};
		deleteAction.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(deleteAction);

		// defect 19550
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
	}

	protected void fillLibraryContextMenu(IMenuManager manager) {
		ISelection selection = fLibraryViewer.getSelection();
		Action newAction = new Action(PDEUIMessages.BuildEditor_RuntimeInfoSection_popupAdd) {
			public void run() {
				handleNew();
			}
		};
		newAction.setEnabled(fEnabled);
		manager.add(newAction);

		manager.add(new Separator());
		IAction renameAction = new Action(PDEUIMessages.EditableTablePart_renameAction) {
			public void run() {
				doRename();
			}
		};
		renameAction.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(renameAction);

		Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(deleteAction);

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, false);
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
			if (!newValue.endsWith(".jar") && //$NON-NLS-1$
					!newValue.endsWith("/") && //$NON-NLS-1$
					!newValue.equals(IBuildEntry.JAR_PREFIX + ".")) //$NON-NLS-1$
				newValue += "/"; //$NON-NLS-1$

			String newName = newValue.substring(7);

			// jars.compile.order
			IBuildEntry tempEntry = build.getEntry(PROPERTY_JAR_ORDER);
			if (tempEntry != null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newName);

			// output.{source folder}.jar
			tempEntry = build.getEntry(PROPERTY_OUTPUT_PREFIX + oldName);
			if (tempEntry != null)
				tempEntry.setName(PROPERTY_OUTPUT_PREFIX + newName);

			// bin.includes
			tempEntry = build.getEntry(PROPERTY_BIN_INCLUDES);
			if (tempEntry != null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newName);

			// bin.excludes
			tempEntry = build.getEntry(PROPERTY_BIN_EXCLUDES);
			if (tempEntry != null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newName);

			// rename
			entry.setName(newValue);

		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void enableSection(boolean enable) {
		fEnabled = enable;
		fLibraryPart.setButtonEnabled(F_NEW_INDEX, enable);
		updateDirectionalButtons();
		fFolderPart.setButtonEnabled(F_NEW_INDEX, enable && !fLibraryViewer.getSelection().isEmpty());
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			if (fEnabled && fLibraryViewer.getControl().isFocusControl())
				handleDelete();
			else if (fEnabled)
				handleDeleteFolder();
			return true;
		}
		return false;
	}

	private void doRename() {
		IStructuredSelection selection = (IStructuredSelection) fLibraryViewer.getSelection();
		if (selection.size() == 1) {
			IBuildEntry entry = (IBuildEntry) selection.getFirstElement();
			String oldName = entry.getName().substring(7);
			RenameDialog dialog = new RenameDialog(fLibraryViewer.getControl().getShell(), true, getLibraryNames(), oldName);
			dialog.setInputValidator(new IInputValidator() {
				public String isValid(String newText) {
					if (newText.indexOf(' ') != -1)
						return PDEUIMessages.AddLibraryDialog_nospaces;
					return null;
				}
			});
			dialog.create();
			dialog.setTitle(PDEUIMessages.RuntimeInfoSection_rename);
			dialog.getShell().setSize(300, 150);
			if (dialog.open() == Window.OK)
				entryModified(entry, dialog.getNewName());
		}
	}

	public void dispose() {
		IBuildModel buildModel = getBuildModel();
		if (buildModel != null)
			buildModel.removeModelChangedListener(this);
		super.dispose();
	}

	private void refreshOutputKeys() {
		if (!isJavaProject())
			return;

		IBuildEntry buildEntry = getLibrarySelection();
		if (buildEntry == null)
			return;
		Set outputFolders = new HashSet();
		String[] jarFolders = buildEntry.getTokens();
		IPackageFragmentRoot[] sourceFolders = computeSourceFolders();
		for (int j = 0; j < jarFolders.length; j++) {
			IPackageFragmentRoot sourceFolder = getSourceFolder(jarFolders[j], sourceFolders);
			if (sourceFolder != null) {
				try {
					IClasspathEntry entry = sourceFolder.getRawClasspathEntry();
					IPath outputPath = entry.getOutputLocation();
					if (outputPath == null) {
						outputFolders.add("bin"); //$NON-NLS-1$
					} else {
						outputPath = outputPath.removeFirstSegments(1);
						outputFolders.add(outputPath.toString());
					}
				} catch (JavaModelException e) {
					PDEPlugin.logException(e);
				}
			}
		}
		if (outputFolders.size() != 0) {
			String libName = buildEntry.getName().substring(7);
			IBuildModel buildModel = getBuildModel();
			IBuild build = buildModel.getBuild();
			String outputName = PROPERTY_OUTPUT_PREFIX + libName;

			IBuildEntry outputEntry = build.getEntry(outputName);
			if (outputEntry == null) {
				outputEntry = buildModel.getFactory().createEntry(outputName);
				try {
					build.add(outputEntry);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			setOutputEntryTokens(outputFolders, outputEntry);
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
		int index = 0;
		if (fFolderViewer.getInput() == variable)
			index = fFolderViewer.getTable().getSelectionIndex();

		fFolderViewer.setInput(variable);
		int count = fFolderViewer.getTable().getItemCount();
		if (index != -1 && count > 0) {
			if (index == count)
				index = index - 1;
			fFolderViewer.getTable().select(index);
		}
		fFolderPart.setButtonEnabled(F_NEW_INDEX, !isReadOnly() && fEnabled && variable != null);
	}

	protected void updateDirectionalButtons() {
		Table table = fLibraryViewer.getTable();
		boolean hasSelection = table.getSelection().length > 0;
		fLibraryPart.setButtonEnabled(F_UP_UNDEX, fEnabled && hasSelection && table.getSelectionIndex() > 0);
		fLibraryPart.setButtonEnabled(F_DOWN_INDEX, fEnabled && hasSelection && table.getSelectionIndex() < table.getItemCount() - 1);
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
		final IPluginModelBase pluginModelBase = (pmodel instanceof IPluginModelBase) ? (IPluginModelBase) pmodel : null;

		BusyIndicator.showWhile(fLibraryViewer.getTable().getDisplay(), new Runnable() {
			public void run() {
				IBuildModel buildModel = getBuildModel();
				IBuild build = buildModel.getBuild();
				AddLibraryDialog dialog = new AddLibraryDialog(getSection().getShell(), libNames, pluginModelBase);
				dialog.create();
				dialog.getShell().setText(PDEUIMessages.RuntimeInfoSection_addEntry);

				try {
					if (dialog.open() == Window.OK) {
						String name = dialog.getNewName();
						if (!name.endsWith(".jar") //$NON-NLS-1$
								&& !name.equals(".") //$NON-NLS-1$
								&& !name.endsWith("/")) //$NON-NLS-1$
							name += "/"; //$NON-NLS-1$

						String keyName = name;
						if (!keyName.startsWith(IBuildEntry.JAR_PREFIX))
							keyName = IBuildEntry.JAR_PREFIX + name;
						if (name.startsWith(IBuildEntry.JAR_PREFIX))
							name = name.substring(7);

						if (!name.endsWith(".")) //$NON-NLS-1$
							handleLibInBinBuild(true, name);

						// add library to jars compile order
						IBuildEntry jarOrderEntry = build.getEntry(PROPERTY_JAR_ORDER);
						int numLib = fLibraryViewer.getTable().getItemCount();

						if (jarOrderEntry == null) {
							jarOrderEntry = getBuildModel().getFactory().createEntry(PROPERTY_JAR_ORDER);

							// add all runtime libraries to compile order
							for (int i = 0; i < numLib; i++) {
								String lib = ((IBuildEntry) fLibraryViewer.getElementAt(i)).getName().substring(7);
								jarOrderEntry.addToken(lib);
							}
							jarOrderEntry.addToken(name);
							build.add(jarOrderEntry);
						} else if (jarOrderEntry.getTokens().length < numLib) {

							// remove and re-add all runtime libraries to compile order
							String[] tokens = jarOrderEntry.getTokens();
							for (int i = 0; i < tokens.length; i++)
								jarOrderEntry.removeToken(tokens[i]);

							for (int i = 0; i < numLib; i++) {
								String lib = ((IBuildEntry) fLibraryViewer.getElementAt(i)).getName().substring(7);
								jarOrderEntry.addToken(lib);
							}
							jarOrderEntry.addToken(name);
						} else {
							jarOrderEntry.addToken(name);
						}
						// end of jars compile order addition

						IBuildEntry library = buildModel.getFactory().createEntry(keyName);
						build.add(library);

					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}

	private IPackageFragmentRoot getSourceFolder(String folderName, IPackageFragmentRoot[] sourceFolders) {
		for (int i = 0; i < sourceFolders.length; i++)
			if (sourceFolders[i].getPath().removeFirstSegments(1).equals(new Path(folderName)))
				return sourceFolders[i];
		return null;
	}

	protected void handleDelete() {
		int index = fLibraryViewer.getTable().getSelectionIndex();
		if (index != -1) {
			String libName = fLibraryViewer.getTable().getItem(index).getText();
			IBuild build = getBuildModel().getBuild();

			try {
				// jars.compile.order
				IBuildEntry entry = build.getEntry(PROPERTY_JAR_ORDER);
				int numLib = fLibraryViewer.getTable().getItemCount();

				if (entry == null) {
					entry = getBuildModel().getFactory().createEntry(PROPERTY_JAR_ORDER);

					// add all runtime libraries to compile order
					for (int i = 0; i < numLib; i++) {
						String lib = ((IBuildEntry) fLibraryViewer.getElementAt(i)).getName().substring(7);
						entry.addToken(lib);
					}
					build.add(entry);
				} else if (entry.getTokens().length < numLib) {

					// remove and re-add all runtime libraries to compile order
					String[] tokens = entry.getTokens();
					for (int i = 0; i < tokens.length; i++)
						entry.removeToken(tokens[i]);

					for (int i = 0; i < numLib; i++) {
						Object element = fLibraryViewer.getElementAt(i);
						if (element == null) {
							continue;
						}
						String lib = ((IBuildEntry) element).getName().substring(7);
						entry.addToken(lib);
					}
				}

				entry.removeToken(libName);

				// output.{source folder}.jar
				entry = build.getEntry(PROPERTY_OUTPUT_PREFIX + libName);
				if (entry != null)
					build.remove(entry);

				// bin.includes
				entry = build.getEntry(PROPERTY_BIN_INCLUDES);
				if (entry != null && entry.contains(libName))
					entry.removeToken(libName);

				// bin.excludes
				entry = build.getEntry(PROPERTY_BIN_EXCLUDES);
				if (entry != null && entry.contains(libName))
					entry.removeToken(libName);

				String entryName = IBuildEntry.JAR_PREFIX + libName;
				entry = build.getEntry(entryName);
				if (entry != null) {
					build.remove(entry);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private void handleDeleteFolder() {
		Object object = ((IStructuredSelection) fFolderViewer.getSelection()).getFirstElement();
		if (object == null)
			return;
		IBuildEntry entry = getLibrarySelection();
		if (entry == null)
			return;
		try {
			entry.removeToken((String) object);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private IFolder openSelectFolderDialog(final IBuildEntry entry, String title, String message) {
		IFile file = (IFile) getBuildModel().getUnderlyingResource();
		final IProject project = file.getProject();

		FolderSelectionDialog dialog = new FolderSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), new WorkbenchLabelProvider(), new JarsNewContentProvider() {});

		dialog.setInput(project.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IProject)
					return ((IProject) element).equals(project);
				return element instanceof IFolder;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(title);
		dialog.setMessage(message);

		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				String id = PDEPlugin.getPluginId();
				if (selection == null || selection.length != 1 || !(selection[0] instanceof IFolder))
					return new Status(IStatus.ERROR, id, IStatus.ERROR, "", null); //$NON-NLS-1$

				String folderPath = ((IFolder) selection[0]).getProjectRelativePath().addTrailingSeparator().toString();
				if (entry != null && entry.contains(folderPath))
					return new Status(IStatus.ERROR, id, IStatus.ERROR, PDEUIMessages.BuildEditor_RuntimeInfoSection_duplicateFolder, null);

				return new Status(IStatus.OK, id, IStatus.OK, "", null); //$NON-NLS-1$
			}
		});

		if (dialog.open() == Window.OK)
			return (IFolder) dialog.getFirstResult();
		return null;
	}

	private void handleNewFolder() {
		IBuildEntry entry = getLibrarySelection();
		IFolder folder = openSelectFolderDialog(entry, PDEUIMessages.ManifestEditor_JarsSection_dialogTitle, PDEUIMessages.ManifestEditor_JarsSection_dialogMessage);
		if (folder != null) {
			try {
				String folderPath = folder.getProjectRelativePath().addTrailingSeparator().toString();
				entry.addToken(folderPath);
				refreshOutputKeys();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private void handleRenameFolder(String oldName) {
		IBuildEntry entry = getLibrarySelection();
		IFolder folder = openSelectFolderDialog(entry, PDEUIMessages.RuntimeInfoSection_replacedialog, PDEUIMessages.ManifestEditor_JarsSection_dialogMessage);
		if (folder != null) {
			try {
				String newFolder = folder.getProjectRelativePath().addTrailingSeparator().toString();
				entry.renameToken(oldName, newFolder);
				refreshOutputKeys();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	protected void updateJarsCompileOrder(boolean up) {
		int direction = up ? -1 : 1;
		int index = fLibraryViewer.getTable().getSelectionIndex();
		String library1 = ((IBuildEntry) fLibraryViewer.getElementAt(index)).getName().substring(7);
		String library2 = ((IBuildEntry) fLibraryViewer.getElementAt(index + direction)).getName().substring(7);

		IBuildModel model = getBuildModel();
		IBuild build = model.getBuild();
		IBuildEntry jarOrderEntry = build.getEntry(PROPERTY_JAR_ORDER);
		try {
			if (jarOrderEntry == null) {
				jarOrderEntry = model.getFactory().createEntry(PROPERTY_JAR_ORDER);
				build.add(jarOrderEntry);
			} else {
				String tokens[] = jarOrderEntry.getTokens();
				for (int i = 0; i < tokens.length; i++)
					jarOrderEntry.removeToken(tokens[i]);
			}

			int numLib = fLibraryViewer.getTable().getItemCount();
			String[] names = new String[numLib];
			for (int i = 0; i < numLib; i++) {
				String name = ((IBuildEntry) fLibraryViewer.getElementAt(i)).getName().substring(7);
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
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED)
			markStale();
		Object changeObject = event.getChangedObjects()[0];
		String keyName = event.getChangedProperty();

		// check if model change applies to this section
		if (!(changeObject instanceof IBuildEntry))
			return;
		IBuildEntry entry = (IBuildEntry) changeObject;
		String entryName = entry.getName();
		if (!entryName.startsWith(IBuildEntry.JAR_PREFIX) && !entryName.equals(PROPERTY_JAR_ORDER) && !entryName.equals(PROPERTY_BIN_INCLUDES))
			return;

		if (entryName.equals(PROPERTY_BIN_INCLUDES))
			return;

		int type = event.getChangeType();

		// account for new key
		if (entry.getName().startsWith(PROPERTY_SOURCE_PREFIX)) {
			IStructuredSelection newSel = null;
			if (type == IModelChangedEvent.INSERT) {
				fLibraryViewer.add(entry);
				newSel = new StructuredSelection(entry);
			} else if (type == IModelChangedEvent.REMOVE) {
				int index = fLibraryViewer.getTable().getSelectionIndex();
				fLibraryViewer.remove(entry);
				Table table = fLibraryViewer.getTable();
				int itemCount = table.getItemCount();
				if (itemCount != 0) {
					index = index < itemCount ? index : itemCount - 1;
					newSel = new StructuredSelection(table.getItem(index).getData());
				}
			} else if (keyName != null && keyName.startsWith(IBuildEntry.JAR_PREFIX)) {
				// modification to source.{libname}.jar
				if (event.getOldValue() != null && event.getNewValue() != null)
					// renaming token
					fLibraryViewer.update(entry, null);

				newSel = new StructuredSelection(entry);
			}
			fLibraryViewer.setSelection(newSel);
		} else if (keyName != null && keyName.equals(PROPERTY_JAR_ORDER)) {
			// account for change in jars compile order
			if (event.getNewValue() == null && event.getOldValue() != null)
				// removing token from jars compile order : do nothing
				return;
			if (event.getOldValue() != null && event.getNewValue() != null)
				// renaming token from jars compile order : do nothing
				return;

			fLibraryViewer.refresh();
			updateDirectionalButtons();
		}
	}

	private IBuildEntry getLibrarySelection() {
		IStructuredSelection selection = (IStructuredSelection) fLibraryViewer.getSelection();
		return (IBuildEntry) selection.getFirstElement();
	}
}
