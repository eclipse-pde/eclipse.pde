/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.wizards.StatusWizardPage;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;

import org.eclipse.jface.dialogs.IDialogSettings;

public class WorkbenchLauncherWizardAdvancedPage extends StatusWizardPage {

	private static final String SETTINGS_USECUSTOM = "default";
	private static final String SETTINGS_WSPROJECT = "wsproject";
	private static final String SETTINGS_EXTPLUGINS = "extplugins";
	public static final String KEY_WORKSPACE_PLUGINS =
		"Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"Preferences.AdvancedTracingPage.externalPlugins";

	private static final String SETTINGS_PREVPATH = "prevpath";

	private Button useDefaultCheck;
	private CheckboxTreeViewer pluginTreeViewer;
	private Image pluginImage;
	private Image fragmentImage;
	private Image pluginsImage;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Vector externalList;
	private Vector workspaceList;

	class PluginLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) obj;
				String name = model.getPlugin().getTranslatedName();
				return name + " (" + model.getPlugin().getVersion() + ")";
			}
			return obj.toString();
		}
		public Image getImage(Object obj) {
			if (obj instanceof IPluginModel)
				return pluginImage;
			if (obj instanceof IFragmentModel)
				return fragmentImage;
			if (obj instanceof NamedElement)
				return ((NamedElement) obj).getImage();
			return null;
		}
	}

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				return getExternalPlugins();
			}
			if (parent == workspacePlugins) {
				return getWorkspacePlugins();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) child;
				if (model.getUnderlyingResource() != null)
					return workspacePlugins;
				else
					return externalPlugins;
			}
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { workspacePlugins, externalPlugins };
		}
	}

	public WorkbenchLauncherWizardAdvancedPage(String title) {
		super("WorkbenchLauncherWizardAdvancedPage", false);
		setTitle(title);
		setDescription("Plugins visible to the plugin loader.");
		pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
		fragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
		pluginsImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
	}
	
	public void dispose() {
		pluginImage.dispose();
		fragmentImage.dispose();
		pluginsImage.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		GridData gd;
		layout.numColumns = 2;
		composite.setLayout(layout);

		useDefaultCheck = new Button(composite, SWT.CHECK);
		useDefaultCheck.setText("&Use default");
		fillIntoGrid(useDefaultCheck, 2, false);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(
			"If this option is checked, the workbench instance you are about to launch will 'see' all the plug-ins and fragments in the workspace, as well as all the external projects enabled in the Preferences.");
		gd = fillIntoGrid(label, 1, false);
		gd.widthHint = convertWidthInCharsToPixels(70);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 2, false);

		Control list = createPluginList(composite);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		//gd.verticalSpan = 2;
		gd.heightHint = 250;
		list.setLayoutData(gd);

		initializeFields();
		
		hookListeners();
		pluginTreeViewer.setInput(PDEPlugin.getDefault());
		setControl(composite);
	}
	
	private void hookListeners() {
		useDefaultCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaultChanged();
			}
		});
	}
	
	private void useDefaultChanged() {
		boolean useDefault = useDefaultCheck.getSelection();
		updateStatus();
	}

	private GridData fillIntoGrid(Control control, int hspan, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = hspan;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	protected Control createPluginList(Composite parent) {
		pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(new PluginLabelProvider());
		pluginTreeViewer.setAutoExpandLevel(2);
		pluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModel) {
					IPluginModel model = (IPluginModel) event.getElement();
					handleCheckStateChanged(model, event.getChecked());
				} else {
					pluginTreeViewer.setChecked(element, false);
				}
			}
		});
		workspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS),
				pluginsImage);
		externalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS),
				pluginsImage);
		return pluginTreeViewer.getTree();
	}
	
	private Object[] getExternalPlugins() {
		return PDEPlugin.getDefault().getExternalModelManager().getModels();
	}

	private Object[] getWorkspacePlugins() {
		return PDEPlugin
			.getDefault()
			.getWorkspaceModelManager()
			.getWorkspacePluginModels();
	}

	private void initializeFields() {
		boolean useDefault = true;
		
		/*

		ArrayList checkedPlugins = new ArrayList();
		checkedPlugins.addAll(available);

		ArrayList externalPlugins = new ArrayList();

		if (initialSettings != null) {
			useDefault = !initialSettings.getBoolean(SETTINGS_USECUSTOM);

			String deselectedPluginIDs = initialSettings.get(SETTINGS_WSPROJECT);
			if (deselectedPluginIDs != null) {
				ArrayList deselected = new ArrayList();
				StringTokenizer tok =
					new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
				while (tok.hasMoreTokens()) {
					deselected.add(tok.nextToken());
				}
				for (int i = checkedPlugins.size() - 1; i >= 0; i--) {
					PluginModel desc = (PluginModel) checkedPlugins.get(i);
					if (deselected.contains(desc.getId())) {
						checkedPlugins.remove(i);
					}
				}
			}

			String ext = initialSettings.get(SETTINGS_EXTPLUGINS);
			if (ext != null) {
				ArrayList urls = new ArrayList();
				StringTokenizer tok = new StringTokenizer(ext, File.pathSeparator);
				while (tok.hasMoreTokens()) {
					try {
						urls.add(new URL(tok.nextToken()));
					} catch (MalformedURLException e) {
						SelfHostingPlugin.log(e);
					}
				}
				URL[] urlsArray = (URL[]) urls.toArray(new URL[urls.size()]);
				PluginModel[] descs = PluginUtil.getPluginModels(urlsArray);
				if (descs != null) {
					externalPlugins.addAll(Arrays.asList(descs));
				}
			}
		}
		*/

		useDefaultCheck.setSelection(useDefault);
		//fWorkspacePluginsList.setCheckedElements(checkedPlugins);
		//fExternalPluginsList.setElements(externalPlugins);
	}
	private void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
	}

	public void storeSettings() {
		IDialogSettings initialSettings = getDialogSettings();
		initialSettings.put(SETTINGS_USECUSTOM, !useDefaultCheck.getSelection());
		/*
		StringBuffer buf = new StringBuffer();
		// store deselected projects
		List selectedProjects = fWorkspacePluginsList.getCheckedElements();
		List projects = fWorkspacePluginsList.getElements();
		for (int i = 0; i < projects.size(); i++) {
			PluginModel curr = (PluginModel) projects.get(i);
			if (!selectedProjects.contains(curr)) {
				buf.append(curr.getId());
				buf.append(File.pathSeparatorChar);
			}
		}
		initialSettings.put(SETTINGS_WSPROJECT, buf.toString());

		buf = new StringBuffer();
		List external = fExternalPluginsList.getElements();
		for (int i = 0; i < external.size(); i++) {
			PluginModel curr = (PluginModel) external.get(i);
			buf.append(curr.getLocation());
			if (curr instanceof PluginDescriptorModel) {
				buf.append("/plugin.xml");
			} else if (curr instanceof PluginFragmentModel) {
				buf.append("/fragment.xml");
			}
			buf.append(File.pathSeparatorChar);
		}
		initialSettings.put(SETTINGS_EXTPLUGINS, buf.toString());
		*/
	}

