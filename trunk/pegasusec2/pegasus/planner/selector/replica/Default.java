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

package edu.isi.pegasus.planner.selector.replica;

import edu.isi.pegasus.common.logging.LogManagerFactory;
import edu.isi.pegasus.planner.classes.ReplicaLocation;

import edu.isi.pegasus.planner.selector.ReplicaSelector;

import edu.isi.pegasus.common.logging.LogManager;
import edu.isi.pegasus.common.util.PegasusURL;
import edu.isi.pegasus.planner.common.PegasusProperties;
import edu.isi.pegasus.planner.common.PegRandom;

import edu.isi.pegasus.planner.catalog.replica.ReplicaCatalogEntry;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * The default replica selector that is used if non is specifed by the user.
 * This gives preference to a replica residing on the same site as the site,
 * where it is required to be staged to. If there is no such replica, then a
 * random replica is selected.
 *
 *
 * <p>
 * In order to use the replica selector implemented by this class,
 * <pre>
 *        - the property pegasus.selector.replica must be set to value Default, or
 *          the property should be left undefined in the properties.
 * </pre>
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class Default implements ReplicaSelector {

    /**
     * A short description of the replica selector.
     */
    private static String mDescription = "Default";


    
    /**
     * The handle to the logging object that is used to log the various debug
     * messages.
     */
    protected LogManager mLogger;

    /**
     * The properties object containing the properties passed to the planner.
     */
    protected PegasusProperties mProps;

    /**
     * The overloaded constructor, that is called by load method.
     *
     * @param properties the <code>PegasusProperties</code> object containing all
     *                   the properties required by Pegasus.
     *
     *
     */
    public Default( PegasusProperties properties ){
        mProps       = properties;
        mLogger      =  LogManagerFactory.loadSingletonInstance( properties );
    }


    /**
     * This chooses a location amongst all the locations returned by the replica
     * location service. If a location is found with re attribute same as the
     * preference pool, it is taken. Else a random location is selected and
     * returned. If more than one location for the lfn is found at the preference
     * pool, then also a random location amongst the ones at the preference pool
     * is selected.
     *
     * @param rl         the <code>ReplicaLocation</code> object containing all
     *                   the pfn's associated with that LFN.
     * @param preferredSite the preffered site for picking up the replicas.
     * @param allowLocalFileURLs indicates whether Replica Selector can select a replica
     *                      on the local site / submit host.
     *
     * @return <code>ReplicaCatalogEntry</code> corresponding to the location selected.
     *
     * @see org.griphyn.cPlanner.classes.ReplicaLocation
     */
    public ReplicaCatalogEntry selectReplica( ReplicaLocation rl,
                                              String preferredSite,
                                              boolean allowLocalFileURLs ){

        ReplicaCatalogEntry rce;
        ArrayList prefPFNs = new ArrayList();
        int locSelected;
        String site = null;

        //create a shallow clone as we will be removing
        //using Iterator.remove() methods
        rl = (ReplicaLocation)rl.clone();
        
        mLogger.log("Selecting a pfn for lfn " + rl.getLFN() + "\n amongst" + rl.getPFNList() ,
                    LogManager.DEBUG_MESSAGE_LEVEL);

        for ( Iterator it = rl.pfnIterator(); it.hasNext(); ) {
            rce = ( ReplicaCatalogEntry ) it.next();
            site = rce.getResourceHandle();

            //check if equal to the execution pool
            if ( site != null && site.equals( preferredSite ) ) {
                prefPFNs.add( rce );
                //return the one with file url for ligo stuff
                //is temporary till new api coded
                if ( rce.getPFN().startsWith( PegasusURL.FILE_URL_SCHEME ) ) {
                    //this is the one which is reqd for ligo
                    //return instead of break;
                    return rce;
                }
            }
            //check if we need to remove a file url or not
            else if ( removeFileURL(rce, preferredSite, allowLocalFileURLs) ){
                it.remove();
                   
            }
                /*
                mLogger.log(
                    "pool attribute not specified for the location objects" +
                    " in the Replica Catalog",LogManager.WARNING_MESSAGE_LEVEL);
                 */
            
        }

        int noOfLocs = rl.getPFNCount();
        if ( noOfLocs == 0 ) {
            //in all likelihood all the urls were file urls and none
            //were associated with the preference pool.
            throw new RuntimeException( "Unable to select a Physical Filename (PFN) for file with logical filename (LFN) as " +
                                         rl.getLFN() );
        }

        if ( prefPFNs.isEmpty() ) {
            //select a random location from
            //all the matching locations
            locSelected = PegRandom.getInteger( noOfLocs - 1 );
            rce = ( ReplicaCatalogEntry ) rl.getPFN( locSelected );

        } else {
            //select a random location
            //amongst the locations
            //on the preference pool
            int length = prefPFNs.size();
            //System.out.println("No of locations found at pool " + prefPool + " are " + length);
            locSelected = PegRandom.getInteger( length - 1 );
            rce = ( ReplicaCatalogEntry ) prefPFNs.get( locSelected );

            //user has specified that
            //he wants to create symbolic
            //links instead of going thru the
            //grid ftp server
            //create symbolic links instead of going through gridftp server
            //moved to Transfer Engine Karan June 8th, 2009
            /*
            if (mUseSymLinks) {
                rce = replaceProtocolFromURL( rce );
            }
            */
        }

        return rce;

    }




    /**
     * This chooses a location amongst all the locations returned by the
     * Replica Mechanism. If a location is found with re/pool attribute same
     * as the preference pool, it is taken. This returns all the locations which
     * match to the preference pool. This function is called to determine if a
     * file does exist on the output pool or not beforehand. We need all the
     * location to ensure that we are able to make a match if it so exists.
     * Else a random location is selected and returned
     *
     * @param rl         the <code>ReplicaLocation</code> object containing all
     *                   the pfn's associated with that LFN.
     * @param preferredSite the preffered site for picking up the replicas.
     * @param allowLocalFileURLs indicates whether Replica Selector can select a replica
     *                      on the local site / submit host.
     *
     * @return <code>ReplicaLocation</code> corresponding to the replicas selected.
     *
     * @see org.griphyn.cPlanner.classes.ReplicaLocation
     */
    public ReplicaLocation selectReplicas( ReplicaLocation rl,
                                           String preferredSite,
                                           boolean allowLocalFileURLs ){

        String lfn = rl.getLFN();
        ReplicaLocation result = new ReplicaLocation();
        result.setLFN( rl.getLFN() );

        ReplicaCatalogEntry rce;
        String site;
        String ucAttrib;
        int noOfLocs = 0;

        for ( Iterator it = rl.pfnIterator(); it.hasNext(); ) {
            noOfLocs++;
            rce = ( ReplicaCatalogEntry ) it.next();
            site = rce.getResourceHandle();

            if ( site != null && site.equals( preferredSite )) {
                result.addPFN( rce );
            }
            else if ( site == null ){
                mLogger.log(
                    " pool attribute not specified for the location objects" +
                    " in the Replica Catalog", LogManager.WARNING_MESSAGE_LEVEL);
            }
        }

        if ( result.getPFNCount() == 0 ) {
            //means we have to choose a random location between 0 and (noOfLocs -1)
            int locSelected = PegRandom.getInteger( noOfLocs - 1 );
            rce = ( ReplicaCatalogEntry ) rl.getPFN(locSelected );
            result.addPFN( rce );
        }
        return result;

    }


    /**
     * A convenience function that determines whether we should be removing a
     * file URL from replica selection or not. The file urls make sense only
     *
     * <pre>
     *      - if associated with the preference site or
     *      - if local File URL are allowed and rce is associated
     *        with local site
     * </pre>
     * 
     * @param rce                   the ReplicaCatalogEntry object.
     * @param preferredSite         the preferred site.
     * @param allowLocalFileURLs    indicates whether Replica Selector can select a replica
     *                      on the local site / submit host.
     * 
     * @return boolean
     */
    public boolean removeFileURL( ReplicaCatalogEntry rce, 
                                  String preferredSite,
                                  boolean allowLocalFileURLs ){
        return this.removeFileURL( rce.getPFN(), rce.getResourceHandle(), preferredSite, allowLocalFileURLs );
    }

    /**
     * A convenience function that determines whether we should be removing a
     * file URL from replica selection or not. The file urls make sense only
     *
     * <pre>
     *      - if associated with the preference site or
     *      - if local File URL are allowed and rce is associated
     *        with local site
     * </pre>
     *
     * @param pfn                   the file url
     * @param site                  the site associated with the pfn.
     * @param preferredSite         the preferred site.
     * @param allowLocalFileURLs    indicates whether Replica Selector can select a replica
     *                      on the local site / submit host.
     *
     * @return boolean
     */
    protected boolean removeFileURL( String pfn,
                                     String site,
                                     String preferredSite,
                                     boolean allowLocalFileURLs ){

        boolean result = false;

        if ( !pfn.startsWith( PegasusURL.FILE_URL_SCHEME )  ){
            //not a file url . dont remove
            return result;
        }

        if( site == null ){
            //remove the url and continue
            //nothing can be done
            result = true;
        }
        else if( !site.equalsIgnoreCase( preferredSite ) ){
            //the URL is not from a preferred site.
            //we can still use it if local file urls are allowed
            //and url is from a local site.
            result =  !( allowLocalFileURLs && site.equals( LOCAL_SITE_HANDLE ) );
        }

        return result;
    }
    

    /**
     * Returns a short description of the replica selector.
     *
     * @return string corresponding to the description.
     */
    public String description(){
        return mDescription;
    }


}
