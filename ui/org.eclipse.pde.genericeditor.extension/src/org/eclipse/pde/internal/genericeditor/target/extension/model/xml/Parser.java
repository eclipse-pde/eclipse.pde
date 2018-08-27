/*******************************************************************************
 * Copyright (c) 2016, 2017 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *     Lucas Bullen (Red Hat Inc.) - [Bug 520004] autocomplete does not respect tag hierarchy
 *                                 - [Bug 528706] autocomplete does not respect multiline tags
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model.xml;

import java.io.ByteArrayInputStream;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.Node;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;

/**
 * Class used to parse the XML code into the model.
 */
public class Parser {

	private static Parser instance;

	private Node target;

	private XMLInputFactory inputFactory;

	public Parser() {
		initializeParser();
	}

	private void initializeParser() {
		inputFactory = XMLInputFactory.newInstance();
	}

	public void parse(IDocument document) throws XMLStreamException {
		target = null;
		Node currentParent = null;
		Node currentNode = null;
		Iterator<XMLElement> tagReaderIterator = createXMLTagItterator(document.get());
		while (tagReaderIterator.hasNext()) {
			XMLElement event = tagReaderIterator.next();
			if (event.isStartElement()) {
				String name = event.getName();
				if (ITargetConstants.UNIT_TAG.equalsIgnoreCase(name)) {
					UnitNode unit = new UnitNode();
					String unitValue = event.getAttributeValueByKey(ITargetConstants.UNIT_ID_ATTR);
					if (unitValue != null) {
						unit.setId(unitValue);
					}
					String versionValue = event.getAttributeValueByKey(ITargetConstants.UNIT_VERSION_ATTR);
					if (versionValue != null) {
						unit.setVersion(versionValue);
					}
					currentNode = unit;
				} else if (ITargetConstants.LOCATION_TAG.equalsIgnoreCase(name)) {
					currentNode = new LocationNode();
				} else if (ITargetConstants.REPOSITORY_TAG.equalsIgnoreCase(name)) {
					currentNode = new Node();
					if (currentParent instanceof LocationNode) {
						String locationValue = event.getAttributeValueByKey(ITargetConstants.REPOSITORY_LOCATION_ATTR);
						((LocationNode) currentParent).setRepositoryLocation(locationValue);
					}
				} else if (ITargetConstants.TARGET_TAG.equalsIgnoreCase(name)) {
					target = new Node();
					currentNode = target;
				} else {
					currentNode = new Node();
				}
				currentNode.setNodeTag(name);
				currentNode.setOffsetStart(event.getStartOffset());
				if (currentParent != null) {
					currentParent.addChildNode(currentNode);
				}
				currentParent = currentNode;
			}

			if (event.isEndElement()) {
				if (currentNode != null) {
					currentNode.setOffsetEnd(event.getEndOffset());
					currentNode = currentNode.getParentNode();
					currentParent = currentNode;
				}
			}
		}
		ByteArrayInputStream inputStream = new ByteArrayInputStream(document.get().getBytes());
		XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
		while (eventReader.hasNext()) {
			eventReader.nextEvent();
		}
	}

	private Iterator<XMLElement> createXMLTagItterator(String document) {
		return new Iterator<XMLElement>() {
			private String tagRegex = "(?<tag><[\\w|/][^<]+?>)";
			private String commentRegex = "(<!--.*?-->)";
			private String beforeTagRegex = ".*?(?=".concat(tagRegex).concat(")");

			private String text = document;

			private Pattern tagPattern = Pattern.compile(tagRegex, Pattern.DOTALL);
			private Pattern commentPattern = Pattern.compile(commentRegex, Pattern.DOTALL);
			private Pattern beforeTagPattern = Pattern.compile(beforeTagRegex, Pattern.DOTALL);

			@Override
			public boolean hasNext() {
				skipComments();
				return text.length() > 0 && beforeTagPattern.matcher(text).find();
			}

			@Override
			public XMLElement next() {
				skipComments();
				text = beforeTagPattern.matcher(text).replaceFirst("");
				Matcher tag = tagPattern.matcher(text);
				tag.find();
				int offset = document.length() - text.length();
				text = text.substring(tag.end());
				return new XMLElement(tag.group("tag"), offset);
			}

			private void skipComments() {
				Matcher comment = commentPattern.matcher(text);
				Matcher tag = tagPattern.matcher(text);
				if (comment.find() && tag.find() && comment.start() < tag.start()) {
					text = text.substring(comment.end());
				}
			}
		};
	}
	public static Parser getDefault() {
		if (instance == null) {
			instance = new Parser();
		}
		return instance;
	}

	public Node getRootNode() {
		return target;
	}

}
