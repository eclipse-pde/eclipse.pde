/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.model.bundle.ExportPackageObject;
import org.eclipse.pde.internal.ui.model.bundle.ImportPackageObject;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

public class DependencyPropertiesDialog extends StatusDialog {

    private Button fReexportButton;
    private Button fOptionalButton;
    private boolean fEditable;
    private boolean fShowReexport;
    
    private boolean fExported;
    private boolean fOptional;
    private String fVersion;
    private Text fVersionText;
    private boolean fShowOptional;

    public DependencyPropertiesDialog(boolean editable, IPluginImport plugin) {
        this (editable, true, plugin.isReexported(), plugin.isOptional(), plugin.getVersion(), true);
    }
    
    public DependencyPropertiesDialog(boolean editable, ImportPackageObject object) {
        this (editable, false, false, object.isOptional(), object.getVersion(), true);
    }

    public DependencyPropertiesDialog(boolean editable, ExportPackageObject object) {
        this (editable, false, false, false, object.getVersion(), false);
    }

    public DependencyPropertiesDialog(boolean editable, boolean showReexport, boolean export, boolean optional, String version, boolean showOptional) {
        super(PDEPlugin.getActiveWorkbenchShell());
        fEditable = editable;
        fShowReexport = showReexport;
        fExported = export;
        fOptional = optional;
        fVersion = version;
        fShowOptional = showOptional;
    }
    
    
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        dialogChanged();
    }
    
    protected Control createDialogArea(Composite parent) {
        Composite comp = (Composite)super.createDialogArea(parent);
        
        Group container = new Group(comp, SWT.NONE);
        container.setText(PDEUIMessages.DependencyPropertiesDialog_properties);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        container.setLayout(layout);
        container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        if (fShowOptional) {
            fOptionalButton = new Button(container, SWT.CHECK);
            fOptionalButton.setText(PDEUIMessages.DependencyPropertiesDialog_optional);
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            fOptionalButton.setLayoutData(gd); 
            fOptionalButton.setEnabled(fEditable);
            fOptionalButton.setSelection(fOptional);
        }
        
        if (fShowReexport) {
            fReexportButton = new Button(container, SWT.CHECK);
            fReexportButton.setText(PDEUIMessages.DependencyPropertiesDialog_reexport);
            GridData gd = new GridData();
            gd.horizontalSpan = 2;
            fReexportButton.setLayoutData(gd);
            fReexportButton.setEnabled(fEditable);
            fReexportButton.setSelection(fExported);
        }
        
        Label label = new Label(container, SWT.NONE);
        label.setText(PDEUIMessages.DependencyPropertiesDialog_version);
        
        fVersionText = new Text(container, SWT.SINGLE|SWT.BORDER);
        fVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fVersionText.setEnabled(fEditable);
        fVersionText.setText(fVersion == null ? "" : fVersion); //$NON-NLS-1$
        fVersionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                dialogChanged();
            }
        });
        return container;
    }
    

    private void dialogChanged() {
        String version = fVersionText.getText().trim();
        if (version.length() > 0) {
            char first = version.charAt(0);
            if (first == '(' || first == '[') {
                updateStatus(validateRange(version));
            } else {
                updateStatus(validateVersion(version));
            }
            return;
        }
        updateStatus(Status.OK_STATUS);    
    }
    
    private IStatus validateRange(String range) {
        if (range.length() > 2) {
            char last = range.charAt(range.length() - 1);
            if (last == ')' || last == ']') {
                StringTokenizer tokenizer = new StringTokenizer(range.substring(1, range.length() - 1), ","); //$NON-NLS-1$
                int count = tokenizer.countTokens();
                if (count == 1 && range.indexOf(',') == -1) {
                    return validateVersion(tokenizer.nextToken());                        
                } else if (count == 2 && range.indexOf(',') == range.lastIndexOf(',')) {
                    String token1 = tokenizer.nextToken();
                    String token2 = tokenizer.nextToken();
                    if (PluginVersionIdentifier.validateVersion(token1).getSeverity() == IStatus.OK
                            && PluginVersionIdentifier.validateVersion(token2).getSeverity() == IStatus.OK) {
                        PluginVersionIdentifier version1 = new PluginVersionIdentifier(token1);
                        PluginVersionIdentifier version2 = new PluginVersionIdentifier(token2);
                        if (version2.isGreaterOrEqualTo(version1)) {
                            return Status.OK_STATUS;
                        }
                    }
                }              
            }
        }
        return new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, PDEUIMessages.DependencyPropertiesDialog_invalidRange, null); //$NON-NLS-1$
    }
    
    private IStatus validateVersion(String text) {
        if (PluginVersionIdentifier.validateVersion(text).getSeverity() != IStatus.OK)
            return new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, PDEUIMessages.DependencyPropertiesDialog_invalidFormat, null); //$NON-NLS-1$
        return Status.OK_STATUS;
    }
    
    public boolean isReexported() {
        return fExported;
    }
    
    public boolean isOptional() {
        return fOptional;
    }
    
    public String getVersion() {
        return fVersion;
    }
    protected void okPressed() {
        fOptional = (fOptionalButton == null) ? false : fOptionalButton.getSelection();
        fVersion = fVersionText.getText().trim();
        fExported = (fReexportButton == null) ? false : fReexportButton.getSelection();
        super.okPressed();
    }
}
