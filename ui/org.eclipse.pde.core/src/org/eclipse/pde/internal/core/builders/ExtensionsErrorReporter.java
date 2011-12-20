/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.util.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class ExtensionsErrorReporter extends ManifestErrorReporter {

	private IPluginModelBase fModel;
	private IBuild fBuildModel;

	public ExtensionsErrorReporter(IFile file) {
		super(file);
		fModel = PluginRegistry.findModel(file.getProject());
		try {
			if (fModel != null && fModel.getUnderlyingResource() != null)
				fBuildModel = ClasspathUtilCore.getBuild(fModel);
		} catch (CoreException e) {
		}
	}

	/**
	 * @throws SAXException  
	 */
	public void characters(char[] characters, int start, int length) throws SAXException {
	}

	public void validateContent(IProgressMonitor monitor) {
		Element element = getDocumentRoot();
		if (element == null)
			return;
		String elementName = element.getNodeName();
		if (!"plugin".equals(elementName) && !"fragment".equals(elementName)) { //$NON-NLS-1$ //$NON-NLS-2$
			reportIllegalElement(element, CompilerFlags.ERROR);
		} else {
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
			if (severity != CompilerFlags.IGNORE) {
				NamedNodeMap attrs = element.getAttributes();
				for (int i = 0; i < attrs.getLength(); i++) {
					reportUnusedAttribute(element, attrs.item(i).getNodeName(), severity);
				}
			}

			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				if (monitor.isCanceled())
					break;
				Element child = (Element) children.item(i);
				String name = child.getNodeName();
				if (name.equals("extension")) { //$NON-NLS-1$
					validateExtension(child);
				} else if (name.equals("extension-point")) { //$NON-NLS-1$
					validateExtensionPoint(child);
				} else {
					if (!name.equals("runtime") && !name.equals("requires")) { //$NON-NLS-1$ //$NON-NLS-2$
						severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
						if (severity != CompilerFlags.IGNORE)
							reportIllegalElement(child, severity);
					} else {
						severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
						if (severity != CompilerFlags.IGNORE)
							reportUnusedElement(child, severity);
					}
				}
			}

			IExtensions extensions = fModel.getExtensions();
			if (extensions != null && extensions.getExtensions().length == 0 && extensions.getExtensionPoints().length == 0)
				report(PDECoreMessages.Builders_Manifest_useless_file, -1, IMarker.SEVERITY_WARNING, PDEMarkerFactory.P_USELESS_FILE, PDEMarkerFactory.CAT_OTHER);
		}
	}

	protected void validateExtension(Element element) {
		if (!assertAttributeDefined(element, "point", CompilerFlags.ERROR)) //$NON-NLS-1$
			return;
		String pointID = element.getAttribute("point"); //$NON-NLS-1$
		if (!PDECore.getDefault().getExtensionsRegistry().hasExtensionPoint(pointID)) {
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNRESOLVED_EX_POINTS);
			if (severity != CompilerFlags.IGNORE) {
				report(NLS.bind(PDECoreMessages.Builders_Manifest_ex_point, pointID), getLine(element, "point"), severity, PDEMarkerFactory.CAT_OTHER); //$NON-NLS-1$
			}
		} else {
			SchemaRegistry reg = PDECore.getDefault().getSchemaRegistry();
			ISchema schema = reg.getSchema(pointID);
			if (schema != null) {
				validateElement(element, schema, true);
			}
		}
	}

	/**
	 * @param parentElement
	 * @param childElement
	 * @param severity
	 */
	private void reportMaxOccurenceViolation(ElementOccurrenceResult result, int severity) {
		Element childElement = result.getElement();
		String allowedOccurrences = new Integer(result.getAllowedOccurrences()).toString();
		String message = NLS.bind(PDECoreMessages.ExtensionsErrorReporter_maxOccurrence, new String[] {allowedOccurrences, childElement.getNodeName()});
		report(message, getLine(childElement), severity, PDEMarkerFactory.P_ILLEGAL_XML_NODE, childElement, null, PDEMarkerFactory.CAT_FATAL);
	}

	/**
	 * @param parentElement
	 * @param childElement
	 * @param severity
	 */
	private void reportMinOccurenceViolation(Element parentElement, ElementOccurrenceResult result, int severity) {

		ISchemaElement childElement = result.getSchemaElement();
		String allowedOccurrences = new Integer(result.getAllowedOccurrences()).toString();
		String message = NLS.bind(PDECoreMessages.ExtensionsErrorReporter_minOccurrence, new String[] {allowedOccurrences, childElement.getName()});
		report(message, getLine(parentElement), severity, PDEMarkerFactory.CAT_FATAL);
	}

	protected void validateElement(Element element, ISchema schema, boolean isTopLevel) {
		String elementName = element.getNodeName();
		ISchemaElement schemaElement = schema.findElement(elementName);

		// Validate element occurrence violations
		if ((schemaElement != null) && (schemaElement.getType() instanceof ISchemaComplexType)) {
			validateMaxElementMult(element, schemaElement);
			validateMinElementMult(element, schemaElement);
		}

		ISchemaElement parentSchema = null;
		if (!"extension".equals(elementName)) { //$NON-NLS-1$
			Node parent = element.getParentNode();
			parentSchema = schema.findElement(parent.getNodeName());
		} else if (isTopLevel == false) {
			// This is an "extension" element; but, not a top level one.  
			// It is nested within another "extension" element somewhere
			// e.g. "extension" element is a child element of another element
			// that is not a "plugin" elment 
			// element
			// Report illegal element
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
			reportIllegalElement(element, severity);
			return;
		}

		if (parentSchema != null) {
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
			if (severity != CompilerFlags.IGNORE) {
				HashSet allowedElements = new HashSet();
				computeAllowedElements(parentSchema.getType(), allowedElements);
				if (!allowedElements.contains(elementName)) {
					reportIllegalElement(element, severity);
					return;
				}
			}

		}
		if (schemaElement == null && parentSchema != null) {
			ISchemaAttribute attr = parentSchema.getAttribute(elementName);
			if (attr != null && attr.getKind() == IMetaAttribute.JAVA) {
				if (attr.isDeprecated())
					reportDeprecatedAttribute(element, element.getAttributeNode("class")); //$NON-NLS-1$
				validateJavaAttribute(element, element.getAttributeNode("class")); //$NON-NLS-1$				
			}
		} else {
			if (schemaElement != null) {
				validateRequiredExtensionAttributes(element, schemaElement);
				validateExistingExtensionAttributes(element, element.getAttributes(), schemaElement);
				validateInternalExtensionAttribute(element, schemaElement);
				if (schemaElement.isDeprecated()) {
					if (schemaElement instanceof ISchemaRootElement)
						reportDeprecatedRootElement(element, ((ISchemaRootElement) schemaElement).getDeprecatedSuggestion());
					else
						reportDeprecatedElement(element);
				}
				if (schemaElement.hasTranslatableContent())
					validateTranslatableElementContent(element);
				// Bug 213457 - look up elements based on the schema in which the parent is found
				schema = schemaElement.getSchema();
			}
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				validateElement((Element) children.item(i), schema, false);
			}
		}
	}

	private void validateInternalExtensionAttribute(Element element, ISchemaElement schemaElement) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_INTERNAL);
		if (severity == CompilerFlags.IGNORE)
			return;

		if (schemaElement instanceof ISchemaRootElement) {
			ISchemaRootElement rootElement = (ISchemaRootElement) schemaElement;
			String epid = schemaElement.getSchema().getPluginId();
			String pid = fModel.getPluginBase().getId();
			if (epid == null || pid == null)
				return;
			if (rootElement.isInternal() && !epid.equals(pid)) {
				String point = element.getAttribute("point"); //$NON-NLS-1$
				if (point == null)
					return; // should never come to this...
				report(NLS.bind(PDECoreMessages.Builders_Manifest_internal_rootElement, point), getLine(element, "point"), severity, PDEMarkerFactory.CAT_DEPRECATION); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @param element
	 * @param schemaElement
	 */
	private void validateMinElementMult(Element element, ISchemaElement schemaElement) {
		// Validate min element occurence violations
		int minSeverity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
		if (minSeverity != CompilerFlags.IGNORE) {
			HashSet minElementSet = ElementOccurenceChecker.findMinOccurenceViolations(schemaElement, element);
			Iterator minIterator = minElementSet.iterator();

			while (minIterator.hasNext()) {
				reportMinOccurenceViolation(element, (ElementOccurrenceResult) minIterator.next(), minSeverity);
			}
		}
	}

	/**
	 * @param element
	 * @param schemaElement
	 */
	private void validateMaxElementMult(Element element, ISchemaElement schemaElement) {
		// Validate max element occurence violations
		int maxSeverity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
		if (maxSeverity != CompilerFlags.IGNORE) {
			HashSet maxElementSet = ElementOccurenceChecker.findMaxOccurenceViolations(schemaElement, element);
			Iterator maxIterator = maxElementSet.iterator();
			while (maxIterator.hasNext()) {
				reportMaxOccurenceViolation((ElementOccurrenceResult) maxIterator.next(), maxSeverity);
			}
		}
	}

	private void computeAllowedElements(ISchemaType type, HashSet elementSet) {
		if (type instanceof ISchemaComplexType) {
			ISchemaComplexType complexType = (ISchemaComplexType) type;
			ISchemaCompositor compositor = complexType.getCompositor();
			if (compositor != null)
				computeAllowedElements(compositor, elementSet);

			ISchemaAttribute[] attrs = complexType.getAttributes();
			for (int i = 0; i < attrs.length; i++) {
				if (attrs[i].getKind() == IMetaAttribute.JAVA)
					elementSet.add(attrs[i].getName());
			}
		}
	}

	private void computeAllowedElements(ISchemaCompositor compositor, HashSet elementSet) {
		ISchemaObject[] children = compositor.getChildren();
		for (int i = 0; i < children.length; i++) {
			ISchemaObject child = children[i];
			if (child instanceof ISchemaObjectReference) {
				ISchemaObjectReference ref = (ISchemaObjectReference) child;
				ISchemaElement refElement = (ISchemaElement) ref.getReferencedObject();
				if (refElement != null)
					elementSet.add(refElement.getName());
			} else if (child instanceof ISchemaCompositor) {
				computeAllowedElements((ISchemaCompositor) child, elementSet);
			}
		}
	}

	private void validateRequiredExtensionAttributes(Element element, ISchemaElement schemaElement) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_NO_REQUIRED_ATT);
		if (severity == CompilerFlags.IGNORE)
			return;

		ISchemaAttribute[] attInfos = schemaElement.getAttributes();
		for (int i = 0; i < attInfos.length; i++) {
			ISchemaAttribute attInfo = attInfos[i];
			if (attInfo.getUse() == ISchemaAttribute.REQUIRED) {
				boolean found = element.getAttributeNode(attInfo.getName()) != null;
				if (!found && attInfo.getKind() == IMetaAttribute.JAVA) {
					NodeList children = element.getChildNodes();
					for (int j = 0; j < children.getLength(); j++) {
						if (attInfo.getName().equals(children.item(j).getNodeName())) {
							found = true;
							break;
						}
					}
				}
				if (!found) {
					reportMissingRequiredAttribute(element, attInfo.getName(), severity);
				}
			}
		}
	}

	private void validateExistingExtensionAttributes(Element element, NamedNodeMap attrs, ISchemaElement schemaElement) {
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr) attrs.item(i);
			ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
			if (attInfo == null) {
				HashSet allowedElements = new HashSet();
				computeAllowedElements(schemaElement.getType(), allowedElements);
				if (allowedElements.contains(attr.getName())) {
					validateJavaAttribute(element, attr);
				} else {
					int flag = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ATTRIBUTE);
					if (flag != CompilerFlags.IGNORE)
						reportUnknownAttribute(element, attr.getName(), flag);
				}
			} else {
				validateExtensionAttribute(element, attr, attInfo);
			}
		}
	}

	private void validateExtensionAttribute(Element element, Attr attr, ISchemaAttribute attInfo) {
		ISchemaSimpleType type = attInfo.getType();

		int kind = attInfo.getKind();
		if (kind == IMetaAttribute.JAVA) {
			validateJavaAttribute(element, attr);
		} else if (kind == IMetaAttribute.RESOURCE) {
			validateResourceAttribute(element, attr);
		} else if (kind == IMetaAttribute.IDENTIFIER) {
			validateIdentifierAttribute(element, attr, attInfo);
		} else if (kind == IMetaAttribute.STRING) {
			ISchemaRestriction restriction = type.getRestriction();
			if (restriction != null) {
				validateRestrictionAttribute(element, attr, restriction);
			}
		} else if (type.getName().equals("boolean")) { //$NON-NLS-1$
			validateBoolean(element, attr);
		}

		validateTranslatableString(element, attr, attInfo.isTranslatable());

		if (attInfo.isDeprecated()) {
			reportDeprecatedAttribute(element, attr);
		}
	}

	protected void validateExtensionPoint(Element element) {
		if (assertAttributeDefined(element, "id", CompilerFlags.ERROR)) { //$NON-NLS-1$
			Attr idAttr = element.getAttributeNode("id"); //$NON-NLS-1$
			double schemaVersion = getSchemaVersion();
			String message = null;
			if (schemaVersion < 3.2 && !IdUtil.isValidSimpleID(idAttr.getValue()))
				message = NLS.bind(PDECoreMessages.Builders_Manifest_simpleID, idAttr.getValue());
			else if (schemaVersion >= 3.2) {
				if (!IdUtil.isValidCompositeID(idAttr.getValue())) {
					message = NLS.bind(PDECoreMessages.Builders_Manifest_compositeID, idAttr.getValue());
				}
			}

			if (message != null)
				report(message, getLine(element, idAttr.getName()), CompilerFlags.WARNING, PDEMarkerFactory.CAT_OTHER);
		}

		assertAttributeDefined(element, "name", CompilerFlags.ERROR); //$NON-NLS-1$

		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ATTRIBUTE);
		NamedNodeMap attrs = element.getAttributes();
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr attr = (Attr) attrs.item(i);
			String name = attr.getName();
			if ("name".equals(name)) { //$NON-NLS-1$
				validateTranslatableString(element, attr, true);
			} else if (!"id".equals(name) && !"schema".equals(name) && severity != CompilerFlags.IGNORE) { //$NON-NLS-1$ //$NON-NLS-2$
				reportUnknownAttribute(element, name, severity);
			}
		}

		severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
		if (severity != CompilerFlags.IGNORE) {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++)
				reportIllegalElement((Element) children.item(i), severity);
		}

		// Validate the "schema" attribute of the extension point
		Attr attr = element.getAttributeNode(IPluginExtensionPoint.P_SCHEMA);
		// Only validate the attribute if it was defined
		if (attr != null) {
			String schemaValue = attr.getValue();
			IResource res = getFile().getProject().findMember(schemaValue);
			String errorMessage = null;
			// Check to see if the value specified is an extension point schema and it exists
			if (!(res instanceof IFile && (res.getName().endsWith(".exsd") || //$NON-NLS-1$
			res.getName().endsWith(".mxsd")))) //$NON-NLS-1$
				errorMessage = PDECoreMessages.ExtensionsErrorReporter_InvalidSchema;
			// Report an error if one was found
			if (errorMessage != null) {
				severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_RESOURCE);
				if (severity != CompilerFlags.IGNORE)
					report(NLS.bind(errorMessage, schemaValue), getLine(element), severity, PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	protected void validateTranslatableString(Element element, Attr attr, boolean shouldTranslate) {
		if (!shouldTranslate)
			return;
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_NOT_EXTERNALIZED);
		if (severity == CompilerFlags.IGNORE)
			return;
		String value = attr.getValue();
		if (!value.startsWith("%")) { //$NON-NLS-1$
			report(NLS.bind(PDECoreMessages.Builders_Manifest_non_ext_attribute, attr.getName()), getLine(element, attr.getName()), severity, PDEMarkerFactory.P_UNTRANSLATED_NODE, element, attr.getName(), PDEMarkerFactory.CAT_NLS);
		} else if (fModel instanceof AbstractNLModel) {
			NLResourceHelper helper = ((AbstractNLModel) fModel).getNLResourceHelper();
			if (helper == null || !helper.resourceExists(value)) {
				report(NLS.bind(PDECoreMessages.Builders_Manifest_key_not_found, value.substring(1)), getLine(element, attr.getName()), severity, PDEMarkerFactory.CAT_NLS);
			}
		}
	}

	protected void validateTranslatableElementContent(Element element) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_NOT_EXTERNALIZED);
		if (severity == CompilerFlags.IGNORE)
			return;
		String value = getTextContent(element);
		if (value == null)
			return;
		if (!value.startsWith("%")) { //$NON-NLS-1$
			report(NLS.bind(PDECoreMessages.Builders_Manifest_non_ext_element, element.getNodeName()), getLine(element), severity, PDEMarkerFactory.P_UNTRANSLATED_NODE, element, null, PDEMarkerFactory.CAT_NLS);
		} else if (fModel instanceof AbstractNLModel) {
			NLResourceHelper helper = ((AbstractNLModel) fModel).getNLResourceHelper();
			if (helper == null || !helper.resourceExists(value)) {
				report(NLS.bind(PDECoreMessages.Builders_Manifest_key_not_found, value.substring(1)), getLine(element), severity, PDEMarkerFactory.CAT_NLS);
			}
		}
	}

	protected void validateResourceAttribute(Element element, Attr attr) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_RESOURCE);
		if (severity != CompilerFlags.IGNORE && !resourceExists(attr.getValue())) {
			report(NLS.bind(PDECoreMessages.Builders_Manifest_resource, (new String[] {attr.getValue(), attr.getName()})), getLine(element, attr.getName()), severity, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private boolean resourceExists(String location) {
		String bundleJar = null;
		IPath path = new Path(location);
		if ("platform:".equals(path.getDevice()) && path.segmentCount() > 2) { //$NON-NLS-1$
			if ("plugin".equals(path.segment(0))) { //$NON-NLS-1$
				String id = path.segment(1);
				IPluginModelBase model = PluginRegistry.findModel(id);
				if (model != null && model.isEnabled()) {
					path = path.setDevice(null).removeFirstSegments(2);
					String bundleLocation = model.getInstallLocation();
					if (new File(bundleLocation).isDirectory()) {
						path = new Path(model.getInstallLocation()).append(path);
					} else {
						bundleJar = bundleLocation;
					}
					location = path.toString();
				}
			}
		} else if (path.getDevice() == null && path.segmentCount() > 3 && "platform:".equals(path.segment(0))) { //$NON-NLS-1$
			if ("plugin".equals(path.segment(1))) { //$NON-NLS-1$
				String id = path.segment(2);
				IPluginModelBase model = PluginRegistry.findModel(id);
				if (model != null && model.isEnabled()) {
					path = path.removeFirstSegments(3);
					String bundleLocation = model.getInstallLocation();
					if (new File(bundleLocation).isDirectory()) {
						path = new Path(model.getInstallLocation()).append(path);
					} else {
						bundleJar = bundleLocation;
					}
					location = path.toString();
				}
			}
		}

		ArrayList paths = new ArrayList();
		if (location.indexOf("$nl$") != -1) { //$NON-NLS-1$
			StringTokenizer tokenizer = new StringTokenizer(TargetPlatform.getNL(), "_"); //$NON-NLS-1$
			String language = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
			String country = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : null;
			if (language != null && country != null)
				paths.add(location.replaceAll("\\$nl\\$", "nl" + IPath.SEPARATOR + language + IPath.SEPARATOR + country)); //$NON-NLS-1$ //$NON-NLS-2$
			if (language != null)
				paths.add(location.replaceAll("\\$nl\\$", "nl" + IPath.SEPARATOR + language)); //$NON-NLS-1$ //$NON-NLS-2$
			paths.add(location.replaceAll("\\$nl\\$", "")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			paths.add(location);
		}

		for (int i = 0; i < paths.size(); i++) {
			if (bundleJar == null) {
				IPath currPath = new Path(paths.get(i).toString());
				if (currPath.isAbsolute() && currPath.toFile().exists())
					return true;
				if (PDEProject.getBundleRoot(fFile.getProject()).findMember(currPath) != null)
					return true;
				if (fBuildModel != null && fBuildModel.getEntry("source." + paths.get(i)) != null) //$NON-NLS-1$
					return true;
			} else {
				if (CoreUtility.jarContainsResource(new File(bundleJar), paths.get(i).toString(), false))
					return true;
			}
		}

		return false;
	}

	protected void validateJavaAttribute(Element element, Attr attr) {
		String value = attr.getValue();
		IJavaProject javaProject = JavaCore.create(fFile.getProject());

		// be careful: people have the option to use the format:
		// fullqualifiedName:staticMethod
		int index = value.indexOf(":"); //$NON-NLS-1$
		if (index != -1)
			value = value.substring(0, index);

		// assume we're on the classpath already
		boolean onClasspath = true;
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_CLASS);
		if (severity != CompilerFlags.IGNORE && javaProject.isOpen()) {
			onClasspath = PDEJavaHelper.isOnClasspath(value, javaProject);
			if (!onClasspath) {
				report(NLS.bind(PDECoreMessages.Builders_Manifest_class, (new String[] {value, attr.getName()})), getLine(element, attr.getName()), severity, PDEMarkerFactory.P_UNKNOWN_CLASS, element, attr.getName() + F_ATT_VALUE_PREFIX + attr.getValue(), PDEMarkerFactory.CAT_FATAL);
			}
		}

		severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DISCOURAGED_CLASS);
		if (severity != CompilerFlags.IGNORE && javaProject.isOpen()) {
			BundleDescription desc = fModel.getBundleDescription();
			if (desc == null)
				return;
			// only check if we're discouraged if there is something on the classpath
			if (onClasspath && PDEJavaHelper.isDiscouraged(value, javaProject, desc)) {
				report(NLS.bind(PDECoreMessages.Builders_Manifest_discouragedClass, (new String[] {value, attr.getName()})), getLine(element, attr.getName()), severity, PDEMarkerFactory.M_DISCOURAGED_CLASS, element, attr.getName() + F_ATT_VALUE_PREFIX + attr.getValue(), PDEMarkerFactory.CAT_OTHER);
			}
		}
	}

	protected void validateRestrictionAttribute(Element element, Attr attr, ISchemaRestriction restriction) {
		Object[] children = restriction.getChildren();
		String value = attr.getValue();
		for (int i = 0; i < children.length; i++) {
			Object child = children[i];
			if (child instanceof ISchemaEnumeration) {
				ISchemaEnumeration enumeration = (ISchemaEnumeration) child;
				if (enumeration.getName().equals(value)) {
					return;
				}
			}
		}
		reportIllegalAttributeValue(element, attr);
	}

	private void validateIdentifierAttribute(Element element, Attr attr, ISchemaAttribute attInfo) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_IDENTIFIER);
		if (severity != CompilerFlags.IGNORE) {
			String value = attr.getValue();
			String basedOn = attInfo.getBasedOn();
			// only validate if we have a valid value and basedOn value
			if (value != null && basedOn != null && value.length() > 0 && basedOn.length() > 0) {
				Map attributes = PDESchemaHelper.getValidAttributes(attInfo);
				if (!attributes.containsKey(value)) { // report error if we are missing something
					report(NLS.bind(PDECoreMessages.ExtensionsErrorReporter_unknownIdentifier, (new String[] {attr.getValue(), attr.getName()})), getLine(element, attr.getName()), severity, PDEMarkerFactory.CAT_OTHER);
				}
			}
		}
	}

	protected void reportUnusedAttribute(Element element, String attName, int severity) {
		String message = NLS.bind(PDECoreMessages.Builders_Manifest_unused_attribute, attName);
		report(message, getLine(element, attName), severity, PDEMarkerFactory.CAT_OTHER);
	}

	protected void reportUnusedElement(Element element, int severity) {
		Node parent = element.getParentNode();
		report(NLS.bind(PDECoreMessages.Builders_Manifest_unused_element, (new String[] {element.getNodeName(), parent.getNodeName()})), getLine(element), severity, PDEMarkerFactory.CAT_OTHER);
	}

	protected void reportDeprecatedElement(Element element) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (severity != CompilerFlags.IGNORE) {
			report(NLS.bind(PDECoreMessages.Builders_Manifest_deprecated_element, element.getNodeName()), getLine(element), severity, PDEMarkerFactory.CAT_DEPRECATION);
		}
	}

	protected void reportDeprecatedRootElement(Element element, String suggestion) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (severity != CompilerFlags.IGNORE) {
			String point = element.getAttribute("point"); //$NON-NLS-1$
			if (point == null)
				return; // should never come to this...
			String message;
			if (suggestion != null)
				message = NLS.bind(PDECoreMessages.Builders_Manifest_deprecated_rootElementSuggestion, point, suggestion);
			else
				message = NLS.bind(PDECoreMessages.Builders_Manifest_deprecated_rootElement, point);
			report(message, getLine(element, "point"), severity, PDEMarkerFactory.CAT_DEPRECATION); //$NON-NLS-1$
		}
	}
}
