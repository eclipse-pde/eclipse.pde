package org.eclipse.pde.internal.ui.editor.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.ischema.*;
import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.resource.*;

public class NewCompositorAction extends Action {
	public static final String KEY_TOOLTIP = "SchemaEditor.NewCompositor.tooltip";
	private ISchemaElement source;
	private Object object;
	private int kind;

public NewCompositorAction(ISchemaElement source, Object object, int kind) {
	this.source = source;
	this.object = object;
	this.kind = kind;
	setText(ISchemaCompositor.kindTable[kind]);
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
