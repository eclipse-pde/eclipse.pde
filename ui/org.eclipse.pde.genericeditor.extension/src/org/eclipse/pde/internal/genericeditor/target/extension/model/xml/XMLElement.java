/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLElement {
	private String element;
	private int offset;
	private String name;
	private Map<String, String> attributes = new HashMap<>();
	private boolean isEndElement;
	private boolean isStartElement;

	private Pattern startElementNamePattern = Pattern.compile("<\\s*(?<name>\\w*).*", Pattern.DOTALL); //$NON-NLS-1$
	private Pattern endElementNamePattern = Pattern.compile("</\\s*(?<name>\\w*).*", Pattern.DOTALL); //$NON-NLS-1$
	private Pattern attributePattern = Pattern.compile("((?<key>\\w*)\\s*=\\s*\"(?<value>.*?)\")", Pattern.DOTALL);//$NON-NLS-1$

	public XMLElement(String element, int offset) {
		this.element = element;
		this.offset = offset;
		this.isEndElement = element.matches("</(.|\n)*|(.|\n)*/>(.|\n)*"); //$NON-NLS-1$
		this.isStartElement = element.matches("<[^/](.|\n)*"); //$NON-NLS-1$

		Pattern namePattern;
		if (isStartElement()) {
			namePattern = startElementNamePattern;
		} else {
			namePattern = endElementNamePattern;
		}
		Matcher nameMatcher = namePattern.matcher(element);
		nameMatcher.matches();
		name = nameMatcher.group("name"); //$NON-NLS-1$

		Matcher attrMatcher = attributePattern.matcher(element);
		while (attrMatcher.find()) {
			String key = attrMatcher.group("key"); //$NON-NLS-1$
			String value = attrMatcher.group("value"); //$NON-NLS-1$
			attributes.put(key, value);
		}
	}
	public boolean isEndElement() {
		return isEndElement;
	}

	public boolean isStartElement() {
		return isStartElement;
	}

	public int getStartOffset() {
		return offset;
	}

	public int getEndOffset() {
		return offset + element.length();
	}

	public String getAttributeValueByKey(String key) {
		return attributes.get(key);
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return element;
	}
}
