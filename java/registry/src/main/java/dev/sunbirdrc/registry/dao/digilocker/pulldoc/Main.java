package dev.sunbirdrc.registry.dao.digilocker.pulldoc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
public class Main {
    public static void main(String[] args) {

        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
                "<PullDocRequest xmlns:ns2=\"http://tempuri.org/\" ver=\"1.0\" ts=\"YYYY-MMDDThh:mm:ss+/-nn:nn\" txn=\"1234567\" orgId=\"upsmfac.org\" format=\"pdf\">\n" +
                " <DocDetails>\n" +
                " <URI>in.gov.kerala.edistrict-INCER-123456</URI>\n" +
                " <DigiLockerId>123e4567-e89b-12d3-a456-426655440000</DigiLockerId>\n" +
                " </DocDetails>\n" +
                "</PullDocRequest>";

        PullDocRequest request = processPullDocRequest(xml);
        System.out.println(request.getTxn());

    }

    public static PullDocRequest processPullDocRequest(String xml) {
        PullDocRequest request = new PullDocRequest();
        DocDetailsType docDetails = new DocDetailsType();
        try {
            // Create a DocumentBuilderFactory and DocumentBuilder to parse the XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the XML string into a Document object
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));
            Element rootElement = document.getDocumentElement();
            Element docDetailsElement = (Element) rootElement.getElementsByTagName("DocDetails").item(0);
            // Get the values of URI and DigiLockerId elements
            String uri = docDetailsElement.getElementsByTagName("URI").item(0).getTextContent();
            String digiLockerId = docDetailsElement.getElementsByTagName("DigiLockerId").item(0).getTextContent();
            docDetails.setUri(uri);
            docDetails.setDigiLockerId(digiLockerId);
            request.setDocDetails(docDetails);
            // Print the values
            System.out.println("URI: " + uri);
            System.out.println("DigiLockerId: " + digiLockerId);
            NamedNodeMap attributes = rootElement.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                String attributeName = attributes.item(i).getNodeName();
                String attributeValue = attributes.item(i).getNodeValue();
                switch (attributeName.toLowerCase()) {
                    case "txn":
                        request.setTxn(attributeValue);
                        break;
                    case "orgid":
                        request.setOrgId(attributeValue);
                        break;
                    case "ts":
                        request.setTs(attributeValue);
                        break;
                    case "format":
                        request.setFormat(attributeValue);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return request;
    }
}
