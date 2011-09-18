package com.tinkerpop.rexster.config;

import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.rexster.Tokens;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.HighlyAvailableGraphDatabase;

import java.util.HashMap;
import java.util.Iterator;

public class Neo4jGraphConfiguration implements GraphConfiguration {

    public Graph configureGraphInstance(Configuration properties) throws GraphConfigurationException {

        String graphFile = properties.getString(Tokens.REXSTER_GRAPH_FILE);

        if (graphFile == null || graphFile.length() == 0) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_FILE);
        }

        boolean highAvailabilityMode = properties.getBoolean(Tokens.REXSTER_GRAPH_HA, false);

        // get the <properties> section of the xml configuration
        HierarchicalConfiguration graphSectionConfig = (HierarchicalConfiguration) properties;
        SubnodeConfiguration neo4jSpecificConfiguration;

        try {
            neo4jSpecificConfiguration = graphSectionConfig.configurationAt(Tokens.REXSTER_GRAPH_PROPERTIES);
        } catch (IllegalArgumentException iae) {
            throw new GraphConfigurationException("Check graph configuration. Missing or empty configuration element: " + Tokens.REXSTER_GRAPH_PROPERTIES);
        }

        try {

            // properties to initialize the neo4j instance.
            HashMap<String, String> neo4jProperties = new HashMap<String, String>();

            // read the properties from the xml file and convert them to properties
            // to be injected into neo4j.
            Iterator<String> neo4jSpecificConfigurationKeys = neo4jSpecificConfiguration.getKeys();
            while (neo4jSpecificConfigurationKeys.hasNext()) {
                String key = neo4jSpecificConfigurationKeys.next();

                // replace the ".." put in play by apache commons configuration.  that's expected behavior
                // due to parsing key names to xml.
                neo4jProperties.put(key.replace("..", "."), neo4jSpecificConfiguration.getString(key));
            }

            if (highAvailabilityMode) {
                if (!neo4jProperties.containsKey("ha.machine_id")) {
                    throw new GraphConfigurationException("Check graph configuration. Neo4j HA requires [ha.machine_id] in the <properties> of the configuration");
                }

                if (!neo4jProperties.containsKey("ha.server")) {
                    throw new GraphConfigurationException("Check graph configuration. Neo4j HA requires [ha.server] <properties> of the configuration");
                }

                if (!neo4jProperties.containsKey("ha.zoo_keeper_servers")) {
                    throw new GraphConfigurationException("Check graph configuration. Neo4j HA requires [ha.zoo_keeper_servers] <properties> of the configuration");
                }

                GraphDatabaseService graphDatabaseService = new HighlyAvailableGraphDatabase(graphFile, neo4jProperties);
                return new Neo4jGraph(graphDatabaseService);

            } else {
                return new Neo4jGraph(graphFile, neo4jProperties);
            }

        } catch (Exception ex) {
            throw new GraphConfigurationException(ex);
        }
    }

}
