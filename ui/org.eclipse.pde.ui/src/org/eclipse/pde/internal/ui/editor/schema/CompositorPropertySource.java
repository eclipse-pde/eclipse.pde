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
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;

public class CompositorPropertySource extends GrammarPropertySource {
	public static final String P_KIND = "kind"; //$NON-NLS-1$
	public static final String KEY_KIND = "SchemaEditor.CompositorPR.kind"; //$NON-NLS-1$

public CompositorPropertySource(ISchemaCompositor obj) {
	super(obj);
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	if (descriptors == null) {
		descriptors = (Vector) super.getPropertyDescriptorsVector();
		PropertyDescriptor cdesc =
			createComboBoxPropertyDescriptor(P_KIND, PDEPlugin.getResourceString(KEY_KIND), ISchemaCompositor.kindTable);
		if (cdesc instanceof ComboBoxPropertyDescriptor)
			((ComboBoxPropertyDescriptor) cdesc).setLabelProvider(
				new ComboProvider(P_KIND, ISchemaCompositor.kindTable));
		descriptors.addElement(cdesc);
	}
	return toDescriptorArray(descriptors);
}
public Object getPropertyValue(Object name) {
	ISchemaCompositor compositor = (ISchemaCompositor)getSourceObject();
	if (name.equals(P_KIND)) {
		return new Integer(compositor.getKind());
	}
	return super.getPropertyValue(name);
}
public void setPropertyValue(String name, Object value) {
	SchemaCompositor compositor = (SchemaCompositor)getSourceObject();
	
	if (name.equals(P_KIND)) {
		compositor.setKind(((Integer)value).intValue());
	}
	else super.setPropertyValue(name, value);
}
}
