/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.io.File;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.target.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.ui.IProvisionerWizard;
import org.eclipse.pde.ui.target.ITargetLocationWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.*;
import org.eclipse.ui.activities.WorkbenchActivityHelper;

/**
 * Wizard page for selecting the type of bundle container to be added to a target
 *
 * @see AddBundleContainerWizard
 * @see ITargetLocation
 */
public class AddBundleContainerSelectionPage extends WizardSelectionPage {

	/**
	 * Extension point that provides target provisioner wizard
	 */
	private static final String TARGET_LOCATION_PROVISIONER_POINT = "targetLocationProvisioners"; //$NON-NLS-1$

	/**
	 * Deprecated extension point providing target provisioner wizards
	 */
	private static final String TARGET_PROVISIONER_POINT = "targetProvisioners"; //$NON-NLS-1$

	/**
	 * Section in the dialog settings for this wizard and the wizards created with selection
	 * Shared with the EditBundleContainerWizard
	 */
	static final String SETTINGS_SECTION = "editBundleContainerWizard"; //$NON-NLS-1$

	private static ITargetPlatformService fTargetService;
	private Text fDescription;
	private ITargetDefinition fTarget;

	protected AddBundleContainerSelectionPage(ITargetDefinition target) {
		super("SelectionPage"); //$NON-NLS-1$
		setTitle(Messages.AddBundleContainerSelectionPage_1);
		setMessage(Messages.AddBundleContainerSelectionPage_2);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fTarget = target;
	}

