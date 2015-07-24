/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.swt.graphics.Image;

public class TypeContentProposal implements IContentProposal {

	private String fLabel;

	private String fContent;

	private String fDescription;

	private Image fImage;

	public TypeContentProposal(String label, String content, String description, Image image) {
		fLabel = label;
		fContent = content;
		fDescription = description;
		fImage = image;
	}

	@Override
	public String getContent() {
		return fContent;
	}

	@Override
	public int getCursorPosition() {
		if (fContent != null) {
			return fContent.length();
		}
		return 0;
	}

	@Override
	public String getDescription() {
		return fDescription;
	}

	@Override
	public String getLabel() {
		return fLabel;
	}

	public Image getImage() {
		return fImage;
	}

	@Override
	public String toString() {
		return fLabel;
	}

}
