package com.jpo.generator.maker;

import com.jpo.generator.maker.util.GeneratorUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
public class MakerApplication {
    public static final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws IOException, URISyntaxException, ParserConfigurationException, SAXException, ParseException {
        String urlStr = "https://apis.data.go.kr/1160100/service/GetDiscInfoService_V2/getAsseTranPutBackOptiDiscInfo_V2?resultType=xml&serviceKey=3NKSD4pMiU1dAnSi9YfhhEcZyp1uL2gFUk8wq7Iy3Nex4lGzhRXbYlaKnxUDb2P5IxztSaDkmL14JHAbRONlDw%3D%3D&";

        String temp = urlStr.split("\\?")[0];
        int lastSlashIndex = temp.lastIndexOf("/");
        String className = temp.substring(lastSlashIndex + 1).replace("get", "").replace("_V2", "");
        String serviceName = temp.substring(temp.lastIndexOf("service/") + 8, lastSlashIndex).replace("Get", "").replace("_V2", "");
        System.out.println("className: " + className + ", serviceName: " + serviceName);

        URL url = (new URL(urlStr)).toURI().toURL();
        Map<String, String> map = GeneratorUtil.getQueryMap(url.getQuery());
        String resultType = map.get("resultType");
        System.out.println("resultType: " + resultType);

        if ("xml".equals(resultType)) {
            xmlGenerator(urlStr, className, serviceName);
        } else {
            jsonGenerator(urlStr, className, serviceName);
        }
    }

