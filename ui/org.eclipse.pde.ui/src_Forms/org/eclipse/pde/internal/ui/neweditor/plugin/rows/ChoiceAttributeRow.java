/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin.rows;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.IContextPart;
import org.eclipse.pde.internal.ui.newparts.ComboPart;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ChoiceAttributeRow extends ExtensionAttributeRow {
	private ComboPart combo;
	/**
	 * @param att
	 */
	public ChoiceAttributeRow(IContextPart part, ISchemaAttribute att) {
		super(part, att);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#createContents(org.eclipse.swt.widgets.Composite, org.eclipse.ui.forms.widgets.FormToolkit, int)
	 */
	
	private Control createCombo(Composite parent, int borderStyle) {
		if (borderStyle==SWT.BORDER)
			return new Combo(parent, SWT.READ_ONLY | SWT.BORDER);
		else
			return new CCombo(parent, SWT.READ_ONLY| SWT.FLAT);
	}
	
	public void createContents(Composite parent, FormToolkit toolkit, int span) {
		createLabel(parent, toolkit);
		combo = new ComboPart();
		combo.createControl(parent, toolkit, SWT.READ_ONLY);
		ISchemaSimpleType type = att.getType();
		ISchemaRestriction restriction = type.getRestriction();
		if (restriction!=null) {
			Object rchildren[] = restriction.getChildren();
			if (att.getUse()!=ISchemaAttribute.REQUIRED)
				combo.add("");
			for (int i=0; i<rchildren.length; i++) {
				Object rchild = rchildren[i];
				if (rchild instanceof ISchemaEnumeration)
					combo.add(((ISchemaEnumeration)rchild).getName());
			}
		}
		GridData gd = new GridData(span==2?GridData.FILL_HORIZONTAL:GridData.HORIZONTAL_ALIGN_FILL);
		gd.widthHint = 20;
		gd.horizontalSpan = span-1;
		combo.getControl().setLayoutData(gd);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!blockNotification) markDirty();
			}
		});
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ExtensionElementEditor#update(org.eclipse.pde.internal.ui.neweditor.plugin.DummyExtensionElement)
	 */
	protected void update() {
		blockNotification=true;
		String value = null;
		if (input!=null) {
			IPluginAttribute patt = input.getAttribute(att.getName());
			if (patt!=null)
				value = patt.getValue();
		}
		combo.setText(value!=null?value:"");
		blockNotification = false;
		dirty=false;
	}
	public void commit() {
		if (dirty && input != null) {
			try {
				String selection = combo.getSelection();
				if (selection.length()==0) selection = null;
				input.setAttribute(att.getName(), selection);
				dirty = false;
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}	
	public void setFocus() {
		combo.getControl().setFocus();
	}
}