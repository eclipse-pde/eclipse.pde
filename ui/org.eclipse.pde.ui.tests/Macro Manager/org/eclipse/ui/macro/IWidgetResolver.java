/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.macro;

import org.eclipse.swt.widgets.Widget;

/**
 * This interface is registered using extension point 
 * <code>org.eclipse.ui.macro</code> return unique identifier
 * from a provided widget. The identifier must be reproducable
 * between sessions so that it can be used to locate the
 * widget on playback.
 * 
 * @since 3.1
 */
public interface IWidgetResolver {
/**
 * Returns a unique identifier for the provided widget.
 * @param widget the widget to identify
 * @return unique identifier that can be used to locate the
 * widget or <code>null</code> if none can be found.
 */
	String getUniqueId(Widget widget);
}