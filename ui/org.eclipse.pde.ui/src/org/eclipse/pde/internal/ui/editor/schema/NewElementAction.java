/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import java.util.Hashtable;

import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRootElement;
import org.eclipse.pde.internal.core.schema.SchemaSimpleType;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class NewElementAction extends Action {
	private Schema schema;
	private static final String NAME_COUNTER_KEY = "__schema_element_name"; //$NON-NLS-1$
	public NewElementAction() {
		setText(PDEUIMessages.SchemaEditor_NewElement_label);
		setImageDescriptor(PDEPluginImages.DESC_GEL_SC_OBJ);
		setToolTipText(PDEUIMessages.SchemaEditor_NewElement_tooltip);
	}
	private String getInitialName() {
		Hashtable counters = PDEPlugin.getDefault().getDefaultNameCounters();
		Integer counter = (Integer) counters.get(NAME_COUNTER_KEY);
		if (counter == null) {
			counter = new Integer(1);
		} else {
			counter = new Integer(counter.intValue() + 1);
		}
		counters.put(NAME_COUNTER_KEY, counter);
		return NLS.bind(PDEUIMessages.SchemaEditor_NewElement_initialName, counter.intValue() + ""); //$NON-NLS-1$
	}
	public org.eclipse.pde.internal.core.schema.Schema getSchema() {
		return schema;
	}
	public void run() {
		String name = getInitialName();
		SchemaElement element;
		if (name.equals("extension")) //$NON-NLS-1$
			element = new SchemaRootElement(schema, name);
		else
			element = new SchemaElement(schema, name);
		element.setType(new SchemaSimpleType(schema, "string")); //$NON-NLS-1$
		schema.addElement(element);
		schema.updateReferencesFor(element, ISchema.REFRESH_ADD);
	}
	public void setSchema(Schema newSchema) {
		schema = newSchema;
	}
}
