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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class HandlerSection extends PDESection {
	public static final String SECTION_TITLE = "FeatureEditor.HandlerSection.title"; //$NON-NLS-1$

	public static final String SECTION_DESC = "FeatureEditor.HandlerSection.desc"; //$NON-NLS-1$

	public static final String SECTION_LIBRARY = "FeatureEditor.HandlerSection.library"; //$NON-NLS-1$

	public static final String SECTION_HANDLER = "FeatureEditor.HandlerSection.handler"; //$NON-NLS-1$

	private FormEntry fLibraryText;

	private FormEntry fHandlerText;

	public HandlerSection(FeatureAdvancedPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

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

	public void commit(boolean onSave) {
		fLibraryText.commit();
		fHandlerText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		fLibraryText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_LIBRARY), null, false);
		fLibraryText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					setLibrary(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		fHandlerText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_HANDLER), null, false);
		fHandlerText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					setHandler(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	private void setLibrary(IFeature feature, String value)
			throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		handler.setLibrary(value);
	}

	private void setHandler(IFeature feature, String value)
			throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		handler.setHandlerName(value);
	}

	private IFeatureInstallHandler getHandler(IFeature feature)
			throws CoreException {
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler == null) {
			handler = feature.getModel().getFactory().createInstallHandler();
			feature.setInstallHandler(handler);
		}
		return handler;
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
		if (model.isEditable() == false) {
			fLibraryText.getText().setEditable(false);
			fHandlerText.getText().setEditable(false);
		}
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}

	public void setFocus() {
		if (fLibraryText != null)
			fLibraryText.getText().setFocus();
	}

	private void setIfDefined(FormEntry formText, Object value) {
		if (value != null)
			formText.setValue(value.toString(), true);
		else
			formText.setValue(null, true);
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler != null) {
			setIfDefined(fLibraryText, handler.getLibrary());
			setIfDefined(fHandlerText, handler.getHandlerName());
		}
		super.refresh();
	}

	public void cancelEdit() {
		fLibraryText.cancelEdit();
		fHandlerText.cancelEdit();
		super.cancelEdit();
	}
}
