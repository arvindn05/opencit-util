/*
 * Copyright (C) 2014 Intel Corporation
 * All rights reserved.
 */
package com.intel.dcsg.cpg.configuration;

import com.intel.dcsg.cpg.util.AllCapsNamingStrategy;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jbuhacoff
 */
public class KeyTransformerConfigurationTest {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(KeyTransformerConfigurationTest.class);

    @Test
    public void testKeyTransformer() {
        PropertiesConfiguration env = new PropertiesConfiguration();
        env.setString("FRUIT_COLOR", "red");
        env.setString("FRUIT_SHAPE", "circle");
        KeyTransformerConfiguration config = new KeyTransformerConfiguration(new AllCapsNamingStrategy(), env);
        assertEquals("red", config.getString("fruit.color"));
        assertEquals("circle", config.getString("fruit.shape"));
    }
}
