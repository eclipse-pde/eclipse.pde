package org.eclipse.pde.internal.ui.editor.schema;

import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.core.schema.Schema;
import org.eclipse.pde.internal.core.schema.SchemaComplexType;
import org.eclipse.pde.internal.core.schema.SchemaCompositor;
import org.eclipse.pde.internal.core.schema.SchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaElementReference;

public class SchemaRearranger {

	private Schema fSchema;
	
	public SchemaRearranger(Schema schema) {
		fSchema = schema;
	}
	
	
	public void moveCompositor(ISchemaObject newParent, ISchemaCompositor compositor) {
		ISchemaObject oldParent = compositor.getParent();
		if (!(newParent != null 
				&& compositor != null
				&& !newParent.equals(oldParent)
				&& !newParent.equals(compositor)))
			return;
		if (newParent instanceof SchemaElement) {
			SchemaElement element = (SchemaElement)newParent;
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
		} else // unknown new parent, abort
			return;
		
		if (oldParent instanceof SchemaElement) {
			ISchemaType oldType = ((SchemaElement)oldParent).getType();
			if (oldType instanceof ISchemaComplexType) {
				((SchemaComplexType)oldType).setCompositor(null);
			}
		} else if (oldParent instanceof SchemaCompositor) {
			((SchemaCompositor)oldParent).removeChild(compositor);
		}
		compositor.setParent(newParent);
	}

	public void moveReference(SchemaElementReference reference, ISchemaCompositor compositor) {
		ISchemaCompositor oldCompositor = reference.getCompositor();
		if (!(compositor != null 
				&& reference != null
				&& oldCompositor != null
				&& !compositor.equals(oldCompositor)))
			return;
		if (compositor instanceof SchemaCompositor) {
			((SchemaCompositor) oldCompositor).removeChild(reference);
			reference.setCompositor(compositor);
			((SchemaCompositor) compositor).addChild(reference);
		}
	}
	
	public void moveElement(ISchemaObject parent, ISchemaElement element, ISchemaObject sibling) {
		if (element == null)
			return;
		if (fSchema.equals(parent)) {
			fSchema.moveElementToSibling(element, sibling);
		}
	}
	
	public void moveAttribute(ISchemaElement newParent, ISchemaAttribute attribute, ISchemaAttribute sibling) {
		ISchemaObject oldParent = attribute.getParent();
		if (!(attribute != null 
				&& newParent != null
				&& oldParent != null))
			return;
		SchemaComplexType type = null;
		if (newParent.getType() instanceof SchemaComplexType) {
			type = (SchemaComplexType) newParent.getType();
		} else {
			type = new SchemaComplexType(newParent.getSchema());
			((SchemaElement)newParent).setType(type);
		}
		if (newParent.equals(oldParent)) {
			type.moveAttributeTo(attribute, sibling);
		} else {
			if (oldParent instanceof ISchemaElement
					&& ((ISchemaElement)oldParent).getType() instanceof SchemaComplexType) {
				SchemaComplexType oldType = (SchemaComplexType)((ISchemaElement)oldParent).getType();
				oldType.removeAttribute(attribute);
			}
			attribute.setParent(newParent);
			type.addAttribute(attribute, sibling);
		}
	}
	
}
