package org.eclipse.pde.internal.schema;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.apache.xerces.parsers.*;
import org.eclipse.pde.internal.base.model.*;
import java.io.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.net.*;
import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.schema.*;
import org.eclipse.core.runtime.PlatformObject;

public class Schema extends PlatformObject implements ISchema {
	private URL url;
	private Vector listeners = new Vector();
	private Vector elements = new Vector();
	private Vector docSections = new Vector();
	private String internalId;
	private ISchemaDescriptor schemaDescriptor;
	private boolean loaded;
	private Vector references;
	private String description;
	private String name;
	private boolean notificationEnabled;
	public final static java.lang.String INDENT = "   ";
	private boolean disposed=false;

public Schema(String id, String name) {
	internalId = id;
	setName(name);
}
public Schema(ISchemaDescriptor schemaDescriptor, URL url) {
	this.schemaDescriptor = schemaDescriptor;
	this.url = url;
}
public void addDocumentSection(IDocumentSection docSection) {
	docSections.addElement(docSection);
	fireModelChanged(
		new ModelChangedEvent(
			ModelChangedEvent.INSERT,
			new Object[] { docSection },
			null));

}
public void addElement(ISchemaElement element) {
	elements.addElement(element);
	fireModelChanged(
		new ModelChangedEvent(
			ModelChangedEvent.INSERT,
			new Object[] { element },
			null));

}
public void addModelChangedListener(IModelChangedListener listener) {
	listeners.addElement(listener);
}
private void collectElements(ISchemaCompositor compositor, Vector result) {
	Object [] children = compositor.getChildren();
	for (int i=0; i<children.length; i++) {
		Object child = children[i];
		if (child instanceof ISchemaCompositor)
			collectElements((ISchemaCompositor) child, result);
		else
			if (child instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) child;
				Object referenced = ref.getReferencedObject();
				if (referenced instanceof ISchemaElement)
					result.addElement(referenced);
			}
	}
}
public void dispose() {
	reset();
	disposed = true;
}
public ISchemaElement findElement(String name) {
	if (!isLoaded()) load();
	for (int i = 0; i < elements.size(); i++) {
		ISchemaElement element = (ISchemaElement) elements.elementAt(i);
		if (element.getName().equals(name))
			return element;
	}
	return null;
}
public void fireModelChanged(IModelChangedEvent event) {
	if (!notificationEnabled) return;
	for (Iterator iter = listeners.iterator(); iter.hasNext();) {
		IModelChangedListener listener = (IModelChangedListener) iter.next();
		listener.modelChanged(event);
	}
}
public void fireModelObjectChanged(Object object, String property) {
	fireModelChanged(
		new ModelChangedEvent(
			ModelChangedEvent.CHANGE,
			new Object[] { object },
			property));
}
private String getAttribute(Node node, String name) {
	NamedNodeMap map = node.getAttributes();
	Node attNode = map.getNamedItem(name);
	if (attNode!=null) return attNode.getNodeValue();
	return null;
}
public ISchemaElement[] getCandidateChildren(ISchemaElement element) {
	Vector candidates = new Vector();

	ISchemaType type = element.getType();
	if (type instanceof ISchemaComplexType) {
		ISchemaCompositor compositor = ((ISchemaComplexType) type).getCompositor();
		if (compositor != null)
			collectElements(compositor, candidates);
	}
	ISchemaElement[] result = new ISchemaElement[candidates.size()];
	candidates.copyInto(result);
	return result;
}
public java.lang.String getDescription() {
	return description;
}
public IDocumentSection[] getDocumentSections() {
	IDocumentSection[] result = new IDocumentSection[docSections.size()];
	docSections.copyInto(result);
	return result;
}
public int getElementCount() {
	return elements.size();
}
public ISchemaElement[] getElements() {
	if (!isLoaded())
		load();
	ISchemaElement[] result = new ISchemaElement[elements.size()];
	elements.copyInto(result);
	return result;
}
public java.lang.String getName() {
	return name;
}
private String getNormalizedText(String source) {
	String result = source.replace('\t', ' ');
	result = result.trim();
	boolean skip=false;

	return result;
	/*
	
	StringBuffer buff = new StringBuffer();
	for (int i=0; i<result.length(); i++) {
		char c = result.charAt(i);
		if (c=='\n') {
			skip = true;
		}
		else if (c==' ') {
			if (skip) continue;
		}
		else skip = false;
		
		buff.append(c);
	}
	return buff.toString();
	*/
}
public ISchemaObject getParent() {
	return null;
}
public String getPointId() {
	//return schemaDescriptor!=null?schemaDescriptor.getPointId():internalId;
	return internalId;
}
public ISchema getSchema() {
	return this;
}
public ISchemaDescriptor getSchemaDescriptor() {
	return schemaDescriptor;
}
public URL getURL() {
	return url;
}
public boolean isDisposed() {
	return disposed;
}
public boolean isEditable() {
	return false;
}
public boolean isLoaded() {
	return loaded;
}
public boolean isNotificationEnabled() {
	return notificationEnabled;
}
public void load() {
	String urlName = getURL().toString();
	try {
		DOMParser p = new DOMParser();
		InputStream source = getURL().openStream();
		p.parse(new InputSource(source));
		Document doc = p.getDocument();
		Node root = (Node) doc.getDocumentElement();
		traverseDocumentTree(root);
	} catch (SAXException se) {
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
}
public void load(InputStream source) {
	try {
		DOMParser p = new DOMParser();
		p.parse(new InputSource(source));
		Document doc = p.getDocument();
		Node root = (Node) doc.getDocumentElement();
		traverseDocumentTree(root);
	} catch (SAXException se) {
	} catch (IOException e) {
		PDEPlugin.logException(e);
	}
}
private ISchemaAttribute processAttribute(
	ISchemaElement element,
	Node elementNode) {
	String aname = getAttribute(elementNode, "name");
	String atype = getAttribute(elementNode, "type");
	String ause = getAttribute(elementNode, "use");
	String avalue = getAttribute(elementNode, "value");

	ISchemaSimpleType type = null;

	if (atype != null) {
		type = (ISchemaSimpleType)resolveTypeReference(atype);
	}

	SchemaAttribute attribute = new SchemaAttribute(element, aname);
	if (ause != null) {
		int use = ISchemaAttribute.OPTIONAL;
		if (ause.equals("required"))
			use = ISchemaAttribute.REQUIRED;
		else
			if (ause.equals("optional"))
				use = ISchemaAttribute.OPTIONAL;
			else
				if (ause.equals("default"))
					use = ISchemaAttribute.DEFAULT;
		attribute.setUse(use);
	}
	if (avalue!=null) attribute.setValue(avalue);

	NodeList children = elementNode.getChildNodes();

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			String tag = child.getNodeName();
			if (tag.equals("annotation")) {
				processAttributeAnnotation(attribute, child);
			} else
				if (tag.equals("simpleType")) {
					processAttributeSimpleType(attribute, child);
				}
		}
	}
	if (type != null && attribute.getType() == null)
		attribute.setType(type);
	return attribute;
}
private void processAttributeAnnotation(SchemaAttribute element, Node node) {
	NodeList children = node.getChildNodes();

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("documentation")) {
				element.setDescription(getNormalizedText(child.getFirstChild().getNodeValue()));
			} else
				if (child.getNodeName().equals("appInfo")) {
					NodeList infos = child.getChildNodes();
					for (int j = 0; j < infos.getLength(); j++) {
						Node meta = infos.item(j);
						if (meta.getNodeType() == Node.ELEMENT_NODE) {
							if (meta.getNodeName().equals("meta.attribute")) {
								element.setKind(processKind(getAttribute(meta, "kind")));
								element.setBasedOn(getAttribute(meta, "basedOn"));
							}
						}
					}
				}
		} 
	}
}
private SchemaSimpleType processAttributeRestriction(SchemaAttribute attribute, Node node) {
	NodeList children = node.getChildNodes();
	if (children.getLength()==0) return null;

	String baseName = getAttribute(node, "base");

	if (baseName.equals("string")==false) return null;

	SchemaSimpleType type = new SchemaSimpleType(attribute.getSchema(), baseName);

	Vector items = new Vector();
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("enumeration")) {
				ISchemaEnumeration enum = processEnumeration(attribute.getSchema(), child);
				if (enum!=null) items.addElement(enum);
			}
		}
	}
	ChoiceRestriction restriction = new ChoiceRestriction(attribute.getSchema());
	restriction.setChildren(items);
	type.setRestriction(restriction);
	return type;
}
private void processAttributeSimpleType(SchemaAttribute attribute, Node node) {
	NodeList children = node.getChildNodes();
	if (children.getLength()==0) return;

	SchemaSimpleType type = null;   

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("restriction")) {
				type = processAttributeRestriction(attribute, child);
			}
		}
	}
	if (type!=null) attribute.setType(type);
}
private SchemaComplexType processComplexType(
	ISchemaElement owner,
	Node typeNode) {
	String aname = getAttribute(typeNode, "name");
	String amixed = getAttribute(typeNode, "mixed");

	SchemaComplexType complexType = new SchemaComplexType(this, aname);
	if (amixed != null && amixed.equals("true"))
		complexType.setMixed(true);

	NodeList children = typeNode.getChildNodes();
	ISchemaCompositor compositor = null;

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("attribute")) {
				complexType.addAttribute(processAttribute(owner, child));
			} else {
				ISchemaObject object = processCompositorChild(owner, child, -1);
				if (object instanceof ISchemaCompositor && compositor==null) {
					compositor = (ISchemaCompositor) object;
				}
			}
		}
	}

	complexType.setCompositor(compositor);
	return complexType;
}
private ISchemaCompositor processCompositor(ISchemaObject parent, Node node, int type) {
	SchemaCompositor compositor = new SchemaCompositor(parent, type);
	NodeList children = node.getChildNodes();

	int minOccurs = 1;
	int maxOccurs = 1;
	String aminOccurs = getAttribute(node, "minOccurs");
	String amaxOccurs = getAttribute(node, "maxOccurs");
	if (aminOccurs != null)
		minOccurs = Integer.valueOf(aminOccurs).intValue();
	if (amaxOccurs != null) {
		if (amaxOccurs.equals("unbounded"))
			maxOccurs = Integer.MAX_VALUE;
		else {
			maxOccurs = Integer.valueOf(amaxOccurs).intValue();
		}
	}
	compositor.setMinOccurs(minOccurs);
	compositor.setMaxOccurs(maxOccurs);

	for (int i=0; i<children.getLength(); i++) {
		Node child = children.item(i);
		ISchemaObject object = processCompositorChild(compositor, child, type);
		if (object != null)
		   compositor.addChild(object);
	}
	return compositor;
}
private ISchemaObject processCompositorChild(ISchemaObject parent, Node child, int parentKind) {
	String tag = child.getNodeName();

	if (tag.equals("element")) {
		return processElement(parent, child);
	}
	// sequence: element | group | choice | sequence    
	if (tag.equals("sequence") && parentKind != ISchemaCompositor.ALL) {
		return processCompositor(parent, child, ISchemaCompositor.SEQUENCE);
	}
	// choice: element | group | choice | sequence
	if (tag.equals("choice") && parentKind != ISchemaCompositor.ALL) {
		return processCompositor(parent, child, ISchemaCompositor.CHOICE);
	}
	// all: element
	if (tag.equals("all")
		&& (parentKind==0 || parentKind == ISchemaCompositor.GROUP)) {
		return processCompositor(parent, child, ISchemaCompositor.ALL);
	}
	// group: all | choice | sequence
	if (tag.equals("group")
		&& (parentKind == ISchemaCompositor.CHOICE
			|| parentKind == ISchemaCompositor.SEQUENCE)) {
		return processCompositor(parent, child, ISchemaCompositor.GROUP);
	}
	return null;
}
private ISchemaElement processElement(ISchemaObject parent, Node elementNode) {
	String aname = getAttribute(elementNode, "name");
	String atype = getAttribute(elementNode, "type");
	String aref = getAttribute(elementNode, "ref");
	int minOccurs = 1;
	int maxOccurs = 1;
	String aminOccurs = getAttribute(elementNode, "minOccurs");
	String amaxOccurs = getAttribute(elementNode, "maxOccurs");
	if (aminOccurs != null)
		minOccurs = Integer.valueOf(aminOccurs).intValue();
	if (amaxOccurs != null) {
		if (amaxOccurs.equals("unbounded"))
			maxOccurs = Integer.MAX_VALUE;
		else {
			maxOccurs = Integer.valueOf(amaxOccurs).intValue();
		}
	}

	if (aref != null) {
		// Reference!!
		SchemaElementReference reference = new SchemaElementReference((ISchemaCompositor)parent, aref);
		reference.setMinOccurs(minOccurs);
		reference.setMaxOccurs(maxOccurs);
		references.addElement(reference);
		return reference;
	}

	ISchemaType type = null;

	if (atype != null) {
		type = resolveTypeReference(atype);
	}

	SchemaElement element = new SchemaElement(parent, aname);
	element.setMinOccurs(minOccurs);
	element.setMaxOccurs(maxOccurs);

	NodeList children = elementNode.getChildNodes();

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			String tag = child.getNodeName();
			if (type == null && tag.equals("complexType")) {
				type = processComplexType(element, child);
			}
			/*
			if (tag.equals("attribute")) {
				processAttribute(element, child);
			}
			else */ if (tag.equals("annotation")) {
				processElementAnnotation(element, child);
			}
		}
	}
	element.setType(type);
	return element;
}
private void processElementAnnotation(SchemaElement element, Node node) {
	NodeList children = node.getChildNodes();

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("documentation")) {
				element.setDescription(getNormalizedText(child.getFirstChild().getNodeValue()));
			} else
				if (child.getNodeName().equals("appInfo")) {
					NodeList infos = child.getChildNodes();
					for (int j = 0; j < infos.getLength(); j++) {
						Node meta = infos.item(j);
						if (meta.getNodeType() == Node.ELEMENT_NODE) {
							if (meta.getNodeName().equals("meta.element")) {
								element.setLabelProperty(getAttribute(meta, "labelAttribute"));
								element.setIconName(getAttribute(meta, "icon"));
							}
						}
					}
				}
		} 
	}
}
private ISchemaEnumeration processEnumeration(ISchema schema, Node node) {
	String name = getAttribute(node, "value");
	SchemaEnumeration enum = new SchemaEnumeration(schema, name);
	return enum;
}
private int processKind(String name) {
	if (name != null) {
		if (name.equals("java"))
			return SchemaAttribute.JAVA;
		if (name.equals("resource"))
			return SchemaAttribute.RESOURCE;
	}
	return SchemaAttribute.STRING;
}
private void processSchemaAnnotation(Node node) {
	NodeList children = node.getChildNodes();
	String section = "overview";
	String sectionName = "Overview";
	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("documentation")) {
				String text = getNormalizedText(child.getFirstChild().getNodeValue());
				if (section != null) {
					if (section.equals("overview"))
						setDescription(text);
					else {
						DocumentSection sec = new DocumentSection(this, section, sectionName);
						sec.setDescription(text);
						docSections.addElement(sec);
					}
				}
			} else
				if (child.getNodeName().equals("appInfo")) {
					NodeList infos = child.getChildNodes();
					for (int j = 0; j < infos.getLength(); j++) {
						Node meta = infos.item(j);
						if (meta.getNodeType() == Node.ELEMENT_NODE) {
							if (meta.getNodeName().equals("meta.schema")) {
								section = "overview";
								setName(getAttribute(meta, "name"));
								internalId = getAttribute(meta, "plugin") + "." + getAttribute(meta, "id");
							} else
								if (meta.getNodeName().equals("meta.section")) {
									section = getAttribute(meta, "type");
									sectionName = getAttribute(meta, "name");
									if (sectionName == null)
										sectionName = section;
								}
						}

					}

				}
		}
	}
}
public void reload() {
	reset();
	load();
	fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
}
public void reload(InputStream is) {
	reset();
	load(is);
	fireModelChanged(
		new ModelChangedEvent(IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
}
public void  removeDocumentSection(IDocumentSection docSection) {
	docSections.removeElement(docSection);
	fireModelChanged(
		new ModelChangedEvent(
			ModelChangedEvent.REMOVE,
			new Object[] { docSection },
			null));

}
public void removeElement(ISchemaElement element) {
	elements.removeElement(element);
	fireModelChanged(
		new ModelChangedEvent(
			ModelChangedEvent.REMOVE,
			new Object[] { element },
			null));
}
public void removeModelChangedListener(IModelChangedListener listener) {
	listeners.removeElement(listener);
}
private void reset() {
	elements = new Vector();
	docSections = new Vector();
	internalId = null;
	Vector references = null;
	description = null;
	name = null;
}
private void resolveElementReference(ISchemaObjectReference reference) {
	for (int i = 0; i < elements.size(); i++) {
		ISchemaElement element = (ISchemaElement) elements.elementAt(i);
		if (!(element instanceof ISchemaObjectReference)
			&& element.getName().equals(reference.getName())) {
			// Link
			reference.setReferencedObject(element);
			break;
		}
	}
}
private void resolveReference(ISchemaObjectReference reference) {
	Class clazz = reference.getReferencedObjectClass();
	if (clazz.equals(ISchemaElement.class)) {
		resolveElementReference(reference);
	}
}
private void resolveReferences(Vector references) {
	for (int i=0; i<references.size(); i++) {
		ISchemaObjectReference reference = (ISchemaObjectReference)references.elementAt(i);
		resolveReference(reference);
	}
}
private SchemaType resolveTypeReference(String typeName) {
	// for now, create a simple type
	return new SchemaSimpleType(this, typeName);
}
public void setDescription(String newDescription) {
	description = newDescription;
	fireModelObjectChanged(this, P_DESCRIPTION);
}
public void setName(String newName) {
	name = newName;
	fireModelObjectChanged(this, P_NAME);
}
public void setNotificationEnabled(boolean newNotificationEnabled) {
	notificationEnabled = newNotificationEnabled;
}
public String toString() {
	return name;
}
public void traverseDocumentTree(Node root) {
	NodeList children = root.getChildNodes();

	references = new Vector();

	for (int i = 0; i < children.getLength(); i++) {
		Node child = children.item(i);
		if (child.getNodeType() == Node.ELEMENT_NODE) {
			if (child.getNodeName().equals("element")) {
				ISchemaElement element = processElement(this, child);
				elements.addElement(element);
			}
			else if (child.getNodeName().equals("annotation")) {
				processSchemaAnnotation(child);
			}
		}
	}
	if (references.size() > 0)
		resolveReferences(references);
	references = null;
	loaded=true;
}
public void updateReferencesFor(ISchemaElement element) {
	for (int i = 0; i < elements.size(); i++) {
		ISchemaElement el = (ISchemaElement) elements.elementAt(i);
		ISchemaType type = el.getType();
		if (type instanceof ISchemaComplexType) {
			SchemaCompositor compositor =
				(SchemaCompositor) ((ISchemaComplexType) type).getCompositor();
			if (compositor != null)
				compositor.updateReferencesFor(element);
		}
	}
}
public void write(String indent, PrintWriter writer) {
	String pointId = this.getPointId();
	int loc = pointId.lastIndexOf('.');
	String pluginId = "";
	if (loc!= -1) {
		pluginId = pointId.substring(0, loc);
		pointId = pointId.substring(loc+1);
	}
	writer.println("<?xml version='1.0' encoding='UTF-8'?>");
	writer.println("<!-- Schema file written by PDE -->");
	writer.println("<schema targetNamespace=\""+pluginId+"\">");
	writer.println("<annotation>");
	writer.println(INDENT+"<appInfo>");
	writer.print(INDENT+INDENT+"<meta.schema plugin=\""+pluginId+"\"");
	writer.print(" id=\""+pointId+"\"");
	writer.println(" name=\""+getName()+"\"/>");
	writer.println(INDENT+"</appInfo>");
	writer.println(INDENT+"<documentation>");
	writer.println(INDENT+INDENT+SchemaObject.getWritableDescription(getDescription()));
	writer.println(INDENT+"</documentation>");
	writer.println("</annotation>");
	writer.println();

	// add elements
	for (int i=0; i<elements.size(); i++) {
		ISchemaElement element = (ISchemaElement)elements.elementAt(i);
		element.write(INDENT, writer);
		writer.println();
	}
	// add document sections
	for (int i=0; i<docSections.size(); i++) {
		IDocumentSection section = (IDocumentSection)docSections.elementAt(i);
		section.write(INDENT, writer);
		writer.println();
	}
	writer.println("</schema>");
}
}
