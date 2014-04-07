/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package i;

/**
 * @noreference This interface is not intended to be referenced by clients
 */
public interface NoRefMemberInterface {

	interface Inner {
		
		String fNoRefInterfaceField = "one"; //$NON-NLS-1$
		
		public void noRefInterfaceMethod();
	}
}
