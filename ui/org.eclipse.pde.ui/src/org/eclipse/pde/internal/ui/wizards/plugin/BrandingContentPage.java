/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.*;

/**
 * @author cgwong
 *  
 */
public class BrandingContentPage extends WizardPage {
    // branding var
    private Text fAppText;
    private Text fProductText;
    protected Button fAppButton;
    protected Text fWindowImagesText;
    protected Text fAboutImageText;
    protected Text fSplashText;
    protected Button fUseDefaultButton;
    protected Button fWindowImagesButton;
    protected Button fAboutImageButton;
    protected Button fSplashButton;
    protected Label fWindowsInfoLabel;
    protected Label fWindowsLabel;
    protected Label fAboutInfoLabel;
    protected Label fAboutLabel;
    protected Label fSplashInfoLabel;
    protected Label fSplashLabel;
    protected IProjectProvider fProvider;
    protected BrandingData fData;
    private boolean fProductChanged = false;
    protected boolean isInitialized = false;
    public final static byte WINDOW_IMAGES = 0x0;
    public final static byte ABOUT_IMAGE = 0x1;
    public final static byte SPLASH_IMAGE = 0x2;
    protected ModifyListener imageListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            validatePage();
        }
    };
    protected ModifyListener productListener = new ModifyListener() {
        public void modifyText(ModifyEvent e) {
            fProductChanged = true;
            validatePage();
        }
    };

    public BrandingContentPage(String pageName, IProjectProvider provider) {
        super(pageName);
        setTitle(PDEPlugin.getResourceString("BrandingContentPage.title")); //$NON-NLS-1$
        setDescription(PDEPlugin.getResourceString("BrandingContentPage.desc")); //$NON-NLS-1$
        fData = new BrandingData();
        fProvider = provider;
    }

    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.verticalSpacing = 10;
        container.setLayout(layout);
        addProductGroup(container);
        addImageGroup(container);
        Dialog.applyDialogFont(container);
        setControl(container);
    }

    public void addProductGroup(Composite container) {
        Group group = new Group(container, SWT.NONE);
        group.setText(PDEPlugin.getResourceString("BrandingContentPage.productGroup")); //$NON-NLS-1$
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = layout.marginWidth = 6;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Product name
        Label label = new Label(group, SWT.WRAP);
        label.setText(PDEPlugin.getResourceString("BrandingContentPage.productDesc")); //$NON-NLS-1$
        GridData gd = new GridData();
        gd.horizontalSpan = 3;
        gd.widthHint = 400;
        label.setLayoutData(gd);
        label = new Label(group, SWT.NONE);
        label.setText(PDEPlugin.getResourceString("BrandingContentPage.productName")); //$NON-NLS-1$
        label.setLayoutData(new GridData());
        fProductText = createText(group, productListener);
        gd = (GridData) fProductText.getLayoutData();
        gd.horizontalSpan = 2;
        fProductText.setLayoutData(gd);
        // Application id
        label = new Label(group, SWT.WRAP);
        label.setText(PDEPlugin.getResourceString("BrandingContentPage.appDesc"));//$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 3;
        gd.widthHint = 500;
        label.setLayoutData(gd);
        label = new Label(group, SWT.NONE);
        label.setText(PDEPlugin.getResourceString("BrandingContentPage.app")); //$NON-NLS-1$
        label.setLayoutData(new GridData());
        fAppText = createText(group, productListener);
        fAppButton = new Button(group, SWT.PUSH);
        fAppButton.setText(PDEPlugin.getResourceString("BrandingContentPage.appBrowse")); //$NON-NLS-1$
        fAppButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String[] appNames = getApplicationNames();
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(
                        getShell(), PDEPlugin.getDefault().getLabelProvider());
                dialog.setElements(appNames);
                dialog.setAllowDuplicates(false);
                dialog.setMessage(PDEPlugin
                        .getResourceString("BrandingContentPage.selectDesc")); //$NON-NLS-1$
                dialog.setStatusLineAboveButtons(true);
                dialog.setTitle(PDEPlugin
                        .getResourceString("BrandingContentPage.selectTitle")); //$NON-NLS-1$
                if (dialog.open() == Dialog.CANCEL)
                    return;
                Object result = dialog.getFirstResult();
                fAppText.setText(result.toString());
            }
        });
        fAppButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTUtil.setButtonDimensionHint(fAppButton);
    }

    /*
     * images here will automatically be put into the icons/ directory in the
     * plug-in; the splash.bmp will be put into the root directory.
     *  
     */
    public void addImageGroup(Composite container) {
        Group group = new Group(container, SWT.NONE);
        group.setText(PDEPlugin.getResourceString("BrandingContentPage.imageGroup")); //$NON-NLS-1$
        GridLayout layout = new GridLayout(3, false);
        layout.verticalSpacing = 5;
        layout.marginHeight = layout.marginWidth = 6;
        group.setLayout(layout);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fUseDefaultButton = new Button(group, SWT.CHECK);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        fUseDefaultButton.setLayoutData(gd);
        fUseDefaultButton.setText(PDEPlugin
                .getResourceString("BrandingContentPage.default")); //$NON-NLS-1$
        fUseDefaultButton.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                boolean isUseDefault = fUseDefaultButton.getSelection();
                fWindowImagesButton.setEnabled(!isUseDefault);
                fWindowImagesText.setEnabled(!isUseDefault);
                fWindowsInfoLabel.setEnabled(!isUseDefault);
                fWindowsLabel.setEnabled(!isUseDefault);
                fAboutImageButton.setEnabled(!isUseDefault);
                fAboutImageText.setEnabled(!isUseDefault);
                fAboutInfoLabel.setEnabled(!isUseDefault);
                fAboutLabel.setEnabled(!isUseDefault);
                fSplashButton.setEnabled(!isUseDefault);
                fSplashText.setEnabled(!isUseDefault);
                fSplashInfoLabel.setEnabled(!isUseDefault);
                fSplashLabel.setEnabled(!isUseDefault);
                validatePage();
            }
        });
        // retrieve images that are associated with the shell
        // property = windowImages
        Composite imageGroup = new Composite(group, SWT.NONE);
        layout = new GridLayout(3, false);
        layout.marginHeight = 5;
        layout.marginWidth = 0;
        layout.verticalSpacing = 2;
        imageGroup.setLayout(layout);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        imageGroup.setLayoutData(gd);
        fWindowsInfoLabel = new Label(imageGroup, SWT.WRAP);
        fWindowsInfoLabel.setText(PDEPlugin
                .getResourceString("BrandingContentPage.winDesc"));//$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 3;
        gd.widthHint = 500;
        fWindowsInfoLabel.setLayoutData(gd);
        fWindowsLabel = new Label(imageGroup, SWT.NONE);
        fWindowsLabel.setText(PDEPlugin.getResourceString("BrandingContentPage.win")); //$NON-NLS-1$
        fWindowsLabel.setLayoutData(new GridData());
        fWindowImagesText = createText(imageGroup, imageListener);
        fWindowImagesButton = new Button(imageGroup, SWT.PUSH);
        fWindowImagesButton.setText(PDEPlugin
                .getResourceString("BrandingContentPage.winAdd")); //$NON-NLS-1$
        fWindowImagesButton.addSelectionListener(new SelectionAdapter() {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
             */
            public void widgetSelected(SelectionEvent e) {
                String[] newFiles = handleImageBrowse(WINDOW_IMAGES);
                if (newFiles == null)
                    return;
                String currentImages = fWindowImagesText.getText();
                StringBuffer buffer = new StringBuffer();
                if (currentImages.length() != 0)
                    buffer.append(currentImages);
                for (int i = 0; i < newFiles.length; i++) {
                    if (currentImages.length() != 0 || i > 0)
                        buffer.append(", "); //$NON-NLS-1$
                    buffer.append(newFiles[i]);
                }
                fWindowImagesText.setText(buffer.toString());
            }
        });
        fWindowImagesButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTUtil.setButtonDimensionHint(fWindowImagesButton);
        // retrieve image that will be in the main about dialog
        // property = aboutImage
        // plugin.properties will be created but populated minimally -
        // the aboutText will default to the product name and version
        // instead of being entertained here
        fAboutInfoLabel = new Label(imageGroup, SWT.WRAP);
        fAboutInfoLabel.setText(PDEPlugin
                .getResourceString("BrandingContentPage.aboutDesc")); // //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 3;
        gd.widthHint = 500;
        fAboutInfoLabel.setLayoutData(gd);
        fAboutLabel = new Label(imageGroup, SWT.NONE);
        fAboutLabel.setText(PDEPlugin.getResourceString("BrandingContentPage.about")); //$NON-NLS-1$
        fAboutLabel.setLayoutData(new GridData());
        fAboutImageText = createText(imageGroup, imageListener);
        fAboutImageButton = new Button(imageGroup, SWT.PUSH);
        fAboutImageButton.setText(PDEPlugin
                .getResourceString("BrandingContentPage.aboutBrowse")); //$NON-NLS-1$
        fAboutImageButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String[] newFiles = handleImageBrowse(ABOUT_IMAGE);
                if (newFiles == null || newFiles.length != 1)
                    return;
                fAboutImageText.setText(newFiles[0]);
            }
        });
        fAboutImageButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTUtil.setButtonDimensionHint(fAboutImageButton);
        // retrieve splash image (splash.bmp) which will be copied into
        // the plug-in's root dir
        // will eventually change config.ini's splashLocation
        fSplashInfoLabel = new Label(imageGroup, SWT.WRAP);
        fSplashInfoLabel.setText(PDEPlugin
                .getResourceString("BrandingContentPage.splashDesc"));// //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 3;
        gd.widthHint = 400;
        fSplashInfoLabel.setLayoutData(gd);
        fSplashLabel = new Label(imageGroup, SWT.NONE);
        fSplashLabel.setText(PDEPlugin.getResourceString("BrandingContentPage.splash")); //$NON-NLS-1$
        fSplashLabel.setLayoutData(new GridData());
        fSplashText = createText(imageGroup, imageListener);
        fSplashButton = new Button(imageGroup, SWT.PUSH);
        fSplashButton.setText(PDEPlugin
                .getResourceString("BrandingContentPage.splashBrowse")); //$NON-NLS-1$
        fSplashButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                String[] newFiles = handleImageBrowse(SPLASH_IMAGE);
                if (newFiles == null || newFiles.length != 1)
                    return;
                fSplashText.setText(newFiles[0]);
            }
        });
        fSplashButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        SWTUtil.setButtonDimensionHint(fSplashButton);
    }

    protected Text createText(Composite parent, ModifyListener listener) {
        Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gd.widthHint = 350;
        text.setLayoutData(gd);
        if (listener != null)
            text.addModifyListener(listener);
        return text;
    }

    protected String[] handleImageBrowse(byte imageType) {
        FileDialog dialog = null;
        if (imageType == WINDOW_IMAGES)
            dialog = new FileDialog(getShell(), SWT.MULTI);
        else
            dialog = new FileDialog(getShell());
        if (imageType == SPLASH_IMAGE)
            dialog.setFilterExtensions(new String[] { "*.bmp" }); //$NON-NLS-1$
        else
            dialog.setFilterExtensions(new String[] { "*.gif;*.jpg;*.jpeg;*.bmp;*.png" }); //$NON-NLS-1$
        String result = dialog.open();
        if (result == null)
            return null;
        IPath imagePath = new Path(result);
        imagePath = imagePath.removeLastSegments(1);
        String[] fileNames = dialog.getFileNames();
        for (int i = 0; i < fileNames.length; i++)
            fileNames[i] = imagePath.toFile().getPath() + File.separator + fileNames[i];
        return fileNames;
    }

    public String[] getApplicationNames() {
        TreeSet result = new TreeSet();
        IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPlugins();
        for (int i = 0; i < plugins.length; i++) {
            IPluginExtension[] extensions = plugins[i].getPluginBase().getExtensions();
            for (int j = 0; j < extensions.length; j++) {
                String point = extensions[j].getPoint();
                if (point != null
                        && point.equals("org.eclipse.core.runtime.applications")) { //$NON-NLS-1$
                    String id = extensions[j].getPluginBase().getId();
                    if (id == null || id.trim().length() == 0
                            || id.startsWith("org.eclipse.pde.junit.runtime")) //$NON-NLS-1$
                        continue;
                    if (extensions[j].getId() != null)
                        result.add(id + "." + extensions[j].getId()); //$NON-NLS-1$
                }
            }
        }
        return (String[]) result.toArray(new String[result.size()]);
    }

    public void updateData() {
        fData.setAboutImage(fAboutImageText.getText().trim());
        fData.setApplicationId(fAppText.getText().trim());
        fData.setProductName(fProductText.getText().trim());
        fData.setSplashImage(fSplashText.getText().trim());
        fData.setUseDefaultImages(fUseDefaultButton.getSelection());
        fData.setWindowImages(fWindowImagesText.getText().trim());
    }

    public BrandingData getBrandingData() {
        return fData;
    }

    public void validatePage() {
        boolean complete = fProductText.getText().length() != 0
                && fAppText.getText().length() != 0;
        setPageComplete(complete);
        if (!isInitialized) {
            setErrorMessage(null);
            return;
        }
        if (fProductText.getText().length() == 0)
            setErrorMessage(PDEPlugin
                    .getResourceString("BrandingContentPage.warningProduct")); //$NON-NLS-1$
        else if (fAppText.getText().length() == 0)
            setErrorMessage(PDEPlugin.getResourceString("BrandingContentPage.warningApp")); //$NON-NLS-1$
        else if (fWindowImagesText.getText().length() == 0
                && fAboutImageText.getText().length() == 0
                && fSplashText.getText().length() == 0
                && !fUseDefaultButton.getSelection()) {
            setErrorMessage(null);
            setMessage(
                    PDEPlugin.getResourceString("BrandingContentPage.warningImages"), DialogPage.WARNING); //$NON-NLS-1$
        } else {
            setErrorMessage(null);
            setMessage(null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.IDialogPage#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible)
            return;
        if (!isInitialized) {
            presetNameField(fProvider.getProjectName());
            fAppText.setText("org.eclipse.ui.ide.workbench"); //$NON-NLS-1$
            fUseDefaultButton.setSelection(true);
            fWindowImagesButton.setEnabled(false);
            fWindowImagesText.setEnabled(false);
            fWindowsInfoLabel.setEnabled(false);
            fWindowsLabel.setEnabled(false);
            fAboutImageButton.setEnabled(false);
            fAboutImageText.setEnabled(false);
            fAboutInfoLabel.setEnabled(false);
            fAboutLabel.setEnabled(false);
            fSplashButton.setEnabled(false);
            fSplashText.setEnabled(false);
            fSplashInfoLabel.setEnabled(false);
            fSplashLabel.setEnabled(false);
            validatePage();
            isInitialized = true;
            fProductChanged = false;
        } else if (!fProductChanged) {
            presetNameField(fProvider.getProjectName());
            fProductChanged = false;
        } else
            validatePage();
    }

private void presetNameField(String id) {
        int index = id.lastIndexOf("."); //$NON-NLS-1$
        if (index != -1 && index!= id.length()-1)
            id = id.substring(index+1, id.length());
        String productName=""; //$NON-NLS-1$
        StringTokenizer tok = new StringTokenizer(id, " "); //$NON-NLS-1$
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            if (productName.length()!= 0)
                productName = productName + " "; //$NON-NLS-1$
            productName = productName + Character.toUpperCase(token.charAt(0)) + ((token.length() > 1) ? token.substring(1) : ""); //$NON-NLS-1$
        }
        productName = productName + ((productName.length()>0) ? " " : "") + PDEPlugin.getResourceString("BrandingContentPage.product"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        fProductText.setText(productName);
    }}
