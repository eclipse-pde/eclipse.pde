/*******************************************************************************
 * Copyright (c) 2009, 2025 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Anyware Technologies - ongoing enhancements
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *     Christoph Läubrich - extracted from {@link StateViewPage}
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.target;

import java.util.ArrayList;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.views.dependencies.DependenciesViewComparator;
import org.eclipse.pde.internal.ui.views.target.StateViewPage.DependencyGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.osgi.framework.Version;

public class StateTree extends FilteredTree {

	@SuppressWarnings("deprecation")
	public StateTree(Composite parent) {
		super(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE, new PatternFilter(), true);
		TreeViewer fTreeViewer = getViewer();
		fTreeViewer.setContentProvider(new StateContentProvider());
		fTreeViewer.setLabelProvider(new StateLabelProvider());
		fTreeViewer.setComparator(DependenciesViewComparator.getViewerComparator());
		fTreeViewer.addDoubleClickListener(event -> handleDoubleClick());
	}

	@Override
	protected void createControl(Composite parent, int treeStyle) {
		super.createControl(parent, treeStyle);

		// add 2px margin around filter text

		FormLayout layout = new FormLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
		if (showFilterControls) {
			FormData filterData = new FormData();
			filterData.top = new FormAttachment(0, 2);
			filterData.left = new FormAttachment(0, 2);
			filterData.right = new FormAttachment(100, -2);
			filterComposite.setLayoutData(filterData);
			data.top = new FormAttachment(filterComposite, 2);
		} else {
			data.top = new FormAttachment(0, 0);
		}
		treeComposite.setLayoutData(data);
	}

	private static class StateContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof BundleDescription desc) {
				if (desc.isResolved()) {
					Object[] required = getResolvedDependencies(desc.getRequiredBundles());
					Object[] imported = getResolvedDependencies(desc.getImportPackages());
					ArrayList<DependencyGroup> list = new ArrayList<>(2);
					if (required.length > 0) {
						list.add(new DependencyGroup(required));
					}
					if (imported.length > 0) {
						list.add(new DependencyGroup(imported));
					}
					return list.toArray();
				}
				return desc.getContainingState().getResolverErrors(desc);
			} else if (parentElement instanceof DependencyGroup) {
				return ((DependencyGroup) parentElement).getChildren();
			}
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof State) {
				return ((State) inputElement).getBundles();
			}
			return new Object[0];
		}

		private Object[] getResolvedDependencies(VersionConstraint[] constraints) {
			ArrayList<VersionConstraint> list = new ArrayList<>(constraints.length);
			for (VersionConstraint constraint : constraints) {
				if (constraint.isResolved()) {
					list.add(constraint);
				}
			}
			return list.toArray();
		}

	}

	private static class StateLabelProvider extends StyledCellLabelProvider implements ILabelProvider {
		private final PDELabelProvider fSharedProvider;

		public StateLabelProvider() {
			fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
			fSharedProvider.connect(this);
		}

		@Override
		public void dispose() {
			fSharedProvider.disconnect(this);
			super.dispose();
		}

		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			StyledString styledString = new StyledString();
			if (element instanceof ImportPackageSpecification spec) {
				styledString.append(spec.getName());
				ExportPackageDescription supplier = (ExportPackageDescription) spec.getSupplier();
				if (isJREPackage(supplier)) {
					styledString.append(PDEUIMessages.StateViewPage_suppliedByJRE);
				} else {
					styledString.append(PDEUIMessages.StateViewPage_suppliedBy);
					getElementString(supplier.getSupplier(), styledString, false);
				}
			} else {
				getElementString(element, styledString, true);
			}

			cell.setText(styledString.toString());
			cell.setStyleRanges(styledString.getStyleRanges());
			cell.setImage(getImage(element));
			super.update(cell);
		}

		private void getElementString(Object element, StyledString styledString, boolean showLocation) {
			if (element instanceof BundleSpecification) {
				styledString.append(((BundleSpecification) element).getSupplier().toString());
			} else if (element instanceof BundleDescription description) {
				styledString.append(fSharedProvider.getObjectText(description));
				Version version = description.getVersion();
				// Bug 183417 - Bidi3.3: Elements' labels in the extensions page
				// in the fragment manifest characters order is incorrect
				// Use the PDELabelProvider.formatVersion function to properly
				// format the version for all languages including bidi
				styledString.append(' ').append(PDELabelProvider.formatVersion(version.toString())).toString();
				if (showLocation && description.getLocation() != null) {
					styledString.append(" - " + description.getLocation(), StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
				}
			} else {
				styledString.append(element.toString());
			}
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof DependencyGroup) {
				element = ((DependencyGroup) element).getChildren()[0];
			}
			if (element instanceof BundleSpecification) {
				element = ((BundleSpecification) element).getSupplier();
			}
			if (element instanceof BundleDescription) {
				int flags = ((BundleDescription) element).isResolved() ? 0 : SharedLabelProvider.F_ERROR;
				return (((BundleDescription) element).getHost() == null)
						? fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags)
						: fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			}
			if (element instanceof ImportPackageSpecification) {
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
			}
			if (element instanceof ResolverError) {
				if (((ResolverError) element).getType() == ResolverError.PLATFORM_FILTER) {
					return fSharedProvider.get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ);
				}
				return fSharedProvider.getImage(element);
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			String result = element.toString();
			if (element instanceof ImportPackageSpecification spec) {
				result = spec.getName();
			} else if (element instanceof BundleSpecification) {
				result = ((BundleSpecification) element).getSupplier().toString();
			} else if (element instanceof BundleDescription description) {
				result = fSharedProvider.getObjectText(description);
			}
			return result;
		}
	}

	private static boolean isJREPackage(ExportPackageDescription supplier) {
		// check for runtime's non-API directive. This may change in the future
		return (((Integer) supplier.getDirective("x-equinox-ee")).intValue() > 0); //$NON-NLS-1$
	}

	protected void handleDoubleClick() {
		IStructuredSelection selection = getViewer().getStructuredSelection();
		if (selection.size() == 1) {
			BundleDescription desc = getBundleDescription();
			if (desc != null) {
				ManifestEditor.open(desc, false);
			}
		}
	}

	public BundleDescription getBundleDescription() {
		IStructuredSelection selection = getViewer().getStructuredSelection();
		if (selection.size() == 1) {
			Object obj = selection.getFirstElement();
			if (obj instanceof BundleSpecification) {
				obj = ((BundleSpecification) obj).getSupplier();
			} else if (obj instanceof ImportPackageSpecification) {
				obj = ((ImportPackageSpecification) obj).getSupplier().getSupplier();
			}
			if (obj instanceof BundleDescription) {
				return (BundleDescription) obj;
			}
		}
		return null;
	}

	public void setInput(State state) {
		TreeViewer viewer = getViewer();
		if (viewer.getControl().isDisposed()) {
			return;
		}
		viewer.setInput(state);
	}

}
