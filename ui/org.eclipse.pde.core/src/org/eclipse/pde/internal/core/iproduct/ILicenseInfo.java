/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.iproduct;

public interface ILicenseInfo extends IProductObject {

	String getLicense();

	void setLicense(String text);

	String getURL();

	void setURL(String url);

}