	/**
	 * Gets the target platform service provided by PDE Core
	 * @return the target platform service
	 * @throws CoreException if unable to acquire the service
	 */
	private static ITargetPlatformService getTargetPlatformService() throws CoreException {
		if (fTargetService == null) {
			fTargetService = PDECore.getDefault().acquireService(ITargetPlatformService.class);
			if (fTargetService == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, Messages.AddDirectoryContainerPage_9));
			}
		}
		return fTargetService;
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	@Override
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);

		SashForm sashForm = new SashForm(comp, SWT.VERTICAL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		// limit the width of the sash form to avoid the wizard opening very wide.
		gd.widthHint = 300;
		sashForm.setLayoutData(gd);
		sashForm.setFont(comp.getFont());

		TableViewer wizardSelectionViewer = new TableViewer(sashForm, SWT.BORDER);
		wizardSelectionViewer.setContentProvider(ArrayContentProvider.getInstance());
		wizardSelectionViewer.setLabelProvider(new LabelProvider() {

			@Override
			public String getText(Object element) {
				if (element instanceof AbstractBundleContainerNode) {
					return ((AbstractBundleContainerNode) element).getName();
				}
				return super.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				if (element instanceof AbstractBundleContainerNode) {
					return ((AbstractBundleContainerNode) element).getImage();
				}
				return super.getImage(element);
			}
		});
		wizardSelectionViewer.addDoubleClickListener(event -> {
			IStructuredSelection selection = (IStructuredSelection) event.getSelection();
			if (!selection.isEmpty()) {
				setSelectedNode((IWizardNode) selection.getFirstElement());
				getContainer().showPage(getNextPage());
			}
		});
		wizardSelectionViewer.addSelectionChangedListener(event -> {
			IStructuredSelection selection = event.getStructuredSelection();
			if (!selection.isEmpty()) {
				Object element = selection.getFirstElement();
				if (element instanceof AbstractBundleContainerNode) {
					fDescription.setText(((AbstractBundleContainerNode) element).getDescription());
				}
				setSelectedNode((IWizardNode) selection.getFirstElement());
			}
		});
		wizardSelectionViewer.getTable().setLayoutData(new GridData(GridData.FILL_BOTH));
		wizardSelectionViewer.getTable().setFont(sashForm.getFont());

		fDescription = SWTFactory.createText(sashForm, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.WRAP, 1);

		sashForm.setWeights(new int[] {70, 30});
		initViewerContents(wizardSelectionViewer);
		setControl(comp);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.ADD_LOCATION_WIZARD);
	}

	/**
	 * Creates the IWizardNode instances that provide choices for the user to select
	 * @param wizardSelectionViewer
	 */
	private void initViewerContents(TableViewer wizardSelectionViewer) {
		List<AbstractBundleContainerNode> choices = new ArrayList<>();
		choices.addAll(getStandardChoices());
		choices.addAll(getTargetLocationProvisionerChoices()); // Extension point contributions
		choices.addAll(getTargetProvisionerChoices()); // Deprecated extension point contributions

		choices.sort(Comparator.comparing(element -> element.isPreferredOption() ? 0 : 1));

		wizardSelectionViewer.setInput(choices.toArray(new IWizardNode[choices.size()]));
	}

	/**
	 * Returns the standard choices of bundle containers to create
	 * @return list of wizard nodes
	 */
	private List<AbstractBundleContainerNode> getStandardChoices() {
		List<AbstractBundleContainerNode> standardChoices = new ArrayList<>(4);
		// Directory Containers
		standardChoices.add(new AbstractBundleContainerNode(Messages.AddBundleContainerSelectionPage_3, Messages.AddBundleContainerSelectionPage_4, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER)) {
			@Override
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					private EditDirectoryContainerPage fPage1;

					@Override
					public void addPages() {
						IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
						if (settings == null) {
							settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
						}
						setDialogSettings(settings);
						fPage1 = new EditDirectoryContainerPage();
						addPage(fPage1);
						addPage(new PreviewContainerPage(fTarget, fPage1));
						setNeedsProgressMonitor(true);
					}

					@Override
					public boolean performFinish() {
						ITargetLocation container = fPage1.getBundleContainer();
						if (container != null) {
							fPage1.storeSettings();
							ITargetLocation[] oldContainers = fTarget.getTargetLocations();
							if (oldContainers == null) {
								fTarget.setTargetLocations(new ITargetLocation[] {container});
							} else {
								ITargetLocation[] newContainers = new ITargetLocation[oldContainers.length + 1];
								System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
								newContainers[newContainers.length - 1] = container;
								fTarget.setTargetLocations(newContainers);
							}
						}
						return true;
					}
				};
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		});
		// Installation/Profile Containers
		standardChoices.add(new AbstractBundleContainerNode(Messages.AddBundleContainerSelectionPage_6, Messages.AddBundleContainerSelectionPage_7, PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION)) {
			@Override
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					private EditProfileContainerPage fPage1;

					@Override
					public void addPages() {
						IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
						if (settings == null) {
							settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
						}
						setDialogSettings(settings);
						setDialogSettings(settings);
						fPage1 = new EditProfileContainerPage();
						addPage(fPage1);
						addPage(new PreviewContainerPage(fTarget, fPage1));
						setNeedsProgressMonitor(true);
					}

					@Override
					public boolean performFinish() {
						ITargetLocation container = fPage1.getBundleContainer();
						if (container != null) {
							fPage1.storeSettings();
							ITargetLocation[] oldContainers = fTarget.getTargetLocations();
							if (oldContainers == null) {
								fTarget.setTargetLocations(new ITargetLocation[] {container});
							} else {
								ITargetLocation[] newContainers = new ITargetLocation[oldContainers.length + 1];
								System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
								newContainers[newContainers.length - 1] = container;
								fTarget.setTargetLocations(newContainers);
							}
						}
						return true;
					}
				};
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		});
		// Feature Containers
		standardChoices.add(new AbstractBundleContainerNode(Messages.AddBundleContainerSelectionPage_9, Messages.AddBundleContainerSelectionPage_10, PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_FEATURE_OBJ)) {
			@Override
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					@Override
					public void addPages() {
						IDialogSettings settings = PDEPlugin.getDefault().getDialogSettings().getSection(SETTINGS_SECTION);
						if (settings == null) {
							settings = PDEPlugin.getDefault().getDialogSettings().addNewSection(SETTINGS_SECTION);
						}
						setDialogSettings(settings);
						addPage(new AddFeatureContainersPage());
					}

					@Override
					public boolean performFinish() {
						try {
							ITargetLocation[] containers = ((AddFeatureContainersPage) getPages()[0]).getBundleContainers();
							if (containers != null) {
								((AddFeatureContainersPage) getPages()[0]).storeSettings();
								ITargetLocation[] oldContainers = fTarget.getTargetLocations();
								if (oldContainers == null) {
									fTarget.setTargetLocations(containers);
								} else {
									ITargetLocation[] newContainers = new ITargetLocation[oldContainers.length + containers.length];
									System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
									System.arraycopy(containers, 0, newContainers, oldContainers.length, containers.length);
									fTarget.setTargetLocations(newContainers);
								}
							}
							return true;
						} catch (CoreException e) {
							setErrorMessage(e.getMessage());
							return false;
						}
					}
				};
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		});
		return standardChoices;
	}

	/**
	 * Returns a list of choices created from the ITargetProvisioner extension
	 * The extension point was deprecated in 3.5 but we need to retain some compatibility.
	 * @return list of wizard nodes
	 */
	private List<AbstractBundleContainerNode> getTargetLocationProvisionerChoices() {
		List<AbstractBundleContainerNode> list = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), TARGET_LOCATION_PROVISIONER_POINT);
		if (point == null)
			return list;
		IExtension[] extensions = point.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element2 : elements) {
				WizardElement element = createWizardElement(element2);
				if (element != null) {
					final String pluginId = element.getPluginId();
					final String contributionId = element.getID();
					IPluginContribution pc = new IPluginContribution() {
						@Override
						public String getLocalId() {
							return contributionId;
						}

						@Override
						public String getPluginId() {
							return pluginId;
						}
					};
					if (!WorkbenchActivityHelper.filterItem(pc)) {
						AbstractBundleContainerNode wizardNode = createTargetLocationProvisionerNode(element);
						wizardNode.setPreferredOption(InstallableUnitWizard.CONTRIBUTION_ID.equals(contributionId));
						list.add(wizardNode);
					}
				}
			}
		}
		return list;
	}

	/**
	 * Returns a list of choices created from the ITargetProvisioner extension
	 * The extension point was deprecated in 3.5 but we need to retain some compatibility.
	 * @return list of wizard nodes
	 */
	private List<AbstractBundleContainerNode> getTargetProvisionerChoices() {
		List<AbstractBundleContainerNode> list = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), TARGET_PROVISIONER_POINT);
		if (point == null)
			return list;
		IExtension[] extensions = point.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] confElements = extension.getConfigurationElements();
			for (IConfigurationElement confElement : confElements) {
				WizardElement element = createWizardElement(confElement);
				if (element != null) {
					final String pluginId = element.getPluginId();
					final String contributionId = element.getID();
					IPluginContribution pc = new IPluginContribution() {
						@Override
						public String getLocalId() {
							return contributionId;
						}

						@Override
						public String getPluginId() {
							return pluginId;
						}
					};
					if (!WorkbenchActivityHelper.filterItem(pc)) {
						list.add(createDeprecatedExtensionNode(element));
					}
				}
			}
		}
		return list;
	}

	/**
	 * Returns a Wizard element representing an extension contributed wizard
	 * @param config config for the extensino
	 * @return new wizard element
	 */
	protected WizardElement createWizardElement(IConfigurationElement config) {
		String name = config.getAttribute(WizardElement.ATT_NAME);
		String id = config.getAttribute(WizardElement.ATT_ID);
		if (name == null || id == null)
			return null;
		WizardElement element = new WizardElement(config);

		String imageName = config.getAttribute(WizardElement.ATT_ICON);
		Image image = null;
		if (imageName != null) {
			String pluginID = config.getNamespaceIdentifier();
			image = PDEPlugin.getDefault().getLabelProvider().getImageFromPlugin(pluginID, imageName);
		}
		element.setImage(image);
		return element;
	}

	/**
	 * Creates a wizard node that will get the pages from the contributed wizard and create a directory bundle container from the result
	 * @param element wizard element representing the extension
	 * @return wizard node
	 */
	private AbstractBundleContainerNode createTargetLocationProvisionerNode(final WizardElement element) {
		return new AbstractBundleContainerNode(element.getLabel(), element.getDescription(), element.getImage()) {
			@Override
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					private ITargetLocationWizard fWizard;

					@Override
					public void addPages() {
						try {
							fWizard = (ITargetLocationWizard) element.createExecutableExtension();
						} catch (CoreException e) {
							PDEPlugin.log(e);
							MessageDialog.openError(getContainer().getShell(), Messages.Errors_CreationError, Messages.Errors_CreationError_NoWizard);
						}
						fWizard.setTarget(fTarget);
						fWizard.setContainer(getContainer());
						fWizard.addPages();
						IWizardPage[] pages = fWizard.getPages();
						for (IWizardPage page : pages)
							addPage(page);
					}

					@Override
					public boolean performFinish() {
						if (fWizard != null) {
							if (!fWizard.performFinish()) {
								return false;
							}
							ITargetLocation[] locations = fWizard.getLocations();
							if (locations != null) {
								ITargetLocation[] oldContainers = fTarget.getTargetLocations();
								if (oldContainers == null) {
									fTarget.setTargetLocations(locations);
								} else {
									ITargetLocation[] newContainers = new ITargetLocation[oldContainers.length + locations.length];
									System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
									System.arraycopy(locations, 0, newContainers, oldContainers.length, locations.length);
									fTarget.setTargetLocations(newContainers);
								}
							}
						}
						return true;
					}
				};
				wizard.setContainer(getContainer());
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		};
	}

	/**
	 * Creates a wizard node that will get the pages from the contributed wizard and create a directory bundle container from the result
	 * @param element wizard element representing the extension
	 * @return wizard node
	 */
	private AbstractBundleContainerNode createDeprecatedExtensionNode(final WizardElement element) {
		return new AbstractBundleContainerNode(element.getLabel(), element.getDescription(), element.getImage()) {
			@Override
			public IWizard createWizard() {
				Wizard wizard = new Wizard() {
					private IProvisionerWizard fWizard;

					@Override
					public void addPages() {
						try {
							fWizard = (IProvisionerWizard) element.createExecutableExtension();
						} catch (CoreException e) {
							PDEPlugin.log(e);
							MessageDialog.openError(getContainer().getShell(), Messages.Errors_CreationError, Messages.Errors_CreationError_NoWizard);
						}
						fWizard.setContainer(getContainer());
						fWizard.addPages();
						IWizardPage[] pages = fWizard.getPages();
						for (IWizardPage page : pages)
							addPage(page);
					}

					@Override
					public boolean performFinish() {
						if (fWizard != null) {
							if (!fWizard.performFinish()) {
								return false;
							}
							File[] dirs = fWizard.getLocations();
							for (int i = 0; i < dirs.length; i++) {
								if (dirs[i] == null || !dirs[i].isDirectory()) {
									ErrorDialog.openError(getShell(), Messages.AddBundleContainerSelectionPage_0, Messages.AddBundleContainerSelectionPage_5, new Status(IStatus.ERROR, PDEPlugin.getPluginId(), Messages.AddDirectoryContainerPage_6));
									return false;
								}
								try {
									// First try the specified dir, then try the plugins dir
									ITargetLocation container = getTargetPlatformService().newDirectoryLocation(dirs[i].getPath());
									ITargetLocation[] oldContainers = fTarget.getTargetLocations();
									if (oldContainers == null) {
										fTarget.setTargetLocations(new ITargetLocation[] {container});
									} else {
										ITargetLocation[] newContainers = new ITargetLocation[oldContainers.length + 1];
										System.arraycopy(oldContainers, 0, newContainers, 0, oldContainers.length);
										newContainers[oldContainers.length] = container;
										fTarget.setTargetLocations(newContainers);
									}
								} catch (CoreException ex) {
									ErrorDialog.openError(getShell(), Messages.AddBundleContainerSelectionPage_0, Messages.AddBundleContainerSelectionPage_5, ex.getStatus());
									return false;
								}
							}
						}
						return true;
					}
				};
				wizard.setContainer(getContainer());
				wizard.setWindowTitle(Messages.AddBundleContainerSelectionPage_1);
				return wizard;
			}
		};
	}

	/**
	 * Abstract implementation of the IWizardNode interface providing a consistent look and feel
	 * for the table displaying a list of possible bundle container types.
	 */
	abstract class AbstractBundleContainerNode implements IWizardNode {
		private String fTypeName;
		private String fTypeDescription;
		private Image fTypeImage;
		private IWizard fWizard;
		private boolean fPreferredOption;

		public AbstractBundleContainerNode(String name, String description, Image image) {
			fTypeName = name;
			fTypeDescription = description;
			fTypeImage = image;
		}

		public abstract IWizard createWizard();

		void setPreferredOption(boolean preferredOption) {
			fPreferredOption = preferredOption;
		}

		boolean isPreferredOption() {
			return fPreferredOption;
		}

		@Override
		public void dispose() {
			if (fWizard != null) {
				fWizard.dispose();
				fWizard = null;
			}
		}

		@Override
		public Point getExtent() {
			return new Point(-1, -1);
		}

		@Override
		public IWizard getWizard() {
			if (fWizard == null) {
				fWizard = createWizard();
			}
			return fWizard;
		}

		@Override
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
