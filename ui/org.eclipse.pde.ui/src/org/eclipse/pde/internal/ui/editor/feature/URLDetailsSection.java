/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.*;

public class URLDetailsSection extends PDESection implements IPartSelectionListener {
	private FormEntry fNameText;

	private FormEntry fUrlText;

	private IFeatureURLElement fInput;

	public URLDetailsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | ExpandableComposite.NO_TITLE, false);
		getSection().setDescription(PDEUIMessages.FeatureEditor_URLDetailsSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void commit(boolean onSave) {
		fUrlText.commit();
		fNameText.commit();
		super.commit(onSave);
	}

	private void commitSiteUrl(String value) {
		if (fInput == null) {
			return;
		}
		try {
			if (value.length() > 0) {
				URL siteUrl = new URL(value);
				fInput.setURL(siteUrl);
			} else {
				fInput.setURL(null);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		} catch (MalformedURLException e) {
			PDEPlugin.logException(e);
		}
	}

	private void commitSiteName(String value) {
		if (fInput == null) {
			return;
		}
		try {
			fInput.setLabel(value);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		final IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		fUrlText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_URLDetailsSection_updateUrl, null, false);
		fUrlText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				String url = text.getValue() != null ? text.getValue() : ""; //$NON-NLS-1$
				if (url.length() > 0 && !verifySiteUrl(feature, url)) {
					warnBadUrl(url);
					setUrlText();
				} else {
					commitSiteUrl(url);
				}
			}
		});

		fNameText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_URLDetailsSection_updateUrlLabel, null, false);
		fNameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				String name = text.getValue() != null ? text.getValue() : ""; //$NON-NLS-1$
				commitSiteName(name);
			}
		});

		GridData gd = (GridData) fUrlText.getText().getLayoutData();
		gd.widthHint = 150;

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private boolean verifySiteUrl(IFeature feature, String value) {
		try {
			new URL(value);
		} catch (MalformedURLException e) {
			return false;
		}
		return true;
	}

	private void warnBadUrl(String text) {
		MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureEditor_URLDetailsSection_badUrlTitle, PDEUIMessages.FeatureEditor_URLDetailsSection_badUrlMessage);
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
			fUrlText.getText().setEditable(false);
			fNameText.getText().setEditable(false);
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
			if (objs.length > 0 && objs[0] instanceof IFeatureURL) {
				markStale();
			}
		}
		Object objs[] = e.getChangedObjects();
		if (objs.length > 0 && objs[0] instanceof IFeatureURLElement) {
			markStale();
		}
	}

	public void setFocus() {
		if (fUrlText != null)
			fUrlText.getText().setFocus();
	}

	public void refresh() {
		update();
		super.refresh();
	}

	private void setUrlText() {
		String updateSiteUrl = ""; //$NON-NLS-1$
		if (fInput != null && fInput.getURL() != null) {
			updateSiteUrl = fInput.getURL().toExternalForm();
		}
		fUrlText.setValue(updateSiteUrl != null ? updateSiteUrl : "", //$NON-NLS-1$
				true);

	}

	private void update() {
		fUrlText.setEditable(fInput != null && fInput.getModel().isEditable());
		fNameText.setEditable(fInput != null && fInput.getModel().isEditable());
		setUrlText();
		setNameText();
	}

	private void setNameText() {
		String updateSiteLabel = ""; //$NON-NLS-1$
		if (fInput != null) {
			updateSiteLabel = fInput.getLabel();
		}
		fNameText.setValue(updateSiteLabel != null ? updateSiteLabel : "", true); //$NON-NLS-1$
	}

	public void cancelEdit() {
		fNameText.cancelEdit();
		fUrlText.cancelEdit();
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

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof IFeatureURLElement && ((IFeatureURLElement) o).getElementType() == IFeatureURLElement.DISCOVERY) {
				fInput = (IFeatureURLElement) o;
			} else {
				fInput = null;
			}
		} else
			fInput = null;
		update();
	}
}
