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

public class RCPData {
    
    private String fProductName;
    private String fApplicationId;
    private boolean fUseDefaultImages;
	private String fApplicationClass;
	private boolean fGenerateTemplates;
	private boolean fAddBranding;

    public void setProductName(String name){
        fProductName = name;
    }
    
    public String getProductName(){
        return fProductName;
    }
    
    public void setApplicationId(String id){
        fApplicationId = id;
    }
    
    public String getApplicationId(){
        return fApplicationId;
    }
    
    public String getApplicationClass() {
    	return fApplicationClass;
    }
    
    public void setApplicationClass(String className) {
    	fApplicationClass = className;
    }
        
    public void setUseDefaultImages(boolean useDefault){
        fUseDefaultImages = useDefault;
    }
    
    public boolean useDefaultImages(){
        return fUseDefaultImages;
    }
    
    public void setGenerateTemplateFiles(boolean doGenerate) {
    	fGenerateTemplates = doGenerate;
    }
    
    public boolean getGenerateTemplateFiles() {
    	return fGenerateTemplates;
    }
    
    public void setAddBranding(boolean addBranding) {
    	fAddBranding = addBranding;
    }
    
    public boolean getAddBranding() {
    	return fAddBranding;
    }
}
