package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.Vector;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.preferences.MainPreferencePage;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

public class RequiresSection
	extends PDEFormSection
	implements IHyperlinkListener, IModelChangedListener {
	private Vector requires = new Vector();
	private Button moreButton;
	private FormWidgetFactory factory;
	private Composite requiresParent;
	private boolean needsUpdate;
	private TableViewer requiresList;
	public static final String SECTION_TITLE =
		"ManifestEditor.RequiresSection.title";
	public static final String SECTION_DESC = "ManifestEditor.RequiresSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.RequiresSection.fdesc";
	public static final String SECTION_MORE = "ManifestEditor.RequiresSection.more";

	public RequiresSection(ManifestFormPage page) {
		super(page);
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	}
	private void addImportLink(IPluginImport importObject) {
		Label imageLabel = factory.createLabel(requiresParent, "");
		String pluginId = importObject.getId();
		IPlugin refPlugin = PDECore.getDefault().findPlugin(pluginId);
		String name = pluginId;
		String tooltip = pluginId;

		if (refPlugin != null) {
			String translatedName = refPlugin.getTranslatedName();
			if (MainPreferencePage.isFullNameModeEnabled())
				name = translatedName;
			else
				tooltip = translatedName;
		}

		int flags = 0;
		if (refPlugin == null)
			flags = SharedLabelProvider.F_ERROR;
		else if (importObject.isReexported())
			flags = SharedLabelProvider.F_EXPORT;

		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		Image image = provider.get(PDEPluginImages.DESC_REQ_PLUGIN_OBJ, flags);
		imageLabel.setImage(image);
		if (refPlugin != null) {
			SelectableFormLabel hyperlink =
				factory.createSelectableLabel(requiresParent, name);
			factory.turnIntoHyperlink(hyperlink, this);
			hyperlink.setToolTipText(tooltip);
			hyperlink.setData(refPlugin);
		} else {
			factory.createLabel(requiresParent, name);
		}
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
		requiresParent = factory.createComposite(container);
		requiresParent.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_BOTH);
		requiresParent.setLayoutData(gd);

		Composite buttonContainer = factory.createComposite(container);
		gd = new GridData(GridData.FILL_VERTICAL);
		buttonContainer.setLayoutData(gd);
		layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		//layout.numColumns = 2;
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
			getFormPage().getEditor().getPage(ManifestEditor.DEPENDENCIES_PAGE);
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
		model.addModelChangedListener(this);
		needsUpdate = true;
		update(true);
	}
	public void linkActivated(Control linkLabel) {
		IPlugin pluginInfo = (IPlugin) linkLabel.getData();
		if (pluginInfo == null)
			return;
		ManifestEditor.openPluginEditor(pluginInfo.getId());
	}
	public void linkEntered(Control linkLabel) {
		IPlugin plugin = (IPlugin) linkLabel.getData();
		if (plugin == null)
			return;
		IPluginModelBase model = plugin.getModel();
		String location = model.getInstallLocation();
		if (location == null)
			return;
		location = location.replace('\\', '/');
		String status = "file://" + location + "/" + "plugin.xml";
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
		else {
			Object[] objects = event.getChangedObjects();
			if (objects[0] instanceof IPluginImport) {
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
	private void update(boolean freshUpdate) {
		if (freshUpdate) {
			Control[] children = requiresParent.getChildren();
			for (int i = 0; i < children.length; i++) {
				children[i].dispose();
			}
		}
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();

		IPluginImport[] imports = model.getPluginBase().getImports();
		//ArraySorter.INSTANCE.sortInPlace(imports);
		for (int i = 0; i < imports.length; i++) {
			addImportLink(imports[i]);
		}
		if (freshUpdate) {
			requiresParent.layout(true);
			requiresParent.redraw();
		}
		needsUpdate = false;
	}
}