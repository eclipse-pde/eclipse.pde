/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.*;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public abstract class ReferenceAttributeRow extends TextAttributeRow {
	/**
	 * @param att
	 */
	public ReferenceAttributeRow(ISchemaAttribute att) {
		super(att);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#createContents(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.ui.forms.widgets.FormToolkit, int)
	 */
	protected void createLabel(Composite parent, FormToolkit toolkit) {
		Hyperlink link = toolkit.createHyperlink(parent, getPropertyLabel(),
				SWT.NULL);
		link.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				openReference();
			}
		});
		link.setToolTipText(getToolTipText());
	}
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		super.createContents(parent, toolkit, span);
		Button button = toolkit.createButton(parent, "...", SWT.PUSH);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				browse();
			}
		});
	}
	protected GridData createGridData(int span) {
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.widthHint = 20;
		return gd;
	}
	
	protected abstract void openReference();
	protected abstract void browse();
}