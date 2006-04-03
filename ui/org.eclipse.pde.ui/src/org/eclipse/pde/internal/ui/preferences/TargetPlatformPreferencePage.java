/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.preferences;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.ExternalModelManager;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.TargetDefinitionManager;
import org.eclipse.pde.internal.core.itarget.IAdditionalLocation;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.target.TargetModel;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.target.OpenTargetProfileAction;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class TargetPlatformPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final int PLUGINS_INDEX = 0;
	public static final int ENVIRONMENT_INDEX = 1;
	public static final int SOURCE_INDEX = 4;
	
	private Label fHomeLabel;
	private Combo fHomeText;
	private Combo fProfileCombo;
	private Button fBrowseButton;
	private Button fLoadProfileButton;
	
	private TargetPluginsTab fPluginsTab;
	private TargetEnvironmentTab fEnvironmentTab;
	private JavaArgumentsTab fArgumentsTab;
	private TargetImplicitPluginsTab fImplicitDependenciesTab;
	private TargetSourceTab fSourceTab;
	private IConfigurationElement [] fElements;
	
	private Preferences fPreferences = null;
	private boolean fNeedsReload = false;
	private String fOriginalText;
	private int fIndex;
	private TabFolder fTabFolder;
	private boolean fContainsWorkspaceProfile = false;
	private boolean fFirstClick = true;
	private Button fResetButton;
	
	/**
	 * MainPreferencePage constructor comment.
	 */
	public TargetPlatformPreferencePage() {
		this(PLUGINS_INDEX);
	}
	
	public TargetPlatformPreferencePage(int index) {
		setDescription(PDEUIMessages.Preferences_TargetPlatformPage_Description); 
		fPreferences = PDECore.getDefault().getPluginPreferences();
		fPluginsTab = new TargetPluginsTab(this);
		fIndex = index;
	}
	
	public void dispose() {
		fPluginsTab.dispose();
		fSourceTab.dispose();
		super.dispose();
	}

	public Control createContents(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		container.setLayout(layout);

		createCurrentTargetPlatformGroup(container);
		createTargetProfilesGroup(container);
		
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.TARGET_PLATFORM_PREFERENCE_PAGE);
		return container;
	}
	
	private void createTargetProfilesGroup(Composite container) {
		Group profiles = new Group(container, SWT.NONE);
		profiles.setText(PDEUIMessages.TargetPlatformPreferencePage_TargetGroupTitle);
		profiles.setLayout(new GridLayout(4, false));
		profiles.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
				
		Link profile = new Link(profiles, SWT.NONE);
		profile.setText(PDEUIMessages.TargetPlatformPreferencePage_CurrentProfileLabel);
		profile.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!fProfileCombo.getText().equals("")) //$NON-NLS-1$
					new OpenTargetProfileAction(getShell(), getTargetModel()).run();
			}
		});
		
		fProfileCombo = new Combo(profiles, SWT.BORDER | SWT.READ_ONLY);
		loadTargetCombo();
		fProfileCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button browse = new Button(profiles, SWT.PUSH);
		browse.setText(PDEUIMessages.TargetPlatformPreferencePage_BrowseButton);
		GridData gd = new GridData();
		browse.setLayoutData(gd);
		browse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleTargetBrowse();
			}			
		});
		SWTUtil.setButtonDimensionHint(browse);
		
		fLoadProfileButton = new Button(profiles, SWT.PUSH);
		fLoadProfileButton.setText(PDEUIMessages.TargetPlatformPreferencePage_ApplyButton);
		fLoadProfileButton.setLayoutData(new GridData());
		fLoadProfileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleLoadTargetProfile();
			}
		});
		fLoadProfileButton.setEnabled(!fProfileCombo.getText().equals("")); //$NON-NLS-1$
		
		fProfileCombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fLoadProfileButton.setEnabled(!fProfileCombo.getText().equals("")); //$NON-NLS-1$
			}
		});
		
		SWTUtil.setButtonDimensionHint(fLoadProfileButton);
	}
	
	private void createCurrentTargetPlatformGroup(Composite container) {
		Composite target = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout(4, false);
		layout.marginHeight = layout.marginWidth = 0;
		target.setLayout(layout);
		target.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fHomeLabel = new Label(target, SWT.NULL);
		fHomeLabel.setText(PDEUIMessages.Preferences_TargetPlatformPage_PlatformHome); 
		
		fHomeText = new Combo(target, SWT.NONE);
		fHomeText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ArrayList locations = new ArrayList();
		for (int i = 0; i < 5; i++) {
			String value = fPreferences.getString(ICoreConstants.SAVED_PLATFORM + i);
			if (value.equals(""))  //$NON-NLS-1$
				break;
			locations.add(value);
		}
		String homeLocation = fPreferences.getString(ICoreConstants.PLATFORM_PATH);
		if (!locations.contains(homeLocation))
			locations.add(0, homeLocation);
		fHomeText.setItems((String[])locations.toArray(new String[locations.size()]));
		fHomeText.setText(homeLocation);
		fOriginalText = fHomeText.getText();
		fHomeText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				fNeedsReload = true;
			}
		});
		fHomeText.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fFirstClick)
					fFirstClick = false;
				else {
					fPluginsTab.handleReload(new ArrayList());
					resetTargetProfile();
					fNeedsReload = false;
				}
			}
		});
		
		fBrowseButton = new Button(target, SWT.PUSH);
		fBrowseButton.setText(PDEUIMessages.Preferences_TargetPlatformPage_PlatformHome_Button); 
		fBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fBrowseButton);
		fBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		fResetButton = new Button(target, SWT.PUSH);
		fResetButton.setText(PDEUIMessages.TargetPlatformPreferencePage_reset); 
		fResetButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		SWTUtil.setButtonDimensionHint(fResetButton);
		fResetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fHomeText.setText(ExternalModelManager.computeDefaultPlatformPath());
				fPluginsTab.handleReload(new ArrayList());
				resetTargetProfile();
			}
		});	
		
		fTabFolder = new TabFolder(target, SWT.NONE);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 4;
		fTabFolder.setLayoutData(gd);
		
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {	
			createPluginsTab(fTabFolder);
			createEnvironmentTab(fTabFolder);
			createArgumentsTab(fTabFolder);
			createExplicitTab(fTabFolder);
			createSourceTab(fTabFolder);
			fTabFolder.setSelection(fIndex);
			}
		});
		
		fTabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (fTabFolder.getSelectionIndex() == ENVIRONMENT_INDEX) {
					fEnvironmentTab.updateChoices();
				}
			}
		});		
	}
	
	private void createPluginsTab(TabFolder folder) {
		Control block = fPluginsTab.createContents(folder);
		block.setLayoutData(new GridData(GridData.FILL_BOTH));	
		fPluginsTab.initialize();

		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_pluginsTab); 
		tab.setControl(block);	
	}
	
	private void createEnvironmentTab(TabFolder folder) {
		fEnvironmentTab = new TargetEnvironmentTab();
		Control block = fEnvironmentTab.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_environmentTab); 
		tab.setControl(block);
	}
	
	private void createSourceTab(TabFolder folder) {
		fSourceTab = new TargetSourceTab();
		Control block = fSourceTab.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_sourceCode);  
		tab.setControl(block);
	}
	
	private void createExplicitTab(TabFolder folder) {
		fImplicitDependenciesTab = new TargetImplicitPluginsTab(this);
		Control block = fImplicitDependenciesTab.createContents(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_implicitTab);
		tab.setControl(block);
	}
	
	private void createArgumentsTab(TabFolder folder) {
		fArgumentsTab = new JavaArgumentsTab(this);
		Control block = fArgumentsTab.createControl(folder);
		
		TabItem tab = new TabItem(folder, SWT.NONE);
		tab.setText(PDEUIMessages.TargetPlatformPreferencePage_agrumentsTab);
		tab.setControl(block);
	}

	String getPlatformPath() {
		return fHomeText.getText();
	}

	private void handleBrowse() {
		DirectoryDialog dialog = new DirectoryDialog(getShell());
		dialog.setMessage(PDEUIMessages.TargetPlatformPreferencePage_chooseInstall);
		if (fHomeText.getText().length() > 0)
			dialog.setFilterPath(fHomeText.getText());
		String newPath = dialog.open();
		if (newPath != null
				&& !ExternalModelManager.arePathsEqual(new Path(fHomeText.getText()), new Path(newPath))) {
			if (fHomeText.indexOf(newPath) == -1)
				fHomeText.add(newPath, 0);
			fHomeText.setText(newPath);
			fPluginsTab.handleReload(new ArrayList());
			fNeedsReload = false;
			resetTargetProfile();
		}
	}
	
	private void loadTargetCombo() {
		String prefId = null;
		String pref = fPreferences.getString(ICoreConstants.TARGET_PROFILE);
		fProfileCombo.add(""); //$NON-NLS-1$
		
		if (pref.startsWith("${workspace_loc:")) { //$NON-NLS-1$
			try {
				pref = pref.substring(16, pref.length() -1);
				IPath targetPath = new Path(pref);
				IFile file = PDEPlugin.getWorkspace().getRoot().getFile(targetPath);
				// If a saved workspace profile no longer exists in the workspace, skip it.
				if (file != null && file.exists()) {
					TargetModel model = new TargetModel();
					model.load(file.getContents(), false);
					String value = model.getTarget().getName();
					value = value + " [" + file.getFullPath().toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
					if (fProfileCombo.indexOf(value) == -1)
						fProfileCombo.add(value, 1); //index 0 for "" (null target)
					fProfileCombo.setText(value);
					fContainsWorkspaceProfile = true;
				} 
			}catch (CoreException e) {
			}
		} else if (pref.length() > 3){ //$NON-NLS-1$
			prefId = pref.substring(3);
		}
		
		//load all pre-canned (ie. registered via extension) targets 
		fElements = PDECore.getDefault().getTargetProfileManager().getSortedTargets();
		for (int i = 0; i < fElements.length; i++) {
			String name =fElements[i].getAttribute("name"); //$NON-NLS-1$
			String id = fElements[i].getAttribute("id"); //$NON-NLS-1$
			if (fProfileCombo.indexOf(name) == -1)
				fProfileCombo.add(name);
			if (id.equals(prefId))
				fProfileCombo.setText(name);
		}
	}
	
	private void handleTargetBrowse() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.TargetPlatformPreferencePage_FileSelectionTitle); 
		dialog.setMessage(PDEUIMessages.TargetPlatformPreferencePage_FileSelectionMessage); 
		dialog.addFilter(new FileExtensionFilter("target"));  //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());
		IFile target = getTargetFile();
		if (target != null) dialog.setInitialSelection(target);

		if (dialog.open() == Window.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			TargetModel model = new TargetModel();
			try {
				model.load(file.getContents(), false);
				String value = model.getTarget().getName();
				value = value + " [" + file.getFullPath().toString() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				if (fProfileCombo.indexOf(value) == -1) 
					fProfileCombo.add(value, 1); // index 0 for "" null profile so add at 1
				fProfileCombo.setText(value);
				fContainsWorkspaceProfile = true;
			} catch (CoreException e) {
			}
		}
	}
	
	private IFile getTargetFile() {
		if (!fContainsWorkspaceProfile || !(fProfileCombo.getSelectionIndex() < (fProfileCombo.getItemCount() - fElements.length)))
			return null;
		String target = fProfileCombo.getText().trim();
		if (target.equals("")) //$NON-NLS-1$
			return null;
		int beginIndex = target.lastIndexOf('[');
		target = target.substring(beginIndex +1, target.length() - 1);
		IPath targetPath = new Path(target);
		if (targetPath.segmentCount() < 2) 
			return null;
		return PDEPlugin.getWorkspace().getRoot().getFile(targetPath);
	}
	
	private URL getExternalTargetURL() {
		int offSet = fProfileCombo.getItemCount() - fElements.length;
		if (offSet > fProfileCombo.getSelectionIndex())
			return null;
		IConfigurationElement elem = fElements[fProfileCombo.getSelectionIndex() - offSet];
		String path = elem.getAttribute("definition");  //$NON-NLS-1$
		String symbolicName = elem.getDeclaringExtension().getNamespaceIdentifier();
		return TargetDefinitionManager.getResourceURL(symbolicName, path);
	}
	
	private String getTargetDescription() {
		int offSet = fProfileCombo.getItemCount() - fElements.length;
		if (offSet > fProfileCombo.getSelectionIndex())
			return null;
		IConfigurationElement elem = fElements[fProfileCombo.getSelectionIndex() - offSet];
		IConfigurationElement [] children = elem.getChildren("description"); //$NON-NLS-1$
		if (children.length > 0) 
			return children[0].getValue();
		return null;
	}

	public void init(IWorkbench workbench) {
	}
	
	private ITargetModel getTargetModel() {
		InputStream stream = null;
		try {
			IFile file = getTargetFile();
			String desc = null;
			if (file != null)
				stream = file.getContents();
			if (stream == null) {
				URL url = getExternalTargetURL();
				desc = getTargetDescription();
				if (url != null)
					stream = url.openStream();
			}
			
			if (stream != null) {
				ITargetModel model = new TargetModel();
				model.load(stream, false);
				if (desc != null)
					model.getTarget().setDescription(desc);
				return model;
			}
		} catch (CoreException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}
		}
		return null;
	}
	
	private void handleLoadTargetProfile() {
		if (fProfileCombo.getText().equals("")) return; //$NON-NLS-1$	
		ITargetModel model = getTargetModel();
		if (model == null) {
			MessageDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage_notFoundTitle, PDEUIMessages.TargetPlatformPreferencePage_notFoundDescription);
			return;
		}
		
		if (!model.isLoaded()) {
			MessageDialog.openError(getShell(), PDEUIMessages.TargetPlatformPreferencePage_invalidTitle, PDEUIMessages.TargetPlatformPreferencePage_invalidDescription);
			return;
		}
		
		ITarget target = model.getTarget();
		ILocationInfo info = target.getLocationInfo();
		String path;
		if (info == null || info.useDefault()) {
			path = ExternalModelManager.computeDefaultPlatformPath();
		} else {
			try {
				IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
				path = manager.performStringSubstitution(info.getPath());
			} catch (CoreException e) {
				return;
			}			
		}
		if (!ExternalModelManager.arePathsEqual(new Path(path), new Path(fHomeText.getText()))
				|| !areAdditionalLocationsEqual(target)) {
			fHomeText.setText(path);
			ArrayList additional = new ArrayList();
			IAdditionalLocation[] locations = target.getAdditionalDirectories();
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			for (int i = 0; i < locations.length; i++) {
				try {
					additional.add(manager.performStringSubstitution(locations[i].getPath()));
				} catch (CoreException e) {
					additional.add(locations[i]);
				}			
			}
			fPluginsTab.handleReload(additional);
		}
		
		fPluginsTab.loadTargetProfile(target);
		fEnvironmentTab.loadTargetProfile(target);
		fArgumentsTab.loadTargetProfile(target);
		fImplicitDependenciesTab.loadTargetProfile(target);
		fSourceTab.loadTargetProfile(target);
	}
	
	private boolean areAdditionalLocationsEqual(ITarget target) {
		IAdditionalLocation[] addtionalLocs = target.getAdditionalDirectories();
		Preferences preferences = PDECore.getDefault().getPluginPreferences();
		String value = preferences.getString(ICoreConstants.ADDITIONAL_LOCATIONS);
		StringTokenizer tokenzier = new StringTokenizer(value);
		if (addtionalLocs.length != tokenzier.countTokens()) 
			return false;
		while (tokenzier.hasMoreTokens()) {
			boolean found = false;
			String location = tokenzier.nextToken();
			for (int i = 0; i < addtionalLocs.length; i++) {
				if (addtionalLocs[i].getPath().equals(location)) {
					found = true;
					break;
				}
			}
			if (!found) 
				return false;
		}
		return true;
	}
	
	public void performDefaults() {
		fHomeText.setText(ExternalModelManager.computeDefaultPlatformPath());
		fPluginsTab.handleReload(new ArrayList());
		fEnvironmentTab.performDefaults();
		fArgumentsTab.performDefaults();
		fImplicitDependenciesTab.performDefauls();
		fSourceTab.performDefaults();
		resetTargetProfile();
		super.performDefaults();
	}

	public boolean performOk() {
		fEnvironmentTab.performOk();
		if (fNeedsReload && !ExternalModelManager.arePathsEqual(new Path(fOriginalText), new Path(fHomeText.getText()))) {
			MessageDialog dialog =
				new MessageDialog(
					getShell(),
					PDEUIMessages.Preferences_TargetPlatformPage_title, 
					null,
					PDEUIMessages.Preferences_TargetPlatformPage_question, 
					MessageDialog.QUESTION,
					new String[] {
						IDialogConstants.YES_LABEL,
						IDialogConstants.NO_LABEL},
					1);
			if (dialog.open() == 1) {
				getContainer().updateButtons();
				return false;
			}
			fPluginsTab.handleReload(new ArrayList());
			resetTargetProfile();
		} 
		fSourceTab.performOk();
		fPluginsTab.performOk();
		fArgumentsTab.performOk();
		fImplicitDependenciesTab.performOk();
		saveTarget();
		return super.performOk();
	}
	
	private void saveTarget() {
		if (fProfileCombo.getText().equals("")) //$NON-NLS-1$
			fPreferences.setValue(ICoreConstants.TARGET_PROFILE, ""); //$NON-NLS-1$ 
		else if (fContainsWorkspaceProfile && (fProfileCombo.getSelectionIndex() < (fProfileCombo.getItemCount() - fElements.length))) {
			String value = fProfileCombo.getText().trim();
			int index = value.lastIndexOf('[');
			value = value.substring(index + 1, value.length() - 1);
			fPreferences.setValue(ICoreConstants.TARGET_PROFILE, "${workspace_loc:" + value + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			int offSet = fProfileCombo.getItemCount() - fElements.length;
			IConfigurationElement elem = fElements[fProfileCombo.getSelectionIndex() - offSet];
			fPreferences.setValue(ICoreConstants.TARGET_PROFILE, "id:" + elem.getAttribute("id")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}
	
	public String[] getPlatformLocations() {
		return fHomeText.getItems();
	}
	 
	public void resetNeedsReload() {
		fNeedsReload = false;
		String location = fHomeText.getText();
		if (fHomeText.indexOf(location) == -1)
			fHomeText.add(location, 0);
	}
	
	public void resetTargetProfile() {
		fProfileCombo.select(0);
	}

	public TargetSourceTab getSourceBlock() {
		return fSourceTab;
	}
	
	protected IPluginModelBase[] getCurrentModels() {
		return fPluginsTab.getCurrentModels();
	}
	
	protected PDEState getCurrentState() {
		return fPluginsTab.getCurrentState();
	}
	
	protected String[] getImplicitPlugins() {
		return fImplicitDependenciesTab.getImplicitPlugins();
	}
}
