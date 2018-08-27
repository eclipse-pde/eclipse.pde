/*******************************************************************************
 * Copyright (c) 2016 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.wizards.ResizableWizardDialog;
import org.eclipse.pde.internal.ui.wizards.tools.ConvertPreferencesWizard;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.*;

public class PreferencesSection extends PDESection {
	private static final String PREFS_CUSTOMIZATION_FILE = "plugin_customization.ini"; //$NON-NLS-1$
	private static final String EXTENSION_PREFS_CUSTOMIZATION = "preferenceCustomization"; //$NON-NLS-1$
	public static final String EXTENSION_PRODUCT = "org.eclipse.core.runtime.products"; //$NON-NLS-1$
	public static final String ELEMENT_PRODUCT = "product"; //$NON-NLS-1$

	FormText configText;

	public PreferencesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		section.setLayoutData(data);

		section.setText(PDEUIMessages.PreferencesSection_title);
		section.setDescription(PDEUIMessages.PreferencesSection_description);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 1));
		client.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));

		configText = toolkit.createFormText(client, true);
		String config = getWizardConfigText();
		configText.setText(config, true, true);
		data = new TableWrapData(TableWrapData.FILL_GRAB);
		configText.setLayoutData(data);

		configText.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkEntered(HyperlinkEvent e) {
				IStatusLineManager mng = getPage().getEditor().getEditorSite().getActionBars().getStatusLineManager();
				mng.setMessage(e.getLabel());
			}

			@Override
			public void linkExited(HyperlinkEvent e) {
				IStatusLineManager mng = getPage().getEditor().getEditorSite().getActionBars().getStatusLineManager();
				mng.setMessage(null);
			}

			@Override
			public void linkActivated(HyperlinkEvent e) {
				String href = (String) e.getHref();
				if (href.equals("command.generate")) { //$NON-NLS-1$
					handleGenerate();
				} else if (href.equals("navigate.overview")) { //$NON-NLS-1$
					getPage().getEditor().setActivePage(OverviewPage.PAGE_ID);
				}
			}
		});

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	private String getWizardConfigText() {
		IPreferencesInfo info = getPreferencesInfo();
		String[] bindings = new String[2];
		bindings[0] = info.getSourceFilePath() == null ? null : TextProcessor.process(info.getSourceFilePath());
		bindings[1] = info.getPreferenceCustomizationPath() == null ? null : TextProcessor.process(info.getPreferenceCustomizationPath());

		boolean isOverwrite = getOverwrite();
		if (bindings[0] == null && bindings[1] == null)
			return isOverwrite ? PDEUIMessages.PreferencesSection_generate_overwrite1 : PDEUIMessages.PreferencesSection_generate_merge1;
		if (bindings[0] == null && bindings[1] != null)
			return isOverwrite ? NLS.bind(PDEUIMessages.PreferencesSection_generate_overwrite2, bindings[1]) : NLS.bind(PDEUIMessages.PreferencesSection_generate_merge2, bindings[1]);
		if (bindings[0] != null && bindings[1] == null)
			return isOverwrite ? NLS.bind(PDEUIMessages.PreferencesSection_generate_overwrite3, bindings[0]) : NLS.bind(PDEUIMessages.PreferencesSection_generate_merge3, bindings[0]);
		if (bindings[0] != null && bindings[1] != null)
			return isOverwrite ? NLS.bind(PDEUIMessages.PreferencesSection_generate_overwrite4, bindings) : NLS.bind(PDEUIMessages.PreferencesSection_generate_merge4, bindings);
		return null;
	}


	void handleGenerate() {
		String preferenceCustomizationPath = null;
		String id = getProduct().getDefiningPluginId();
		IPluginModelBase model = PluginRegistry.findModel(id);
		if (model == null || model.getUnderlyingResource() == null) {
			MessageDialog.openError(getSection().getShell(), NLS.bind(PDEUIMessages.PreferencesSection_errorNoDefiningPluginTitle, id == null ? "" : id), PDEUIMessages.PreferencesSection_errorNoDefiningPlugin); //$NON-NLS-1$
			return;
		}
		// 1 - First look for a plugin_customization.ini defined in the product model.
		IProject project = model.getUnderlyingResource().getProject();
		IPreferencesInfo info = getPreferencesInfo();

		if (info != null) {
			preferenceCustomizationPath = info.getPreferenceCustomizationPath();
		}
		if (preferenceCustomizationPath == null || preferenceCustomizationPath.length() == 0) {
			// 2 - None was found in product. See if one has been defined in the plugin extension point

			IPluginExtension productExtension = findProductExtension(model);
			if (productExtension != null) {
				// Find the product element
				IPluginElement productElement = findProductElement(productExtension);
				if (productElement != null) {
					// Find the preference customization property
					IPluginElement propertyElement = findPrefCustPropertyElement(productElement);
					if (propertyElement != null) {
						IPluginAttribute valueAttribute = propertyElement.getAttribute("value"); //$NON-NLS-1$
						preferenceCustomizationPath = valueAttribute.getValue();
						// get the path with respect to root
						if (PREFS_CUSTOMIZATION_FILE.equals(preferenceCustomizationPath)) {
							IResource resource = project.findMember(PREFS_CUSTOMIZATION_FILE);
							boolean existing = resource != null && resource instanceof IFile;
							preferenceCustomizationPath = existing ? ((IFile) resource).getFullPath().toString() : null;
						}
					}
				}
			}
		}
		try {
			if (preferenceCustomizationPath == null || preferenceCustomizationPath.length() == 0) {
				// 3 - If we don't have a file path defined in the extension point, look for one in the defining plugin.
				IResource resource = project.findMember(PREFS_CUSTOMIZATION_FILE);
				boolean existing = resource != null && resource instanceof IFile;
				if (existing) {
					preferenceCustomizationPath = ((IFile) resource).getFullPath().toString();
				} else {
					// Looks like we need to create one. Create plugin_customization.ini as an empty file at project root
					IFile customFile = project.getFile(PREFS_CUSTOMIZATION_FILE);
					preferenceCustomizationPath = customFile.getFullPath().toString();
					byte[] bytes = "".getBytes(); //$NON-NLS-1$
					InputStream source = new ByteArrayInputStream(bytes);
					customFile.create(source, IResource.NONE, null);
				}
			}

			// Now get a resource from whichever path we found and ensure one last time that it exists. For example, a path might
			// have been referenced in the products extension and subsequently deleted.
			IFile customizationFile = project.getWorkspace().getRoot().getFile(new Path(preferenceCustomizationPath));
			if (!customizationFile.exists()) {
				byte[] bytes = "".getBytes(); //$NON-NLS-1$
				InputStream source = new ByteArrayInputStream(bytes);
				customizationFile.create(source, IResource.NONE, null);
			}
			// Ensure that any of these paths (existing or not) is in the build.properties
			IFile buildProps = PDEProject.getBuildProperties(project);
			if (buildProps.exists()) {
				WorkspaceBuildModel wkspc = new WorkspaceBuildModel(buildProps);
				wkspc.load();
				if (wkspc.isLoaded()) {
					IBuildEntry entry = wkspc.getBuild().getEntry("bin.includes"); //$NON-NLS-1$
					if (entry == null) {
						entry = wkspc.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
						wkspc.getBuild().add(entry);
					}
					if (!entry.contains(preferenceCustomizationPath))
						entry.addToken(preferenceCustomizationPath);
					wkspc.save();
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e, null, NLS.bind(PDEUIMessages.PreferencesSection_errorReading, preferenceCustomizationPath));
			return;
		}

		ConvertPreferencesWizard wizard = new ConvertPreferencesWizard(preferenceCustomizationPath, getPreferencesInfo().getSourceFilePath(), getOverwrite());
		WizardDialog wd = new ResizableWizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		wd.create();
		if (wd.open() == Window.OK) {
			info = getPreferencesInfo();
			preferenceCustomizationPath = info.getPreferenceCustomizationPath();
			info.setPreferenceCustomizationPath(wizard.getPluginCustomizationFilePath());
			info.setSourceFilePath(wizard.getPreferencesFilePath());
			info.setOverwrite(Boolean.toString(wizard.getOverwrite()));
			configText.setText(getWizardConfigText(), true, true);
			getSection().getParent().layout();
		}
	}

	private boolean getOverwrite() {
		boolean overwrite = false;
		if (getPreferencesInfo().getOverwrite() != null) {
			overwrite = Boolean.parseBoolean(getPreferencesInfo().getOverwrite());
		}
		return overwrite;
	}

	private IPluginElement findPrefCustPropertyElement(IPluginElement productElement) {
		// Ensure the produce element has children
		if (productElement.getChildCount() == 0) {
			return null;
		}
		// Get the product element children
		IPluginObject[] objects = productElement.getChildren();
		// Process all children
		for (IPluginObject object : objects) {
			// Ensure we have an element
			if ((object instanceof IPluginElement) == false) {
				continue;
			}
			// Property elements are the only legitimate children of product elements
			if (object.getName().equals("property") == false) { //$NON-NLS-1$
				continue;
			}
			IPluginElement element = (IPluginElement) object;
			// Get the name
			IPluginAttribute nameAttribute = element.getAttribute("name"); //$NON-NLS-1$
			// Ensure we have a preference customization property
			if (nameAttribute == null) {
				continue;
			} else if (PDETextHelper.isDefined(nameAttribute.getValue()) == false) {
				continue;
			} else if (nameAttribute.getValue().equals(EXTENSION_PREFS_CUSTOMIZATION) == false) {
				continue;
			}

			return element;
		}
		return null;
	}

	private IPluginElement findProductElement(IPluginExtension extension) {
		// The product extension is only allowed one child
		if (extension.getChildCount() != 1) {
			return null;
		}
		// Get the one child
		IPluginObject pluginObject = extension.getChildren()[0];
		// Ensure that the child is an element
		if ((pluginObject instanceof IPluginElement) == false) {
			return null;
		}
		// Ensure that the child is a product element
		if (pluginObject.getName().equals(ELEMENT_PRODUCT) == false) {
			return null;
		}
		return (IPluginElement) pluginObject;
	}

	private IPluginExtension findProductExtension(IPluginModelBase model) {
		// Get all the extensions
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		// Get the extension matching the product extension point ID
		// and product ID
		for (IPluginExtension extension : extensions) {
			// Get the extension point
			String point = extension.getPoint();
			// Ensure we have a product extension
			if (point.equals(EXTENSION_PRODUCT) == false) {
				continue;
			}
			// Ensure we have the exact product
			// Get the fully qualified product ID
			String id = model.getPluginBase().getId() + '.' + extension.getId();
			if (id.equals(getProduct().getId()) == false) {
				continue;
			}
			return extension;
		}
		return null;
	}

	private IPreferencesInfo getPreferencesInfo() {
		IPreferencesInfo info = getProduct().getPreferencesInfo();
		if (info == null) {
			info = getModel().getFactory().createPreferencesInfo();
			getProduct().setPreferencesInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}


	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}
}
