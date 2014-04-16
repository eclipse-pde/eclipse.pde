/*******************************************************************************
 * Copyright (c) April 15, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
