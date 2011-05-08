/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Simon Muschel <smuschel@gmx.de> - bug 215743
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.IncludedSchemaDescriptor;
import org.eclipse.pde.internal.core.schema.SchemaDescriptor;
import org.w3c.dom.*;

public class SchemaErrorReporter extends XMLErrorReporter {

	class StackEntry {
		String tag;
		int line;

		public StackEntry(String tag, int line) {
			this.tag = tag;
			this.line = line;
		}
	}

	public static final String[] forbiddenEndTagKeys = {"area", //$NON-NLS-1$
			"base", //$NON-NLS-1$
			"basefont", //$NON-NLS-1$
			"col", //$NON-NLS-1$
			"frame", //$NON-NLS-1$
			"hr", //$NON-NLS-1$
			"img", //$NON-NLS-1$
			"input", //$NON-NLS-1$
			"isindex", //$NON-NLS-1$
			"link", //$NON-NLS-1$
			"meta", //$NON-NLS-1$
			"param"}; //$NON-NLS-1$

	public static final String[] optionalEndTagKeys = {"body", //$NON-NLS-1$
			"br", //$NON-NLS-1$
			"colgroup", //$NON-NLS-1$
			"dd", //$NON-NLS-1$
			"dt", //$NON-NLS-1$
			"head", //$NON-NLS-1$
			"html", //$NON-NLS-1$
			"li", //$NON-NLS-1$
			"option", //$NON-NLS-1$
			"p", //$NON-NLS-1$
			"tbody", //$NON-NLS-1$
			"td", //$NON-NLS-1$
			"tfoot", //$NON-NLS-1$
			"th", //$NON-NLS-1$
			"thead", //$NON-NLS-1$
			"tr"}; //$NON-NLS-1$

	private ISchema fSchema;
	private static final String ELEMENT = "element"; //$NON-NLS-1$
	private static final String DOCUMENTATION = "documentation"; //$NON-NLS-1$
	private static final String ANNOTATION = "annotation"; //$NON-NLS-1$
	private static final String ATTRIBUTE = "attribute"; //$NON-NLS-1$
	private static final String INCLUDE = "include"; //$NON-NLS-1$

	private static final String ATTR_NAME = "name"; //$NON-NLS-1$
	private static final String ATTR_VALUE = "value"; //$NON-NLS-1$
	private static final String ATTR_USE = "use"; //$NON-NLS-1$
	private static final String ATTR_REF = "ref"; //$NON-NLS-1$
	private static final String ATTR_LOCATION = "schemaLocation"; //$NON-NLS-1$

	public SchemaErrorReporter(IFile file) {
		super(file);
		SchemaDescriptor desc = new SchemaDescriptor(fFile, true);
		fSchema = desc.getSchema(false);
	}

