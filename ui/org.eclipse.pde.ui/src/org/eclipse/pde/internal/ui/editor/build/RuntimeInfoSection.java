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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
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
		try {
			if (newValue.equals(entry.getName()))
				return;
			if (!newValue.startsWith(IBuildEntry.JAR_PREFIX))
				newValue = IBuildEntry.JAR_PREFIX + newValue;
			if (!newValue.endsWith(".jar"))
				newValue = newValue + ".jar";

			IBuildEntry jarsEntry =
				build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
			if (jarsEntry !=null)
				jarsEntry.renameToken(
					entry.getName().substring(7),
					newValue.substring(7));
			
			IBuildEntry outputEntry = build.getEntry(IXMLConstants.PROPERTY_OUTPUT_PREFIX + entry.getName().substring(7));
			if (outputEntry !=null)
				build.remove(outputEntry);
			entry.setName(newValue);
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
			jarIncludeButton.setSelection(isJarIncluded(item.toString().replaceFirst(IBuildEntry.JAR_PREFIX,"")));
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
		IBuildEntry binIncl =
			model.getBuild().getEntry(IXMLConstants.PROPERTY_BIN_INCLUDES);
		if (libPath.segmentCount() ==1)
			return binIncl.contains(libName) || binIncl.contains("*.jar");
		else 
			return binIncl.contains(libName);
	}

	public void modelChanged(IModelChangedEvent event) {

	}

	protected void handleNew() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		IBuild build = model.getBuild();

		try {
			RenameDialog dialog =
				new RenameDialog(
					getFormPage().getControl().getShell(),
					PDEPlugin.getResourceString(NEW_LIBRARY_ENTRY));
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

	protected void handleDelete() {
		String libName="";
		int index = libraryTable.getTable().getSelectionIndex();
		Object object =
			((IStructuredSelection) libraryTable.getSelection())
				.getFirstElement();
		if (object != null && object instanceof IBuildEntry) {
			IBuildEntry library = (IBuildEntry) object;
			IBuild build = library.getModel().getBuild();
			IBuildEntry jarsEntry =
				build.getEntry(IXMLConstants.PROPERTY_JAR_ORDER);
			IBuildEntry outputEntry = build.getEntry(IXMLConstants.PROPERTY_OUTPUT_PREFIX + library.getName().substring(7));
			try {
				if (jarsEntry !=null)
					jarsEntry.removeToken(library.getName().substring(7));
				if (outputEntry!=null)
					build.remove(outputEntry);
				build.remove(library);
				libraryTable.refresh();
				if (jarsEntry.getTokens().length > index) {
					libName = libraryTable.getElementAt(index).toString();
				} else if (jarsEntry.getTokens().length==1){
					libName = libraryTable.getElementAt(0).toString();
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