/*
	private PluginModel chooseExternalPlugin() {
		String prevPath = getDialogSettings().get(SETTINGS_PREVPATH);

		FileDialog dialog = new FileDialog(getShell());
		dialog.setText("External plugins (plugin.xml or fragment.xml file)");
		dialog.setFilterExtensions(new String[] { "plugin.xml", "fragment.xml" });
		if (prevPath != null) {
			dialog.setFilterPath(prevPath);
		}
		String res = dialog.open();
		if (res != null) {
			getDialogSettings().put(SETTINGS_PREVPATH, dialog.getFilterPath());
			PluginModel desc = PluginUtil.getPluginModel(new Path(res));
			if (desc != null) {
				return desc;
			}
		}
		return null;

	}
	*/

/*
	private void doDialogFieldChanged(DialogField field) {
		if (field == fUseDefaultCheckBox) {
			boolean useDefault = fUseDefaultCheckBox.isSelected();
			fWorkspacePluginsList.setEnabled(!useDefault);
			fExternalPluginsList.setEnabled(!useDefault);
		} else if (field == fWorkspacePluginsList) {
		} else if (field == fExternalPluginsList) {
		}
	}
*/
	
	private void updateStatus() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
	}

	private IStatus validatePlugins() {
		/*
		List plugins = Arrays.asList(getPlugins());
		if (plugins.isEmpty()) {
			return createStatus(IStatus.ERROR, "No plugins available.");
		}
		PluginDescriptorModel boot =
			PluginUtil.findPlugin("org.eclipse.core.boot", plugins);
		if (boot == null) {
			return createStatus(IStatus.ERROR, "Plugin 'org.eclipse.core.boot' not found.");
		}
		if (PluginUtil.findPlugin("org.eclipse.ui", plugins) != null) {
			if (PluginUtil.findPlugin("org.eclipse.sdk", plugins) == null) {
				return createStatus(
					IStatus.WARNING,
					"'org.eclipse.sdk' not found. It is implicitly required by 'org.eclipse.ui'.");
			}
			try {
				File bootDir = new File(new URL(boot.getLocation()).getFile());
				File installDir = new File(bootDir.getParentFile().getParentFile(), "install");
				if (!installDir.exists()) {
					return createStatus(
						IStatus.WARNING,
						installDir.getPath()
							+ " not found.\nThe install directory is required by 'org.eclipse.ui'.");
				}
			} catch (MalformedURLException e) {
			}
		};
		*/
		return createStatus(IStatus.OK, "");
	}

	/**
	 * Returns the selected plugins.
	 */
/*
	public PluginModel[] getPlugins() {
		ArrayList res = new ArrayList();
		boolean useDefault = fUseDefaultCheckBox.isSelected();
		if (useDefault) {
			res.addAll(fWorkspacePluginsList.getElements());
		} else {
			res.addAll(fWorkspacePluginsList.getCheckedElements());
			res.addAll(fExternalPluginsList.getElements());
		}
		return (PluginModel[]) res.toArray(new PluginModel[res.size()]);
	}
*/
}