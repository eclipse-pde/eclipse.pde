/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

public interface IEnvironmentVariables {
	String OS = "org.eclipse.pde.ui.os"; //$NON-NLS-1$
	String WS = "org.eclipse.pde.ui.ws"; //$NON-NLS-1$
	String NL = "org.eclipse.pde.ui.nl"; //$NON-NLS-1$
	String ARCH = "org.eclipse.pde.ui.arch"; //$NON-NLS-1$
	
	String OS_EXTRA = "org.eclipse.pde.os.extra"; //$NON-NLS-1$
	String WS_EXTRA = "org.eclipse.pde.ws.extra"; //$NON-NLS-1$
	String NL_EXTRA = "org.eclipse.pde.nl.extra"; //$NON-NLS-1$
	String ARCH_EXTRA = "org.eclipse.pde.arch.extra"; //$NON-NLS-1$
}
