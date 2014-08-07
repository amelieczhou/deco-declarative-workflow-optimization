/**
 *  Copyright 2007-2008 University Of Southern California
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package edu.isi.pegasus.planner.invocation;

/**
 * The proc element.
 * 
 * @author Karan Vahi
 * @version $Revision$
 */
public class Task extends MachineInfo {

   
    
    /**
     * The element name
     */
    public static final String ELEMENT_NAME = "task";
    
    /**
     * The default constructor
     */
    public Task(){
        super();
    }
    
    /**
     * Returns the name of the xml element corresponding to the object.
     * 
     * @return name
     */
    public String getElementName() {
        return ELEMENT_NAME;
    }


   
}
