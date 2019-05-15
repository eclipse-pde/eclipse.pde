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
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.osgi.framework.Version;

public class FeatureDetailsSection extends IUDetailsSection<ISiteFeature> {

	private FormEntry fUrlText;
	private Button fIncludeUrlCheckbox;
	private SelectionListener fRecomputeAdapter;

	public FeatureDetailsSection(PDEFormPage page, Composite parent) {
		super(page, parent, PDEUIMessages.FeatureDetails_title, PDEUIMessages.FeatureDetails_sectionDescription,
				FeatureDetailsSection::extractFromSelection);

	}

	private static ISiteFeature extractFromSelection(Object o) {
		if (o instanceof ISiteFeature) {
			return (ISiteFeature) o;
		} else if (o instanceof SiteFeatureAdapter) {
			return ((SiteFeatureAdapter) o).feature;
		}
		return null;
	}

	@Override
	protected void clearFields() {
		super.clearFields();
		fUrlText.setValue(null, true);
		fIncludeUrlCheckbox.setSelection(false);
	}

	@Override
	protected void onCreateClient(Composite container, FormToolkit toolkit) {
		fUrlText = new FormEntry(container, toolkit, PDEUIMessages.FeatureDetails_url, null, false);
		limitTextWidth(fUrlText);
		fUrlText.setEditable(false);

		toolkit.createLabel(container, ""); //$NON-NLS-1$
		fIncludeUrlCheckbox = toolkit.createButton(container, PDEUIMessages.FeatureDetails_include_url, SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalIndent = FormLayoutFactory.CONTROL_HORIZONTAL_INDENT;
		fIncludeUrlCheckbox.setLayoutData(gd);
		fRecomputeAdapter = widgetSelectedAdapter(e -> {
			try {
				applyUrl(fIncludeUrlCheckbox.getSelection());
			} catch (CoreException ex) {
				PDEPlugin.logException(ex);
			}
		});
		fIncludeUrlCheckbox.addSelectionListener(fRecomputeAdapter);
		fIncludeUrlCheckbox.setEnabled(isEditable());
	}

	@Override
	protected void applyId(String value) throws CoreException {
		super.applyId(value);
		onUrlDependencyChanged(value);
	}

	@Override
	protected void applyVersion(String value) throws CoreException {
		super.applyVersion(value);
		onUrlDependencyChanged(value);
	}

	private void onUrlDependencyChanged(String dependencyValue) throws CoreException {
		boolean includeUrl = (dependencyValue != null) && fIncludeUrlCheckbox.getSelection();
		applyUrl(includeUrl);
		updateUrlEnablement();
	}

	private void applyUrl(boolean include) throws CoreException {
		ISiteFeature feature = getCurrentItem();
		if (feature != null) {
			String value = include ? recomputeUrl() : null;
			feature.setURL(value);
		}
	}

	private String recomputeUrl() {
		ISiteFeature feature = getCurrentItem();
		if (feature == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("features/").append(feature.getId()).append("_"); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			sb.append(new Version(feature.getVersion()));
		} catch (Exception e) {
			sb.append("0.0.0"); //$NON-NLS-1$
		}
		sb.append(".jar"); //$NON-NLS-1$
		return sb.toString();
	}

	private void updateUrlEnablement() {
		ISiteFeature feature = getCurrentItem();
		boolean hasVersionAndId = (feature != null) && (feature.getId() != null) && (feature.getVersion() != null);
		fIncludeUrlCheckbox.setEnabled(isEditable() && hasVersionAndId);
	}

	@Override
	protected void fillControls(ISiteFeature currentItem) {
		super.fillControls(currentItem);

		fIncludeUrlCheckbox.removeSelectionListener(fRecomputeAdapter);
		String url = currentItem.getURL();
		fIncludeUrlCheckbox.setSelection(url != null);
		fUrlText.setValue(url, true);
		fIncludeUrlCheckbox.addSelectionListener(fRecomputeAdapter);

		updateUrlEnablement();
	}

}
