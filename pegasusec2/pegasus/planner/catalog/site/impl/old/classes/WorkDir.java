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

package edu.isi.pegasus.planner.catalog.site.impl.old.classes;

/**
 * This is a data class that is used to store information about the scratch
 * work directory or the execution mount point on the remote pool.
 * <p>
 * The various attributes that can be associated with the work directory
 * displayed in the following table.
 *
 * <p>
 * <table border="1">
 * <tr align="left"><th>Attribute Name</th><th>Attribute Description</th></tr>
 * <tr align="left"><th>path</th>
 *  <td>the absolute path on the remote site to the work directory.</td>
 * </tr>
 * <tr align="left"><th>total size</th>
 *  <td>the total scratch space available under the work directory.</td>
 * </tr>
 * <tr align="left"><th>free size</th>
 *  <td>the free space available under the work directory.</td>
 * </tr>
 * </table>

 *
 * @author Gaurang Mehta gmehta@isi.edu
 * @author Karan Vahi vahi@isi.edu
 *
 * @version $Revision$
 */
public class WorkDir {

    /**
     * Array storing the names of the attributes that are stored with the
     * work directory.
     */
    public static final String[] WORKDIRINFO = {"path", "total-size",
        "free-size"};

    /**
     * The constant to be passed to the accessor functions to get or set the
     * path to the work directory.
     */
    public static final int WORKDIR = 0;

    /**
     * The constant to be passed to the accessor functions to get or set the
     * total space available.
     */
    public static final int TOTAL_SIZE = 1;

    /**
     * The constant to be passed to the accessor functions to get or set the
     * free space available.
     */
    public static final int FREE_SIZE = 2;

    /**
     * The path to the work directory.
     */
    private String mWorkDir;

    /**
     * The total space available at the file system under this directory.
     */
    private String mTotalSize;

    /**
     * The free space available at the file system under this directory.
     */
    private String mFreeSize;

    /**
     * The default constructor. Sets all the variables to null.
     */
    public WorkDir() {
        mWorkDir   = null;
        mTotalSize = null;
        mFreeSize  = null;
    }


    /**
     * Returns the attribute value of a particular attribute of the work
     * directory.
     *
     * @param key the key/attribute name.
     *
     * @return the attribute value
     * @throws RuntimeException if illegal key defined.
     */
    public String getInfo( int key ) {
        switch ( key ) {
            case 0:
                return mWorkDir;

            case 1:
                return mTotalSize;

            case 2:
                return mFreeSize;

            default:
                throw new RuntimeException( "Illegal workdir key type=" +
                    key + ". Use on of the predefined types" );
        }
    }

    /**
     * Sets an attribute associated with the work directory.
     *
     * @param key  the attribute key, which is one of the predefined keys.
     * @param value value of the attribute.
     *
     * @throws Exception if illegal key defined.
     */
    public void setInfo( int key, String value ) throws RuntimeException {
        switch ( key ) {
            case 0:
                mWorkDir = value == null ? null : new String( value );
                break;

            case 1:
                mTotalSize = value == null ? null : new String( value );
                break;

            case 2:
                mFreeSize = value == null ? null : new String( value );
                break;

            default:
                throw new RuntimeException( "Illegal workdir key type=" +
                    key + ". Use on of the predefined types" );
        }
    }

    /**
     * Returns the textual description of the  contents of <code>WorkDir</code>
     * object in the multiline format.
     *
     * @return the textual description in multiline format.
     */
    public String toMultiLine() {
        String output = "workdir \"" + mWorkDir + "\"";
        return output;
    }

    /**
     * Returns the textual description of the  contents of <code>WorkDir</code>
     * object.
     *
     * @return the textual description.
     */
    public String toString() {
        String output = "workdir \"" + mWorkDir + "\"";
        if ( mWorkDir != null ) {
            output += " " + WORKDIRINFO[ 0 ] + "=" + mWorkDir;
        }
        if(mTotalSize!=null){
            output+=" " + WORKDIRINFO[1] + "=" + mTotalSize;
        }
        if(mFreeSize!=null){
            output+=" " + WORKDIRINFO[2] + "=" + mFreeSize;
        }
        output+=" )";
        // System.out.println(output);
        return output;
    }

    /**
     * Returns the XML description of the  contents of <code>WorkDir</code>
     * object.
     *
     * @return the xml description.
     */
    public String toXML() {
        String output = "";
        if ( mWorkDir != null ) {
            output += "<workdirectory";

            if ( mTotalSize != null ) {
                output += " " + WORKDIRINFO[ 1 ] + "=\"" + mTotalSize + "\"";
            }
            if ( mFreeSize != null ) {
                output += " " + WORKDIRINFO[ 2 ] + "=\"" + mFreeSize + "\"";
            }
            output += " >";
            output += mWorkDir;
        }
        output += "</workdirectory>";

        return output;
    }

}
