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
package edu.isi.pegasus.planner.transfer.mapper.impl;

import edu.isi.pegasus.common.logging.LogManager;
import edu.isi.pegasus.planner.catalog.ReplicaCatalog;
import edu.isi.pegasus.planner.catalog.replica.ReplicaFactory;

import edu.isi.pegasus.planner.catalog.site.classes.FileServer;
import edu.isi.pegasus.planner.catalog.site.classes.SiteStore;
import edu.isi.pegasus.planner.classes.ADag;
import edu.isi.pegasus.planner.classes.PegasusBag;
import edu.isi.pegasus.planner.classes.PlannerOptions;

import edu.isi.pegasus.planner.transfer.mapper.MapperException;
import edu.isi.pegasus.planner.transfer.mapper.OutputMapper;

import java.util.LinkedList;
import java.util.List;

import java.util.Properties;


/**
 * This class connects to a Replica Catalog backend to determine where an output
 * file should be placed on the output site. At present the location on the output
 * site returned is the first matching entry in the Replica Catalog.
 * 
 * By default, if no replica catalog backend is specified, the RC defaults to 
 * Regex replica catalog backend.
 * 
 * To use this mapper, user needs to set the following properties
 * <pre>
 * pegasus.dir.storage.mapper               Replica
 * pegasus.dir.storage.mapper.replica       <replica-catalog backend to use> 
 * pegasus.dir.storage.mapper.replica.file  the RC file at the backend to use, \
 *                                          if using a file based RC
 * </pre>
 * 
 * 
 * @author Karan Vahi
 */
public class Replica implements OutputMapper {

    /**
     * The prefix for the property subset for connecting to the individual
     *  catalogs.
     */
    public static final String PROPERTY_PREFIX = "pegasus.dir.storage.mapper.replica";
    
    
    /**
     * Short description.
     */
    private static final String DESCRIPTION = "Replica Catalog Mapper";
    
    /**
     * The name of the key that disables writing back to the cache file.
     * Designates a static file. i.e. read only
     */
    public static final String READ_ONLY_KEY = "read.only";
    
    
    /**
     * The short name for this backend.
     */
    private static final String SHORT_NAME = "Replica";

    /**
     * The default replica catalog backend.
     */
    private String DEFAULT_REPLICA_BACKEND = "Regex";
    
    /**
     * The handle to the logger.
     */
    protected LogManager mLogger;
    
    
    /**
     * Handle to the Site Catalog contents.
     */
    protected SiteStore mSiteStore;
    
    /**
     * The output site where the data needs to be placed.
     */
    protected String mOutputSite;
    
    
    protected ReplicaCatalog mRCCatalog;
    
    /**
     * The default constructor.
     */
    public Replica(){
        
    }
    
    /**
     * Initializes the mappers.
     *
     * @param bag   the bag of objects that is useful for initialization.
     * @param workflow   the workflow refined so far.
     *
     */
    public void initialize( PegasusBag bag, ADag workflow)  throws MapperException{
        PlannerOptions options = bag.getPlannerOptions();
        String      outputSite = options.getOutputSite();
        mLogger       = bag.getLogger();
        mSiteStore    = bag.getHandleToSiteStore();
        mOutputSite   = outputSite;
        
        boolean stageOut = (( outputSite != null ) && ( outputSite.trim().length() > 0 ));

        if (!stageOut ){
            //no initialization and return
            mLogger.log( "No initialization of StageOut Site Directory Factory",
                         LogManager.DEBUG_MESSAGE_LEVEL );
            return;
        }
        
        Properties props = bag.getPegasusProperties().matchingSubset( PROPERTY_PREFIX, false );
        String catalogImplementor = bag.getPegasusProperties().getProperty( Replica.PROPERTY_PREFIX );
       
        //we only are reading not inserting any entries
        props.setProperty( Replica.READ_ONLY_KEY, "true" );
        
        catalogImplementor = ( catalogImplementor == null ) ?
                DEFAULT_REPLICA_BACKEND:
                catalogImplementor;
        try {
            mRCCatalog = ReplicaFactory.loadInstance( catalogImplementor, props );
        }
        catch( Exception e ){
            //log the connection error
            throw new MapperException( "Unable to connect to replica catalog backend for output mapper " + catalogImplementor , e);
        }

    }
    
    /**
     * Maps a LFN to a location on the filsystem of a site and returns a single
     * externally accessible URL corresponding to that location. It queries the
     * underlying Replica Catalog and returns the first matching PFN.
     * 
     * @param lfn          the lfn
     * @param site         the output site
     * @param operation    whether we want a GET or a PUT URL
     * 
     * @return the URL to file that was mapped
     * 
     * @throws MapperException if unable to construct URL for any reason
     */
    public String map( String lfn , String site , FileServer.OPERATION operation )  throws MapperException{
        //in this case we want to create an entry in factory namespace and use that addOn
        return this.map( lfn, site, operation, false );
        
    }
    
    /**
     * Maps a LFN to a location on the filsystem of a site and returns a single
     * externally accessible URL corresponding to that location. It queries the
     * underlying Replica Catalog and returns the first matching PFN.
     * 
     * @param lfn          the lfn
     * @param site         the output site
     * @param operation    whether we want a GET or a PUT URL
     * @param existing     indicates whether to create a new location/placement for a file, 
     *                     or rely on existing placement on the site.
     * 
     * @return  externally accessible URL to the mapped file.
     * 
     * @throws MapperException if unable to construct URL for any reason
     */
    public String map( String lfn, String site, FileServer.OPERATION operation, boolean existing ) throws MapperException{
        
        //we just return the first matching URL
        String url = mRCCatalog.lookup(lfn, site);
        
        if( url == null ){
            throw new MapperException( this.getErrorMessagePrefix() + "Unable to retrive location from Mapper Replica Backend for lfn " + lfn );
        }
        
        return url;
    }
    
    /**
     * Maps a LFN to a location on the filsystem of a site and returns all the possible
     * equivalent externally accessible URL corresponding to that location. In case
     * of the replica backed only one URL is returned and that is the first 
     * matching PFN for the output site.
     * 
     * @param lfn          the lfn
     * @param site         the output site
     * @param operation    whether we want a GET or a PUT URL
     * 
     * @return List<String> of externally accessible URLs to the mapped file.
     * 
     * @throws MapperException if unable to construct URL for any reason
     */
    public List<String> mapAll( String lfn, String site, FileServer.OPERATION operation) throws MapperException{
        String url = this.map( lfn, site, operation);
        List result = new LinkedList();
        result.add(url);
        return result;
    }
    
    

    
    
    /**
     * Returns the prefix message to be attached to an error message
     * 
     * @return 
     */
    protected String getErrorMessagePrefix(){
        StringBuilder error = new StringBuilder();
        error.append( "[" ).append( this.getShortName() ).append( "] ");
        return error.toString();
    }

    private String getShortName() {
        return Replica.SHORT_NAME;
    }
    
    /**
     * Returns a short description of the mapper.
     * 
     * @return 
     */
    public String description(){
        return this.DESCRIPTION;
    }
}
