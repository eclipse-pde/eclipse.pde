/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.PDEPlugin;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.wizards.StatusWizardPage;

import org.eclipse.jface.dialogs.IDialogSettings;

public class WorkbenchLauncherWizardAdvancedPage extends StatusWizardPage {
	
	private static final String SETTINGS_USECUSTOM= "default";
	private static final String SETTINGS_WSPROJECT= "wsproject";
	private static final String SETTINGS_EXTPLUGINS= "extplugins";
	
	private static final String SETTINGS_PREVPATH= "prevpath";
		
	private SelectionButtonDialogField fUseDefaultCheckBox;
	private CheckedListDialogField fWorkspacePluginsList;
	private ListDialogField fExternalPluginsList;
	
	public WorkbenchLauncherWizardAdvancedPage(String title) {
		super("WorkbenchLauncherWizardAdvancedPage", false);
		setTitle(title);
		setDescription("Plugins visible to the plugin loader.");
		
		fUseDefaultCheckBox= new SelectionButtonDialogField(SWT.CHECK);
		fUseDefaultCheckBox.setDialogFieldListener(listener);
		fUseDefaultCheckBox.setLabelText("&Use default (All projects in workspace visible)");
		
		IListAdapter listAdapter= new IListAdapter() {
			public void customButtonPressed(DialogField field, int index) {
				doButtonPressed(field, index);
			}
				
			public void selectionChanged(DialogField field) {}
		};	
		String[] buttonNames= new String[] {
			/* 0 */ "&Select All",
			/* 1 */ "&Deselect All"
		};
		
		fWorkspacePluginsList= new CheckedListDialogField(listAdapter, buttonNames, new PluginModelLabelProvider(true, true));
		fWorkspacePluginsList.setDialogFieldListener(listener);
		fWorkspacePluginsList.setLabelText("Wor&kspace projects that are valid plugins or fragments:");
		fWorkspacePluginsList.setCheckAllButtonIndex(0);
		fWorkspacePluginsList.setUncheckAllButtonIndex(1);
		
		buttonNames= new String[] {
			/* 0 */ "&Add",
			/* 1 */ null,
			/* 2 */ "&Remove"
		};
		fExternalPluginsList= new ListDialogField(listAdapter, buttonNames, new PluginModelLabelProvider(true, true));
		fExternalPluginsList.setDialogFieldListener(listener);
		fExternalPluginsList.setLabelText("E&xternal plugins (Location of 'plugin.xml' or 'fragment.xml').\nNote that the debugger will not show source for these plugins.");
		fExternalPluginsList.setRemoveButtonIndex(2);
		
		initializeFields(initialSettings);
	}
	
	
	private void initializeFields(IDialogSettings initialSettings) {
		IWorkspaceRoot root= SelfHostingPlugin.getWorkspace().getRoot();
		IProject[] projects= root.getProjects();
		ArrayList available= new ArrayList();
		for (int i= 0; i < projects.length; i++) {
			IProject curr= projects[i];
			IFile pluginXMLFile= curr.getFile("plugin.xml");
			if (!pluginXMLFile.exists()) {
				pluginXMLFile= curr.getFile("fragment.xml");
			}
			if (pluginXMLFile.exists()) {
				PluginModel desc= PluginUtil.getPluginModel(pluginXMLFile.getLocation());
				if (desc != null) {
					available.add(desc);
				}
			}	
		}
		fWorkspacePluginsList.setElements(available);
		
		boolean useDefault= true;
		
		ArrayList checkedPlugins= new ArrayList();
		checkedPlugins.addAll(available);
		
		ArrayList externalPlugins= new ArrayList();
		
		if (initialSettings != null) {
			useDefault= !initialSettings.getBoolean(SETTINGS_USECUSTOM);
			
			String deselectedPluginIDs= initialSettings.get(SETTINGS_WSPROJECT);
			if (deselectedPluginIDs != null) {
				ArrayList deselected= new ArrayList();
				StringTokenizer tok= new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
				while (tok.hasMoreTokens()) {
					deselected.add(tok.nextToken());
				}
				for (int i= checkedPlugins.size() - 1; i>= 0; i--) {
					PluginModel desc= (PluginModel) checkedPlugins.get(i);
					if (deselected.contains(desc.getId())) {
						checkedPlugins.remove(i);
					}
				}
			}
			
			String ext= initialSettings.get(SETTINGS_EXTPLUGINS);
			if (ext != null) {
				ArrayList urls= new ArrayList();
				StringTokenizer tok= new StringTokenizer(ext, File.pathSeparator);
				while (tok.hasMoreTokens()) {
					try {
						urls.add(new URL(tok.nextToken()));
					} catch (MalformedURLException e) {
						SelfHostingPlugin.log(e);
					}
				}
				URL[] urlsArray= (URL[]) urls.toArray(new URL[urls.size()]);
				PluginModel[] descs= PluginUtil.getPluginModels(urlsArray);
				if (descs != null) {
					externalPlugins.addAll(Arrays.asList(descs));
				}
			}
		}
		
		fUseDefaultCheckBox.setSelection(useDefault);
		fWorkspacePluginsList.setCheckedElements(checkedPlugins);
		fExternalPluginsList.setElements(externalPlugins);
	}
	
	
	public void storeSettings() {
		IDialogSettings initialSettings = getDialogSettings();
		initialSettings.put(SETTINGS_USECUSTOM, !fUseDefaultCheckBox.isSelected());

		StringBuffer buf= new StringBuffer();
		// store deselected projects
		List selectedProjects= fWorkspacePluginsList.getCheckedElements();
		List projects= fWorkspacePluginsList.getElements(); 
		for (int i= 0; i < projects.size(); i++) {
			PluginModel curr= (PluginModel) projects.get(i);
			if (!selectedProjects.contains(curr)) {
				buf.append(curr.getId());
				buf.append(File.pathSeparatorChar);
			}
		}
		initialSettings.put(SETTINGS_WSPROJECT, buf.toString());
		
		buf= new StringBuffer();
		List external= fExternalPluginsList.getElements();
		for (int i= 0; i < external.size(); i++) {
			PluginModel curr= (PluginModel) external.get(i);
			buf.append(curr.getLocation());
			if (curr instanceof PluginDescriptorModel) {
				buf.append("/plugin.xml");
			} else if (curr instanceof PluginFragmentModel) {
				buf.append("/fragment.xml");
			}
			buf.append(File.pathSeparatorChar);
		}			
		initialSettings.put(SETTINGS_EXTPLUGINS, buf.toString());	
	}
		
