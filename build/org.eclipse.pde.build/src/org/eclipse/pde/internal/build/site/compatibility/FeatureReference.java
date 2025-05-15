/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site.compatibility;

import java.nio.file.Path;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.equinox.p2.publisher.eclipse.Feature;
import org.eclipse.pde.internal.build.site.BuildTimeFeatureFactory;
import org.eclipse.pde.internal.build.site.BuildTimeSite;

public class FeatureReference {
	private BuildTimeSite site;
	private Path path;
	private Feature feature;

	public void setSiteModel(BuildTimeSite site) {
		this.site = site;
	}

	public void setPath(Path path) {
		this.path = path;
	}

	public Feature getFeature() throws CoreException {
		if (feature != null) {
			return feature;
		}
		if (site != null) {
			feature = site.createFeature(path);
		} else {
			BuildTimeFeatureFactory factory = BuildTimeFeatureFactory.getInstance();
			feature = factory.createFeature(path, null);
		}
		return feature;
	}

	public Path getPath() {
		return path;
	}
}
