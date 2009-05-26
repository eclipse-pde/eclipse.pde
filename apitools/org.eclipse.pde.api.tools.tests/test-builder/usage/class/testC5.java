/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package x.y.z;

import i.IExtInterface1;
import i.IExtInterface2;
import i.IExtInterface3;
import i.IExtInterface4;

/**
 * Indirectly implements 4 @noimplement interfaces with no 
 * parent class that implements them
 */
public class testC5 implements IExtInterface1, IExtInterface2, IExtInterface3, IExtInterface4 {

}
