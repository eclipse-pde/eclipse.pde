/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiProfileWizardPage.EEEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.ibm.icu.text.MessageFormat;

/**
 * Label provider for API tools objects.
 * 
 * @since 1.0.0
 */
public class ApiToolsLabelProvider extends BaseLabelProvider implements ILabelProvider, IFontProvider {

	/**
	 * Font for the default {@link IApiProfile} 
	 */
	private Font font = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
	 */
	public void dispose() {
		if(font != null) {
			font.dispose();
		}
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
	 */
	public Image getImage(Object element) {
		if (element instanceof IApiComponent) {
			IApiComponent comp = (IApiComponent) element;
			if(comp.isSystemComponent()) {
				return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY);
			}
			if (comp.isFragment()) {
				return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_FRAGMENT);
			}
			return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_BUNDLE);
		}
		if (element instanceof File) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
		if(element instanceof IApiProfile) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_FORWARD);
		}
		if(element instanceof EEEntry) {
			return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof IApiComponent) {
			IApiComponent comp = (IApiComponent) element;
			return MessageFormat.format(Messages.ApiToolsLabelProvider_0, new String[]{comp.getId(), comp.getVersion()});
		}
		if (element instanceof File) {
			try {
				return ((File)element).getCanonicalPath();
			} catch (IOException e) {
				return ((File)element).getName();
			}
		}
		if(element instanceof IApiProfile) {
			IApiProfile profile  = (IApiProfile) element;
			StringBuffer label = new StringBuffer();
			label.append(profile.getName());
			if(isDefaultProfile(profile)) {
				label.append("  [default profile]"); //$NON-NLS-1$
			}
			return label.toString();
		}
		if(element instanceof EEEntry) {
			return ((EEEntry)element).toString();
		}
		return "<unknown>"; //$NON-NLS-1$
	}

	/**
	 * Returns if the specified {@link IApiProfile} is the default profile or not
	 * @param element
	 * @return if the profile is the default or not
	 */
	protected boolean isDefaultProfile(Object element) {
		if(element instanceof IApiProfile) {
			IApiProfile profile = (IApiProfile) element;
			IApiProfile def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
			if(def != null) {
				return profile.getName().equals(def.getName());
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	public Font getFont(Object element) {
		if(isDefaultProfile(element)) {
			if (font == null) {
	            Font dialogFont = JFaceResources.getDialogFont();
				FontData[] fontData = dialogFont.getFontData();
	            for (int i = 0; i < fontData.length; i++) {
					FontData data = fontData[i];
					data.setStyle(SWT.BOLD);
				}
                Display display = ApiUIPlugin.getShell().getDisplay();
	            font = new Font(display, fontData);
			}
			return font;
		}
		return null;
	}

}
