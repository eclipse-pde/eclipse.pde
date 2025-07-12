/*******************************************************************************
 *  Copyright (c) 2000, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;
import org.osgi.framework.Constants;

public class DependenciesViewTreePage extends DependenciesViewPage {
	class CollapseAllAction extends Action {
		public CollapseAllAction() {
			super();
			setText(PDEUIMessages.DependenciesViewTreePage_CollapseAllAction_label);
			setDescription(PDEUIMessages.DependenciesViewTreePage_CollapseAllAction_description);
			setToolTipText(PDEUIMessages.DependenciesViewTreePage_CollapseAllAction_tooltip);
			setImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
			setDisabledImageDescriptor(PDEPluginImages.DESC_COLLAPSE_ALL);
		}

		@Override
		public void run() {
			super.run();
			fTreeViewer.collapseAll();
		}
	}

	static class OptionalFilter extends ViewerFilter {

		@Override
		public boolean select(Viewer v, Object parent, Object element) {
			if (element instanceof BundleSpecification) {
				return !((BundleSpecification) element).isOptional();
			} else if (element instanceof ImportPackageSpecification) {
				return !Constants.RESOLUTION_OPTIONAL.equals(((ImportPackageSpecification) element).getDirective(Constants.RESOLUTION_DIRECTIVE));
			}
			return true;
		}
	}

	TreeViewer fTreeViewer;
	private FilteredTree fFilteredTreeViewer;
	private final OptionalFilter fHideOptionalFilter = new OptionalFilter();

	public DependenciesViewTreePage(DependenciesView view, ITreeContentProvider contentProvider) {
		super(view, contentProvider);
	}

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		fFilteredTreeViewer = new DependencyFilteredTree(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		fTreeViewer = fFilteredTreeViewer.getViewer();
		fTreeViewer.setContentProvider(fContentProvider);
		final DependenciesLabelProvider labelProvider = new DependenciesLabelProvider(true);
		fTreeViewer.setLabelProvider(labelProvider);
		fTreeViewer.getControl().addDisposeListener(e -> labelProvider.dispose());
		fTreeViewer.setAutoExpandLevel(2);

		return fTreeViewer;
	}

	@Override
	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		if (toolBarManager.find(DependenciesView.TREE_ACTION_GROUP) != null) {
			toolBarManager.prependToGroup(DependenciesView.TREE_ACTION_GROUP, new CollapseAllAction());
		} else {
			toolBarManager.add(new CollapseAllAction());
		}
	}

	@Override
	protected void handleShowOptional(boolean isChecked, boolean refreshIfNecessary) {
		if (isChecked) {
			fTreeViewer.removeFilter(fHideOptionalFilter);
		} else {
			fTreeViewer.addFilter(fHideOptionalFilter);
		// filter automatically refreshes tree, therefore can ignore refreshIfNecessary
		}
	}

	@Override
	protected boolean isShowingOptional() {
		ViewerFilter[] filters = fTreeViewer.getFilters();
		for (ViewerFilter filter : filters) {
			if (filter.equals(fHideOptionalFilter)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Control getControl() {
		return fFilteredTreeViewer;
	}

	private static class DependencyFilteredTree extends FilteredTree {
		/**
		 * Maximum time spent expanding the tree after the filter text has been
		 * updated (this is only used if we were able to at least expand the
		 * visible nodes)
		 */
		private static final long SOFT_MAX_EXPAND_TIME = 200;

		protected DependencyFilteredTree(Composite parent, int style) {
			super(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, new DependencyPatternFilter(), true, true);
		}

		@Override
		protected WorkbenchJob doCreateRefreshJob() {
			return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (treeViewer.getControl().isDisposed()) {
						return Status.CANCEL_STATUS;
					}

					String text = getFilterString();
					if (text == null) {
						return Status.OK_STATUS;
					}

					boolean initial = initialText != null && initialText.equals(text);
					getPatternFilter().setPattern(initial ? null : text);

					try {
						treeComposite.setRedraw(false);
						treeViewer.collapseAll();
						treeViewer.refresh(true);

						if (text.length() > 0 && !initial) {
							Tree tree = getViewer().getTree();
							TreeItem[] items = tree.getItems();
							int numVisibleItems = tree.getBounds().height / tree.getItemHeight();
							long stopTime = SOFT_MAX_EXPAND_TIME + System.currentTimeMillis();

							if (items.length > 0 && recursiveExpand(items, monitor, stopTime, numVisibleItems)) {
								return Status.CANCEL_STATUS;
							}
						} else {
							treeViewer.expandToLevel(treeViewer.getAutoExpandLevel());
						}
					} finally {
						treeComposite.setRedraw(true);
					}
					return Status.OK_STATUS;
				}

				/**
				 * Returns {@code true} if the job should be canceled (because
				 * of timeout or actual cancellation).
				 *
				 * @return {@code true} if canceled
				 */
				private boolean recursiveExpand(TreeItem[] items, IProgressMonitor monitor, long cancelTime,
						int numItemsLeft) {
					boolean canceled = false;
					for (TreeItem item : items) {
						if (canceled) {
							break;
						}
						boolean visible = numItemsLeft-- >= 0;
						if (monitor.isCanceled() || (!visible && System.currentTimeMillis() > cancelTime)) {
							canceled = true;
						} else {
							Object itemData = item.getData();
							if (itemData != null) {
								if (!item.getExpanded()) {
									treeViewer.setExpandedState(itemData, true);
								}
								TreeItem[] children = item.getItems();
								if (items.length > 0) {
									canceled = recursiveExpand(children, monitor, cancelTime, numItemsLeft);
								}
							}
						}
					}
					return canceled;
				}
			};
		}
	}

	private static class DependencyPatternFilter extends PatternFilter {
		private Set<Object> visited = new HashSet<>();

		@Override
		public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
			try {
				return super.filter(viewer, parent, elements);
			} finally {
				visited.clear();
			}
		}

		@Override
		protected boolean isParentMatch(Viewer viewer, Object element) {
			// Cycle detection
			if (!visited.add(element)) {
				return false;
			}
			return super.isParentMatch(viewer, element);
		}
	}
}
