/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.tests;

import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSReference;

public class DSReferenceTestCase extends AbstractDSModelTestCase {

	public void testServiceReference() {
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		IDSReference reference = fModel.getFactory().createReference();
		String name = "HTTP";
		String interfaceName = "org.osgi.service.http.HttpService";
		String cardinality = "0..n";
		String policy = "dynamic";
		String bind = "setPage";
		String unbind = "unsetPage";
		String target = "(component.factory=acme.application)";

		reference.setReferenceName(name);
		reference.setReferenceInterface(interfaceName);
		reference.setReferenceCardinality(cardinality);
		reference.setReferencePolicy(policy);
		reference.setReferenceBind(bind);
		reference.setReferenceUnbind(unbind);

		reference.setReferenceTarget(target);

		component.addChildNode(reference);

		IDSReference[] references = component.getReferences();

		assertTrue(references.length == 1);

		IDSReference reference0 = references[0];

		assertEquals(reference0.getReferenceName(), name);
		assertEquals(reference0.getReferenceInterface(), interfaceName);
		assertEquals(reference0.getReferenceCardinality(), cardinality);
		assertEquals(reference0.getReferencePolicy(), policy);
		assertEquals(reference0.getReferenceBind(), bind);
		assertEquals(reference0.getReferenceUnbind(), unbind);
		assertEquals(reference0.getName(), name);

	}

	public void testDefaultServiceReference() {
		StringBuffer buffer = new StringBuffer();
		setXMLContents(buffer, LF);
		load();

		IDSComponent component = fModel.getDSComponent();
		assertNotNull(component);
		IDSReference reference = fModel.getFactory().createReference();
		component.addChildNode(reference);

		IDSReference[] references = component.getReferences();

		assertTrue(references.length == 1);

		IDSReference reference0 = references[0];

		String defautCardinality = "1..1";
		String defaultPolicy = "static";
		
		assertEquals(reference0.getReferenceCardinality(), defautCardinality);
		
		assertEquals(reference0.getReferencePolicy(), defaultPolicy);

		
	}

}
