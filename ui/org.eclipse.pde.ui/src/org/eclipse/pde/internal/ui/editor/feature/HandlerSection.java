/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.forms.widgets.Section;

public class HandlerSection extends PDESection {
	public static final String SECTION_TITLE =
		"FeatureEditor.HandlerSection.title"; //$NON-NLS-1$
	public static final String SECTION_DESC =
		"FeatureEditor.HandlerSection.desc"; //$NON-NLS-1$
	public static final String SECTION_URL = "FeatureEditor.HandlerSection.url"; //$NON-NLS-1$
	public static final String SECTION_LIBRARY = "FeatureEditor.HandlerSection.library"; //$NON-NLS-1$
	public static final String SECTION_HANDLER = "FeatureEditor.HandlerSection.handler"; //$NON-NLS-1$

	private FormEntry urlText;
	private FormEntry libraryText;
	private FormEntry handlerText;

	public HandlerSection(FeatureAdvancedPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		//setCollapsable(true);
		//IFeatureModel model = (IFeatureModel)page.getModel();
		//IFeature feature = model.getFeature();
		//setCollapsed(feature.getInstallHandler()==null);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}
	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers =
			new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}
	public void commit(boolean onSave) {
		urlText.commit();
		libraryText.commit();
		handlerText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		urlText =
			new FormEntry(container, toolkit,
			PDEPlugin.getResourceString(SECTION_URL),
			null,
			false);
		urlText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					setURL(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		libraryText =
			new FormEntry(container, toolkit,
			PDEPlugin.getResourceString(SECTION_LIBRARY), 
			null,
			false);
		libraryText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					setLibrary(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		handlerText =
			new FormEntry(container, toolkit,
				PDEPlugin.getResourceString(SECTION_HANDLER), 
				null,
				false);
		handlerText.setFormEntryListener(new FormEntryAdapter(this) {
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
	
	private void setURL(IFeature feature, String value) throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		try {
			URL url = new URL(value);
			handler.setURL(url);
		}
		catch (MalformedURLException e) {
			MessageDialog.openError(getPage().getEditor().getEditorSite().getShell(),
					PDEPlugin.getResourceString("HandlerSection.featureEditor"),  //$NON-NLS-1$
					PDEPlugin.getFormattedMessage("HandlerSection.invalid", value)); //$NON-NLS-1$
			setIfDefined(urlText, handler.getURL());
		}
	}
	private void setLibrary(IFeature feature, String value) throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		handler.setLibrary(value);
	}
	private void setHandler(IFeature feature, String value) throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		handler.setHandlerName(value);
	}
	private IFeatureInstallHandler getHandler(IFeature feature) throws CoreException {
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler==null) {
			handler = feature.getModel().getFactory().createInstallHandler();
			feature.setInstallHandler(handler);
		}
		return handler;
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model!=null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel)getPage().getModel();
		refresh();
		if (model.isEditable() == false) {
			urlText.getText().setEditable(false);
			libraryText.getText().setEditable(false);
			handlerText.getText().setEditable(false);
		}
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}
	public void setFocus() {
		if (urlText != null)
			urlText.getText().setFocus();
	}

	private void setIfDefined(FormEntry formText, Object value) {
		if (value!=null)
			formText.setValue(value.toString(), true);
		else
			formText.setValue(null, true);
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel)getPage().getModel();
		IFeature feature = model.getFeature();
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler!=null) {
			setIfDefined(urlText, handler.getURL());
			setIfDefined(libraryText, handler.getLibrary());
			setIfDefined(handlerText, handler.getHandlerName());
		}
		super.refresh();
	}
	public void cancelEdit() {
		urlText.cancelEdit();
		libraryText.cancelEdit();
		handlerText.cancelEdit();
		super.cancelEdit();
	}
}
