package org.eclipse.pde.internal.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.feature.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.swt.custom.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import java.net.*;

public class HandlerSection extends PDEFormSection {
	public static final String SECTION_TITLE =
		"FeatureEditor.HandlerSection.title";
	public static final String SECTION_DESC =
		"FeatureEditor.HandlerSection.desc";
	public static final String SECTION_URL = "FeatureEditor.HandlerSection.url";
	public static final String SECTION_LIBRARY = "FeatureEditor.HandlerSection.library";
	public static final String SECTION_CLASS = "FeatureEditor.HandlerSection.class";

	private FormEntry urlText;
	private FormEntry libraryText;
	private FormEntry classText;
	private boolean updateNeeded;

	public HandlerSection(FeatureFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		setCollapsable(true);
		IFeatureModel model = (IFeatureModel)page.getModel();
		IFeature feature = model.getFeature();
		setCollapsed(feature.getInstallHandler()==null);
	}
	public void commitChanges(boolean onSave) {
		IFeatureModel model = (IFeatureModel) getFormPage().getModel();
		urlText.commit();
		libraryText.commit();
		classText.commit();
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
		classText =
			new FormEntry(
				createText(container, PDEPlugin.getResourceString(SECTION_CLASS), factory));
		classText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					setClass(feature, text.getValue());
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
	private void setClass(IFeature feature, String value) throws CoreException {
		IFeatureInstallHandler handler = getHandler(feature);
		handler.setClassName(value);
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
			urlText.getControl().setEnabled(false);
			libraryText.getControl().setEnabled(false);
			classText.getControl().setEnabled(false);
		}
		model.addModelChangedListener(this);
	}
	public boolean isDirty() {
		return urlText.isDirty() || libraryText.isDirty() || classText.isDirty();
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
	private void setIfDefined(Text text, Object value) {
		if (value != null)
			text.setText(value.toString());
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
			setIfDefined(classText, handler.getClassName());
		}
		updateNeeded = false;
	}
}