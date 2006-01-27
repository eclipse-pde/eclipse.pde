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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.schema.SchemaComplexType;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class NewCompositorAction extends Action {
	private ISchemaElement source;
	private Object object;
	private int kind;

	public NewCompositorAction(ISchemaElement source, Object object, int kind) {
		this.source = source;
		this.object = object;
		this.kind = kind;
		setText("&" + ISchemaCompositor.kindTable[kind]); //$NON-NLS-1$
		setToolTipText(NLS.bind(PDEUIMessages.SchemaEditor_NewCompositor_tooltip, ISchemaCompositor.kindTable[kind]));
		ImageDescriptor desc = null;
	
		switch (kind) {
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
		if (object instanceof SchemaElement) {
			SchemaComplexType type = null;
			SchemaElement element = (SchemaElement) source;
			if (element.getType() instanceof SchemaComplexType) {
				type = (SchemaComplexType) element.getType();
				ISchemaCompositor oldComp = type.getCompositor();
				if (oldComp != null) {
					ISchemaObject[] oldChildren = oldComp.getChildren();
					for (int i = 0; i < oldChildren.length; i++) {
						compositor.addChild(oldChildren[i]);
					}
				}
				type.setCompositor(compositor);
			} else {
				type = new SchemaComplexType(source.getSchema());
				type.setCompositor(compositor);
				((SchemaElement) source).setType(type);
			}
		} else if (object instanceof SchemaCompositor) {
			((SchemaCompositor) object).addChild(compositor);
		}
	}
}
