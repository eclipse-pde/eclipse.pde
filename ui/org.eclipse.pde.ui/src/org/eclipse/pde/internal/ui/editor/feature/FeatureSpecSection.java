/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.dialogs.PluginSelectionDialog;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.plugin.NewPluginProjectWizard;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class FeatureSpecSection extends PDESection {
	private FormEntry fIdText;

	private FormEntry fTitleText;

	private FormEntry fVersionText;

	private FormEntry fProviderText;

	private FormEntry fPluginText;

	private FormEntry fUpdateSiteNameText;

	private FormEntry fUpdateSiteUrlText;

	private FormEntry fPatchedIdText;

	private FormEntry fPatchedVersionText;

	private boolean fPatch = false;

	public FeatureSpecSection(FeatureFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.FeatureEditor_SpecSection_title);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		fTitleText.commit();
		fProviderText.commit();
		fIdText.commit();
		fPluginText.commit();
		fVersionText.commit();
		if (fPatchedIdText != null) {
			fPatchedIdText.commit();
			fPatchedVersionText.commit();
		}
		fUpdateSiteUrlText.commit();
		fUpdateSiteNameText.commit();
		super.commit(onSave);
	}

	private void commitSiteUrl(String value) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		IFeatureURL urlElement = feature.getURL();
		if (urlElement == null) {
			urlElement = model.getFactory().createURL();
			try {
				feature.setURL(urlElement);
			} catch (CoreException e) {
				return;
			}
		}
		try {
			IFeatureURLElement updateElement = urlElement.getUpdate();
			if (value.length() > 0) {
				URL siteUrl = new URL(value);
				if (updateElement == null) {
					// element needed, create it
					updateElement = model.getFactory().createURLElement(urlElement, IFeatureURLElement.UPDATE);
					updateElement.setURL(siteUrl);
					urlElement.setUpdate(updateElement);
				} else {
					updateElement.setURL(siteUrl);
				}
			} else {
				if (updateElement == null) {
					// do nothing
				} else {
					if (updateElement.getLabel() != null && updateElement.getLabel().length() > 0) {
						updateElement.setURL(null);
					} else {
						// element not needed, remove it
						urlElement.setUpdate(null);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		}
	}

	private void commitSiteName(String value) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		IFeatureURL urlElement = feature.getURL();
		if (urlElement == null) {
			urlElement = model.getFactory().createURL();
			try {
				feature.setURL(urlElement);
			} catch (CoreException e) {
				return;
			}
		}
		try {
			IFeatureURLElement updateElement = urlElement.getUpdate();
			if (value.length() > 0) {
				if (updateElement == null) {
					// element needed, create it
					updateElement = model.getFactory().createURLElement(urlElement, IFeatureURLElement.UPDATE);
					updateElement.setLabel(value);
					// URL not set, so element will be flagged during validation
					urlElement.setUpdate(updateElement);
				} else {
					updateElement.setLabel(value);
				}
			} else {
				if (updateElement == null) {
					// do nothing
				} else {
					if (updateElement.getURL() != null) {
						updateElement.setLabel(null);
					} else {
						// element not needed, remove it
						urlElement.setUpdate(null);
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	/**
	 * Obtains or creates a feature import with patch="true"
	 */
	private IFeatureImport getPatchedFeature() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			if (imports[i].isPatch()) {
				return imports[i];
			}
		}
		// need to recreate the import element
		FeatureImport fimport = (FeatureImport) model.getFactory().createImport();
		try {
			fimport.setType(IFeatureImport.FEATURE);
			fimport.setPatch(true);
			feature.addImports(new IFeatureImport[] {fimport});
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		}
		return null;
	}

	private boolean isPatch() {
		return fPatch;
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData twd = new TableWrapData();
		twd.grabHorizontal = true;
		section.setLayoutData(twd);

		fPatch = ((FeatureEditor) getPage().getEditor()).isPatchEditor();

		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		if (isPatch()) {
			getSection().setDescription(PDEUIMessages.FeatureEditor_SpecSection_desc_patch);
		} else {
			getSection().setDescription(PDEUIMessages.FeatureEditor_SpecSection_desc);
		}

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fIdText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_id, null, false);
		fIdText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setId(text.getValue().trim());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		fVersionText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_version, null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				if (verifySetVersion(feature, text.getValue()) == false) {
					warnBadVersionFormat(text.getValue());
					text.setValue(feature.getVersion());
				}
			}
		});

		fTitleText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_name, null, false);
		fTitleText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setLabel(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
				getPage().getManagedForm().getForm().setText(model.getResourceString(feature.getLabel()));
				((FeatureEditor) getPage().getEditor()).updateTitle();
			}
		});
		fProviderText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_provider, null, false);
		fProviderText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					feature.setProviderName((value.length() > 0 ? value : null));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		fPluginText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_plugin, PDEUIMessages.GeneralInfoSection_browse, isEditable());

		fPluginText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					feature.setPlugin((value.length() > 0 ? value : null));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}

			public void linkActivated(HyperlinkEvent e) {
				String plugin = fPluginText.getValue();
				if (PluginRegistry.findModel(plugin) == null) {
					createFeaturePlugin();
				}
				ManifestEditor.openPluginEditor(fPluginText.getValue());
			}

			public void browseButtonSelected(FormEntry entry) {
				handleOpenDialog();
			}

			private void createFeaturePlugin() {
				NewPluginProjectWizard wizard = new NewPluginProjectWizard();
				WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
				dialog.create();
				SWTUtil.setDialogSize(dialog, 400, 500);
				if (dialog.open() == Window.OK) {
					String plugin = wizard.getPluginId();
					try {
						feature.setPlugin(plugin);
						fPluginText.setValue(plugin, false);
					} catch (CoreException ce) {
						PDEPlugin.logException(ce);
					}
				}
			}
		});

		if (isPatch()) {
			fPatchedIdText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_patchedId, null, false);
			fPatchedIdText.setFormEntryListener(new FormEntryAdapter(this) {
				public void textValueChanged(FormEntry text) {
					try {
						IFeatureImport patchImport = getPatchedFeature();
						if (patchImport != null) {
							patchImport.setId(text.getValue());
						}
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
				}
			});

			fPatchedVersionText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_patchedVersion, null, false);
			fPatchedVersionText.setFormEntryListener(new FormEntryAdapter(this) {
				public void textValueChanged(FormEntry text) {
					IFeatureImport patchImport = getPatchedFeature();
					if (patchImport != null) {
						if (verifySetVersion(patchImport, text.getValue()) == false) {
							warnBadVersionFormat(text.getValue());
							text.setValue(patchImport.getVersion());
						}
					}
				}
			});

		}

		fUpdateSiteUrlText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_updateUrl, null, false);
		fUpdateSiteUrlText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				String url = text.getValue() != null ? text.getValue() : ""; //$NON-NLS-1$
				if (url.length() > 0 && !verifySiteUrl(feature, url)) {
					warnBadUrl(url);
					setUpdateSiteUrlText();
				} else {
					commitSiteUrl(url);
				}
			}
		});

		fUpdateSiteNameText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_updateUrlLabel, null, false);
		fUpdateSiteNameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				String name = text.getValue() != null ? text.getValue() : ""; //$NON-NLS-1$
				commitSiteName(name);
			}
		});

		GridData gd = (GridData) fIdText.getText().getLayoutData();
		gd.widthHint = 150;

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private boolean verifySetVersion(IFeature feature, String value) {
		try {
			if (VersionUtil.validateVersion(value).isOK())
				feature.setVersion(value);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean verifySetVersion(IFeatureImport featureImport, String value) {
		try {
			if (VersionUtil.validateVersion(value).isOK())
				featureImport.setVersion(value);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean verifySiteUrl(IFeature feature, String value) {
		try {
			new URL(value);
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	private void warnBadVersionFormat(String text) {
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureEditor_SpecSection_badVersionTitle, PDEUIMessages.FeatureEditor_SpecSection_badVersionMessage);
	}

	private void warnBadUrl(String text) {
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureEditor_SpecSection_badUrlTitle, PDEUIMessages.FeatureEditor_SpecSection_badUrlMessage);
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		if (!model.isEditable()) {
			fIdText.getText().setEditable(false);
			fTitleText.getText().setEditable(false);
			fVersionText.getText().setEditable(false);
			fProviderText.getText().setEditable(false);
			fPluginText.getText().setEditable(false);
			if (isPatch()) {
				fPatchedIdText.getText().setEditable(false);
				fPatchedVersionText.getText().setEditable(false);
			}
			fUpdateSiteUrlText.getText().setEditable(false);
			fUpdateSiteNameText.getText().setEditable(false);
		}
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
			return;
		}
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length > 0 && objs[0] instanceof IFeature) {
				markStale();
			}
		}
		if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			Object objs[] = e.getChangedObjects();
			if (objs.length > 0 && objs[0] instanceof IFeatureURL) {
				markStale();
			}
		}
		Object objs[] = e.getChangedObjects();
		if (objs.length > 0 && objs[0] instanceof IFeatureURLElement) {
			markStale();
		}
		if (isPatch() && objs.length > 0 && objs[0] instanceof IFeatureImport) {
			markStale();
		}
	}

	public void setFocus() {
		if (fIdText != null)
			fIdText.getText().setFocus();
	}

	private void setIfDefined(FormEntry formText, String value) {
		if (value != null) {
			formText.setValue(value, true);
		}
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		setIfDefined(fIdText, feature.getId());
		setIfDefined(fTitleText, feature.getLabel());
		getPage().getManagedForm().getForm().setText(model.getResourceString(feature.getLabel()));
		setIfDefined(fVersionText, feature.getVersion());
		setIfDefined(fProviderText, feature.getProviderName());
		setIfDefined(fPluginText, feature.getPlugin());
		if (isPatch()) {
			IFeatureImport featureImport = getPatchedFeature();
			if (featureImport != null) {
				fPatchedIdText.setValue(featureImport.getId() != null ? featureImport.getId() : "", true); //$NON-NLS-1$
				fPatchedVersionText.setValue(featureImport.getVersion() != null ? featureImport.getVersion() : "", true); //$NON-NLS-1$
			} else {
				fPatchedIdText.setValue("", true); //$NON-NLS-1$
				fPatchedVersionText.setValue("", true); //$NON-NLS-1$
			}
		}
		setUpdateSiteUrlText();
		setUpdateSiteNameText();
		super.refresh();
	}

	private void setUpdateSiteUrlText() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		String updateSiteUrl = ""; //$NON-NLS-1$
		IFeatureURL featureUrl = feature.getURL();
		if (featureUrl != null) {
			IFeatureURLElement urlElement = featureUrl.getUpdate();
			if (urlElement != null) {
				updateSiteUrl = urlElement.getURL() != null ? urlElement.getURL().toExternalForm() : null;
			}
		}
		fUpdateSiteUrlText.setValue(updateSiteUrl != null ? updateSiteUrl : "", //$NON-NLS-1$
				true);

	}

	private void setUpdateSiteNameText() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();

		String updateSiteLabel = ""; //$NON-NLS-1$
		IFeatureURL featureUrl = feature.getURL();
		if (featureUrl != null) {
			IFeatureURLElement urlElement = featureUrl.getUpdate();
			if (urlElement != null) {
				updateSiteLabel = urlElement.getLabel();
			}
		}
		fUpdateSiteNameText.setValue(updateSiteLabel != null ? updateSiteLabel : "", true); //$NON-NLS-1$
	}

	public void cancelEdit() {
		fIdText.cancelEdit();
		fTitleText.cancelEdit();
		fVersionText.cancelEdit();
		fProviderText.cancelEdit();
		fPluginText.cancelEdit();
		if (isPatch()) {
			fPatchedIdText.cancelEdit();
			fPatchedVersionText.cancelEdit();
		}
		fUpdateSiteNameText.cancelEdit();
		fUpdateSiteUrlText.cancelEdit();
		super.cancelEdit();
	}

	/**
	 * @see org.eclipse.update.ui.forms.internal.FormSection#canPaste(Clipboard)
	 */
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}

	protected void handleOpenDialog() {
		PluginSelectionDialog dialog = new PluginSelectionDialog(getSection().getShell(), false, false);
		dialog.create();
		if (dialog.open() == Window.OK) {
			IPluginModel model = (IPluginModel) dialog.getFirstResult();
			IPlugin plugin = model.getPlugin();
			fPluginText.setValue(plugin.getId());
		}
	}
}
