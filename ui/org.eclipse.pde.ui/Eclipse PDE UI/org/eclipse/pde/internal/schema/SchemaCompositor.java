package org.eclipse.pde.internal.schema;

import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import java.util.*;
import org.eclipse.pde.internal.base.schema.*;

public class SchemaCompositor extends RepeatableSchemaObject implements ISchemaCompositor {
	public static final String P_KIND = "p_kind";

	private int kind;
	private Vector children = new Vector();

public SchemaCompositor(ISchemaObject parent, int kind) {
	super(parent, "");
	this.kind = kind;
	switch (kind) {
		case ALL:
		name = "All";
		break;
		case CHOICE:
		name = "Choice";
		break;
		case GROUP:
		name = "Group";
		break;
		case SEQUENCE:
		name = "Sequence";
		break;
	}
}
public SchemaCompositor(ISchemaObject parent, String id, int kind) {
	super(parent, id);
	this.kind=kind;
}
public void addChild(ISchemaObject child) {
	children.addElement(child);
	getSchema().fireModelChanged(
		new ModelChangedEvent(ModelChangedEvent.INSERT, new Object[] { child }, null));
}
public int getChildCount() {
	return children.size();
}
public ISchemaObject[] getChildren() {
	ISchemaObject[] result = new ISchemaObject[children.size()];
	children.copyInto(result);
	return result;
}
public int getKind() {
	return kind;
}
public void removeChild(ISchemaObject child) {
	children.removeElement(child);
	getSchema().fireModelChanged(
		new ModelChangedEvent(ModelChangedEvent.REMOVE, new Object[] { child }, null));
}
public void setKind(int kind) {
	if (this.kind != kind) {
		this.kind = kind;
		switch (kind) {
			case ALL :
				name = "All";
				break;
			case CHOICE :
				name = "Choice";
				break;
			case GROUP :
				name = "Group";
				break;
			case SEQUENCE :
				name = "Sequence";
				break;
		}
		getSchema().fireModelObjectChanged(this, P_KIND);
	}
}
public void updateReferencesFor(ISchemaElement element) {
	for (int i = 0; i < children.size(); i++) {
		Object child = children.elementAt(i);
		if (child instanceof SchemaElementReference) {
			SchemaElementReference ref = (SchemaElementReference) child;
			if (ref.getReferencedElement() == element)
				ref.setReferenceName(element.getName());
		} else {
			SchemaCompositor compositor = (SchemaCompositor) child;
			compositor.updateReferencesFor(element);
		}
	}
}
public void write(String indent, PrintWriter writer) {
	String tag = null;

	switch (kind) {
		case this.ALL :
			tag = "all";
			break;
		case this.CHOICE :
			tag = "choice";
			break;
		case this.GROUP :
			tag = "group";
			break;
		case this.SEQUENCE :
			tag = "sequence";
			break;
	}
	if (tag == null)
		return;
	writer.print(indent+"<"+tag);
	if (getMinOccurs() != 1 && getMaxOccurs() != 1) {
		String min = "" + getMinOccurs();
		String max =
			getMaxOccurs() == Integer.MAX_VALUE ? "unbounded" : ("" + getMaxOccurs());
		writer.print(" minOccurs=\""+min+"\" maxOccurs=\""+max+"\"");
	}
	writer.println(">");
	String indent2= indent + Schema.INDENT;
	for (int i=0; i<children.size(); i++) {
		Object obj = children.elementAt(i);
		if (obj instanceof IWritable) {
			((IWritable)obj).write(indent2, writer);
		}
	}
	writer.println(indent+"</"+tag+">");
}
}
