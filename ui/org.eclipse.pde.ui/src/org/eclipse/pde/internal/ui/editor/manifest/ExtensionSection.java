package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;
import org.eclipse.jface.action.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.events.*;
import org.w3c.dom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.core.*;

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
		if (pointInfo != null && MainPreferencePage.isFullNameModeEnabled())
			name = pointInfo.getResourceString(pointInfo.getName());
		SelectableFormLabel hyperlink =
			factory.createSelectableLabel(extensionParent, name);
		factory.turnIntoHyperlink(hyperlink, this);
		hyperlink.setToolTipText(point);
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
		}
		else if (type == IModelChangedEvent.CHANGE) {
			String property = event.getChangedProperty();
			if (property.equals(IPluginBase.P_EXTENSION_ORDER) ||
			property.equals(IPluginParent.P_SIBLING_ORDER)) {
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