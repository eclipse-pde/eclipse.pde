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
package org.eclipse.pde.internal.ui.neweditor.build;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.InputContext;
import org.eclipse.pde.internal.ui.newparts.EditableTablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.views.navigator.ResourceSorter;

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
	// TODO we should not use hard-coded colors
	private static RGB LIGHT_GRAY =  new RGB(172, 168, 153);
	// TODO we should not use hard-coded colors
	private Color grayColor;
	

	/**
	 * Implementation of a <code>ISelectionValidator</code> to validate the
	 * type of an element.
	 * Empty selections are not accepted.
	 */
	class ElementSelectionValidator implements ISelectionStatusValidator {

		private Class[] fAcceptedTypes;
		private boolean fAllowMultipleSelection;

	
		/**
		 * @param acceptedTypes The types accepted by the validator
		 * @param allowMultipleSelection If set to <code>true</code>, the validator
		 * allows multiple selection.
		 */
		public ElementSelectionValidator(Class[] acceptedTypes, boolean allowMultipleSelection) {
			Assert.isNotNull(acceptedTypes);
			fAcceptedTypes= acceptedTypes;
			fAllowMultipleSelection= allowMultipleSelection;
		}
	

		/*
		 * @see org.eclipse.ui.dialogs.ISelectionValidator#isValid(java.lang.Object)
		 */
		public IStatus validate(Object[] elements) {
			if (isValid(elements)) {
				return new Status(
					IStatus.OK,
					PDEPlugin.getPluginId(),
					IStatus.OK,
					"",
					null);
			}
			return new Status(
				IStatus.ERROR,
				PDEPlugin.getPluginId(),
				IStatus.ERROR,
				"",
				null);
		}	

		private boolean isOfAcceptedType(Object o) {
			for (int i= 0; i < fAcceptedTypes.length; i++) {
				if (fAcceptedTypes[i].isInstance(o)) {
					return true;
				}
			}
			return false;
		}
	
		private boolean isValid(Object[] selection) {
			if (selection.length == 0) {
				return false;
			}
		
			if (!fAllowMultipleSelection && selection.length != 1) {
				return false;
			}
		
			for (int i= 0; i < selection.length; i++) {
				Object o= selection[i];	
				if (!isOfAcceptedType(o)) {
					return false;
				}
			}
			return true;
		}
	}

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel)parent).getBuild();
				IBuildEntry entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
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
	public BuildClasspathSection(PDEFormPage page, Composite parent) {
		super(
			page,
			parent,
			Section.DESCRIPTION | Section.TWISTIE,
			new String[] {
				PDEPlugin.getResourceString(SECTION_ADD),
				PDEPlugin.getResourceString(SECTION_REMOVE),
				null,
				null });
		buildModel = getBuildModel();
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		initialize();

	}
	
	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
				.findContext(BuildInputContext.CONTEXT_ID);
		return (IBuildModel) context.getModel();
	}

	public void initialize(){
		buildModel.addModelChangedListener(this);
		IBuildEntry entry = buildModel.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
		getSection().addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanged(ExpansionEvent e) {
				//getPage().getManagedForm().reflow(true);
			}
		});
		getSection().setExpanded(entry!=null && entry.getTokens().length>0);
	}

	private void initializeImages() {
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		entryImage = sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
	}

	public void createClient(
		Section section,
		FormToolkit toolkit) {
		initializeImages();
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);

		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(true);
		entryTable = tablePart.getTableViewer();

		entryTable.setContentProvider(new TableContentProvider());
		entryTable.setLabelProvider(new TableLabelProvider());

		toolkit.paintBordersFor(container);
		entryTable.setInput(buildModel);
		grayColor = new Color(section.getDisplay(), LIGHT_GRAY);
		enableSection();
		section.setClient(container);
	}
	
	public void disableSection(){
		EditableTablePart tablePart  = getTablePart();
		tablePart.setButtonEnabled(1, false);
		tablePart.setButtonEnabled(0, false);
		tablePart.getTableViewer().setSelection(null,false);
		// TODO we should not use hardcoded colors
		tablePart.getControl().setForeground(grayColor);
		if (getSectionControl()!=null)
			getSectionControl().setEnabled(false);
	}
 
	public void setSectionControl(Control control){
		sectionControl = control;
	}
	
	
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = entryTable.getSelection();

		// add NEW action
		Action action =
			new Action(PDEPlugin.getResourceString(POPUP_NEW)) {
				public void run() {
					handleNew();
				}
			};
		action.setEnabled(true);
		manager.add(action);

		manager.add(new Separator());

		// add DELETE action
		action =
			new Action(PDEPlugin.getResourceString(POPUP_DELETE)) {
				public void run() {
					handleDelete();
				}
			};
		action.setEnabled(!selection.isEmpty());
		manager.add(action);

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager, false);
	}
	
	public Control getSectionControl(){
		return sectionControl;
	}
	
	public void dispose() {
		buildModel.removeModelChangedListener(this);
		grayColor.dispose();
		super.dispose();
	}
	
	public void refresh() {
		entryTable.refresh();
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		return false;
	}
	
	public void enableSection(){
		EditableTablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(1, false);
		tablePart.setButtonEnabled(0, true);
		tablePart.getTableViewer().setSelection(null,false);
		tablePart.getControl().setForeground(null);
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
		int index = entryTable.getTable().getSelectionIndex();
		if (selection != null && selection instanceof String) {
			IBuildEntry entry = buildModel.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
			if (entry != null) {
				try {
					entry.removeToken(selection.toString());
					entryTable.remove(selection);
					String[] tokens=entry.getTokens();
					if (tokens.length == 0) {
						buildModel.getBuild().remove(entry);
					} else if (tokens.length >index){
						entryTable.setSelection(new StructuredSelection(tokens[index]));
					} else {
						entryTable.setSelection(new StructuredSelection(tokens[index-1]));
					}
					updateRemoveStatus();

				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

		}

	}

	private void initializeDialogSettings(ElementTreeSelectionDialog dialog){
		Class[] acceptedClasses = new Class[] { IFile.class };
		dialog.setValidator(new ElementSelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEPlugin.getResourceString("BuildPropertiesEditor.BuildClasspathSection.JarsSelection.title"));
		dialog.setMessage(PDEPlugin.getResourceString("BuildPropertiesEditor.BuildClasspathSection.JarsSelection.desc"));
		dialog.addFilter(new JARFileFilter());
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		dialog.setInitialSelection(buildModel.getUnderlyingResource().getProject());

	}
	private void handleNew() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		initializeDialogSettings(dialog);

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			
			Object[] elements = dialog.getResult();

			for (int i = 0; i < elements.length; i++) {
				IResource elem = (IResource) elements[i];
				String tokenName = getRelativePathTokenName(elem);
				addClasspathToken(tokenName);
				entryTable.refresh();
				entryTable.setSelection(new StructuredSelection(tokenName));
			}
		}
	}
	
	private void addClasspathToken(String tokenName){
		IBuildEntry entry = buildModel.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
		try {
			if (entry==null){
				entry = buildModel.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
				buildModel.getBuild().add(entry);
			}
			if (!entry.contains(tokenName))
				entry.addToken(tokenName);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	
	private String getRelativePathTokenName(IResource elem){
		IPath path = elem.getFullPath();
		IPath projectPath =
			buildModel
				.getUnderlyingResource()
				.getProject()
				.getFullPath();
		int sameSegments = path.matchingFirstSegments(projectPath);
		if (sameSegments > 0)
			return path.removeFirstSegments(sameSegments).toString();
		else
			return ".."+path.toString();
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

