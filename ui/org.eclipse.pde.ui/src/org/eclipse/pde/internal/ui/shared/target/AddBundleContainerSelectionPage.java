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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.core.target.provisional.ITargetDefinition;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Wizard page for selecting the type of bundle container to be added to a target
 * 
 * @see AddBundleContainerWizard
 * @see IBundleContainer
 */
public class AddBundleContainerSelectionPage extends WizardSelectionPage {

	/**
	 * Extension point that provides target provisioner wizard
	 */
//	private static final String PROVISIONER_POINT = "targetProvisioners"; //$NON-NLS-1$
	private Text fDescription;
	private ITargetDefinition fTarget;

	protected AddBundleContainerSelectionPage(ITargetDefinition target) {
		super("SelectionPage"); //$NON-NLS-1$
		setTitle(Messages.AddBundleContainerSelectionPage_1);
		setMessage(Messages.AddBundleContainerSelectionPage_2);
		fTarget = target;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH);

		SashForm sashForm = new SashForm(comp, SWT.VERTICAL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		// limit the width of the sash form to avoid the wizard opening very wide.
		gd.widthHint = 300;
		sashForm.setLayoutData(gd);

		TableViewer wizardSelectionViewer = new TableViewer(sashForm, SWT.BORDER);
		wizardSelectionViewer.setContentProvider(new ArrayContentProvider());
		wizardSelectionViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AbstractBundleContainerNode) {
					return ((AbstractBundleContainerNode) element).getName();
				}
				return super.getText(element);
			}

			public Image getImage(Object element) {
				if (element instanceof AbstractBundleContainerNode) {
					return ((AbstractBundleContainerNode) element).getImage();
				}
				return super.getImage(element);
			}
		});
		wizardSelectionViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					setSelectedNode((IWizardNode) selection.getFirstElement());
					getContainer().showPage(getNextPage());
				}
			}
		});
		wizardSelectionViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (!selection.isEmpty()) {
					fDescription.setText(((AbstractBundleContainerNode) selection.getFirstElement()).getDescription());
					setSelectedNode((AbstractBundleContainerNode) selection.getFirstElement());
				}
			}
		});
		wizardSelectionViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));

		fDescription = SWTFactory.createText(sashForm, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.WRAP, 1);

		sashForm.setWeights(new int[] {70, 30});
		initViewerContents(wizardSelectionViewer);
		setControl(comp);
	}

	private void initViewerContents(TableViewer wizardSelectionViewer) {
		AbstractBundleContainerNode directoryNode = new AbstractBundleContainerNode(Messages.AddBundleContainerSelectionPage_3, Messages.AddBundleContainerSelectionPage_4, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER)) {
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					public void addPages() {
						addPage(new AddDirectoryContainerPage("DirectoryPage")); //$NON-NLS-1$
					}

					public boolean performFinish() {
						IBundleContainer container = ((AddDirectoryContainerPage) getPages()[0]).getBundleContainer();
						if (container != null) {
							IBundleContainer[] oldContainers = fTarget.getBundleContainers();
							if (oldContainers == null) {
								fTarget.setBundleContainers(new IBundleContainer[] {container});
							} else {
								IBundleContainer[] newContainers = new IBundleContainer[oldContainers.length + 1];
								System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
								newContainers[newContainers.length - 1] = container;
								fTarget.setBundleContainers(newContainers);
							}
						}
						return true;
					}
				};
				// TODO Use same constant for all sub-wizards and this wizard
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		};
		AbstractBundleContainerNode installationNode = new AbstractBundleContainerNode(Messages.AddBundleContainerSelectionPage_6, Messages.AddBundleContainerSelectionPage_7, PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION)) {
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					public void addPages() {
						addPage(new AddProfileContainerPage("ProfilePage")); //$NON-NLS-1$
					}

					public boolean performFinish() {
						IBundleContainer container = ((AddProfileContainerPage) getPages()[0]).getBundleContainer();
						if (container != null) {
							IBundleContainer[] oldContainers = fTarget.getBundleContainers();
							if (oldContainers == null) {
								fTarget.setBundleContainers(new IBundleContainer[] {container});
							} else {
								IBundleContainer[] newContainers = new IBundleContainer[oldContainers.length + 1];
								System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
								newContainers[newContainers.length - 1] = container;
								fTarget.setBundleContainers(newContainers);
							}
						}
						return true;
					}
				};
				// TODO Use same constant for all sub-wizards and this wizard
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		};
		AbstractBundleContainerNode featureNode = new AbstractBundleContainerNode(Messages.AddBundleContainerSelectionPage_9, Messages.AddBundleContainerSelectionPage_10, PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ)) {
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					public void addPages() {
						addPage(new AddFeatureContainerPage("FeaturePage")); //$NON-NLS-1$
					}

					public boolean performFinish() {
						try {
							IBundleContainer[] containers = ((AddFeatureContainerPage) getPages()[0]).getContainers();
							if (containers != null) {
								IBundleContainer[] oldContainers = fTarget.getBundleContainers();
								if (oldContainers == null) {
									fTarget.setBundleContainers(containers);
								} else {
									IBundleContainer[] newContainers = new IBundleContainer[oldContainers.length + containers.length];
									System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
									System.arraycopy(containers, 0, newContainers, oldContainers.length, containers.length);
									fTarget.setBundleContainers(newContainers);
								}
							}
							return true;
						} catch (CoreException e) {
							setErrorMessage(e.getMessage());
							return false;
						}
					}
				};
				// TODO Use same constant for all sub-wizards and this wizard
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		};
		wizardSelectionViewer.setInput(new AbstractBundleContainerNode[] {directoryNode, installationNode, featureNode});

	}

