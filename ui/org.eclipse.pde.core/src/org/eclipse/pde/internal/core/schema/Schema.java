/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.schema;

import java.io.*;
import java.net.URL;
import java.util.*;

import javax.xml.parsers.*;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.w3c.dom.*;

public class Schema extends PlatformObject implements ISchema {
	private URL url;
	private Vector listeners = new Vector();
	private Vector elements = new Vector();
	private Vector docSections = new Vector();
	private Vector includes;
	private String pointId;
	private String pluginId;
	private ISchemaDescriptor schemaDescriptor;
	private boolean loaded;
	private Vector references;
	private String description;
	private String name = "";
	private boolean notificationEnabled;
	public final static java.lang.String INDENT = "   ";
	private boolean disposed = false;
	private Hashtable lineTable;
	private boolean valid;
	private int startLine, endLine;
	
	public Schema(String pluginId, String pointId, String name) {
		this.pluginId = pluginId;
		this.pointId = pointId;
		this.name = name;
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
		addElement(element, null);
	}

	public void addElement(
		ISchemaElement element,
		ISchemaElement afterElement) {
		int index = -1;
		if (afterElement != null) {
			index = elements.indexOf(afterElement);
		}
		if (index != -1)
			elements.add(index + 1, element);
		else
			elements.add(element);
		fireModelChanged(
			new ModelChangedEvent(
				ModelChangedEvent.INSERT,
				new Object[] { element },
				null));
	}

	public void addInclude(ISchemaInclude include) {
		if (includes == null)
			includes = new Vector();
		includes.add(include);
		fireModelChanged(
			new ModelChangedEvent(
				ModelChangedEvent.INSERT,
				new Object[] { include },
				null));
	}

	public void removeInclude(ISchemaInclude include) {
		if (includes == null)
			return;
		includes.remove(include);
		fireModelChanged(
			new ModelChangedEvent(
				ModelChangedEvent.REMOVE,
				new Object[] { include },
				null));
	}

