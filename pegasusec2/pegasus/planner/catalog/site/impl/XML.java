/*
 * 
 *   Copyright 2007-2008 University Of Southern California
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package edu.isi.pegasus.planner.catalog.site.impl;

import edu.isi.pegasus.common.logging.LogManagerFactory;
import edu.isi.pegasus.common.logging.LoggingKeys;
import edu.isi.pegasus.planner.catalog.SiteCatalog;
import edu.isi.pegasus.planner.catalog.site.SiteCatalogException;
import edu.isi.pegasus.planner.catalog.site.classes.SiteCatalogEntry;

import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;

import edu.isi.pegasus.common.logging.LogManager;

import edu.isi.pegasus.planner.classes.PegasusBag;
import edu.isi.pegasus.planner.common.PegasusProperties;
import edu.isi.pegasus.planner.parser.SiteCatalogXMLParser;
import edu.isi.pegasus.planner.parser.SiteCatalogXMLParserFactory;
import edu.isi.pegasus.planner.parser.StackBasedXMLParser;
import java.io.File;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;


/**
 * An implementation of the Site Catalog interface that is backed up by 
 * an XML file conforming to site catalog xml schema version 3.
 * 
 * The schema can be found online at 
 * 
 * <pre>
 *  http://pegasus.isi.edu/schema/sc-3.0.xsd
 * </pre>
 * 
 * @author Karan Vahi
 * @version $Revision$
 */
public class XML implements SiteCatalog {
    
    /**
     * The handle to parser instance that will parse the site catalog.
     */
    private SiteCatalogXMLParser mParser;
   
    /**
     * Stores sites in memory
     */
    //private Map<String, SiteCatalogEntry>mSiteMap;
    private SiteStore mSiteStore;
    
    /**
     * The Site Catalog file to be parser.
     */
    private String mFilename;
    
    /**
     * The handle to the log manager.
     */
    private LogManager mLogger;


    /**
     * The bag of Pegasus Initialization objects
     */
    private PegasusBag mBag;

    /**
     * The default constructor.
     */
    public XML(){
        mLogger = LogManagerFactory.loadSingletonInstance();
        mBag = new PegasusBag();
        mBag.add( PegasusBag.PEGASUS_LOGMANAGER, mLogger );
        mBag.add( PegasusBag.PEGASUS_PROPERTIES, PegasusProperties.nonSingletonInstance() );
        //mSiteMap = new HashMap<String,SiteCatalogEntry>();
    }
    
    /**
     * Establishes a connection to the file from the properties.
     * You will need to specify a "file" property to point to the
     * location of the on-disk instance. 
     * 
     * @param props is the property table with sufficient settings to
     *              to connect to the implementation.
     * 
     * @return true if connected, false if failed to connect.
     *
     * @throws SiteCatalogException
     */
    public boolean connect( Properties props ) throws SiteCatalogException{
        if ( props.containsKey("file") )
          return connect( props.getProperty("file") );
        return false;
    } 

    /**
     * Initializes the Site Catalog Parser instance for the file.
     *
     * @param filename is the name of the file to read.
     * 
     * @return true, 
     */
    public boolean connect( String filename ){
        mFilename = filename;
        File f = new File( filename );
        if ( f.exists() && f.canRead() ){    
            return true;
        }
        else{
            throw new RuntimeException( "Cannot read or access file " + filename );
        }
    }
    
    /**
     * Close the connection to backend file.
     */
    public void close() {
        mFilename = null;
    }

    /**
     * Returns if the connection is closed or not.
     * 
     * @return  boolean
     */
    public boolean isClosed() {
        return mFilename == null;
    }

    /**
     * Loads up the Site Catalog implementation  with the sites whose
     * site handles are specified. This is a convenience method, that can 
     * allow the backend implementations to maintain soft state if required.
     * 
     * If the implementation chooses not to implement this, just do an empty
     * implementation.
     * 
     * The site handle * is a special handle designating all sites are to be 
     * loaded.
     * 
     * @param sites   the list of sites to be loaded.
     * 
     * @return the number of sites loaded.
     * 
     * @throws SiteCatalogException in case of error.
     */
    public int load( List<String> sites ) throws SiteCatalogException {
        if( this.isClosed() ){
            throw new SiteCatalogException( "Need to connect to site catalog before loading" );
        }

        mParser = SiteCatalogXMLParserFactory.loadSiteCatalogXMLParser( this.mBag, mFilename, sites );


        mLogger.logEventStart( LoggingKeys.EVENT_PEGASUS_PARSE_SITE_CATALOG , "site-catalog.id", mFilename,
                                LogManager.DEBUG_MESSAGE_LEVEL );
        ((StackBasedXMLParser)mParser).startParser( mFilename );
        mLogger.logEventCompletion( LogManager.DEBUG_MESSAGE_LEVEL );

        mSiteStore = mParser.getSiteStore();
        return mSiteStore.list().size();
    }

    /**
     * Not implemented as yet.
     * 
     * @param entry
     * @return number of entries inserted.
     * @throws edu.isi.pegasus.planner.catalog.site.SiteCatalogException
     */
    public int insert(SiteCatalogEntry entry) throws SiteCatalogException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Lists  the site handles for all the sites in the Site Catalog.
     *
     * @return A set of site handles.
     * 
     * @throws SiteCatalogException in case of error.
     */
    public Set<String> list() throws SiteCatalogException {
        return ( mSiteStore == null )?
                new HashSet():
                mSiteStore.list();
               
    }

    
    /**
     * Retrieves the <code>SiteCatalogEntry</code> for a site.
     *
     * @param handle   the site handle / identifier.
     * 
     * @return SiteCatalogEntry in case an entry is found , or <code>null</code>
     *         if no match is found.
     * 
     * 
     * @throws SiteCatalogException in case of error.
     */
    public SiteCatalogEntry lookup(String handle) throws SiteCatalogException {
        return ( mSiteStore == null )?
                null:
                mSiteStore.lookup( handle );
    }

    /**
     * Not yet implemented as yet.
     * 
     * @param handle
     * @return number of entries removed.
     * @throws edu.isi.pegasus.planner.catalog.site.SiteCatalogException
     */
    public int remove( String handle ) throws SiteCatalogException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

   

}
