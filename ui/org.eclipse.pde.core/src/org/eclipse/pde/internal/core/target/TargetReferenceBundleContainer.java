/*******************************************************************************
 * Copyright (c) 2021, 2022 Christoph Läubrich and others.
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
 *     Patrick Ziegler - issue #90: Unable to load p2 dependencies from nested target files
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;
import org.eclipse.pde.internal.core.PDECore;

public class TargetReferenceBundleContainer extends AbstractBundleContainer {

	public static final String ATTRIBUTE_URI = "uri"; //$NON-NLS-1$

	public static final String TYPE = "Target"; //$NON-NLS-1$

	private final String uri;

	private volatile ITargetDefinition targetDefinition;

	public TargetReferenceBundleContainer(String uri) {
		this.uri = uri;
	}

	@Override
	protected TargetBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		return getTarget(monitor).getBundles();
	}

	@Override
	protected int getResolveBundlesWork() {
		return 99;
	}

	@Override
	protected int getResolveFeaturesWork() {
		return 1;
	}

	@Override
	protected TargetFeature[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor)
			throws CoreException {
		return getTarget(monitor).getAllFeatures();
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getLocation(boolean resolve) throws CoreException {
		if (resolve) {
			return resolveVariables(uri);
		}
		return uri;
	}

	@Override
	public String serialize() {
		return String.format("<location %s=\"%s\" type=\"%s\" />", ATTRIBUTE_URI, getUri(), TYPE); //$NON-NLS-1$
	}

	public String getUri() {
		return uri;
	}

	synchronized ITargetDefinition getTargetDefinition() throws CoreException {
		// only synchronize here, we just want to make sure not two threads are
		// loading the target in parallel but not block the targetDefinition or
		// reload operation as these might be called from the UI
		if (targetDefinition == null) {
			ITargetDefinition definition = RemoteTargetHandle.get(uri).getTargetDefinition();
			targetDefinition = definition;
			return definition;
		}
		return targetDefinition;
	}

	public Optional<ITargetDefinition> targetDefinition() {
		return Optional.ofNullable(targetDefinition);
	}

	public void reload() {
		if (targetDefinition != null) {
			try {
				P2TargetUtils.deleteProfile(targetDefinition.getHandle());
			} catch (CoreException e) {
				PDECore.log(e);
			}
		}
		targetDefinition = null;
	}

	private ITargetDefinition getTarget(IProgressMonitor monitor) throws CoreException {
		ITargetDefinition definition = getTargetDefinition();
		synchronized (definition) {
			if (!definition.isResolved()) {
				IStatus status = definition.resolve(monitor);
				if (monitor.isCanceled()) {
					throw new CoreException(Status.CANCEL_STATUS);
				}
				if (!status.isOK()) {
					throw new CoreException(status);
				}
				MultiStatus bundleStatus = new MultiStatus(getClass(), IStatus.OK,
						Messages.TargetRefrenceBundleContainer_Failure);
				Optional.ofNullable(definition.getBundles()).stream().flatMap(Arrays::stream)
						.map(TargetBundle::getStatus).filter(Predicate.not(IStatus::isOK)).forEach(bundleStatus::add);
				if (!bundleStatus.isOK()) {
					throw new CoreException(status);
				}
			}
			return definition;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TargetReferenceBundleContainer)) {
			return false;
		}
		return uri.equals(((TargetReferenceBundleContainer) obj).getUri());
	}

}