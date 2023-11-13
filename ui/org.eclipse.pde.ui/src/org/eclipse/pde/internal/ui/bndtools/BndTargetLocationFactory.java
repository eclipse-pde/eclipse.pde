/*******************************************************************************
 * Copyright (c) 2017, 2023 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Elias N Vasylenko <eliasvasylenko@gmail.com> - initial API and implementation
 *     BJ Hargrave <bj@bjhargrave.com> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.internal.ui.bndtools;

import java.io.ByteArrayInputStream;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public abstract class BndTargetLocationFactory implements ITargetLocationFactory {
	private final String type;

	public BndTargetLocationFactory(String type) {
		this.type = Objects.requireNonNull(type);
	}

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		if (this.type.equals(type)) {
			Element locationElement;
			try {
				DocumentBuilder docBuilder = org.eclipse.core.internal.runtime.XmlProcessorFactory
						.createDocumentBuilderFactoryIgnoringDOCTYPE()
					.newDocumentBuilder();
				Document document = docBuilder.parse(new ByteArrayInputStream(serializedXML.getBytes("UTF-8")));
				locationElement = document.getDocumentElement();

				if (this.type.equals(locationElement.getAttribute(BndTargetLocation.ATTRIBUTE_LOCATION_TYPE))) {
					return getTargetLocation(locationElement);
				}
			} catch (Exception e) {
				ILog.get()
						.error("Problem reading target location " + type);
				return null;
			}
		}
		return null;
	}

	public boolean isElement(Node node, String elementName) {
		return node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName()
			.equalsIgnoreCase(elementName);
	}

	public abstract ITargetLocation getTargetLocation(Element locationElement) throws CoreException;
}
