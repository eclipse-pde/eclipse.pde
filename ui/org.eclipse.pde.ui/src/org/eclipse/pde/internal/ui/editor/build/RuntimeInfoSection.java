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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

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
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.StructuredViewerPart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

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
	private static RGB LIGHT_GRAY = new RGB(172, 168, 153);
	private static RGB BLACK = new RGB(0, 0, 0);
	public Image libImage;
	public Image jarsImage;
	protected TableViewer libraryTable;
	protected TableViewer jarsTable;
	protected Control sectionControl;

	protected StructuredViewerPart libraryPart;
	protected StructuredViewerPart jarsPart;
	private IBuildEntry currentLibrary;
	private Button jarIncludeButton;
	private PDEFormPage page;


	class PartAdapter extends EditableTablePart {
		public PartAdapter(String[] buttonLabels) {
			super(buttonLabels);
		}
		public void entryModified(Object entry, String value) {
			RuntimeInfoSection.this.entryModified(entry, value);
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
			} else if (this.getViewer() == jarsPart.getViewer()) {
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
				try {
					IBuild build = ((IBuildModel) parent).getBuild();
					IBuildEntry jarOrderEntry =
						build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
					IBuildEntry[] libraries =
						BuildUtil.getBuildLibraries(build.getBuildEntries());
					if (jarOrderEntry == null) {
						return libraries;
					}

					for (int i = 0;
						i < libraries.length&& jarOrderEntry.getTokens().length < libraries.length;
						i++) {
						String name =
							libraries[i].getName().substring(7);
						if (!jarOrderEntry.contains(name))
							jarOrderEntry.addToken(name);
					}

					String[] tokens = jarOrderEntry.getTokens();
					libraries = new IBuildEntry[tokens.length];
					for (int i = 0; i < tokens.length; i++) {
						libraries[i] =
							build.getEntry(IBuildEntry.JAR_PREFIX + tokens[i]);
					}
					return libraries;
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
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
		libraryPart =
			createViewerPart(
				new String[] {
					PDEPlugin.getResourceString(SECTION_NEW),
					null,
					PDEPlugin.getResourceString(SECTION_UP),
					PDEPlugin.getResourceString(SECTION_DOWN)});
		//libraryPart.setMinimumSize(50, 50);

		jarsPart =
			createViewerPart(
				new String[] { PDEPlugin.getResourceString(JSECTION_NEW)});
		//jarsPart.setMinimumSize(50, 50);

		setAddSeparator(true);
		setHeaderPainted(true);
		setDescriptionPainted(true);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}

	protected void handleLibInBinBuild(boolean isSelected){
		String libName = currentLibrary.getName().substring(7);
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuildEntry binIncl = model.getBuild().getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
		IProject project = model.getUnderlyingResource().getProject();
		IPath libPath = project.getFile(libName).getProjectRelativePath();
		
		try {
			if (binIncl == null && !isSelected)
				return;
			if (binIncl == null){
				binIncl = model.getFactory().createEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
				model.getBuild().add(binIncl);
			}
			if (!isSelected && libPath.segmentCount() == 1 && binIncl.contains("*.jar")){
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
	public void initialize(Object input) {
		IBuildModel model = (IBuildModel) input;
		libraryTable.setInput(model);
		setReadOnly(false);
		model.addModelChangedListener(this);
	}

	private void createOutputKey(String libName, Set outputFolders){
		if (outputFolders.size()==0)
			return;
		IBuildModel buildModel = (IBuildModel)getFormPage().getModel();
		IBuild build = buildModel.getBuild();
		String outputName = IXMLConstants.PROPERTY_OUTPUT_PREFIX + libName;
		IBuildEntry outputEntry = build.getEntry(outputName);
		Iterator iter = outputFolders.iterator();
		try {
			if (outputEntry == null){		
				outputEntry = buildModel.getFactory().createEntry(outputName);
				build.add(outputEntry);
			} else {
				String[] tokens = outputEntry.getTokens();
				for (int i = 0 ; i<tokens.length; i++ ){
					outputEntry.removeToken(tokens[i]);
				}
			}

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
		Composite container = createClientContainer(parent, 4, factory);
		EditableTablePart tablePart = getLibTablePart();
		tablePart.setEditable(true);
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);

		libraryTable = tablePart.getTableViewer();
		libraryTable.setContentProvider(new LibTableContentProvider());
		libraryTable.setLabelProvider(new LibTableLabelProvider());

		tablePart.setButtonEnabled(2, false);
		tablePart.setButtonEnabled(3, false);

		EditableTablePart JtablePart = getJarsTablePart();
		JtablePart.setEditable(true);
		jarsTable = JtablePart.getTableViewer();
		jarsTable.setContentProvider(new JarsTableContentProvider());
		jarsTable.setLabelProvider(new JarsTableLabelProvider());

		jarIncludeButton =
			factory.createButton(
				container,
				PDEPlugin.getResourceString(JAR_INCLUDE),
				SWT.CHECK);
		jarIncludeButton.setAlignment(SWT.RIGHT);
		jarIncludeButton.setVisible(false);
		jarIncludeButton.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				handleLibInBinBuild(jarIncludeButton.getSelection());

			}
		});

		enableSection();
		factory.paintBordersFor(container);

		return container;
	}
	
	protected void createViewerPartControl(
		Composite parent,
		int style,
		int span,
		FormWidgetFactory factory) {
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint=200;
		libraryPart.createControl(parent, style, span, factory);
		MenuManager libPopupMenuManager = new MenuManager();
		IMenuListener libListener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillLibContextMenu(mng);
			}
		};
		libPopupMenuManager.addMenuListener(libListener);
		libPopupMenuManager.setRemoveAllWhenShown(true);
		Control control = libraryPart.getControl();
		Menu menu = libPopupMenuManager.createContextMenu(control);
		control.setLayoutData(gd);
		control.setMenu(menu);


		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 200;
		jarsPart.createControl(parent, style, span, factory);
		MenuManager jarsPopupMenuManager = new MenuManager();
		IMenuListener jarsListener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillJarsContextMenu(mng);
			}
		};
		jarsPopupMenuManager.addMenuListener(jarsListener);
		jarsPopupMenuManager.setRemoveAllWhenShown(true);
		control = jarsPart.getControl();
		menu = jarsPopupMenuManager.createContextMenu(control);
		control.setMenu(menu);
		control.setLayoutData(gd);
	}

	protected Composite createClientContainer(
		Composite parent,
		int span,
		FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 2;
		layout.numColumns = span;
		container.setLayout(layout);
		return container;
	}

	protected EditableTablePart getLibTablePart() {
		return (EditableTablePart) libraryPart;
	}

	protected EditableTablePart getJarsTablePart() {
		return (EditableTablePart) jarsPart;
	}

	protected IAction getLibRenameAction() {
		return getLibTablePart().getRenameAction();
	}

	protected void fillJarsContextMenu(IMenuManager manager) {
		ISelection selection = jarsTable.getSelection();

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

		if (!selection.isEmpty()) {
			manager.add(new Separator());
			Action deleteAction =
				new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleJarsDelete();
				}
			};
			deleteAction.setEnabled(true);
			manager.add(deleteAction);
		}
		manager.add(new Separator());
		// defect 19550
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager,
			false);

	}

	protected void fillLibContextMenu(IMenuManager manager) {

		ISelection selection = libraryTable.getSelection();

		Action newAction =
			new Action(PDEPlugin.getResourceString(POPUP_NEW_LIBRARY)) {
			public void run() {
				handleNew();
			}
		};

		newAction.setEnabled(true);
		manager.add(newAction);

		if (!selection.isEmpty()) {
			manager.add(new Separator());
			IAction renameAction = getLibRenameAction();
			renameAction.setEnabled(true);
			manager.add(renameAction);

			Action deleteAction =
				new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			};

			deleteAction.setEnabled(true);
			manager.add(deleteAction);
		}
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager,
			false);
	}

	protected void entryModified(Object object, String newValue) {
		Item item = (Item) object;
		final IBuildEntry entry = (IBuildEntry) item.getData();
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
				build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
			if (tempEntry !=null)
				tempEntry.renameToken(tempEntry.getName().substring(7),	newValue.substring(7));
				
			// output.{source folder}.jar				
			tempEntry = build.getEntry(IXMLConstants.PROPERTY_OUTPUT_PREFIX + oldName);
			if (tempEntry!=null){
				build.remove(tempEntry);
				refreshOutputKeys();
			}
			// bin.includes
			tempEntry = build.getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
			if (tempEntry!=null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newValue.substring(7));
				
			// bin.excludes
			tempEntry = build.getEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES);
			if (tempEntry!=null && tempEntry.contains(oldName))
				tempEntry.renameToken(oldName, newValue.substring(7));
			
			libraryTable.getTable().getDisplay().asyncExec(new Runnable() {
				public void run() {
					libraryTable.update(entry, null);
				}
			});
			libraryTable.refresh();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void expandTo(Object object) {
		libraryTable.setSelection(new StructuredSelection(object), true);
	}

	public void handleDoubleClick(IStructuredSelection selection) {
		
	}

	public void disableSection() {

		if (getSectionControl() != null)
			getSectionControl().setEnabled(false);

		EditableTablePart tablePart = getJarsTablePart();
		tablePart.setButtonEnabled(0, false);
		tablePart.getControl().setForeground(
			new Color(tablePart.getControl().getDisplay(), LIGHT_GRAY));
		tablePart.getTableViewer().setSelection(null, false);

		tablePart = getLibTablePart();
		tablePart.setButtonEnabled(0, false);
		tablePart.setButtonEnabled(2, false);
		tablePart.setButtonEnabled(3, false);

		tablePart.getControl().setForeground(
			new Color(tablePart.getControl().getDisplay(), LIGHT_GRAY));
		tablePart.getTableViewer().setSelection(null, false);

	}

	public void setSectionControl(Control control) {
		sectionControl = control;
	}

	public Control getSectionControl() {
		return sectionControl;
	}
	
	public boolean doGlobalAction(String actionId) {
		IStructuredSelection currentSelection = (IStructuredSelection)getFormPage().getSelection();
		
		if (actionId.equals(IWorkbenchActionConstants.DELETE)) {
			if (currentSelection.getFirstElement().toString().startsWith(IBuildEntry.JAR_PREFIX))
				handleDelete();
			else
				handleJarsDelete();
			return true;
		}
		return false;
	}
	
	public void enableSection() {
		EditableTablePart tablePart = getLibTablePart();
		tablePart.setButtonEnabled(0, true);
		tablePart.setButtonEnabled(2, false);
		tablePart.setButtonEnabled(3, false);

		tablePart.getControl().setForeground(
			new Color(tablePart.getControl().getDisplay(), BLACK));

		tablePart = getJarsTablePart();
		tablePart.setButtonEnabled(0, false);
		tablePart.getControl().setForeground(
			new Color(tablePart.getControl().getDisplay(), BLACK));
		if (getSectionControl() != null)
			getSectionControl().setEnabled(true);
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

			createOutputKey(currentLibrary.getName().substring(7), outputFolders);
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
	public void setJarsFocus() {
		jarsTable.getTable().setFocus();
	}

	public void setLibFocus() {
		libraryTable.getTable().setFocus();
	}

	private void update(IBuildEntry variable) {
		currentLibrary = variable;
		jarsTable.setInput(currentLibrary);
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
		Table table = libraryTable.getTable();
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
		IBuildEntry binIncl = model.getBuild().getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
		
		if (binIncl == null)
			return false;
			
		if (libPath.segmentCount() ==1)
			return binIncl.contains(libName) || binIncl.contains("*.jar");
		else 
			return binIncl.contains(libName);
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			libraryTable.refresh();
			jarsTable.refresh();
			libraryTable.setSelection(null);
			jarsTable.setInput(null);
			jarIncludeButton.setVisible(false);
		}
	}

	protected void handleNew() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();
		
		IBuildEntry[] libraries = BuildUtil.getBuildLibraries(build.getBuildEntries());
		final String[] libNames = new String[libraries.length];
		for (int i =0 ; i<libraries.length; i++){
			libNames[i] = libraries[i].getName().substring(7);
		}
		
		try {
			AddLibraryDialog dialog =
				new AddLibraryDialog(
					getFormPage().getControl().getShell(), libNames);
			dialog.create();
			dialog.getShell().setText("Add Entry"); //$NON-NLS-1$
			dialog.getShell().setSize(300, 150);
			if (dialog.open() == Dialog.OK) {

				String name = dialog.getNewName();
				if (!name.endsWith(".jar"))
					name = name + ".jar";

				if (!name.startsWith(IBuildEntry.JAR_PREFIX))
					name = IBuildEntry.JAR_PREFIX + name;

				IBuildEntry library = model.getFactory().createEntry(name);
				build.add(library);

				libraryTable.refresh();
				libraryTable.setSelection(new StructuredSelection(library));
				jarIncludeButton.setSelection(true);
				handleLibInBinBuild(true);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

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
		String libName;
		int index = libraryTable.getTable().getSelectionIndex();
		Object object =
			((IStructuredSelection) libraryTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof IBuildEntry) {
			IBuildEntry library = (IBuildEntry) object;
			IBuild build = ((IBuildModel)getFormPage().getModel()).getBuild();
			
			try {
				// jars.compile.order
				IBuildEntry entry =
					build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
				if (entry !=null)
					entry.removeToken(library.getName().substring(7));
				
				// output.{source folder}.jar				
				entry = build.getEntry(IXMLConstants.PROPERTY_OUTPUT_PREFIX + library.getName().substring(7));
				if (entry!=null)
					build.remove(entry);
					
				// bin.includes
				entry = build.getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
				if (entry!=null && entry.contains(library.getName().substring(7)))
					entry.removeToken(library.getName().substring(7));
				
				// bin.excludes
				entry = build.getEntry(IXMLConstants.PROPERTY_BIN_EXCLUDES);
				if (entry!=null && entry.contains(library.getName().substring(7)))
					entry.removeToken(library.getName().substring(7));
 
				build.remove(library);
				libraryTable.refresh();
				IBuildEntry[] libraries = BuildUtil.getBuildLibraries(build.getBuildEntries());
				if (libraries.length > index) {
					libName = libraryTable.getElementAt(index).toString();
				} else if (libraries.length==index && libraries.length != 0){
					libName = libraryTable.getElementAt(index-1).toString();
				} else {
					libName="";
				}
				
				IBuildEntry selection = build.getEntry(libName);
				if (selection!=null){
					libraryTable.setSelection(new StructuredSelection(selection));
				}else{ 
					libraryTable.setSelection(null);
					jarsTable.setInput(null);
					jarIncludeButton.setVisible(false);
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	private void handleJarsDelete() {

		IBuildModel buildModel = (IBuildModel) getFormPage().getModel();

		Object object =
			((IStructuredSelection) jarsTable.getSelection()).getFirstElement();
		if (object != null && object instanceof String) {
			String libKey = currentLibrary.getName();
			IBuildEntry entry = buildModel.getBuild().getEntry(libKey);
			if (entry != null) {
				try {
					entry.removeToken(object.toString());
					jarsTable.remove(object);
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
				new ContentProvider() {
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
				jarsTable.refresh();
				jarsTable.setSelection(new StructuredSelection(folderPath));
				refreshOutputKeys();		
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	protected void handleDown() {
		int index = libraryTable.getTable().getSelectionIndex();
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();
		IBuildEntry jarsOrderEntry = build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
		String[] newLibEntries;
		IBuildEntry selectionEntry;
		
		if (jarsOrderEntry == null){
			IBuildEntry[] libraries =
				BuildUtil.getBuildLibraries(build.getBuildEntries());
				
			IBuildEntry tempLib = libraries[index];
			libraries[index] = libraries[index + 1];
			libraries[index + 1] = tempLib;
	
			newLibEntries = new String[libraries.length];
			for (int i=0; i<libraries.length; i++){
				newLibEntries[i] = libraries[i].getName().substring(7);
			}
			selectionEntry = tempLib;
		} else {
			newLibEntries = jarsOrderEntry.getTokens();

			String tempLib = newLibEntries[index];
			newLibEntries[index] = newLibEntries[index + 1];
			newLibEntries[index + 1] = tempLib;		
			selectionEntry = build.getEntry(IBuildEntry.JAR_PREFIX + tempLib);	
		}
		updateJarsCompileOrder(newLibEntries);
		libraryTable.refresh();
		libraryTable.setSelection(new StructuredSelection(selectionEntry));
		updateDirectionalButtons();
	}
	
	protected void handleUp() {
		int index = libraryTable.getTable().getSelectionIndex();
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();
		IBuildEntry jarsOrderEntry = build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
		String[] newLibEntries;
		IBuildEntry selectionEntry;
		if (jarsOrderEntry == null){
			IBuildEntry[] libraries =
				BuildUtil.getBuildLibraries(build.getBuildEntries());
				
			IBuildEntry tempLib = libraries[index];
			libraries[index] = libraries[index - 1];
			libraries[index - 1] = tempLib;
	
			newLibEntries = new String[libraries.length];
			for (int i=0; i<libraries.length; i++){
				newLibEntries[i] = libraries[i].getName().substring(7);
			}
			selectionEntry = tempLib;
		} else {
			newLibEntries = jarsOrderEntry.getTokens();

			String tempLib = newLibEntries[index];
			newLibEntries[index] = newLibEntries[index - 1];
			newLibEntries[index - 1] = tempLib;		
			selectionEntry = build.getEntry(IBuildEntry.JAR_PREFIX + tempLib);	
		}
		updateJarsCompileOrder(newLibEntries);
		libraryTable.refresh();
		libraryTable.setSelection(new StructuredSelection(selectionEntry));
		updateDirectionalButtons();
	}

	public void updateJarsCompileOrder(String[] libraries) {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();
		IBuildEntry jarOrderEntry =
			build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
		try {
			if (jarOrderEntry != null){
				build.remove(jarOrderEntry);	
			} 
			jarOrderEntry =
				model.getFactory().createEntry(
				IXMLConstants.PROPERTY_JAR_ORDER);

			for (int i = 0; i < libraries.length; i++) {
				jarOrderEntry.addToken(libraries[i]);
			}
			build.add(jarOrderEntry);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
