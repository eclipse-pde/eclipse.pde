package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.update.ui.forms.internal.*;

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
	public static final String KEY_VERSION_FORMAT = "ManifestEditor.PluginSpecSection.versionFormat";
	public static final String KEY_VERSION_TITLE = "ManifestEditor.PluginSpecSection.versionTitle";
	public static final String KEY_MATCH = "ManifestEditor.PluginSpecSection.versionMatch";
	public static final String KEY_MATCH_PERFECT = "ManifestEditor.MatchSection.perfect";
	public static final String KEY_MATCH_EQUIVALENT = "ManifestEditor.MatchSection.equivalent";
	public static final String KEY_MATCH_COMPATIBLE = "ManifestEditor.MatchSection.compatible";
	public static final String KEY_MATCH_GREATER = "ManifestEditor.MatchSection.greater";

	private Text idText;
	private FormEntry titleText;
	private boolean updateNeeded;
	private boolean fragment;
	private FormEntry providerText;
	private FormEntry versionText;
	private FormEntry classText;
	private FormEntry pluginIdText;
	private FormEntry pluginVersionText;
	private CCombo matchCombo;

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
	layout.marginWidth = 2;
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
	titleText = new FormEntry(createText(container, labelName, factory));
	titleText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				pluginBase.setName(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			String name = pluginBase.getName();
	        name = pluginBase.getResourceString(name);

			if (pluginBase.getModel().isEditable()==false) {
		       name = PDEPlugin.getFormattedMessage(ManifestEditor.KEY_READ_ONLY, name);
			}
			getFormPage().getForm().setHeadingText(name);
			((ManifestEditor) getFormPage().getEditor()).updateTitle();
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});
	versionText =
		new FormEntry(
			createText(container, PDEPlugin.getResourceString(KEY_VERSION), factory));
	versionText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				PluginVersionIdentifier pvi = new PluginVersionIdentifier(text.getValue());
				String formatted = pvi.toString();
				text.setValue(formatted, true);
				pluginBase.setVersion(formatted);
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			} catch (Throwable e) {
				String message = PDEPlugin.getResourceString(KEY_VERSION_FORMAT);
				MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(),
							PDEPlugin.getResourceString(KEY_VERSION_TITLE),
							message);
				text.setValue(pluginBase.getVersion(), true);
			}
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});

	providerText =
		new FormEntry(
			createText(container, PDEPlugin.getResourceString(KEY_PROVIDER_NAME), factory));
	providerText.addFormTextListener(new IFormTextListener() {
		public void textValueChanged(FormEntry text) {
			try {
				pluginBase.setProviderName(text.getValue());
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		public void textDirty(FormEntry text) {
			forceDirty();
		}
	});
	if (isFragment()) {
		final IFragment fragment = (IFragment) pluginBase;
		SelectableFormLabel link =
			factory.createSelectableLabel(
					container,
					PDEPlugin.getResourceString(KEY_PLUGIN_ID));
		factory.turnIntoHyperlink(link, new HyperlinkAdapter() {
			public void linkActivated(Control link) {
				handleOpen();
			}
		});
		link.setToolTipText(PDEPlugin.getResourceString(KEY_PLUGIN_ID_TOOLTIP));
		pluginIdText = new FormEntry(createText(container, factory, 1));
		pluginIdText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					fragment.setPluginId(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		factory.createLabel(container, PDEPlugin.getResourceString(KEY_PLUGIN_VERSION));
		pluginVersionText = new FormEntry(createText(container, factory, 1));
		pluginVersionText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					fragment.setPluginVersion(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		factory.createLabel(container, PDEPlugin.getResourceString(KEY_MATCH));
		matchCombo = new CCombo(container, SWT.READ_ONLY|SWT.FLAT);
		matchCombo.setBackground(factory.getBackgroundColor());
		matchCombo.setForeground(factory.getForegroundColor());
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		matchCombo.setLayoutData(gd);
		String [] items = new String [] {
				"",
				PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
				PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
				PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
				PDEPlugin.getResourceString(KEY_MATCH_GREATER) };
				
		matchCombo.setItems(items);
		matchCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				int match = matchCombo.getSelectionIndex();
				try {
					fragment.setRule(match);
				}
				catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
	} else {
		final IPlugin plugin = (IPlugin) pluginBase;
		if (model.isEditable()) {
			SelectableFormLabel link =
				factory
					.createSelectableLabel(
						container,
						PDEPlugin.getResourceString(KEY_CLASS));
				factory.turnIntoHyperlink(link,
						new HyperlinkAdapter() {
					public void linkActivated(Control link) {
						handleOpen();
					}
				});
			link.setToolTipText(PDEPlugin.getResourceString(KEY_CLASS_TOOLTIP));
		} else {
			factory.createLabel(container, PDEPlugin.getResourceString(KEY_CLASS));
		}

		classText = new FormEntry(createText(container, factory, 1));
		classText.addFormTextListener(new IFormTextListener() {
			public void textValueChanged(FormEntry text) {
				try {
					plugin.setClassName(text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
			public void textDirty(FormEntry text) {
				forceDirty();
			}
		});
		GridData gd = (GridData) classText.getControl().getLayoutData();
		gd.widthHint = 150;
	}
	factory.paintBordersFor(container);
	return container;
}

private void forceDirty() {
	setDirty(true);
	IModel model = (IModel)getFormPage().getModel();
	if (model instanceof IEditable) {
		IEditable editable = (IEditable)model;
		editable.setDirty(true);
		getFormPage().getEditor().fireSaveNeeded();
	}
}

public void dispose() {
	IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
	model.removeModelChangedListener(this);
	super.dispose();
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
		titleText.getControl().setEditable(false);
		versionText.getControl().setEditable(false);
		providerText.getControl().setEditable(false);
		if (isFragment()) {
			pluginVersionText.getControl().setEditable(false);
			pluginIdText.getControl().setEditable(false);
			matchCombo.setEnabled(false);
		} else {
			classText.getControl().setEditable(false);
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
	else if (e.getChangeType()==IModelChangedEvent.CHANGE) {
		Object obj = e.getChangedObjects()[0];
		if (obj instanceof IPluginBase) {
			updateNeeded=true;
			update();
		}
	}
}
public void setFocus() {
	if (titleText != null)
		titleText.getControl().setFocus();
}
public void setFragment(boolean newFragment) {
	fragment = newFragment;
}
private void setIfDefined(FormEntry formText, String value) {
	if (value != null) {
		formText.setValue(value, true);
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
	getFormPage().getForm().setHeadingText(
		pluginBase.getResourceString(pluginBase.getName()));
	((ManifestEditor) getFormPage().getEditor()).updateTitle();
	setIfDefined(idText, pluginBase.getId());
	setIfDefined(versionText, pluginBase.getVersion());
	setIfDefined(providerText, pluginBase.getProviderName());
	if (isFragment()) {
		IFragment fragment = (IFragment) pluginBase;
		setIfDefined(pluginIdText, fragment.getPluginId());
		setIfDefined(pluginVersionText, fragment.getPluginVersion());
		matchCombo.select(fragment.getRule());
	} else {
		setIfDefined(classText, ((IPlugin) pluginBase).getClassName());
	}
	updateNeeded = false;
}
}
