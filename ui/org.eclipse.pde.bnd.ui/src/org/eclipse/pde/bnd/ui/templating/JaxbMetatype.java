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
package org.eclipse.pde.bnd.ui.templating;

import java.io.InputStream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.pde.osgi.xmlns.metatype.v1_4.ObjectFactory;
import org.eclipse.pde.osgi.xmlns.metatype.v1_4.Tmetadata;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;

class JaxbMetatype {
	
	private static JAXBContext jaxbContext;
	
	private static boolean loaded;


	static Tmetadata readMetaType(InputStream entryInput) throws JAXBException {
		JAXBContext context = getJaxbContext();
		if (context == null) {
			return null;
		}
		Object unmarshal = context.createUnmarshaller().unmarshal(entryInput);
		if (unmarshal instanceof Tmetadata metadata) {
			return metadata;
		}
		if (unmarshal instanceof JAXBElement<?> elem) {
			Object value = elem.getValue();
			if (value instanceof Tmetadata metadata) {
				return metadata;
			}
		}
		return null;
	}


	public static JAXBContext getJaxbContext() {
		if (!loaded && jaxbContext == null) {
			try {
				jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			} catch (JAXBException e) {
				ILog.get().warn("Can't load JAXBContext, bnd template processing might be incomplete!", e);
			}
			loaded = true;
		}
		return jaxbContext;
	}
}
