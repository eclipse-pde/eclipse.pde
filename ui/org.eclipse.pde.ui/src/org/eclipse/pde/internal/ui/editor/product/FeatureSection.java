package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.feature.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.forms.widgets.*;


public class FeatureSection extends TableSection {
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return getProduct().getFeatures();
		}
	}

	private TableViewer fFeatureTable;

	public FeatureSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels()); //$NON-NLS-1$
	}
	
	private static String[] getButtonLabels() {
		String[] labels = new String[5];
		labels[0] = PDEPlugin.getResourceString("Product.FeatureSection.add"); //$NON-NLS-1$
		labels[1] = PDEPlugin.getResourceString("Product.PluginSection.removeAll"); //$NON-NLS-1$
		labels[2] = null;
		labels[3] = null;
		labels[4] = "New Feature...";
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
		fFeatureTable.setInput(PDECore.getDefault().getFeatureModelManager());
		
		tablePart.setButtonEnabled(0, isEditable());
		tablePart.setButtonEnabled(1, isEditable());
		tablePart.setButtonEnabled(4, isEditable());
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
		section.setText(PDEPlugin.getResourceString("Product.FeatureSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("Product.FeatureSection.desc")); //$NON-NLS-1$		

		getProduct().getModel().addModelChangedListener(this);	
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
			handleRemoveAll();
			break;
		case 4:
			handleNewFeature();
		}
	}
	
	private void handleNewFeature() {
		NewFeatureProjectWizard wizard = new NewFeatureProjectWizard();
		WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 500);
		if (dialog.open() == WizardDialog.OK) {
			addFeature(wizard.getFeatureId(), wizard.getFeatureVersion());
		}
	}
	
	private void addFeature(String id, String version) {
		IProduct product = getProduct();
		IProductModelFactory factory = product.getModel().getFactory();
		IProductFeature feature = factory.createFeature();
		feature.setId(id);
		feature.setVersion(version);
		product.addFeature(feature);
	}
	

	private void handleRemoveAll() {
		TableItem[] items = fFeatureTable.getTable().getItems();
		IProduct product = getProduct();
		for (int i = 0; i < items.length; i++) {
			product.removeFeature((IProductFeature)items[i].getData());
		}
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
		getProduct().getModel().removeModelChangedListener(this);
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
		return false;
	}
	
	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection)fFeatureTable.getSelection();
		if (ssel.size() > 0) {
			Object[] objects = ssel.toArray();
			IProduct product = getProduct();
			for (int i = 0; i < objects.length; i++) {
				product.removeFeature((IProductFeature)objects[i]);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
	}

	private void handleOpen(IStructuredSelection selection) {
	}

	private void handleAdd() {
		FeatureSelectionDialog dialog = new FeatureSelectionDialog(PDEPlugin.getActiveWorkbenchShell(), getAvailableChoices());
		dialog.setMultipleSelection(true);
		if (dialog.open() == FeatureSelectionDialog.OK) {
			Object[] models = dialog.getResult();
			for (int i = 0; i < models.length; i++) {
				IFeature feature = ((IFeatureModel)models[i]).getFeature();
				addFeature(feature.getId(), feature.getVersion());
			}
		}
	}
	
	private IFeatureModel[] getAvailableChoices() {
		IFeatureModel[] models = PDECore.getDefault().getFeatureModelManager().getAllFeatures();
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
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
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
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fFeatureTable.refresh();
		super.refresh();
	}

}
