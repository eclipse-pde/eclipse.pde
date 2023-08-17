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

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.ui.target.ITargetLocationEditor;
import org.eclipse.pde.ui.target.ITargetLocationHandler;
import org.eclipse.pde.ui.target.ITargetLocationUpdater;

@SuppressWarnings("deprecation")
public class LegacyAdapterFactory implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof ITargetLocation location) {
			if (adapterType == ITargetLocationHandler.class) {
				LegacyProxy proxy = new LegacyProxy(location);
				if (proxy.editor != null || proxy.updater != null) {
					return adapterType.cast(proxy);
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class<?>[] { ITargetLocationHandler.class };
	}

	private static class LegacyProxy implements ITargetLocationHandler {

		private final ITargetLocation location;
		private final ITargetLocationEditor editor;
		private final ITargetLocationUpdater updater;

		public LegacyProxy(ITargetLocation location) {
			this.location = location;
			editor = Adapters.adapt(location, ITargetLocationEditor.class);
			updater = Adapters.adapt(location, ITargetLocationUpdater.class);
		}

		@Override
		public boolean canEdit(ITargetDefinition target, TreePath treePath) {
			return editor != null && editor.canEdit(target, location);
		}

		@Override
		public IWizard getEditWizard(ITargetDefinition target, TreePath treePath) {
			if (editor != null) {
				return editor.getEditWizard(target, location);
			}
			return null;
		}

		@Override
		public boolean canUpdate(ITargetDefinition target, TreePath treePath) {
			return updater != null && updater.canUpdate(target, location);
		}

		@Override
		public IStatus update(ITargetDefinition target, TreePath[] treePath, IProgressMonitor monitor) {
			if (updater != null) {
				return updater.update(target, location, monitor);
			}
			return Status.CANCEL_STATUS;
		}

	}
}
