package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
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
		TableWrapLayout layout = new TableWrapLayout();
		layout.leftMargin = layout.rightMargin = toolkit.getBorderStyle() != SWT.NULL ? 0 : 2;
		layout.numColumns = 3;
		layout.verticalSpacing = 10;
		comp.setLayout(layout);
		
		FormText text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.validate"), true, false);
		TableWrapData td = new TableWrapData();
		td.colspan = 3;
		text.setLayoutData(td);
				
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fArchiveEntry = new FormEntry(comp, toolkit, "Archive:", "Browse...", false, 45);
		fArchiveEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		fArchiveEntry.setEditable(isEditable());
		fArchiveEntry.getText().setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
		
		text = toolkit.createFormText(comp, true);
		text.setText(PDEPlugin.getResourceString("Product.overview.export"), true, false);
		td = new TableWrapData();
		td.colspan = 3;
		text.setLayoutData(td);
		
		toolkit.paintBordersFor(comp);
		section.setClient(comp);
		section.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
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

}
