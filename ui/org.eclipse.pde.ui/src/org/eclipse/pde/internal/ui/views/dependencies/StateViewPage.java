/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.dependencies;

import java.util.ArrayList;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.service.resolver.BundleDelta;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.pde.internal.core.IStateDeltaListener;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.Page;
import org.osgi.framework.Version;

public class StateViewPage extends Page implements IStateDeltaListener {
	
	private IPropertyChangeListener fPropertyListener;
	private FilteredTree fFilteredTree = null;
	private TreeViewer fTreeViewer = null;
	private DependenciesView fView;
	private Composite fComposite;
	private ViewerFilter fHideResolvedFilter = new ViewerFilter() {

		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			return ((element instanceof BundleDescription && !((BundleDescription)element).isResolved()) ||
				parentElement instanceof BundleDescription && !((BundleDescription)parentElement).isResolved());
		}
		
	};
	
	class DependencyGroup {
		Object [] dependencies;
		
		public DependencyGroup(Object[] constraints) {
			dependencies = constraints;
		}
		
		public Object[] getChildren() {
			return dependencies;
		}
		
		public String toString() {
			return (dependencies[0] instanceof BundleSpecification) ? PDEUIMessages.StateViewPage_requiredBundles 
					: PDEUIMessages.StateViewPage_importedPackages;
		}
	}
	
	class StateContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof BundleDescription) {
				BundleDescription desc = (BundleDescription)parentElement;
				if (desc.isResolved()) {
					Object[] required = getResolvedDependencies(desc.getRequiredBundles());
					Object[] imported = getResolvedDependencies(desc.getImportPackages());
					ArrayList list = new ArrayList(2);
					if (required.length > 0)
						list.add(new DependencyGroup(required));
					if (imported.length > 0)
						list.add(new DependencyGroup(imported));
					return list.toArray();
				}
				return desc.getContainingState().getResolverErrors(desc);
			} else if (parentElement instanceof DependencyGroup) {
				return ((DependencyGroup)parentElement).getChildren();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof State)
				return ((State)inputElement).getBundles();
			return new Object[0];
		}
		
		private Object[] getResolvedDependencies(VersionConstraint[] constraints) {
			ArrayList list = new ArrayList(constraints.length);
			for (int i = 0; i < constraints.length; i++) 
				if (constraints[i].isResolved())
					list.add(constraints[i]);
			return list.toArray();
		}
		
	}
	
	class StateLabelProvider extends LabelProvider {
		private PDELabelProvider fSharedProvider;
		
		public StateLabelProvider() {
			fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
			fSharedProvider.connect(this);
		}
		
		public void dispose() {
			fSharedProvider.disconnect(this);
			super.dispose();
		}
		
		public Image getImage(Object element) {
			if (element instanceof DependencyGroup) 
				element = ((DependencyGroup)element).getChildren()[0];
			if (element instanceof BundleSpecification) 
				element = ((BundleSpecification)element).getSupplier();
			if (element instanceof BundleDescription) {
				int flags = ((BundleDescription)element).isResolved() ? 0 : SharedLabelProvider.F_ERROR;
				return (((BundleDescription)element).getHost() == null) ? 
						fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags) :
						fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			}
			if (element instanceof ImportPackageSpecification)
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
			if (element instanceof ResolverError) {
				if (((ResolverError)element).getType() == ResolverError.PLATFORM_FILTER)
					return fSharedProvider.get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ);
				return fSharedProvider.getImage(element);
			}
			return null;
		}

		public String getText(Object element) {
			StringBuffer buffer = new StringBuffer();
			if (element instanceof ImportPackageSpecification) {
				ImportPackageSpecification spec = (ImportPackageSpecification) element;
				buffer.append(spec.getName()).append(PDEUIMessages.StateViewPage_suppliedBy);
				element = ((ExportPackageDescription)spec.getSupplier()).getSupplier();
			}
			if (element instanceof BundleSpecification)
				element = ((BundleSpecification)element).getSupplier();
			if (element instanceof BundleDescription) {
				buffer.append(fSharedProvider.getObjectText((BundleDescription)element));
				Version version = ((BundleDescription)element).getVersion();
				return buffer.append(" (").append(version).append(")").toString(); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return element.toString();
		}
		
	}
	
	public StateViewPage(DependenciesView view) {
		fView = view;
		fPropertyListener = new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				String property = event.getProperty();
				if (property.equals(IPreferenceConstants.PROP_SHOW_OBJECTS)) {
					fTreeViewer.refresh();
				}
			}
		};
	}

	public void createControl(Composite parent) {
		fComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		fComposite.setLayout(layout);
		fComposite.setLayoutData(new GridData(GridData.FILL_BOTH));	
		
		fFilteredTree = new FilteredTree(fComposite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, new PatternFilter());
		fFilteredTree.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		
		// need to give filter Textbox some space from the border
		Layout filterLayout = fFilteredTree.getFilterControl().getParent().getLayout();
		if (filterLayout instanceof GridLayout) {
			((GridLayout)filterLayout).marginHeight = 4;
			((GridLayout)filterLayout).marginWidth = 3;
		}
		
		fTreeViewer = fFilteredTree.getViewer();
		fTreeViewer.setContentProvider(new StateContentProvider());
		fTreeViewer.setLabelProvider(new StateLabelProvider());
		fTreeViewer.setComparator(DependenciesViewComparator.getViewerComparator());
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}
			
		});
		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(
				fPropertyListener);
		getSite().setSelectionProvider(fTreeViewer);
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
	}

	public Control getControl() {
		return fComposite;
	}

	public void setFocus() {
		if (fFilteredTree != null) {
			Control c = fFilteredTree.getFilterControl();
			if (!c.isFocusControl()) {
				c.setFocus();
			}
		}
	}
	
	protected void handleDoubleClick() {
		StructuredSelection selection = (StructuredSelection)fTreeViewer.getSelection();
		if (selection.size() == 1) {
			Object obj = selection.getFirstElement();
			if (obj instanceof BundleSpecification)
				obj = ((BundleSpecification)obj).getSupplier();
			else if (obj instanceof ImportPackageSpecification)
				obj = ((ExportPackageDescription)((ImportPackageSpecification)obj).getSupplier()).getSupplier();
			
			if (obj instanceof BundleDescription)
				ManifestEditor.openPluginEditor(((BundleDescription)obj).getSymbolicName());
		}
	}
	
	protected void setActive(boolean active) {
		if (active) {
			fView.updateTitle(PDEUIMessages.StateViewPage_title);
			State state = PDECore.getDefault().getModelManager().getState().getState();
			state.resolve(true);
			fTreeViewer.setInput(state);
		}
	}
	
	public void makeContributions(IMenuManager menuManager,
			IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		getSite().getActionBars().getMenuManager().add(
				new Action(PDEUIMessages.StateViewPage_showOnlyUnresolved_label, IAction.AS_CHECK_BOX) {
					public void run() {
						if (isChecked())
							fTreeViewer.addFilter(fHideResolvedFilter);
						else
							fTreeViewer.removeFilter(fHideResolvedFilter);
					}
				}
		);
	}
	
	public void dispose() {
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
		super.dispose();
	}

	public void stateResolved(final StateDelta delta) {
		if (!fView.getCurrentPage().equals(this) || fTreeViewer == null || fTreeViewer.getTree().isDisposed())
			// if this page is not active, then wait until we call refresh on next activation
			return;
		fTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (delta == null) {	
					fTreeViewer.refresh();
				} else {
					BundleDelta[] deltas = delta.getChanges();
					for (int i = 0; i < deltas.length; i++) {
						int type = deltas[i].getType();
						if (type == BundleDelta.REMOVED || type == BundleDelta.RESOLVED || type == BundleDelta.ADDED) {
							fTreeViewer.refresh();
							break;
						}
					}
				}
			}
		});
	}
	
	public void stateChanged(State newState) {
		if (!fView.getCurrentPage().equals(this) || fTreeViewer == null || fTreeViewer.getTree().isDisposed())
			// if this page is not active, then wait until we call refresh on next activation
			return;
		fTreeViewer.setInput(newState);
	}

}
