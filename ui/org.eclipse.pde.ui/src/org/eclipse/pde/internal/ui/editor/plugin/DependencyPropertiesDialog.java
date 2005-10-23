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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Version;

public class DependencyPropertiesDialog extends StatusDialog {

    private Button fReexportButton;
    private Button fOptionalButton;
    private boolean fEditable;
    private boolean fShowReexport;
    
    private boolean fExported;
    private boolean fOptional;

    private Text fMinVersionText;
    private Text fMaxVersionText;
    private Combo fMinVersionBound;
    private Combo fMaxVersionBound;
    
    private VersionRange fVersionRange;
    private boolean ranged;
    private String fVersion;
    
    private boolean fShowOptional;
	private boolean fRangeAllowed;

    public DependencyPropertiesDialog(boolean editable, IPluginImport plugin) {
        this (editable, true, plugin.isReexported(), plugin.isOptional(), plugin.getVersion(), true, true);
    }
    
    public DependencyPropertiesDialog(boolean editable, ImportPackageObject object) {
        this (editable, false, false, object.isOptional(), object.getVersion(), true, true);
    }

    public DependencyPropertiesDialog(boolean editable, ExportPackageObject object) {
        this (editable, false, false, false, object.getVersion(), false, false);
    }

    public DependencyPropertiesDialog(boolean editable, boolean showReexport, boolean export, boolean optional, String version, boolean showOptional, boolean rangeAllowed) {
        super(PDEPlugin.getActiveWorkbenchShell());
        fEditable = editable;
        fShowReexport = showReexport;
        fExported = export;
        fOptional = optional;
        fShowOptional = showOptional;
        fRangeAllowed = rangeAllowed;
        loadVersion(version);
    }
    
    
    private void loadVersion(String version) {
    	try {
        	if (version != null && !version.equals("")) { //$NON-NLS-1$
        		fVersionRange = new VersionRange(version);
        	} 
        } catch (IllegalArgumentException e) {
        	// illegal version string passed
        	fVersionRange = new VersionRange("[0.0.0,0.0.0]"); //$NON-NLS-1$
        }
	}

	protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
    }
    
    protected Control createDialogArea(Composite parent) {
        Composite comp = (Composite)super.createDialogArea(parent);
        
        if (fShowOptional || fShowReexport) {
		    Group container = new Group(comp, SWT.NONE);
		    container.setText(PDEUIMessages.DependencyPropertiesDialog_properties);
		    container.setLayout(new GridLayout());
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
        }
        if (fRangeAllowed)
    	    createRangedGroup(comp);
        else
        	createSingleGroup(comp);

        preloadFields();
        
        return comp;
    }
    
    private void createRangedGroup(Composite parent) {
	    Group rangedGroup = new Group(parent, SWT.NONE);
	    rangedGroup.setText(PDEUIMessages.DependencyPropertiesDialog_groupText);
	    rangedGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    rangedGroup.setLayout(new GridLayout(3, false));
        String[] comboItems = new String[] {PDEUIMessages.DependencyPropertiesDialog_comboInclusive, PDEUIMessages.DependencyPropertiesDialog_comboExclusive};        
	    
	    Label minlabel = new Label(rangedGroup, SWT.NONE);
	    minlabel.setText(PDEUIMessages.DependencyPropertiesDialog_minimumVersion);
	    fMinVersionText = new Text(rangedGroup, SWT.SINGLE|SWT.BORDER);
	    fMinVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fMinVersionText.setEnabled(fEditable);
	    fMinVersionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
            	updateStatus(validateFullVersionRangeText());
            }
        });
	    
	    fMinVersionBound = new Combo(rangedGroup, SWT.SINGLE|SWT.BORDER|SWT.READ_ONLY );
	    fMinVersionBound.setEnabled(fEditable);
	    fMinVersionBound.setItems(comboItems);
	    
	    Label maxlabel = new Label(rangedGroup, SWT.NONE);
	    maxlabel.setText(PDEUIMessages.DependencyPropertiesDialog_maximumVersion);
	    fMaxVersionText = new Text(rangedGroup, SWT.SINGLE|SWT.BORDER);
	    fMaxVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fMaxVersionText.setEnabled(fEditable);
	    fMaxVersionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
            	updateStatus(validateFullVersionRangeText());
            }
        });
	    
	    fMaxVersionBound = new Combo(rangedGroup, SWT.SINGLE|SWT.BORDER|SWT.READ_ONLY);
	    fMaxVersionBound.setEnabled(fEditable);
	    fMaxVersionBound.setItems(comboItems);
    }
    


	private void createSingleGroup(Composite parent) {
	    Group singleGroup = new Group(parent, SWT.NONE);
	    singleGroup.setText(PDEUIMessages.DependencyPropertiesDialog_groupText);
        singleGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING|GridData.FILL_HORIZONTAL));
        singleGroup.setLayout(new GridLayout(2, false));
        
        Label label = new Label(singleGroup, SWT.NONE);
        label.setText(PDEUIMessages.DependencyPropertiesDialog_version);
        
        fMinVersionText = new Text(singleGroup, SWT.SINGLE|SWT.BORDER);
        fMinVersionText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fMinVersionText.setEnabled(fEditable);
        fMinVersionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                updateStatus(validateVersion(getMinVersion()));
            }
        });
	}
    
	private void preloadFields() {
		if (fRangeAllowed) {
		    fMinVersionText.setText((fVersionRange != null) ? fVersionRange.getMinimum().toString() : ""); //$NON-NLS-1$
		    fMaxVersionText.setText((fVersionRange != null && fVersionRange.getMaximum().getMajor() != Integer.MAX_VALUE) ? fVersionRange.getMaximum().toString() : ""); //$NON-NLS-1$

		    if (fVersionRange != null)
		    	fMinVersionBound.select((fVersionRange.getIncludeMinimum()) ? 0 : 1);
		    else
		    	fMinVersionBound.select(0);
		    
		    if (fVersionRange != null)
		    	fMaxVersionBound.select((fVersionRange.getIncludeMaximum()) ? 0 : 1);
		    else
		    	fMaxVersionBound.select(1);
		}
        fMinVersionText.setText((fVersionRange != null) ? fVersionRange.getMinimum().toString() : ""); //$NON-NLS-1$
	}
	
    private IStatus validateVersion(String text) {
    	if (text.length() == 0) return Status.OK_STATUS;
        if (PluginVersionIdentifier.validateVersion(text).getSeverity() != IStatus.OK)
            return new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, PDEUIMessages.DependencyPropertiesDialog_invalidFormat, null); //$NON-NLS-1$
        return Status.OK_STATUS;
    }
    
	protected IStatus validateVersionRange() {
		if (getMinVersion().length() == 0 || getMaxVersion().length() == 0) {
			ranged = false;
			return Status.OK_STATUS;
		}
		Version v1 = new Version(getMinVersion());
		Version v2 = new Version(getMaxVersion());
        if (v1.compareTo(v2) == 0) {
        	ranged = false;
        	return Status.OK_STATUS;
        } else if (v1.compareTo(v2) < 0) {
        	ranged = true;
            return Status.OK_STATUS;
        }
		return new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, PDEUIMessages.DependencyPropertiesDialog_versionRangeError, null); //$NON-NLS-1$;
	}
    

	private IStatus validateFullVersionRangeText() {
    	IStatus status = validateVersion(getMinVersion());
    	if (status.isOK()) status = validateVersion(getMaxVersion());
    	if (status.isOK()) status = validateVersionRange();
        return status;
	}
	
	private String getMinVersion() {
		return fMinVersionText.getText().trim();
	}
	private String getMaxVersion() {
		if (fMaxVersionText != null)
			return fMaxVersionText.getText().trim();
		return ""; //$NON-NLS-1$
	}
	private boolean getMinInclusive() {
		if (fMinVersionBound != null)
			return fMinVersionBound.getSelectionIndex() == 0;
		return false;
	}
	private boolean getMaxInclusive() {
		if (fMaxVersionBound != null)
			return fMaxVersionBound.getSelectionIndex() == 0;
		return true;
	}
	
	private String extractSingleVersionFromText() {
		if (!fRangeAllowed) return getMinVersion();
		if (getMinVersion().length() == 0)
			return getMaxVersion();
		return getMinVersion();
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
        fExported = (fReexportButton == null) ? false : fReexportButton.getSelection();
        
        if (ranged)
        	fVersion = new VersionRange(new Version(getMinVersion()), getMinInclusive(), new Version(getMaxVersion()), getMaxInclusive()).toString();
        else {
        	String version = extractSingleVersionFromText();
        	if (version == null || version.length() == 0)
        		fVersion = version;
        	else
        		fVersion = new Version(version).toString(); 
        }
        super.okPressed();
    }
}
