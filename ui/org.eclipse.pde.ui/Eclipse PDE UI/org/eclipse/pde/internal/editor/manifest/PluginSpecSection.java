package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.*;
import org.eclipse.core.resources.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.base.model.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.forms.*;
import org.eclipse.pde.internal.editor.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.jdt.ui.*;
import org.eclipse.pde.internal.PDEPlugin;

public class PluginSpecSection extends PDEFormSection {
	public static final String SECTION_TITLE = "ManifestEditor.PluginSpecSection.title";
	public static final String SECTION_DESC = "ManifestEditor.PluginSpecSection.desc";
	public static final String SECTION_FDESC = "ManifestEditor.PluginSpecSection.fdesc";

	public static final String KEY_ID = "ManifestEditor.PluginSpecSection.id";
	public static final String KEY_FID = "ManifestEditor.PluginSpecSection.fid";
	public static final String KEY_NAME = "ManifestEditor.PluginSpecSection.name";
	public static final String KEY_FNAME = "ManifestEditor.PluginSpecSection.fname";
	public static final String KEY_VERSION = "ManifestEditor.PluginSpecSection.version";
	public static final String KEY_PROVIDER_NAME = "ManifestEditor.PluginSpecSection.providerName";
	public static final String KEY_PLUGIN_ID = "ManifestEditor.PluginSpecSection.pluginId";
	public static final String KEY_PLUGIN_ID_TOOLTIP = "ManifestEditor.PluginSpecSection.pluginId.tooltip";
	public static final String KEY_PLUGIN_VERSION = "ManifestEditor.PluginSpecSection.pluginVersion";
	public static final String KEY_CLASS = "ManifestEditor.PluginSpecSection.class";
	public static final String KEY_CLASS_TOOLTIP = "ManifestEditor.PluginSpecSection.class.tooltip";




	
	private Text idText;
	private FormText titleText;
	private boolean updateNeeded;
	private boolean fragment;
	private FormText providerText;
	private FormText versionText;
	private FormText classText;
	private FormText pluginIdText;
	private FormText pluginVersionText;

public PluginSpecSection(ManifestFormPage page) {
	super(page);
	setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
	boolean fragment = ((ManifestEditor) page.getEditor()).isFragmentEditor();
	if (fragment)
		setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
	else
		setDescription(PDEPlugin.getResourceString(SECTION_DESC));
	setFragment(fragment);
}
public void commitChanges(boolean onSave) {
	titleText.commit();
	providerText.commit();
	versionText.commit();
	if (isFragment()) {
		pluginIdText.commit();
		pluginVersionText.commit();
	} else {
		classText.commit();
	}
}
public Composite createClient(Composite parent, FormWidgetFactory factory) {
	Composite container = factory.createComposite(parent);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 7;
	layout.horizontalSpacing = 6;
	container.setLayout(layout);
	String labelName =
		isFragment()
			? PDEPlugin.getResourceString(KEY_FID)
			: PDEPlugin.getResourceString(KEY_ID);
	idText = createText(container, labelName, factory);
	idText.setEnabled(false);

	IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
	final IPluginBase pluginBase = model.getPluginBase();

	labelName =
		isFragment()
			? PDEPlugin.getResourceString(KEY_FNAME)
			: PDEPlugin.getResourceString(KEY_NAME);
	titleText = new FormText(createText(container, labelName, factory));
	titleText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				pluginBase.setName(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			getFormPage().getForm().setTitle(
				pluginBase.getResourceString(pluginBase.getName()));
			((ManifestEditor) getFormPage().getEditor()).updateTitle();
		}
	});
	versionText =
		new FormText(
			createText(container, PDEPlugin.getResourceString(KEY_VERSION), factory));
	versionText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				pluginBase.setVersion(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	});

	providerText =
		new FormText(
			createText(container, PDEPlugin.getResourceString(KEY_PROVIDER_NAME), factory));
	providerText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormText text) {
			try {
				pluginBase.setProviderName(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	});
	if (isFragment()) {
		final IFragment fragment = (IFragment) pluginBase;
		Label link =
			factory
				.createHyperlinkLabel(
					container,
					PDEPlugin.getResourceString(KEY_PLUGIN_ID),
					new HyperlinkAdapter() {
			public void linkActivated(Control link) {
				handleOpen();
			}
		});
		link.setToolTipText(PDEPlugin.getResourceString(KEY_PLUGIN_ID_TOOLTIP));
		pluginIdText = new FormText(createText(container, factory, 1));
		pluginIdText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormText text) {
				try {
					fragment.setPluginId(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		Label label =
			factory.createLabel(container, PDEPlugin.getResourceString(KEY_PLUGIN_VERSION));
		pluginVersionText = new FormText(createText(container, factory, 1));
		pluginVersionText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormText text) {
				try {
					fragment.setPluginVersion(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	} else {
		final IPlugin plugin = (IPlugin) pluginBase;
		if (model.isEditable()) {
			Label link =
				factory
					.createHyperlinkLabel(
						container,
						PDEPlugin.getResourceString(KEY_CLASS),
						new HyperlinkAdapter() {
				public void linkActivated(Control link) {
					handleOpen();
				}
			});
			link.setToolTipText(PDEPlugin.getResourceString(KEY_CLASS_TOOLTIP));
		} else {
			Label label = factory.createLabel(container, PDEPlugin.getResourceString(KEY_CLASS));
		}

		classText = new FormText(createText(container, factory, 1));
		classText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormText text) {
				try {
					plugin.setClassName(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		GridData gd = (GridData) classText.getControl().getLayoutData();
		gd.widthHint = 150;
	}
	factory.paintBordersFor(container);
	return container;
}
public void dispose() {
	IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
	model.removeModelChangedListener(this);
}
private void handleOpen() {
	if (isFragment()) {
		handleOpenPlugin();
		return;
	}
	String name = classText.getControl().getText();
	IFile file =
		((IFileEditorInput) getFormPage().getEditor().getEditorInput()).getFile();
	IProject project = file.getProject();
	IJavaProject javaProject = JavaCore.create(project);

	String path = name.replace('.', '/') + ".java";
	try {
		IJavaElement result = javaProject.findElement(new Path(path));
		if (result != null) {
			JavaUI.openInEditor(result);
		}
	} catch (PartInitException e) {
		Display.getCurrent().beep();
	} catch (JavaModelException e) {
		// nothing
		Display.getCurrent().beep();
	}
}
private void handleOpenPlugin() {
	IFragmentModel model = (IFragmentModel) getFormPage().getModel();
	String id = model.getFragment().getPluginId();
	if (id == null)
		return;
	((ManifestEditor) getFormPage().getEditor()).openPluginEditor(id);
}
public void initialize(Object input) {
	IPluginModelBase model = (IPluginModelBase) input;
	update(input);
	if (model.isEditable() == false) {
		titleText.getControl().setEnabled(false);
		versionText.getControl().setEnabled(false);
		providerText.getControl().setEnabled(false);
		if (isFragment()) {
			pluginVersionText.getControl().setEnabled(false);
			pluginIdText.getControl().setEnabled(false);

		} else {
			classText.getControl().setEnabled(false);
		}
	}
	model.addModelChangedListener(this);
}
public boolean isDirty() {
	boolean baseDirty =
		titleText.isDirty() || providerText.isDirty() || versionText.isDirty();
	if (isFragment()) {
		return baseDirty || pluginVersionText.isDirty() || pluginIdText.isDirty();
	} else {
		return baseDirty || classText.isDirty();
	}
}
public boolean isFragment() {
	return fragment;
}
public void modelChanged(IModelChangedEvent e) {
	if (e.getChangeType()==IModelChangedEvent.WORLD_CHANGED) {
		updateNeeded=true;
	}
}
public void setFocus() {
	if (titleText != null)
		titleText.getControl().setFocus();
}
public void setFragment(boolean newFragment) {
	fragment = newFragment;
}
private void setIfDefined(FormText formText, String value) {
	if (value != null) {
		formText.setValue(value);
		formText.setDirty(false);
	}
}
private void setIfDefined(Text text, String value) {
	if (value != null)
		text.setText(value);
}
public void update() {
	if (updateNeeded) {
		this.update(getFormPage().getModel());
	}
}
public void update(Object input) {
	IPluginModelBase model = (IPluginModelBase) input;
	IPluginBase pluginBase = model.getPluginBase();
	setIfDefined(titleText, pluginBase.getName());
	getFormPage().getForm().setTitle(
		pluginBase.getResourceString(pluginBase.getName()));
	((ManifestEditor) getFormPage().getEditor()).updateTitle();
	setIfDefined(idText, pluginBase.getId());
	setIfDefined(versionText, pluginBase.getVersion());
	setIfDefined(providerText, pluginBase.getProviderName());
	if (isFragment()) {
		IFragment fragment = (IFragment) pluginBase;
		setIfDefined(pluginIdText, fragment.getPluginId());
		setIfDefined(pluginVersionText, fragment.getPluginVersion());
	} else {
		setIfDefined(classText, ((IPlugin) pluginBase).getClassName());
	}
	updateNeeded = false;
}
}
