/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TargetRefrenceLocationFactory implements ITargetLocationFactory {

	@Override
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		if (!TargetReferenceBundleContainer.TYPE.equals(type)) {
			throw new CoreException(
					Status.error(NLS.bind(Messages.TargetRefrenceLocationFactory_Unsupported_Type, type)));
		}
		try {
			@SuppressWarnings("restriction")
			Document document = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.parseWithErrorOnDOCTYPE(new ByteArrayInputStream(serializedXML.getBytes(StandardCharsets.UTF_8)));
			Element location = document.getDocumentElement();
			return new TargetReferenceBundleContainer(
					location.getAttribute(TargetReferenceBundleContainer.ATTRIBUTE_URI));
		} catch (Exception e) {
			throw new CoreException(
					Status.error(NLS.bind(Messages.TargetRefrenceLocationFactory_Parsing_Failed, e.getMessage()), e));
		}
	}

}
