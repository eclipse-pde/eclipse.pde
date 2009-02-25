package org.eclipse.pde.internal.ui.shared.target;

import com.ibm.icu.text.MessageFormat;
import java.util.*;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.target.impl.AbstractBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.IResolvedBundle;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

public class IncludedBundlesTree extends FilteredTree {

	private CheckboxTreeViewer fTree;
	private Button fSelectButton;
	private Button fDeselectButton;
	private Button fSelectAllButton;
	private Button fDeselectAllButton;
//	private Button fSelectRequiredButton;
	private Label fShowLabel;
	private Button fShowSourceButton;
	private Button fShowPluginsButton;
	private Label fCountLabel;
	private ViewerFilter fSourceFilter;
	private ViewerFilter fPluginFilter;
	private IResolvedBundle[] fAllBundles;

	public IncludedBundlesTree(Composite parent) {
		super(parent, SWT.BORDER | SWT.MULTI, new PatternFilter(), true);
	}

	protected Control createTreeControl(Composite parent, int style) {
		Composite treeComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);
		super.createTreeControl(treeComp, style);
		((GridData) fTree.getControl().getLayoutData()).heightHint = 300;
		createButtons(treeComp);
		fCountLabel = SWTFactory.createLabel(treeComp, "", 2); //$NON-NLS-1$
		updateButtons();
		initializeFilters();
		return treeComp;
	}

	protected void createButtons(Composite parent) {
		Composite buttonComp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_VERTICAL, 0, 0);

		// TODO Add Mnemonics
		fSelectButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_0, null);
		fSelectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					for (int i = 0; i < selected.length; i++) {
						fTree.setChecked(selected[i], true);
					}
					updateButtons();
				}
			}
		});
		fDeselectButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_1, null);
		fDeselectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fTree.getSelection().isEmpty()) {
					Object[] selected = ((IStructuredSelection) fTree.getSelection()).toArray();
					for (int i = 0; i < selected.length; i++) {
						fTree.setChecked(selected[i], false);
					}
					updateButtons();
				}
			}
		});

		createEmptySpace(buttonComp);

		fSelectAllButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_2, null);
		fSelectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// We only want to check visible
				fTree.setAllChecked(true);
				updateButtons();
			}
		});
		fDeselectAllButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_3, null);
		fDeselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// We only want to uncheck visible
				fTree.setAllChecked(false);
				updateButtons();
			}
		});

		createEmptySpace(buttonComp);

		// TODO Support selecting required.
