/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin.rows;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class BooleanAttributeRow extends ExtensionAttributeRow {
	private Button button;
	/**
	 * @param att
	 */
	public BooleanAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#createContents(org.eclipse.swt.widgets.Composite,
	 *      org.eclipse.ui.forms.widgets.FormToolkit, int)
	 */
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		createLabel(parent, toolkit);
		button = toolkit.createButton(parent, "", SWT.CHECK);
		GridData gd = new GridData();
		//gd.horizontalIndent = 10;
		gd.horizontalSpan = span - 1;
		//gd.horizontalSpan = span;
		button.setLayoutData(gd);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!blockNotification) markDirty();
				updateText();
			}
		});
		button.setEnabled(part.isEditable());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#update(org.eclipse.pde.internal.ui.neweditor.plugin.DummyExtensionElement)
	 */
	protected void update() {
		blockNotification = true;
		String value = getValue();
		boolean state = value != null && value.toLowerCase().equals("true");
		button.setSelection(state);
		updateText();
		blockNotification=false;
	}
	
	private void updateText() {
		boolean state = button.getSelection();
		button.setText(state?"true":"false");
	}
	public void commit() {
		if (dirty && input != null) {
			try {
				input.setAttribute(getName(), button.getSelection()
						? "true"
						: "false");
				dirty = false;
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	public void setFocus() {
		button.setFocus();
	}
}