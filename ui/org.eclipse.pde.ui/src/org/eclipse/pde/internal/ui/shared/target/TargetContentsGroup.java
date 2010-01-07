/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import com.ibm.icu.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.equinox.internal.provisional.p2.metadata.MetadataFactory.InstallableUnitDescription;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.wizards.target.TargetDefinitionContentPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * UI Part that displays all of the bundle contents of a target.  The bundles can be
 * excluded by unchecking them.  There are a variety of options to change the tree's
 * format.
 * 
 * @see TargetEditor
 * @see TargetDefinitionContentPage
 * @see ITargetDefinition
 * @see IResolvedBundle
 */
public class TargetContentsGroup {

	private FilteredCheckboxTree fFilteredTree;
	private ContainerCheckedTreeViewer fTree;
	private MenuManager fMenuManager;
	private Button fSelectButton;
	private Button fDeselectButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
	private Label fShowLabel;
	private Button fShowSourceButton;
	private Button fShowPluginsButton;
	private Label fCountLabel;
	private Label fGroupLabel;
	private Combo fGroupCombo;
	private ComboPart fGroupComboPart;

	private ViewerFilter fSourceFilter;
	private ViewerFilter fPluginFilter;

	private ITargetDefinition fTargetDefinition;

	private int fItemCount;

	private int fGrouping;
	private static final int GROUP_BY_NONE = 0;
//	private static final int GROUP_BY_FILE_LOC = 1;
//	private static final int GROUP_BY_CONTAINER = 2;

