/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiBaselineWizardPage.EEEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE.SharedImages;

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
			return getApiComponentImage(comp);
		}
		if(element instanceof IResource) {
			IResource resource = (IResource) element;
			switch(resource.getType()) {
				case IResource.FILE: return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
				case IResource.FOLDER: return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
				case IResource.PROJECT: return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
			}
		}
		if (element instanceof File) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
		}
		if(element instanceof IApiBaseline) {
			return getBaselineImage();
		}
		if(element instanceof EEEntry) {
			return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY);
		}
		if(element instanceof IApiProblemFilter) {
			IApiProblemFilter filter = (IApiProblemFilter) element;
			IApiProblem problem = filter.getUnderlyingProblem();
			/*int flags = (problem.getSeverity() == ApiPlugin.SEVERITY_ERROR ? CompositeApiImageDescriptor.ERROR : CompositeApiImageDescriptor.WARNING);
			CompositeApiImageDescriptor desc = new CompositeApiImageDescriptor(image, flags);*/
			return getApiProblemElementImage(problem);/*ApiUIPlugin.getImage(desc);*/
		}
		return null;
	}

	/**
	 * Returns the image to use for the given {@link IApiProblem}
	 * 
	 * @param problem
	 * @return the image to use for the given {@link IApiProblem
	 */
	private Image getApiProblemElementImage(IApiProblem problem) {
		if(problem.getCategory() != IApiProblem.CATEGORY_USAGE) {
			switch(problem.getElementKind()) {
				case IDelta.ANNOTATION_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ANNOTATION);
				case IDelta.ENUM_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_ENUM);
				case IDelta.CLASS_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS);
				case IDelta.INTERFACE_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_INTERFACE);
				case IDelta.FIELD_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PUBLIC);
				case IDelta.METHOD_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PUBLIC);
				case IDelta.TYPE_PARAMETER_ELEMENT_TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CUNIT);
				case IDelta.API_BASELINE_ELEMENT_TYPE: return getBaselineImage();
				case IDelta.API_COMPONENT_ELEMENT_TYPE: {
					IPath path = new Path(problem.getResourcePath());
					//try to find the component via the resource handle
					IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
					if(res != null) {
						IApiComponent comp = ApiPlugin.getDefault().
														getApiBaselineManager().
														getWorkspaceBaseline().
														getApiComponent(res.getProject().getName());
						if(comp != null) {
							return getApiComponentImage(comp);
						}
					}
					return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_BUNDLE);
				}
			}
		}
		else {
			switch(problem.getElementKind()) {
				case IElementDescriptor.TYPE: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_CLASS);
				case IElementDescriptor.METHOD: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_OBJS_PUBLIC);
				case IElementDescriptor.FIELD: return JavaUI.getSharedImages().getImage(org.eclipse.jdt.ui.ISharedImages.IMG_FIELD_PUBLIC);
				default: {
					System.out.println();
				}
			}
		}
		return null;
	}
	
	/**
	 * @return the image to use for an {@link IApiBaseline}
	 */
	private Image getBaselineImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_ECLIPSE_PROFILE);
	}
	
	/**
	 * Returns the image to use for the given {@link IApiComponent}
	 * 
	 * @param component
	 * @return the image to use for the given {@link IApiComponent}
	 */
	private Image getApiComponentImage(IApiComponent component) {
		if(component.isSystemComponent()) {
			return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_API_SYSTEM_LIBRARY);
		}
		try {
			if (component.isFragment()) {
				return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_FRAGMENT);
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_OBJ_BUNDLE);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
	 */
	public String getText(Object element) {
		if (element instanceof IApiComponent) {
			IApiComponent comp = (IApiComponent) element;
			return MessageFormat.format(Messages.ApiToolsLabelProvider_0, new String[]{comp.getSymbolicName(), comp.getVersion()});
		}
		if (element instanceof File) {
			try {
				return ((File)element).getCanonicalPath();
			} catch (IOException e) {
				return ((File)element).getName();
			}
		}
		if(element instanceof IApiBaseline) {
			IApiBaseline baseline  = (IApiBaseline) element;
			StringBuffer buffer = new StringBuffer();
			buffer.append(baseline.getName());
			if(isDefaultBaseline(baseline)) {
				buffer.append(NLS.bind(Messages.ApiToolsLabelProvider_default_baseline_place_holder, Messages.ApiToolsLabelProvider_default_baseline));
			}
			return buffer.toString();
		}
		if(element instanceof EEEntry) {
			return ((EEEntry)element).toString();
		}
		if(element instanceof IApiProblemFilter) {
			IApiProblemFilter filter = (IApiProblemFilter) element;
			return filter.getUnderlyingProblem().getMessage();
		}
		if(element instanceof IResource) {
			IResource resource = (IResource) element;
			IPath path = resource.getProjectRelativePath();
			StringBuffer buffer = new StringBuffer();
			buffer.append(path.removeFileExtension().lastSegment());
			buffer.append(" ("); //$NON-NLS-1$
			buffer.append(path.removeLastSegments(1));
			buffer.append(")"); //$NON-NLS-1$
			return buffer.toString();
		}
		if(element instanceof String) {
			return (String) element;
		}
		return "<unknown>"; //$NON-NLS-1$
	}

	/**
	 * Returns if the specified {@link IApiProfile} is the default profile or not
	 * @param element
	 * @return if the profile is the default or not
	 */
	protected boolean isDefaultBaseline(Object element) {
		if(element instanceof IApiBaseline) {
			IApiBaseline profile = (IApiBaseline) element;
			IApiBaseline def = ApiPlugin.getDefault().getApiBaselineManager().getDefaultApiBaseline();
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
		if(isDefaultBaseline(element)) {
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
