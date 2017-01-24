/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Sopot Cela (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.model.xml;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.TargetNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;

/**
 * Class used to parse the XML code into the model.
 */
public class Parser {

	private static Parser instance;

	private LocationNode locationNode;
	private TargetNode target;
	private UnitNode unit;

	private XMLInputFactory inputFactory;

	public Parser() {
		initializeParser();
	}

	private void initializeParser() {
		inputFactory = XMLInputFactory.newInstance();
	}

	public void parse(IDocument document) throws XMLStreamException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(document.get().getBytes());
		XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			Location locator = event.getLocation();
			if (event.isStartElement()) {
				StartElement startElement = event.asStartElement();
				String name = startElement.getName().getLocalPart();
				if (ITargetConstants.TARGET_TAG.equalsIgnoreCase(name)) {
					target = new TargetNode();
					int lineNr = locator.getLineNumber();
					try {
						int offset = document.getLineOffset(lineNr - 1);
						target.setOffsetStart(offset);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}

				if (ITargetConstants.LOCATION_TAG.equalsIgnoreCase(name)) {
					int lineNr = locator.getLineNumber();
					try {
						int offset = document.getLineOffset(lineNr - 1);
						locationNode = new LocationNode();
						locationNode.setOffsetStart(offset);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}

				if (ITargetConstants.REPOSITORY_TAG.equalsIgnoreCase(name)) {
					Attribute locationAttribute = startElement
							.getAttributeByName(new QName(ITargetConstants.REPOSITORY_LOCATION_ATTR));
					String value = locationAttribute == null ? "" : locationAttribute.getValue(); //$NON-NLS-1$
					if (locationNode == null) {
						return;
					}
					locationNode.setRepositoryLocation(value);
				}

				if (ITargetConstants.UNIT_TAG.equalsIgnoreCase(name)) {
					if (locationNode == null) {
						return;
					}
					int lineNr = locator.getLineNumber();
					try {
						int offset = document.getLineOffset(lineNr - 1);
						unit = new UnitNode();
						Attribute idAttribute = startElement
								.getAttributeByName(new QName(ITargetConstants.UNIT_ID_ATTR));
						if (idAttribute != null) {
							unit.setId(idAttribute.getValue());
						}
						Attribute versionAttribute = startElement
								.getAttributeByName(new QName(ITargetConstants.UNIT_VERSION_ATTR));
						if (versionAttribute != null) {
							unit.setVersion(versionAttribute.getValue());
						}
						unit.setOffsetStart(offset);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}

			if (event.isEndElement()) {
				EndElement endElement = event.asEndElement();
				String name = endElement.getName().getLocalPart();
				if (ITargetConstants.TARGET_TAG.equalsIgnoreCase(name)) {
					int lineNr = locator.getLineNumber();
					try {
						int offset = document.getLineOffset(lineNr - 1);
						target.setOffsetEnd(offset);
						target.setNodeText(
								document.get(target.getOffsetStart(), target.getOffsetEnd() - target.getOffsetStart()));
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}

				if (ITargetConstants.LOCATION_TAG.equalsIgnoreCase(name)) {
					int lineNr = locator.getLineNumber();
					try {
						int offset = document.getLineOffset(lineNr - 1);
						locationNode.setOffsetEnd(offset);
						target.getNodes().add(locationNode);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}

				if (ITargetConstants.UNIT_TAG.equalsIgnoreCase(name)) {
					if (unit == null) {
						return;
					}
					if (locationNode == null) {
						return;
					}
					int lineNr = locator.getLineNumber();
					try {
						int offset = document.getLineOffset(lineNr);
						unit.setOffsetEnd(offset);
						locationNode.addUnitNode(unit);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static Parser getDefault() {
		if (instance == null) {
			instance = new Parser();
		}
		return instance;
	}

	public TargetNode getRootNode() {
		return target;
	}

}
