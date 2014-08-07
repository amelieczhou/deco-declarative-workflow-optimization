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
package edu.isi.pegasus.planner.parser.tokens;

/**
 * Class to capture reserved words for the textual format of Transformation
 * Catalog
 *
 * @version $Revision$
 * @author Jens Vöckler
 * @author Karan Vahi
 */
public class TransformationCatalogReservedWord
        implements Token {

    /**
     * token value for the reserved word "tr".
     */
    public static final int TRANSFORMATION = 0;

    /**
     * token value for the reserved word "site".
     */
    public static final int SITE = 1;

    /**
     * token value for the reserved word "profile".
     */
    public static final int PROFILE = 2;

    /**
     * token value for the reserved word "pfn".
     */
    public static final int PFN = 3;
    /**
     * token value for the reserved word "arch".
     */
    public static final int ARCH = 4;
    /**
     * token value for the reserved word "os".
     */
    public static final int OS = 5;
    /**
     * token value for the reserved word "osrelease".
     */
    public static final int OSRELEASE = 6;

    /**
     * token value for the reserver word "osversion".
     */
    public static final int OSVERSION = 7;

    /**
     * token value for the reserver word "osversion".
     */
    public static final int TYPE = 8;

    /**
     * token value for the reserver word "instancetype".
     */
    public static final int INSTANCETYPE = 9;
    /**
     * Singleton implementation of a symbol table for reserved words.
     */
    private static java.util.Map mSymbolTable = null;

    /**
     * Singleton access to the symbol table as a whole.
     * @return Map
     */
    public static java.util.Map symbolTable() {
        if (mSymbolTable == null) {
            // only initialize once and only once, as needed.
            mSymbolTable = new java.util.TreeMap();
            mSymbolTable.put( "tr",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.TRANSFORMATION));
            mSymbolTable.put( "site",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.SITE ));
            mSymbolTable.put( "profile",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.PROFILE ));
            mSymbolTable.put( "pfn",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.PFN ));
            mSymbolTable.put( "arch",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.ARCH));
            mSymbolTable.put( "os",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.OS));
            mSymbolTable.put( "instancetype",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.INSTANCETYPE));//Amelie
            mSymbolTable.put( "osrelease",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.OSRELEASE ));
            mSymbolTable.put( "osversion",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.OSVERSION ));
            mSymbolTable.put( "type",
                    new TransformationCatalogReservedWord(TransformationCatalogReservedWord.TYPE));
            
        }

        return mSymbolTable;
    }
    /**
     * This instance variable captures the token value for the reserved word.
     */
    private int mValue;

    /**
     * Initializes an instance of a reserved word token. The constructor
     * is unreachable from the outside. Use symbol table lookups to obtain
     * reserved word tokens.
     *
     * @param tokenValue is the token value to memorize.
     * @see #symbolTable()
     */
    protected TransformationCatalogReservedWord(int tokenValue) {
        mValue = tokenValue;
    }

    /**
     * Obtains the token value of a given reserved word token.
     * @return the token value.
     */
    public int getValue() {
        return this.mValue;
    }
}
