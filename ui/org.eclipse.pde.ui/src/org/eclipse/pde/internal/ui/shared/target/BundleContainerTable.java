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
	private AbstractFormPart fFormPart; // TODO Remove when proper model/editor listening is done

	/**
	 * Creates this part using the form toolkit and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @param toolkit toolkit to create the widgets with
	 * @param tempPart form part used to mark the editor dirty or <code>null</code>
	 * @return generated instance of the table part
	 */
	public static BundleContainerTable createTableInForm(Composite parent, FormToolkit toolkit, AbstractFormPart tempPart) {
		BundleContainerTable contentTable = new BundleContainerTable(tempPart);
		contentTable.createFormContents(parent, toolkit);
		return contentTable;
	}

	/**
	 * Creates this part using standard dialog widgets and adds it to the given composite.
	 * 
	 * @param parent parent composite
	 * @return generated instance of the table part
	 */
	public static BundleContainerTable createTableInDialog(Composite parent) {
		BundleContainerTable contentTable = new BundleContainerTable(null);
		contentTable.createDialogContents(parent);
		return contentTable;
	}

	/**
	 * Constructor
	 * @param tempPart form part used to mark an editor dirty or <code>null</code>
	 */
	private BundleContainerTable(AbstractFormPart tempPart) {
		fFormPart = tempPart;
	}

	/**
	 * Creates the part contents from a toolkit
	 * @param parent parent composite
	 * @param toolkit form toolkit to create widgets
	 */
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
		fEditButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_1, SWT.PUSH);
		fRemoveButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_2, SWT.PUSH);
		fRemoveAllButton = toolkit.createButton(buttonComp, Messages.BundleContainerTable_3, SWT.PUSH);

		initializeButtons();

		toolkit.paintBordersFor(comp);
	}

	/**
	 * Creates the part contents using SWTFactory
	 * @param parent parent composite
	 */
	private void createDialogContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);

		Tree atree = new Tree(comp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		atree.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		atree.setLayoutData(gd);

		initializeTreeViewer(atree);

		Composite buttonComp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		buttonComp.setLayout(layout);
		buttonComp.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fAddButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_0, null);
		fEditButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_1, null);
		fRemoveButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_2, null);
		fRemoveAllButton = SWTFactory.createPushButton(buttonComp, Messages.BundleContainerTable_3, null);

		initializeButtons();
	}

	/**
	 * Sets up the tree viewer using the given tree
	 * @param tree
	 */
	private void initializeTreeViewer(Tree tree) {
		fTreeViewer = new TreeViewer(tree);
		fTreeViewer.setContentProvider(new TargetContentProvider());
		fTreeViewer.setLabelProvider(new TargetLabelProvider());
		fTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
	}

	/**
	 * Sets up the buttons, the button fields must already be created before calling this method
	 */
	private void initializeButtons() {
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		fAddButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fAddButton);

		fEditButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleEdit();
			}
		});
		fEditButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fEditButton);

		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		fRemoveButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fRemoveButton);

		fRemoveAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemoveAll();
			}
		});
		fRemoveAllButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING));
		SWTFactory.setButtonDimensionHint(fRemoveAllButton);
	}

	/**
	 * Sets the target definition model to use as input for the tree, can be called with different
	 * models to change the tree's input
	 * @param target target model
	 */
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
			markDirty();
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
							if (oldRestrictions != null) {
								markDirty();
								refresh();
							}
						} else {
							BundleInfo[] selectedRestrictions = new BundleInfo[result.length];
							System.arraycopy(result, 0, selectedRestrictions, 0, result.length);
							BundleInfo[] newRestrictions = new BundleInfo[selectedRestrictions.length];
							for (int i = 0; i < selectedRestrictions.length; i++) {
								newRestrictions[i] = new BundleInfo(selectedRestrictions[i].getSymbolicName(), dialog.isUseVersion() ? selectedRestrictions[i].getVersion() : null, null, BundleInfo.NO_LEVEL, false);
							}
							container.setRestrictions(newRestrictions);
							markDirty();
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
			markDirty();
			refresh();
		}
	}

	private void handleRemoveAll() {
		fTarget.setBundleContainers(null);
		markDirty();
		refresh();
	}

	private void updateButtons() {
		IStructuredSelection selection = (IStructuredSelection) fTreeViewer.getSelection();
		fEditButton.setEnabled(!selection.isEmpty() && selection.getFirstElement() instanceof IBundleContainer);
		fRemoveButton.setEnabled(!selection.isEmpty());
		fRemoveAllButton.setEnabled(fTarget.getBundleContainers() != null && fTarget.getBundleContainers().length > 0);
	}

	private void markDirty() {
		if (fFormPart != null) {
			fFormPart.markDirty();
		}
	}

	/**
	 * Content provider for the tree, primary input is a ITargetDefinition, children are IBundleContainers
	 */
	class TargetContentProvider implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
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
				IBundleContainer[] containers = ((ITargetDefinition) inputElement).getBundleContainers();
				if (containers != null) {
					return containers;
				}
			}
			return new Object[0];
		}

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

	}

	/**
	 * Label provider for the tree
	 */
	class TargetLabelProvider extends BundleInfoLabelProvider {
		public String getText(Object element) {
			// TODO The label provider should be NLS'd
			if (element instanceof FeatureBundleContainer) {
				StringBuffer buf = new StringBuffer();
				buf.append(((FeatureBundleContainer) element).getFeatureId());
				String version = ((FeatureBundleContainer) element).getFeatureVersion();
				if (version != null) {
					buf.append(" (").append(version).append(") ");
				}
				try {
					buf.append(((FeatureBundleContainer) element).getLocation(false));
				} catch (CoreException e) {
					buf.append(e.getMessage());
				}
				getRestrictionLabel((FeatureBundleContainer) element, buf);
				return buf.toString();
			} else if (element instanceof DirectoryBundleContainer) {
				StringBuffer buf = new StringBuffer();
				try {
					buf.append(((DirectoryBundleContainer) element).getLocation(false));
				} catch (CoreException e) {
					buf.append(e.getMessage());
				}
				getRestrictionLabel((DirectoryBundleContainer) element, buf);
				return buf.toString();
			} else if (element instanceof ProfileBundleContainer) {
				StringBuffer buf = new StringBuffer();
				try {
					buf.append(((ProfileBundleContainer) element).getLocation(false));
				} catch (CoreException e) {
					buf.append(e.getMessage());
				}
				String configArea = ((ProfileBundleContainer) element).getConfigurationLocation();
				if (configArea != null) {
					buf.append(" (Configuration:").append(configArea).append(")");
				}
				getRestrictionLabel((ProfileBundleContainer) element, buf);
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

		private void getRestrictionLabel(IBundleContainer container, StringBuffer buf) {
			BundleInfo[] restrictions = container.getRestrictions();
			buf.append(" <");
			if (restrictions != null) {
				buf.append(restrictions.length);
			} else {
				buf.append("all");
			}
			buf.append(" plug-in(s) selected>");
		}
	}

}
