/*******************************************************************************
 *  Copyright (c) 2012 Christian Pontesegger and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser;

import org.eclipse.pde.internal.ui.PDEUIMessages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.services.ISourceProviderService;

/**
 * Handler for the image browser view's save to workspace command.
 *
 */
public class SaveToWorkspace extends AbstractHandler implements IHandler {

	public Object execute(final ExecutionEvent event) throws ExecutionException {
		ISourceProviderService service = (ISourceProviderService) PlatformUI.getWorkbench().getService(ISourceProviderService.class);
		ISourceProvider provider = service.getSourceProvider(ActiveImageSourceProvider.ACTIVE_IMAGE);
		if (provider != null) {
			@SuppressWarnings("rawtypes")
			Map currentState = provider.getCurrentState();

			Object data = currentState.get(ActiveImageSourceProvider.ACTIVE_IMAGE);
			if (data instanceof ImageElement) {
				SaveAsDialog dialog = new SaveAsDialog(HandlerUtil.getActiveShell(event));
				dialog.setTitle(PDEUIMessages.SaveToWorkspace_SaveImageToWorkspace);
				dialog.setOriginalName(((ImageElement) data).getFileName());
				// dialog.setMessage("select location & filename to store image to");
				if (dialog.open() == Window.OK) {
					IPath result = dialog.getResult();

					IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(result);
					boolean exists = file.exists();
					if (exists) {
						boolean confirm = MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), PDEUIMessages.SaveToWorkspace_ConfirmOverwrite, NLS.bind(PDEUIMessages.SaveToWorkspace_ConfirmOverwriteText, result));
						if (!confirm)
							return null;
					}

					int imageType = getImageType(result);
					if (imageType != SWT.NONE) {
						try {
							ByteArrayOutputStream out = new ByteArrayOutputStream();

							ImageLoader imageLoader = new ImageLoader();
							imageLoader.data = new ImageData[] {((ImageElement) data).getImageData()};
							imageLoader.save(out, imageType);

							ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
							if (exists)
								file.setContents(in, true, false, null);
							else
								file.create(in, true, null);

							file.getParent().refreshLocal(IResource.DEPTH_ZERO, null);

						} catch (CoreException e) {
							PDEPlugin.log(e);
						}
					}
				}
			}
		}
		return null;
	}

	private int getImageType(final IPath path) {
		String filename = path.lastSegment();
		String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
		if ("gif".equals(extension)) //$NON-NLS-1$
			return SWT.IMAGE_GIF;

		else if ("bmp".equals(extension)) //$NON-NLS-1$
			return SWT.IMAGE_BMP;

		else if ("png".equals(extension)) //$NON-NLS-1$
			return SWT.IMAGE_PNG;

		else if ("ico".equals(extension)) //$NON-NLS-1$
			return SWT.IMAGE_ICO;

		return SWT.NONE;
	}

}
