package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.runtime.Platform;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.widgets.*;


public class ExportSection extends PDESection {

	private FormEntry fArchiveEntry;

	public ExportSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Exporting");
		section.setDescription("To package and export this product:");
		
		Composite comp = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.marginLeft = layout.marginRight = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 3;
		comp.setLayout(layout);
		
		FormText text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.validate"), true, false);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		text.setLayoutData(gd);
				
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fArchiveEntry = new FormEntry(comp, toolkit, null, "Browse...", false, 25);
		fArchiveEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getProduct().setExportDestination(entry.getValue().trim());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
			private void handleBrowse() {
				FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
				dialog.setFileName(fArchiveEntry.getValue());
				String extension = Platform.getOS().equals("macosx") ? ".tar.gz" : ".zip"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				dialog.setFilterExtensions(new String[] {"*" + extension});
				String res = dialog.open();
				if (res != null) {
					fArchiveEntry.getText().setText(res);
				}
			}
		});
		fArchiveEntry.setEditable(isEditable());
		fArchiveEntry.getText().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.export"), true, false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		text.setLayoutData(gd);
		
		toolkit.paintBordersFor(comp);
		section.setClient(comp);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#commit(boolean)
	 */
	public void commit(boolean onSave) {
		fArchiveEntry.commit();
		super.commit(onSave);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#cancelEdit()
	 */
	public void cancelEdit() {
		fArchiveEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private IProduct getProduct() {
		IBaseModel model = getPage().getPDEEditor().getAggregateModel();
		return ((IProductModel)model).getProduct();
	}
	
	private Shell getShell() {
		return getPage().getEditor().getSite().getShell();
	}
	
	public void refresh() {
		fArchiveEntry.setValue(getProduct().getExportDestination(), true);
		super.refresh();
	}


}