	public void validateContent(IProgressMonitor monitor) {
		List elements = new ArrayList();
		Element element = getDocumentRoot();
		if (element != null) {
			NodeList children = element.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child instanceof Element) {
					Element childElement = (Element) child;
					String name = childElement.getNodeName();
					if (name != null && name.equals(ELEMENT)) {
						String value = childElement.getAttribute(ATTR_NAME);
						if (value != null && value.length() > 0) {
							if (elements.contains(value)) { // report error
								report(NLS.bind(PDECoreMessages.Builders_Schema_duplicateElement, value), getLine((Element) child), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
							} else { // add to list
								elements.add(value);
							}
						}
					} else if (name != null && name.equals(INCLUDE)) {
						validateInclude(childElement);
					}
					validate((Element) child);
				}
			}
		}
	}

	private void validate(Element element) {
		if (element.getNodeName().equals(ATTRIBUTE))
			validateAttribute(element);

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				if (child.getNodeName().equals(ANNOTATION)) {
					validateAnnotation((Element) child);
				} else if (child.getNodeName().equals(ELEMENT)) {
					validateElementReference((Element) child);
				} else {
					validate((Element) child);
				}
			}
		}
	}

	private void validateAnnotation(Element element) {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element && child.getNodeName().equals(DOCUMENTATION)) {
				validateDocumentation((Element) child);
			}
		}
	}

	private void validateDocumentation(Element element) {
		int flag = CompilerFlags.getFlag(fProject, CompilerFlags.S_OPEN_TAGS);

		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Text) {
				Text textNode = (Text) children.item(i);
				StringTokenizer text = new StringTokenizer(textNode.getData(), "<>", true); //$NON-NLS-1$

				int lineNumber = getLine(element);
				Stack stack = new Stack();
				boolean errorReported = false;
				while (text.hasMoreTokens()) {
					if (errorReported)
						break;

					String next = text.nextToken();
					if (next.equals("<")) { //$NON-NLS-1$
						if (text.countTokens() > 2) {
							String tagName = text.nextToken();
							String closing = text.nextToken();
							if (closing.equals(">")) { //$NON-NLS-1$
								// Skip comments and processing instructions
								if (tagName.startsWith("!--") || //$NON-NLS-1$
										tagName.endsWith("--") || //$NON-NLS-1$
										tagName.startsWith("?") || //$NON-NLS-1$
										tagName.endsWith("?")) { //$NON-NLS-1$
									lineNumber += getLineBreakCount(tagName);
									continue;
								}

								if (tagName.endsWith("/")) { //$NON-NLS-1$
									tagName = getTagName(tagName.substring(0, tagName.length() - 1));
									if (forbiddenEndTag(tagName)) {
										report(NLS.bind(PDECoreMessages.Builders_Schema_forbiddenEndTag, tagName), lineNumber, flag, PDEMarkerFactory.CAT_OTHER);
										errorReported = true;
									}
								} else if (tagName.startsWith("/")) { //$NON-NLS-1$
									lineNumber += getLineBreakCount(tagName);
									tagName = tagName.substring(1).trim();
									boolean found = false;
									while (!stack.isEmpty()) {
										StackEntry entry = (StackEntry) stack.peek();
										if (entry.tag.equalsIgnoreCase(tagName)) {
											stack.pop();
											found = true;
											break;
										} else if (optionalEndTag(entry.tag)) {
											stack.pop();
										} else {
											break;
										}
									}
									if (stack.isEmpty() && !found) {
										report(NLS.bind(PDECoreMessages.Builders_Schema_noMatchingStartTag, tagName), lineNumber, flag, PDEMarkerFactory.CAT_OTHER);
										errorReported = true;
									}
								} else {
									String shortTag = getTagName(tagName);
									if (!forbiddenEndTag(shortTag))
										stack.push(new StackEntry(shortTag, lineNumber));
									lineNumber += getLineBreakCount(tagName);
								}
							}
						}
					} else {
						lineNumber += getLineBreakCount(next);
					}
				}
				if (!errorReported) {
					if (!stack.isEmpty()) {
						StackEntry entry = (StackEntry) stack.pop();
						if (!optionalEndTag(entry.tag))
							report(NLS.bind(PDECoreMessages.Builders_Schema_noMatchingEndTag, entry.tag), entry.line, flag, PDEMarkerFactory.CAT_OTHER);
					}
					stack.clear();
				}
			}
		}
	}

	private String getTagName(String text) {
		StringTokenizer tokenizer = new StringTokenizer(text);
		return tokenizer.nextToken();
	}

	private boolean optionalEndTag(String tag) {
		for (int i = 0; i < optionalEndTagKeys.length; i++) {
			if (tag.equalsIgnoreCase(optionalEndTagKeys[i]))
				return true;
		}
		return false;
	}

	private boolean forbiddenEndTag(String tag) {
		for (int i = 0; i < forbiddenEndTagKeys.length; i++) {
			if (tag.equalsIgnoreCase(forbiddenEndTagKeys[i]))
				return true;
		}
		return false;
	}

	private int getLineBreakCount(String tag) {
		StringTokenizer tokenizer = new StringTokenizer(tag, "\n", true); //$NON-NLS-1$
		int token = 0;
		while (tokenizer.hasMoreTokens()) {
			if (tokenizer.nextToken().equals("\n")) //$NON-NLS-1$
				token++;
		}
		return token;
	}

	private void validateAttribute(Element element) {
		validateUse(element);
	}

	private void validateUse(Element element) {
		Attr use = element.getAttributeNode(ATTR_USE);
		Attr value = element.getAttributeNode(ATTR_VALUE);
		if (use != null && "default".equals(use.getValue()) && value == null) { //$NON-NLS-1$
			report(NLS.bind(PDECoreMessages.Builders_Schema_valueRequired, element.getNodeName()), getLine(element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_OTHER);
		} else if (use == null && value != null) {
			report(NLS.bind(PDECoreMessages.Builders_Schema_valueNotRequired, element.getNodeName()), getLine(element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private void validateInclude(Element element) {
		if (fSchema != null) {
			ISchemaInclude[] includes = fSchema.getIncludes();
			String schemaLocation = element.getAttribute(ATTR_LOCATION);
			for (int i = 0; i < includes.length; i++) {
				ISchemaInclude include = includes[i];
				ISchema includedSchema = include.getIncludedSchema();
				try {
					if (includedSchema == null)
						continue;
					URL includedSchemaUrl = includedSchema.getURL();
					URL computedUrl = IncludedSchemaDescriptor.computeURL(fSchema.getSchemaDescriptor(), schemaLocation);
					if (includedSchemaUrl != null && computedUrl != null && includedSchemaUrl.equals(computedUrl)) {
						if (!includedSchema.isValid())
							report(NLS.bind(PDECoreMessages.Builders_Schema_includeNotValid, schemaLocation), getLine(element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_OTHER);
					}
				} catch (MalformedURLException e) {
					// this should not happen since fSchema's URL is valid 
				}
			}
		}
	}

	private void validateElementReference(Element element) {
		String value = element.getAttribute(ATTR_REF);
		if (value != null && value.length() > 0) {
			ISchemaElement referencedElement = fSchema.findElement(value);
			if (referencedElement == null) {
				report(NLS.bind(PDECoreMessages.Builders_Schema_referencedElementNotFound, value), getLine(element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_OTHER);
			}
		}
	}
}
