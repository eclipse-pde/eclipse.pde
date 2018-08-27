/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import java.util.List;

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSDocumentFactory;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSObject;
import org.eclipse.pde.internal.ds.core.IDSProperties;
import org.eclipse.pde.internal.ds.core.IDSProperty;
import org.eclipse.pde.internal.ds.core.IDSReference;
import org.eclipse.pde.internal.ds.core.IDSService;

public class DSObjectTestCase extends AbstractDSModelTestCase {


	public void testObject(){
		StringBuilder buffer = new StringBuilder();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);

		IDSDocumentFactory factory = fModel.getFactory();

		IDSImplementation implementation = factory.createImplementation();
		implementation.setClassName("ImplementationClassName");
		component.setImplementation(implementation);

		IDSProperties properties = factory.createProperties();
		properties.setEntry("PropertiesEntry");
		component.addPropertiesElement(properties);

		IDSProperty property = factory.createProperty();
		property.setPropertyElemBody("Body Values");
		property.setPropertyType("java.lang.String");
		component.addPropertyElement(property);

		IDSService service = factory.createService();
		service.setServiceFactory(true);
		component.setService(service);

		IDSReference reference = factory.createReference();
		reference.setReferenceBind("methodBind");
		reference.setReferenceUnbind("methodUnBind");
		reference.setReferenceInterface("ReferenceInterface");
		reference.setReferenceName("ReferenceName");
		component.addReference(reference);

		List<?> children = component.getChildNodesList();
		assertTrue(children.size() == 5);
		assertEquals(component.getModel(), fModel);

		IDSObject child = (IDSObject)children.get(0);
		assertEquals(child.getComponent(), component);

		assertEquals(child.getParentNode(), component);
	}
}
