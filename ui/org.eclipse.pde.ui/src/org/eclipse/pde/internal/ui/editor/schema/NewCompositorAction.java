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

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.resource.*;

public class NewCompositorAction extends Action {
	public static final String KEY_TOOLTIP = "SchemaEditor.NewCompositor.tooltip"; //$NON-NLS-1$
	private ISchemaElement source;
	private Object object;
	private int kind;

public NewCompositorAction(ISchemaElement source, Object object, int kind) {
	this.source = source;
	this.object = object;
	this.kind = kind;
	setText("&"+ISchemaCompositor.kindTable[kind]); //$NON-NLS-1$
	setToolTipText(PDEPlugin.getFormattedMessage(KEY_TOOLTIP, ISchemaCompositor.kindTable[kind]));
	ImageDescriptor desc = null;

	switch (kind) {
		case ISchemaCompositor.ALL :
			desc = PDEPluginImages.DESC_ALL_SC_OBJ;
			break;
		case ISchemaCompositor.GROUP :
			desc = PDEPluginImages.DESC_GROUP_SC_OBJ;
			break;
		case ISchemaCompositor.SEQUENCE :
			desc = PDEPluginImages.DESC_SEQ_SC_OBJ;
			break;
		case ISchemaCompositor.CHOICE :
			desc = PDEPluginImages.DESC_CHOICE_SC_OBJ;
			break;

	}
	setImageDescriptor(desc);
	setEnabled(source.getSchema().isEditable());
}
public void run() {
	SchemaCompositor compositor = new SchemaCompositor(source, kind);

	if (object == null) {
		// first time
		SchemaComplexType type = null;
		SchemaElement element = (SchemaElement) source;
		if (element.getType() instanceof SchemaComplexType) {
			type = (SchemaComplexType) element.getType();
			type.setCompositor(compositor);
		} else {
			type = new SchemaComplexType(source.getSchema());
			type.setCompositor(compositor);
			((SchemaElement) source).setType(type);
		}
	} else
		if (object instanceof SchemaCompositor) {
			((SchemaCompositor) object).addChild(compositor);
		} else
			if (object instanceof SchemaElementReference) {
				ISchemaCompositor comp = ((SchemaElementReference) object).getCompositor();
				((SchemaCompositor) comp).addChild(compositor);
			}
}
}
