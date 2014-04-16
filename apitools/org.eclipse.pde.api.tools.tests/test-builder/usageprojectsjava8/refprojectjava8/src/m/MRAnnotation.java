/*******************************************************************************
 * Copyright (c) April 5, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package m;

import java.util.function.Supplier;

import org.eclipse.pde.api.tools.annotations.NoReference;

public class MRAnnotation {

   @NoReference
   public <T> void con (Supplier<T> supplier) {}

   @NoReference
   public int mrCompare2(String str1, String str2) {
	   return 0;
	   }

   @NoReference
  public static int mrCompare(String str1, String str2) {
  return 0;
  }


}
