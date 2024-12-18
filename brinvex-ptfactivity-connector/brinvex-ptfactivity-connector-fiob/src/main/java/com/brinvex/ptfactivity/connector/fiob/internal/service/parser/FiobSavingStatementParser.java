package com.brinvex.ptfactivity.connector.fiob.internal.service.parser;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.SavingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Statement;
import com.brinvex.java.validation.Assert;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class FiobSavingStatementParser {

    @SuppressWarnings("DataFlowIssue")
    public Statement.SavingTransStatement parseStatement(String statementContent) {

        ArrayList<SavingTransaction> trans = new ArrayList<>();
        String accountId = null;
        LocalDate periodFrom = null;
        LocalDate periodTo = null;
        try {
            XMLEventReader reader = Lazy.XML_INPUT_FACTORY.createXMLEventReader(new StringReader(statementContent));

            SavingTransaction.SavingTransactionBuilder tranBuilder = null;
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (xmlEvent.isStartElement()) {
                    StartElement startElement = xmlEvent.asStartElement();
                    String elementName = startElement.getName().getLocalPart();

                    switch (elementName) {
                        case "accountId":
                            xmlEvent = reader.nextEvent();
                            accountId = xmlEvent.asCharacters().getData();
                            reader.nextEvent();
                            break;
                        case "dateStart":
                            xmlEvent = reader.nextEvent();
                            periodFrom = parseDate(xmlEvent);
                            reader.nextEvent();
                            break;
                        case "dateEnd":
                            xmlEvent = reader.nextEvent();
                            periodTo = parseDate(xmlEvent);
                            reader.nextEvent();
                            break;
                        case "Transaction":
                            Assert.isTrue(tranBuilder == null);
                            tranBuilder = SavingTransaction.builder();
                            break;
                        case "column_22":
                            xmlEvent = reader.nextEvent();
                            tranBuilder.id(xmlEvent.asCharacters().getData());
                            reader.nextEvent();
                            break;
                        case "column_0":
                            xmlEvent = reader.nextEvent();
                            tranBuilder.date(parseDate(xmlEvent));
                            reader.nextEvent();
                            break;
                        case "column_1":
                            xmlEvent = reader.nextEvent();
                            tranBuilder.volume(new BigDecimal(xmlEvent.asCharacters().getData()));
                            reader.nextEvent();
                            break;
                        case "column_8":
                            xmlEvent = reader.nextEvent();
                            tranBuilder.type(xmlEvent.asCharacters().getData());
                            reader.nextEvent();
                            break;
                        case "column_14":
                            xmlEvent = reader.nextEvent();
                            tranBuilder.ccy(Currency.valueOf(xmlEvent.asCharacters().getData()));
                            reader.nextEvent();
                            break;
                    }
                } else if (xmlEvent.isEndElement()) {
                    EndElement endElement = xmlEvent.asEndElement();
                    String elementName = endElement.getName().getLocalPart();
                    if (elementName.equals("Transaction")) {
                        Assert.isTrue(tranBuilder != null);
                        trans.add(tranBuilder.build());
                        tranBuilder = null;
                    }
                }
            }
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return new Statement.SavingTransStatement(
                accountId,
                periodFrom,
                periodTo,
                trans
        );
    }

    private static LocalDate parseDate(XMLEvent xmlEvent) {
        return LocalDate.parse(xmlEvent.asCharacters().getData(), DateTimeFormatter.ISO_OFFSET_DATE);
    }

    private static class Lazy {
        private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    }
}
