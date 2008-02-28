/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;

public abstract class PDETextHover implements ITextHoverExtension, ITextHover {

	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		return new Region(offset, 0);
	}

	public IInformationControlCreator getHoverControlCreator() {
		return getInformationControlCreator();
	}

	public static IInformationControlCreator getInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new PDEDefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
			}
		};
	}

	/**
	 * @param infoControl
	 * @param control
	 * @param provider
	 */
	public static void addHoverListenerToControl(final IInformationControl infoControl, final Control control, final IControlHoverContentProvider provider) {

		control.addMouseTrackListener(new MouseTrackListener() {
			public void mouseEnter(MouseEvent e) {
			}

			public void mouseExit(MouseEvent e) {
				if (infoControl instanceof PDEDefaultInformationControl && ((PDEDefaultInformationControl) infoControl).isDisposed())
					return;
				infoControl.setVisible(false);
			}

			public void mouseHover(MouseEvent e) {
				if (infoControl instanceof PDEDefaultInformationControl && ((PDEDefaultInformationControl) infoControl).isDisposed())
					return;
				String text = provider.getHoverContent(control);
				if (text == null || text.trim().length() == 0)
					return;
				updateHover(infoControl, text);
				infoControl.setLocation(control.toDisplay(new Point(10, 25)));
				infoControl.setVisible(true);
			}
		});
	}

	/**
	 * @param infoControl
	 * @param text
	 */
	public static void updateHover(IInformationControl infoControl, String text) {
		infoControl.setInformation(text);
		Point p = infoControl.computeSizeHint();
		infoControl.setSize(p.x, p.y);
		if (text == null || text.trim().length() == 0)
			infoControl.setVisible(false);
	}

}
