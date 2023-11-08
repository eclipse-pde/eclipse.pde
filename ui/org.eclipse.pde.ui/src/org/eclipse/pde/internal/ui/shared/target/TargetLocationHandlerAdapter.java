/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.ui.shared.target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.ui.target.ITargetLocationHandler;

/**
 * Single Entry Point handler for the UI that delegates to adapter framework
 * when necessary
 *
 */
class TargetLocationHandlerAdapter implements ITargetLocationHandler {

	@Override
	public boolean canEdit(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getLastSegment();
		ITargetLocationHandler handler = Adapters.adapt(segment, ITargetLocationHandler.class);
		if (handler != null) {
			return handler.canEdit(target, treePath);
		}
		return false;
	}

	@Override
	public boolean canUpdate(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getLastSegment();
		ITargetLocationHandler handler = Adapters.adapt(segment, ITargetLocationHandler.class);
		if (handler != null) {
			return handler.canUpdate(target, treePath);
		}
		return false;
	}

	@Override
	public boolean canDisable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getLastSegment();
		ITargetLocationHandler handler = Adapters.adapt(segment, ITargetLocationHandler.class);
		if (handler != null) {
			return handler.canDisable(target, treePath);
		}
		return false;
	}

	@Override
	public boolean canEnable(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getLastSegment();
		ITargetLocationHandler handler = Adapters.adapt(segment, ITargetLocationHandler.class);
		if (handler != null) {
			return handler.canEnable(target, treePath);
		}
		return false;
	}

	@Override
	public boolean canRemove(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getLastSegment();
		if (segment instanceof ITargetLocation) {
			return true;
		}
		ITargetLocationHandler handler = Adapters.adapt(segment, ITargetLocationHandler.class);
		if (handler != null) {
			return handler.canRemove(target, treePath);
		}
		return false;
	}

	@Override
	public IStatus update(ITargetDefinition target, TreePath[] treePath, IProgressMonitor monitor) {
		Map<ITargetLocationHandler, List<TreePath>> handlerMap = computeHandlerMap(target, treePath,
				ITargetLocationHandler::canUpdate, null);
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, "update"); //$NON-NLS-1$
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100 * handlerMap.size());
		for (Entry<ITargetLocationHandler, List<TreePath>> entry : handlerMap.entrySet()) {
			status.add(entry.getKey().update(target, entry.getValue().toArray(TreePath[]::new), subMonitor.split(100)));
		}
		return status;
	}

	@Override
	public IStatus reload(ITargetDefinition target, ITargetLocation[] targetLocations, IProgressMonitor monitor) {
		Map<ITargetLocationHandler, List<ITargetLocation>> handlerMap = new HashMap<>();
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, "reload target"); //$NON-NLS-1$
		if (targetLocations != null && targetLocations.length > 0) {
			for (ITargetLocation targetLocation : targetLocations) {
				ITargetLocationHandler handler = Adapters.adapt(targetLocation, ITargetLocationHandler.class);
				if (handler != null) {
					handlerMap.computeIfAbsent(handler, h -> new ArrayList<>()).add(targetLocation);
				}
			}
		}
		SubMonitor subMonitor = SubMonitor.convert(monitor, handlerMap.size() * 100);
		for (Entry<ITargetLocationHandler, List<ITargetLocation>> entry : handlerMap.entrySet()) {
			status.add(entry.getKey().reload(target, entry.getValue().toArray(ITargetLocation[]::new),
					subMonitor.split(100)));
		}
		return status;
	}

	@Override
	public IStatus remove(ITargetDefinition target, TreePath[] treePath) {
		List<ITargetLocation> removedLocations = new ArrayList<>();
		Map<ITargetLocationHandler, List<TreePath>> handlerMap = computeHandlerMap(target, treePath,
				ITargetLocationHandler::canRemove, path -> {
					Object currentSelection = path.getLastSegment();
					if (currentSelection instanceof ITargetLocation) {
						// record all locations that are about to removed
						removedLocations.add((ITargetLocation) currentSelection);
					}
				});
		boolean forceReload = false;
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, "remove"); //$NON-NLS-1$
		for (Entry<ITargetLocationHandler, List<TreePath>> entry : handlerMap.entrySet()) {
			IStatus remove = entry.getKey().remove(target, entry.getValue().toArray(TreePath[]::new));
			forceReload |= remove.isOK() && remove.getCode() == ITargetLocationHandler.STATUS_FORCE_RELOAD;
			status.add(remove);
		}
		if (removedLocations.size() > 0) {
			ITargetLocation[] containers = target.getTargetLocations();
			if (containers != null && containers.length > 0) {
				List<ITargetLocation> updatedLocations = new ArrayList<>();
				for (ITargetLocation location : containers) {
					if (removedLocations.contains(location)) {
						continue;
					}
					updatedLocations.add(location);
				}
				if (updatedLocations.isEmpty()) {
					target.setTargetLocations(null);
				} else {
					target.setTargetLocations(updatedLocations.toArray(ITargetLocation[]::new));
				}
			}
		}
		return statusWithCode(status, ITargetLocationHandler.STATUS_FORCE_RELOAD, forceReload);
	}

	@Override
	public IStatus toggle(ITargetDefinition target, TreePath[] treePath) {
		boolean forceReload = false;
		MultiStatus status = new MultiStatus(PDECore.PLUGIN_ID, 0, "disable"); //$NON-NLS-1$
		Map<ITargetLocationHandler, List<TreePath>> handlerMap = computeHandlerMap(target, treePath,
				(h, t, p) -> h.canEnable(t, p) || h.canDisable(t, p), null);
		for (Entry<ITargetLocationHandler, List<TreePath>> entry : handlerMap.entrySet()) {
			TreePath[] handlerTreePaths = entry.getValue().toArray(TreePath[]::new);
			ITargetLocationHandler handler = entry.getKey();
			IStatus enable = handler.toggle(target, handlerTreePaths);
			forceReload |= enable.isOK() && enable.getCode() == ITargetLocationHandler.STATUS_FORCE_RELOAD;
			status.add(enable);
		}
		return statusWithCode(status, ITargetLocationHandler.STATUS_FORCE_RELOAD, forceReload);
	}

	@Override
	public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
		Object segment = treePath.getLastSegment();

		ITargetLocationHandler handler = Adapters.adapt(segment, ITargetLocationHandler.class);
		if (handler != null) {
			return handler.getEditWizard(target, treePath);
		}
		return null;
	}

	private static IStatus statusWithCode(MultiStatus status, int code, boolean useCode) {
		if (status.isOK() && useCode) {
			return new MultiStatus(PDECore.PLUGIN_ID, code, status.getChildren(),
					status.getMessage(), null);
		}
		return status;
	}

	private static Map<ITargetLocationHandler, List<TreePath>> computeHandlerMap(ITargetDefinition target,
			TreePath[] treePath, TriFunction<ITargetLocationHandler, ITargetDefinition, TreePath, Boolean> filter,
			Consumer<TreePath> interceptor) {
		Map<ITargetLocationHandler, List<TreePath>> handlerMap = new HashMap<>();
		for (TreePath path : treePath) {
			Object currentSelection = path.getLastSegment();
			ITargetLocationHandler handler = Adapters.adapt(currentSelection, ITargetLocationHandler.class);
			if (handler != null && filter.test(handler, target, path)) {
				handlerMap.computeIfAbsent(handler, h -> new ArrayList<>()).add(path);
			}
			if (interceptor != null) {
				interceptor.accept(path);
			}
		}
		return handlerMap;
	}

	static interface TriFunction<A, B, C, R> {
		R test(A a, B b, C c);
	}
}
