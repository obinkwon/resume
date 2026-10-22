package resume.core.importer.extract;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import resume.core.importer.ResumeFileType;
import resume.core.importer.ResumeImportException;
import resume.core.importer.SafeZip;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한글(.hwpx) 글자 추출 — 추가 라이브러리 없음
 *  - hwpx = ZIP 안에 XML. 본문은 Contents/section0.xml, section1.xml …
 *  - 문단 hp:p / 글자 hp:t / 표 hp:tbl > hp:tr > hp:tc
 */
@Component
public class HwpxTextExtractor implements ResumeTextExtractor {

    private static final Pattern SECTION = Pattern.compile("Contents/section(\\d+)\\.xml");
    private static final String MANIFEST = "META-INF/manifest.xml";
    private static final int MAX_TABLE_DEPTH = 5;

    @Override
    public ResumeFileType supports() {
        return ResumeFileType.HWPX;
    }

    @Override
    public String extract(byte[] bytes) {
        SafeZip.Result zip = SafeZip.read(bytes, name -> SECTION.matcher(name).matches() || MANIFEST.equals(name));
        Map<String, byte[]> files = zip.files();

        byte[] manifest = files.get(MANIFEST);
        if (manifest != null && new String(manifest, StandardCharsets.UTF_8).contains("encryption-data")) {
            throw unreadable(null);
        }

        List<String> sections = new ArrayList<>(files.keySet().stream()
                .filter(n -> SECTION.matcher(n).matches())
                .toList());
        sections.sort(Comparator.comparingInt(HwpxTextExtractor::sectionNo));
        if (sections.isEmpty()) throw unreadable(null);

        try {
            DocumentBuilder builder = newSecureBuilder();
            StringBuilder out = new StringBuilder();
            for (String name : sections) {
                Document doc = builder.parse(new ByteArrayInputStream(files.get(name)));
                StringBuilder line = new StringBuilder();
                walk(doc.getDocumentElement(), out, line, 0);
                flush(line, out);
            }
            return out.toString();
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw unreadable(e);
        }
    }

    private static int sectionNo(String name) {
        Matcher m = SECTION.matcher(name);
        return m.matches() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    // ---------- XML 순회 ----------

    private void walk(Element el, StringBuilder out, StringBuilder line, int depth) {
        for (Node c = el.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() != Node.ELEMENT_NODE) continue;
            Element e = (Element) c;
            String name = localName(e);
            switch (name) {
                case "tbl" -> {
                    flush(line, out);
                    if (depth < MAX_TABLE_DEPTH) appendTable(e, out, depth + 1);
                }
                case "t" -> appendRunText(e, line);
                case "p" -> {
                    walk(e, out, line, depth);
                    flush(line, out);
                }
                default -> walk(e, out, line, depth);
            }
        }
    }

    /** hp:t 안의 글자 (탭·줄바꿈·특수 공백 요소 처리) */
    private void appendRunText(Element t, StringBuilder line) {
        for (Node c = t.getFirstChild(); c != null; c = c.getNextSibling()) {
            switch (c.getNodeType()) {
                case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> line.append(c.getNodeValue());
                case Node.ELEMENT_NODE -> {
                    switch (localName((Element) c)) {
                        case "tab" -> line.append('\t');
                        case "lineBreak" -> line.append('\n');
                        case "fwSpace", "nbSpace" -> line.append(' ');
                        default -> appendRunText((Element) c, line);
                    }
                }
                default -> {
                }
            }
        }
    }

    private void appendTable(Element tbl, StringBuilder out, int depth) {
        for (Element tr : children(tbl, "tr")) {
            List<String> cells = new ArrayList<>();
            for (Element tc : children(tr, "tc")) {
                StringBuilder cellOut = new StringBuilder();
                StringBuilder cellLine = new StringBuilder();
                walk(tc, cellOut, cellLine, depth);
                flush(cellLine, cellOut);
                cells.add(cellOut.toString());
            }
            TableText.appendRow(cells, out);
        }
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> list = new ArrayList<>();
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() == Node.ELEMENT_NODE && localName.equals(localName((Element) c))) {
                list.add((Element) c);
            }
        }
        return list;
    }

    private static String localName(Element e) {
        String n = e.getLocalName();
        if (n != null) return n;
        String tag = e.getTagName();
        int colon = tag.indexOf(':');
        return colon < 0 ? tag : tag.substring(colon + 1);
    }

    private static void flush(StringBuilder line, StringBuilder out) {
        if (!line.toString().isBlank()) out.append(line).append('\n');
        line.setLength(0);
    }

    // ---------- 안전한 XML 파서 (외부 엔티티·DTD 차단 = XXE 방지) ----------

    private static DocumentBuilder newSecureBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setXIncludeAware(false);
        f.setExpandEntityReferences(false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        DocumentBuilder b = f.newDocumentBuilder();
        b.setErrorHandler(new ErrorHandler() {       // 기본 핸들러는 콘솔에 [Fatal Error] 를 찍음
            @Override public void warning(SAXParseException e) { }
            @Override public void error(SAXParseException e) throws SAXException { throw e; }
            @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
        });
        return b;
    }

    private static ResumeImportException unreadable(Exception cause) {
        return new ResumeImportException(ResumeImportException.UNREADABLE,
                "한글 파일 내용을 읽지 못했어요. 암호가 걸려 있거나 배포용 문서라면 해제한 뒤 다시 저장해 올려 주세요.", cause);
    }
}
