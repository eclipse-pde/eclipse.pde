/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import org.eclipse.update.core.Feature;
import org.eclipse.update.core.IIncludedFeatureReference;

public class BuildTimeFeature extends Feature {
	public IIncludedFeatureReference[] getRawIncludedFeatureReferences() {
		return getFeatureIncluded();
	}
}
