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

/**
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @author vikchand
 *
 */
public class ConstructorReference2 {
	  private String str;
		 
	    private List<String> strs;
	 
	   
	    public ConstructorReference2() {
	        this.str = "test1";
	    }
	
	    public ConstructorReference2(String str) {
	        this.str = str;
	    }
	 
	    public ConstructorReference2(List<String> strs) {
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
