/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class TextAttributeRow extends ExtensionAttributeRow {
	protected Text text;
	/**
	 * @param att
	 */
	public TextAttributeRow(ISchemaAttribute att) {
		super(att);
	}
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		createLabel(parent, toolkit);
		text = toolkit.createText(parent, "", SWT.SINGLE);
		text.setLayoutData(createGridData(span));
	}
	protected GridData createGridData(int span) {
		GridData gd = new GridData(span == 2
				? GridData.FILL_HORIZONTAL
				: GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 20;
		gd.horizontalSpan = span - 1;
		return gd;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#update(org.eclipse.pde.internal.ui.neweditor.plugin.DummyExtensionElement)
	 */
	protected void update() {
		String value = input != null ? input.getProperty(att.getName()) : null;
		text.setText(value != null ? value : "");
	}
	public void setFocus() {
		text.setFocus();
	}
}