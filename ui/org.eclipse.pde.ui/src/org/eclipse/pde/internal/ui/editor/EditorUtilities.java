/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.product.LauncherSection;
import org.eclipse.pde.internal.ui.editor.validation.IValidatorMessageHandler;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class EditorUtilities {

	private static final int F_EXACT_IMAGE_SIZE = 0;
	private static final int F_MAX_IMAGE_SIZE = 1;
	private static final int F_ICO_IMAGE = 2;
	private static final int F_IMAGE_DEPTH = 3;

	static class ValidationMessage {
		String fMessage;
		int fSeverity = IMessageProvider.WARNING;

		ValidationMessage(String message) {
			fMessage = message;
		}
	}

	static class ValidationInfo {
		int maxWidth, maxHeight, warningWidth, warningHeight, requiredDepth;
	}

	private static ImageData[] getImageData(IValidatorMessageHandler validator, FormEntry provider, IProduct product) {
		String imagePath = provider.getText().getText();
		String message = null;
		try {
			IPath path = getFullPath(new Path(imagePath), product);
			URL url = new URL(path.toString());
			ImageLoader loader = new ImageLoader();
			InputStream stream = url.openStream();
			ImageData[] idata = loader.load(stream);
			stream.close();
			if (idata != null && idata.length > 0)
				return idata;
			message = PDEUIMessages.EditorUtilities_noImageData;
		} catch (SWTException e) {
			message = PDEUIMessages.EditorUtilities_pathNotValidImage;
		} catch (MalformedURLException e) {
			message = PDEUIMessages.EditorUtilities_invalidFilePath;
		} catch (IOException e) {
			message = PDEUIMessages.EditorUtilities_invalidFilePath;
		}
		validator.addMessage(message, IMessageProvider.WARNING);
		return null;
	}

	private static boolean containsEmptyField(FormEntry provider) {
		return provider.getText().getText().length() == 0;
	}

	private static boolean imageEntryInternalValidate(IValidatorMessageHandler validator, FormEntry provider, IProduct product, ValidationInfo info, int validationType) {
		if (containsEmptyField(provider))
			return true;
		ImageData[] idata = getImageData(validator, provider, product);
		if (idata == null)
			return false;

		ValidationMessage ms = null;
		switch (validationType) {
			case F_MAX_IMAGE_SIZE :
				ms = getMS_maxImageSize(idata[0], info.maxWidth, info.maxHeight, info.warningWidth, info.warningHeight);
				break;
			case F_ICO_IMAGE :
				ms = getMS_icoImage(idata);
				break;
			case F_IMAGE_DEPTH : // do not break after F_IMAGEDEPTH since we are also checking exact size
				ms = getMS_imageDepth(idata[0], info.requiredDepth);
			case F_EXACT_IMAGE_SIZE :
				if (ms == null)
					ms = getMS_exactImageSize(idata[0], info.maxWidth, info.maxHeight);
				break;
		}

		if (ms != null) {
			validator.addMessage(ms.fMessage, ms.fSeverity);
		}

		return ms == null;
	}

	public static boolean imageEntryHasValidIco(IValidatorMessageHandler validator, FormEntry provider, IProduct product) {
		ValidationInfo info = new ValidationInfo();
		return imageEntryInternalValidate(validator, provider, product, info, F_ICO_IMAGE);
	}

	public static boolean imageEntrySizeDoesNotExceed(IValidatorMessageHandler validator, FormEntry provider, IProduct product, int mwidth, int mheight, int wwidth, int wheight) {
		ValidationInfo info = new ValidationInfo();
		info.maxWidth = mwidth;
		info.maxHeight = mheight;
		info.warningWidth = wwidth;
		info.warningHeight = wheight;
		return imageEntryInternalValidate(validator, provider, product, info, F_MAX_IMAGE_SIZE);
	}

	public static boolean imageEntryHasExactSize(IValidatorMessageHandler validator, FormEntry provider, IProduct product, int width, int height) {
		ValidationInfo info = new ValidationInfo();
		info.maxWidth = width;
		info.maxHeight = height;
		return imageEntryInternalValidate(validator, provider, product, info, F_EXACT_IMAGE_SIZE);
	}

	public static boolean imageEntryHasExactDepthAndSize(IValidatorMessageHandler validator, FormEntry provider, IProduct product, int width, int height, int depth) {
		ValidationInfo info = new ValidationInfo();
		info.maxWidth = width;
		info.maxHeight = height;
		info.requiredDepth = depth;
		return imageEntryInternalValidate(validator, provider, product, info, F_IMAGE_DEPTH);
	}

	private static ValidationMessage getMS_icoImage(ImageData[] imagedata) {
		int totalSizes = LauncherSection.F_WIN_ICON_DIMENSIONS.length;
		boolean[] found = new boolean[totalSizes];
		for (int i = 0; i < imagedata.length; i++) {
			int width = imagedata[i].width;
			int height = imagedata[i].height;
			int depth = imagedata[i].depth;
			for (int w = 0; w < totalSizes; w++)
				if (width == LauncherSection.F_WIN_ICON_DIMENSIONS[w][0] && height == LauncherSection.F_WIN_ICON_DIMENSIONS[w][1] && depth == LauncherSection.F_WIN_ICON_DEPTHS[w])
					found[w] = true;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < found.length; i++) {
			if (!found[i]) {
				if (sb.length() == 0)
					sb.append(PDEUIMessages.EditorUtilities_icoError);
				else
					sb.append(", "); //$NON-NLS-1$
				int width = LauncherSection.F_WIN_ICON_DIMENSIONS[i][0];
				int height = LauncherSection.F_WIN_ICON_DIMENSIONS[i][1];
				int depth = LauncherSection.F_WIN_ICON_DEPTHS[i];
				sb.append(NLS.bind(PDEUIMessages.EditorUtilities_missingIcoNote, getSizeString(width, height), Integer.toString(depth)));
			}
		}
		if (sb.length() > 0)
			return new ValidationMessage(sb.toString());
		return null;
	}

	private static ValidationMessage getMS_exactImageSize(ImageData imagedata, int mwidth, int mheight) {
		int width = imagedata.width;
		int height = imagedata.height;
		if (width != mwidth || height != mheight)
			return new ValidationMessage(NLS.bind(PDEUIMessages.EditorUtilities_incorrectSize, getSizeString(width, height)));
		return null;
	}

	private static ValidationMessage getMS_maxImageSize(ImageData imagedata, int mwidth, int mheight, int wwidth, int wheight) {
		int width = imagedata.width;
		int height = imagedata.height;
		if (width > mwidth || height > mheight)
			return new ValidationMessage(NLS.bind(PDEUIMessages.EditorUtilities_imageTooLarge, getSizeString(width, height)));
		else if (width > wwidth || height > wheight)
			return new ValidationMessage(NLS.bind(PDEUIMessages.EditorUtilities_imageTooLargeInfo, getSizeString(wwidth, wheight)));
		return null;
	}

	private static ValidationMessage getMS_imageDepth(ImageData imagedata, int depth) {
		if (imagedata.depth != depth)
			return new ValidationMessage(NLS.bind(PDEUIMessages.EditorUtilities_incorrectImageDepth, Integer.toString(imagedata.depth)));
		return null;
	}

	private static String getSizeString(int width, int height) {
		return width + " x " + height; //$NON-NLS-1$
	}

	private static IPath getFullPath(IPath path, IProduct product) throws MalformedURLException {
		String filePath = path.toString();
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		// look in root
		if (filePath.indexOf('/') == 0) {
			IResource resource = root.findMember(filePath);
			if (resource != null)
				return new Path("file:", resource.getLocation().toString()); //$NON-NLS-1$
			throw new MalformedURLException();
		}
		// look in project
		IProject project = product.getModel().getUnderlyingResource().getProject();
		IResource resource = project.findMember(filePath);
		if (resource != null)
			return new Path("file:", resource.getLocation().toString()); //$NON-NLS-1$

		// look in external models
		IPluginModelBase model = PluginRegistry.findModel(product.getDefiningPluginId());
		if (model != null && model.getInstallLocation() != null) {
			File modelNode = new File(model.getInstallLocation());
			String pluginPath = modelNode.getAbsolutePath();
			if (modelNode.isFile() && CoreUtility.jarContainsResource(modelNode, filePath, false))
				return new Path("jar:file:", pluginPath + "!/" + filePath); //$NON-NLS-1$ //$NON-NLS-2$
			return new Path("file:", pluginPath + "/" + filePath); //$NON-NLS-1$ //$NON-NLS-2$
		}
		// no file found - throw exception
		throw new MalformedURLException();
	}

	private static IPath getRootPath(IPath path, String definingPluginId) {
		IPluginModelBase model = PluginRegistry.findModel(definingPluginId);
		if (model != null && model.getInstallLocation() != null) {
			IPath newPath = new Path(model.getInstallLocation()).append(path);
			IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
			IContainer container = root.getContainerForLocation(newPath);
			if (container != null)
				return container.getFullPath();
		}
		return path;
	}

	private static IResource getImageResource(String value, String definingPluginId) {
		if (value == null)
			return null;
		IPath path = new Path(value);
		if (path.isEmpty())
			return null;

		if (!path.isAbsolute()) {
			path = getRootPath(path, definingPluginId);
		}
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		return root.findMember(path);
	}

	public static void openImage(String value, String definingPluginId) {
		IResource resource = getImageResource(value, definingPluginId);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile) resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.AboutSection_open, PDEUIMessages.AboutSection_warning); // 
		} catch (PartInitException e) {
		}
	}

}
