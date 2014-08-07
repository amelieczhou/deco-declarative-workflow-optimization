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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A Workflow metrics class that stores the metrics about the workflow.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class WorkflowMetrics extends Data implements Cloneable{


    /**
     * The number of compute tasks in the DAX
     */
    @SerializedName("compute_tasks") @Expose private int mNumComputeTasks;

    /**
     * The number of DAX tasks in the DAX
     */
    @SerializedName("dax_tasks") @Expose private int mNumDAXTasks;

    /**
     * The number of DAG tasks in the DAX.
     */
    @SerializedName("dag_tasks") @Expose private int mNumDAGTasks;


    /**
     * The total number of tasks in the executable workflow.
     */
    @SerializedName("total_tasks") @Expose private int mNumTotalTasks;
    
    /**
     * The number of compute jobs.
     */
    @SerializedName("compute_jobs") @Expose private int mNumComputeJobs;

    /**
     * The number of clustered compute jobs.
     */
    @SerializedName("clustered_jobs") @Expose private int mNumClusteredJobs;

    /**
     * The number of stage in transfer jobs.
     */
    @SerializedName("si_tx_jobs") @Expose private int mNumSITxJobs;

    /**
     * The number of stage-out transfer jobs.
     */
    @SerializedName("so_tx_jobs") @Expose private int mNumSOTxJobs;

    /**
     * The number of inter-site transfer jobs.
     */
    @SerializedName("inter_tx_jobs") @Expose private int mNumInterTxJobs;

    /**
     * The number of registration jobs.
     */
    @SerializedName("reg_jobs") @Expose private int mNumRegJobs;

    /**
     * The number of cleanup jobs.
     */
    @SerializedName("cleanup_jobs") @Expose private int mNumCleanupJobs;

    /**
     * The number of create dir jobs.
     */
    @SerializedName("create_dir_jobs") @Expose private int mNumCreateDirJobs;

    /**
     * The number of dax jobs in the workflow
     */
    @SerializedName("dax_jobs") @Expose private int mNumDAXJobs;

    /**
     * The number of DAG jobs in the workflow
     */
    @SerializedName("dag_jobs") @Expose private int mNumDAGJobs;

    /*
     * The number of chmod jobs in the workflow
     */
    @SerializedName("chmod_jobs") @Expose private int mNumChmodJobs;

    /**
     * The total number of  jobs in the executable workflow.
     */
    @SerializedName("total_jobs") @Expose private int mNumTotalJobs;
    
    /**
     * The label of the dax.
     */
    private String mDAXLabel;
    
    

    /**
     * A boolean indicating whether to update task metrics
     */
    private transient boolean mLockTaskMetrics;



    /**
     * The default constructor.
     */
    public WorkflowMetrics() {
        reset( true );
        mLockTaskMetrics = false;
    }

    /**
     * Resets the internal counters to zero.
     *
     * @param resetTaskMetrics   whether to reset task metrics or not
     */
    public final void  reset( boolean resetTaskMetrics ){

        if( resetTaskMetrics ){
            mNumComputeTasks  = 0;
            mNumDAXTasks      = 0;
            mNumDAGTasks      = 0;
            mNumTotalTasks    = 0;
        }
        
        mNumTotalJobs    = 0;
        mNumComputeJobs  = 0;
        mNumSITxJobs     = 0;
        mNumSOTxJobs     = 0;
        mNumInterTxJobs  = 0;
        mNumRegJobs      = 0;
        mNumCleanupJobs  = 0;
        mNumCreateDirJobs = 0;
        mNumClusteredJobs = 0;
        mNumChmodJobs     = 0;
        mNumDAXJobs          = 0;
        mNumDAGJobs          = 0;
    }


    /**
     * Sets the DAXlabel.
     *
     * @param label  the dax label
     */
    public void setLabel( String label ){
        mDAXLabel = label;
    }


    /**
     * Returns the DAXlabel.
     *
     * @return the dax label
     */
    public String getLabel(  ){
        return mDAXLabel;
    }

    /**
     * Sets the lock task metrics parameters.
     * If the lock is set, the task metrics are no longer updated on subsequent
     * calls to increment / decrement.
     *
     * @param lock  the boolean parameter
     */
    public void lockTaskMetrics( boolean lock ){
        this.mLockTaskMetrics = lock;
    }

    /**
     * Increment the metrics when on the basis of type of job.
     *
     * @param job                the job being added.
     */
    public void increment( Job job  ){
         this.incrementJobMetrics( job );
         this.incrementTaskMetrics( job );
    }


    /**
     * Increment the metrics when on the basis of type of job.
     *
     * @param job                the job being added.
     *
     */
    private void incrementTaskMetrics( Job job ){
        //sanity check
        if( job == null || mLockTaskMetrics ){
            //job is null or we have locked updates to task metrics
            return;
        }

        //update the total account
        mNumTotalTasks++;

        //update the task metrics
        //incrementJobMetrics on basis of type of job
        int type = job.getJobType();
        switch( type ){

            //treating compute and staged compute as same
            case Job.COMPUTE_JOB:
                mNumComputeTasks++;
                break;

            case Job.DAX_JOB:
                mNumDAXTasks++;
                break;

            case Job.DAG_JOB:
                mNumDAGTasks++;
                break;


            default:
                throw new RuntimeException( "Unknown or Unassigned Task " + job.getID() + " of type " + type );
        }
    }

    /**
     * Increment the metrics when on the basis of type of job.
     *
     * @param job                the job being added.
     */
    private void incrementJobMetrics( Job job  ){
        //sanity check
        if( job == null ){
            return;
        }

        //incrementJobMetrics the total
        mNumTotalJobs++;


        //incrementJobMetrics on basis of type of job
        int type = job.getJobType();
        switch( type ){

            //treating compute and staged compute as same
            case Job.COMPUTE_JOB:
/*
                if( job instanceof AggregatedJob ){
                    mNumClusteredJobs++;
                    
                    for( Iterator<Job> it = ((AggregatedJob)job).constituentJobsIterator(); it.hasNext(); ){
                        Job j = it.next();
                        this.incrementJobMetrics( j , false );
                    }
                    
                }else{
                    if( incrementJobs ){
                        mNumComputeJobs++;
                    }
                    mNumComputeTasks++;
                }
 */
                if( job instanceof AggregatedJob ){
                    mNumClusteredJobs++;
                }
                else{
                    mNumComputeJobs++;
                }
                break;

            case Job.DAX_JOB:
                mNumDAXJobs++;
                break;

            case Job.DAG_JOB:
                mNumDAGJobs++;
                break;


            case Job.STAGE_IN_JOB:
            case Job.STAGE_IN_WORKER_PACKAGE_JOB:
                mNumSITxJobs++;
                break;

            case Job.STAGE_OUT_JOB:
                mNumSOTxJobs++;
                break;

            case Job.INTER_POOL_JOB:
                mNumInterTxJobs++;
                break;

            case Job.REPLICA_REG_JOB:
                mNumRegJobs++;
                break;

            case Job.CLEANUP_JOB:
                mNumCleanupJobs++;
                break;

            case Job.CREATE_DIR_JOB:
                mNumCreateDirJobs++;
                break;


            case Job.CHMOD_JOB:
                mNumChmodJobs++;
                break;

            default:
                throw new RuntimeException( "Unknown or Unassigned job " + job.getID() + " of type " + type );

        }

    }

    /**
     * Decrement the metrics when on the basis of type of job being removed
     *
     * @param job                the job being added.
     */
    public void decrement( Job job  ){
         this.decrementJobMetrics( job );
         this.decrementTaskMetrics( job );
    }



    /**
     * Decrement the metrics when on the basis of type of job.
     * Does not decrement the task related metrics.
     *
     * @param job  the job being removed.
     */
    private void decrementJobMetrics( Job job ){
        //sanity check
        if( job == null ){
            return;
        }

        //incrementJobMetrics the total
        mNumTotalJobs--;

        //incrementJobMetrics on basis of type of job
        int type = job.getJobType();
        switch( type ){

            //treating compute and staged compute as same
            case Job.COMPUTE_JOB:
                if( job instanceof AggregatedJob ){
                    mNumClusteredJobs--;
                }
                else{
                    mNumComputeJobs--;
                }
                break;

            case Job.DAX_JOB:
                mNumDAXJobs--;
                break;

            case Job.DAG_JOB:
                mNumDAGJobs--;
                break;


            case Job.STAGE_IN_JOB:
            case Job.STAGE_IN_WORKER_PACKAGE_JOB:
                mNumSITxJobs--;
                break;

            case Job.STAGE_OUT_JOB:
                mNumSOTxJobs--;
                break;

            case Job.INTER_POOL_JOB:
                mNumInterTxJobs--;
                break;

            case Job.REPLICA_REG_JOB:
                mNumRegJobs--;
                break;

            case Job.CLEANUP_JOB:
                mNumCleanupJobs--;
                break;

             case Job.CREATE_DIR_JOB:
                mNumCreateDirJobs--;
                break;

            case Job.CHMOD_JOB:
                mNumChmodJobs--;
                break;

            default:
                throw new RuntimeException( "Unknown or Unassigned job " + job.getID() + " of type " + type );

        }

    }


    /**
     * Decrement the task metrics when on the basis of type of job.
     *
     * @param job   the job being removed.
     *
     */
    private void decrementTaskMetrics( Job job ){
        //sanity check
        if( job == null || mLockTaskMetrics ){
            //job is null or we have locked updates to task metrics
            return;
        }

        //decrement the total count
        mNumTotalTasks--;

        //update the task metrics
        //incrementJobMetrics on basis of type of job
        int type = job.getJobType();
        switch( type ){

            //treating compute and staged compute as same
            case Job.COMPUTE_JOB:
                mNumComputeTasks--;
                break;

            case Job.DAX_JOB:
                mNumDAXTasks--;
                break;

            case Job.DAG_JOB:
                mNumDAGTasks--;
                break;


            default:
                throw new RuntimeException( "Unknown or Unassigned Task " + job.getID() + " of type " + type );
        }
    }

    /**
     * Returns a textual description of the object.
     *
     * @return Object
     */
    public String toString(){
        StringBuffer sb = new StringBuffer();

        
        append( sb, "dax-label", this.mDAXLabel );

        //dax task related metrics
        append( sb, "compute-tasks.count", this.mNumComputeTasks );
        append( sb, "dax-tasks.count", this.mNumDAXTasks );
        append( sb, "dag-tasks.count", this.mNumDAGTasks );
        append( sb, "total-tasks.count", this.mNumTotalTasks );

        //job related metrics
        append( sb, "createdir-jobs.count", this.mNumCreateDirJobs );
        append( sb, "chmod-jobs.count", this.mNumChmodJobs );
        append( sb, "unclustered-compute-jobs.count", this.mNumComputeJobs );
        append( sb, "clustered-compute-jobs.count", this.mNumClusteredJobs );
        append( sb, "dax-jobs.count", this.mNumDAXJobs );
        append( sb, "dag-jobs.count", this.mNumDAGJobs );
        append( sb, "si-jobs.count", this.mNumSITxJobs );
        append( sb, "so-jobs.count", this.mNumSOTxJobs );
        append( sb, "inter-jobs.count", this.mNumInterTxJobs );
        append( sb, "reg-jobs.count", this.mNumRegJobs );
        append( sb, "cleanup-jobs.count", this.mNumCleanupJobs );
        append( sb, "total-jobs.count", this.mNumTotalJobs );

        return sb.toString();
    }

    /**
     * Appends a key=value pair to the StringBuffer.
     *
     * @param buffer    the StringBuffer that is to be appended to.
     * @param key   the key.
     * @param value the value.
     */
    protected void append( StringBuffer buffer, String key, String value ){
        buffer.append( key ).append( " = " ).append( value ).append( "\n" );
    }

    /**
     * Appends a key=value pair to the StringBuffer.
     *
     * @param buffer    the StringBuffer that is to be appended to.
     * @param key   the key.
     * @param value the value.
     */
    protected void append( StringBuffer buffer, String key, int value ){
        buffer.append( key ).append( " = " ).append( value ).append( "\n" );
    }

    /**
     * Converts the planner metrics to JSON
     * 
     * @return  the planner metrics in JSON
     */
    public String toJson(){
        Gson gson = new Gson();
        return gson.toJson( this );      
    }

    /**
     * Converts the planner metrics to JSON
     * 
     * @return  the planner metrics in JSON
     */
    public String toPrettyJson(){
        //Gson gson = new Gson();
        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();
        return gson.toJson( this );      
    }

    /**
     * Returns the clone of the object.
     *
     * @return the clone
     */
    public Object clone(){
        WorkflowMetrics wm;
        try {
            wm = (WorkflowMetrics)super.clone();
        }
        catch (CloneNotSupportedException e) {
            //somewhere in the hierarch chain clone is not implemented
            throw new RuntimeException( "Clone not implemented in the base class of " +
                                        this.getClass().getName(),
                                        e);
        }

        wm.mLockTaskMetrics = this.mLockTaskMetrics;
        wm.mNumCleanupJobs = this.mNumCleanupJobs;
        wm.mNumComputeJobs = this.mNumComputeJobs;
        wm.mNumInterTxJobs = this.mNumInterTxJobs;
        wm.mNumRegJobs     = this.mNumRegJobs;
        wm.mNumSITxJobs    = this.mNumSITxJobs;
        wm.mNumSOTxJobs    = this.mNumSOTxJobs;
        wm.mNumTotalJobs   = this.mNumTotalJobs;
        wm.mDAXLabel       = this.mDAXLabel;
        wm.mNumCreateDirJobs = this.mNumCreateDirJobs;
        wm.mNumClusteredJobs = this.mNumClusteredJobs;
        wm.mNumChmodJobs     = this.mNumChmodJobs;
        wm.mNumDAXJobs       = this.mNumDAGJobs;
        wm.mNumDAGJobs       = this.mNumDAGJobs;
        wm.mNumComputeTasks  = this.mNumComputeTasks;
        wm.mNumDAXTasks      = this.mNumDAXTasks;
        wm.mNumDAGTasks      = this.mNumDAGTasks;
        wm.mNumTotalTasks    = this.mNumTotalTasks;
        return wm;
    }


}
