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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.editor.IPDEEditorPage;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.update.ui.forms.internal.IFormPage;
import org.eclipse.update.ui.forms.internal.IHyperlinkListener;
import org.eclipse.update.ui.forms.internal.SelectableFormLabel;

public class ExtensionSection
	extends PDEFormSection
	implements IHyperlinkListener, IModelChangedListener {
	public static final String SECTION_TITLE =
		"ManifestEditor.ExtensionSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.ExtensionSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.ExtensionSection.fdesc";
	public static final String SECTION_MORE =
		"ManifestEditor.ExtensionSection.more";
	private FormWidgetFactory factory;
	private boolean needsUpdate;
	private Composite extensionParent;
	private Button moreButton;
	private Image extensionImage;

	public ExtensionSection(ManifestFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	private void addExtensionLink(IPluginExtension extension) {
		String point = extension.getPoint();
		IPluginExtensionPoint pointInfo =
			PDECore.getDefault().findExtensionPoint(point);
		Label imageLabel = factory.createLabel(extensionParent, "");
		String name = point;
		String tooltip = point;

		if (extension.getName() != null) {
			String extensionName = extension.getTranslatedName();
			if (MainPreferencePage.isFullNameModeEnabled())
				name = extensionName;
			else
				tooltip = extensionName;
		} else if (pointInfo != null) {
			String translatedName = pointInfo.getTranslatedName();
			if (MainPreferencePage.isFullNameModeEnabled())
				name = translatedName;
			else
				tooltip = translatedName;
		}

		SelectableFormLabel hyperlink =
			factory.createSelectableLabel(extensionParent, name);
		factory.turnIntoHyperlink(hyperlink, this);
		hyperlink.setToolTipText(tooltip);
		hyperlink.setData(extension);
		imageLabel.setImage(extensionImage);
	}
	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		this.factory = factory;
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 2;
		extensionParent = factory.createComposite(container);
		extensionParent.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		extensionParent.setLayoutData(gd);

		Composite buttonContainer = factory.createComposite(container);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		buttonContainer.setLayout(layout);

		moreButton =
			factory.createButton(
				buttonContainer,
				PDEPlugin.getResourceString(SECTION_MORE),
				SWT.PUSH);
		gd =
			new GridData(
				GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		moreButton.setLayoutData(gd);
		final IPDEEditorPage targetPage =
			getFormPage().getEditor().getPage(ManifestEditor.EXTENSIONS_PAGE);
		moreButton.setToolTipText(((IFormPage) targetPage).getTitle());
		moreButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				getFormPage().getEditor().showPage(targetPage);
			}
		});
		return container;
	}
	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		super.dispose();
	}
	public void initialize(Object input) {
		initializeImages();
		IPluginModelBase model = (IPluginModelBase) input;
		update(false);
		model.addModelChangedListener(this);
	}
	private void initializeImages() {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		extensionImage = provider.get(PDEPluginImages.DESC_EXTENSION_OBJ);
	}
	public void linkActivated(Control linkLabel) {
		IPluginExtension extension = (IPluginExtension) linkLabel.getData();
		getFormPage().getEditor().showPage(ManifestEditor.EXTENSIONS_PAGE, extension);
	}
	public void linkEntered(Control link) {
		IPDEEditorPage page =
			getFormPage().getEditor().getPage(ManifestEditor.EXTENSIONS_PAGE);
		String status =
			((PDEFormPage) page).getStatusText()
				+ "#"
				+ ((SelectableFormLabel) link).getText();
		IStatusLineManager manager = getFormPage().getEditor().getStatusLineManager();
		if (manager != null)
			manager.setMessage(status);
	}
	public void linkExited(Control linkLabel) {
		IStatusLineManager manager = getFormPage().getEditor().getStatusLineManager();
		if (manager != null)
			manager.setMessage("");
	}
	public void modelChanged(IModelChangedEvent event) {
		int type = event.getChangeType();
		if (type == IModelChangedEvent.WORLD_CHANGED)
			needsUpdate = true;
		if (getFormPage().isVisible())
			update();
		else if (
			type == IModelChangedEvent.INSERT || type == IModelChangedEvent.REMOVE) {
			Object[] objects = event.getChangedObjects();
			if (objects[0] instanceof IPluginExtension) {
				needsUpdate = true;
			}
		} else if (type == IModelChangedEvent.CHANGE) {
			String property = event.getChangedProperty();
			if (property.equals(IPluginBase.P_EXTENSION_ORDER)
				|| property.equals(IPluginParent.P_SIBLING_ORDER)) {
				needsUpdate = true;
			}
		}
	}
	public void update() {
		if (needsUpdate)
			update(true);
	}
	public void update(boolean removePrevious) {
		if (removePrevious) {
			Control[] children = extensionParent.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i].dispose();
			}
		}
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension[] extensions = plugin.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			addExtensionLink(extensions[i]);
		}
		needsUpdate = false;
		if (removePrevious) {
			extensionParent.layout(true);
			extensionParent.redraw();
		}
	}
}
