/*
 * Created on Jan 30, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ResourceAttributeRow extends ReferenceAttributeRow {
	public ResourceAttributeRow(ISchemaAttribute att) {
		super(att);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#openReference()
	 */
	protected void openReference() {
		// open the resource for editing
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.plugin.ReferenceAttributeRow#browse()
	 */
	protected void browse() {
		// open the resource selection dialog
	}
}