/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 568865 - [target] add advanced editing capabilities for custom target platforms
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.IUBundleContainer;
import org.eclipse.pde.internal.core.target.P2TargetUtils;
import org.eclipse.pde.internal.ui.shared.target.IUContentProvider.IUWrapper;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

/**
 * Adapter factory for providing all necessary UI components for the
 * {@link IUBundleContainer}
 *
 */
public class IUFactory implements IAdapterFactory, ITargetLocationHandler {

	private static final Status STATUS_NO_CHANGE = new Status(IStatus.OK, PDECore.PLUGIN_ID, STATUS_CODE_NO_CHANGE, "", //$NON-NLS-1$

			null);
	private static final Status STATUS_FORCE_RELOAD = new Status(IStatus.OK, PDECore.PLUGIN_ID,
			ITargetLocationHandler.STATUS_FORCE_RELOAD, "", null); //$NON-NLS-1$ ;
	private ILabelProvider fLabelProvider;
	private ITreeContentProvider fContentProvider;

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[] { ILabelProvider.class, ITreeContentProvider.class, ITargetLocationHandler.class };
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof IUBundleContainer) {
			if (adapterType == ILabelProvider.class) {
				return (T) getLabelProvider();
			} else if (adapterType == ITreeContentProvider.class) {
				return (T) getContentProvider();
			} else if (adapterType == ITargetLocationHandler.class) {
				return (T) this;
			}
		} else if (adaptableObject instanceof IUWrapper) {
			if (adapterType == ILabelProvider.class) {
				return (T) getLabelProvider();
			} else if (adapterType == ITargetLocationHandler.class) {
				return (T) this;
			}
		}
		return null;
	}

	@Override
	public boolean canEdit(ITargetDefinition target, TreePath path) {
		Object segment = path.getLastSegment();
		return segment instanceof IUBundleContainer || segment instanceof IUWrapper;
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath path) {
		Object segment = path.getFirstSegment();
		if (segment instanceof IUBundleContainer) {
			return new EditBundleContainerWizard(target, (ITargetLocation) segment);
		}
		return null;
	}

	@Override
	public IStatus update(ITargetDefinition target, TreePath[] treePaths, IProgressMonitor monitor) {
		Set<IUBundleContainer> containers = new HashSet<>();
		Map<IUBundleContainer, Set<String>> wrappersMap = new HashMap<>();
		for (TreePath path : treePaths) {
			Object lastSegment = path.getLastSegment();
			if (lastSegment instanceof IUBundleContainer) {
				containers.add((IUBundleContainer) lastSegment);
			} else if (lastSegment instanceof IUWrapper wrapper) {
				wrappersMap.computeIfAbsent(wrapper.getParent(), k -> new HashSet<>()).add(wrapper.getIU().getId());
			}
		}
		boolean changed = false;
		SubMonitor subMonitor = SubMonitor.convert(monitor, (containers.size() + wrappersMap.size()) * 100);
		for (IUBundleContainer container : containers) {
			try {
				changed |= container.update(Collections.emptySet(), subMonitor.split(100));
			} catch (CoreException e) {
				return e.getStatus();
			}
		}
		for (Entry<IUBundleContainer, Set<String>> entry : wrappersMap.entrySet()) {
			SubMonitor split = subMonitor.split(100);
			IUBundleContainer container = entry.getKey();
			if (containers.contains(container)) {
				continue;
			}
			try {
				changed |= container.update(entry.getValue(), split);
			} catch (CoreException e) {
				return e.getStatus();
			}
		}
		return changed ? Status.OK_STATUS : STATUS_NO_CHANGE;
	}

	@Override
	public boolean canRemove(ITargetDefinition target, TreePath treePath) {
		boolean isValidRoot = treePath.getFirstSegment() instanceof IUBundleContainer;
		if (treePath.getSegmentCount() == 1) {
			return isValidRoot;
		}
		return isValidRoot && treePath.getLastSegment() instanceof IUWrapper;
	}

	@Override
	public boolean canUpdate(ITargetDefinition target, TreePath treePath) {
		Object lastSegment = treePath.getLastSegment();
		return lastSegment instanceof IUBundleContainer || lastSegment instanceof IUWrapper;
	}

	@Override
	public IStatus remove(ITargetDefinition target, TreePath[] treePaths) {
		boolean forceReload = false;
		for (TreePath treePath : treePaths) {
			Object segment = treePath.getLastSegment();
			if (segment instanceof IUBundleContainer) {
				// nothing to do but force reload the target
				forceReload = true;
			} else if (segment instanceof IUWrapper wrapper) {
				wrapper.getParent().removeInstallableUnit(wrapper.getIU());
			}
		}
		return forceReload ? STATUS_FORCE_RELOAD : Status.OK_STATUS;
	}

	@Override
	public IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
		// delete profile
		try {
			// TODO might want to merge forceCheckTarget into delete Profile?
			P2TargetUtils.forceCheckTarget(target);
			P2TargetUtils.deleteProfile(target.getHandle());
			return Status.OK_STATUS;
		} catch (CoreException e) {
			return e.getStatus();
		}
	}

	private ILabelProvider getLabelProvider() {
		if (fLabelProvider == null) {
			fLabelProvider = new StyledBundleLabelProvider(true, false);
		}
		return fLabelProvider;
	}

	private ITreeContentProvider getContentProvider() {
		if (fContentProvider == null) {
			fContentProvider = new IUContentProvider();
		}
		return fContentProvider;
	}

}
