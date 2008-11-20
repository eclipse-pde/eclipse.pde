/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.io.File;
import java.util.Dictionary;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.artifact.repository.IArtifactDescriptor;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.publisher.eclipse.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.Utils;

public class GatherBundleAction extends BundlesAction {
	private GatheringComputer computer = null;
	private String unpack = null;
	private File manifestRoot = null;
	private IPublisherInfo publisherInfo = null;

	/**
	 * @param location
	 */
	public GatherBundleAction(File location, File manifestRoot) {
		super(new File[] {location});
		this.manifestRoot = manifestRoot;
	}

	public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
		this.publisherInfo = info;
		return super.perform(info, results, monitor);
	}

	protected void publishArtifact(IArtifactDescriptor descriptor, File base, File[] inclusions, IPublisherInfo info) {
		//ignore passed in inclusions, publish according to our computer
		publishArtifact(descriptor, computer.getFiles(), null, info, computer);
	}

	protected BundleDescription[] getBundleDescriptions(File[] bundleLocations, IProgressMonitor monitor) {
		Dictionary manifest = basicLoadManifest(manifestRoot);
		if (manifest == null)
			return null;

		BundleDescription bundle = createBundleDescription(manifest, bundleLocations[0]);
		createShapeAdvice(bundle);
		return new BundleDescription[] {bundle};
	}

	protected void createShapeAdvice(BundleDescription bundle) {
		Dictionary manifest = (Dictionary) bundle.getUserObject();
		String shape = (String) manifest.get(BUNDLE_SHAPE);
		if (shape == null) {
			if (unpack != null) {
				shape = Boolean.valueOf(unpack).booleanValue() ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
			} else {
				shape = Utils.guessUnpack(bundle, BundleHelper.getClasspath(manifest)) ? IBundleShapeAdvice.DIR : IBundleShapeAdvice.JAR;
			}
		}
		BundleShapeAdvice advice = new BundleShapeAdvice(bundle.getSymbolicName(), bundle.getVersion(), shape);
		publisherInfo.addAdvice(advice);
	}

	public void setComputer(GatheringComputer computer) {
		this.computer = computer;
	}

	public void setUnpack(String unpack) {
		this.unpack = unpack;
	}
}
