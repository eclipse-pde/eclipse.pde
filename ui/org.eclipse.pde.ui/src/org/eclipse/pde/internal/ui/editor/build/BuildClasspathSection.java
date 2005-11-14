/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.context.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.views.navigator.*;

public class BuildClasspathSection
	extends TableSection
	implements IModelChangedListener {

	private TableViewer fTableViewer;
	private boolean fEnabled = true;
	private IStructuredSelection fCurrentSelection;
	private IStructuredSelection fOldSelection;
	

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
					"", //$NON-NLS-1$
					null);
			}
			return new Status(
				IStatus.ERROR,
				PDEPlugin.getPluginId(),
				IStatus.ERROR,
				"", //$NON-NLS-1$
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
			ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
		}
	}
	public BuildClasspathSection(PDEFormPage page, Composite parent) {
		super(
			page,
			parent,
			Section.DESCRIPTION | Section.TWISTIE,
			new String[] {
				PDEUIMessages.BuildEditor_ClasspathSection_add,
				PDEUIMessages.BuildEditor_ClasspathSection_remove,
				null,
				null });
		getSection().setText(PDEUIMessages.BuildEditor_ClasspathSection_title);
		getSection().setDescription(PDEUIMessages.BuildEditor_ClasspathSection_desc);
		initialize();

	}
	
	private IBuildModel getBuildModel() {
		InputContext context = getPage().getPDEEditor().getContextManager()
				.findContext(BuildInputContext.CONTEXT_ID);
		if (context==null) return null;
		return (IBuildModel) context.getModel();
	}

	public void initialize(){
		getBuildModel().addModelChangedListener(this);
		IBuildEntry entry = getBuildModel().getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
		getSection().setExpanded(entry!=null && entry.getTokens().length>0);
	}

	public void createClient(
		Section section,
		FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.FULL_SELECTION, 2, toolkit);

		EditableTablePart tablePart = getTablePart();
		tablePart.setEditable(true);
		fTableViewer = tablePart.getTableViewer();

		fTableViewer.setContentProvider(new TableContentProvider());
		fTableViewer.setLabelProvider(new TableLabelProvider());
		fTableViewer.setInput(getBuildModel());

		toolkit.paintBordersFor(container);
		enableSection(true);
		section.setClient(container);
	}
	
	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = fTableViewer.getSelection();

		// add NEW action
		Action action =
			new Action(PDEUIMessages.BuildEditor_ClasspathSection_add) {
				public void run() {
					handleNew();
				}
			};
		action.setEnabled(fEnabled);
		manager.add(action);

		manager.add(new Separator());

		// add DELETE action
		action =
			new Action(PDEUIMessages.BuildEditor_ClasspathSection_remove) {
				public void run() {
					handleDelete();
				}
			};
		action.setEnabled(!selection.isEmpty() && fEnabled);
		manager.add(action);

		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(
			manager, false);
	}
	
	public void dispose() {
		IBuildModel model = getBuildModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	public void refresh() {
		fTableViewer.refresh();
	}
	
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			if (fEnabled) {
				handleDelete();
			}
			return true;
		}
		return false;
	}
	
	public void enableSection(boolean enable){
		fEnabled = enable;
		EditableTablePart tablePart = getTablePart();
		tablePart.setButtonEnabled(1, enable && !fTableViewer.getSelection().isEmpty());
		tablePart.setButtonEnabled(0, enable);
	}

	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		getTablePart().setButtonEnabled(1, selection != null && selection.size() > 0 && fEnabled);
	}

	private void handleDelete() {
		Object selection =
			((IStructuredSelection) fTableViewer.getSelection())
				.getFirstElement();
		fOldSelection = (IStructuredSelection) fTableViewer.getSelection();
		int index = fTableViewer.getTable().getSelectionIndex();
		if (selection != null && selection instanceof String) {
			IBuild build = getBuildModel().getBuild();
			IBuildEntry entry = build.getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
			if (entry != null) {
				try {
					entry.removeToken(selection.toString());

					String[] tokens=entry.getTokens();
					if (tokens.length == 0) {
						build.remove(entry);
					} else if (tokens.length >index){
						fCurrentSelection = new StructuredSelection(tokens[index]);
					} else {
						fCurrentSelection = new StructuredSelection(tokens[index-1]);
					}
					
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
	}

	private void initializeDialogSettings(ElementTreeSelectionDialog dialog){
		Class[] acceptedClasses = new Class[] { IFile.class };
		dialog.setValidator(new ElementSelectionValidator(acceptedClasses, true));
		dialog.setTitle(PDEUIMessages.BuildEditor_ClasspathSection_jarsTitle); 
		dialog.setMessage(PDEUIMessages.BuildEditor_ClasspathSection_jarsDesc); 
		dialog.addFilter(new JARFileFilter());
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		dialog.setSorter(new ResourceSorter(ResourceSorter.NAME));
		dialog.setInitialSelection(getBuildModel().getUnderlyingResource().getProject());

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
				fCurrentSelection = new StructuredSelection(tokenName);
				fOldSelection = null;
			}
		}
	}
	
	private void addClasspathToken(String tokenName){
		IBuildModel model = getBuildModel();
		IBuildEntry entry = model.getBuild().getEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
		try {
			if (entry==null){
				entry = model.getFactory().createEntry(IBuildPropertiesConstants.PROPERTY_JAR_EXTRA_CLASSPATH);
				model.getBuild().add(entry);
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
			getBuildModel().getUnderlyingResource().getProject().getFullPath();
		int sameSegments = path.matchingFirstSegments(projectPath);
		if (sameSegments > 0)
			return path.removeFirstSegments(sameSegments).toString();
		return ".."+path.toString(); //$NON-NLS-1$
	}

	protected void buttonSelected(int index) {
		switch (index) {
			case 0 :
				handleNew();
				break;
			case 1 :
				handleDelete();
				break;
			default :
				break;
		}
	}
	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (event.getChangeType() == IModelChangedEvent.INSERT){
			
		} else if (event.getChangeType() == IModelChangedEvent.REMOVE){
			
		} else if (event.getChangeType() == IModelChangedEvent.CHANGE){
			Object changeObject = event.getChangedObjects()[0];
			
			if (changeObject instanceof IBuildEntry && ((IBuildEntry)changeObject).getName().equals(IBuildEntry.JARS_EXTRA_CLASSPATH)){
				if (fOldSelection == null){
					fTableViewer.refresh();
					fTableViewer.setSelection(fCurrentSelection);
				} else {
					fTableViewer.remove(fOldSelection);
					fTableViewer.refresh();
					fTableViewer.setSelection(fCurrentSelection);
				}
			}
		}
	}
}

