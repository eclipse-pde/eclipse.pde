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

package org.eclipse.pde.internal.ui.wizards.feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.ExternalFeatureModel;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.help.*;

/**
 * @author cgwong
 *  
 */
public abstract class BaseFeatureSpecPage extends WizardPage {
	
	private boolean isPatch;
	protected WizardNewProjectCreationPage mainPage;
	protected Text featureIdText;
	protected Text featureNameText;
	protected Text featureVersionText;
	protected Text featureProviderText;
	protected Text patchIdText;
	protected Text patchNameText;
	protected Text patchProviderText;
	protected Text libraryText;
	protected Button browseButton;
	protected Button customChoice;
	protected String initialId;
	protected String initialName;
	protected Label libraryLabel;
	protected boolean isInitialized = false;
	protected IFeatureModel fFeatureToPatch;
	
	public static final String PATCH_ID = "NewFeaturePatch.SpecPage.id"; //$NON-NLS-1$
	public static final String PATCH_NAME = "NewFeaturePatch.SpecPage.name"; //$NON-NLS-1$
	public static final String PATCH_PROVIDER = "NewFeaturePatch.SpecPage.provider"; //$NON-NLS-1$
	
	public static final String FEATURE_ID = "NewFeatureWizard.SpecPage.id"; //$NON-NLS-1$
	public static final String FEATURE_NAME = "NewFeatureWizard.SpecPage.name"; //$NON-NLS-1$
	public static final String FEATURE_VERSION = "NewFeatureWizard.SpecPage.version"; //$NON-NLS-1$
	public static final String FEATURE_PROVIDER = "NewFeatureWizard.SpecPage.provider"; //$NON-NLS-1$
	public static final String KEY_LIBRARY = "NewFeatureWizard.SpecPage.library"; //$NON-NLS-1$
	
	public static final String KEY_VERSION_FORMAT = "NewFeatureWizard.SpecPage.versionFormat"; //$NON-NLS-1$
	public static final String KEY_INVALID_ID = "NewFeatureWizard.SpecPage.invalidId"; //$NON-NLS-1$
	public static final String KEY_MISSING = "NewFeatureWizard.SpecPage.missing"; //$NON-NLS-1$
	public static final String KEY_PMISSING = "NewFeatureWizard.SpecPage.pmissing"; //$NON-NLS-1$
	public static final String KEY_LIBRARY_MISSING = "NewFeatureWizard.SpecPage.error.library"; //$NON-NLS-1$
	
