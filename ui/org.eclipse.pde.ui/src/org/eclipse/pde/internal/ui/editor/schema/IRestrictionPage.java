package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.swt.widgets.*;

public interface IRestrictionPage {
	public Control createControl(Composite parent);
	public Class getCompatibleRestrictionClass();
	public Control getControl();
	public ISchemaRestriction getRestriction();
	public void initialize(ISchemaRestriction restriction);
}
