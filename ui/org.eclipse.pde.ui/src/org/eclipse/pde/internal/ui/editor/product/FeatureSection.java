/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.FeatureModelManager;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.IProductModelFactory;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.TableSection;
import org.eclipse.pde.internal.ui.editor.feature.FeatureEditor;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.FeatureSelectionDialog;
import org.eclipse.pde.internal.ui.wizards.feature.NewFeatureProjectWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class FeatureSection extends TableSection {
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return getProduct().getFeatures();
		}
	}

	private TableViewer fFeatureTable;

	public FeatureSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels()); 
	}
	
	private static String[] getButtonLabels() {
		String[] labels = new String[6];
		labels[0] = PDEUIMessages.Product_FeatureSection_add; 
		labels[1] = PDEUIMessages.Product_FeatureSection_remove; 
		labels[2] = PDEUIMessages.Product_PluginSection_removeAll; 
		labels[3] = null;
		labels[4] = null;
		labels[5] = PDEUIMessages.Product_FeatureSection_newFeature; 
		return labels;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		Composite container = createClientContainer(section, 2, toolkit);
		createViewerPartControl(container, SWT.MULTI, 2, toolkit);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TablePart tablePart = getTablePart();
		fFeatureTable = tablePart.getTableViewer();
		fFeatureTable.setContentProvider(new ContentProvider());
		fFeatureTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		GridData data = (GridData)tablePart.getControl().getLayoutData();
		data.minimumWidth = 200;
		fFeatureTable.setInput(PDECore.getDefault().getFeatureModelManager());
		
		tablePart.setButtonEnabled(0, isEditable());
		
		// remove buttons updated on refresh
		
		tablePart.setButtonEnabled(5, isEditable());
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
		section.setText(PDEUIMessages.Product_FeatureSection_title); 
		section.setDescription(PDEUIMessages.Product_FeatureSection_desc); //		

		getModel().addModelChangedListener(this);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#buttonSelected(int)
	 */
	protected void buttonSelected(int index) {
		switch (index) {
		case 0:
			handleAdd();
			break;
		case 1:
			handleDelete();
			break;
		case 2: 
			handleRemoveAll();
			break;
		case 5:
			handleNewFeature();
		}
	}
	
	private void handleNewFeature() {
		NewFeatureProjectWizard wizard = new NewFeatureProjectWizard();
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == Window.OK) {
			addFeature(wizard.getFeatureId(), wizard.getFeatureVersion());
		}
	}
	
	private void addFeature(String id, String version) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductFeature feature = factory.createFeature();
		feature.setId(id);
		feature.setVersion(version);
		product.addFeatures(new IProductFeature[] {feature});
	}
	

	private void handleRemoveAll() {
		IProduct product = getProduct();
		product.removeFeatures(product.getFeatures());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.TableSection#handleDoubleClick(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	protected void handleDoubleClick(IStructuredSelection selection) {
		handleOpen(selection);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}
	
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof IProductFeature)
				return true;
		}
		return false;
	}
	
	protected void doPaste(Object target, Object[] objects) {
		IProductFeature[] features;
		if (objects instanceof IProductFeature[])
			features = (IProductFeature[])objects;
		else {
			features = new IProductFeature[objects.length];
			for (int i = 0; i < objects.length; i++)
				if (objects[i] instanceof IProductFeature)
					features[i] = (IProductFeature)objects[i];
		}
		getProduct().addFeatures(features);
	}

	
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection)fFeatureTable.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			IProductFeature[] features = new IProductFeature[objects.length];
			System.arraycopy(objects, 0, features, 0, objects.length);
			getProduct().removeFeatures(features);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		IStructuredSelection ssel = (IStructuredSelection)fFeatureTable.getSelection();
		if (ssel == null)
			return;
		
		Action openAction = new Action(PDEUIMessages.Product_FeatureSection_open) { 
			public void run() {
				handleDoubleClick((IStructuredSelection)fFeatureTable.getSelection());
			}
		};
		openAction.setEnabled(isEditable() && ssel.size() == 1);
		manager.add(openAction);
		
		manager.add(new Separator());
		
		Action removeAction = new Action(PDEUIMessages.Product_FeatureSection_remove) { 
			public void run() {
				handleDelete();
			}
		};
		removeAction.setEnabled(isEditable() && ssel.size() > 0);
		manager.add(removeAction);
		
		Action removeAll = new Action(PDEUIMessages.FeatureSection_removeAll) { 
			public void run() {
				handleRemoveAll();
			}
		};
		removeAll.setEnabled(isEditable());
		manager.add(removeAll);

		manager.add(new Separator());
		
		getPage().getPDEEditor().getContributor().contextMenuAboutToShow(manager);
	}

	private void handleOpen(IStructuredSelection selection) {
		if (!selection.isEmpty()) {
			IProductFeature feature = (IProductFeature)selection.getFirstElement();
			FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
			IFeatureModel model = manager.findFeatureModel(feature.getId(), feature.getVersion());
			FeatureEditor.openFeatureEditor(model);
		}
	}

	private void handleAdd() {
		FeatureSelectionDialog dialog = new FeatureSelectionDialog(PDEPlugin
				.getActiveWorkbenchShell(), getAvailableChoices(), true);
		if (dialog.open() == Window.OK) {
			Object[] models = dialog.getResult();
			for (int i = 0; i < models.length; i++) {
				IFeature feature = ((IFeatureModel)models[i]).getFeature();
				addFeature(feature.getId(), feature.getVersion());
			}
		}
	}
	
	private IFeatureModel[] getAvailableChoices() {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getModels();
		IProduct product = getProduct();
		ArrayList list = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			String id = models[i].getFeature().getId();
			if (id != null && !product.containsFeature(id)) {
				list.add(models[i]);
			}
		}
		return (IFeatureModel[])list.toArray(new IFeatureModel[list.size()]);
	}

	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		Object[] objects = e.getChangedObjects();
		if (e.getChangeType() == IModelChangedEvent.INSERT) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IProductFeature)
					fFeatureTable.add(objects[i]);
			}
		} else if (e.getChangeType() == IModelChangedEvent.REMOVE) {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof IProductFeature)
					fFeatureTable.remove(objects[i]);
			}
		}
		updateRemoveButtons(false, true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fFeatureTable.refresh();
		updateRemoveButtons(true, true);
		super.refresh();
	}
	
	protected void selectionChanged(IStructuredSelection selection) {
		getPage().getPDEEditor().setSelection(selection);
		updateRemoveButtons(true, false);
	}

	public boolean setFormInput(Object input) {
		if (input instanceof IProductFeature) {
			fFeatureTable.setSelection(new StructuredSelection(input), true);
			return true;
		}
		return super.setFormInput(input);
	}

	private void updateRemoveButtons(boolean updateRemove, boolean updateRemoveAll) {
		TablePart tablePart = getTablePart();
		if (updateRemove) {
			ISelection selection = getViewerSelection();
			tablePart.setButtonEnabled(1,
					isEditable() &&	!selection.isEmpty() && selection instanceof IStructuredSelection && 
					((IStructuredSelection)selection).getFirstElement() instanceof IProductFeature);
		}
		if (updateRemoveAll)
			tablePart.setButtonEnabled(2, isEditable() && fFeatureTable.getTable().getItemCount() > 0);
	}
	
}
