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
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
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
import org.eclipse.ui.model.*;
import org.eclipse.update.ui.forms.internal.*;

public class RuntimeInfoSection
	extends PDEFormSection
	implements IModelChangedListener {

	public static final String SECTION_TITLE =
		"BuildPropertiesEditor.RuntimeInfoSection.title";
	public static final String SECTION_DESC =
		"BuildPropertiesEditor.RuntimeInfoSection.desc";
	public static final String SECTION_NEW =
		"BuildPropertiesEditor.RuntimeInfoSection.addLibrary";
	public static final String SECTION_UP = "ManifestEditor.LibrarySection.up";
	public static final String SECTION_DOWN =
		"ManifestEditor.LibrarySection.down";
	public static final String POPUP_NEW_LIBRARY =
		"ManifestEditor.LibrarySection.newLibrary";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String NEW_LIBRARY_ENTRY =
		"ManifestEditor.LibrarySection.newLibraryEntry";
	public static final String JSECTION_NEW = "BuildPropertiesEditor.RuntimeInfoSection.addFolder";
	public static final String POPUP_NEW_FOLDER =
		"ManifestEditor.JarsSection.newFolder";
	public static final String JAR_INCLUDE =
		"BuildPropertiesEditor.RuntimeInfoSection.buildInclude";
	public Image libImage;
	public Image jarsImage;
	protected TableViewer libraryViewer;
	protected TableViewer foldersViewer;
	protected Control sectionControl;

	protected StructuredViewerPart libraryPart;
	protected StructuredViewerPart foldersPart;
	private IBuildEntry currentLibrary;
	private Button jarIncludeButton;
	private PDEFormPage page;

	class RenameAction extends Action {
		public RenameAction() {
			super(PDEPlugin.getResourceString("EditableTablePart.renameAction")); //$NON-NLS-1$
		}
		public void run() {
			doRename();
		}
		
	}

	class PartAdapter extends EditableTablePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}
		public void entryModified(Object entry, String value) {

		}
		public void selectionChanged(IStructuredSelection selection) {
			if (selection.size() != 0)
				RuntimeInfoSection.this.selectionChanged(selection);
		}
		public void handleDoubleClick(IStructuredSelection selection) {
			RuntimeInfoSection.this.handleDoubleClick(selection);
		}
		public void buttonSelected(Button button, int index) {

			if (this.getViewer() == libraryPart.getViewer()) {
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
			} else if (this.getViewer() == foldersPart.getViewer()) {
				if (index == 0)
					handleJarsNew();
			} else {
				button.getShell().setDefaultButton(null);
			}
		}
	}

	public class LibTableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {

		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel) parent).getBuild();
				IBuildEntry jarOrderEntry =
					build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
				IBuildEntry[] libraries =
					BuildUtil.getBuildLibraries(build.getBuildEntries());
				if (jarOrderEntry == null) {
					return libraries;
				}

				Vector libList = new Vector();
				String[] tokens = jarOrderEntry.getTokens();
				for (int i = 0; i < tokens.length; i++) {
					IBuildEntry entry = build.getEntry(IBuildEntry.JAR_PREFIX + tokens[i]);
					if (entry!=null)
						libList.add(entry);
				}
				for (int i =0; i<libraries.length;i++){
					if (!libList.contains(libraries[i]))
						libList.add(libraries[i]);
				}
				return (IBuildEntry[])libList.toArray(new IBuildEntry[libList.size()]);
			}
			return new Object[0];
		}
	}

	public class LibTableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj.toString().startsWith(IBuildEntry.JAR_PREFIX))
				return obj.toString().substring(7);
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return libImage;
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

	public class JarsTableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildEntry) {

				IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
				IBuild build = buildModel.getBuild();
				IBuildEntry entry =
					build.getEntry(((IBuildEntry) parent).getName());
				if (entry != null) {
					return entry.getTokens();
				}
			}
			return new Object[0];
		}
	}

	public class JarsTableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return jarsImage;
		}
	}

	public RuntimeInfoSection(PDEFormPage page) {
		super(page);
		this.page = page;

		setAddSeparator(true);
		setHeaderPainted(true);
		setDescriptionPainted(true);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}

	protected void handleLibInBinBuild(boolean isSelected){
		String libName = currentLibrary.getName().substring(7);
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuildEntry binIncl = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IProject project = model.getUnderlyingResource().getProject();
		IPath libPath = project.getFile(libName).getProjectRelativePath();
		
		try {
			if (binIncl == null && !isSelected)
				return;
			if (binIncl == null){
				binIncl = model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
				model.getBuild().add(binIncl);
			}
			if (!isSelected && libPath.segmentCount() == 1 && binIncl.contains("*.jar")){
				addAllJarsToBinIncludes(binIncl, project, model);
			} 
			if (isSelected && !binIncl.contains(libName)){
				binIncl.addToken(libName);
			} else if (!isSelected && binIncl.contains(libName)){
				binIncl.removeToken(libName);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		
	}
	
	protected void addAllJarsToBinIncludes(IBuildEntry binIncl, IProject project, IBuildModel model){
		try {
			IResource[] members = project.members();
			for (int i=0; i<members.length; i++){
				if (!(members[i] instanceof IFolder) && members[i].getFileExtension().equals("jar")){
					binIncl.addToken(members[i].getName());
				}
			}
					
			IBuildEntry[] libraries = BuildUtil.getBuildLibraries(model.getBuild().getBuildEntries());
			if (libraries.length!=0){
				for (int j=0; j<libraries.length; j++){
					String libraryName = libraries[j].getName().substring(7);
					IPath path = project.getFile(libraryName).getProjectRelativePath();
					if (path.segmentCount()==1 && !binIncl.contains(libraryName))
						binIncl.addToken(libraryName);
				}
			}
			binIncl.removeToken("*.jar");
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	public void initialize(Object input) {
		IBuildModel model = (IBuildModel) input;
		setReadOnly(false);
		model.addModelChangedListener(this);
	}

	private IBuildEntry createOutputKey(String libName){
		IBuildModel buildModel = (IBuildModel)getFormPage().getModel();
		IBuild build = buildModel.getBuild();
		String outputName = IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX + libName;
		IBuildEntry outputEntry = build.getEntry(outputName);
		
		try {
			if (outputEntry != null)
				build.remove(outputEntry);	
			
			outputEntry = buildModel.getFactory().createEntry(outputName);
			build.add(outputEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		
		return outputEntry;

	}	
	
	private void setOutputEntryTokens(Set outputFolders, IBuildEntry outputEntry){
		Iterator iter = outputFolders.iterator();
		try {
			while(iter.hasNext()){
				String outputFolder = iter.next().toString();
				if (!outputFolder.endsWith(""+ Path.SEPARATOR))
					outputFolder = outputFolder.concat(""+Path.SEPARATOR);
				outputEntry.addToken(outputFolder.toString());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	protected StructuredViewerPart createViewerPart(String[] buttonLabels) {
		EditableTablePart tablePart = new PartAdapter(buttonLabels);
		tablePart.setEditable(true);
		return tablePart;
	}

	private void intializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		libImage = provider.get(PDEPluginImages.DESC_JAVA_LIB_OBJ);

		IWorkbench workbench = PlatformUI.getWorkbench();
		ISharedImages sharedImages = workbench.getSharedImages();
		jarsImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
	}
	
	private IPackageFragmentRoot[] computeSourceFolders() {
		ArrayList folders = new ArrayList();
		IBuildModel buildModel = (IBuildModel)getFormPage().getModel();
		IProject project = buildModel.getUnderlyingResource().getProject();
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jProject = JavaCore.create(project);
				IPackageFragmentRoot[] roots =
					jProject.getPackageFragmentRoots();
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
		return (IPackageFragmentRoot[]) folders.toArray(
			new IPackageFragmentRoot[folders.size()]);
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {

		intializeImages();
		
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 0;
		layout.makeColumnsEqualWidth = true;
		container.setLayout(layout);
		
		createLeftSection(container, factory);
		createRightSection(container, factory);

		jarIncludeButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(JAR_INCLUDE),
				SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		jarIncludeButton.setLayoutData(gd);
		jarIncludeButton.setVisible(false);
		jarIncludeButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				handleLibInBinBuild(jarIncludeButton.getSelection());
			}
		});

		factory.paintBordersFor(container);

		return container;
	}
	
	private void createLeftSection(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 2;
		layout.numColumns = 2;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		container.setLayoutData(gd);
		
		libraryPart =
			createViewerPart(
				new String[] {
					PDEPlugin.getResourceString(SECTION_NEW),
					null,
					PDEPlugin.getResourceString(SECTION_UP),
					PDEPlugin.getResourceString(SECTION_DOWN)});
		libraryPart.createControl(container, SWT.FULL_SELECTION, 2, factory);
		((EditableTablePart)libraryPart).setEditable(true);
		libraryViewer = (TableViewer)libraryPart.getViewer();
		libraryViewer.setContentProvider(new LibTableContentProvider());
		libraryViewer.setLabelProvider(new LibTableLabelProvider());
		libraryPart.setButtonEnabled(2, false);
		libraryPart.setButtonEnabled(3, false);
		libraryViewer.setInput(getFormPage().getModel());	
		factory.paintBordersFor(container);
		
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillLibContextMenu(manager);
			}
		});

		libraryViewer.getControl().setMenu(
			menuMgr.createContextMenu(libraryViewer.getControl()));
	}
	

	private void createRightSection(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = layout.marginWidth = 2;
		container.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 100;
		container.setLayoutData(gd);
		
		foldersPart =
			createViewerPart(new String[] { PDEPlugin.getResourceString(JSECTION_NEW)});
		foldersPart.createControl(container, SWT.FULL_SELECTION, 2, factory);
		((EditableTablePart)foldersPart).setEditable(true);
		foldersViewer = (TableViewer)foldersPart.getViewer();
		foldersViewer.setContentProvider(new JarsTableContentProvider());
		foldersViewer.setLabelProvider(new JarsTableLabelProvider());
		factory.paintBordersFor(container);		

		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillJarsContextMenu(manager);
			}
		});
		foldersViewer.getControl().setMenu(
			menuMgr.createContextMenu(foldersViewer.getControl()));
	}

	protected EditableTablePart getLibTablePart() {
		return (EditableTablePart) libraryPart;
	}

	protected EditableTablePart getJarsTablePart() {
		return (EditableTablePart) foldersPart;
	}

	protected void fillJarsContextMenu(IMenuManager manager) {
		ISelection selection = foldersViewer.getSelection();

		if (currentLibrary != null) {
			Action newAction =
				new Action(PDEPlugin.getResourceString(POPUP_NEW_FOLDER)) {
				public void run() {
					handleJarsNew();
				}
			};
			newAction.setEnabled(true);
			manager.add(newAction);
		}

		manager.add(new Separator());
		Action deleteAction =
			new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
			public void run() {
				handleJarsDelete();
			}
		};
		deleteAction.setEnabled(!selection.isEmpty());
		manager.add(deleteAction);

		manager.add(new Separator());
		// defect 19550
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager,
			false);

	}

	protected void fillLibContextMenu(IMenuManager manager) {

		ISelection selection = libraryViewer.getSelection();

		Action newAction =
			new Action(PDEPlugin.getResourceString(POPUP_NEW_LIBRARY)) {
			public void run() {
				handleNew();
			}
		};

		newAction.setEnabled(true);
		manager.add(newAction);

		manager.add(new Separator());
		IAction renameAction = new RenameAction();
		renameAction.setEnabled(!selection.isEmpty());
		manager.add(renameAction);

		Action deleteAction =
			new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
			public void run() {
				handleDelete();
			}
		};

		deleteAction.setEnabled(!selection.isEmpty());
		manager.add(deleteAction);

		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager,
			false);
	}

	protected void entryModified(IBuildEntry oldEntry, String newValue) {
		final IBuildEntry entry = oldEntry;
		IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
		IBuild build = buildModel.getBuild();
		String oldName = entry.getName().substring(7);
		try {
			if (newValue.equals(entry.getName()))
				return;
			if (!newValue.startsWith(IBuildEntry.JAR_PREFIX))
				newValue = IBuildEntry.JAR_PREFIX + newValue;
			if (!newValue.endsWith(".jar"))
				newValue = newValue + ".jar";
			entry.setName(newValue);

			// jars.compile.order
			IBuildEntry tempEntry =
				build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
			if (tempEntry !=null)
				tempEntry.renameToken(oldName, newValue.substring(7));
				
			// output.{source folder}.jar				
			tempEntry = build.getEntry(IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX + oldName);
			if (tempEntry!=null){
				build.remove(tempEntry);
				refreshOutputKeys();
			}
			// bin.includes
			tempEntry = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
			if (tempEntry!=null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newValue.substring(7));
				
			// bin.excludes
			tempEntry = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
			if (tempEntry!=null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newValue.substring(7));
			
			libraryViewer.getTable().getDisplay().asyncExec(new Runnable() {
				public void run() {
					libraryViewer.update(entry, null);
				}
			});
			libraryViewer.refresh();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void expandTo(Object object) {
		libraryViewer.setSelection(new StructuredSelection(object), true);
	}

	public void handleDoubleClick(IStructuredSelection selection) {
		
	}
	public void enableSection() {
		EditableTablePart tablePart = getLibTablePart();
		tablePart.setEnabled(true);
		tablePart.setButtonEnabled(0, true);
		tablePart.setButtonEnabled(2, false);
		tablePart.setButtonEnabled(3, false);

		tablePart = getJarsTablePart();
		tablePart.setEnabled(true);
		tablePart.setButtonEnabled(0, false);

		jarIncludeButton.setEnabled(true);
	}
	public void disableSection() {
		getJarsTablePart().setEnabled(false);
		getLibTablePart().setEnabled(false);
		jarIncludeButton.setEnabled(false);
	}

	public void setSectionControl(Control control) {
		sectionControl = control;
	}

	public Control getSectionControl() {
		return sectionControl;
	}
	
	public boolean doGlobalAction(String actionId) {
		IStructuredSelection currentSelection = (IStructuredSelection)getFormPage().getSelection();
		
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			if (currentSelection.getFirstElement().toString().startsWith(IBuildEntry.JAR_PREFIX))
				handleDelete();
			else
				handleJarsDelete();
			return true;
		}
		return false;
	}
	
	private void doRename() {
		IStructuredSelection selection = (IStructuredSelection)libraryViewer.getSelection();
		if (selection.size()==1) {
			IBuildEntry entry = (IBuildEntry)selection.getFirstElement();
			String oldName = entry.getName().substring(7);
			RenameDialog dialog = new RenameDialog(libraryViewer.getControl().getShell(), oldName);
			dialog.create();
			dialog.getShell().setText(PDEPlugin.getResourceString("EditableTablePart.renameTitle")); //$NON-NLS-1$
			dialog.getShell().setSize(300, 150);
			if (dialog.open()==Dialog.OK) {
				entryModified(entry, dialog.getNewName());
			}
		}
	}

	public void dispose() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	
	private void refreshOutputKeys() {
		if (!isJavaProject())
			return;
		IPackageFragmentRoot[] sourceFolders = computeSourceFolders();

		String[] jarFolders = currentLibrary.getTokens();
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
			if (outputFolders.size()!=0){
				IBuildEntry outputEntry = createOutputKey(currentLibrary.getName().substring(7));
				setOutputEntryTokens(outputFolders, outputEntry);
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}

	}
	
	private boolean isJavaProject() {
		try {
			IBuildModel buildModel = (IBuildModel)page.getModel();
			IProject project = buildModel.getUnderlyingResource().getProject();
			if (project.hasNature(JavaCore.NATURE_ID))
				return true;
		} catch (CoreException e) {
		}
		return false;
	}

	public void sectionChanged(Object changeObject) {
		IBuildEntry variable = (IBuildEntry) changeObject;
		update(variable);
	}

	private void update(IBuildEntry variable) {
		currentLibrary = variable;
		foldersViewer.setInput(currentLibrary);
		getJarsTablePart().setButtonEnabled(
			0,
			!isReadOnly() && variable != null);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		if (item.toString().startsWith(IBuildEntry.JAR_PREFIX)) {
			sectionChanged(item);
			updateDirectionalButtons();
			jarIncludeButton.setVisible(true);
			jarIncludeButton.setSelection(isJarIncluded(item.toString().substring(7)));
		}
		getFormPage().setSelection(selection);
	}

	protected void updateDirectionalButtons() {
		Table table = libraryViewer.getTable();
		TableItem[] selection = table.getSelection();
		boolean hasSelection = selection.length > 0;
		boolean canMove = table.getItemCount() > 1;
		TablePart tablePart = getLibTablePart();
		tablePart.setButtonEnabled(
			2,
			canMove && hasSelection && table.getSelectionIndex() > 0);
		tablePart.setButtonEnabled(
			3,
			canMove
				&& hasSelection
				&& table.getSelectionIndex() < table.getItemCount() - 1);
	}

	private boolean isJarIncluded(String libName) {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		IPath libPath = project.getFile(libName).getProjectRelativePath();
		IBuildEntry binIncl = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		IBuildEntry binExcl = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
		if (binIncl == null)
			return false;
			
		if (libPath.segmentCount() ==1){
			return binIncl.contains(libName) || binIncl.contains("*.jar");
		} else if (binIncl.contains(libName)){
			return true;
		} else if (binExcl!=null && binExcl.contains(libName)){
			return false;
		} else {
			return isParentIncluded(libPath, binIncl, binExcl);
		}
	}

	protected boolean isParentIncluded(IPath libPath, IBuildEntry binIncl, IBuildEntry binExcl){
		while (libPath.segmentCount()>1){
			libPath = libPath.removeLastSegments(1);
			if (binIncl.contains(libPath.toString() + Path.SEPARATOR))
				return true;
			else if (binExcl!=null && binExcl.contains(libPath.toString() + Path.SEPARATOR))
				return false;
		}
		return false;
	}
	
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			libraryViewer.refresh();
			foldersViewer.refresh();
			libraryViewer.setSelection(null);
			foldersViewer.setInput(null);
			getJarsTablePart().setButtonEnabled(0,false);
			jarIncludeButton.setVisible(false);
			updateDirectionalButtons();
		}
	}

	protected String[] getLibraryNames(){
		String[] libNames = new String[libraryViewer.getTable().getItemCount()];
		for (int i =0 ; i<libNames.length; i++)
			libNames[i] = libraryViewer.getTable().getItem(i).getText();
		return libNames;
	}

	protected void handleNew() {
		final String[] libNames = getLibraryNames();
		final IPluginModelBase pluginModelBase;
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IProject project = model.getUnderlyingResource().getProject();
		IModel pluginModel = PDECore.getDefault().getWorkspaceModelManager().getWorkspaceModel(project);
		if (pluginModel instanceof IPluginModelBase)
			pluginModelBase = (IPluginModelBase)pluginModel;
		else
			pluginModelBase = null;

		BusyIndicator.showWhile(libraryViewer.getTable().getDisplay(), new Runnable() {
			public void run(){
				IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
				IBuild build = buildModel.getBuild();
				AddLibraryDialog dialog =
					new AddLibraryDialog(
						getFormPage().getControl().getShell(), libNames, pluginModelBase);
				dialog.create();
				dialog.getShell().setText("Add Entry"); //$NON-NLS-1$
				dialog.getShell().setSize(300, 350);
				
				try {
					if (dialog.open() == Dialog.OK) {
						
						String name = dialog.getNewName();
						if (!name.endsWith(".jar"))
							name = name + ".jar";

						if (!name.startsWith(IBuildEntry.JAR_PREFIX))
							name = IBuildEntry.JAR_PREFIX + name;

						IBuildEntry library = buildModel.getFactory().createEntry(name);
						build.add(library);
						libraryViewer.refresh();
						libraryViewer.setSelection(new StructuredSelection(library));
						jarIncludeButton.setSelection(true);
						handleLibInBinBuild(true);

						if (libraryViewer.getTable().getItemCount()>1)
							updateJarsCompileOrder(libraryViewer.getTable().getItems());
						
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	}
	
	private IPackageFragmentRoot getSourceFolder(
		String folderName,
		IPackageFragmentRoot[] sourceFolders) {
		for (int i = 0; i < sourceFolders.length; i++) {
			if (sourceFolders[i]
				.getPath()
				.removeFirstSegments(1)
				.equals(new Path(folderName))) {
				return sourceFolders[i];
			}
		}
		return null;
	}
	protected void handleDelete() {
		int index = libraryViewer.getTable().getSelectionIndex();
		if (index!=-1){
			String libName = libraryViewer.getTable().getItem(index).getText();
			IBuild build = ((IBuildModel)getFormPage().getModel()).getBuild();
			
			try {
				// jars.compile.order
				IBuildEntry entry =
					build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
				if (entry !=null)
					entry.removeToken(libName);
				
				// output.{source folder}.jar				
				entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX + libName);
				if (entry!=null)
					build.remove(entry);
					
				// bin.includes
				entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_INCLUDES);
				if (entry!=null && entry.contains(libName))
					entry.removeToken(libName);
				
				// bin.excludes
				entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_BIN_EXCLUDES);
				if (entry!=null && entry.contains(libName))
					entry.removeToken(libName);
 
				build.remove(build.getEntry(IBuildEntry.JAR_PREFIX + libName));
				libraryViewer.refresh();
				
				int libCount = libraryViewer.getTable().getItemCount();
				if (libCount > index) {
					libName = libraryViewer.getElementAt(index).toString();
				} else if (libCount==index && libCount != 0){
					libName = libraryViewer.getElementAt(index-1).toString();
				} else {
					libName="";
				}
				
				IBuildEntry selection = build.getEntry(libName);
				if (selection!=null){
					libraryViewer.setSelection(new StructuredSelection(selection));
				}else{ 
					getJarsTablePart().setButtonEnabled(0,false);
					libraryViewer.setSelection(null);
					foldersViewer.setInput(null);
					jarIncludeButton.setVisible(false);
					currentLibrary = null;
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private void handleJarsDelete() {
		IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
		int index = foldersViewer.getTable().getSelectionIndex();
		Object object =
			((IStructuredSelection) foldersViewer.getSelection()).getFirstElement();
		if (object != null && object instanceof String) {
			String libKey = currentLibrary.getName();
			IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
			if (entry != null) {
				try {
					entry.removeToken(object.toString());
					foldersViewer.remove(object);
					String[] tokens=entry.getTokens();
					if (tokens.length >index){
						foldersViewer.setSelection(new StructuredSelection(tokens[index]));
					} else if (tokens.length!=0){
						foldersViewer.setSelection(new StructuredSelection(tokens[index-1]));
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private void handleJarsNew() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IFile file = (IFile) model.getUnderlyingResource();
		final IProject project = file.getProject();

		FolderSelectionDialog dialog =
			new FolderSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				new WorkbenchLabelProvider(),
				new JarsNewContentProvider() {
		});

		dialog.setInput(project.getWorkspace());
		dialog.addFilter(new ViewerFilter() {
			public boolean select(
				Viewer viewer,
				Object parentElement,
				Object element) {
				if (element instanceof IProject) {
					return ((IProject) element).equals(project);
				}
				return element instanceof IFolder;
			}
		});
		dialog.setAllowMultiple(false);
		dialog.setTitle(
			PDEPlugin.getResourceString(
				"ManifestEditor.JarsSection.dialogTitle"));
		dialog.setMessage(
			PDEPlugin.getResourceString(
				"ManifestEditor.JarsSection.dialogMessage"));

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

				IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
				String libKey = currentLibrary.getName();

				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);

				String folderPath =
					((IFolder) selection[0])
						.getProjectRelativePath()
						.addTrailingSeparator()
						.toString();

				if (entry != null && entry.contains(folderPath))
					return new Status(
						IStatus.ERROR,
						PDEPlugin.getPluginId(),
						IStatus.ERROR,
						PDEPlugin.getResourceString(
							"BuildPropertiesEditor.RuntimeInfoSection.missingSource.duplicateFolder"),
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
					folder
						.getProjectRelativePath()
						.addTrailingSeparator()
						.toString();
				IBuildModel buildModel = (IBuildModel) getFormPage().getModel();
				String libKey = currentLibrary.getName();
				IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
				if (entry == null) {
					entry = buildModel.getFactory().createEntry(libKey);
					buildModel.getBuild().add(entry);
				}
				entry.addToken(folderPath);
				foldersViewer.refresh();
				foldersViewer.setSelection(new StructuredSelection(folderPath));
				refreshOutputKeys();		
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	protected void handleDown() {
		int index = libraryViewer.getTable().getSelectionIndex();
		Vector libElements = swapLibraries(index, index+1);
		updateJarsCompileOrder((IBuildEntry[])libElements.toArray(new IBuildEntry[libElements.size()]));
		libraryViewer.refresh();
		libraryViewer.setSelection(new StructuredSelection(libElements.get(index+1)));
		updateDirectionalButtons();
	}
	
	protected void handleUp() {
		int index = libraryViewer.getTable().getSelectionIndex();
		Vector libElements = swapLibraries(index, index-1);
		updateJarsCompileOrder((IBuildEntry[])libElements.toArray(new IBuildEntry[libElements.size()]));
		libraryViewer.refresh();
		libraryViewer.setSelection(new StructuredSelection(libElements.get(index-1)));
		updateDirectionalButtons();
	}
	
	protected Vector swapLibraries(int index_old, int index_new){
		int i =0;
		Vector libElements = new Vector();
		while (libraryViewer.getElementAt(i)!=null){
			libElements.add(libraryViewer.getElementAt(i));
			i++;
		}
			
		IBuildEntry tempLib_curr = (IBuildEntry)libElements.elementAt(index_old);
		IBuildEntry tempLib_prev = (IBuildEntry)libElements.elementAt(index_new);
		libElements.setElementAt(tempLib_prev, index_old);
		libElements.setElementAt(tempLib_curr, index_new);	
		return libElements;
	}
	public void updateJarsCompileOrder(IBuildEntry[] libraries) {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();
		IBuildEntry jarOrderEntry =
			build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
		try {
			if (jarOrderEntry != null){
				build.remove(jarOrderEntry);	
			} 
			jarOrderEntry =
				model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);

			for (int i = 0; i < libraries.length; i++) {
				jarOrderEntry.addToken(libraries[i].getName().substring(7));
			}
			build.add(jarOrderEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void updateJarsCompileOrder(TableItem[] libraries) {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();
		IBuildEntry jarOrderEntry =
			build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);
		try {
			if (jarOrderEntry != null){
				build.remove(jarOrderEntry);	
			} 
			jarOrderEntry =
				model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JAR_ORDER);

			for (int i = 0; i < libraries.length; i++) {
				jarOrderEntry.addToken(libraries[i].getText());
			}
			build.add(jarOrderEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
