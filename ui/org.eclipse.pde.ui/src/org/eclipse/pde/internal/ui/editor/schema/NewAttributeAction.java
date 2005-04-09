/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.*;

import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.*;

public class NewAttributeAction extends Action {
	private SchemaElement element;
	private static final String NAME_COUNTER_KEY = "__schema_attribute_name"; //$NON-NLS-1$
	public NewAttributeAction() {
	setText(PDEUIMessages.SchemaEditor_NewAttribute_label);
	setImageDescriptor(PDEPluginImages.DESC_ATT_IMPL_OBJ);
	setToolTipText(PDEUIMessages.SchemaEditor_NewAttribute_tooltip);
}
public org.eclipse.pde.internal.core.schema.SchemaElement getElement() {
	return element;
}
private String getInitialName() {
	Hashtable counters = PDEPlugin.getDefault().getDefaultNameCounters();
	Integer counter = (Integer)counters.get(NAME_COUNTER_KEY);
	if (counter==null) {
		counter = new Integer(1);
	}
	else {
		counter = new Integer(counter.intValue()+1);
	}
	counters.put(NAME_COUNTER_KEY, counter);
	return NLS.bind(PDEUIMessages.SchemaEditor_NewAttribute_initialName, counter.intValue()+""); //$NON-NLS-1$
}
public void run() {
	String name = getInitialName();
	SchemaAttribute att = new SchemaAttribute(element, name);
	att.setType(new SchemaSimpleType(element.getSchema(), "string")); //$NON-NLS-1$
	ISchemaType type = element.getType();
	SchemaComplexType complexType=null;
	if (!(type instanceof ISchemaComplexType)) {
		complexType = new SchemaComplexType(element.getSchema());
		element.setType(complexType);
	}
	else {
		complexType = (SchemaComplexType)type;
	}
	complexType.addAttribute(att);
}
public void setElement(org.eclipse.pde.internal.core.schema.SchemaElement newElement) {
	element = newElement;
}
}