    /**
     * json 변환 처리
     * @param urlStr String
     * @param className String
     * @param serviceName String
     * @throws IOException IOException
     * @throws org.json.simple.parser.ParseException org.json.simple.parser.ParseException
     * @throws URISyntaxException URISyntaxException
     */
    private static void jsonGenerator(String urlStr, String className, String serviceName) throws IOException, org.json.simple.parser.ParseException, URISyntaxException {
        URL url = (new URL(urlStr)).toURI().toURL();
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("CONTENT-TYPE", "application/json");
        con.setDoOutput(true);
        con.setConnectTimeout(10000);
        con.setReadTimeout(5000);

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputline;
            while ((inputline = in.readLine()) != null) {
                response.append(inputline.trim());
            }
            processResponseJson(response.toString(), className, serviceName);
        } catch (IOException | org.json.simple.parser.ParseException ex) {
            throw ex;
        } finally {
            con.disconnect();
        }
    }

    /**
     * xml 변환 처리
     * @param urlStr String
     * @param className String
     * @param serviceName String
     * @throws IOException IOException
     * @throws ParserConfigurationException ParserConfigurationException
     * @throws SAXException SAXException
     * @throws URISyntaxException URISyntaxException
     */
    private static void xmlGenerator(String urlStr, String className, String serviceName) throws IOException, ParserConfigurationException, SAXException, URISyntaxException {
        //URL url = new URL(urlStr);
        URL url = (new URL(urlStr)).toURI().toURL();
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("CONTENT-TYPE", "text/xml");
        con.setDoOutput(true);
        con.setConnectTimeout(10000);
        con.setReadTimeout(5000);

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            String inputline;
            while ((inputline = in.readLine()) != null) {
                response.append(inputline.trim());
            }
            processResponseXML(response.toString(), className, serviceName);
        } catch (IOException ex) {
            throw ex;
        } finally {
            con.disconnect();
        }
    }

    /**
     * json response 처리
     * @param responseBody String
     * @param className String
     * @param serviceName String
     * @throws org.json.simple.parser.ParseException org.json.simple.parser.ParseException
     * @throws IOException IOException
     */
    private static void processResponseJson(String responseBody, String className, String serviceName) throws org.json.simple.parser.ParseException, IOException {
        // System.out.println("responseBody: " + responseBody);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(responseBody);

        JSONObject response = (JSONObject) jsonObject.get("response");
        JSONObject body = (JSONObject) response.get("body");

        int totalCount = Integer.parseInt(body.get("totalCount").toString());
        // System.out.println("totalCount: " + totalCount);

        JSONObject items = (JSONObject) body.get("items");
        JSONArray item = (JSONArray) items.get("item");

        JSONObject field = (JSONObject) item.getFirst();
        // System.out.println("field: " + field);

        StringBuilder classContent = new StringBuilder();
        GeneratorUtil.defaultEntityStringBuilder(className, classContent);

        int n = 0;
        for (Object key : field.keySet()) {
            // System.out.println("key: " + key + ", value: " + field.get(key));
            String fieldName = key.toString();
            String nValue = field.get(key).toString();
            if (n < 1) {
                String fieldType = GeneratorUtil.determineFieldType(fieldName, nValue == null ? "" : nValue);
                classContent.append("    @Id\n");
                classContent.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
                classContent.append("    private Long id;\n\n");
            }

            String fieldType = GeneratorUtil.determineFieldType(fieldName, nValue == null ? "" : nValue);
            classContent.append("    @Column(name = \"").append(fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase()).append("\")\n");
            classContent.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
            n++;
        }
        classContent.append("}\n");
        GeneratorUtil.writeFile(className + "Entity.java", classContent.toString());

        StringBuilder repositoryContent = new StringBuilder();
        GeneratorUtil.defaultRepositoryStringBuilder(className, repositoryContent);

        GeneratorUtil.writeFile(className + "Repository.java", repositoryContent.toString());

        StringBuilder serviceContent = new StringBuilder();
        String repositoryName = GeneratorUtil.toCamelCase(className.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());

        serviceContent.append("package com.herojoon.jpaproject.service;\n\n");
        serviceContent.append("import com.herojoon.jpaproject.entity.").append(className + "Entity").append(";\n");
        serviceContent.append("import com.herojoon.jpaproject.repository.").append(className).append("Repository;\n");
        serviceContent.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        serviceContent.append("import org.springframework.stereotype.Service;\n\n");
        serviceContent.append("@Service\n");
        serviceContent.append("public class ").append(serviceName).append(" {\n");
        serviceContent.append("    @Autowired\n");
        serviceContent.append("    private ").append(className).append("Repository ").append(repositoryName).append("Repository;\n");
        serviceContent.append("    private void ").append(className).append("ProcessResponse(String responseBody) throws ParserConfigurationException, SAXException, IOException {\n");
        serviceContent.append("        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();\n");
        serviceContent.append("        DocumentBuilder builder = dbFactory.newDocumentBuilder();\n");
        serviceContent.append("        Document document = dBuilder.parse(new InputSource(new StringReader(responseBody)));\n");
        serviceContent.append("        document.getDocumentElement().normalize();\n");
        serviceContent.append("        NodeList childList = doc.getElementsByTagName(\"item\");\n");
        serviceContent.append("        for (int i = 0; i < childList.getLength(); i++) {\n");
        serviceContent.append("            Node item = childList.item(i);\n");
        serviceContent.append("            if (item.getNodeType() == Node.ELEMENT_NODE) {\n");
        serviceContent.append("                Element element = (Element) item;\n");
        serviceContent.append("                ").append(repositoryName).append("Repository.save(").append(className).append("Entity.builder()\n");

        for (Object key : field.keySet()) {
            String fieldName = key.toString();
            String nValue = field.get(key).toString();
            switch (GeneratorUtil.determineFieldType(fieldName, nValue == null ? "" : nValue)) {
                case "String":
                    serviceContent.append("                    .").append(fieldName).append("(BatchUtil.getTagValue(\"").append(fieldName).append("\", element))\n");
                    break;
                case "int":
                    serviceContent.append("                    .").append(fieldName).append("(Integer.parseInt(Objects.requireNonNull(BatchUtil.getTagValue(\"").append(fieldName).append("\", element))))\n");
                    break;
                case "double":
                    serviceContent.append("                    .").append(fieldName).append("(Double.parseDouble(Objects.requireNonNull(BatchUtil.getTagValue(\"").append(fieldName).append("\", element))))\n");
                    break;
            }
        }

        serviceContent.append("                    .build());\n");
        serviceContent.append("            }\n");
        serviceContent.append("        }\n");
        serviceContent.append("    }\n");
        serviceContent.append("}\n");
        GeneratorUtil.writeFile(serviceName + ".java", serviceContent.toString());
    }

    /**
     * xml response 처리
     * @param responseBody String
     * @param className String
     * @param serviceName  String
     * @throws ParserConfigurationException ParserConfigurationException
     * @throws IOException IOException
     * @throws SAXException SAXException
     */
    private static void processResponseXML(String responseBody, String className, String serviceName) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new InputSource(new StringReader(responseBody)));
        doc.getDocumentElement().normalize();

        StringBuilder classContent = new StringBuilder();
        GeneratorUtil.defaultEntityStringBuilder(className, classContent);

        NodeList childList = doc.getElementsByTagName("item");
        int n = 0;
        for (int i = 0; i < 1; i++) {
            Node item = childList.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) item;
                NodeList itemChildren = element.getChildNodes();
                for (int j = 0; j < itemChildren.getLength(); j++) {
                    Node child = itemChildren.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String fieldName = child.getNodeName();
                        Element element1 = (Element) item;
                        NodeList nlList = element1.getElementsByTagName(fieldName).item(0).getChildNodes();
                        Node nValue = nlList.item(0);

                        if (n < 1) {
                            String fieldType = GeneratorUtil.determineFieldType(fieldName, nValue == null ? "" : nValue.getNodeValue());
                            classContent.append("    @Id\n");
                            classContent.append("    @GeneratedValue(strategy = GenerationType.IDENTITY)\n");
                            classContent.append("    private Long id;\n\n");
                        }

                        String fieldType = GeneratorUtil.determineFieldType(fieldName, nValue == null ? "" : nValue.getNodeValue());
                        classContent.append("    @Column(name = \"").append(fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase()).append("\")\n");
                        classContent.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n\n");
                        n++;
                    }
                }
            }
        }
        classContent.append("}\n");
        GeneratorUtil.writeFile(className + "Entity.java", classContent.toString());

        StringBuilder repositoryContent = new StringBuilder();
        GeneratorUtil.defaultRepositoryStringBuilder(className, repositoryContent);

        GeneratorUtil.writeFile(className + "Repository.java", repositoryContent.toString());

        StringBuilder serviceContent = new StringBuilder();
        String repositoryName = GeneratorUtil.toCamelCase(className.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase());

        serviceContent.append("package com.herojoon.jpaproject.service;\n\n");
        serviceContent.append("import com.herojoon.jpaproject.entity.").append(className + "Entity").append(";\n");
        serviceContent.append("import com.herojoon.jpaproject.repository.").append(className).append("Repository;\n");
        serviceContent.append("import org.springframework.beans.factory.annotation.Autowired;\n");
        serviceContent.append("import org.springframework.stereotype.Service;\n\n");
        serviceContent.append("@Service\n");
        serviceContent.append("public class ").append(serviceName).append(" {\n");
        serviceContent.append("    @Autowired\n");
        serviceContent.append("    private ").append(className).append("Repository ").append(repositoryName).append("Repository;\n");
        serviceContent.append("    private void ").append(className).append("ProcessResponse(String responseBody) throws ParserConfigurationException, SAXException, IOException {\n");
        serviceContent.append("        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();\n");
        serviceContent.append("        DocumentBuilder builder = factory.newDocumentBuilder();\n");
        serviceContent.append("        Document document = builder.parse(new InputSource(new StringReader(responseBody)));\n");
        serviceContent.append("        document.getDocumentElement().normalize();\n");
        serviceContent.append("        NodeList childList = document.getElementsByTagName(\"item\");\n\n");

        serviceContent.append("        for (int i = 0; i < childList.getLength(); i++) {\n");
        serviceContent.append("            Node item = childList.item(i);\n");
        serviceContent.append("            if (item.getNodeType() == Node.ELEMENT_NODE) {\n");
        serviceContent.append("                Element element = (Element) item;\n");
        serviceContent.append("                ").append(repositoryName).append("Repository.save(").append(className).append("Entity.builder()\n");

        for (int i = 0; i < 1; i++) {
            Node item = childList.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) item;
                NodeList itemChildren = element.getChildNodes();
                for (int j = 0; j < itemChildren.getLength(); j++) {
                    Node child = itemChildren.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE) {
                        String fieldName = child.getNodeName();
                        Element element1 = (Element) item;
                        NodeList nlList = element1.getElementsByTagName(fieldName).item(0).getChildNodes();
                        Node nValue = nlList.item(0);
                        switch (GeneratorUtil.determineFieldType(fieldName, nValue == null ? "" : nValue.getNodeValue())) {
                            case "String":
                                serviceContent.append("                    .").append(fieldName).append("(BatchUtil.getTagValue(\"").append(fieldName).append("\", element))\n");
                                break;
                            case "int":
                                serviceContent.append("                    .").append(fieldName).append("(Integer.parseInt(Objects.requireNonNull(BatchUtil.getTagValue(\"").append(fieldName).append("\", element))))\n");
                                break;
                            case "double":
                                serviceContent.append("                    .").append(fieldName).append("(Double.parseDouble(Objects.requireNonNull(BatchUtil.getTagValue(\"").append(fieldName).append("\", element))))\n");
                                break;
                        }
                    }
                }
            }
        }

        serviceContent.append("                    .build());\n");
        serviceContent.append("            }\n");
        serviceContent.append("        }\n");
        serviceContent.append("    }\n");
        serviceContent.append("}\n");
        GeneratorUtil.writeFile(serviceName + ".java", serviceContent.toString());

        System.out.println("Entity class generated successfully!");
    }
}