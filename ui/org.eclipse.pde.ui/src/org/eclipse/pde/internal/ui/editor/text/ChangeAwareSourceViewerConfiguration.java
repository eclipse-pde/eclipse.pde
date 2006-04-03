/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

public abstract class ChangeAwareSourceViewerConfiguration extends TextSourceViewerConfiguration {
	
	public ChangeAwareSourceViewerConfiguration(IPreferenceStore store) {
		super(store);
	}
	
 	public abstract boolean affectsTextPresentation(PropertyChangeEvent event);
 	
 	public abstract boolean affectsColorPresentation(PropertyChangeEvent event);
 	
	public abstract void adaptToPreferenceChange(PropertyChangeEvent event);

}
