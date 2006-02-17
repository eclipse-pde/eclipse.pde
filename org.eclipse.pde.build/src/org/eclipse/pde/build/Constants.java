/**********************************************************************
 * Copyright (c) 2006 Eclipse Foundation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - Initial API and implementation
 *     IBM Corporation - Initial API and implementation
 **********************************************************************/
 package org.eclipse.pde.build;

/**
 * Constants for the files usually manipulated by the fetch factory.
 * @since 3.2
 */
public interface Constants {
	public final static String FEATURE_FILENAME_DESCRIPTOR = "feature.xml"; //$NON-NLS-1$
	public final static String FRAGMENT_FILENAME_DESCRIPTOR = "fragment.xml"; //$NON-NLS-1$
	public final static String PLUGIN_FILENAME_DESCRIPTOR = "plugin.xml"; //$NON-NLS-1$
	public final static String BUNDLE_FILENAME_DESCRIPTOR = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
}
