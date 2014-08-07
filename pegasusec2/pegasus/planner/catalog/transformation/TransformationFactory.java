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



package edu.isi.pegasus.planner.catalog.transformation;

import edu.isi.pegasus.planner.common.PegasusProperties;
import edu.isi.pegasus.common.util.DynamicLoader;
import edu.isi.pegasus.planner.catalog.TransformationCatalog;
import edu.isi.pegasus.common.logging.LogManager;
import edu.isi.pegasus.common.logging.LogManagerFactory;
import edu.isi.pegasus.planner.classes.PegasusBag;


/**
 * A factory class to load the appropriate implementation of Transformation
 * Catalog as specified by properties.
 *
 * @author Karan Vahi
 * @version $Revision$
 */
public class TransformationFactory {

    /**
     * Some Constants for backward compatibility.
     */

    public static final String DEFAULT_TC_CLASS = "File";

    /**
     * The default package where all the implementations reside.
     */
    public static final String DEFAULT_PACKAGE_NAME =
        "edu.isi.pegasus.planner.catalog.transformation.impl";

    /**
     * Connects the interface with the transformation catalog implementation. The
     * choice of backend is configured through properties. This method uses default
     * properties from the property singleton.
     *
     * @return handle to the Transformation Catalog.
     *
     * @throws TransformationFactoryException that nests any error that
     *         might occur during the instantiation
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    public static TransformationCatalog loadInstance() throws
        TransformationFactoryException {
        PegasusBag bag = new PegasusBag();
        bag.add( PegasusBag.PEGASUS_LOGMANAGER,  LogManagerFactory.loadSingletonInstance() );
        bag.add( PegasusBag.PEGASUS_PROPERTIES, PegasusProperties.nonSingletonInstance() );
        
        return loadInstance( bag );
    }

    

    /**
     * Connects the interface with the transformation catalog implementation. The
     * choice of backend is configured through properties. This class is
     * useful for non-singleton instances that may require changing
     * properties.
     *
     * @param properties is an instance of properties to use.
     *
     * @return handle to the Transformation Catalog.
     *
     * @throws TransformationFactoryException that nests any error that
     *         might occur during the instantiation
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    public static TransformationCatalog loadInstance( PegasusProperties
        properties) throws
        TransformationFactoryException {
                
        PegasusBag bag = new PegasusBag();
        bag.add( PegasusBag.PEGASUS_LOGMANAGER,  LogManagerFactory.loadSingletonInstance( properties ) );
        bag.add( PegasusBag.PEGASUS_PROPERTIES, properties );
        
        return loadInstance( bag );
    }
     

    
    /**
     * Connects the interface with the transformation catalog implementation. The
     * choice of backend is configured through properties. This class is
     * useful for non-singleton instances that may require changing
     * properties.
     *
     * @param properties is an instance of properties to use.
     * @param logger  handle to the logging.
     * 
     * @return handle to the Transformation Catalog.
     *
     * @throws TransformationFactoryException that nests any error that
     *         might occur during the instantiation
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    public static TransformationCatalog loadInstance( PegasusProperties
        properties, LogManager logger ) throws
        TransformationFactoryException {
                
        PegasusBag bag = new PegasusBag();
        bag.add( PegasusBag.PEGASUS_LOGMANAGER, logger );
        bag.add( PegasusBag.PEGASUS_PROPERTIES, properties );
        
        return loadInstance( bag );
    }
    
    /**
     * Connects the interface with the transformation catalog implementation. The
     * choice of backend is configured through properties. This class is
     * useful for non-singleton instances that may require changing
     * properties.
     *
     * @param bag is bag of initialization objects
     *
     * @return handle to the Transformation Catalog.
     *
     * @throws TransformationFactoryException that nests any error that
     *         might occur during the instantiation
     *
     * @see #DEFAULT_PACKAGE_NAME
     */
    public static TransformationCatalog loadInstance( PegasusBag bag ) throws
        TransformationFactoryException {

 //       TransformationCatalog tc = null;

        /* get the implementor from properties */
        String catalogImplementor = bag.getPegasusProperties().getTCMode().trim(); //return "Text"

        /* prepend the package name if required */
        catalogImplementor = (catalogImplementor.indexOf('.') == -1) ? //Amelie: check the Text class in DEFAULT_PACKAGE_NAME
            //pick up from the default package
            DEFAULT_PACKAGE_NAME + "." + catalogImplementor :
            //load directly
            catalogImplementor;

        TransformationCatalog catalog;

        /* try loading the catalog implementation dynamically */
        try {            
            DynamicLoader dl = new DynamicLoader(catalogImplementor);
            catalog = ( TransformationCatalog ) dl.instantiate( new Object[0] );

            if ( catalog == null ){
                throw new RuntimeException( "Unable to load " + catalogImplementor );
            }

            catalog.initialize( bag ); //Amelie: Text.initialize(); Text.mTCStore is loaded with the transformations
        }
        catch (Exception e) {
            throw new TransformationFactoryException(
                " Unable to instantiate Transformation Catalog ",
                catalogImplementor,
                e);
        }
        if (catalog == null) {
            throw new TransformationFactoryException(
                " Unable to instantiate Transformation Catalog ",
                catalogImplementor);
        }
        return catalog;

    }

}
