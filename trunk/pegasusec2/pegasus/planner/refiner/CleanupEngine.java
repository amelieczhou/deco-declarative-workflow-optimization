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


package edu.isi.pegasus.planner.refiner;

import edu.isi.pegasus.planner.classes.ADag;
import edu.isi.pegasus.planner.classes.Job;

import edu.isi.pegasus.planner.refiner.cleanup.CleanupStrategy;

import edu.isi.pegasus.planner.partitioner.graph.Graph;
import edu.isi.pegasus.planner.partitioner.graph.GraphNode;
import edu.isi.pegasus.planner.partitioner.graph.Adapter;

import java.util.Iterator;
import edu.isi.pegasus.planner.classes.PegasusBag;
import edu.isi.pegasus.planner.refiner.cleanup.CleanupFactory;

/**
 * The refiner that results in the creation of cleanup jobs within the workflow.
 *
 * @author  Karan Vahi
 * @version $Revision$
 *
 */
public class CleanupEngine extends Engine {


    /**
     * The overloaded constructor.
     *
     * @param bag  the bag of initialization objects
     *
     */
    public CleanupEngine( PegasusBag bag ) {
        super( bag );
    }

    /**
     * Adds the cleanup jobs in the workflow that removes the files staged to the
     * remote site.
     *
     * @param dag the scheduled dag that has to be clustered.
     *
     * @return ADag containing the cleanup jobs for the workflow.
     */
    public ADag addCleanupJobs( ADag dag ) {
        ADag result;

        //load the appropriate strategy and implementation that is to be used
        //CleanupStrategy strategy = new InPlace( mBag );
        CleanupStrategy strategy = CleanupFactory.loadCleanupStraegyInstance( mBag );

        //we first need to convert internally into graph format
        Graph resultGraph =  strategy.addCleanupJobs( Adapter.convert(dag ) );

        //convert back to ADag and return
        result = dag;
        //we need to reset the jobs and the relations in it
        result.clearJobs();

        //traverse through the graph and jobs and edges
        for( Iterator it = resultGraph.nodeIterator(); it.hasNext(); ){
            GraphNode node = ( GraphNode )it.next();

            //get the job associated with node
            result.add( ( Job )node.getContent() );

            //all the children of the node are the edges of the DAG
            for( Iterator childrenIt = node.getChildren().iterator(); childrenIt.hasNext(); ){
                GraphNode child = ( GraphNode ) childrenIt.next();
                result.addNewRelation( node.getID(), child.getID() );
            }
        }

        return result;
    }
}