//		fSelectRequiredButton = SWTFactory.createPushButton(buttonComp, Messages.IncludedBundlesTree_4, null);
//		fSelectRequiredButton.addSelectionListener(new SelectionAdapter() {
//			public void widgetSelected(SelectionEvent e) {
//				updateButtons();
//			}
//		});

		Composite filterComp = SWTFactory.createComposite(buttonComp, 1, 1, SWT.NONE, 0, 0);
		filterComp.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, true));

		fShowLabel = SWTFactory.createLabel(filterComp, Messages.BundleContainerTable_9, 1);

		fShowPluginsButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_14, null, true, 1);
		fShowPluginsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowPluginsButton.getSelection()) {
					fTree.addFilter(fPluginFilter);
				} else {
					fTree.removeFilter(fPluginFilter);
				}
				updateButtons();
			}
		});
		fShowPluginsButton.setSelection(true);
		GridData gd = new GridData();
		gd.horizontalIndent = 10;
		fShowPluginsButton.setLayoutData(gd);

		fShowSourceButton = SWTFactory.createCheckButton(filterComp, Messages.BundleContainerTable_15, null, true, 1);
		fShowSourceButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fShowSourceButton.getSelection()) {
					fTree.addFilter(fSourceFilter);
				} else {
					fTree.removeFilter(fSourceFilter);
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
				if (element instanceof IResolvedBundle) {
					if (((IResolvedBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
		fPluginFilter = new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if (element instanceof IResolvedBundle) {
					if (!((IResolvedBundle) element).isSourceBundle()) {
						return false;
					}
				}
				return true;
			}
		};
	}

	private Label createEmptySpace(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
		gd.widthHint = gd.heightHint = 5;
		label.setLayoutData(gd);
		return label;
	}

	private void updateButtons() {
		if (fAllBundles != null && !fTree.getSelection().isEmpty()) {
			Object[] selection = ((IStructuredSelection) fTree.getSelection()).toArray();
			boolean allSelected = true;
			boolean noneSelected = true;
			for (int i = 0; i < selection.length; i++) {
				boolean checked = fTree.getChecked(selection[i]);
				if (checked) {
					noneSelected = false;
				} else {
					allSelected = false;
				}
			}
			fSelectButton.setEnabled(!allSelected);
			fDeselectButton.setEnabled(!noneSelected);
//			fSelectRequiredButton.setEnabled(true);
		} else {
			fSelectButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
//			fSelectRequiredButton.setEnabled(false);
		}

		int checked = fTree.getCheckedElements().length;
		fSelectAllButton.setEnabled(fAllBundles != null && checked != fTree.getTree().getItemCount());
		fDeselectAllButton.setEnabled(fAllBundles != null && checked != 0);

		if (fAllBundles != null) {
			fCountLabel.setText(MessageFormat.format(Messages.IncludedBundlesTree_5, new String[] {Integer.toString(checked), Integer.toString(fAllBundles.length)}));
		} else {
			fCountLabel.setText(""); //$NON-NLS-1$
		}
	}

	/**
	 * Set the container to display in the tree or <code>null</code> to disable the tree 
	 * @param input bundle container or <code>null</code>
	 */
	public void setInput(IBundleContainer input) {
		fAllBundles = null;

		// Check that the input is a container with valid, resolved bundles
		if (!(input instanceof AbstractBundleContainer)) {
			fTree.setInput(Messages.AddDirectoryContainerPage_7);
			setEnabled(false);
			return;
		}
		if (!input.isResolved()) {
			fTree.setInput(new Status(IStatus.ERROR, PDEPlugin.getPluginId(), Messages.BundleContainerTable_19));
			setEnabled(false);
			return;
		}
		IStatus status = input.getBundleStatus();
		if (!status.isOK() && !status.isMultiStatus()) {
			fTree.setInput(status);
			setEnabled(false);
			return;
		}
		IResolvedBundle[] allResolvedBundles = ((AbstractBundleContainer) input).getAllBundles();
		if (allResolvedBundles == null || allResolvedBundles.length == 0) {
			fTree.setInput(Messages.AddDirectoryContainerPage_7);
			setEnabled(false);
			return;
		}

		// Input is valid, setup the tree
		fAllBundles = allResolvedBundles;
		fTree.setInput(allResolvedBundles);

		// Check the included bundles
		BundleInfo[] included = input.getIncludedBundles();
		if (included == null) {
			fTree.setCheckedElements(fAllBundles);
		} else {
			Set includedBundles = new HashSet();
			for (int i = 0; i < included.length; i++) {
				includedBundles.add(included[i].getSymbolicName());
			}
			java.util.List toCheck = new ArrayList(includedBundles.size());
			for (int i = 0; i < allResolvedBundles.length; i++) {
				if (includedBundles.contains(allResolvedBundles[i].getBundleInfo().getSymbolicName())) {
					toCheck.add(allResolvedBundles[i]);
				}
			}
			fTree.setCheckedElements(toCheck.toArray());
		}

		// Enable the tree and update the buttons
		setEnabled(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			updateButtons();
		} else {
			fSelectButton.setEnabled(false);
			fSelectAllButton.setEnabled(false);
			fDeselectButton.setEnabled(false);
			fDeselectAllButton.setEnabled(false);
//			fSelectRequiredButton.setEnabled(false);
			fCountLabel.setText(""); //$NON-NLS-1$
		}
		fShowLabel.setEnabled(enabled);
		fShowPluginsButton.setEnabled(enabled);
		fShowSourceButton.setEnabled(enabled);
		super.setEnabled(enabled);
	}

	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		fTree = new CheckboxTreeViewer(parent, style) {
			public void refresh(boolean updateLabels) {
				super.refresh(updateLabels);
				if (updateLabels) {
					// We want to update the labels and buttons as users change the filtering
					updateButtons();
				}
			}
		};
		fTree.setContentProvider(new ITreeContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				if (inputElement instanceof IResolvedBundle[]) {
					return (IResolvedBundle[]) inputElement;
				}
				return new Object[] {inputElement};
			}

			public boolean hasChildren(Object element) {
				return false;
			}

			public Object getParent(Object element) {
				return null;
			}

			public Object[] getChildren(Object parentElement) {
				return new Object[0];
			}
		});
		fTree.setLabelProvider(new BundleInfoLabelProvider(false));
		fTree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				fTree.setChecked(selection.getFirstElement(), !fTree.getChecked(selection.getFirstElement()));
				updateButtons();
			}
		});
		fTree.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		fTree.setSorter(new ViewerSorter());
		return fTree;
	}

	/**
	 * Return the set of bundles to include in this bundle container based on what is
	 * checked in the tree.  If all bundles in the container are checked or there was
	 * a problem getting the bundles from the container, this method will return 
	 * <code>null</code>
	 * 
	 * @return set of bundles to include or <code>null</code>
	 */
	public BundleInfo[] getIncludedBundles() {
		if (fTree.getControl().isEnabled() && fAllBundles != null) {
			Object[] checked = fTree.getCheckedElements();
			if (checked.length == fAllBundles.length) {
				return null;
			}

			java.util.List included = new ArrayList(checked.length);
			for (int i = 0; i < checked.length; i++) {
				if (checked[i] instanceof IResolvedBundle) {
					included.add(new BundleInfo(((IResolvedBundle) checked[i]).getBundleInfo().getSymbolicName(), null, null, BundleInfo.NO_LEVEL, false));
				}
			}
			return (BundleInfo[]) included.toArray(new BundleInfo[included.size()]);
		}
		return null;
	}
}
