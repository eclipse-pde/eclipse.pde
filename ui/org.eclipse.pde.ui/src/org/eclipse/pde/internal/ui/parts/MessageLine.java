/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

/**
 * A message line displaying a status.
 */
public class MessageLine extends CLabel {

	private static final RGB ERROR_BACKGROUND_RGB = new RGB(230, 226, 221);

	private final Color fNormalMsgAreaBackground;
	private Color fErrorMsgAreaBackground;

	/**
	 * Creates a new message line as a child of the given parent.
	 */
	public MessageLine(Composite parent) {
		this(parent, SWT.LEFT);
	}

	/**
	 * Creates a new message line as a child of the parent and with the given SWT stylebits.
	 */
	public MessageLine(Composite parent, int style) {
		super(parent, style);
		fNormalMsgAreaBackground = getBackground();
		fErrorMsgAreaBackground = null;
	}

	private Image findImage(IStatus status) {
		PDELabelProvider provider = PDEPlugin.getDefault().getLabelProvider();
		if (status.isOK()) {
			return null;
		} else if (status.matches(IStatus.ERROR)) {
			return provider.get(PDEPluginImages.DESC_ERROR_ST_OBJ);
		} else if (status.matches(IStatus.WARNING)) {
			return provider.get(PDEPluginImages.DESC_WARNING_ST_OBJ);
		} else if (status.matches(IStatus.INFO)) {
			return provider.get(PDEPluginImages.DESC_INFO_ST_OBJ);
		}
		return null;
	}

	/**
	 * Sets the message and image to the given status.
	 * <code>null</code> is a valid argument and will set the empty text and no image
	 */
	public void setErrorStatus(IStatus status) {
		if (status != null) {
			String message = status.getMessage();
			if (message != null && message.length() > 0) {
				setText(message);
				setImage(findImage(status));
				if (fErrorMsgAreaBackground == null) {
					fErrorMsgAreaBackground = new Color(getDisplay(), ERROR_BACKGROUND_RGB);
				}
				setBackground(fErrorMsgAreaBackground);
				return;
			}
		}
		setText(""); //$NON-NLS-1$
		setImage(null);
		setBackground(fNormalMsgAreaBackground);
	}

	@Override
	public void dispose() {
		fErrorMsgAreaBackground = null;
		super.dispose();
	}
}
