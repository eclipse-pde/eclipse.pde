package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.events.*;
import java.util.*;
import org.eclipse.pde.internal.core.*;
import org.w3c.dom.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.core.*;

public class ExtensionPointSection
	extends PDEFormSection
	implements IHyperlinkListener, IModelChangedListener {
	private FormWidgetFactory factory;
	private Composite pointParent;
	private boolean needsUpdate;
	public static final String SECTION_TITLE =
		"ManifestEditor.ExtensionPointSection.title";
	public static final String SECTION_MORE =
		"ManifestEditor.ExtensionPointSection.more";
	public static final String SECTION_DESC =
		"ManifestEditor.ExtensionPointSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.ExtensionPointSection.fdesc";
	private Button moreButton;

	public ExtensionPointSection(ManifestFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	private void addExtensionPointLink(IPluginExtensionPoint point) {
		Label imageLabel = factory.createLabel(pointParent, "");
		String name = point.getId();
		String tooltip = name;

		if (MainPreferencePage.isFullNameModeEnabled())
			name = point.getTranslatedName();
		else
			tooltip = point.getTranslatedName();
		SelectableFormLabel hyperlink =
			factory.createSelectableLabel(pointParent, name);
		factory.turnIntoHyperlink(hyperlink, this);
		hyperlink.setToolTipText(tooltip);
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		Image image = provider.get(PDEPluginImages.DESC_EXT_POINT_OBJ);
		imageLabel.setImage(image);
		hyperlink.setData(point);
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

		pointParent = factory.createComposite(container);

		RowLayout rlayout = new RowLayout();
		rlayout.wrap = true;
		pointParent.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_BOTH);
		pointParent.setLayoutData(gd);

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
			getFormPage().getEditor().getPage(ManifestEditor.EXTENSION_POINT_PAGE);
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
		IPluginModelBase model = (IPluginModelBase) input;
		update(false);
		model.addModelChangedListener(this);
	}

	public void linkActivated(Control linkLabel) {
		IPluginExtensionPoint point = (IPluginExtensionPoint) linkLabel.getData();
		getFormPage().getEditor().showPage(ManifestEditor.EXTENSION_POINT_PAGE, point);
	}
	public void linkEntered(Control linkLabel) {
		IPDEEditorPage page =
			getFormPage().getEditor().getPage(ManifestEditor.EXTENSION_POINT_PAGE);
		IPluginExtensionPoint point = (IPluginExtensionPoint) linkLabel.getData();
		String status = ((PDEFormPage) page).getStatusText() + "#" + point.getName();
		IStatusLineManager manager = getFormPage().getEditor().getStatusLineManager();
		if (manager != null)
			manager.setMessage(status);
	}
	public void linkExited(org.eclipse.swt.widgets.Control linkLabel) {
		IStatusLineManager manager = getFormPage().getEditor().getStatusLineManager();
		if (manager != null)
			manager.setMessage("");
	}
	public void modelChanged(IModelChangedEvent event) {
		int type = event.getChangeType();
		if (type == IModelChangedEvent.WORLD_CHANGED)
			needsUpdate = true;
		else if (
			type == IModelChangedEvent.INSERT || type == IModelChangedEvent.REMOVE) {
			Object[] objects = event.getChangedObjects();
			if (objects[0] instanceof IPluginExtensionPoint) {
				needsUpdate = true;
			}
		}
		if (getFormPage().isVisible())
			update();
	}
	public void update() {
		if (needsUpdate)
			update(true);
	}
	public void update(boolean removePrevious) {
		if (removePrevious) {
			Control[] children = pointParent.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i].dispose();
			}
		}
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();
		IPluginExtensionPoint[] points = plugin.getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			addExtensionPointLink(points[i]);
		}
		if (removePrevious) {
			pointParent.layout(true);
			pointParent.redraw();
		}
		needsUpdate = false;
	}
}