//	private IWizardNode createExtensionWizardNode(WizardElement element) {
//		return new WizardNode(this, element) {
//			public IBasePluginWizard createWizard() throws CoreException {
//				return (IBasePluginWizard) wizardElement.createExecutableExtension();
//			}
//		};
//	}
//
//	private List getAvailableProvisioners() {
//		List list = new ArrayList();
//		IExtensionRegistry registry = Platform.getExtensionRegistry();
//		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), PROVISIONER_POINT);
//		if (point == null)
//			return list;
//		IExtension[] extensions = point.getExtensions();
//		for (int i = 0; i < extensions.length; i++) {
//			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
//			for (int j = 0; j < elements.length; j++) {
//				WizardElement element = createWizardElement(elements[j]);
//				if (element != null) {
//					final String pluginId = element.getPluginId();
//					final String contributionId = element.getID();
//					IPluginContribution pc = new IPluginContribution() {
//						public String getLocalId() {
//							return contributionId;
//						}
//
//						public String getPluginId() {
//							return pluginId;
//						}
//					};
//					if (!WorkbenchActivityHelper.filterItem(pc)) {
//						list.add(element);
//					}
//				}
//			}
//		}
//		return list;
//	}

	/**
	 * Abstract implementation of the IWizardNode interface providing a consistent look and feel
	 * for the table displaying a list of possible bundle container types.
	 */
	abstract class AbstractBundleContainerNode implements IWizardNode {
		private String fTypeName;
		private String fTypeDescription;
		private Image fTypeImage;
		private IWizard fWizard;

		public AbstractBundleContainerNode(String name, String description, Image image) {
			fTypeName = name;
			fTypeDescription = description;
			fTypeImage = image;
		}

		public abstract IWizard createWizard();

		public void dispose() {
			if (fWizard != null) {
				fWizard.dispose();
				fWizard = null;
			}
		}

		public Point getExtent() {
			return new Point(-1, -1);
		}

		public IWizard getWizard() {
			if (fWizard == null) {
				fWizard = createWizard();
			}
			return fWizard;
		}

		public boolean isContentCreated() {
			return fWizard != null;
		}

		public String getName() {
			return fTypeName;
		}

		public String getDescription() {
			return fTypeDescription;
		}

		public Image getImage() {
			return fTypeImage;
		}
	}

}
