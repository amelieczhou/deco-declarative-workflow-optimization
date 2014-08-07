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

package edu.isi.pegasus.planner.transfer.refiner;


import edu.isi.pegasus.planner.classes.ADag;
import edu.isi.pegasus.planner.classes.Job;
import edu.isi.pegasus.planner.classes.FileTransfer;

import edu.isi.pegasus.common.logging.LogManager;

import edu.isi.pegasus.planner.refiner.ReplicaCatalogBridge;

import edu.isi.pegasus.planner.transfer.MultipleFTPerXFERJobRefiner;


import edu.isi.pegasus.planner.provenance.pasoa.PPS;
import edu.isi.pegasus.planner.provenance.pasoa.pps.PPSFactory;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import edu.isi.pegasus.planner.classes.PegasusBag;
import edu.isi.pegasus.planner.transfer.Implementation;
import edu.isi.pegasus.planner.transfer.Refiner;

/**
 * An Empty implementation for performance evaluation purposes
 *
 * @author Karan Vahi
 * @version $Revision: 5403 $
 */

public class Empty extends MultipleFTPerXFERJobRefiner {

    /**
     * A short description of the transfer refinement.
     */
    public static final String DESCRIPTION = "Empty Implementation";

    /**
     * The string holding  the logging messages
     */
    protected String mLogMsg;


    /**
     * A Map containing information about which logical file has been
     * transferred to which site and the name of the stagein transfer node
     * that is transferring the file from the location returned from
     * the replica catalog.
     * The key for the hashmap is logicalfilename:sitehandle and the value would be
     * the name of the transfer node.
     *
     */
    protected Map mFileTable;

    /**
     * The handle to the provenance store implementation.
     */
    protected PPS mPPS;
    
    /**
     * Boolean indicating whether to create registration jobs or not.
     */
    protected Boolean mCreateRegistrationJobs;

    /**
     * The overloaded constructor.
     *
     * @param dag        the workflow to which transfer nodes need to be added.
     * @param bag   the bag of initialization objects.
     *
     */
    public Empty( ADag dag,
                    PegasusBag bag ){
        super( dag, bag );
        mLogMsg = null;
        mFileTable = new HashMap(10000);
        
        mCreateRegistrationJobs = ( mProps.getReplicaMode() != null ) &&
                                    mProps.createRegistrationJobs();
        if( !mCreateRegistrationJobs ){
            mLogger.log( "No Replica Registration Jobs will be created .",
                          LogManager.CONFIG_MESSAGE_LEVEL );
        }
        
        //load the PPS implementation
        mPPS = PPSFactory.loadPPS( this.mProps );

        mXMLStore.add( "<workflow url=\"" + mPOptions.getDAX() + "\">" );

        //call the begin workflow method
        try{
            mPPS.beginWorkflowRefinementStep( this, PPS.REFINEMENT_STAGE, false );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception", e );
        }

        //clear the XML store
        mXMLStore.clear();

    }


    /**
     * Adds the stage in transfer nodes which transfer the input files for a job,
     * from the location returned from the replica catalog to the job's execution
     * pool.
     *
     * @param job   <code>Job</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param symlinkFiles Collection of <code>FileTransfer</code> objects containing
     *                     source and destination file url's for symbolic linking
     *                     on compute site.
     */
    public  void addStageInXFERNodes( Job job,
                                      Collection<FileTransfer> files,
                                      Collection<FileTransfer> symlinkFiles ){
        
        addStageInXFERNodes( job,
                             files,
                             Refiner.STAGE_IN_PREFIX + Refiner.LOCAL_PREFIX,
                             this.mTXStageInImplementation);
        
        addStageInXFERNodes( job,
                             symlinkFiles,
                             Refiner.STAGE_IN_PREFIX + Refiner.REMOTE_PREFIX,
                             this.mTXSymbolicLinkImplementation );
        
    }
    
    /**
     * Adds the stage in transfer nodes which transfer the input files for a job,
     * from the location returned from the replica catalog to the job's execution
     * pool.
     *
     * @param job   <code>Job</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param prefix the prefix to be used while constructing the transfer jobname.
     * @param implementation  the transfer implementation to use
     * 
     */
    public  void addStageInXFERNodes( Job job,
                                      Collection<FileTransfer> files,
                                      String prefix,
                                      Implementation implementation ){





    }


    /**
     * Adds the inter pool transfer nodes that are required for  transferring
     * the output files of the parents to the jobs execution site.
     *
     * @param job   <code>Job</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     *
     * @param localTransfer  boolean indicating that associated transfer job will run
     *                       on local site.
     */
    public void addInterSiteTXNodes(Job job,
                                    Collection files,
                                    boolean localTransfer ){


    }

