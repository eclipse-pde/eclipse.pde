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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class HandlerSection extends PDEFormSection {
	public static final String SECTION_TITLE =
		"FeatureEditor.HandlerSection.title";
	public static final String SECTION_DESC =
		"FeatureEditor.HandlerSection.desc";
	public static final String SECTION_URL = "FeatureEditor.HandlerSection.url";
	public static final String SECTION_LIBRARY = "FeatureEditor.HandlerSection.library";
	public static final String SECTION_HANDLER = "FeatureEditor.HandlerSection.handler";

	private FormEntry urlText;
	private FormEntry libraryText;
	private FormEntry handlerText;
	private boolean updateNeeded;

	public HandlerSection(FeatureAdvancedPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		//setCollapsable(true);
		//IFeatureModel model = (IFeatureModel)page.getModel();
		//IFeature feature = model.getFeature();
		//setCollapsed(feature.getInstallHandler()==null);
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
	public void commitChanges(boolean onSave) {
		urlText.commit();
		libraryText.commit();
		handlerText.commit();
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		final IFeature feature = model.getFeature();

		urlText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_URL), factory));
		urlText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					setURL(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		libraryText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_LIBRARY), factory));
		libraryText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					setLibrary(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		handlerText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_HANDLER), factory));
		handlerText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					setHandler(feature, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		factory.paintBordersFor(container);
		return container;
	}
	
	private void setURL(IFeature feature, String value) throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		try {
			URL url = new URL(value);
			handler.setURL(url);
		}
		catch (MalformedURLException e) {
			PDEPlugin.logException(e);
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

	private void forceDirty() {
		setDirty(true);
		IModel model = (IModel) getFormPage().getModel();
		if (model instanceof IEditable) {
			IEditable editable = (IEditable) model;
			editable.setDirty(true);
			getFormPage().getEditor().fireSaveNeeded();
		}
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		update(input);
		if (model.isEditable() == false) {
			urlText.getControl().setEditable(false);
			libraryText.getControl().setEditable(false);
			handlerText.getControl().setEditable(false);
		}
		model.addModelChangedListener(this);
	}
	public boolean isDirty() {
		return urlText.isDirty() || libraryText.isDirty() || handlerText.isDirty();
	}
	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			updateNeeded = true;
		}
	}
	public void setFocus() {
		if (urlText != null)
			urlText.getControl().setFocus();
	}

	private void setIfDefined(FormEntry formText, Object value) {
		if (value != null) {
			formText.setValue(value.toString(), true);
		}
	}
	public void update() {
		if (updateNeeded) {
			this.update(getFormPage().getModel());
		}
	}
	public void update(Object input) {
		IFeatureModel model = (IFeatureModel) input;
		IFeature feature = model.getFeature();
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler!=null) {
			setIfDefined(urlText, handler.getURL());
			setIfDefined(libraryText, handler.getLibrary());
			setIfDefined(handlerText, handler.getHandlerName());
		}
		updateNeeded = false;
	}
}
