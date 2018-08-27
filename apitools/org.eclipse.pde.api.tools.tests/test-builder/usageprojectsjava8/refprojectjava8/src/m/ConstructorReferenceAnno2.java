/*******************************************************************************
 * Copyright (c) April 15, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package m;
import java.util.List;
import org.eclipse.pde.api.tools.annotations.NoInstantiate;

@NoInstantiate
public class ConstructorReferenceAnno2 {
	  private String str;
		 
	    private List<String> strs;
	 
	   
	    public ConstructorReferenceAnno2() {
	        this.str = "test1";
	    }
	
	    public ConstructorReferenceAnno2(String str) {
	        this.str = str;
	    }
	 
	    public ConstructorReferenceAnno2(List<String> strs) {
	        this.strs = strs;
	    }
	 
	    public String getString()
	    {
	        return str;
	    }
	 
	    public List<String> getStrings()
	    {
	        return strs;
	    }


}