    /**
     * Adds the stageout transfer nodes, that stage data to an output site
     * specified by the user.
     *
     * @param job   <code>Job</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param rcb   bridge to the Replica Catalog. Used for creating registration
     *              nodes in the workflow.
     * @param localTransfer  boolean indicating that associated transfer job will run
     *                       on local site.
     */
    public void addStageOutXFERNodes(Job job,
                                     Collection files,
                                     ReplicaCatalogBridge rcb,
                                     boolean localTransfer ) {

        this.addStageOutXFERNodes( job, files, rcb, localTransfer, false);
    }

    /**
     * Adds the stageout transfer nodes, that stage data to an output site
     * specified by the user.
     *
     * @param job   <code>Job</code> object corresponding to the node to
     *              which the files are to be transferred to.
     * @param files Collection of <code>FileTransfer</code> objects containing the
     *              information about source and destURL's.
     * @param rcb   bridge to the Replica Catalog. Used for creating registration
     *              nodes in the workflow.
     * @param localTransfer  boolean indicating that associated transfer job will run
     *                       on local site.
     * @param deletedLeaf to specify whether the node is being added for
     *                      a deleted node by the reduction engine or not.
     *                      default: false
     */
    public  void addStageOutXFERNodes( Job job,
                                       Collection files,
                                       ReplicaCatalogBridge rcb,
                                       boolean localTransfer,
                                       boolean deletedLeaf ){

        


    }

    /**
     * Creates the registration jobs, which registers the materialized files on
     * the output site in the Replica Catalog.
     *
     * @param regJobName  The name of the job which registers the files in the
     *                    Replica Mechanism.
     * @param job         The job whose output files are to be registered in the
     *                    Replica Mechanism.
     * @param files       Collection of <code>FileTransfer</code> objects containing
     *                    the information about source and destURL's.
     * @param rcb   bridge to the Replica Catalog. Used for creating registration
     *              nodes in the workflow.
     *
     *
     * @return the registration job.
     */
    protected Job createRegistrationJob(String regJobName,
                                            Job job,
                                            Collection files,
                                            ReplicaCatalogBridge rcb ) {

        Job regJob = rcb.makeRCRegNode( regJobName, job, files );

        //log the registration action for provenance purposes
        StringBuffer sb = new StringBuffer();
  

        mXMLStore.add( sb.toString() );

        //log the action for creating the relationship assertions
        try{
            mPPS.registrationIntroducedFor( regJob.getName(),job.getName() );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception while logging relationship assertion for registration",
                                         e );
        }



        return regJob;
    }


    /**
     * Signals that the traversal of the workflow is done. It signals to the
     * Provenace Store, that refinement is complete.
     */
    public void done(){

        try{
            mPPS.endWorkflowRefinementStep( this );
        }
        catch( Exception e ){
            throw new RuntimeException( "PASOA Exception", e );
        }


    }





    /**
     * Add a new job to the workflow being refined.
     *
     * @param job  the job to be added.
     */
    public void addJob(Job job){
        mDAG.add(job);
    }

    /**
     * Adds a new relation to the workflow being refiner.
     *
     * @param parent    the jobname of the parent node of the edge.
     * @param child     the jobname of the child node of the edge.
     */
    public void addRelation(String parent,
                            String child){
        mLogger.log("Adding relation " + parent + " -> " + child,
                    LogManager.DEBUG_MESSAGE_LEVEL);
        
        mDAG.addNewRelation(parent,child);

    }


    /**
     * Adds a new relation to the workflow. In the case when the parent is a
     * transfer job that is added, the parentNew should be set only the first
     * time a relation is added. For subsequent compute jobs that maybe
     * dependant on this, it needs to be set to false.
     *
     * @param parent    the jobname of the parent node of the edge.
     * @param child     the jobname of the child node of the edge.
     * @param site      the execution pool where the transfer node is to be run.
     * @param parentNew the parent node being added, is the new transfer job
     *                  and is being called for the first time.
     */
    public void addRelation(String parent,
                            String child,
                            String site,
                            boolean parentNew){
        mLogger.log("Adding relation " + parent + " -> " + child,
                    LogManager.DEBUG_MESSAGE_LEVEL);
        mDAG.addNewRelation(parent,child);

    }


    /**
     * Returns a textual description of the transfer mode.
     *
     * @return a short textual description
     */
    public  String getDescription(){
        return Empty.DESCRIPTION;
    }

    

}
