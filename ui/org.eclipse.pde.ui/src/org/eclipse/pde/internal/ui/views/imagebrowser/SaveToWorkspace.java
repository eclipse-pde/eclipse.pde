/*******************************************************************************
 *  Copyright (c) 2012, 2016 Christian Pontesegger and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christian Pontesegger - initial API and implementation
 *     IBM Corporation - bug fixing
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/

package org.eclipse.pde.internal.ui.views.imagebrowser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.eclipse.core.commands.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for the image browser view's save to workspace command.
 *
 */
public class SaveToWorkspace extends AbstractHandler {

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {

		IViewPart view = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.findView(ImageBrowserView.VIEW_ID);
		ImageElement data = view.getAdapter(ImageElement.class);
		if (data != null) {
				SaveAsDialog dialog = new SaveAsDialog(HandlerUtil.getActiveShell(event));
				dialog.setTitle(PDEUIMessages.SaveToWorkspace_SaveImageToWorkspace);
				dialog.setOriginalName(data.getFileName());
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
					try {
						ByteArrayOutputStream out = new ByteArrayOutputStream();

						ImageLoader imageLoader = new ImageLoader();
						imageLoader.data = new ImageData[] {data.getImageData()};
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
		return null;
	}

	private int getImageType(final IPath path) {
		String extension = path.getFileExtension();
		if ("gif".equalsIgnoreCase(extension)) //$NON-NLS-1$
			return SWT.IMAGE_GIF;

		else if ("bmp".equalsIgnoreCase(extension)) //$NON-NLS-1$
			return SWT.IMAGE_BMP;

		else if ("png".equalsIgnoreCase(extension)) //$NON-NLS-1$
			return SWT.IMAGE_PNG;

		else if ("ico".equalsIgnoreCase(extension)) //$NON-NLS-1$
			return SWT.IMAGE_ICO;

		return SWT.IMAGE_PNG;
	}

}
