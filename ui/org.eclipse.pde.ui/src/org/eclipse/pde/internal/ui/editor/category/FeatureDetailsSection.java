/*******************************************************************************
 * Copyright (c) 2019 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.osgi.framework.Version;

public class FeatureDetailsSection extends PDESection implements IPartSelectionListener {

	private static final String PROPERTY_ID = "id"; //$NON-NLS-1$
	private static final String PROPERTY_VERSION = "version"; //$NON-NLS-1$
	private static final String PROPERTY_URL = "url"; //$NON-NLS-1$

	private ISiteFeature fCurrentSiteFeature;
	private FormEntry fIdText;
	private FormEntry fVersionText;
	private FormEntry fUrlText;
	private Button fIncludeUrlCheckbox;
	private SelectionListener fRecomputeAdapter;

	public FeatureDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.FeatureDetails_title, PDEUIMessages.FeatureDetails_sectionDescription,
				SWT.NULL);

	}

	public FeatureDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyValue(String property, String value) throws CoreException {
		if (fCurrentSiteFeature == null) {
			return;
		}
		if (property.equals(PROPERTY_ID)) {
			fCurrentSiteFeature.setId(value);
		} else if (property.equals(PROPERTY_VERSION)) {
			fCurrentSiteFeature.setVersion(value);
		} else if (property.equals(PROPERTY_URL)) {
			fCurrentSiteFeature.setURL(value);
		}
	}

	@Override
	public void cancelEdit() {
		fIdText.cancelEdit();
		fVersionText.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (TransferData type : types) {
			for (Transfer transfer : transfers) {
				if (transfer.isSupportedType(type)) {
					return true;
				}
			}
		}
		return false;
	}

	private void clearField(String property) {
		if (property.equals(PROPERTY_ID)) {
			fIdText.setValue(null, true);
		} else if (property.equals(PROPERTY_VERSION)) {
			fVersionText.setValue(null, true);
		} else if (property.equals(PROPERTY_URL)) {
			fUrlText.setValue(null, true);
			fIncludeUrlCheckbox.setSelection(false);
		}
	}

	private void clearFields() {
		fIdText.setValue(null, true);
		fVersionText.setValue(null, true);
		fUrlText.setValue(null, true);
		fIncludeUrlCheckbox.setSelection(false);
	}

	@Override
	public void commit(boolean onSave) {
		fIdText.commit();
		fVersionText.commit();
		super.commit(onSave);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		fIdText = new FormEntry(container, toolkit, PDEUIMessages.FeatureDetails_id, null, false);
		fIdText.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(PROPERTY_ID, text.getValue());
					if (fIncludeUrlCheckbox.getSelection()) {
						applyUrl(true);
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fIdText);
		fIdText.setEditable(isEditable());

		fVersionText = new FormEntry(container, toolkit, PDEUIMessages.FeatureDetails_version, null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this) {

			@Override
			public void textValueChanged(FormEntry text) {
				applyVersion(text.getValue());
				if (fIncludeUrlCheckbox.getSelection()) {
					applyUrl(true);
				}
			}

		});
		limitTextWidth(fVersionText);
		fVersionText.setEditable(isEditable());

		fUrlText = new FormEntry(container, toolkit, PDEUIMessages.FeatureDetails_url, null, false);
		limitTextWidth(fUrlText);
		fUrlText.setEditable(false);

		toolkit.createLabel(container, ""); //$NON-NLS-1$
		fIncludeUrlCheckbox = toolkit.createButton(container, PDEUIMessages.FeatureDetails_include_url, SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fIncludeUrlCheckbox.setLayoutData(gd);
		fRecomputeAdapter = widgetSelectedAdapter(e -> applyUrl(fIncludeUrlCheckbox.getSelection()));
		fIncludeUrlCheckbox.addSelectionListener(fRecomputeAdapter);
		fIncludeUrlCheckbox.setEnabled(isEditable());

		toolkit.paintBordersFor(container);
		section.setClient(container);

		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null) {
			model.addModelChangedListener(this);
		}
	}

	private void applyVersion(String version) {
		String value = version;
		if (value != null && value.isEmpty()) {
			// do not store empty version
			value = null;
		}
		if (value == null) {
			applyUrl(false);
		}
		try {
			applyValue(PROPERTY_VERSION, value);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		updateUrlEnablement();
	}

	private void applyUrl(boolean include) {
		String value = include ? recomputeUrl() : null;
		try {
			applyValue(PROPERTY_URL, value);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private String recomputeUrl() {
		if (fCurrentSiteFeature == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("features/").append(fCurrentSiteFeature.getId()).append("_"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			sb.append(new Version(fCurrentSiteFeature.getVersion()));
		} catch (Exception e) {
			sb.append("0.0.0"); //$NON-NLS-1$
		}
		sb.append(".jar"); //$NON-NLS-1$
		return sb.toString();
	}

	private void updateUrlEnablement() {
		fIncludeUrlCheckbox.setEnabled(isEditable() && !fVersionText.getValue().isEmpty());
	}

	@Override
	public void dispose() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}

	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	@Override
	public void refresh() {
		if (fCurrentSiteFeature == null) {
			clearFields();
			super.refresh();
			return;
		}
		setValue(PROPERTY_ID);
		setValue(PROPERTY_VERSION);
		setValue(PROPERTY_URL);
		updateUrlEnablement();
		super.refresh();
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		ISiteFeature siteFeature = null;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structured = (IStructuredSelection) selection;
			Object o = structured.getFirstElement();
			if (o instanceof ISiteFeature) {
				siteFeature = (ISiteFeature) o;
			} else if (o instanceof SiteFeatureAdapter) {
				siteFeature = ((SiteFeatureAdapter) o).feature;
			}
		}
		fCurrentSiteFeature = siteFeature;
		refresh();
	}

	@Override
	public void setFocus() {
		if (fIdText != null) {
			fIdText.getText().setFocus();
		}
	}

	private void setValue(String property) {
		if (fCurrentSiteFeature == null) {
			clearField(property);
		} else {
			if (property.equals(PROPERTY_ID)) {
				fIdText.setValue(fCurrentSiteFeature.getId(), true);
			} else if (property.equals(PROPERTY_VERSION)) {
				fVersionText.setValue(fCurrentSiteFeature.getVersion(), true);
			} else if (property.equals(PROPERTY_URL)) {
				fIncludeUrlCheckbox.removeSelectionListener(fRecomputeAdapter);
				String url = fCurrentSiteFeature.getURL();
				fIncludeUrlCheckbox.setSelection(url != null);
				fUrlText.setValue(url, true);
				fIncludeUrlCheckbox.addSelectionListener(fRecomputeAdapter);
			}
		}
	}
}
