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

package org.eclipse.pde.internal.ui.editor.build;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.context.InputContext;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.wizards.FolderSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.RenameDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class RuntimeInfoSection extends PDESection
implements
IModelChangedListener {
	
	protected TableViewer fLibraryViewer;
	protected TableViewer fFolderViewer;
	
	protected StructuredViewerPart fLibraryPart;
	protected StructuredViewerPart fFolderPart;
	private IBuildEntry fCurrentLibrary;
	private IStructuredSelection fCurrentSelection;
	
	private boolean fEnabled = true;
	
	class RenameAction extends Action {
		
		public RenameAction() {
			super(PDEUIMessages.EditableTablePart_renameAction); 
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
				return libList.toArray();
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
		getSection().setText(PDEUIMessages.BuildEditor_RuntimeInfoSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_RuntimeInfoSection_desc);
		getBuildModel().addModelChangedListener(this);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	
	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
		.findContext(BuildInputContext.CONTEXT_ID);
		if (context==null)
			return null;
		return (IBuildModel) context.getModel();
	}
	
	protected void handleLibInBinBuild(boolean isSelected, String libName) {
		IBuildModel model = getBuildModel();
		IBuildEntry binIncl = model.getBuild().getEntry(
				IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
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
				binIncl = model.getFactory().createEntry(
						IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
				model.getBuild().add(binIncl);
			}
			if (libPath != null){
				if (!isSelected && libPath.segmentCount() == 1	&& binIncl.contains("*.jar")) { //$NON-NLS-1$
					addAllJarsToBinIncludes(binIncl, project, model);
				} else if (!isSelected && libPath.segmentCount() > 1){
					IPath parent = libPath.removeLastSegments(1);
					String parentPath = parent.toString() + IPath.SEPARATOR;
					if (binIncl.contains(parentPath) && !project.exists(parent)){
						binIncl.removeToken(parentPath);
					} else if (parent.segmentCount() > 1){
						parent = parent.removeLastSegments(1);
						parentPath = parent.toString() + IPath.SEPARATOR;
						if (binIncl.contains(parentPath) && !project.exists(parent))
							binIncl.removeToken(parentPath);
					}
				}
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
						&& members[i].getFileExtension().equals("jar")) { //$NON-NLS-1$
					binIncl.addToken(members[i].getName());
				}
			}
			
			IBuildEntry[] libraries = BuildUtil.getBuildLibraries(model
					.getBuild().getBuildEntries());
			if (libraries.length != 0) {
				for (int j = 0; j < libraries.length; j++) {
					String libraryName = libraries[j].getName().substring(7);
					IPath path = project.getFile(libraryName).getProjectRelativePath();
					if (path.segmentCount() == 1
							&& !binIncl.contains(libraryName))
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
				PDEUIMessages.BuildEditor_RuntimeInfoSection_addLibrary, null,
				PDEUIMessages.ManifestEditor_LibrarySection_up,
				PDEUIMessages.ManifestEditor_LibrarySection_down});
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
		
		fFolderPart = new PartAdapter(new String[]{PDEUIMessages.BuildEditor_RuntimeInfoSection_addFolder});
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
			Action newAction = new Action(PDEUIMessages.BuildEditor_RuntimeInfoSection_popupFolder) {
				
				public void run() {
					handleNewFolder();
				}
			};
			newAction.setEnabled(fEnabled);
			manager.add(newAction);
		}
		
		manager.add(new Separator());
		Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
			
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
		Action newAction = new Action(PDEUIMessages.BuildEditor_RuntimeInfoSection_popupAdd) {
			
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
		
		Action deleteAction = new Action(PDEUIMessages.Actions_delete_label) {
			
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
			if (!newValue.endsWith(".jar") && !newValue.endsWith("/") && !newValue.equals(IBuildEntry.JAR_PREFIX + ".")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				newValue +="/"; //$NON-NLS-1$
			
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
            dialog.setTitle(PDEUIMessages.RuntimeInfoSection_rename); 
			dialog.getShell().setSize(300, 150);
			if (dialog.open() == Window.OK) {
				entryModified(entry, dialog.getNewName());
			}
		}
	}
	
	public void dispose() {
		IBuildModel buildModel = getBuildModel();
		if (buildModel!=null)
			buildModel.removeModelChangedListener(this);
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
						outputFolders.add("bin"); //$NON-NLS-1$
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
			String name = ((IBuildEntry) item).getName();
			if (name.startsWith(IBuildEntry.JAR_PREFIX))
				name = name.substring(IBuildEntry.JAR_PREFIX.length());
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
	
	
	protected boolean isParentIncluded(IPath libPath, IBuildEntry binIncl,
			IBuildEntry binExcl) {
		while (libPath.segmentCount() > 1) {
			libPath = libPath.removeLastSegments(1);
			if (binIncl.contains(libPath.toString() + IPath.SEPARATOR))
				return true;
			else if (binExcl != null
					&& binExcl.contains(libPath.toString() + IPath.SEPARATOR))
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
				dialog.getShell().setText(PDEUIMessages.RuntimeInfoSection_addEntry);  
				
				try {
					if (dialog.open() == Window.OK) {
						
						String name = dialog.getNewName();
						
						if (!name.endsWith(".jar") && !name.equals(".") && !name.endsWith("/")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							name += "/"; //$NON-NLS-1$
						
						String keyName = name;
						if (!keyName.startsWith(IBuildEntry.JAR_PREFIX))
							keyName = IBuildEntry.JAR_PREFIX + name;
						if (name.startsWith(IBuildEntry.JAR_PREFIX))
							name = name.substring(7);
						
						if (!name.endsWith(".")) //$NON-NLS-1$
							handleLibInBinBuild(true, name);
						
						// add library to jars compile order
						IBuildEntry jarOrderEntry = build
						.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
						int numLib = fLibraryViewer.getTable().getItemCount();
						
						if (jarOrderEntry == null) {
							jarOrderEntry = getBuildModel()
							.getFactory()
							.createEntry(
									IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
							
							// add all runtime libraries to compile order
							for (int i = 0; i < numLib; i++) {
								String lib = ((IBuildEntry) fLibraryViewer
										.getElementAt(i)).getName().substring(7);
								jarOrderEntry.addToken(lib);
							}
							jarOrderEntry.addToken(name);
							build.add(jarOrderEntry);
						} else if (jarOrderEntry.getTokens().length < numLib){
							
							// remove and re-add all runtime libraries to compile order
							String[] tokens = jarOrderEntry.getTokens();
							for (int i = 0; i<tokens.length; i++){
								jarOrderEntry.removeToken(tokens[i]);
							}
							for (int i = 0; i < numLib; i++) {
								String lib = ((IBuildEntry) fLibraryViewer
										.getElementAt(i)).getName().substring(7);
								jarOrderEntry.addToken(lib);
							}
							jarOrderEntry.addToken(name);
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
				int numLib = fLibraryViewer.getTable().getItemCount();
				
				if (entry == null) {
					entry = getBuildModel().getFactory().createEntry(
							IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
					
					// add all runtime libraries to compile order
					for (int i = 0; i < numLib; i++) {
						String lib = ((IBuildEntry) fLibraryViewer
								.getElementAt(i)).getName().substring(7);
						entry.addToken(lib);
					}
					build.add(entry);
				} else if (entry.getTokens().length < numLib){
					
					// remove and re-add all runtime libraries to compile order
					String[] tokens = entry.getTokens();
					for (int i = 0; i<tokens.length; i++){
						entry.removeToken(tokens[i]);
					}
					
					for (int i = 0; i < numLib; i++) {
						String lib = ((IBuildEntry) fLibraryViewer
								.getElementAt(i)).getName().substring(7);
						entry.addToken(lib);
					}
				} 
				
				entry.removeToken(libName);
				
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
					else
						fCurrentSelection = null;
					fCurrentLibrary = entry;
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
		dialog.setTitle(PDEUIMessages.ManifestEditor_JarsSection_dialogTitle); 
		dialog.setMessage(PDEUIMessages.ManifestEditor_JarsSection_dialogMessage); 
		
		dialog.setValidator(new ISelectionStatusValidator() {
			
			public IStatus validate(Object[] selection) {
				if (selection == null || selection.length != 1
						|| !(selection[0] instanceof IFolder))
					return new Status(IStatus.ERROR, PDEPlugin.getPluginId(),
							IStatus.ERROR, "", null); //$NON-NLS-1$
				
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
							PDEUIMessages.BuildEditor_RuntimeInfoSection_duplicateFolder, 
							null);
				
				return new Status(IStatus.OK, PDEPlugin.getPluginId(),
						IStatus.OK, "", null); //$NON-NLS-1$
			}
		});
		
		if (dialog.open() == Window.OK) {
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
		}
		
		
		
		if (type == IModelChangedEvent.INSERT){
//			account for new key
			fLibraryViewer.refresh();
			if (fCurrentSelection != null) {
				fLibraryViewer.setSelection(fCurrentSelection);
				updateDirectionalButtons();
			} else {
				fFolderPart.setButtonEnabled(0, false);
				fLibraryViewer.setSelection(null);
				fFolderViewer.setInput(null);
			}
		} else if (type == IModelChangedEvent.REMOVE){
			// account for key removal
			fLibraryViewer.remove(entry);
			fLibraryViewer.refresh();
			fFolderPart.setButtonEnabled(0, false);
			fLibraryViewer.setSelection(null);
			fFolderViewer.setInput(null);
		} else if (keyName!=null && keyName.startsWith(IBuildEntry.JAR_PREFIX)){ 
			// modification to source.{libname}.jar
			// renaming token
			if (event.getOldValue() != null && event.getNewValue() != null){
				fLibraryViewer.update(entry, null);
				return;
			} 
			// add/remove source folder
			refresh();
			if (fCurrentSelection != null) {
				fFolderViewer.setSelection(fCurrentSelection);
				updateDirectionalButtons();
			} else {
				fFolderPart.setButtonEnabled(0, false);
				fLibraryViewer.setSelection(null);
				fFolderViewer.setInput(null);
			}
			if (fCurrentLibrary != null)
				update(fCurrentLibrary);		
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
