/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Anyware Technologies - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.views.target;

import java.util.ArrayList;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.pde.internal.ui.views.dependencies.DependenciesViewComparator;
import org.eclipse.pde.internal.ui.views.plugins.ImportActionGroup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.part.PageBookView;
import org.osgi.framework.Version;

public class StateViewPage extends Page implements IStateDeltaListener, IPluginModelListener {

	private IPropertyChangeListener fPropertyListener;
	private FilteredTree fFilteredTree = null;
	private TreeViewer fTreeViewer = null;
	private PageBookView fView;
	private Composite fComposite;
	private Action fOpenAction;
	private String DIALOG_SETTINGS = "targetStateView"; //$NON-NLS-1$

	private static final String HIDE_RESOLVED = "hideResolved"; //$NON-NLS-1$
	private static final String SHOW_NONLEAF = "hideNonLeaf"; //$NON-NLS-1$

	private ViewerFilter fHideResolvedFilter = new ViewerFilter() {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return ((element instanceof BundleDescription && !((BundleDescription) element).isResolved()) || parentElement instanceof BundleDescription && !((BundleDescription) parentElement).isResolved());
		}
	};

	private ViewerFilter fShowLeaves = new ViewerFilter() {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof BundleDescription) {
				return ((BundleDescription) element).getDependents().length == 0;
			}
			return true;
		}
	};

	class DependencyGroup {
		Object[] dependencies;

		public DependencyGroup(Object[] constraints) {
			dependencies = constraints;
		}

		public Object[] getChildren() {
			return dependencies;
		}

		public String toString() {
			return (dependencies[0] instanceof BundleSpecification) ? PDEUIMessages.StateViewPage_requiredBundles : PDEUIMessages.StateViewPage_importedPackages;
		}
	}

	class StateContentProvider extends DefaultContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof BundleDescription) {
				BundleDescription desc = (BundleDescription) parentElement;
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
				return ((DependencyGroup) parentElement).getChildren();
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
				return ((State) inputElement).getBundles();
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

	class StateLabelProvider extends StyledCellLabelProvider implements ILabelProvider {
		private PDELabelProvider fSharedProvider;

		public StateLabelProvider() {
			fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
			fSharedProvider.connect(this);
		}

		public void dispose() {
			fSharedProvider.disconnect(this);
			super.dispose();
		}

		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			StyledString styledString = new StyledString();
			if (element instanceof ImportPackageSpecification) {
				ImportPackageSpecification spec = (ImportPackageSpecification) element;
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
			} else if (element instanceof BundleDescription) {
				BundleDescription description = (BundleDescription) element;
				styledString.append(fSharedProvider.getObjectText(description));
				Version version = description.getVersion();
				// Bug 183417 - Bidi3.3: Elements' labels in the extensions page in the fragment manifest characters order is incorrect
				// Use the PDELabelProvider.formatVersion function to properly format the version for all languages including bidi
				styledString.append(' ').append(PDELabelProvider.formatVersion(version.toString())).toString();
				if (showLocation && description.getLocation() != null) {
					styledString.append(" - " + description.getLocation(), StyledString.DECORATIONS_STYLER); //$NON-NLS-1$
				}
			} else {
				styledString.append(element.toString());
			}
		}

		public Image getImage(Object element) {
			if (element instanceof DependencyGroup)
				element = ((DependencyGroup) element).getChildren()[0];
			if (element instanceof BundleSpecification)
				element = ((BundleSpecification) element).getSupplier();
			if (element instanceof BundleDescription) {
				int flags = ((BundleDescription) element).isResolved() ? 0 : SharedLabelProvider.F_ERROR;
				return (((BundleDescription) element).getHost() == null) ? fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags) : fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			}
			if (element instanceof ImportPackageSpecification)
				return JavaUI.getSharedImages().getImage(ISharedImages.IMG_OBJS_PACKAGE);
			if (element instanceof ResolverError) {
				if (((ResolverError) element).getType() == ResolverError.PLATFORM_FILTER)
					return fSharedProvider.get(PDEPluginImages.DESC_OPERATING_SYSTEM_OBJ);
				return fSharedProvider.getImage(element);
			}
			return null;
		}

		public String getText(Object element) {
			String result = element.toString();
			if (element instanceof ImportPackageSpecification) {
				ImportPackageSpecification spec = (ImportPackageSpecification) element;
				result = spec.getName();
			} else if (element instanceof BundleSpecification) {
				result = ((BundleSpecification) element).getSupplier().toString();
			} else if (element instanceof BundleDescription) {
				BundleDescription description = (BundleDescription) element;
				result = fSharedProvider.getObjectText(description);
			}
			return result;
		}
	}

	private boolean isJREPackage(ExportPackageDescription supplier) {
		// check for runtime's non-API directive.  This may change in the future
		return (((Integer) supplier.getDirective("x-equinox-ee")).intValue() > 0); //$NON-NLS-1$
	}

	public StateViewPage(PageBookView view) {
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

		fFilteredTree = new FilteredTree(fComposite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE, new PatternFilter(), true) {
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
		};

		fFilteredTree.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));

		fTreeViewer = fFilteredTree.getViewer();
		fTreeViewer.setContentProvider(new StateContentProvider());
		fTreeViewer.setLabelProvider(new StateLabelProvider());
		fTreeViewer.setComparator(DependenciesViewComparator.getViewerComparator());
		fTreeViewer.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				handleDoubleClick();
			}

		});

		if (getSettings().getBoolean(HIDE_RESOLVED))
			fTreeViewer.addFilter(fHideResolvedFilter);
		if (getSettings().getBoolean(SHOW_NONLEAF))
			fTreeViewer.addFilter(fShowLeaves);

		PDEPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(fPropertyListener);
		getSite().setSelectionProvider(fTreeViewer);
		PDECore.getDefault().getModelManager().addStateDeltaListener(this);
		setActive(true);
	}

	public Control getControl() {
		return fComposite;
	}

	public void setFocus() {
		if (fFilteredTree != null) {
			Control c = fFilteredTree.getFilterControl();
			if (c != null && !c.isFocusControl()) {
				c.setFocus();
			}
		}
	}

	protected void handleDoubleClick() {
		StructuredSelection selection = (StructuredSelection) fTreeViewer.getSelection();
		if (selection.size() == 1) {
			BundleDescription desc = getBundleDescription(selection.getFirstElement());
			if (desc != null)
				ManifestEditor.openPluginEditor(desc);
		}
	}

	private BundleDescription getBundleDescription(Object obj) {
		if (obj instanceof BundleSpecification)
			obj = ((BundleSpecification) obj).getSupplier();
		else if (obj instanceof ImportPackageSpecification)
			obj = ((ExportPackageDescription) ((ImportPackageSpecification) obj).getSupplier()).getSupplier();
		if (obj instanceof BundleDescription)
			return (BundleDescription) obj;
		return null;
	}

	protected void setActive(boolean active) {
		if (active) {
			State state = PDECore.getDefault().getModelManager().getState().getState();
			state.resolve(true);
			fTreeViewer.setInput(state);
			PDECore.getDefault().getModelManager().addPluginModelListener(this);
		} else {
			PDECore.getDefault().getModelManager().removePluginModelListener(this);
		}
	}

	public void makeContributions(IMenuManager menuManager, IToolBarManager toolBarManager, IStatusLineManager statusLineManager) {
		super.makeContributions(menuManager, toolBarManager, statusLineManager);
		Action filterResolved = new Action(PDEUIMessages.StateViewPage_showOnlyUnresolved_label, IAction.AS_CHECK_BOX) {
			public void run() {
				getSettings().put(HIDE_RESOLVED, isChecked());
				if (isChecked())
					fTreeViewer.addFilter(fHideResolvedFilter);
				else
					fTreeViewer.removeFilter(fHideResolvedFilter);
			}
		};
		Action filterLeaves = new Action(PDEUIMessages.StateViewPage_showLeaves, IAction.AS_CHECK_BOX) {
			public void run() {
				getSettings().put(SHOW_NONLEAF, isChecked());
				if (isChecked())
					fTreeViewer.addFilter(fShowLeaves);
				else
					fTreeViewer.removeFilter(fShowLeaves);
			}
		};

		filterResolved.setChecked(getSettings().getBoolean(HIDE_RESOLVED));
		filterLeaves.setChecked(getSettings().getBoolean(SHOW_NONLEAF));
		menuManager.add(filterResolved);
		menuManager.add(filterLeaves);

		hookContextMenu();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu"); //$NON-NLS-1$
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(fTreeViewer.getControl());
		fTreeViewer.getControl().setMenu(menu);

		getSite().registerContextMenu(fView.getSite().getId(), menuMgr, fTreeViewer);
	}

	private void fillContextMenu(IMenuManager menu) {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		BundleDescription desc = getBundleDescription(selection.getFirstElement());
		if (desc != null) {
			if (fOpenAction == null) {
				fOpenAction = new Action(PDEUIMessages.StateViewPage_openItem) {
					public void run() {
						handleDoubleClick();
					}
				};
			}
			menu.add(fOpenAction);
			if (ImportActionGroup.canImport(selection)) {
				ImportActionGroup actionGroup = new ImportActionGroup();
				actionGroup.setContext(new ActionContext(selection));
				actionGroup.fillContextMenu(menu);
				menu.add(new Separator());
			}
			menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
	}

	public void dispose() {
		PDECore.getDefault().getModelManager().removeStateDeltaListener(this);
		setActive(false);
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
						if (type == BundleDelta.REMOVED || type == BundleDelta.RESOLVED || type == BundleDelta.ADDED || type == BundleDelta.UNRESOLVED) {
							fTreeViewer.refresh();
							break;
						}
					}
				}
			}
		});
	}

	public void stateChanged(final State newState) {
		if (!fView.getCurrentPage().equals(this) || fTreeViewer == null || fTreeViewer.getTree().isDisposed())
			// if this page is not active, then wait until we call refresh on next activation
			return;
		fTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {
			public void run() {
				fTreeViewer.setInput(newState);
			}
		});
	}

	private IDialogSettings getSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		IDialogSettings section = master.getSection(DIALOG_SETTINGS);
		if (section == null) {
			section = master.addNewSection(DIALOG_SETTINGS);
		}
		return section;
	}

	public void modelsChanged(PluginModelDelta delta) {
		if (fTreeViewer == null || fTreeViewer.getTree().isDisposed())
			return;
		if (delta.getAddedEntries().length > 0 || delta.getChangedEntries().length > 0 || delta.getRemovedEntries().length > 0)
			fTreeViewer.getTree().getDisplay().asyncExec(new Runnable() {
				public void run() {
					fTreeViewer.refresh();
				}
			});
	}

}
