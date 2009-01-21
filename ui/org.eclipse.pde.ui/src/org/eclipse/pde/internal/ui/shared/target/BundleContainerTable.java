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

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.internal.provisional.frameworkadmin.BundleInfo;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.target.impl.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.targetdefinition.TargetEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * UI part that can be added to a dialog or to a form editor.  Contains a table displaying
 * the bundle containers of a target definition.  Also has buttons to add, edit and remove
 * bundle containers of varying types.
 * 
 * @see TargetEditor
 * @see ITargetDefinition
 * @see IBundleContainer
 */
public class BundleContainerTable {

	private TreeViewer fTreeViewer;
	private Button fAddButton;
	private Button fEditButton;
	private Button fRemoveButton;
	private Button fRemoveAllButton;
	private ITargetDefinition fTarget;
	private AbstractFormPart fTempPart; // TODO Remove when proper model/editor listening is done

	public static BundleContainerTable createTableInForm(Composite parent, FormToolkit toolkit, AbstractFormPart tempPart) {
		BundleContainerTable contentTable = new BundleContainerTable(tempPart);
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	public static BundleContainerTable createTableInDialog(Composite parent) {
		BundleContainerTable contentTable = new BundleContainerTable(null);
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	private BundleContainerTable(AbstractFormPart tempPart) {
		fTempPart = tempPart;
	}

	private void createFormContents(Composite parent, FormToolkit toolkit) {
		Composite comp = toolkit.createComposite(parent);
		comp.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		comp.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));

		Tree atree = toolkit.createTree(comp, SWT.V_SCROLL | SWT.H_SCROLL);
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		atree.setLayoutData(gd);
		initializeTreeViewer(atree);

		Composite buttonComp = toolkit.createComposite(comp);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_0, SWT.PUSH);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fAddButton);

		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_1, SWT.PUSH);
		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fEditButton);

		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_2, SWT.PUSH);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fRemoveButton);

		fRemoveAllButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_3, SWT.PUSH);
		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fRemoveAllButton);

		toolkit.paintBordersFor(comp);

	}

	/**
	 * @param parent
	 */
	private void createDialogContents(Composite parent) {
		// TODO Auto-generated method stub
	}

	private void initializeTreeViewer(Tree aTree) {
		fTreeViewer = new TreeViewer(aTree);
		fTreeViewer.setContentProvider(new TargetContentProvider());
		fTreeViewer.setLabelProvider(new TargetLabelProvider());
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
	}

	public void setInput(ITargetDefinition target) {
		fTarget = target;
		refresh();
	}

	public void refresh() {
		fTreeViewer.setInput(fTarget);
		fTreeViewer.refresh();
		updateButtons();
	}

	private void handleAdd() {
		AddBundleContainerWizard wizard = new AddBundleContainerWizard((ITargetDefinition) fTreeViewer.getInput());
		Shell parent = fTreeViewer.getTree().getShell();
		WizardDialog dialog = new WizardDialog(parent, wizard);
		if (dialog.open() != Window.CANCEL) {
			refresh();
			// TODO Mark editor dirty
			fTempPart.markDirty();
		}
	}

	private void handleEdit() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			IBundleContainer container = null;
			if (selected instanceof IBundleContainer) {
				container = (IBundleContainer) selected;
			} else if (selected instanceof BundleInfo) {
				// TODO Selecting a child should allow editing its parent.
			}
			if (container != null) {
				// We need to get a list of all possible bundles, remove restrictions while resolving
				BundleInfo[] oldRestrictions = container.getRestrictions();
				container.setRestrictions(null);
				BundleInfo[] resolvedBundles = null;
				try {
					resolvedBundles = container.resolveBundles(null);
				} catch (CoreException e) {
					resolvedBundles = new BundleInfo[0];
					PDEPlugin.log(e);
				}
				container.setRestrictions(oldRestrictions);

				RestrictionsListSelectionDialog dialog = new RestrictionsListSelectionDialog(fTreeViewer.getTree().getShell(), resolvedBundles, oldRestrictions);
				if (dialog.open() == Window.OK) {
					Object[] result = dialog.getResult();
					if (result != null) {
						if (result.length == resolvedBundles.length) {
							container.setRestrictions(null);
						} else {
							BundleInfo[] selectedRestrictions = new BundleInfo[result.length];
							System.arraycopy(result, 0, selectedRestrictions, 0, result.length);
							BundleInfo[] newRestrictions = new BundleInfo[selectedRestrictions.length];
							for (int i = 0; i < selectedRestrictions.length; i++) {
								newRestrictions[i] = new BundleInfo(selectedRestrictions[i].getSymbolicName(), dialog.isUseVersion() ? selectedRestrictions[i].getVersion() : null, null, BundleInfo.NO_LEVEL, false);
							}
							container.setRestrictions(newRestrictions);
							// TODO Mark the editor dirty
							fTempPart.markDirty();
							refresh();
						}
					}
				}
			}
		}
	}

	private void handleRemove() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		if (!selection.isEmpty()) {
			Object selected = selection.getFirstElement();
			IBundleContainer container = null;
			if (selected instanceof IBundleContainer) {
				container = (IBundleContainer) selected;
			} else if (selected instanceof BundleInfo) {
				// TODO Selecting a child should allow removing its parent?
			}
			IBundleContainer[] currentContainers = fTarget.getBundleContainers();
			ArrayList newBundleContainers = new ArrayList(currentContainers.length);
			for (int i = 0; i < currentContainers.length; i++) {
				if (!currentContainers[i].equals(container)) {
					newBundleContainers.add(currentContainers[i]);
				}
			}
			fTarget.setBundleContainers((IBundleContainer[]) newBundleContainers.toArray(new IBundleContainer[newBundleContainers.size()]));
			fTempPart.markDirty();
			refresh();
		}
	}

	private void handleRemoveAll() {
		fTarget.setBundleContainers(null);
		fTempPart.markDirty();
		refresh();
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		fEditButton.setEnabled(!selection.isEmpty());
		fRemoveButton.setEnabled(!selection.isEmpty());
	}

	class TargetContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			// TODO If returning null is valid we can simplify this code
			if (parentElement instanceof ITargetDefinition) {
				IBundleContainer[] containers = ((ITargetDefinition) parentElement).getBundleContainers();
				return containers != null ? containers : new Object[0];
			} else if (parentElement instanceof IBundleContainer) {
				try {
					return ((IBundleContainer) parentElement).resolveBundles(new NullProgressMonitor());
				} catch (CoreException e) {
					// TODO Handle proper status
					return new String[] {"Error getting bundle list: " + e.getMessage()};
				}
			}
			return new Object[0];
		}

		public Object getParent(Object element) {
			if (element instanceof IBundleContainer) {
			}
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof ITargetDefinition) {
				IBundleContainer[] containers = ((ITargetDefinition) element).getBundleContainers();
				return containers != null && containers.length > 0;
			}
			if (element instanceof IBundleContainer) {
				return true;
			}
			return false;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ITargetDefinition) {
				return ((ITargetDefinition) inputElement).getBundleContainers();
			}
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	// TODO The label provider should be NLS'd
	class TargetLabelProvider extends BundleInfoLabelProvider {
		public String getText(Object element) {
			if (element instanceof FeatureBundleContainer) {
				StringBuffer buf = new StringBuffer();
				buf.append("Feature ").append("Name: ").append(((FeatureBundleContainer) element).getFeatureId());
				String version = ((FeatureBundleContainer) element).getFeatureVersion();
				if (version != null) {
					buf.append(" Version: ").append(version);
				}
				try {
					buf.append(" Location: ").append(((FeatureBundleContainer) element).getLocation(false));
				} catch (CoreException e) {
					buf.append(e.getMessage());
				}
				if (((FeatureBundleContainer) element).getRestrictions() != null) {
					buf.append(" <Restricted>");
				}
				return buf.toString();
			} else if (element instanceof DirectoryBundleContainer) {
				StringBuffer buf = new StringBuffer();
				buf.append("Directory ");
				try {
					buf.append("Location: ").append(((DirectoryBundleContainer) element).getLocation(false));
				} catch (CoreException e) {
					buf.append(e.getMessage());
				}
				if (((DirectoryBundleContainer) element).getRestrictions() != null) {
					buf.append(" <Restricted>");
				}
				return buf.toString();
			} else if (element instanceof ProfileBundleContainer) {
				StringBuffer buf = new StringBuffer();
				buf.append("Installation ");
				try {
					buf.append("Location: ").append(((ProfileBundleContainer) element).getLocation(false));
				} catch (CoreException e) {
					buf.append(e.getMessage());
				}
				String configArea = ((ProfileBundleContainer) element).getConfigurationLocation();
				if (configArea != null) {
					buf.append(" Configuration:").append(configArea);
				}
				if (((ProfileBundleContainer) element).getRestrictions() != null) {
					buf.append(" <Restricted>");
				}
				return buf.toString();
			}
			return super.getText(element);
		}

		public Image getImage(Object element) {
			if (element instanceof FeatureBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ);
			} else if (element instanceof DirectoryBundleContainer) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
			} else if (element instanceof ProfileBundleContainer) {
				return PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION);
			}
			return super.getImage(element);
		}
	}

}
