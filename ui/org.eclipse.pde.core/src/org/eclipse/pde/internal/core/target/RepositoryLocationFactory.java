/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.osgi.resource.Requirement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.resource.CapReqBuilder;

public class RepositoryLocationFactory implements ITargetLocationFactory {

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		if (!RepositoryBundleContainer.TYPE.equals(type)) {
			throw new CoreException(
					Status.error(NLS.bind(Messages.TargetRefrenceLocationFactory_Unsupported_Type, type)));
		}
		try {
			@SuppressWarnings("restriction")
			DocumentBuilder docBuilder = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createDocumentBuilderWithErrorOnDOCTYPE();
			Document document = docBuilder
					.parse(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
			Element location = document.getDocumentElement();
			NodeList childNodes = location.getChildNodes();
			List<Requirement> requirements = IntStream.range(0, childNodes.getLength()).mapToObj(childNodes::item)
					.filter(Element.class::isInstance).map(Element.class::cast)
					.filter(element -> element.getNodeName()
							.equalsIgnoreCase(RepositoryBundleContainer.ELEMENT_REQUIRE))
					.flatMap(element -> {
						String textContent = element.getTextContent();
						Parameters parameters = new Parameters(textContent);
						return CapReqBuilder.getRequirementsFrom(parameters).stream();
					}).toList();
			return new RepositoryBundleContainer(
					location.getAttribute(RepositoryBundleContainer.ATTRIBUTE_URI), requirements);
		} catch (Exception e) {
			throw new CoreException(
					Status.error(NLS.bind(Messages.TargetRefrenceLocationFactory_Parsing_Failed, e.getMessage()), e));
		}
	}

}
