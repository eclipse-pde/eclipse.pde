package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.*;


public class ProductInfoSection extends PDESection {

	private FormEntry fNameEntry;
	private FormEntry fIdEntry;
	private ComboPart fAppCombo;

	public ProductInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.TITLE_BAR);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Product Definition");
		
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginWidth = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 3;
		client.setLayout(layout);

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		createNameEntry(client, toolkit, actionBars);
		createIdEntry(client, toolkit, actionBars);
		createApplicationEntry(client, toolkit, actionBars);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
		GridData gd = new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING);
		gd.verticalSpan = 2;
		section.setLayoutData(gd);
	}
	
	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, "Product Name:", null, false); //$NON-NLS-1$
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setName(entry.getValue());
			}
		});
		fNameEntry.setEditable(isEditable());
	}
	
	private void createIdEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIdEntry = new FormEntry(client, toolkit, "Product ID:", "Browse...", false); //$NON-NLS-1$
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setId(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {			
			}
		});
		fIdEntry.setEditable(isEditable());
	}

	private void createApplicationEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		Label label = toolkit.createLabel(client, "Application:");
		label.setForeground(toolkit.getColors().getColor(FormColors.TITLE));
		
		fAppCombo = new ComboPart();
		fAppCombo.createControl(client, toolkit, SWT.READ_ONLY);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fAppCombo.getControl().setLayoutData(gd);
		fAppCombo.setItems(TargetPlatform.getApplicationNames());
		fAppCombo.add("");
		
		fAppCombo.getControl().setEnabled(isEditable());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fNameEntry.commit();
		fIdEntry.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fNameEntry.cancelEdit();
		fIdEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private IProduct getProduct() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		IProduct product = getProduct();
		fNameEntry.setValue(product.getName(), true);
		fIdEntry.setValue(product.getId(), true);
		fAppCombo.setText(product.getApplication());
		super.refresh();
	}

}
