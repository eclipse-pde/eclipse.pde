/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Jakub Jurkiewicz <jakub.jurkiewicz@pl.ibm.com> - bug 185995
 *     Rudiger Herrmann <rherrmann@innoopract.com> - bug 249707
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.bndtools.templating.Template;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.bnd.ui.templating.RepoTemplateLabelProvider;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.internal.ui.wizards.WizardListSelectionPage;
import org.eclipse.pde.internal.ui.wizards.WizardNode;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class TemplateListSelectionPage extends WizardListSelectionPage {
	private final ContentPage fContentPage;
	private Button fUseTemplate;
	private String fInitialTemplateId;
	private Map<Template, CompletableFuture<String>> templateTextLoadings = new HashMap<>();

	class WizardFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			PluginFieldData data = (PluginFieldData) fContentPage.getData();
			boolean simple = data.isSimple();
			boolean generate = data.doGenerateClass();
			boolean ui = data.isUIPlugin();
			boolean rcp = data.isRCPApplicationPlugin();
			boolean osgi = data.getOSGiFramework() != null;
			boolean automatic = osgi && data.isAutomaticMetadataGeneration()
					&& !data.getOSGiFramework().equals(ICoreConstants.EQUINOX);
			WizardElement welement = (WizardElement) element;
			boolean active = TemplateWizardHelper.isActive(welement);
			boolean uiFlag = welement.getFlag(TemplateWizardHelper.FLAG_UI, true);
			boolean javaFlag = welement.getFlag(TemplateWizardHelper.FLAG_JAVA, true);
			boolean rcpFlag = welement.getFlag(TemplateWizardHelper.FLAG_RCP, false);
			boolean osgiFlag = welement.getFlag(TemplateWizardHelper.FLAG_OSGI, false);
			boolean activatorFlag = welement.getFlag(TemplateWizardHelper.FLAG_ACTIVATOR, false);
			boolean requireBnd = welement.getFlag(TemplateWizardHelper.FLAG_BND, false);

			//filter out wizards from disabled activities
			if (!active)
				return false;
			// filter out items that require bnd but not having automatic
			// enabled
			if (automatic) {
				return !simple && requireBnd;
			} else if (requireBnd) {
				// if BND is required this can't work otherwise!
				return false;
			}
			//osgi projects need java
			if (osgi && simple)
				return false;
			//filter out java wizards for simple projects
			if (simple)
				return !javaFlag;
			//filter out ui wizards for non-ui plug-ins
			if (uiFlag && !ui)
				return false;
			//filter out wizards that require an activator when the user specifies not to generate a class
			if (activatorFlag && !generate)
				return false;
			//filter out non-RCP wizard if RCP option is selected
			if (!osgi && (rcp != rcpFlag))
				return false;
			//filter out non-UI wizards if UI option is selected for rcp and osgi projects
			return (osgi == osgiFlag && ((!osgiFlag && !rcpFlag) || ui == uiFlag));
		}

	}

	/**
	 * Constructor
	 * @param wizardElements a list of TemplateElementWizard objects
	 * @param page content wizard page
	 * @param message message to provide to the user
	 */
	public TemplateListSelectionPage(ElementList wizardElements, ContentPage page, String message) {
		super(wizardElements, message);
		fContentPage = page;
		setTitle(PDEUIMessages.WizardListSelectionPage_title);
		setDescription(PDEUIMessages.WizardListSelectionPage_desc);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		wizardSelectionViewer.setLabelProvider(new RepoTemplateLabelProvider() {
			@Override
			public String getText(Object element) {
				return ListUtil.TABLE_LABEL_PROVIDER.getText(element);
			}

			@Override
			public Image getImage(Object element) {
				return ListUtil.TABLE_LABEL_PROVIDER.getImage(element);
			}
		});
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE);
	}

	@Override
	public void createAbove(Composite container, int span) {
		fUseTemplate = new Button(container, SWT.CHECK);
		fUseTemplate.setText(PDEUIMessages.WizardListSelectionPage_label);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		fUseTemplate.setLayoutData(gd);
		fUseTemplate.addSelectionListener(widgetSelectedAdapter(e -> {
			wizardSelectionViewer.getControl().setEnabled(fUseTemplate.getSelection());
			if (!fUseTemplate.getSelection())
				setDescription(""); //$NON-NLS-1$
			else
				setDescription(PDEUIMessages.WizardListSelectionPage_desc);
			setDescriptionEnabled(fUseTemplate.getSelection());
			getContainer().updateButtons();
		}));
		fUseTemplate.setSelection(false);
	}

	@Override
	protected void initializeViewer() {
		wizardSelectionViewer.addFilter(new WizardFilter());
		if (getInitialTemplateId() != null)
			selectInitialTemplate();
		setDescriptionEnabled(false);
	}

	private void selectInitialTemplate() {
		Object[] children = wizardElements.getChildren();
		for (Object child : children) {
			WizardElement welement = (WizardElement) child;
			if (welement.getID().equals(getInitialTemplateId())) {
				wizardSelectionViewer.setSelection(new StructuredSelection(welement), true);
				setSelectedNode(createWizardNode(welement));
				setDescriptionText(welement.getDescription());
				break;
			}
		}
	}

	@Override
	protected String getWizardDescription(WizardElement element) {
		Template template = Adapters.adapt(element, Template.class);
		if (template != null) {
			URI helpContent = template.getHelpContent();
			if (helpContent != null) {
				CompletableFuture<String> future = templateTextLoadings.computeIfAbsent(template, tmpl -> {
					CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> {
						try (InputStream stream = helpContent.toURL().openStream()) {
							return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
						} catch (IOException e) {
							e.printStackTrace();
							return PDEUIMessages.BaseWizardSelectionPage_noDesc;
						}
					});
					f.thenAcceptAsync(helpText -> {
						if (wizardSelectionViewer.getStructuredSelection().getFirstElement() == element) {
							setDescriptionText(helpText);
						}
					}, wizardSelectionViewer.getControl().getDisplay());
					return f;
				});
				try {
					return future.getNow(PDEUIMessages.BaseWizardSelectionPage_loadingDesc);
				} catch (CancellationException | CompletionException e) {
					// fall through to show no help ...
				}
			}
			return PDEUIMessages.BaseWizardSelectionPage_noDesc;
		}
		return super.getWizardDescription(element);
	}

	@Override
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			@Override
			public IBasePluginWizard createWizard() throws CoreException {
				IPluginContentWizard wizard = (IPluginContentWizard) wizardElement.createExecutableExtension();
				wizard.init(fContentPage.getData());
				return wizard;
			}
		};
	}

	@Override
	public IPluginContentWizard getSelectedWizard() {
		if (fUseTemplate.getSelection())
			return super.getSelectedWizard();
		return null;
	}

	@Override
	public boolean isPageComplete() {
		PluginFieldData data = (PluginFieldData) fContentPage.getData();
		boolean rcp = data.isRCPApplicationPlugin();

		return !rcp || (fUseTemplate.getSelection() && rcp && getSelectedNode() != null);
	}

	@Override
	public boolean canFlipToNextPage() {
		IStructuredSelection ssel = wizardSelectionViewer.getStructuredSelection();
		return fUseTemplate.getSelection() && ssel != null && !ssel.isEmpty();
	}

	/**
	 * @return Returns the fInitialTemplateId.
	 */
	public String getInitialTemplateId() {
		return fInitialTemplateId;
	}

	/**
	 * @param initialTemplateId The fInitialTemplateId to set.
	 */
	public void setInitialTemplateId(String initialTemplateId) {
		fInitialTemplateId = initialTemplateId;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			fContentPage.updateData();
			if (((PluginFieldData) fContentPage.getData()).isRCPApplicationPlugin()) {
				fUseTemplate.setSelection(true);
				fUseTemplate.setEnabled(false);
				fUseTemplate.setVisible(false);
				wizardSelectionViewer.getControl().setEnabled(true);
				setDescriptionEnabled(true);
			} else {
				fUseTemplate.setVisible(true);
				if (fUseTemplate.getSelection() == false)
					wizardSelectionViewer.getControl().setEnabled(false);
				else
					setDescriptionEnabled(true);
				fUseTemplate.setEnabled(true);
			}
			wizardSelectionViewer.refresh();
		}
		super.setVisible(visible);
	}

	/**
	 * @return Returns <code>false</code> if no Template is available,
	 * and <code>true</code> otherwise.
	 */
	public boolean isAnyTemplateAvailable() {
		if (wizardSelectionViewer != null) {
			wizardSelectionViewer.refresh();
			Object firstElement = wizardSelectionViewer.getElementAt(0);
			if (firstElement != null) {
				return true;
			}
		}
		return false;
	}
}