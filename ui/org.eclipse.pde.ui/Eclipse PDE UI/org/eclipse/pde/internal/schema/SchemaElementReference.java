package org.eclipse.pde.internal.schema;

import java.io.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.runtime.PlatformObject;

public class SchemaElementReference extends PlatformObject implements ISchemaElement, IMetaElement, ISchemaObjectReference {
	private ISchemaElement element;
	private ISchemaCompositor compositor;
	private String referenceName;
	public static final String P_MAX_OCCURS="max_occurs";
	public static final String P_MIN_OCCURS="min_occurs";
	public static final String P_REFERENCE_NAME="reference_name";
	private int minOccurs=1;
	private int maxOccurs=1;

public SchemaElementReference(ISchemaCompositor compositor, String ref) {
	referenceName = ref;
	this.compositor = compositor;
}
public ISchemaAttribute getAttribute(String name) {
	return getReferencedElement().getAttribute(name);
}
public int getAttributeCount() {
	return getReferencedElement().getAttributeCount();
}
public ISchemaAttribute[] getAttributes() {
	return getReferencedElement().getAttributes();
}
public org.eclipse.pde.internal.base.schema.ISchemaCompositor getCompositor() {
	return compositor;
}
public java.lang.String getDescription() {
	return getReferencedElement().getDescription();
}
public String getDTDRepresentation() {
	return  getReferencedElement().getDTDRepresentation();
}
public org.eclipse.jface.resource.ImageDescriptor getIconDescriptor() {
   return getReferencedElement().getIconDescriptor();
}
public java.lang.String getIconName() {
   return getReferencedElement().getIconName();
}
public String getLabelProperty() {
   return getReferencedElement().getLabelProperty();
}
public int getMaxOccurs() {
	return maxOccurs;
}
public int getMinOccurs() {
	return minOccurs;
}
public String getName() {
	return referenceName;
}
public ISchemaObject getParent() {
	return compositor;
}
protected ISchemaElement getReferencedElement() {
	return element;
}
public ISchemaObject getReferencedObject() {
	return element;
}
public Class getReferencedObjectClass() {
	return ISchemaElement.class;
}
public String getReferenceName() {
	return referenceName;
}
public ISchema getSchema() {
	if (element != null)
		return element.getSchema();
	return null;
}
public ISchemaType getType() {
	return getReferencedElement().getType();
}
public boolean isLinked() {
	return getReferencedObject()!=null;
}
public void setCompositor(org.eclipse.pde.internal.base.schema.ISchemaCompositor newCompositor) {
	compositor = newCompositor;
}
public void setMaxOccurs(int newMaxOccurs) {
	maxOccurs = newMaxOccurs;
	ISchema schema = getSchema();
	if (schema != null)
		schema.fireModelObjectChanged(this, P_MAX_OCCURS);
}
public void setMinOccurs(int newMinOccurs) {
	minOccurs = newMinOccurs;
	ISchema schema = getSchema();
	if (schema != null)
		schema.fireModelObjectChanged(this, P_MIN_OCCURS);
}
public void setReferencedObject(ISchemaObject referencedObject) {
	if (referencedObject instanceof ISchemaElement)
		this.element = (ISchemaElement) referencedObject;
}
public void setReferenceName(String name) {
	this.referenceName = name;
	ISchema schema = getSchema();
	if (schema != null)
		schema.fireModelObjectChanged(this, P_REFERENCE_NAME);
}
public void write(String indent, PrintWriter writer) {
	writer.print(indent+"<element");
	writer.print(" ref=\""+getReferenceName()+"\"");
	if (getMinOccurs() != 1 || getMaxOccurs() != 1) {
		String min = "" + getMinOccurs();
		String max =
			getMaxOccurs() == Integer.MAX_VALUE ? "unbounded" : ("" + getMaxOccurs());
		writer.print(" minOccurs=\""+min+"\" maxOccurs=\""+max+"\"");
	}
	writer.println("/>");
}
}
