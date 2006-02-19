/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Stack;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;


public class SchemaErrorReporter extends XMLErrorReporter {
	
	class StackEntry {
		String tag;
		int line;
		
		public StackEntry(String tag, int line) {
			this.tag = tag;
			this.line = line;
		}
	}

	public static final String[] forbiddenEndTagKeys =
	{
		"area", //$NON-NLS-1$
		"base", //$NON-NLS-1$
		"basefont", //$NON-NLS-1$
		"br", //$NON-NLS-1$
		"col", //$NON-NLS-1$
		"frame", //$NON-NLS-1$
		"hr", //$NON-NLS-1$
		"img", //$NON-NLS-1$
		"input", //$NON-NLS-1$
		"isindex", //$NON-NLS-1$
		"link", //$NON-NLS-1$
		"meta", //$NON-NLS-1$
		"param" }; //$NON-NLS-1$
	
	public static final String[] optionalEndTagKeys =
		{
			"body", //$NON-NLS-1$
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
			"tr" }; //$NON-NLS-1$
	
	
	public SchemaErrorReporter(IFile file) {
		super(file);
	}
	
	public void validateContent(IProgressMonitor monitor) {
		Element element = getDocumentRoot();
		if (element != null)
			validateElement(element);
	}
	
	private void validateElement(Element element) {
		if (element.getNodeName().equals("attribute")) //$NON-NLS-1$
			validateAttribute(element);
	
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				if (child.getNodeName().equals("annotation")) { //$NON-NLS-1$
					validateAnnotation((Element)child);
				} else {
					validateElement((Element)child);
				}
			}
		}
	}
	
	private void validateAnnotation(Element element) {
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element && child.getNodeName().equals("documentation")) { //$NON-NLS-1$
				validateDocumentation((Element)child);
			}
		}
	}

	private void validateDocumentation(Element element) {
		int flag = CompilerFlags.getFlag(fProject, CompilerFlags.S_OPEN_TAGS);
		
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i) instanceof Text) {
				Text textNode = (Text)children.item(i);	
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
								if (tagName.startsWith("!--") || tagName.endsWith("--")) { //$NON-NLS-1$ //$NON-NLS-2$
									lineNumber += getLineBreakCount(tagName);
									continue;
								}
								
								if (tagName.endsWith("/")) { //$NON-NLS-1$
									tagName = getTagName(tagName.substring(0, tagName.length() - 1));
									if (forbiddenEndTag(tagName)) {
										report(NLS.bind(PDECoreMessages.Builders_Schema_forbiddenEndTag, tagName), lineNumber, flag); 
										errorReported = true;
									}
								} else if (tagName.startsWith("/")) { //$NON-NLS-1$
									lineNumber += getLineBreakCount(tagName);
									tagName = tagName.substring(1).trim();
									boolean found = false;
									while (!stack.isEmpty()) {
										StackEntry entry = (StackEntry)stack.peek();
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
										report(NLS.bind(PDECoreMessages.Builders_Schema_noMatchingStartTag, tagName), lineNumber, flag); 
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
						StackEntry entry = (StackEntry)stack.pop();
						if (!optionalEndTag(entry.tag))
							report(NLS.bind(PDECoreMessages.Builders_Schema_noMatchingEndTag, entry.tag), entry.line, flag);
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
	
	private int getLineBreakCount(String tag){
		StringTokenizer tokenizer = new StringTokenizer(tag, "\n", true); //$NON-NLS-1$
		int token = 0;
		while (tokenizer.hasMoreTokens()){
			if (tokenizer.nextToken().equals("\n")) //$NON-NLS-1$
				token++;
		}
		return token;
	}

	private void validateAttribute(Element element) {
		validateUse(element);
	}
	
	private void validateUse(Element element) {
		Attr use = element.getAttributeNode("use"); //$NON-NLS-1$
		Attr value = element.getAttributeNode("value"); //$NON-NLS-1$
		if (use != null && "default".equals(use.getValue()) && value == null) { //$NON-NLS-1$
			report(NLS.bind(PDECoreMessages.Builders_Schema_valueRequired, element.getNodeName()),  
					getLine(element),
					CompilerFlags.ERROR);
		} else if (use == null && value != null) {
			report(NLS.bind(PDECoreMessages.Builders_Schema_valueNotRequired, element.getNodeName()),  
					getLine(element),
					CompilerFlags.ERROR);
		}
	}

}
