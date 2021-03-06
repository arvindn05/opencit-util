/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.jdbi.util;

//import com.intel.mtwilson.My;
//import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import org.skife.jdbi.v2.tweak.ConnectionFactory;

/**
 *
 * @author jbuhacoff
 */
public class ExistingConnectionFactory implements ConnectionFactory {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExistingConnectionFactory.class);

    private Connection connection;
    public ExistingConnectionFactory(Connection connection) {
        this.connection = connection;
    }

    
    @Override
    public Connection openConnection() throws SQLException {
        log.debug("openConnection returning connection: {}", connection);
        return connection;
        /*
        try {
            return My.jdbc().connection();
        } catch (IOException | ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
        */
    }
}
