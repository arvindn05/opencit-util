/*
 * Copyright (C) 2013 Intel Corporation
 * All rights reserved.
 */
package com.intel.dcsg.cpg.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

/**
 * Reading XML example:
 * 
 * JAXB codec = new JAXB();
 * MyPojo pojo = codec.read(xmlstring, MyPojo.class);
 * 
 * Writing XML example:
 * 
 * JAXB codec = new JAXB();
 * String xmlstring = codec.write(pojo);
 * 
 * A single codec instance can be used for reading and writing any number of
 * times and it does not keep any state.
 * 
 * @author jbuhacoff
 */
public class JAXB {

    private ClassLoader jaxbClassLoader = null;

    public ClassLoader getJaxbClassLoader() {
        return jaxbClassLoader;
    }

    /**
     * Some environments such as OSGi require the use of their own ClassLoader
     * when using JAXB. Use this method to set the ClassLoader that should be
     * used when deserializing XML responses with JAXB. The default is to use
     * the system class loader.
     *
     * @param classLoader to use with JAXB, or null to use the default class
     * loader
     */
    public void setJaxbClassLoader(ClassLoader classLoader) {
        jaxbClassLoader = classLoader;
    }

    /**
     * Does not allow XML External Entity (XXE) injection CWE-611
     * http://cwe.mitre.org/data/definitions/611.html
     *
     * @param <T>
     * @param document
     * @param valueType
     * @return
     * @throws IOException
     * @throws JAXBException
     */
    public <T> T read(String document, Class<T> valueType) throws IOException, JAXBException, XMLStreamException {
        JAXBContext jc;
        if (jaxbClassLoader != null) {
            jc = JAXBContext.newInstance(valueType.getPackage().getName(), jaxbClassLoader);
        } else {
            jc = JAXBContext.newInstance(valueType.getPackage().getName());
        }
        // CWE-611 restrict XML external entity references
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false); // if true allows sender to include external files via entity declaration in the DTD, which is a security vulnerability
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false); // if true allows sender to declare a DTD, and the DTD spec has security vulnerabilities so a reference implementation cannot be secure
        xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, true); // if true allows sender to encode &gt; &lt; &quot; &amp; and &apos;  but not custom-defined entity references because we disable dtd support ; http://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
        XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(new StringReader(document)));
        Unmarshaller u = jc.createUnmarshaller();
        JAXBElement<T> doc = u.unmarshal(xsr, valueType);
        return doc.getValue();
    }
    
    public String write(Object value) throws JAXBException {
        String jaxbPackage;
        if( value instanceof JAXBElement ) {
            jaxbPackage = ((JAXBElement)value).getValue().getClass().getPackage().getName();
        }
        else {
            jaxbPackage = value.getClass().getPackage().getName();
        }
        JAXBContext jc;
        if (jaxbClassLoader != null) {
            jc = JAXBContext.newInstance(jaxbPackage, jaxbClassLoader);
        } else {
            jc = JAXBContext.newInstance(jaxbPackage);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        m.marshal(value, out);
        byte[] xml = out.toByteArray();
        return new String(xml, Charset.forName("UTF-8"));
    }
}
