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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.EditableTablePart;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class JavadocPackagesSection extends TableSection implements IModelChangedListener{

	private TableViewer nameTableViewer;
	public static final String SECTION_TITLE =
		"BuildPropertiesEditor.JavadocPackagesSection.title";
	public static final String SECTION_DESC =
		"BuildPropertiesEditor.JavadocPackagesSection.desc";
	public static final String KEY_ADD =
		"BuildPropertiesEditor.JavadocPackagesSection.add";
	public static final String KEY_REMOVE =
		"BuildPropertiesEditor.JavadocPackagesSection.remove";
	public static final String POPUP_NEW = "BuildPropertiesEditor.JavadocPackagesSection.popupAdd";
	public static final String POPUP_DELETE = "BuildPropertiesEditor.JavadocPackagesSection.popupDelete";
	public static final String JAVADOC_PACKAGES = "javadoc.packages";
	public static final String NEW_PKG_TITLE = "BuildPropertiesEditor.JavadocPackagesSection.newPkgTitle";
	public static final String NEW_PKG_DESC = "BuildPropertiesEditor.JavadocPackagesSection.newPkgDesc";
	private Action addAction;
	private Action deleteAction;
	private Control sectionControl;
	private static RGB LIGHT_GRAY =  new RGB(172, 168, 153);
	private static RGB BLACK = new RGB(0, 0, 0);
	private boolean isJavaProject;

	class TableContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (parent instanceof IBuildModel) {
				IBuild build = ((IBuildModel)parent).getBuild();
				IBuildEntry jpkgEntry = build.getEntry(JAVADOC_PACKAGES);
				if (jpkgEntry!=null)
					return jpkgEntry.getTokens();
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
			return JavaUI.getSharedImages().getImage(
				ISharedImages.IMG_OBJS_PACKAGE);
		}
	}

	public JavadocPackagesSection(BuildPage page) {
		super(
			page,
			new String[] {
				PDEPlugin.getResourceString(KEY_ADD),
				PDEPlugin.getResourceString(KEY_REMOVE)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
		handleDefaultButton = false;
		setCollapsable(true);
		initialize();
	}

	public void initialize(){
		IBuildModel buildModel = (IBuildModel)getFormPage().getModel();
		IBuildEntry entry = buildModel.getBuild().getEntry(JAVADOC_PACKAGES);
		setCollapsed(entry==null || entry.getTokens().length==0);
		
		IProject project = buildModel.getUnderlyingResource().getProject();
		try {
			isJavaProject = project.hasNature(JavaCore.NATURE_ID);
			if (!isJavaProject)
				disableSection();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);

		createViewerPartControl(container, SWT.FULL_SELECTION, 2, factory);
		TablePart part = getTablePart();
		nameTableViewer = part.getTableViewer();
		nameTableViewer.setContentProvider(new TableContentProvider());
		nameTableViewer.setLabelProvider(new TableLabelProvider());
		nameTableViewer.setInput(getFormPage().getModel());
		factory.paintBordersFor(container);

		return container;
	}

	protected void selectionChanged(IStructuredSelection selection) {
		Object item = selection.getFirstElement();
		getFormPage().setSelection(selection);
		getTablePart().setButtonEnabled(1, item != null);
	}

	public void setSectionControl(Control control){
		sectionControl = control;
	}
	
	protected void buttonSelected(int index) {
		if (index == 0)
			handleNew();
		else if (index == 1)
			handleDelete();
	}

	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(org.eclipse.ui.IWorkbenchActionConstants.DELETE)) {
			handleDelete();
			return true;
		}
		return false;
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
	
	public void enableSection(){
		if (!isJavaProject)
			return;
		EditableTablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(1, false);
		tablePart.setButtonEnabled(0, true);
		tablePart.getControl().setForeground(new Color(tablePart.getControl().getDisplay(), BLACK));
		if (getSectionControl()!=null)
			getSectionControl().setEnabled(true);
	}
	
	public void dispose() {
		IBuildModel model = (IBuildModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	protected void fillContextMenu(IMenuManager manager) {
		manager.add(addAction);
		deleteAction.setEnabled(!nameTableViewer.getSelection().isEmpty());
		manager.add(deleteAction);
		manager.add(new Separator());
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void makeActions() {
		addAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		addAction.setText(PDEPlugin.getResourceString(POPUP_NEW));
		addAction.setEnabled(true);
		deleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
	}

	private void handleNew() {
		try {
			IBuildModel model =
				(IBuildModel) getFormPage().getModel();
			IProject project = model.getUnderlyingResource().getProject();
			SelectionDialog dialog;
			dialog = JavaUI.createPackageDialog(
						nameTableViewer.getControl().getShell(),
						JavaCore.create(project),
						0);
			dialog.setTitle(PDEPlugin.getResourceString("Java Packages"));
			dialog.setMessage("");
			int status = dialog.open();
			if (status == SelectionDialog.OK) {
				Object[] result = dialog.getResult();
				for (int i = 0; i < result.length; i++) {
					IPackageFragment packageFragment = (IPackageFragment) result[i];
					String eleName =packageFragment.getElementName();
					if (eleName.length()!=0){
						eleName = eleName.concat(".*");
						createJavadocEntry(eleName);
						nameTableViewer.refresh();
						nameTableViewer.setSelection(new StructuredSelection(eleName));
					}
					updateRemoveStatus();
				}
			}
		} catch (JavaModelException e) {
			PDEPlugin.logException(e);
		}
	}

	private void createJavadocEntry(String token){
		IBuildModel buildModel = (IBuildModel)getFormPage().getModel();
		IBuildEntry javadocEntry = buildModel.getBuild().getEntry(JAVADOC_PACKAGES);

		try {
			if (javadocEntry==null){
				javadocEntry = buildModel.getFactory().createEntry(JAVADOC_PACKAGES);
				buildModel.getBuild().add(javadocEntry);
			}
			if (!javadocEntry.contains(token))
				javadocEntry.addToken(token);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public Control getSectionControl(){
		return sectionControl;
	}
	
	private void handleDelete() {
		Object selection =((IStructuredSelection)nameTableViewer.getSelection()).getFirstElement();
		int index = nameTableViewer.getTable().getSelectionIndex();
		if (selection != null && selection instanceof String) {
			IBuildModel buildModel = (IBuildModel)getFormPage().getModel();
			IBuildEntry entry = buildModel.getBuild().getEntry(JAVADOC_PACKAGES);
			if (entry != null) {
				try {
					entry.removeToken(selection.toString());
					nameTableViewer.remove(selection);
					String[] tokens=entry.getTokens();
					if (tokens.length == 0) {
						buildModel.getBuild().remove(entry);
					} else if (tokens.length >index){
						nameTableViewer.setSelection(new StructuredSelection(tokens[index]));
					} else {
						nameTableViewer.setSelection(new StructuredSelection(tokens[index-1]));
					}
					updateRemoveStatus();
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

		}

	}

	public void initialize(Object input) {
		IBuildModel model = (IBuildModel) input;
		model.addModelChangedListener(this);
		
		makeActions();
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			nameTableViewer.refresh();
		}
	}
	
	public void updateRemoveStatus() {
		getTablePart().setButtonEnabled(1, nameTableViewer.getTable().getSelection().length > 0);
	}
}
