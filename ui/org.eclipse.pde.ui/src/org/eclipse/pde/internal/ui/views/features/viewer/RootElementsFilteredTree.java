/*******************************************************************************
 * Copyright (c) 2019, 2026 Ed Scadding and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ed Scadding <edscadding@secondfiddle.org.uk> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.features.viewer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

public class RootElementsFilteredTree extends FilteredTree {

	public RootElementsFilteredTree(Composite parent, int treeStyle) {
		super(parent, treeStyle, new RootElementsPatternFilter(), true, true);
	}

	/**
	 * Avoid the expansion stage of the refresh by using a monitor in the
	 * "canceled" state.
	 */
	@Override
	protected WorkbenchJob doCreateRefreshJob() {
		final WorkbenchJob realJob = super.doCreateRefreshJob();
		return new WorkbenchJob(realJob.getName()) {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IProgressMonitor cancelledMonitor = new NullProgressMonitor();
				cancelledMonitor.setCanceled(true);
				realJob.runInUIThread(cancelledMonitor);
				return Status.OK_STATUS;
			}
		};
	}

	private static class RootElementsPatternFilter extends PatternFilter {

		private Object currentParent;

		@Override
		public boolean isElementVisible(Viewer viewer, Object element) {
			if (viewer.getInput() == currentParent) {
				return isLeafMatch(viewer, element);
			}

			return true;
		}

		@Override
		public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
			currentParent = parent;
			return super.filter(viewer, parent, elements);
		}

	}

}
