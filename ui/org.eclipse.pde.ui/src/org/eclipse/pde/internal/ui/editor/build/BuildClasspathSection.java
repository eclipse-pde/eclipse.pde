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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.ui.wizards.TypedElementSelectionValidator;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceSorter;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class BuildClasspathSection
	extends TableSection
	implements IModelChangedListener {

	private final static String SECTION_ADD =
		"BuildPropertiesEditor.BuildClasspathSection.add";
	private final static String SECTION_REMOVE =
		"BuildPropertiesEditor.BuildClasspathSection.remove";
	private final static String SECTION_TITLE =
		"BuildPropertiesEditor.BuildClasspathSection.title";
	private final static String SECTION_DESC =
		"BuildPropertiesEditor.BuildClasspathSection.desc";
	private final static String POPUP_NEW =
		"BuildPropertiesEditor.BuildClasspathSection.popupAdd";
	private final static String POPUP_DELETE =
		"BuildPropertiesEditor.BuildClasspathSection.popupDelete";			
	protected IBuildModel buildModel;
	private TableViewer entryTable;
	private Image entryImage;
	protected Control sectionControl;
	private static RGB LIGHT_GRAY =  new RGB(172, 168, 153);
	private static RGB BLACK = new RGB(0, 0, 0);
	
	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel)parent).getBuild();
				IBuildEntry entry = build.getEntry(IXMLConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
				if (entry != null) {
					return entry.getTokens();
				}
			}
			return new Object[0];
		}
	}

	class TableLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return obj.toString();
		}
		public Image getColumnImage(Object obj, int index) {
			return entryImage;
		}
	}
	public BuildClasspathSection(PDEFormPage page) {
		super(
			page,
			new String[] {
				PDEPlugin.getResourceString(SECTION_ADD),
				PDEPlugin.getResourceString(SECTION_REMOVE),
				null,
				null });
		buildModel = (IBuildModel) getFormPage().getModel();
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		setCollapsable(true);
		setCollapsed(true);

	}

	private void initializeImages() {
		IWorkbench workbench = PlatformUI.getWorkbench();
		ISharedImages sharedImages = workbench.getSharedImages();
		entryImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		initializeImages();
		Composite container = createClientContainer(parent, 2, factory);
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);

		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(true);
		entryTable = tablePart.getTableViewer();

		entryTable.setContentProvider(new TableContentProvider());
		entryTable.setLabelProvider(new TableLabelProvider());

		enableSection();
		factory.paintBordersFor(container);

		entryTable
			.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {

			}
		});
		entryTable.setInput(getFormPage().getModel());
		
		return container;
	}
	
	public void disableSection(){
		EditableTablePart tablePart  = getTablePart();
		tablePart.setButtonEnabled(1, false);
		tablePart.setButtonEnabled(0, false);
		tablePart.getTableViewer().setSelection(null,false);
		tablePart.getControl().setForeground(new Color(tablePart.getControl().getDisplay(), LIGHT_GRAY));
		if (getSectionControl()!=null)
			getSectionControl().setEnabled(false);
	}
 
	public void setSectionControl(Control control){
		sectionControl = control;
	}
	
	
	protected void fillContextMenu(IMenuManager manager) {

		ISelection selection = entryTable.getSelection();

		Action newAction =
			new Action(PDEPlugin.getResourceString(POPUP_NEW)) {
			public void run() {
				handleNew();
			}
		};

		newAction.setEnabled(true);
		manager.add(newAction);

		if (!selection.isEmpty()) {
			manager.add(new Separator());

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
			manager,false);
	}
	
	public Control getSectionControl(){
		return sectionControl;
	}
	
	public void dispose() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	
	public void enableSection(){
		EditableTablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(1, false);
		tablePart.setButtonEnabled(0, true);
		tablePart.getControl().setForeground(new Color(tablePart.getControl().getDisplay(), BLACK));
		if (getSectionControl()!=null)
			getSectionControl().setEnabled(true);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		if (selection.size() == 0)
			return;
		updateRemoveStatus();
	}

	public void updateRemoveStatus() {
		Table table = entryTable.getTable();
		getTablePart().setButtonEnabled(1, table.getSelection().length > 0);
	}

	private void handleDelete() {
		Object selection =
			((IStructuredSelection) entryTable.getSelection())
				.getFirstElement();
		if (selection != null && selection instanceof String) {
			IBuildEntry entry = buildModel.getBuild().getEntry(IXMLConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
			if (entry != null) {
				try {
					entry.removeToken(selection.toString());
					entryTable.remove(selection);
					if (entry.getTokens().length == 0) {
						buildModel.getBuild().remove(entry);
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

		}

	}

	private void handleNew() {
		Class[] acceptedClasses = new Class[] { IFile.class };
		TypedElementSelectionValidator validator =
			new TypedElementSelectionValidator(acceptedClasses, true);
		ViewerFilter filter = new JARFileFilter();

		String title = PDEPlugin.getResourceString("BuildPropertiesEditor.BuildClasspathSection.JarsSelection.title"); //$NON-NLS-1$ //$NON-NLS-2$
		String message = PDEPlugin.getResourceString("BuildPropertiesEditor.BuildClasspathSection.JarsSelection.desc"); //$NON-NLS-1$ //$NON-NLS-2$

		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getFormPage().getControl().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
		dialog.setValidator(validator);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.addFilter(filter);
		dialog.setInput(
			buildModel.getUnderlyingResource().getWorkspace().getRoot());
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		dialog.setInitialSelection(
			buildModel.getUnderlyingResource().getProject());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IBuildEntry entry = buildModel.getBuild().getEntry(IXMLConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
			Object[] elements = dialog.getResult();

			for (int i = 0; i < elements.length; i++) {
				IResource elem = (IResource) elements[i];
				try {

					IPath path = elem.getFullPath();
					IPath projectPath =
						buildModel
							.getUnderlyingResource()
							.getProject()
							.getFullPath();
					String tokenName = path.toString();
					int sameSegments = path.matchingFirstSegments(projectPath);
					if (sameSegments > 0) {
						tokenName = path.removeFirstSegments(sameSegments).toString();
					} else {
						tokenName = ".."+path.toString();
					}
					
					if (entry==null){
						entry = buildModel.getFactory().createEntry(IXMLConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
						buildModel.getBuild().add(entry);
					}
					if (!entry.contains(tokenName))
						entry.addToken(tokenName);
					entryTable.refresh();
					entryTable.setSelection(new StructuredSelection(tokenName));

				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

		}

	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				entryTable.refresh();
				break;
			case 1 :
				handleDelete();
				entryTable.refresh();
				break;
			default :
				break;
		}
	}

}
