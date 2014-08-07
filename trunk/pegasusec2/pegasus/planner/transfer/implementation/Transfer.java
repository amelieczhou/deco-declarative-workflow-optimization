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

package edu.isi.pegasus.planner.transfer.implementation;

import edu.isi.pegasus.planner.classes.TransferJob;
import edu.isi.pegasus.planner.classes.NameValue;
import edu.isi.pegasus.planner.classes.FileTransfer;
import edu.isi.pegasus.planner.classes.Profile;

import edu.isi.pegasus.common.logging.LogManager;

import edu.isi.pegasus.planner.namespace.Pegasus;

import edu.isi.pegasus.planner.catalog.transformation.classes.TCType;

import edu.isi.pegasus.planner.catalog.transformation.TransformationCatalogEntry;

import edu.isi.pegasus.common.util.Separator;



import edu.isi.pegasus.planner.classes.Job;
import java.io.FileWriter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;


import java.io.File;
import edu.isi.pegasus.planner.classes.PegasusBag;

/**
 * The implementation that creates transfer jobs referring to the python based
 * transfer script distributed with Pegasus since version 3.0
 *
 * <p>
 * Transfer is distributed as part of the Pegasus worker package and can be found at
 * $PEGASUS_HOME/bin/pegasus-transfer.
 *
 * <p>
 * It leads to the creation of the setup chmod jobs to the workflow, that appear
 * as parents to compute jobs in case the transfer implementation does not
 * preserve the X bit on the file being transferred. This is required for
 * staging of executables as part of the workflow. The setup jobs are only added
 * as children to the stage in jobs.
 * <p>
 * In order to use the transfer implementation implemented by this class, the
 * property <code>pegasus.transfer.*.impl</code> must be set to
 * value <code>Transfer</code>.
 *
 * The arguments with which the pegasus-transfer client is invoked can be specified
 * <pre>
 *       - by specifying the property pegasus.transfer.arguments
 *       - associating the Pegasus profile key transfer.arguments
 * </pre>
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class Transfer extends AbstractMultipleFTPerXFERJob {

    /**
     * The transformation namespace for the transfer job.
     */
    public static final String TRANSFORMATION_NAMESPACE = "pegasus";

    /**
     * The name of the underlying transformation that is queried for in the
     * Transformation Catalog.
     */
    public static final String TRANSFORMATION_NAME = "transfer";

    /**
     * The version number for the transfer job.
     */
    public static final String TRANSFORMATION_VERSION = null;

    /**
     * The derivation namespace for for the transfer job.
     */
    public static final String DERIVATION_NAMESPACE = "pegasus";

    /**
     * The name of the underlying derivation.
     */
    public static final String DERIVATION_NAME = "transfer";

    /**
     * The derivation version number for the transfer job.
     */
    public static final String DERIVATION_VERSION = "1.0";

    /**
     * A short description of the transfer implementation.
     */
    public static final String DESCRIPTION = "Python based Transfer Script";


    /**
     * The executable basename for the transfer executable.
     */
    public static final String EXECUTABLE_BASENAME = "pegasus-transfer";


    /**
     * The overloaded constructor, that is called by the Factory to load the
     * class.
     *
     * @param bag   the bag of initialization objects.
     */
    public Transfer( PegasusBag bag ){
        super( bag );
    }

    /**
     * Return a boolean indicating whether the transfers to be done always in
     * a third party transfer mode. A value of false, results in the
     * direct or peer to peer transfers being done.
     * <p>
     * A value of false does not preclude third party transfers. They still can
     * be done, by setting the property "pegasus.transfer.*.thirdparty.sites".
     *
     * @return boolean indicating whether to always use third party transfers
     *         or not.
     *
     */
    public boolean useThirdPartyTransferAlways(){
        return false;
    }

    /**
     * Returns a boolean indicating whether the transfer protocol being used by
     * the implementation preserves the X Bit or not while staging.
     *
     * @return boolean
     */
    public boolean doesPreserveXBit(){
        return false;
    }


    /**
     * Returns a textual description of the transfer implementation.
     *
     * @return a short textual description
     */
    public  String getDescription(){
        return Transfer.DESCRIPTION;
    }

    /**
     * Retrieves the transformation catalog entry for the executable that is
     * being used to transfer the files in the implementation.
     *
     * @param siteHandle  the handle of the  site where the transformation is
     *                    to be searched.
     * @param jobClass    the job Class for the newly added job. Can be one of the
     *                    following:
     *                              stage-in
     *                              stage-out
     *                              inter-pool transfer
     *                              stage-in worker transfer
     *
     *
     * @return  the transformation catalog entry if found, else null.
     */
    public TransformationCatalogEntry getTransformationCatalogEntry(String siteHandle, int jobClass ){

        if(  jobClass == Job.STAGE_IN_WORKER_PACKAGE_JOB && !siteHandle.equalsIgnoreCase( "local") ){
            //PM-538
            //construct an entry for the local site and transfer it.

            return this.defaultTCEntry( Transfer.TRANSFORMATION_NAMESPACE,
                                      Transfer.TRANSFORMATION_NAME,
                                      Transfer.TRANSFORMATION_VERSION,
                                      Transfer.EXECUTABLE_BASENAME,
                                      "local" );

        }

        List tcentries = null;
        try {
            //namespace and version are null for time being
            tcentries = mTCHandle.lookup( Transfer.TRANSFORMATION_NAMESPACE,
                                          Transfer.TRANSFORMATION_NAME,
                                          Transfer.TRANSFORMATION_VERSION,
                                          siteHandle,
                                          TCType.INSTALLED);
        } catch (Exception e) {
            mLogger.log(
                "Unable to retrieve entry from TC for " + getCompleteTCName()
                + " Cause:" + e, LogManager.DEBUG_MESSAGE_LEVEL );
        }

        TransformationCatalogEntry entry = ( tcentries == null ) ?
                 //attempt to create a default entry on the basis of
                 //PEGASUS_HOME defined in the site catalog
                 this.defaultTCEntry( Transfer.TRANSFORMATION_NAMESPACE,
                                      Transfer.TRANSFORMATION_NAME,
                                      Transfer.TRANSFORMATION_VERSION,
                                      Transfer.EXECUTABLE_BASENAME,
                                      siteHandle ):
                 //get what was returned in the transformation catalog
                 (TransformationCatalogEntry) tcentries.get(0);

        
        

        return entry;

    }


    /**
     * An optional method that allows the derived classes to do their own
     * post processing on the the transfer job before it is returned to
     * the calling module.
     *
     * @param job  the <code>TransferJob</code> that has been created.
     */
    public void postProcess( TransferJob job ){

        if( job.getJobType() == Job.STAGE_IN_WORKER_PACKAGE_JOB ){
            //all stage worker jobs are classified as stage in jobs
            //for further use in the planner
            job.setJobType( Job.STAGE_IN_JOB );


            if(   !job.getSiteHandle().equalsIgnoreCase( "local" ) ){
                //PM-538
                //executable for remote stage worker jobs is transferred
                //from local site.
                job.condorVariables.setExecutableForTransfer();
            }

        }

    }


    /**
     * Returns the environment profiles that are required for the default
     * entry to sensibly work. Tries to retrieve the following variables
     *
     * <pre>
     * PEGASUS_HOME
     * GLOBUS_LOCATION
     * LD_LIBRARY_PATH
     * </pre>
     *
     *
     * @param site the site where the job is going to run.
     *
     * @return List of environment variables, else empty list if none are found
     */
    protected List getEnvironmentVariables( String site ){
        List result = new ArrayList(2) ;

        String pegasusHome =  mSiteStore.getEnvironmentVariable( site, "PEGASUS_HOME" );
        if( pegasusHome != null ){
            //we have both the environment variables
            result.add( new Profile( Profile.ENV, "PEGASUS_HOME", pegasusHome ) );
        }

        String globus = mSiteStore.getEnvironmentVariable( site, "GLOBUS_LOCATION" );
        if( globus != null && globus.length() > 1 ){
            //check for LD_LIBRARY_PATH
            String ldpath = mSiteStore.getEnvironmentVariable( site, "LD_LIBRARY_PATH" );
            if ( ldpath == null ){
                //construct a default LD_LIBRARY_PATH
                ldpath = globus;
                //remove trailing / if specified
                ldpath = ( ldpath.charAt( ldpath.length() - 1 ) == File.separatorChar )?
                                ldpath.substring( 0, ldpath.length() - 1 ):
                                ldpath;

                ldpath = ldpath + File.separator + "lib";
                mLogger.log( "Constructed default LD_LIBRARY_PATH " + ldpath,
                         LogManager.DEBUG_MESSAGE_LEVEL );
            }

            //we have both the environment variables
            result.add( new Profile( Profile.ENV, "GLOBUS_LOCATION", globus) );
            result.add( new Profile( Profile.ENV, "LD_LIBRARY_PATH", ldpath) );
        }

        return result;
    }



    /**
     * Returns the namespace of the derivation that this implementation
     * refers to.
     *
     * @return the namespace of the derivation.
     */
    protected String getDerivationNamespace(){
        return Transfer.DERIVATION_NAMESPACE;
    }


    /**
     * Returns the logical name of the derivation that this implementation
     * refers to.
     *
     * @return the name of the derivation.
     */
    protected String getDerivationName(){
        return Transfer.DERIVATION_NAME;
    }

    /**
     * Returns the version of the derivation that this implementation
     * refers to.
     *
     * @return the version of the derivation.
     */
    protected String getDerivationVersion(){
        return Transfer.DERIVATION_VERSION;
    }


    /**
     * It constructs the arguments to the transfer executable that need to be passed
     * to the executable referred to in this transfer mode.
     *
     * @param job   the object containing the transfer node.
     * @return  the argument string
     */
    protected String generateArgumentString(TransferJob job) {
        StringBuffer sb = new StringBuffer();
        if(job.vdsNS.containsKey(Pegasus.TRANSFER_ARGUMENTS_KEY)){
            sb.append(
                      job.vdsNS.removeKey(Pegasus.TRANSFER_ARGUMENTS_KEY)
                      );
        }

        return sb.toString();
    }


    /**
     * Writes to a FileWriter stream the stdin which goes into the magic script
     * via standard input
     *
     * @param job     the transfer job
     * @param writer    the writer to the stdin file.
     * @param files    Collection of <code>FileTransfer</code> objects containing
     *                 the information about sourceam fin and destURL's.
     * @param stagingSite the site where the data will be populated by first
     *                    level staging jobs.
     * @param jobClass    the job Class for the newly added job. Can be one of the
     *                    following:
     *                              stage-in
     *                              stage-out
     *                              inter-pool transfer
     *
     * @throws Exception
     */
    protected void writeStdInAndAssociateCredentials(TransferJob job, FileWriter writer, Collection files, String stagingSite, int jobClass ) throws
        Exception {
        int num = 1;
        for( Iterator it = files.iterator(); it.hasNext(); ){
            FileTransfer ft = (FileTransfer) it.next();
            NameValue source = ft.getSourceURL();
            //we want to leverage multiple dests if possible
            NameValue dest   = ft.getDestURL( true );

            //write to the file one URL pair at a time
            StringBuffer urlPair = new StringBuffer( );
            urlPair.append( "# " ).append( "src " ).append( num ).append( " " ).append( source.getKey() ).append( " ").append( "prio" ).append( " ").append( ft.getPriority()).append( "\n" ).
                    append( source.getValue() ).append( "\n" ).
                    append( "# " ).append( "dst " ).append( num ).append( " " ).append( dest.getKey() ).append( "\n" ).
                    append( dest.getValue() ).append( "\n" );
            writer.write( urlPair.toString() );
            writer.flush();
            num++;

            //associate any credential required , both with destination
            // and the source urls
            job.addCredentialType( source.getValue() );
            job.addCredentialType( dest.getValue() );
        }


    }

    /**
     * Returns the complete name for the transformation.
     *
     * @return the complete name.
     */
    protected String getCompleteTCName(){
        return Separator.combine( Transfer.TRANSFORMATION_NAMESPACE,
                                  Transfer.TRANSFORMATION_NAME,
                                  Transfer.TRANSFORMATION_VERSION);
    }
}
