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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 351356
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.plugin.PluginLibrary;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.build.BuildInputContext;
import org.eclipse.pde.internal.ui.editor.build.BuildSourcePage;
import org.eclipse.pde.internal.ui.editor.build.JARFileFilter;
import org.eclipse.pde.internal.ui.editor.context.InputContextManager;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class LibrarySection extends TableSection implements IBuildPropertiesConstants {

	private static final int NEW_INDEX = 0;
	private static final int ADD_INDEX = 1;
	private static final int REMOVE_INDEX = 2;
	private static final int UP_INDEX = 3;
	private static final int DOWN_INDEX = 4;

	private Action fRenameAction;
	private Action fRemoveAction;
	private Action fNewAction;

	private TableViewer fLibraryTable;

	class LibraryFilter extends JARFileFilter {
		public LibraryFilter(Set<IPath> set) {
			super(set);
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			if (element instanceof IFolder)
				return isPathValid(((IFolder) element).getProjectRelativePath());
			if (element instanceof IFile)
				return isFileValid(((IFile) element).getProjectRelativePath());
			return false;
		}
	}

	class LibrarySelectionValidator extends JarSelectionValidator {

		public LibrarySelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection) {
			super(acceptedTypes, allowMultipleSelection);
		}

		@Override
		public boolean isValid(Object element) {
			return (element instanceof IFolder) ? true : super.isValid(element);
		}
	}

	class TableContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			return getModel().getPluginBase().getLibraries();
		}
	}

	public LibrarySection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION, new String[] {PDEUIMessages.NewManifestEditor_LibrarySection_new, PDEUIMessages.NewManifestEditor_LibrarySection_add, PDEUIMessages.NewManifestEditor_LibrarySection_remove, PDEUIMessages.ManifestEditor_LibrarySection_up, PDEUIMessages.ManifestEditor_LibrarySection_down});
	}

	private String getSectionDescription() {
		IPluginModelBase model = getModel();
		if (isBundle()) {
			return (model != null && model.isFragmentModel()) ? PDEUIMessages.ClasspathSection_fragment
					: PDEUIMessages.ClasspathSection_plugin;
		}
		return (model != null && model.isFragmentModel()) ? PDEUIMessages.ManifestEditor_LibrarySection_fdesc
				: PDEUIMessages.ManifestEditor_LibrarySection_desc;
	}

	protected boolean isBundle() {
		return getBundleContext() != null;
	}

	private BundleInputContext getBundleContext() {
		InputContextManager manager = getPage().getPDEEditor().getContextManager();
		return (BundleInputContext) manager.findContext(BundleInputContext.CONTEXT_ID);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ManifestEditor_LibrarySection_title);
		section.setDescription(getSectionDescription());

		Composite container = createClientContainer(section, 2, toolkit);
		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(isEditable());

		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		fLibraryTable = tablePart.getTableViewer();
		fLibraryTable.setContentProvider(new TableContentProvider());
		fLibraryTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		toolkit.paintBordersFor(container);

		makeActions();
		updateButtons();
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		section.setClient(container);

		IPluginModelBase model = getModel();
		if (model != null) {
			fLibraryTable.setInput(model.getPluginBase());
			model.addModelChangedListener(this);
		}
	}

	private void updateButtons() {
		Table table = fLibraryTable.getTable();
		boolean hasSelection = table.getSelection().length > 0;
		boolean singleSelection = table.getSelection().length == 1;
		int count = table.getItemCount();
		int index = table.getSelectionIndex();
		boolean canMoveUp = singleSelection && index > 0;
		boolean canMoveDown = singleSelection && index < count - 1;

		TablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(ADD_INDEX, isEditable());
		tablePart.setButtonEnabled(NEW_INDEX, isEditable());
		tablePart.setButtonEnabled(REMOVE_INDEX, isEditable() && hasSelection);
		tablePart.setButtonEnabled(UP_INDEX, isEditable() && canMoveUp);
		tablePart.setButtonEnabled(DOWN_INDEX, isEditable() && canMoveDown);
	}

	private void makeActions() {
		fNewAction = new Action(PDEUIMessages.ManifestEditor_LibrarySection_newLibrary) {
			@Override
			public void run() {
				handleNew();
			}
		};
		fNewAction.setEnabled(isEditable());

		fRenameAction = new Action(PDEUIMessages.EditableTablePart_renameAction) {
			@Override
			public void run() {
				getRenameAction().run();
			}
		};
		fRenameAction.setEnabled(isEditable());

		fRemoveAction = new Action(PDEUIMessages.NewManifestEditor_LibrarySection_remove) {
			@Override
			public void run() {
				handleRemove();
			}
		};
		fRemoveAction.setEnabled(isEditable());
	}

	@Override
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		IBaseModel model = getPage().getModel();
		if (model != null && model.isEditable())
			updateButtons();
	}

	@Override
	protected void buttonSelected(int index) {
		switch (index) {
			case NEW_INDEX -> handleNew();
			case ADD_INDEX -> handleAdd();
			case REMOVE_INDEX -> handleRemove();
			case UP_INDEX -> handleUp();
			case DOWN_INDEX ->handleDown();
		}
	}

	@Override
	public void dispose() {
		IPluginModelBase model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public boolean doGlobalAction(String actionId) {

		if (!isEditable()) {
			return false;
		}

		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleRemove();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleRemove();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return super.doGlobalAction(actionId);
	}

	@Override
	public boolean setFormInput(Object object) {
		if (object instanceof IPluginLibrary) {
			fLibraryTable.setSelection(new StructuredSelection(object), true);
			return true;
		}
		return false;
	}

	@Override
	protected void fillContextMenu(IMenuManager manager) {
		manager.add(fNewAction);
		if (!fLibraryTable.getStructuredSelection().isEmpty()) {
			manager.add(new Separator());
			manager.add(fRenameAction);
			manager.add(fRemoveAction);
		}
		// Copy, cut, and paste operations not supported for plug-ins that do
		// not have a MANIFEST.MF (not a Bundle)
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager, isBundle());
	}

	private void handleRemove() {
		Object[] selection = fLibraryTable.getStructuredSelection().toArray();
		int index = fLibraryTable.getTable().getSelectionIndex();
		int[] indices = fLibraryTable.getTable().getSelectionIndices();
		for (int indice : indices)
			if (indice < index)
				index = indice;

		String[] remove = new String[selection.length];
		for (int i = 0; i < selection.length; i++) {
			if (selection[i] != null && selection[i] instanceof IPluginLibrary ep) {
				IPluginBase plugin = ep.getPluginBase();
				try {
					plugin.remove(ep);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
				remove[i] = ep.getName();
			}
		}
		updateBuildProperties(remove, new String[remove.length], true);
		updateJavaClasspathLibs(remove, new String[remove.length]);

		int itemCount = fLibraryTable.getTable().getItemCount();
		if (itemCount > 0) {
			if (index >= itemCount)
				index = itemCount - 1;
			fLibraryTable.getTable().setSelection(index);
			fLibraryTable.getTable().setFocus();
		}
		updateButtons();
	}

	private void handleDown() {
		Table table = getTablePart().getTableViewer().getTable();
		int index = table.getSelectionIndex();
		if (index != table.getItemCount() - 1)
			swap(index, index + 1);
	}

	private void handleUp() {
		int index = getTablePart().getTableViewer().getTable().getSelectionIndex();
		if (index >= 1)
			swap(index, index - 1);
	}

	public void swap(int index1, int index2) {
		Table table = getTablePart().getTableViewer().getTable();
		IPluginLibrary l1 = (IPluginLibrary) table.getItem(index1).getData();
		IPluginLibrary l2 = (IPluginLibrary) table.getItem(index2).getData();

		try {
			IPluginModelBase model = getModel();
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.swap(l1, l2);
			refresh();
			table.setSelection(index2);
			table.setFocus();
			updateButtons();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleNew() {
		IPluginModelBase model = getModel();
		NewRuntimeLibraryDialog dialog = new NewRuntimeLibraryDialog(getPage().getSite().getShell(), model.getPluginBase().getLibraries());
		dialog.create();
		dialog.getShell().setText(PDEUIMessages.ManifestEditor_LibrarySection_newLibraryEntry);
		SWTUtil.setDialogSize(dialog, 250, 175);

		if (dialog.open() == Window.OK) {
			String libName = dialog.getLibraryName();
			if (libName == null || libName.length() == 0)
				return;
			try {
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				library.setName(libName);
				library.setExported(true);
				model.getPluginBase().add(library);
				checkSourceRootEntry();
				updateBuildProperties(new String[] {null}, new String[] {library.getName()}, true);
				fLibraryTable.setSelection(new StructuredSelection(library));
				fLibraryTable.getTable().setFocus();
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private void checkSourceRootEntry() {
		IPluginModelBase pluginModel = getModel();
		IPluginLibrary[] libraries = pluginModel.getPluginBase().getLibraries();
		for (IPluginLibrary library : libraries)
			if (library.getName().equals(".")) //$NON-NLS-1$
				return;
		IBuildModel model = getBuildModel();
		if (model == null)
			return;

		IBuildEntry[] entires = model.getBuild().getBuildEntries();
		for (IBuildEntry entry : entires) {
			if (entry.getName().equals(PROPERTY_SOURCE_PREFIX + '.')) {
				IPluginLibrary library = pluginModel.getPluginFactory().createLibrary();
				try {
					library.setName("."); //$NON-NLS-1$
					pluginModel.getPluginBase().add(library);
				} catch (CoreException e) {
				}
			}
		}
	}

	private IBuildModel getBuildModel() {
		IFormPage page = getPage().getEditor().findPage(BuildInputContext.CONTEXT_ID);
		IBaseModel model = null;
		if (page instanceof BuildSourcePage)
			model = ((BuildSourcePage) page).getInputContext().getModel();

		if (model != null && model instanceof IBuildModel)
			return (IBuildModel) model;
		return null;
	}

	private void configureSourceBuildEntry(IBuildModel bmodel, String oldPath, String newPath) throws CoreException {
		IBuild build = bmodel.getBuild();
		IBuildEntry entry = build.getEntry(PROPERTY_SOURCE_PREFIX + (oldPath != null ? oldPath : newPath));
		try {
			if (newPath != null) {
				if (entry == null) {
					IProject project = ((IModel) getPage().getModel()).getUnderlyingResource().getProject();
					IJavaProject jproject = JavaCore.create(project);
					ArrayList<String> tokens = new ArrayList<>();
					IClasspathEntry[] entries = jproject.getRawClasspath();
					for (IClasspathEntry cpe : entries)
						if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE)
							tokens.add(cpe.getPath().removeFirstSegments(1).addTrailingSeparator().toString());
					if (tokens.isEmpty())
						return;

					entry = bmodel.getFactory().createEntry(PROPERTY_SOURCE_PREFIX + newPath);
					for (int i = 0; i < tokens.size(); i++)
						entry.addToken(tokens.get(i));
					build.add(entry);
				} else
					entry.setName(PROPERTY_SOURCE_PREFIX + newPath);
			} else if (entry != null)
				build.remove(entry);
		} catch (JavaModelException e) {
		}
	}

	private void handleAdd() {
		final boolean[] updateClasspath = new boolean[] {true};
		ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getPage().getSite().getShell(), new WorkbenchLabelProvider(), new WorkbenchContentProvider()) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite comp = (Composite) super.createDialogArea(parent);
				final Button button = new Button(comp, SWT.CHECK);
				button.setText(PDEUIMessages.LibrarySection_addDialogButton);
				button.setSelection(updateClasspath[0]);
				button.addSelectionListener(widgetSelectedAdapter(e -> updateClasspath[0] = button.getSelection()));
				applyDialogFont(button);
				return comp;
			}
		};

		Class<?>[] acceptedClasses = new Class[] {IFile.class};
		dialog.setValidator(new LibrarySelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEUIMessages.BuildEditor_ClasspathSection_jarsTitle);
		dialog.setMessage(PDEUIMessages.ClasspathSection_jarsMessage);
		IPluginLibrary[] libraries = getModel().getPluginBase().getLibraries();
		IProject project = ((IModel) getPage().getModel()).getUnderlyingResource().getProject();
		HashSet<IPath> set = new HashSet<>();
		for (IPluginLibrary library : libraries) {
			IPath bundlePath = IPath.fromOSString(ClasspathUtilCore.expandLibraryName(library.getName()));
			IPath buildPath = PDEProject.getBundleRoot(project).getProjectRelativePath().append(bundlePath);
			set.add(buildPath);
		}

		dialog.addFilter(new LibraryFilter(set));
		dialog.setInput(project);
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
		dialog.create();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(dialog.getShell(), IHelpContextIds.ADD_LIBRARY);
		if (dialog.open() == Window.OK) {
			Object[] elements = dialog.getResult();
			String[] bundlePaths = new String[elements.length];
			String[] buildPaths = new String[elements.length];
			IPluginModelBase model = getModel();
			ArrayList<IPluginLibrary> list = new ArrayList<>();
			for (int i = 0; i < elements.length; i++) {
				IResource elem = (IResource) elements[i];
				IContainer bundleRoot = PDEProject.getBundleRoot(project);
				IPath rootPath = bundleRoot.getFullPath();
				// make path relative to bundle root
				IPath bundlePath = elem.getFullPath().makeRelativeTo(rootPath);
				IPath buildPath = elem.getProjectRelativePath();
				if (elem instanceof IFolder) {
					bundlePath = bundlePath.addTrailingSeparator();
					buildPath = buildPath.addTrailingSeparator();
				}
				bundlePaths[i] = bundlePath.toString();
				buildPaths[i] = buildPath.toString();
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				try {
					library.setName(bundlePaths[i]);
					library.setExported(true);
					model.getPluginBase().add(library);
					list.add(library);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			checkSourceRootEntry();
			updateBuildProperties(new String[bundlePaths.length], bundlePaths, false);
			if (updateClasspath[0])
				updateJavaClasspathLibs(new String[buildPaths.length], buildPaths);
			fLibraryTable.setSelection(new StructuredSelection(list.toArray()));
			fLibraryTable.getTable().setFocus();
		}
	}

	private void updateBuildProperties(final String[] oldPaths, final String[] newPaths, boolean modifySourceEntry) {
		IBuildModel bmodel = getBuildModel();
		if (bmodel == null)
			return;

		IBuild build = bmodel.getBuild();

		IBuildEntry entry = build.getEntry(PROPERTY_BIN_INCLUDES);
		if (entry == null)
			entry = bmodel.getFactory().createEntry(PROPERTY_BIN_INCLUDES);

		try {
			// adding new entries
			if (oldPaths[0] == null) {
				for (String newPath : newPaths)
					if (newPath != null) {
						entry.addToken(newPath);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, null, newPath);
					}
				// removing entries
			} else if (newPaths[0] == null) {
				for (String oldPath : oldPaths)
					if (oldPath != null) {
						entry.removeToken(oldPath);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, oldPath, null);
					}
				if (entry.getTokens().length == 0)
					build.remove(entry);
				// rename entries
			} else {
				for (int i = 0; i < oldPaths.length; i++)
					if (newPaths[i] != null && oldPaths[i] != null) {
						entry.renameToken(oldPaths[i], newPaths[i]);
						if (modifySourceEntry)
							configureSourceBuildEntry(bmodel, oldPaths[i], newPaths[i]);
					}
			}
		} catch (CoreException e) {
		}
	}

	private void updateJavaClasspathLibs(String[] oldPaths, String[] newPaths) {
		IProject project = ((IModel) getPage().getModel()).getUnderlyingResource().getProject();
		IJavaProject jproject = JavaCore.create(project);
		try {
			IClasspathEntry[] entries = jproject.getRawClasspath();
			ArrayList<IClasspathEntry> toBeAdded = new ArrayList<>();
			int index = -1;
			entryLoop: for (int i = 0; i < entries.length; i++) {
				if (entries[i].getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					if (index == -1)
						index = i;
					// do not add the old paths (handling deletion/renaming)
					IPath path = entries[i].getPath().removeFirstSegments(1).removeTrailingSeparator();
					for (int j = 0; j < oldPaths.length; j++)
						if (oldPaths[j] != null && path.equals(IPath.fromOSString(oldPaths[j]).removeTrailingSeparator()))
							continue entryLoop;
				} else if (entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER)
					if (index == -1)
						index = i;
				toBeAdded.add(entries[i]);
			}
			if (index == -1)
				index = entries.length;

			// add paths
			for (String newPath : newPaths) {
				if (newPath == null)
					continue;
				IClasspathEntry entry = JavaCore.newLibraryEntry(project.getFullPath().append(newPath), null, null, true);
				if (!toBeAdded.contains(entry))
					toBeAdded.add(index++, entry);
			}

			if (toBeAdded.size() == entries.length)
				return;

			IClasspathEntry[] updated = toBeAdded.toArray(new IClasspathEntry[toBeAdded.size()]);
			jproject.setRawClasspath(updated, null);
		} catch (JavaModelException e) {
		}
	}

	@Override
	public void refresh() {
		if (fLibraryTable.getControl().isDisposed())
			return;
		fLibraryTable.setSelection(null);
		fLibraryTable.refresh();
		super.refresh();
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginLibrary) {
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				fLibraryTable.refresh();
			} else if (event.getChangeType() == IModelChangedEvent.REMOVE) {
				fLibraryTable.remove(changeObject);
			} else {
				fLibraryTable.update(changeObject, null);
			}
		} else if (changeObject.equals(fLibraryTable.getInput())) {
			markStale();
		} else if (changeObject instanceof IPluginElement && ((IPluginElement) changeObject).getParent() instanceof IPluginLibrary) {
			fLibraryTable.update(((IPluginElement) changeObject).getParent(), null);
		}
	}

	@Override
	public void setFocus() {
		fLibraryTable.getTable().setFocus();
	}

	@Override
	protected void doPaste(Object targetObject, Object[] sourceObjects) {
		// Get the model
		IPluginModelBase model = getModel();
		IPluginBase plugin = model.getPluginBase();
		try {
			// Paste all source objects
			for (Object sourceObject : sourceObjects) {
				if (sourceObject instanceof PluginLibrary library) {
					// Plugin library object
					// Adjust all the source object transient field values to
					// acceptable values
					library.reconnect(model, plugin);
					// Add the library to the plug-in
					plugin.add(library);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	@Override
	protected boolean canPaste(Object targetObject, Object[] sourceObjects) {
		Set<IPath> librarySet = null;
		// Only source objects that are plugin libraries that have not already
		// been specified can be pasted
		for (Object sourceObject : sourceObjects) {
			// Only plugin libraries are allowed
			if ((sourceObject instanceof IPluginLibrary) == false) {
				return false;
			}
			// We have a plugin library
			// Get the current libraries already specified and store them in
			// a set to assist with searching
			if (librarySet == null) {
				librarySet = createPluginLibrarySet();
			}
			// No duplicate libraries are allowed
			IPluginLibrary library = (IPluginLibrary) sourceObject;
			if (librarySet.contains(IPath.fromOSString(ClasspathUtilCore.expandLibraryName(library.getName())))) {
				return false;
			}
		}
		return true;
	}

	private Set<IPath> createPluginLibrarySet() {
		// Get the current libraries and add them to a set for easy searching
		IPluginLibrary[] libraries = getModel().getPluginBase().getLibraries();
		Set<IPath> librarySet = new HashSet<>();
		for (IPluginLibrary library : libraries) {
			librarySet.add(IPath.fromOSString(ClasspathUtilCore.expandLibraryName(library.getName())));
		}
		return librarySet;
	}

	@Override
	protected void entryModified(Object entry, String value) {
		try {
			IPluginModelBase model = getModel();
			IProject project = model.getUnderlyingResource().getProject();
			IPluginLibrary library = (IPluginLibrary) entry;
			model.getPluginBase().remove(library);
			String[] oldValue = {library.getName()};
			String[] newValue = {value};
			library.setName(value);
			boolean memberExists = project.findMember(value) != null;
			updateBuildProperties(oldValue, newValue, !memberExists);
			updateJavaClasspathLibs(oldValue, memberExists ? newValue : new String[] {null});
			model.getPluginBase().add(library);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private IPluginModelBase getModel() {
		if (getPage().getModel() instanceof IPluginModelBase base) {
			return base;
		}
		return null;
	}

	@Override
	protected boolean isDragAndDropEnabled() {
		return true;
	}

	@Override
	public boolean canDragMove(Object[] sourceObjects) {
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourceObjects
	 */
	private boolean validateDragMoveSanity(Object[] sourceObjects) {
		// Validate source
		if (sourceObjects == null) {
			// No objects
			return false;
		} else if (sourceObjects.length != 1) {
			// Multiple selection not supported
			return false;
		} else if ((sourceObjects[0] instanceof IPluginLibrary) == false) {
			// Must be the right type
			return false;
		}
		return true;
	}

	@Override
	public boolean canDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			return false;
		}
		// Multiple selection not supported
		IPluginLibrary sourcePluginLibrary = (IPluginLibrary) sourceObjects[0];
		IPluginLibrary targetPluginLibrary = (IPluginLibrary) targetObject;
		// Validate model
		if (validateDropMoveModel(sourcePluginLibrary, targetPluginLibrary) == false) {
			return false;
		}
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getPluginBase();
		// Validate move
		if (targetLocation == ViewerDropAdapter.LOCATION_BEFORE) {
			// Get the previous element of the target
			IPluginLibrary previousLibrary = bundlePluginBase.getPreviousLibrary(targetPluginLibrary);
			// Ensure the previous element is not the source
			if (sourcePluginLibrary.equals(previousLibrary)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			// Get the next element of the target
			IPluginLibrary nextLibrary = bundlePluginBase.getNextLibrary(targetPluginLibrary);
			// Ensure the next element is not the source
			if (sourcePluginLibrary.equals(nextLibrary)) {
				return false;
			}
			return true;
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
			return false;
		}
		return false;
	}

	/**
	 * @param targetObject
	 * @param sourceObjects
	 */
	private boolean validateDropMoveSanity(Object targetObject, Object[] sourceObjects) {
		// Validate target object
		if ((targetObject instanceof IPluginLibrary) == false) {
			return false;
		}
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return false;
		}
		return true;
	}

	/**
	 * @param sourcePluginLibrary
	 * @param targetPluginLibrary
	 */
	private boolean validateDropMoveModel(IPluginLibrary sourcePluginLibrary, IPluginLibrary targetPluginLibrary) {
		// Objects have to be from the same model
		ISharedPluginModel sourceModel = sourcePluginLibrary.getModel();
		ISharedPluginModel targetModel = targetPluginLibrary.getModel();
		if (sourceModel.equals(targetModel) == false) {
			return false;
		} else if ((getModel().getPluginBase() instanceof BundlePluginBase) == false) {
			return false;
		}
		return true;
	}

	@Override
	public void doDropMove(Object targetObject, Object[] sourceObjects, int targetLocation) {
		// Sanity check
		if (validateDropMoveSanity(targetObject, sourceObjects) == false) {
			Display.getDefault().beep();
			return;
		}
		// Multiple selection not supported
		IPluginLibrary sourcePluginLibrary = (IPluginLibrary) sourceObjects[0];
		IPluginLibrary targetPluginLibrary = (IPluginLibrary) targetObject;
		// Validate move
		if ((targetLocation == ViewerDropAdapter.LOCATION_BEFORE) || (targetLocation == ViewerDropAdapter.LOCATION_AFTER)) {
			// Do move
			doDropMove(sourcePluginLibrary, targetPluginLibrary, targetLocation);
		} else if (targetLocation == ViewerDropAdapter.LOCATION_ON) {
			// Not supported
		}
	}

	/**
	 * @param sourcePluginLibrary
	 * @param targetPluginLibrary
	 * @param targetLocation
	 */
	private void doDropMove(IPluginLibrary sourcePluginLibrary, IPluginLibrary targetPluginLibrary, int targetLocation) {
		// Remove the original source object
		// Normally we remove the original source object after inserting the
		// serialized source object; however, the libraries are removed via ID
		// and having both objects with the same ID co-existing will confound
		// the remove operation
		doDragRemove();
		// Get the bundle plug-in base
		IPluginModelBase model = getModel();
		BundlePluginBase bundlePluginBase = (BundlePluginBase) model.getPluginBase();
		// Get the index of the target
		int index = bundlePluginBase.getIndexOf(targetPluginLibrary);
		// Ensure the target index was found
		if (index == -1) {
			return;
		}
		// Determine the location index
		int targetIndex = index;
		if (targetLocation == ViewerDropAdapter.LOCATION_AFTER) {
			targetIndex++;
		}
		// Ensure the plugin library is concrete
		if ((sourcePluginLibrary instanceof PluginLibrary) == false) {
			return;
		}
		// Adjust all the source object transient field values to
		// acceptable values
		((PluginLibrary) sourcePluginLibrary).reconnect(model, bundlePluginBase);
		// Add source as sibling of target
		try {
			bundlePluginBase.add(sourcePluginLibrary, targetIndex);
		} catch (CoreException e) {
			// CoreException only if model is not editable, which should never be the case
		}
	}

	/**
	 *
	 */
	private void doDragRemove() {
		// Get the bundle plug-in base
		BundlePluginBase bundlePluginBase = (BundlePluginBase) getModel().getPluginBase();
		// Retrieve the original non-serialized source objects dragged initially
		Object[] sourceObjects = getDragSourceObjects();
		// Validate source objects
		if (validateDragMoveSanity(sourceObjects) == false) {
			return;
		}
		// Remove the library
		IPluginLibrary sourcePluginLibrary = (IPluginLibrary) sourceObjects[0];
		try {
			bundlePluginBase.remove(sourcePluginLibrary);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

}
