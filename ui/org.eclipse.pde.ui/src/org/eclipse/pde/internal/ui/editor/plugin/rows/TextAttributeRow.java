/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.editor.plugin.rows;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
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
	public TextAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	
	public TextAttributeRow(IContextPart part, IPluginAttribute att) {
		super(part, att);
	}
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		createLabel(parent, toolkit);
		text = toolkit.createText(parent, "", SWT.SINGLE);
		text.setLayoutData(createGridData(span));
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!blockNotification) markDirty();
			}
		});
		text.setEditable(part.isEditable());
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
		blockNotification = true;
		String value = getValue();
		text.setText(value != null ? value : "");
		blockNotification = false;
	}
	public void commit() {
		if (dirty && input!=null) {
			String value = text.getText();
			if (value.length()==0) value=null;
			try {
				input.setAttribute(getName(), value);
				dirty = false;
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}
	public void setFocus() {
		text.setFocus();
	}
}