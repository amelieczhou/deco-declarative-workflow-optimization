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


package edu.isi.pegasus.planner.classes;
/**
 * The object of this class holds the name value pair.
 * At present to be used for environment variables. Will be used more
 * after integration of Spitfire.
 *
 * @author Karan Vahi
 * @author Gaurang Mehta
 * @version $Revision$
 */
public class NameValue extends Data
implements Comparable{

    /**
     * stores the name of the pair.
     */
    private String name;

    /**
     * stores the corresponding value to the name in the pair.
     */
    private String value;

    /**
     * the default constructor  which initialises the class member variables.
     */
    public NameValue(){
        name  = "";
        value = "";
    }

    /**
     * Initialises the class member variables to the values passed in the
     * arguments.
     *
     * @param name  corresponds to the name in the NameValue pair.
     * @param value corresponds to the value for the name in the NameValue pair.
     */
    public NameValue(String name,String value){
        this.name  = name;
        this.value = value;
    }

    /**
     * Sets the key associated with this tuple.
     *
     * @param key the key associated with the tuple.
     */
    public void setKey( String key ){
         this.name = key;
    }

    /**
     * Sets the value associated with this tuple.
     *
     * @param value the value associated with the tuple.
     */
    public void setValue( String value ){
         this.value = value;
    }

    /**
     * Returns the key associated with this tuple.
     *
     * @return the key associated with the tuple.
     */
    public String getKey(){
        return this.name;
    }

    /**
     * Returns the value associated with this tuple.
     *
     * @return value associated with the tuple.
     */
    public String getValue(){
        return this.value;
    }

   /**
     * Returns a copy of this object
     *
     * @return object containing a cloned copy of the tuple.
     */
    public Object clone(){
        NameValue nv = new NameValue(this.name,this.value) ;
        return nv;

    }

    /**
     * Writes out the contents of the class to a String
     * in form suitable for displaying.
     *
     * @return the textual description.
     */
    public String toString(){
        String str = this.getKey() + "=" + this.getValue();
        return str;
    }

    /**
     * Implementation of the {@link java.lang.Comparable} interface.
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object. The
     * NameValue are compared by their keys.
     *
     * @param o is the object to be compared
     * @return a negative number, zero, or a positive number, if the
     * object compared against is less than, equals or greater than
     * this object.
     * @exception ClassCastException if the specified object's type
     * prevents it from being compared to this Object.
     */
    public int compareTo( Object o ){
        if ( o instanceof NameValue ) {
            NameValue nv = (NameValue) o;
            return this.name.compareTo(nv.name);
        } else {
            throw new ClassCastException( "Object is not a NameValue" );
        }
    }


}
