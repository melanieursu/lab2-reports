package ru.itmo.TolstovaUrsu.storage;

import org.w3c.dom.*;
import ru.itmo.TolstovaUrsu.domain.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.Instant;
import java.util.*;

public class XmlFileStorage {


    public void save(File file,
                     List<Report> reports,
                     List<ReportLine> lines) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("lab-data");
        doc.appendChild(root);

        Element reportsEl = doc.createElement("reports");
        for (Report r : reports) {
            reportsEl.appendChild(reportToXml(doc, r));
        }
        root.appendChild(reportsEl);

        Element linesEl = doc.createElement("reportLines");
        for (ReportLine l : lines) {
            linesEl.appendChild(lineToXml(doc, l));
        }
        root.appendChild(linesEl);

        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.INDENT, "yes");
        tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        tf.transform(new DOMSource(doc), new StreamResult(file));
    }

    private Element reportToXml(Document doc, Report r) {
        Element el = doc.createElement("report");
        appendText(doc, el, "id",             String.valueOf(r.getId()));
        appendText(doc, el, "name",           r.getName());
        appendText(doc, el, "sampleId",       String.valueOf(r.getSampleId()));
        appendText(doc, el, "experimentId",   String.valueOf(r.getExperimentId()));
        appendText(doc, el, "status",         r.getStatus().name());
        appendText(doc, el, "ownerUsername",  r.getOwnerUsername());
        appendText(doc, el, "signedBy",       r.getSignedBy() == null ? "" : r.getSignedBy());
        appendText(doc, el, "createdAt",      r.getCreatedAt().toString());
        appendText(doc, el, "updatedAt",      r.getUpdatedAt().toString());
        return el;
    }

    private Element lineToXml(Document doc, ReportLine l) {
        Element el = doc.createElement("reportLine");
        appendText(doc, el, "id",       String.valueOf(l.getId()));
        appendText(doc, el, "reportId", String.valueOf(l.getReportId()));
        appendText(doc, el, "param",    l.getParam().name());
        appendText(doc, el, "value",    String.valueOf(l.getValue()));
        appendText(doc, el, "unit",     l.getUnit());
        appendText(doc, el, "createdAt", l.getCreatedAt().toString());
        appendText(doc, el, "updatedAt", l.getUpdatedAt().toString());
        return el;
    }

    private void appendText(Document doc, Element parent, String tag, String value) {
        Element el = doc.createElement(tag);
        el.appendChild(doc.createTextNode(value == null ? "" : value));
        parent.appendChild(el);
    }


    public LoadResult load(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();

        List<Report>     reports = new ArrayList<>();
        List<ReportLine> lines   = new ArrayList<>();

        NodeList reportNodes = doc.getElementsByTagName("report");
        for (int i = 0; i < reportNodes.getLength(); i++) {
            Element el = (Element) reportNodes.item(i);

            long   id            = parseLong(el, "id");
            String name          = getText(el, "name");
            long   sampleId      = parseLong(el, "sampleId");
            long   experimentId  = parseLong(el, "experimentId");
            ReportStatus status  = ReportStatus.valueOf(getText(el, "status"));
            String owner         = getText(el, "ownerUsername");
            String signedBy      = getText(el, "signedBy");
            Instant createdAt    = Instant.parse(getText(el, "createdAt"));
            Instant updatedAt    = Instant.parse(getText(el, "updatedAt"));

            Report r = new Report(id, name, sampleId, experimentId, owner);
            r.setStatus(status);
            r.setCreatedAt(createdAt);
            r.setUpdatedAt(updatedAt);
            if (!signedBy.isBlank()) r.setSignedBy(signedBy);
            reports.add(r);
        }

        NodeList lineNodes = doc.getElementsByTagName("reportLine");
        for (int i = 0; i < lineNodes.getLength(); i++) {
            Element el = (Element) lineNodes.item(i);

            long   id       = parseLong(el, "id");
            long   reportId = parseLong(el, "reportId");
            MeasurementParam param = MeasurementParam.valueOf(getText(el, "param"));
            double value    = Double.parseDouble(getText(el, "value"));
            String unit     = getText(el, "unit");
            Instant createdAt = Instant.parse(getText(el, "createdAt"));
            Instant updatedAt = Instant.parse(getText(el, "updatedAt"));

            ReportLine line = new ReportLine(id, reportId, param, value, unit);
            line.setCreatedAt(createdAt);
            line.setUpdatedAt(updatedAt);
            lines.add(line);
        }

        return new LoadResult(reports, lines);
    }

    private String getText(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        if (nl.getLength() == 0) return "";
        return nl.item(0).getTextContent().trim();
    }

    private long parseLong(Element el, String tag) {
        return Long.parseLong(getText(el, tag));
    }

    public static class LoadResult {
        public final List<Report>     reports;
        public final List<ReportLine> lines;

        public LoadResult(List<Report> reports, List<ReportLine> lines) {
            this.reports = reports;
            this.lines   = lines;
        }
    }
}
