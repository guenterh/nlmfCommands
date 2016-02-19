package org.swissbib.mf.stream.converter.xml;

import org.culturegraph.mf.exceptions.MetafactureException;
import org.culturegraph.mf.framework.DefaultXmlPipe;
import org.culturegraph.mf.framework.StreamReceiver;

import org.culturegraph.mf.framework.XmlReceiver;
import org.culturegraph.mf.framework.annotations.Description;
import org.culturegraph.mf.framework.annotations.In;
import org.culturegraph.mf.framework.annotations.Out;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Pattern;

/**
 *
 * A generic xml reader with kind of DTD processing
 * @author Guenter Hipler
 */
@Description("A generic xml reader which cares about Entity references")
@In(XmlReceiver.class)
@Out(StreamReceiver.class)
public class GenericXmlDTDHandler extends DefaultXmlPipe<StreamReceiver> {


    private static final Pattern TABS = Pattern.compile("\t+");
    private final String recordTagName;
    private boolean inRecord;
    private StringBuilder valueBuffer = new StringBuilder();

    public GenericXmlDTDHandler() {
        super();
        this.recordTagName = System.getProperty("org.culturegraph.metamorph.xml.recordtag");
        if (recordTagName == null) {
            throw new MetafactureException("Missing name for the tag marking a record.");
        }
    }

    public GenericXmlDTDHandler(final String recordTagName) {
        super();
        this.recordTagName = recordTagName;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
            throws SAXException {

        if (inRecord) {
            writeValue();
            getReceiver().startEntity(localName);
            writeAttributes(attributes);
        } else if (localName.equals(recordTagName)) {
            final String identifier = attributes.getValue("id");
            if (identifier == null) {
                getReceiver().startRecord("");
            } else {
                getReceiver().startRecord(identifier);
            }
            writeAttributes(attributes);
            inRecord = true;
        }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) throws SAXException {
        if (inRecord) {
            writeValue();
            if (localName.equals(recordTagName)) {
                inRecord = false;
                getReceiver().endRecord();
            } else {
                getReceiver().endEntity();
            }
        }
    }

    @Override
    public void characters(final char[] chars, final int start, final int length) throws SAXException {
        if (inRecord) {
            valueBuffer.append(TABS.matcher(new String(chars, start, length)).replaceAll(""));
        }
    }

    private void writeValue() {
        final String value = valueBuffer.toString();
        if (!value.trim().isEmpty()) {
            getReceiver().literal("value", value.replace('\n', ' '));
        }
        valueBuffer = new StringBuilder();
    }

    private void writeAttributes(final Attributes attributes) {
        final int length = attributes.getLength();

        for (int i = 0; i < length; ++i) {
            final String name = attributes.getLocalName(i);
            final String value = attributes.getValue(i);
            getReceiver().literal(name, value);
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        //return super.resolveEntity(publicId, systemId);
        return new InputSource(new StringReader(""));

    }

}
