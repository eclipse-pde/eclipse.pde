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

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
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
	protected Button browseButton;
	protected String initialId;
	protected String initialName;
	protected boolean isInitialized = false;
	protected IFeatureModel fFeatureToPatch;

	public static final String PATCH_ID = "NewFeaturePatch.SpecPage.id";
	public static final String PATCH_NAME = "NewFeaturePatch.SpecPage.name";
	public static final String PATCH_PROVIDER = "NewFeaturePatch.SpecPage.provider";

	public static final String FEATURE_ID = "NewFeatureWizard.SpecPage.id";
	public static final String FEATURE_NAME = "NewFeatureWizard.SpecPage.name";
	public static final String FEATURE_VERSION = "NewFeatureWizard.SpecPage.version";
	public static final String FEATURE_PROVIDER = "NewFeatureWizard.SpecPage.provider";

	public static final String KEY_VERSION_FORMAT = "NewFeatureWizard.SpecPage.versionFormat";
	public static final String KEY_INVALID_ID = "NewFeatureWizard.SpecPage.invalidId";
	public static final String KEY_MISSING = "NewFeatureWizard.SpecPage.missing";
	
	public BaseFeatureSpecPage(WizardNewProjectCreationPage mainPage,
			boolean isPatch) {
		super("specPage");
		this.isPatch = isPatch;
		this.mainPage = mainPage;
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 9;
		container.setLayout(layout);

		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyComplete();
			}
		};

		if (isPatch()) {
			Label label = new Label(container, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(PATCH_ID));
			patchIdText = new Text(container, SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			patchIdText.setLayoutData(gd);
			if (initialId != null)
				patchIdText.setText(initialId);
			patchIdText.addModifyListener(listener);

			label = new Label(container, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(PATCH_NAME));
			patchNameText = new Text(container, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchNameText.setLayoutData(gd);
			if (initialName != null)
				patchNameText.setText(initialName);
			patchNameText.addModifyListener(listener);

			label = new Label(container, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(PATCH_PROVIDER));
			patchProviderText = new Text(container, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			patchProviderText.setLayoutData(gd);
			patchProviderText.addModifyListener(listener);

			Group patchGroup = new Group(container, SWT.NULL);
			layout = new GridLayout(2, false);
			layout.marginHeight = layout.marginWidth = 10;
			patchGroup.setLayout(layout);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 2;
			patchGroup.setLayoutData(gd);
			patchGroup.setText("Properties of feature being patched");
			addFeatureProperties(patchGroup, listener);
		} else {
			addFeatureProperties(container, listener);
		}
		
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_FEATURE_DATA);
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
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
			isInitialized = true;
			if (isPatch())
				patchIdText.setFocus();
			else
				featureIdText.setFocus();
		}
	}
	
	protected void initialize(){
	}

	private void addFeatureProperties(Composite container, ModifyListener listener){
		
		if (isPatch()){

			
			Label label = new Label(container, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(FEATURE_ID));
			
			Composite patchContainer = new Composite(container, SWT.NONE);
			GridLayout layout = new GridLayout(2, false);
			layout.marginHeight = layout.marginWidth = 0;
			patchContainer.setLayout(layout);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			gd.horizontalSpan = 1;
			patchContainer.setLayoutData(gd);
			
			featureIdText = new Text(patchContainer, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureIdText.setLayoutData(gd);
			if (initialId != null)
				featureIdText.setText(initialId);
			featureIdText.addModifyListener(listener);
			
			browseButton = new Button(patchContainer, SWT.PUSH);
			browseButton.setText("Browse...");
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
			Label label = new Label(container, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(FEATURE_ID));
			featureIdText = new Text(container, SWT.BORDER);
			GridData gd = new GridData(GridData.FILL_HORIZONTAL);
			featureIdText.setLayoutData(gd);
			if (initialId != null)
				featureIdText.setText(initialId);
			featureIdText.addModifyListener(listener);
			
		}

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(FEATURE_NAME));
		featureNameText = new Text(container, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		featureNameText.setLayoutData(gd);
		if (initialName != null)
			featureNameText.setText(initialName);
		featureNameText.addModifyListener(listener);

		label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(FEATURE_VERSION));
		featureVersionText = new Text(container, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		featureVersionText.setLayoutData(gd);
		featureVersionText.addModifyListener(listener);
		if (!isPatch()) {
			label = new Label(container, SWT.NULL);
			label.setText(PDEPlugin.getResourceString(FEATURE_PROVIDER));
			featureProviderText = new Text(container, SWT.BORDER);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			featureProviderText.setLayoutData(gd);
			featureProviderText.addModifyListener(listener);
		}
	}
	protected String computeInitialId(String projectName) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stok = new StringTokenizer(projectName, ".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)))
					buffer.append(token.charAt(i));
			}
			if (stok.hasMoreTokens()
					&& buffer.charAt(buffer.length() - 1) != '.')
				buffer.append(".");
		}
		return buffer.toString();
	}

	protected boolean verifyVersion() {
		String value = featureVersionText.getText();
		boolean result = true;
		if (value.length() == 0)
			result = false;
		try {
			new PluginVersionIdentifier(value);
		} catch (Throwable e) {
			result = false;
		}
		if (result == false) {
			setPageComplete(false);
			setErrorMessage(PDEPlugin.getResourceString(KEY_VERSION_FORMAT));
		}
		return result;
	}

	protected String verifyIdRules() {
		String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
		String name = featureIdText.getText();
		StringTokenizer stok = new StringTokenizer(name, ".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)) == false)
					return problemText;
			}
		}
		if (isPatch()){
			name = patchIdText.getText();
			stok = new StringTokenizer(name, ".");
			while (stok.hasMoreTokens()) {
				String token = stok.nextToken();
				for (int i = 0; i < token.length(); i++) {
					if (Character.isLetterOrDigit(token.charAt(i)) == false)
						return problemText;
				}
			}
		}
		return null;
	}
	
	public IFeatureModel getFeatureToPatch(){
		return fFeatureToPatch;
	}
	
	public static IFeatureModel[] getAllFeatureModels(){
		/* get features from target platform */
		ArrayList models = new ArrayList();
		IPath targetPath = ExternalModelManager.getEclipseHome();
		File targetPlatform = targetPath.toFile();
		if (targetPlatform.isDirectory()){
			File mainFeatureDir = targetPath.append("features").toFile(); //$NON-NLS-1$
			if (mainFeatureDir.exists() == false || !mainFeatureDir.isDirectory())
				return null;
			File[] featureDirs = mainFeatureDir.listFiles();

			
			

			for (int i = 0; i < featureDirs.length; i++) {
				PluginVersionIdentifier bestVid = null;
				File bestDir = null;
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
					if (error || !model.isLoaded())
						continue;
					models.add(model);
				}
			}
		}
		IFeatureModel[] targetModels = (IFeatureModel[])models.toArray(new IFeatureModel[models.size()]);
		
		WorkspaceModelManager mng = PDECore.getDefault()
		.getWorkspaceModelManager();
		IFeatureModel[] workspaceModels = mng.getFeatureModels();

		IFeatureModel[] allModels = new IFeatureModel[workspaceModels.length + targetModels.length];
		for (int i = 0; i<workspaceModels.length; i++){
			allModels[i] = workspaceModels[i];
		}
		for (int i = 0; i<targetModels.length;i++){
			allModels[i + workspaceModels.length] = targetModels[i];
		}
		return allModels;
	}
}
