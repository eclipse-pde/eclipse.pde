/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureURL;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class FeatureSpecSection extends PDESection {
	public static final String SECTION_TITLE = "FeatureEditor.SpecSection.title"; //$NON-NLS-1$

	public static final String SECTION_DESC = "FeatureEditor.SpecSection.desc"; //$NON-NLS-1$

	public static final String SECTION_DESC_PATCH = "FeatureEditor.SpecSection.desc.patch"; //$NON-NLS-1$

	public static final String SECTION_ID = "FeatureEditor.SpecSection.id"; //$NON-NLS-1$

	public static final String SECTION_PATCHED_ID = "FeatureEditor.SpecSection.patchedId"; //$NON-NLS-1$

	public static final String SECTION_NAME = "FeatureEditor.SpecSection.name"; //$NON-NLS-1$

	public static final String SECTION_VERSION = "FeatureEditor.SpecSection.version"; //$NON-NLS-1$

	public static final String SECTION_PATCHED_VERSION = "FeatureEditor.SpecSection.patchedVersion"; //$NON-NLS-1$

	public static final String SECTION_PROVIDER = "FeatureEditor.SpecSection.provider"; //$NON-NLS-1$

	public static final String SECTION_UPDATE_SITE = "FeatureEditor.SpecSection.updateSite"; //$NON-NLS-1$

	public static final String SECTION_UPDATE_SITE_LABEL = "FeatureEditor.SpecSection.updateUrlLabel"; //$NON-NLS-1$

	public static final String SECTION_UPDATE_SITE_URL = "FeatureEditor.SpecSection.updateUrl"; //$NON-NLS-1$

	public static final String KEY_BAD_VERSION_TITLE = "FeatureEditor.SpecSection.badVersionTitle"; //$NON-NLS-1$

	public static final String KEY_BAD_VERSION_MESSAGE = "FeatureEditor.SpecSection.badVersionMessage"; //$NON-NLS-1$

	public static final String KEY_BAD_URL_TITLE = "FeatureEditor.SpecSection.badUrlTitle"; //$NON-NLS-1$

	public static final String KEY_BAD_URL_MESSAGE = "FeatureEditor.SpecSection.badUrlMessage"; //$NON-NLS-1$

	private FormEntry fIdText;

	private FormEntry fTitleText;

	private FormEntry fVersionText;

	private FormEntry fProviderText;

	private FormEntry fUpdateSiteNameText;

	private FormEntry fUpdateSiteUrlText;

	private FormEntry fPatchedIdText;

	private FormEntry fPatchedVersionText;

	private boolean fPatch = false;

	public FeatureSpecSection(FeatureFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		fTitleText.commit();
		fProviderText.commit();
		fIdText.commit();
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
					updateElement = model.getFactory().createURLElement(
							urlElement, IFeatureURLElement.UPDATE);
					updateElement.setURL(siteUrl);
					urlElement.setUpdate(updateElement);
				} else {
					updateElement.setURL(siteUrl);
				}
			} else {
				if (updateElement == null) {
					// do nothing
				} else {
					if (updateElement.getLabel() != null
							&& updateElement.getLabel().length() > 0) {
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
					updateElement = model.getFactory().createURLElement(
							urlElement, IFeatureURLElement.UPDATE);
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
	 * 
	 * @return
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
		FeatureImport fimport = (FeatureImport) model.getFactory()
				.createImport();
		try {
			fimport.setType(IFeatureImport.FEATURE);
			fimport.setPatch(true);
			feature.addImports(new IFeatureImport[] { fimport });
		} catch (CoreException ce) {
			PDEPlugin.logException(ce);
		}
		return null;
	}

	private boolean isPatch() {
		return fPatch;
	}

	public void createClient(Section section, FormToolkit toolkit) {
		fPatch = ((FeatureEditor) getPage().getEditor()).isPatchEditor();

		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		getSection().setDescription(
				PDEPlugin.getResourceString(isPatch() ? SECTION_DESC_PATCH
						: SECTION_DESC));

		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		fIdText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_ID), null, false);
		fIdText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setId(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		fVersionText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_VERSION), null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				if (verifySetVersion(feature, text.getValue()) == false) {
					warnBadVersionFormat(text.getValue());
					text.setValue(feature.getVersion());
				}
			}
		});

		fTitleText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_NAME), null, false);
		fTitleText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					feature.setLabel(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
				getPage().getManagedForm().getForm().setText(
						model.getResourceString(feature.getLabel()));
				((FeatureEditor) getPage().getEditor()).updateTitle();
			}
		});
		fProviderText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_PROVIDER), null, false);
		fProviderText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					String value = text.getValue();
					feature
							.setProviderName((value.length() > 0 ? value : null));
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		if (isPatch()) {
			fPatchedIdText = new FormEntry(container, toolkit, PDEPlugin
					.getResourceString(SECTION_PATCHED_ID), null, false);
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

			fPatchedVersionText = new FormEntry(container, toolkit, PDEPlugin
					.getResourceString(SECTION_PATCHED_VERSION), null, false);
			fPatchedVersionText
					.setFormEntryListener(new FormEntryAdapter(this) {
						public void textValueChanged(FormEntry text) {
							IFeatureImport patchImport = getPatchedFeature();
							if (patchImport != null) {
								if (verifySetVersion(patchImport, text
										.getValue()) == false) {
									warnBadVersionFormat(text.getValue());
									text.setValue(patchImport.getVersion());
								}
							}
						}
					});

		}

		fUpdateSiteUrlText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_UPDATE_SITE_URL), null, false);
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

		fUpdateSiteNameText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_UPDATE_SITE_LABEL), null, false);
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
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(value);
			feature.setVersion(pvi.toString());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean verifySetVersion(IFeatureImport featureImport, String value) {
		try {
			PluginVersionIdentifier pvi = new PluginVersionIdentifier(value);
			featureImport.setVersion(pvi.toString());
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
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin
				.getResourceString(KEY_BAD_VERSION_TITLE), PDEPlugin
				.getResourceString(KEY_BAD_VERSION_MESSAGE));
	}

	private void warnBadUrl(String text) {
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEPlugin
				.getResourceString(KEY_BAD_URL_TITLE), PDEPlugin
				.getResourceString(KEY_BAD_URL_MESSAGE));
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
		getPage().getManagedForm().getForm().setText(
				model.getResourceString(feature.getLabel()));
		setIfDefined(fVersionText, feature.getVersion());
		setIfDefined(fProviderText, feature.getProviderName());
		if (isPatch()) {
			IFeatureImport featureImport = getPatchedFeature();
			if (featureImport != null) {
				fPatchedIdText.setValue(
						featureImport.getId() != null ? featureImport.getId()
								: "", true); //$NON-NLS-1$
				fPatchedVersionText.setValue(
						featureImport.getVersion() != null ? featureImport
								.getVersion() : "", true); //$NON-NLS-1$
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
				updateSiteUrl = urlElement.getURL() != null ? urlElement
						.getURL().toExternalForm() : null;
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
		fUpdateSiteNameText.setValue(updateSiteLabel != null ? updateSiteLabel
				: "", true); //$NON-NLS-1$
	}

	public void cancelEdit() {
		fIdText.cancelEdit();
		fTitleText.cancelEdit();
		fVersionText.cancelEdit();
		fProviderText.cancelEdit();
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
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(),
				RTFTransfer.getInstance() };
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}
}
