package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;


public class PluginSection extends TableSection implements IPluginModelListener{
	
	class ContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			return getProduct().getPlugins();
		}
	}

	private TableViewer fPluginTable;
	private Button fIncludeFragments;

	public PluginSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, Section.DESCRIPTION, getButtonLabels());
	}
	
	private static String[] getButtonLabels() {
		String[] labels = new String[3];
		labels[0] = "Add...";
		labels[1] = "Add Working Set...";
		labels[2] = "Add Required Plug-ins";
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
		fPluginTable = tablePart.getTableViewer();
		fPluginTable.setContentProvider(new ContentProvider());
		fPluginTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		
		fIncludeFragments = toolkit.createButton(container, "Include fragments when computing required plug-ins", SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fIncludeFragments.setLayoutData(gd);
		
		toolkit.paintBordersFor(container);
		section.setClient(container);
		
		gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
		section.setText("Plug-ins and Fragments");
		section.setDescription("List all the plug-ins and fragments that constitute the product:");
		PDECore.getDefault().getModelManager().addPluginModelListener(this);
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
			handleAddWorkingSet();
			break;
		case 2:
			handleAddRequired();
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
		PDECore.getDefault().getModelManager().removePluginModelListener(this);
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#doGlobalAction(java.lang.String)
	 */
	public boolean doGlobalAction(String actionId) {
		return super.doGlobalAction(actionId);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.StructuredViewerSection#fillContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	protected void fillContextMenu(IMenuManager manager) {
		super.fillContextMenu(manager);
	}

	private void handleOpen(IStructuredSelection selection) {
	}

	private void handleAddRequired() {
	}

	private void handleAddWorkingSet() {
	}

	private void handleAdd() {
	}
	
	private IProduct getProduct() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		super.modelChanged(e);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		fPluginTable.refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.IPluginModelListener#modelsChanged(org.eclipse.pde.internal.core.PluginModelDelta)
	 */
	public void modelsChanged(PluginModelDelta delta) {
		final Control control = fPluginTable.getControl();
		if (!control.isDisposed()) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (!control.isDisposed())
						fPluginTable.refresh();
				}
			});
		}
	}
	

}
