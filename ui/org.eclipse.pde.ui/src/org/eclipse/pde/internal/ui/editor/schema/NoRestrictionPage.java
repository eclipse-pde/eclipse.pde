/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.ischema.*;
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