	private static final String KEY_CUSTOM_INSTALL_HANDLER =
		"NewFeatureWizard.SpecPage.customProject"; //$NON-NLS-1$
	private static final String KEY_PATCH_CUSTOM_INSTALL_HANDLER =
		"NewFeatureWizard.SpecPage.patch.customProject"; //$NON-NLS-1$
	public BaseFeatureSpecPage(WizardNewProjectCreationPage mainPage,
			boolean isPatch) {
		super("specPage"); //$NON-NLS-1$
		this.isPatch = isPatch;
		this.mainPage = mainPage;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 12;
		layout.horizontalSpacing = 9;
		container.setLayout(layout);
		
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyComplete();
			}
		};
		
		if (isPatch()) {
			Group patchPropertiesGroup = new Group(container, SWT.NULL);
			layout = new GridLayout(2, false);
			patchPropertiesGroup.setLayout(layout);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			patchPropertiesGroup.setLayoutData(gd);
			patchPropertiesGroup.setText(PDEPlugin.getResourceString("NewFeatureWizard.SpecPage.patchProperties")); //$NON-NLS-1$
			Label label = new Label(patchPropertiesGroup, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(PATCH_ID));
			patchIdText = new Text(patchPropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchIdText.setLayoutData(gd);
			if (initialId != null)
				patchIdText.setText(initialId);
			patchIdText.addModifyListener(listener);
			
			label = new Label(patchPropertiesGroup, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(PATCH_NAME));
			patchNameText = new Text(patchPropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchNameText.setLayoutData(gd);
			if (initialName != null)
				patchNameText.setText(initialName);
			patchNameText.addModifyListener(listener);
			
			label = new Label(patchPropertiesGroup, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(PATCH_PROVIDER));
			patchProviderText = new Text(patchPropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchProviderText.setLayoutData(gd);
			patchProviderText.addModifyListener(listener);
		}
		addFeatureProperties(container, listener);
		addCustomInstallHandlerSection(container, listener);
		
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_FEATURE_DATA);
	}
	private void addCustomInstallHandlerSection(Composite parent, ModifyListener listener) {
		Group customHandlerGroup = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		customHandlerGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		customHandlerGroup.setLayoutData(gd);
		customHandlerGroup.setText(PDEPlugin.getResourceString("BaseFeatureSpecPage.customGroup")); //$NON-NLS-1$

		customChoice = new Button(customHandlerGroup, SWT.CHECK);
		if (!isPatch())
			customChoice.setText(PDEPlugin.getResourceString(KEY_CUSTOM_INSTALL_HANDLER));
		else 
			customChoice.setText(PDEPlugin.getResourceString(KEY_PATCH_CUSTOM_INSTALL_HANDLER));
		customChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = ((Button) e.widget).getSelection();
				libraryText.setEnabled(isSelected);
				libraryLabel.setEnabled(isSelected);
				verifyComplete();
			}
		});
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		customChoice.setLayoutData(gd);
		
		libraryLabel = new Label(customHandlerGroup, SWT.NULL);
		libraryLabel.setText(
			PDEPlugin.getResourceString(KEY_LIBRARY));
		gd = new GridData();
		gd.horizontalIndent = 22;
		libraryLabel.setLayoutData(gd);
		libraryText = new Text(customHandlerGroup, SWT.SINGLE | SWT.BORDER);
		libraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		libraryText.addModifyListener(listener);
		
	}
	public boolean isPatch() {
		return isPatch;
	}
	
	protected abstract void verifyComplete();
	/**
	 * @return Returns the initialName.
	 */
	public String getInitialName() {
		return initialName;
	}
	
	/**
	 * @param initialName
	 *            The initialName to set.
	 */
	public void setInitialName(String initialName) {
		this.initialName = initialName;
	}
	
	/**
	 * 
	 * @param initialId
	 */
	public void setInitialId(String initialId) {
		this.initialId = initialId;
	}
	
	/**
	 * @return Returns the initialId.
	 */
	public String getInitialId() {
		return initialId;
	}
	
	protected void initialize(){
		customChoice.setSelection(false);
		libraryText.setEnabled(false);
		libraryLabel.setEnabled(false);
	}
	
	private void addFeatureProperties(Composite container, ModifyListener listener){
		Group featurePropertiesGroup = new Group(container, SWT.NULL);
		GridLayout layout = new GridLayout(2, false);
		featurePropertiesGroup.setLayout(layout);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		featurePropertiesGroup.setLayoutData(gd);
		
		if (isPatch()){
			featurePropertiesGroup.setText(PDEPlugin.getResourceString("BaseFeatureSpecPage.patchGroup.title")); //$NON-NLS-1$
						
			Label label = new Label(featurePropertiesGroup, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(FEATURE_ID));
			
			Composite patchcontainer = new Composite(featurePropertiesGroup, SWT.NULL);
			layout = new GridLayout(2, false);
			layout.marginHeight = layout.marginWidth =0;
			layout.horizontalSpacing = 5;
			patchcontainer.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			patchcontainer.setLayoutData(gd);
			
			featureIdText = new Text(patchcontainer, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureIdText.setLayoutData(gd);
			if (initialId != null)
				featureIdText.setText(initialId);
			featureIdText.addModifyListener(listener);
			
			browseButton = new Button(patchcontainer, SWT.PUSH);
			browseButton.setText(PDEPlugin.getResourceString("BaseFeatureSpecPage.browse")); //$NON-NLS-1$
			gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
			browseButton.setLayoutData(gd);
			browseButton.addSelectionListener(new SelectionAdapter() {
				
				public void widgetSelected(SelectionEvent e) {
					FeatureSelectionDialog dialog = new FeatureSelectionDialog(getShell(), getAllFeatureModels());
					dialog.create();
					if (dialog.open() == Dialog.OK) {
						Object[] result = dialog.getResult();
						IFeatureModel selectedModel = (IFeatureModel) result[0];
						featureIdText.setText(selectedModel.getFeature().getId());
						featureNameText.setText(selectedModel.getFeature().getLabel());
						featureVersionText.setText(selectedModel.getFeature().getVersion());
						fFeatureToPatch = selectedModel;
					}
				}
			});
			SWTUtil.setButtonDimensionHint(browseButton);
		} else {
			featurePropertiesGroup.setText(PDEPlugin.getResourceString("BaseFeatureSpecPage.featurePropertiesGroup.title")); //$NON-NLS-1$
			
			Label label = new Label(featurePropertiesGroup, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(FEATURE_ID));
			featureIdText = new Text(featurePropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureIdText.setLayoutData(gd);
			if (initialId != null)
				featureIdText.setText(initialId);
			featureIdText.addModifyListener(listener);
			
		}
		
		Label label = new Label(featurePropertiesGroup, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(FEATURE_NAME));
		featureNameText = new Text(featurePropertiesGroup, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		featureNameText.setLayoutData(gd);
		if (initialName != null)
			featureNameText.setText(initialName);
		featureNameText.addModifyListener(listener);
		
		label = new Label(featurePropertiesGroup, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(FEATURE_VERSION));
		featureVersionText = new Text(featurePropertiesGroup, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		featureVersionText.setLayoutData(gd);
		featureVersionText.addModifyListener(listener);
		if (!isPatch()) {
			label = new Label(featurePropertiesGroup, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(FEATURE_PROVIDER));
			featureProviderText = new Text(featurePropertiesGroup, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureProviderText.setLayoutData(gd);
			featureProviderText.addModifyListener(listener);
		}
	}
	protected String computeInitialId(String projectName) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stok = new StringTokenizer(projectName, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)))
					buffer.append(token.charAt(i));
			}
			if (stok.hasMoreTokens()
					&& buffer.charAt(buffer.length() - 1) != '.')
				buffer.append("."); //$NON-NLS-1$
		}
		return buffer.toString();
	}
	
	protected String verifyVersion() {
		String problemText = PDEPlugin.getResourceString(KEY_VERSION_FORMAT);
		String value = featureVersionText.getText();
		if (value.length() == 0)
			return problemText;
		try {
			new PluginVersionIdentifier(value);
		} catch (Throwable e) {
			return problemText;
		}
		return null;
	}
	
	protected String verifyIdRules() {
		String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
		String name = featureIdText.getText();
		if (name == null || name.length() == 0)
			 return PDEPlugin.getResourceString(KEY_MISSING);
		StringTokenizer stok = new StringTokenizer(name, "."); //$NON-NLS-1$
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)) == false)
					return problemText;
			}
		}
		return null;
	}
	
	public IFeatureModel getFeatureToPatch(){
		return fFeatureToPatch;
	}
	
	public IFeatureModel[] getAllFeatureModels(){
		IPath targetPath = ExternalModelManager.getEclipseHome();
		File mainFeatureDir = targetPath.append("features").toFile(); //$NON-NLS-1$
		if (mainFeatureDir.exists() == false || !mainFeatureDir.isDirectory())
			return null;
		File[] featureDirs = mainFeatureDir.listFiles();
		
		PluginVersionIdentifier bestVid = null;
		File bestDir = null;
		ArrayList allModels = new ArrayList();
		
		for (int i = 0; i < featureDirs.length; i++) {
			bestVid = null;
			bestDir = null;
			File featureDir = featureDirs[i];
			String name = featureDir.getName();
			if (featureDir.isDirectory()) {
				int loc = name.lastIndexOf("_"); //$NON-NLS-1$
				if (loc == -1)
					continue;
				String version = name.substring(loc + 1);
				PluginVersionIdentifier vid =
					new PluginVersionIdentifier(version);
				if (bestVid == null || vid.isGreaterThan(bestVid)) {
					bestVid = vid;
					bestDir = featureDir;
				}
			}
			
			if (bestVid == null)
				return null;
			// We have a feature and know the version
			File manifest = new File(bestDir, "feature.xml"); //$NON-NLS-1$
			ExternalFeatureModel model = new ExternalFeatureModel();
			model.setInstallLocation(bestDir.getAbsolutePath());
			
			InputStream stream = null;
			boolean error = false;
			try {
				stream = new FileInputStream(manifest);
				model.load(stream, false);
			} catch (Exception e) {
				error = true;
			}
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
			if (!(error || !model.isLoaded()))
				allModels.add(model);
		}
		WorkspaceModelManager mng = PDECore.getDefault().getWorkspaceModelManager();
		IFeatureModel[] workspaceModels = mng.getFeatureModels();
		for (int i = 0; i<workspaceModels.length; i++){
		if (!isFeatureIncluded(allModels, workspaceModels[i]))
			allModels.add(workspaceModels[i]);
		}
		return (IFeatureModel[])allModels.toArray(new IFeatureModel[allModels.size()]);
	}
	
	protected boolean isFeatureIncluded(ArrayList models, IFeatureModel workspaceModel){
		for (int i = 0; i<models.size(); i++){
			if (!(models.get(i) instanceof IFeatureModel))
				continue;
			IFeatureModel model = (IFeatureModel)models.get(i);
			if (model.getFeature().getId().equals(workspaceModel.getFeature().getId()) 
					&& model.getFeature().getVersion().equals(workspaceModel.getFeature().getVersion()))
				return true;	
		}
		return false;
	}
	
	protected String getInstallHandlerLibrary() {
		String library = libraryText.getText();
		if (library != null && !library.endsWith(".jar")) //$NON-NLS-1$
			library += ".jar"; //$NON-NLS-1$
		return library;
	}
}
