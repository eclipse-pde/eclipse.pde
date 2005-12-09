package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class EditorUtilities {
	
	public static final int[][] F_ICON_DIMENSIONS = new int[][] {
		{16, 16}, {32, 32}, {48, 48}, {64, 64}, {128, 128}
	};
	public static final int F_EXACTIMAGE = 0;
	public static final int F_MAXIMAGE = 1;
	public static final int F_ICOIMAGE = 2;
	
	static class MessageSeverity {
		String fMessage;
		int fSeverity = -1;
		MessageSeverity(String message) {
			fMessage = message;
		}
		MessageSeverity(String message, int severity) {
			this(message);
			fSeverity = severity;
		}
		String getMessage() { return fMessage; }
		int getSeverity() { return fSeverity; }
	}
	
	
	/*
	 * dimensions must be atleast of size 2
	 * dimensions[0] = width of image
	 * dimensions[1] = height of image
	 * dimensions[2] = width2
	 * dimensions[3] = height2
	 */
	public static boolean isValidImage(IEditorValidationProvider provider, IProduct product, int[] dimensions, int checkType) {
		String imagePath = provider.getProviderValue();
		String message = null;
		int severity = -1;
		if (imagePath.length() == 0) {// ignore empty strings
			return true;
		}
		IResource resource = getImageResource(provider.getProviderValue(), false, product);
		if (resource != null && resource instanceof IFile) {
			try {
				ImageLoader loader = new ImageLoader();
				ImageData[] idata = loader.load(((IFile)resource).getLocation().toString());
				if (idata.length == 0) {
					message = " no image data found in " + imagePath;
				} else {
					MessageSeverity ms = null;
					switch (checkType) {
					case F_EXACTIMAGE:
						ms = getMS_exactImageSize(idata[0], dimensions);
						break;
					case F_MAXIMAGE:
						ms = getMS_maxImageSize(idata[0], dimensions);
						break;
					case F_ICOIMAGE:
						ms = getMS_icoImage(idata);
					}
					if (ms != null) {
						message = ms.getMessage();
						severity = ms.getSeverity();
					}
				}
			} catch (SWTException e) {
				message = " is not a path to a valid image.";
			}	
		} else
			message = " is not a path to a valid file.";
		
		if (message != null) {
			String fieldName = null;
			if (provider instanceof FormEntry)
				fieldName = ((FormEntry)provider).getLabelText();
			if (fieldName != null)
				fieldName = " " + fieldName + " " + imagePath + ", ";
			else
				fieldName = "";
			provider.getValidator().setMessage(fieldName + message);
			if (severity >= 0)
				provider.getValidator().setSeverity(severity);
		}
		
		return message == null;
	}
	
	private static MessageSeverity getMS_icoImage(ImageData[] imagedata) {
		if (imagedata.length < 6)
			return new MessageSeverity("Ico files must contain at least 6 images.");
		return null;
	}

	private static MessageSeverity getMS_exactImageSize(ImageData imagedata, int[] sizes) {
		if (sizes.length < 2)
			return null;
		int width = imagedata.width;
		int height = imagedata.height;
		if (width != sizes[0] || height != sizes[1])
			return new MessageSeverity("Image has incorrect size: " + getSizeString(width, height) + ". Required size: " + getSizeString(sizes[0], sizes[1]));
		return null;
	}
	
	private static MessageSeverity getMS_maxImageSize(ImageData imagedata, int[] sizes) {
		if (sizes.length < 2)
			return null;
		int width = imagedata.width;
		int height = imagedata.height;
		if (width > sizes[0] || height > sizes[1]) {
			return new MessageSeverity("Image is too large: " + getSizeString(width, height) + ". Max size: " + getSizeString(sizes[0], sizes[1]));
		} else if (sizes.length > 2) {
			if (width > sizes[2] || height > sizes[3])
				return new MessageSeverity("Images larger than " + getSizeString(sizes[2], sizes[3])+ " will overlap/hide text, see section description.",
						IMessageProvider.INFORMATION);
		}
		return null;
	}
	
	private static String getSizeString(int width, int height) {
		return width + " x " + height;
	}
	
	private static IPath getFullPath(IPath path, IProduct product) {
		String productId = product.getId();
		int dot = productId.lastIndexOf('.');
		String pluginId = (dot != -1) ? productId.substring(0, dot) : ""; //$NON-NLS-1$
		IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(pluginId);
		if (model != null && model.getUnderlyingResource() != null) {
			IPath newPath = new Path(model.getInstallLocation()).append(path);
			IContainer container = PDEPlugin.getWorkspace().getRoot().getContainerForLocation(newPath);
			if (container != null) {
				return container.getFullPath();
			}
		}
		return path;
	}
	
	private static IResource getImageResource(String value, boolean showWarning, IProduct product) {
		if (value == null)
			return null;
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		IPath path = new Path(value);
		if (path.isEmpty()){
			if (showWarning)
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); // 
			return null;
		}
		if (!path.isAbsolute()) {
			path = EditorUtilities.getFullPath(path, product);
		}
		return root.findMember(path);
	}
	
	public static void openImage(String value, IProduct product) {
		IResource resource = EditorUtilities.getImageResource(value, true, product);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.AboutSection_open, PDEUIMessages.AboutSection_warning); // 
		} catch (PartInitException e) {
		}		
	}
}