	private ListenerList fChangeListeners = new ListenerList();

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 * @return generated instance of the table part
	 */
	public static TargetContentsGroup createInForm(Composite parent, FormToolkit toolkit) {
		TargetContentsGroup contentTable = new TargetContentsGroup();
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param reporter reporter implementation that will handle resolving and changes to the containers
	 * @return generated instance of the table part
	 */
	public static TargetContentsGroup createInDialog(Composite parent) {
		TargetContentsGroup contentTable = new TargetContentsGroup();
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	private TargetContentsGroup() {
		fGrouping = GROUP_BY_NONE;
	}

	private void createFormContents(Composite parent, FormToolkit toolkit) {
		Composite treeComp = null;

		treeComp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 0;
		treeComp.setLayout(layout);
		treeComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		createTreeViewer(treeComp, toolkit);
		createButtons(treeComp, toolkit);

		fCountLabel = toolkit.createLabel(treeComp, ""); //$NON-NLS-1$
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fCountLabel.setLayoutData(data);

		updateButtons();
		initializeFilters();
	}

	private void createDialogContents(Composite parent) {
		Composite treeComp = null;

		treeComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);

		createTreeViewer(treeComp, null);
		createButtons(treeComp, null);

		fCountLabel = SWTFactory.createLabel(treeComp, "", 2); //$NON-NLS-1$

		updateButtons();
		initializeFilters();
	}

	/**
	 * Adds a listener to the set of listeners that will be notified when the bundle containers
	 * are modified.  This method has no effect if the listener has already been added. 
	 * 
	 * @param listener target changed listener to add
	 */
	public void addTargetChangedListener(ITargetChangedListener listener) {
		fChangeListeners.add(listener);
	}

	private void createTreeViewer(Composite parent, FormToolkit toolkit) {
		TreeContentProvider contentProvider = new TreeContentProvider();
		fFilteredTree = new FilteredCheckboxTree(parent, contentProvider, toolkit);
		fFilteredTree.getPatternFilter().setIncludeLeadingWildcard(true);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 300;
		fFilteredTree.setLayoutData(data);

		fTree = fFilteredTree.getCheckboxTreeViewer();
		fTree.setUseHashlookup(true);
		fTree.setContentProvider(contentProvider);
		fTree.setLabelProvider(new StyledBundleLabelProvider(true, false));
		fTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				if (!event.getSelection().isEmpty()) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					fFilteredTree.setChecked(selection.getFirstElement(), !fTree.getChecked(selection.getFirstElement()));
					saveCheckState();
				}
			}
		});
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				saveCheckState();
			}
		});
		fTree.setSorter(new ViewerSorter() {
//			public int compare(Viewer viewer, Object e1, Object e2) {
//				if (e1 instanceof IInstallableUnit && e2 instanceof IInstallableUnit) {
//					// Put non bundle IUs ahead of bundle IUs
//					IProvidedCapability[] provided = ((IInstallableUnit) e1).getProvidedCapabilities();
//					boolean isBundle = false;
//					for (int j = 0; j < provided.length; j++) {
//						if (provided[j].getNamespace().equals(P2Utils.NAMESPACE_ECLIPSE_TYPE)) {
//							if (provided[j].getName().equals(P2Utils.TYPE_ECLIPSE_SOURCE)) {
//								isBundle = true;
//								break;
//							}
//						}
//						if (provided[j].getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_BUNDLE)) {
//							isBundle = true;
//							break;
//						}
//						if (provided[j].getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_FRAGMENT)) {
//							isBundle = true;
//							break;
//						}
//					}
//
//					provided = ((IInstallableUnit) e2).getProvidedCapabilities();
//					boolean isBundle2 = false;
//					for (int j = 0; j < provided.length; j++) {
//						if (provided[j].getNamespace().equals(P2Utils.NAMESPACE_ECLIPSE_TYPE)) {
//							if (provided[j].getName().equals(P2Utils.TYPE_ECLIPSE_SOURCE)) {
//								isBundle2 = true;
//								break;
//							}
//						}
//						if (provided[j].getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_BUNDLE)) {
//							isBundle2 = true;
//							break;
//						}
//						if (provided[j].getNamespace().equals(P2Utils.CAPABILITY_NS_OSGI_FRAGMENT)) {
//							isBundle2 = true;
//							break;
//						}
//					}
//
//					if (!isBundle && isBundle2) {
//						return -1;
//					}
//
//					if (isBundle && isBundle2) {
//						return 1;
//					}
//
//					return super.compare(viewer, ((IInstallableUnit) e1).getId(), ((IInstallableUnit) e2).getId());
//				}
//				return super.compare(viewer, e1, e2);
//			}
		});
		fMenuManager = new MenuManager();
		fMenuManager.add(new Action(Messages.TargetContentsGroup_collapseAll, PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_COLLAPSEALL)) {
			public void run() {
				// TODO Menu not appearing
				fTree.collapseAll();
			}
		});
		Menu contextMenu = fMenuManager.createContextMenu(fTree.getControl());
		fTree.getControl().setMenu(contextMenu);
	}

	public void dispose() {
		// TODO Need to dispose properly
		if (fMenuManager != null) {
			fMenuManager.dispose();
		}

	}

	private void createButtons(Composite parent, FormToolkit toolkit) {
		if (toolkit != null) {
			Composite buttonComp = toolkit.createComposite(parent);
			GridLayout layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			buttonComp.setLayout(layout);
			buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

			fSelectButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_0, SWT.PUSH);
			fSelectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_1, SWT.PUSH);
			fDeselectButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label emptySpace = new Label(buttonComp, SWT.NONE);
			GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectAllButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_2, SWT.PUSH);
			fSelectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			fDeselectAllButton = toolkit.createButton(buttonComp, Messages.IncludedBundlesTree_3, SWT.PUSH);
			fDeselectAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Composite filterComp = toolkit.createComposite(buttonComp);
			layout = new GridLayout();
			layout.marginWidth = layout.marginHeight = 0;
			filterComp.setLayout(layout);
			filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

			fShowLabel = toolkit.createLabel(filterComp, Messages.BundleContainerTable_9);

			fShowPluginsButton = toolkit.createButton(filterComp, Messages.BundleContainerTable_14, SWT.CHECK);
			fShowPluginsButton.setSelection(true);
			fShowSourceButton = toolkit.createButton(filterComp, Messages.BundleContainerTable_15, SWT.CHECK);
			fShowSourceButton.setSelection(true);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fGroupLabel = toolkit.createLabel(filterComp, Messages.TargetContentsGroup_0);

			fGroupComboPart = new ComboPart();
			fGroupComboPart.createControl(filterComp, toolkit, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent = 10;
			fGroupComboPart.getControl().setLayoutData(gd);
			fGroupComboPart.setItems(new String[] {Messages.TargetContentsGroup_1, Messages.TargetContentsGroup_2, Messages.TargetContentsGroup_3});
			fGroupComboPart.setVisibleItemCount(30);
			fGroupComboPart.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleGroupChange();
				}
			});
			fGroupComboPart.select(0);

		} else {
			Composite buttonComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL, 0, 0);
			fSelectButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_0, null);
			fDeselectButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_1, null);

			Label emptySpace = new Label(buttonComp, SWT.NONE);
			GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fSelectAllButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_2, null);
			fDeselectAllButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_3, null);

			Composite filterComp = SWTFactory.createComposite(buttonComp, 1, 1, SWT.NONE, 0, 0);
			filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

			fShowLabel = SWTFactory.createLabel(filterComp, Messages.BundleContainerTable_9, 1);

			fShowPluginsButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_14, null, true, 1);
			fShowSourceButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_15, null, true, 1);

			emptySpace = new Label(filterComp, SWT.NONE);
			gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
			gd.widthHint = gd.heightHint = 5;
			emptySpace.setLayoutData(gd);

			fGroupLabel = SWTFactory.createLabel(filterComp, Messages.TargetContentsGroup_0, 1);
			fGroupCombo = SWTFactory.createCombo(filterComp, SWT.READ_ONLY, 1, new String[] {Messages.TargetContentsGroup_1, Messages.TargetContentsGroup_2, Messages.TargetContentsGroup_3});
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalIndent = 10;
			fGroupCombo.setLayoutData(gd);
			fGroupCombo.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					handleGroupChange();
				}
			});
			fGroupCombo.select(0);
		}

		// TODO Don't allow different grouping for now.
		fGroupLabel.setVisible(false);
		if (fGroupCombo != null) {
			fGroupCombo.setVisible(false);
		}
		if (fGroupComboPart != null) {
			fGroupComboPart.getControl().setVisible(false);
		}

		fSelectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					for (int i = 0; i < selected.length; i++) {
						fFilteredTree.setChecked(selected[i], true);
					}
					saveCheckState();
					updateButtons();
				}
			}
		});

		fDeselectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					for (int i = 0; i < selected.length; i++) {
						fFilteredTree.setChecked(selected[i], false);
					}
					saveCheckState();
					updateButtons();
				}
			}
		});

		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTree.setAllChecked(true);
				fFilteredTree.saveCheckState();
				saveCheckState();
				updateButtons();

