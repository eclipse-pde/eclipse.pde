package org.eclipse.pde.internal.ui.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;

public class ValidationUtilities {
	
	
	/*
	 * dimensions[0] = width of image
	 * dimensions[1] = height of image
	 */
	public static boolean isValidImage(IEditorValidationProvider provider, IResource resource, int[] dimensions) {
		String imagePath = provider.getProviderValue();
		String message = null;
		if (imagePath.length() == 0) // ignore empty strings
			return true;
		if (resource != null && resource instanceof IFile) {
			Image image = null;
			try {
				image = new Image(PDEPlugin.getActiveWorkbenchShell().getDisplay(),
						((IFile)resource).getLocation().toString());
			} catch (SWTException e) {
				message = imagePath + " is not a path to a valid image.";
			}
			if (image != null) {
				Rectangle bounds = image.getBounds();
				if (bounds.width != dimensions[0]) {
					message = "Image has incorrect width: " + bounds.width;
				} else if (bounds.height != dimensions[1]) {
					message = "Image has incorrect height: " + bounds.height;
				}
				image.dispose();
			}
		} else
			message = imagePath + " is not a path to a valid file.";
		
		if (message != null) {
			String fieldName = null;
			if (provider instanceof FormEntry)
				fieldName = ((FormEntry)provider).getLabelValue();
			if (fieldName != null)
				fieldName = fieldName + " ";
			else
				fieldName = "";
			provider.getValidator().setMessage(fieldName + message);
		}
		
		return message == null;
	}
}
