/*******************************************************************************
 * Copyright (c) 2016 Google Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stefan Xenos (Google) - initial API and implementation
 *
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.spy.dialogs;

import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class GeometryUtil {
	public static Rectangle getDisplayBounds(Control boundsControl) {
		Control parent = boundsControl.getParent();
		if (parent == null || boundsControl instanceof Shell) {
			return boundsControl.getBounds();
		}
	
		return Geometry.toDisplay(parent, boundsControl.getBounds());
	}

	public static Rectangle extrudeEdge(Rectangle innerBoundsWrtOverlay, int distanceToTop, int side) {
		if (distanceToTop <= 0) {
			return new Rectangle(0, 0, 0, 0);
		}
		return Geometry.getExtrudedEdge(innerBoundsWrtOverlay, distanceToTop, side);
	}

	public static int getBottom(Rectangle rect) {
		return rect.y + rect.height;
	}

	public static int getRight(Rectangle rect) {
		return rect.x + rect.width;
	}
}
