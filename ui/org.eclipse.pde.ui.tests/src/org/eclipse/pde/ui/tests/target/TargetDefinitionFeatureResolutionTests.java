/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.target;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.pde.core.plugin.TargetPlatform;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * Tests whether targets and bundle containers manage features correctly.
 *
 * @since 3.6
 */
public class TargetDefinitionFeatureResolutionTests extends MinimalTargetDefinitionFeatureResolutionTests {

	public static Test suite() {
		return new TestSuite(TargetDefinitionFeatureResolutionTests.class);
	}

	/**
	 * Tests that a feature bundle container provides the correct features to a target
	 */
	public void testFeatureBundleContainer() throws Exception{
		ITargetDefinition definition = getNewTarget();
		ITargetLocation featureContainer = getTargetService().newFeatureLocation(TargetPlatform.getDefaultLocation(), "org.eclipse.pde", null);

		assertNull(featureContainer.getFeatures());

		IFeatureModel[] possibleFeatures = PDECore.getDefault().getFeatureModelManager().findFeatureModels("org.eclipse.pde");
		assertTrue(possibleFeatures.length > 0);

		featureContainer.resolve(definition, null);
		TargetFeature[] features = featureContainer.getFeatures();
		assertNotNull(features);
		assertEquals(features.length, 1);
		assertEquals(features[0].getId(),possibleFeatures[0].getFeature().getId());
	}

}