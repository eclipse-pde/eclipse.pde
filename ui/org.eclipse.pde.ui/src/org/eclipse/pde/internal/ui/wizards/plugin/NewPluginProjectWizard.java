/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.stream.Collectors;

import org.bndtools.templating.Template;
import org.bndtools.templating.TemplateLoader;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.IProjectProvider;
import org.eclipse.pde.internal.ui.wizards.NewWizard;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.promise.Promise;
import org.osgi.util.tracker.ServiceTracker;

import aQute.bnd.osgi.Processor;

public class NewPluginProjectWizard extends NewWizard implements IExecutableExtension {
	public static final String PLUGIN_POINT = "pluginContent"; //$NON-NLS-1$
	public static final String TAG_WIZARD = "wizard"; //$NON-NLS-1$
	public static final String DEF_PROJECT_NAME = "project_name"; //$NON-NLS-1$
	public static final String DEF_TEMPLATE_ID = "template-id"; //$NON-NLS-1$

	private IConfigurationElement fConfig;
	private final PluginFieldData fPluginData;
	private IProjectProvider fProjectProvider;
	protected NewProjectCreationPage fMainPage;
	protected PluginContentPage fContentPage;
	private TemplateListSelectionPage fWizardListPage;

	public NewPluginProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWPPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setWindowTitle(PDEUIMessages.NewProjectWizard_title);
		setNeedsProgressMonitor(true);
		fPluginData = new PluginFieldData();
	}

	public NewPluginProjectWizard(String osgiFramework) {
		this();
		fPluginData.setOSGiFramework(osgiFramework);
	}

	@Override
	public void addPages() {
		fMainPage = new NewProjectCreationPage("main", fPluginData, false, getSelection()); //$NON-NLS-1$
		fMainPage.setTitle(PDEUIMessages.NewProjectWizard_MainPage_title);
		fMainPage.setDescription(PDEUIMessages.NewProjectWizard_MainPage_desc);
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname != null) {
			fMainPage.setInitialProjectName(pname);
		}
		addPage(fMainPage);

		fProjectProvider = new IProjectProvider() {
			@Override
			public String getProjectName() {
				return fMainPage.getProjectName();
			}

			@Override
			public IProject getProject() {
				return fMainPage.getProjectHandle();
			}

			@Override
			public IPath getLocationPath() {
				return fMainPage.getLocationPath();
			}
		};

		fContentPage = new PluginContentPage("page2", fProjectProvider, fMainPage, fPluginData); //$NON-NLS-1$

		fWizardListPage = new TemplateListSelectionPage(getAvailableCodegenWizards(), fContentPage, PDEUIMessages.WizardListSelectionPage_templates);
		String tid = getDefaultValue(DEF_TEMPLATE_ID);
		if (tid != null) {
			fWizardListPage.setInitialTemplateId(tid);
		}

		addPage(fContentPage);
		addPage(fWizardListPage);
	}

	@Override
	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return super.canFinish() && page != fMainPage;
	}

	@Override
	public boolean performFinish() {
		try {
			fMainPage.updateData();
			fContentPage.updateData();
			IDialogSettings settings = getDialogSettings();
			if (settings != null) {
				fMainPage.saveSettings(settings);
				fContentPage.saveSettings(settings);
			}
			BasicNewProjectResourceWizard.updatePerspective(fConfig);

			// If the PDE models are not initialized, initialize with option to cancel
			if (!PDECore.getDefault().areModelsInitialized()) {
				try {
					getContainer().run(true, true, monitor -> {
						// Target reloaded method clears existing models (which don't exist currently) and inits them with a progress monitor
						PDECore.getDefault().getModelManager().targetReloaded(monitor);
						if (monitor.isCanceled()) {
							throw new InterruptedException();
						}
					});
				} catch (InterruptedException e) {
					// Target platform will be empty, but project still can be created
				}
			}

			IPluginContentWizard contentWizard = fWizardListPage.getSelectedWizard();
			getContainer().run(false, true, new NewProjectCreationOperation(fPluginData, fProjectProvider, contentWizard));

			IWorkingSet[] workingSets = fMainPage.getSelectedWorkingSets();
			if (workingSets.length > 0) {
				getWorkbench().getWorkingSetManager().addToWorkingSets(fProjectProvider.getProject(), workingSets);
			}

			return true;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		fConfig = config;
	}

	protected WizardElement createWizardElement(IConfigurationElement config) {
		return WizardElement.create(config, WizardElement.ATT_NAME, WizardElement.ATT_ID, WizardElement.ATT_CLASS);
	}

	public ElementList getAvailableCodegenWizards() {
		ElementList wizards = new ElementList("CodegenWizards"); //$NON-NLS-1$
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDEPlugin.getPluginId(), PLUGIN_POINT);
		if (point == null) {
			return wizards;
		}
		IExtension[] extensions = point.getExtensions();
		for (IExtension extension : extensions) {
			IConfigurationElement[] elements = extension.getConfigurationElements();
			for (IConfigurationElement element2 : elements) {
				if (element2.getName().equals(TAG_WIZARD)) {
					WizardElement element = createWizardElement(element2);
					if (element != null) {
						wizards.add(element);
					}
				}
			}
		}
		try {
			IWizardContainer container = getContainer();
			if (container == null) {
				// can happen in tests or when the wizard is not setup
				// properly...
				return wizards;
			}
			container.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					ServiceTracker<TemplateLoader, TemplateLoader> tracker = new ServiceTracker<>(
							PDEPlugin.getDefault().getBundle().getBundleContext(), TemplateLoader.class, null);
					tracker.open();
					try {
						SortedMap<ServiceReference<TemplateLoader>, TemplateLoader> tracked = tracker.getTracked();
						SubMonitor subMonitor = SubMonitor.convert(monitor, PDEUIMessages.NewPluginProjectWizard_0,
								tracked.size());
						Map<String, List<WizardElement>> templatesByCategory = new HashMap<>();
						for (Entry<ServiceReference<TemplateLoader>, TemplateLoader> entry : tracked.entrySet()) {
							ServiceReference<TemplateLoader> reference = entry.getKey();
							TemplateLoader templateLoader = entry.getValue();
							String label = (String) reference.getProperty(Constants.SERVICE_DESCRIPTION);
							if (label == null) {
								label = (String) reference.getProperty("component.name"); //$NON-NLS-1$
							}
							if (label == null) {
								label = "Template Loader  " + templateLoader.getClass().getSimpleName(); //$NON-NLS-1$
							}
							subMonitor.subTask(label);
							Promise<? extends Collection<Template>> templates = templateLoader.findTemplates("project", //$NON-NLS-1$
									new Processor());
							Collection<Template> loadedTemplates = templates.getValue();
							int templ = 0;
							for (Template template : loadedTemplates) {
								WizardElement element = WizardElement.create(template,
										templateLoader.getClass().getName() + "." + (templ++)); //$NON-NLS-1$
								if (element != null) {
									templatesByCategory.computeIfAbsent(
											Objects.requireNonNullElse(template.getCategory(), ""), //$NON-NLS-1$
											nil -> new ArrayList<>()).add(element);
								}
							}
						}
						for (List<WizardElement> list : templatesByCategory.values()) {
							list.stream().collect(Collectors.groupingBy(WizardElement::getName)).values().stream()
									.map(elements -> elements.stream()
											.max(Comparator.comparing(WizardElement::getVersion)).orElse(null))
									.filter(Objects::nonNull).forEach(wizards::add);
						}
					} finally {
						tracker.close();
					}
				}
			});
		} catch (InvocationTargetException e) {
			PDEPlugin.getDefault().getLog().error("Loading Templates from OSGi registry failed", e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			// canceled
		}
		return wizards;
	}

	public String getPluginId() {
		return fPluginData.getId();
	}

	public String getPluginVersion() {
		return fPluginData.getVersion();
	}

}