//				// The following code can be used to select everything (including things that are not visible
//				Object[] selected = null;
//				if (fGrouping == GROUP_BY_NONE) {
//					selected = fTargetDefinition.getAvailableUnits();
//				} else {
//					Object[] expanded = fTree.getExpandedElements();
//					fTree.expandAll();
//					selected = fTree.getVisibleExpandedElements();
//					// TODO Check that the check state can be changed after resetting the expanded elements
//					fTree.setExpandedElements(expanded);
//				}
//				if (selected != null && selected.length > 0) {
//					fFilteredTree.setCheckedElements(selected);
//					saveCheckState();
//				}

			}
		});

		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fTree.setAllChecked(false);
				fFilteredTree.saveCheckState();
				saveCheckState();
				updateButtons();
			}
		});

		fShowPluginsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowPluginsButton.getSelection()) {
					fFilteredTree.saveCheckState();
					fTree.addFilter(fPluginFilter);
				} else {
					fTree.removeFilter(fPluginFilter);
					fTree.expandAll();
					fFilteredTree.restoreCheckState();
				}
				updateButtons();
			}
		});
		fShowPluginsButton.setSelection(true);
		GridData gd = new GridData();
		gd.horizontalIndent = 10;
		fShowPluginsButton.setLayoutData(gd);

		fShowSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowSourceButton.getSelection()) {
					fFilteredTree.saveCheckState();
					fTree.addFilter(fSourceFilter);
				} else {
					fTree.removeFilter(fSourceFilter);
					fTree.expandAll();
					fFilteredTree.restoreCheckState();
				}
				updateButtons();
			}
		});
		fShowSourceButton.setSelection(true);
		gd = new GridData();
		gd.horizontalIndent = 10;
		fShowSourceButton.setLayoutData(gd);
	}

	private void initializeFilters() {
		fSourceFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IInstallableUnit) {
					return !P2Utils.isSourceBundle((IInstallableUnit) element);
				}
				return true;
			}
		};
		fPluginFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IInstallableUnit) {
					return P2Utils.isSourceBundle((IInstallableUnit) element);
				}
				return true;
			}
		};
	}

	private void handleGroupChange() {
		int index;
		if (fGroupCombo != null) {
			index = fGroupCombo.getSelectionIndex();
		} else {
			index = fGroupComboPart.getSelectionIndex();
		}
		if (index != fGrouping) {
			fGrouping = index;
			fTree.getControl().setRedraw(false);
			fTree.refresh(false);
			fTree.expandAll();
			updateButtons();
			fTree.getControl().setRedraw(true);
		}
	}

	private void updateButtons() {
		if (fTargetDefinition != null && !fTree.getSelection().isEmpty()) {
			Object[] selection = ((IStructuredSelection) fTree.getSelection()).toArray();
			boolean hasResolveBundle = false;
			boolean hasParent = false;
			boolean allSelected = true;
			boolean noneSelected = true;
			for (int i = 0; i < selection.length; i++) {
				if (!hasResolveBundle || !hasParent) {
					if (selection[i] instanceof IInstallableUnit) {
						hasResolveBundle = true;
					} else {
						hasParent = true;
					}
				}
				boolean checked = fTree.getChecked(selection[i]);
				if (checked) {
					noneSelected = false;
				} else {
					allSelected = false;
				}
			}
			// Selection is available is not everything is already selected and not both a parent and child item are selected
			fSelectButton.setEnabled(!allSelected && !(hasResolveBundle && hasParent));
			fDeselectButton.setEnabled(!noneSelected && !(hasResolveBundle && hasParent));
		} else {
			fSelectButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
		}

		fSelectAllButton.setEnabled(fTargetDefinition != null);
		fDeselectAllButton.setEnabled(fTargetDefinition != null);

		// TODO
		if (fTargetDefinition != null) {
			fCountLabel.setText(MessageFormat.format(Messages.TargetContentsGroup_9, new String[] {Integer.toString(fFilteredTree.getCheckedLeafNodeCount()), Integer.toString(fItemCount)}));
		} else {
			fCountLabel.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * Set the container to display in the tree or <code>null</code> to disable the tree 
	 * @param input bundle container or <code>null</code>
	 */
	public void setInput(ITargetDefinition input) {
		fTargetDefinition = input;
		fItemCount = 0;

		if (input == null) {
			fTree.setInput(Messages.TargetContentsGroup_10);
			setEnabled(false);
			return;
		}

		IStatus resolveStatus = input.getResolveStatus();
		if (resolveStatus != null && resolveStatus.getSeverity() == IStatus.ERROR) {
			fTree.setInput(resolveStatus);
			setEnabled(false);
			return;
		}

		if (!input.isResolved()) {
			fTree.setInput(Messages.TargetContentsGroup_10);
			setEnabled(false);
			return;
		}

		IInstallableUnit[] units = input.getAvailableUnits();
		if (units == null || units.length == 0) {
			fTree.setInput(Messages.TargetContentsGroup_11);
			setEnabled(false);
			return;
		}

		fItemCount = units.length;

		setEnabled(false);
		fTree.setInput(fTargetDefinition);
		// Expand everything first so that children have been calculated
		fTree.expandAll();
		IInstallableUnit[] included = fTargetDefinition.getIncludedUnits();
		fTree.setCheckedElements(included);
		fFilteredTree.saveCheckState();

		setEnabled(true);
	}

	/**
	 * This method clears any current target information and puts "Resolve Cancelled" into the
	 * tree.  Setting the input to null results in "Resolving..." to be put into the table which 
	 * may not be accurate.
	 */
	public void setCancelled() {
		fTargetDefinition = null;
		fTree.setInput(Messages.TargetContentsGroup_resolveCancelled);
		setEnabled(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		fTree.getTree().setEnabled(enabled);
		if (enabled) {
			updateButtons();
		} else {
			fSelectButton.setEnabled(false);
			fSelectAllButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
			fDeselectAllButton.setEnabled(false);
			fCountLabel.setText(""); //$NON-NLS-1$
		}
		fShowLabel.setEnabled(enabled);
		fShowPluginsButton.setEnabled(enabled);
		fShowSourceButton.setEnabled(enabled);
		fGroupLabel.setEnabled(enabled);
		if (fGroupCombo != null) {
			fGroupCombo.setEnabled(enabled);
		} else {
			fGroupComboPart.setEnabled(enabled);
		}
	}

	/**
	 * Saves the check state of the viewer to the target definition, informs any listeners that
	 * the contents have changed.
	 */
	public void saveCheckState() {
		if (fTargetDefinition != null) {
			IInstallableUnit[] allUnits = fTargetDefinition.getAvailableUnits();
			Object[] checked = fTree.getCheckedElements();
			// Do the conversion to bundle infos now, rather than looping through the units twice
			java.util.List leafChecked = new ArrayList(checked.length);
			for (int i = 0; i < checked.length; i++) {
				if (!((ITreeContentProvider) fTree.getContentProvider()).hasChildren(checked[i])) {
					if (checked[i] instanceof IInstallableUnit) {
						IInstallableUnit unit = (IInstallableUnit) checked[i];
						InstallableUnitDescription unitDescription = new InstallableUnitDescription();
						unitDescription.setId(unit.getId());
						unitDescription.setVersion(unit.getVersion());
						leafChecked.add(unitDescription);
					}
				}
			}
			if (leafChecked.size() == allUnits.length) {
				// Everything is included
				fTargetDefinition.setIncluded(null);
			} else {
				fTargetDefinition.setIncluded((InstallableUnitDescription[]) leafChecked.toArray(new InstallableUnitDescription[leafChecked.size()]));
			}
		}

		Object[] listeners = fChangeListeners.getListeners();
		for (int i = 0; i < listeners.length; i++) {
			((ITargetChangedListener) listeners[i]).contentsChanged(fTargetDefinition, this, false);
		}
	}

	/**
	 * Content provider for the content tree.  Allows for different groupings to be used.
	 *
	 */
	class TreeContentProvider implements ITreeContentProvider {
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof Collection) {
				return ((Collection) parentElement).toArray();
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof Collection) {
				return ((Collection) element).size() > 0;
			}
//			if (fGrouping == GROUP_BY_NONE || element instanceof IResolvedBundle) {
//				return false;
//			}
//			if (element instanceof IBundleContainer || element instanceof IPath) {
//				return getBundleChildren(element).size() > 0;
//			}
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {

				return ((ITargetDefinition) inputElement).getAvailableUnits();

				// Temporary code to test nesting in the tree
//				Collection[] lists = new Collection[] {new ArrayList(), new ArrayList(), new ArrayList()};
//				IInstallableUnit[] units = ((ITargetDefinition) inputElement).getAvailableUnits();
//				for (int i = 0; i < units.length; i++) {
//					lists[i % 3].add(units[i]);
//				}
//				return lists;

				// TODO Support grouping?
//				if (fGrouping == GROUP_BY_NONE) {
//					return fAllBundles.toArray();
//				} else if (fGrouping == GROUP_BY_CONTAINER) {
//					return fContainerBundles.keySet().toArray();
//				} else {
//					return fFileBundles.keySet().toArray();
//				}
			}
			return new Object[] {inputElement};
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

}
