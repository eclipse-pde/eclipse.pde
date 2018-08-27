/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
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

		String text = upperCaseFirstLetter(ISchemaCompositor.kindTable[kind]);
		setText("&" + text); //$NON-NLS-1$
		setToolTipText(NLS.bind(PDEUIMessages.SchemaEditor_NewCompositor_tooltip, text));
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

	/**
	 * @param text must have a length of at least two
	 */
	private String upperCaseFirstLetter(String text) {
		if ((text == null) || (text.length() < 2)) {
			return text;
		}
		String firstLetter = text.substring(0, 1).toUpperCase();
		String rest = text.substring(1);

		return firstLetter + rest;
	}

	@Override
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
					for (ISchemaObject oldChild : oldChildren) {
						compositor.addChild(oldChild);
					}
				}
				type.setCompositor(compositor);
			} else {
				type = new SchemaComplexType(source.getSchema());
				type.setCompositor(compositor);
				((SchemaElement) source).setType(type);
			}
			// Any element that defines a root compositor cannot be translatable
			if (element.hasTranslatableContent()) {
				element.setTranslatableProperty(false);
			}
		} else if (object instanceof SchemaCompositor) {
			((SchemaCompositor) object).addChild(compositor);
		}
	}
}
