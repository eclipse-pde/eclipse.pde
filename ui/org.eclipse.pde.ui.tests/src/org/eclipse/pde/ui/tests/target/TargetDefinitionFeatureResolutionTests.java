/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.pde.ui.tests.target;

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