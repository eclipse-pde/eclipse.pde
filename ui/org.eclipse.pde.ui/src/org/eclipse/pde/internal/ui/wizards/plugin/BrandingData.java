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

import java.util.*;
import org.eclipse.core.runtime.*;

/**
 * @author cgwong
 */
public class BrandingData {
    
    private String productName;
    private String applicationId;
    private String[] windowImages;
    private String aboutImage;
    private String splashImage;
    private boolean useDefaultImages;
    public static final String[] defaultWindowImages = new String[]{
            "eclipse.gif", "eclipse32.gif"}; //$NON-NLS-1$ //$NON-NLS-2$
    public static final String defaultAboutImage = "eclipse_lg.gif"; //$NON-NLS-1$
    public static final String defaultSplashImage = "splash.bmp"; //$NON-NLS-1$

    public void setProductName(String name){
        productName = name;
    }
    
    public String getProductName(){
        return productName;
    }
    
    public void setApplicationId(String id){
        applicationId = id;
    }
    
    public String getApplicationId(){
        return applicationId;
    }
    
    public void setWindowImages(String imagePaths){
        StringTokenizer tokenizer = new StringTokenizer(imagePaths,","); //$NON-NLS-1$
        windowImages = new String[tokenizer.countTokens()];
        int count = 0;
        while (tokenizer.hasMoreTokens()){
            windowImages[count] = tokenizer.nextToken().trim();
            count++;
        }
    }
    
    public String[] getWindowImages(){
        if (useDefaultImages)
            return defaultWindowImages;
        return windowImages;
    }
    
    public String getExtWindowImages(){
        StringBuffer buffer = new StringBuffer();
        String[] path = getWindowImages();
        for (int i = 0; i<path.length;i++){
            Path imagePath = new Path(path[i]);
            if (i>0)
                buffer.append(","); //$NON-NLS-1$
            buffer.append("icons/" + imagePath.toFile().getName()); //$NON-NLS-1$
        }
        return buffer.toString();
    }
    
    public void setAboutImage(String imagePath){
        aboutImage = imagePath;
    }
    
    public String getAboutImage(){
        if (useDefaultImages)
            return defaultAboutImage;
        return aboutImage;
    }
    
    public String getExtAboutImage(){
        if (getAboutImage().length() == 0)
            return ""; //$NON-NLS-1$
        Path imagePath = new Path(getAboutImage());
        return "icons/" + imagePath.toFile().getName(); //$NON-NLS-1$
    }
    
    public void setSplashImage(String imagePath){
        splashImage = imagePath;
    }
    
    public String getSplashImage(){
        if (useDefaultImages)
            return defaultSplashImage;
        return splashImage;
    }
    
    public void setUseDefaultImages(boolean useDefault){
        useDefaultImages = useDefault;
    }
    
    public boolean useDefaultImages(){
        return useDefaultImages;
    }
}
