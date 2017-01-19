package com.github.i49.komorebi.epub;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.github.i49.komorebi.publication.Publication;
import com.github.i49.komorebi.publication.PublicationWriter;

public class Epub3Writer implements PublicationWriter {

	private static final String MIMETYPE = "application/epub+zip";
	private static final String DEFAULT_PACKAGE_DIR = "EPUB/";

	private final Transformer transformer;
	private final ZipOutputStream zstream;
	
	private final DocumentBuilder documentBuilder;
	
	private String packageDir = DEFAULT_PACKAGE_DIR;
	
	public Epub3Writer(OutputStream stream) throws Exception {
		this.documentBuilder = createDocumentBuilder();
		this.transformer = createTransformer();
		this.zstream = new ZipOutputStream(stream);
	}

	@Override
	public void write(Publication book) throws Exception {
		writeMimeType();
		writeContainerXml();
		writePackageXml(book);
	}

	@Override
	public void close() throws IOException {
		this.zstream.close();
	}
	
	private void writeMimeType() throws IOException {
		byte[] content = MIMETYPE.getBytes();
		writeUncompressedEntry("mimetype", content);
	}
	
	private void writeContainerXml() throws IOException, TransformerException {
		ContainerXmlBuilder builder = new ContainerXmlBuilder(documentBuilder); 
		Document doc = builder.build(packageDir);
		writeXmlEntry("META-INF/container.xml", doc);
	}

	private void writePackageXml(Publication publication) throws IOException, TransformerException {
		PackageXmlBuilder builder = new PackageXmlBuilder(documentBuilder); 
		Document doc = builder.build(publication.getMetadata());
		writeXmlEntry(this.packageDir + "package.opf", doc);
	}
	
	private void writeUncompressedEntry(String entryName, byte[] content) throws IOException {
		ZipEntry entry = new ZipEntry(entryName);
		CRC32 crc = new CRC32();
		crc.update(content);
		entry.setCrc(crc.getValue());
		entry.setSize(content.length);
		try (OutputStream stream = newEntryStream(entry, false)) {
			stream.write(content);	
		}
	}

	private void writeXmlEntry(String entryName, Document doc) throws IOException, TransformerException {
		ZipEntry entry = new ZipEntry(entryName);
		try (OutputStream stream = newEntryStream(entry, true)) {
			writeXmlDeclaration(stream);	
			transform(doc, stream);
		}
	}
	
	private OutputStream newEntryStream(ZipEntry entry, boolean compressing) throws IOException {
		final int method = compressing ? ZipEntry.DEFLATED : ZipEntry.STORED;
		entry.setMethod(method);
		this.zstream.putNextEntry(entry);
		return new ZipEntryOutputStream(this.zstream);
	}

	private void writeXmlDeclaration(OutputStream stream) throws IOException {
		writeText(stream, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
	}
	
	private void writeXmlDocType(OutputStream stream) throws IOException {
		writeText(stream, "<!DOCTYPE html>\n");
	}
	
	private void writeText(OutputStream stream, String text) throws IOException {
		stream.write(text.getBytes());
	}
	
	private void transform(Document doc, OutputStream stream) throws TransformerException {
		DOMSource source = new DOMSource(doc);
		StreamResult target = new StreamResult(stream);
		this.transformer.transform(source, target);
	}
	
	private static DocumentBuilder createDocumentBuilder() throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		return factory.newDocumentBuilder();
	}
	
	private static Transformer createTransformer() throws TransformerConfigurationException {
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer t = factory.newTransformer();
		t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		t.setOutputProperty(OutputKeys.INDENT, "yes");
		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		t.setOutputProperty(OutputKeys.METHOD, "xml");
		return t;
	}
}