	/*
	 * @see WizardPage#createControl
	 */	
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NONE);
		
		MGridLayout layout= new MGridLayout();
		layout.numColumns= 2;
		layout.minimumWidth= convertWidthInCharsToPixels(60);
		composite.setLayout(layout);
		
		fUseDefaultCheckBox.doFillIntoGrid(composite, 2);
				
		fWorkspacePluginsList.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fWorkspacePluginsList.getLabelControl(null), 2);
		
		fExternalPluginsList.doFillIntoGrid(composite, 3);
		LayoutUtil.setHorizontalSpan(fExternalPluginsList.getLabelControl(null), 2);		

		MGridData gd= (MGridData)fExternalPluginsList.getListControl(null).getLayoutData();
		gd.heightHint= convertHeightInCharsToPixels(6);
	
		setControl(composite);
	}
	
	private void doButtonPressed(DialogField field, int index) {
		if (field == fExternalPluginsList) {
			if (index == 0) {
				PluginModel ext= chooseExternalPlugin();
				if (ext != null) {
					fExternalPluginsList.addElement(ext);
				}
			}
		}
	}
	
	private PluginModel chooseExternalPlugin() {
		String prevPath= getDialogSettings().get(SETTINGS_PREVPATH);
		
		FileDialog dialog= new FileDialog(getShell());
		dialog.setText("External plugins (plugin.xml or fragment.xml file)");
		dialog.setFilterExtensions(new String[] {"plugin.xml", "fragment.xml"});
		if (prevPath != null) {
			dialog.setFilterPath(prevPath);
		}
		String res= dialog.open();
		if (res != null) {
			getDialogSettings().put(SETTINGS_PREVPATH, dialog.getFilterPath());
			PluginModel desc= PluginUtil.getPluginModel(new Path(res));
			if (desc != null) {
				return desc;
			}
		}
		return null;

	}

	private void doDialogFieldChanged(DialogField field) {
		if (field == fUseDefaultCheckBox) {
			boolean useDefault= fUseDefaultCheckBox.isSelected();
			fWorkspacePluginsList.setEnabled(!useDefault);
			fExternalPluginsList.setEnabled(!useDefault);
		} else if (field == fWorkspacePluginsList) {
		} else if (field == fExternalPluginsList) {
		}
		IStatus genStatus= validatePlugins();
		updateStatus(genStatus);
	}
	
	private IStatus validatePlugins() {
		List plugins= Arrays.asList(getPlugins());
		if (plugins.isEmpty()) {
			return createStatus(IStatus.ERROR, "No plugins available.");
		}
		PluginDescriptorModel boot= PluginUtil.findPlugin("org.eclipse.core.boot", plugins);
		if (boot == null) {
			return createStatus(IStatus.ERROR, "Plugin 'org.eclipse.core.boot' not found.");
		}
		if (PluginUtil.findPlugin("org.eclipse.ui", plugins) != null) {
			if (PluginUtil.findPlugin("org.eclipse.sdk", plugins) == null) {
				return createStatus(IStatus.WARNING, "'org.eclipse.sdk' not found. It is implicitly required by 'org.eclipse.ui'.");
			}
			try {
				File bootDir= new File(new URL(boot.getLocation()).getFile());
				File installDir= new File(bootDir.getParentFile().getParentFile(), "install");
				if (!installDir.exists()) {
					return createStatus(IStatus.WARNING, installDir.getPath() + " not found.\nThe install directory is required by 'org.eclipse.ui'.");
				}
			} catch (MalformedURLException e) {
			}			
		};	
		return createStatus(IStatus.OK, "");
	}

	/**
	 * Returns the selected plugins.
	 */		
	public PluginModel[] getPlugins() {
		ArrayList res= new ArrayList();
		boolean useDefault= fUseDefaultCheckBox.isSelected();
		if (useDefault) {
			res.addAll(fWorkspacePluginsList.getElements());
		} else {
			res.addAll(fWorkspacePluginsList.getCheckedElements());
			res.addAll(fExternalPluginsList.getElements());
		}
		return (PluginModel[]) res.toArray(new PluginModel[res.size()]);
	}
	
		
}
