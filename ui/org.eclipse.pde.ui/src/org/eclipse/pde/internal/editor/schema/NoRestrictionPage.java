package org.eclipse.pde.internal.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.schema.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.*;

public class NoRestrictionPage implements IRestrictionPage {
	private Control control;

public Control createControl(Composite parent) {
	control = new Composite(parent, SWT.NULL);
	return control;
}
public Class getCompatibleRestrictionClass() {
	return null;
}
public org.eclipse.swt.widgets.Control getControl() {
	return control;
}
public ISchemaRestriction getRestriction() {
	return new ChoiceRestriction((ISchema)null);
}
public void initialize(ISchemaRestriction restriction) {
}
}