	public void addModelChangedListener(IModelChangedListener listener) {
		listeners.addElement(listener);
	}
	private void collectElements(ISchemaCompositor compositor, Vector result) {
		Object[] children = compositor.getChildren();
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof ISchemaCompositor)
				collectElements((ISchemaCompositor) child, result);
			else if (child instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) child;
				Object referenced = ref.getReferencedObject();
				if (referenced instanceof ISchemaElement)
					result.addElement(referenced);
			}
		}
	}
	public void dispose() {
		if (includes != null) {
			for (int i = 0; i < includes.size(); i++) {
				ISchemaInclude include = (ISchemaInclude) includes.get(i);
				include.dispose();
			}
		}
		reset();
		disposed = true;
	}

	public ISchemaElement findElement(String name) {
		if (!isLoaded())
			load();
		
		for (int i = 0; i < elements.size(); i++) {
			ISchemaElement element = (ISchemaElement) elements.elementAt(i);
			if (element.getName().equals(name))
				return element;
		}
		if (includes!=null) {
			for (int i=0; i<includes.size(); i++) {
				ISchemaInclude include = (ISchemaInclude)includes.get(i);
				ISchema ischema = include.getIncludedSchema();
				if (ischema==null) continue;
				ISchemaElement element = ischema.findElement(name);
				if (element!=null) return element;
			}
		}
		return null;
	}
	public void fireModelChanged(IModelChangedEvent event) {
		if (!notificationEnabled)
			return;
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IModelChangedListener listener =
				(IModelChangedListener) iter.next();
			listener.modelChanged(event);
		}
	}
	public void fireModelObjectChanged(
		Object object,
		String property,
		Object oldValue,
		Object newValue) {
		fireModelChanged(
			new ModelChangedEvent(object, property, oldValue, newValue));
	}
	private String getAttribute(Node node, String name) {
		NamedNodeMap map = node.getAttributes();
		Node attNode = map.getNamedItem(name);
		if (attNode != null) {
			String value = attNode.getNodeValue();
			if (value.length() > 0)
				return value;
		}
		return null;
	}
	public ISchemaElement[] getCandidateChildren(ISchemaElement element) {
		Vector candidates = new Vector();

		ISchemaType type = element.getType();
		if (type instanceof ISchemaComplexType) {
			ISchemaCompositor compositor =
				((ISchemaComplexType) type).getCompositor();
			if (compositor != null)
				collectElements(compositor, candidates);
		}
		ISchemaElement[] result = new ISchemaElement[candidates.size()];
		candidates.copyInto(result);
		return result;
	}
	public String getDescription() {
		return description;
	}
	public boolean isValid() {
		return valid;
	}
	public IDocumentSection[] getDocumentSections() {
		IDocumentSection[] result = new IDocumentSection[docSections.size()];
		docSections.copyInto(result);
		return result;
	}
	public int getElementCount() {
		return elements.size();
	}
	public int getResolvedElementCount() {
		int localCount = getElementCount();
		if (includes == null)
			return localCount;
		int totalCount = localCount;
		for (int i = 0; i < includes.size(); i++) {
			ISchemaInclude include = (ISchemaInclude) includes.get(i);
			ISchema schema = include.getIncludedSchema();
			if (schema == null)
				continue;
			totalCount += schema.getResolvedElementCount();
		}
		return totalCount;
	}
	public ISchemaElement[] getElements() {
		if (!isLoaded())
			load();
		ISchemaElement[] result = new ISchemaElement[elements.size()];
		elements.copyInto(result);
		return result;
	}
	public ISchemaElement[] getResolvedElements() {
		if (includes == null)
			return getElements();
		if (!isLoaded())
			load();
		Vector result = (Vector) elements.clone();
		for (int i = 0; i < includes.size(); i++) {
			ISchemaInclude include = (ISchemaInclude) includes.get(i);
			ISchema schema = include.getIncludedSchema();
			if (schema == null)
				continue;
			ISchemaElement[] ielements = schema.getElements();
			for (int j = 0; j < ielements.length; j++)
				result.add(ielements[j]);
		}
		return (ISchemaElement[]) result.toArray(
			new ISchemaElement[result.size()]);
	}

	public ISchemaInclude[] getIncludes() {
		if (includes == null)
			return new ISchemaInclude[0];
		return (ISchemaInclude[]) includes.toArray(
			new ISchemaInclude[includes.size()]);
	}
	public String getName() {
		return name;
	}
	private String getNormalizedText(String source) {
		String result = source.replace('\t', ' ');
		result = result.trim();

		return result;
		/*
		boolean skip=false;
		
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

	public void setParent(ISchemaObject obj) {
	}

	public String getQualifiedPointId() {
		//return schemaDescriptor!=null?schemaDescriptor.getPointId():internalId;
		return pluginId + "." + pointId;
	}

	public String getPluginId() {
		return pluginId;
	}

	public String getPointId() {
		return pointId;
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
		try {
			InputStream source = getURL().openStream();
			load(source);
			source.close();
		} catch (FileNotFoundException e) {
			loaded = false;
		} catch (IOException e) {
			PDECore.logException(e);
		}
	}

	public void load(InputStream stream) {
		try {
			SAXParser parser = AbstractPluginModelBase.getSaxParser();
			XMLDefaultHandler handler = new XMLDefaultHandler();			
			parser.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
			parser.parse(stream, handler);
			traverseDocumentTree(handler.getDocumentElement(), handler.getLineTable());
		} catch (Exception e) {
			PDECore.logException(e);
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
			type = (ISchemaSimpleType) resolveTypeReference(atype);
		}

		SchemaAttribute attribute = new SchemaAttribute(element, aname);
		attribute.bindSourceLocation(elementNode, lineTable);
		attribute.addComments(elementNode);
		if (ause != null) {
			int use = ISchemaAttribute.OPTIONAL;
			if (ause.equals("required"))
				use = ISchemaAttribute.REQUIRED;
			else if (ause.equals("optional"))
				use = ISchemaAttribute.OPTIONAL;
			else if (ause.equals("default"))
				use = ISchemaAttribute.DEFAULT;
			attribute.setUse(use);
		}
		if (avalue != null)
			attribute.setValue(avalue);

		NodeList children = elementNode.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String tag = child.getNodeName();
				if (tag.equals("annotation")) {
					processAttributeAnnotation(attribute, child);
				} else if (tag.equals("simpleType")) {
					processAttributeSimpleType(attribute, child);
				}
			}
		}
		if (type != null && attribute.getType() == null)
			attribute.setType(type);
		return attribute;
	}
	private void processAttributeAnnotation(
		SchemaAttribute element,
		Node node) {
		NodeList children = node.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("documentation")) {
					element.setDescription(
						getNormalizedText(
							child.getFirstChild().getNodeValue()));
				} else if (child.getNodeName().equals("appInfo")) {
					NodeList infos = child.getChildNodes();
					for (int j = 0; j < infos.getLength(); j++) {
						Node meta = infos.item(j);
						if (meta.getNodeType() == Node.ELEMENT_NODE) {
							if (meta.getNodeName().equals("meta.attribute")) {
								element.setKind(
									processKind(getAttribute(meta, "kind")));
								element.setBasedOn(
									getAttribute(meta, "basedOn"));
							}
						}
					}
				}
			}
		}
	}
	private SchemaSimpleType processAttributeRestriction(
		SchemaAttribute attribute,
		Node node) {
		NodeList children = node.getChildNodes();
		if (children.getLength() == 0)
			return null;

		String baseName = getAttribute(node, "base");

		if (baseName.equals("string") == false) {
			return new SchemaSimpleType(attribute.getSchema(), "string");
		}
		SchemaSimpleType type =
			new SchemaSimpleType(attribute.getSchema(), baseName);

		Vector items = new Vector();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("enumeration")) {
					ISchemaEnumeration enum =
						processEnumeration(attribute.getSchema(), child);
					if (enum != null)
						items.addElement(enum);
				}
			}
		}
		ChoiceRestriction restriction =
			new ChoiceRestriction(attribute.getSchema());
		restriction.setChildren(items);
		type.setRestriction(restriction);
		return type;
	}
	private void processAttributeSimpleType(
		SchemaAttribute attribute,
		Node node) {
		NodeList children = node.getChildNodes();
		if (children.getLength() == 0)
			return;

		SchemaSimpleType type = null;

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("restriction")) {
					type = processAttributeRestriction(attribute, child);
				}
			}
		}
		if (type != null)
			attribute.setType(type);
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
					ISchemaObject object =
						processCompositorChild(owner, child, ISchemaCompositor.ROOT);
					if (object instanceof ISchemaCompositor
						&& compositor == null) {
						compositor = (ISchemaCompositor) object;
					}
				}
			}
		}

		complexType.setCompositor(compositor);
		return complexType;
	}
	private ISchemaCompositor processCompositor(
		ISchemaObject parent,
		Node node,
		int type) {
		SchemaCompositor compositor = new SchemaCompositor(parent, type);
		compositor.addComments(node);
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

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			ISchemaObject object =
				processCompositorChild(compositor, child, type);
			if (object != null)
				compositor.addChild(object);
		}
		return compositor;
	}
	private ISchemaObject processCompositorChild(
		ISchemaObject parent,
		Node child,
		int parentKind) {
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
			&& (parentKind == ISchemaCompositor.ROOT || parentKind == ISchemaCompositor.GROUP)) {
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
	private ISchemaElement processElement(
		ISchemaObject parent,
		Node elementNode) {
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
			SchemaElementReference reference =
				new SchemaElementReference((ISchemaCompositor) parent, aref);
			reference.addComments(elementNode);
			reference.setMinOccurs(minOccurs);
			reference.setMaxOccurs(maxOccurs);
			references.addElement(reference);
			reference.bindSourceLocation(elementNode, lineTable);
			return reference;
		}

		ISchemaType type = null;

		if (atype != null) {
			type = resolveTypeReference(atype);
		}

		SchemaElement element = new SchemaElement(parent, aname);
		element.bindSourceLocation(elementNode, lineTable);
		element.addComments(elementNode);
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
				else */
				if (tag.equals("annotation")) {
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
					element.setDescription(
						getNormalizedText(
							child.getFirstChild().getNodeValue()));
				} else if (child.getNodeName().equals("appInfo")) {
					NodeList infos = child.getChildNodes();
					for (int j = 0; j < infos.getLength(); j++) {
						Node meta = infos.item(j);
						if (meta.getNodeType() == Node.ELEMENT_NODE) {
							if (meta.getNodeName().equals("meta.element")) {
								element.setLabelProperty(
									getAttribute(meta, "labelAttribute"));
								element.setIconProperty(
									getAttribute(meta, "icon"));
								if (element.getIconProperty() == null)
									element.setIconProperty(
										getAttribute(meta, "iconName"));
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
		enum.bindSourceLocation(node, lineTable);
		enum.addComments(node);
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
	
	void setSourceLocation(Node node){
			if (lineTable==null) return;
			Integer [] lines = (Integer[]) lineTable.get(node);
			if (lines != null) {
				startLine = lines[0].intValue();
				endLine = lines[1].intValue();
			} else {
				startLine = -1;
				endLine = -1;
			}
	}
	
	public int getOverviewStartLine(){
		return startLine;
	}
	
	public int getOverviewEndLine(){
		return endLine;
	}
	
	private void processSchemaAnnotation(Node node) {
		NodeList children = node.getChildNodes();
		String section = "overview";
		String sectionName = "Overview";
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("documentation")) {
					String text =
						getNormalizedText(child.getFirstChild().getNodeValue());
					if (section != null) {
						if (section.equals("overview")){
							setDescription(text);
							setSourceLocation(child);
						} else {
							DocumentSection sec =
								new DocumentSection(this, section, sectionName);
							sec.bindSourceLocation(child, lineTable);
							sec.setDescription(text);
							docSections.addElement(sec);
						}
					}
				} else if (child.getNodeName().equals("appInfo")) {
					NodeList infos = child.getChildNodes();
					for (int j = 0; j < infos.getLength(); j++) {
						Node meta = infos.item(j);
						if (meta.getNodeType() == Node.ELEMENT_NODE) {
							if (meta.getNodeName().equals("meta.schema")) {
								section = "overview";
								setName(getAttribute(meta, "name"));
								pluginId = getAttribute(meta, "plugin");
								pointId = getAttribute(meta, "id");
								valid = true;
							} else if (
								meta.getNodeName().equals("meta.section")) {
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

	private void processInclude(Node node) {
		String location = getAttribute(node, "schemaLocation");
		SchemaInclude include = new SchemaInclude(this, location);
		if (includes == null)
			includes = new Vector();
		includes.add(include);
	}

	public void reload() {
		reload(null);
	}
	public void reload(InputStream is) {
		setNotificationEnabled(false);
		reset();
		if (is != null)
			load(is);
		else
			load();
		setNotificationEnabled(true);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[0],
				null));
	}
	public void removeDocumentSection(IDocumentSection docSection) {
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
		lineTable = null;
		elements = new Vector();
		docSections = new Vector();
		includes = null;
		pointId = null;
		pluginId = null;
		references = null;
		description = null;
		name = null;
		valid = false;
	}
	private void resolveElementReference(ISchemaObjectReference reference) {
		ISchemaElement[] elementList = getResolvedElements();
		for (int i = 0; i < elementList.length; i++) {
			ISchemaElement element = elementList[i];
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
		for (int i = 0; i < references.size(); i++) {
			ISchemaObjectReference reference =
				(ISchemaObjectReference) references.elementAt(i);
			resolveReference(reference);
		}
	}
	private SchemaType resolveTypeReference(String typeName) {
		// for now, create a simple type
		return new SchemaSimpleType(this, typeName);
	}
	public void setDescription(String newDescription) {
		String oldValue = description;
		description = newDescription;
		fireModelObjectChanged(this, P_DESCRIPTION, oldValue, description);
	}
	public void setName(String newName) {
		if (newName == null)
			newName = "";
		String oldValue = name;
		name = newName;
		fireModelObjectChanged(this, P_NAME, oldValue, name);
	}
	public void setPluginId(String newId) {
		String oldValue = pluginId;
		pluginId = newId;
		fireModelObjectChanged(this, P_PLUGIN, oldValue, newId);
	}
	public void setPointId(String newId) {
		String oldValue = pointId;
		pointId = newId;
		fireModelObjectChanged(this, P_POINT, oldValue, newId);
	}
	public void setNotificationEnabled(boolean newNotificationEnabled) {
		notificationEnabled = newNotificationEnabled;
	}
	public String toString() {
		return name;
	}
	public void traverseDocumentTree(Node root, Hashtable lineTable) {
		this.lineTable = lineTable;
		NodeList children = root.getChildNodes();

		references = new Vector();

		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = child.getNodeName().toLowerCase();
				if (nodeName.equals("element")) {
					ISchemaElement element = processElement(this, child);
					elements.addElement(element);
				} else if (nodeName.equals("annotation")) {
					processSchemaAnnotation(child);
				} else if (nodeName.equals("include")) {
					processInclude(child);
				}
			}
		}
		loaded = true;
		if (references.size() > 0)
			resolveReferences(references);
		references = null;
		this.lineTable = null;
	}

	public void updateReferencesFor(ISchemaElement element) {
		updateReferencesFor(element, ISchema.REFRESH_RENAME);
	}

	public void updateReferencesFor(ISchemaElement element, int kind) {
		for (int i = 0; i < elements.size(); i++) {
			ISchemaElement el = (ISchemaElement) elements.elementAt(i);
			if (el.equals(element))
				continue;
			ISchemaType type = el.getType();
			if (type instanceof ISchemaComplexType) {
				SchemaCompositor compositor =
					(SchemaCompositor) ((ISchemaComplexType) type)
						.getCompositor();
				if (compositor != null)
					compositor.updateReferencesFor(element, kind);
			}
		}
	}

	public void write(String indent, PrintWriter writer) {
		String pointId = this.getQualifiedPointId();
		int loc = pointId.lastIndexOf('.');
		String pluginId = "";
		if (loc != -1) {
			pluginId = pointId.substring(0, loc);
			pointId = pointId.substring(loc + 1);
		}
		writer.println("<?xml version='1.0' encoding='UTF-8'?>");
		writer.println("<!-- Schema file written by PDE -->");
		writer.println("<schema targetNamespace=\"" + pluginId + "\">");
		String indent2 = INDENT + INDENT;
		String indent3 = indent2 + INDENT;
		writer.println(indent + "<annotation>");
		writer.println(indent2 + "<appInfo>");
		writer.print(indent3 + "<meta.schema plugin=\"" + pluginId + "\"");
		writer.print(" id=\"" + pointId + "\"");
		writer.println(" name=\"" + getName() + "\"/>");
		writer.println(indent2 + "</appInfo>");
		writer.println(indent2 + "<documentation>");
		writer.println(
			indent3 + SchemaObject.getWritableDescription(getDescription()));
		writer.println(indent2 + "</documentation>");
		writer.println(INDENT + "</annotation>");
		writer.println();

		// add includes, if defined
		if (includes != null) {
			for (int i = 0; i < includes.size(); i++) {
				ISchemaInclude include = (ISchemaInclude) includes.get(i);
				include.write(INDENT, writer);
				writer.println();
			}
		}

		// add elements
		for (int i = 0; i < elements.size(); i++) {
			ISchemaElement element = (ISchemaElement) elements.elementAt(i);
			element.write(INDENT, writer);
			writer.println();
		}
		// add document sections
		for (int i = 0; i < docSections.size(); i++) {
			IDocumentSection section =
				(IDocumentSection) docSections.elementAt(i);
			section.write(INDENT, writer);
			writer.println();
		}
		writer.println("</schema>");
	}
}
