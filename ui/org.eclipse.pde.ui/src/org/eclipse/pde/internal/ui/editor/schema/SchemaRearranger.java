/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class SchemaRearranger {

	private Schema fSchema;

	public SchemaRearranger(Schema schema) {
		fSchema = schema;
	}

	public void moveCompositor(ISchemaObject newParent, ISchemaCompositor compositor) {
		ISchemaObject oldParent = compositor.getParent();
		if (!(newParent != null && compositor != null && !newParent.equals(oldParent) && !newParent.equals(compositor)))
			return;
		if (newParent instanceof SchemaElement) {
			SchemaElement element = (SchemaElement) newParent;
			SchemaComplexType type = null;
			if (element.getType() instanceof SchemaComplexType) {
				type = (SchemaComplexType) element.getType();
				type.setCompositor(compositor);
			} else {
				type = new SchemaComplexType(element.getSchema());
				type.setCompositor(compositor);
				element.setType(type);
			}
		} else if (newParent instanceof SchemaCompositor) {
			((SchemaCompositor) newParent).addChild(compositor);
		} else
			// unknown new parent, abort
			return;

		if (oldParent instanceof SchemaElement) {
			ISchemaType oldType = ((SchemaElement) oldParent).getType();
			if (oldType instanceof ISchemaComplexType) {
				((SchemaComplexType) oldType).setCompositor(null);
			}
		} else if (oldParent instanceof SchemaCompositor) {
			((SchemaCompositor) oldParent).removeChild(compositor);
		}
		compositor.setParent(newParent);
	}

	public void moveReference(SchemaElementReference reference, ISchemaCompositor compositor, ISchemaObject sibling) {
		ISchemaCompositor oldCompositor = reference.getCompositor();
		if (!(compositor != null && reference != null && oldCompositor != null))
			return;
		if (compositor instanceof SchemaCompositor) {
			if (compositor.equals(oldCompositor)) {
				((SchemaCompositor) compositor).moveChildToSibling(reference, sibling);
			} else {
				((SchemaCompositor) oldCompositor).removeChild(reference);
				reference.setCompositor(compositor);
				((SchemaCompositor) compositor).addChild(reference);
			}
		}
	}

	public void moveElement(ISchemaObject parent, ISchemaElement element, ISchemaObject sibling) {
		if (element == null)
			return;
		if (fSchema.equals(parent)) {
			fSchema.moveElementToSibling(element, sibling);
		} else if (parent instanceof ISchemaCompositor) {
			linkReference((ISchemaCompositor) parent, element, sibling);
		}
	}

	public void moveAttribute(ISchemaElement newParent, ISchemaAttribute attribute, ISchemaAttribute sibling) {
		ISchemaObject oldParent = attribute.getParent();
		if (!(attribute != null && newParent != null && oldParent != null))
			return;
		SchemaComplexType type = null;
		if (newParent.getType() instanceof SchemaComplexType) {
			type = (SchemaComplexType) newParent.getType();
		} else {
			type = new SchemaComplexType(newParent.getSchema());
			((SchemaElement) newParent).setType(type);
		}
		if (newParent.equals(oldParent)) {
			type.moveAttributeTo(attribute, sibling);
		} else {
			if (oldParent instanceof ISchemaElement && ((ISchemaElement) oldParent).getType() instanceof SchemaComplexType) {
				SchemaComplexType oldType = (SchemaComplexType) ((ISchemaElement) oldParent).getType();
				oldType.removeAttribute(attribute);
			}
			attribute.setParent(newParent);
			if (attribute instanceof SchemaAttribute)
				((SchemaAttribute) attribute).setName(PDELabelUtility.generateName(newParent.getAttributeNames(), PDELabelUtility.getBaseName(attribute.getName(), false), false));
			type.addAttribute(attribute, sibling);
		}
	}

	public void pasteCompositor(ISchemaObject realTarget, ISchemaCompositor compositor, ISchemaObject sibling) {
		if (realTarget instanceof SchemaElement) {
			SchemaElement element = (SchemaElement) realTarget;
			SchemaComplexType type = null;
			if (element.getType() instanceof SchemaComplexType) {
				type = (SchemaComplexType) element.getType();
				type.setCompositor(compositor);
			} else {
				type = new SchemaComplexType(element.getSchema());
				element.setType(type);
				type.setCompositor(compositor);
			}
		} else if (realTarget instanceof SchemaCompositor) {
			((SchemaCompositor) realTarget).addChild(compositor, sibling);
		}
	}

	public void pasteReference(ISchemaObject realTarget, ISchemaObjectReference object, ISchemaObject sibling) {
		if (realTarget instanceof SchemaCompositor) {
			SchemaCompositor parent = (SchemaCompositor) realTarget;
			((SchemaElementReference) object).setCompositor(parent);
			parent.addChild((SchemaElementReference) object, sibling);
		}
	}

	public void pasteElement(ISchemaElement object, ISchemaObject sibling) {
		SchemaElement element = (SchemaElement) object;
		element.setParent(fSchema);
		element.setName(PDELabelUtility.generateName(element.getSchema().getElementNames(), PDELabelUtility.getBaseName(element.getName(), false), false));
		fSchema.addElement(element, (ISchemaElement) sibling);
		fSchema.updateReferencesFor(element, ISchema.REFRESH_ADD);
	}

	public void pasteAttribute(ISchemaElement realTarget, ISchemaAttribute object, ISchemaObject sibling) {
		SchemaElement element = (SchemaElement) realTarget;
		SchemaAttribute attribute = (SchemaAttribute) object;
		attribute.setParent(element);
		attribute.setName(PDELabelUtility.generateName(element.getAttributeNames(), PDELabelUtility.getBaseName(attribute.getName(), false), false));
		ISchemaType type = element.getType();
		SchemaComplexType complexType = null;
		if (!(type instanceof ISchemaComplexType)) {
			complexType = new SchemaComplexType(element.getSchema());
			element.setType(complexType);
		} else {
			complexType = (SchemaComplexType) type;
		}
		if (sibling instanceof ISchemaAttribute)
			complexType.addAttribute(attribute, (ISchemaAttribute) sibling);
		else
			complexType.addAttribute(attribute);
	}

	public void linkReference(ISchemaCompositor realTarget, ISchemaElement object, ISchemaObject sibling) {
		if (sibling instanceof SchemaElementReference)
			realTarget = ((SchemaElementReference) sibling).getCompositor();

		SchemaCompositor parent = (SchemaCompositor) realTarget;
		String refName = object.getName();
		SchemaElementReference reference = new SchemaElementReference(parent, refName);
		reference.setReferencedObject(fSchema.findElement(refName));
		parent.addChild(reference, sibling);
	}

	public void deleteCompositor(ISchemaCompositor compositor) {
		ISchemaObject cparent = compositor.getParent();
		if (cparent instanceof ISchemaElement) {
			SchemaElement element = (SchemaElement) cparent;
			ISchemaType type = element.getType();
			if (type instanceof SchemaComplexType && ((SchemaComplexType) type).getAttributeCount() != 0)
				((SchemaComplexType) type).setCompositor(null);
			else
				element.setType(new SchemaSimpleType(element.getSchema(), "string")); //$NON-NLS-1$

		} else if (cparent instanceof SchemaCompositor) {
			((SchemaCompositor) cparent).removeChild(compositor);
		}
	}

	public void deleteAttribute(ISchemaAttribute attribute) {
		ISchemaElement element = (ISchemaElement) attribute.getParent();
		SchemaComplexType type = (SchemaComplexType) element.getType();
		type.removeAttribute(attribute);
	}

	public void deleteElement(ISchemaElement element) {
		if (!(element instanceof ISchemaRootElement)) {
			Schema schema = (Schema) element.getParent();
			schema.removeElement(element);
			schema.updateReferencesFor(element, ISchema.REFRESH_DELETE);
		}
	}

	public void deleteReference(SchemaElementReference reference) {
		SchemaCompositor compositor = (SchemaCompositor) reference.getCompositor();
		compositor.removeChild(reference);
	}
}
