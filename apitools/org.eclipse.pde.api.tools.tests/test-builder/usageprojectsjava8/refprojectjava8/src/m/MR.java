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

public class MR {

	/**
	 * @noreference This method is not intended to be referenced by clients.
	 * @param supplier
	 */
   public <T> void con (Supplier<T> supplier) {}

  /**@noreference This method is not intended to be referenced by clients.
   * 
   * @param str1
   * @param str2
   * @return
   */
  public int mrCompare2(String str1, String str2) {
	   return 0;
	   }
  /**
   * @noreference
   * @param str1
   * @param str2
   * @return
   */
  
  public static int mrCompare(String str1, String str2) {
  return 0;
  }


}
