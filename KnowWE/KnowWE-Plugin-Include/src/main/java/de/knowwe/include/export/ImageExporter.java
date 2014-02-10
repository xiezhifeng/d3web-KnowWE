/*
 * Copyright (C) 2014 denkbares GmbH
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package de.knowwe.include.export;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.MyXWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import de.d3web.strings.Strings;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.wikiConnector.WikiAttachment;
import de.knowwe.core.wikiConnector.WikiConnector;
import de.knowwe.include.export.DocumentBuilder.Style;
import de.knowwe.jspwiki.types.PluginType;

/**
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 07.02.2014
 */
public class ImageExporter implements Exporter<PluginType> {

	@Override
	public boolean canExport(Section<PluginType> section) {
		return Strings.startsWithIgnoreCase(section.getText(), "[{Image");
	}

	@Override
	public Class<PluginType> getSectionType() {
		return PluginType.class;
	}

	@Override
	public void export(Section<PluginType> section, DocumentBuilder manager) throws ExportException {
		try {
			String file = attr(section, "src");
			String path = section.getTitle() + "/" + file;
			WikiConnector connector = Environment.getInstance().getWikiConnector();
			WikiAttachment attachment = connector.getAttachment(path);

			InputStream stream = attachment.getInputStream();
			try {
				XWPFRun run = manager.getNewParagraph(Style.image).createRun();
				int width = intAttr(section, "width", 530);
				// TODO: keep aspect ratio
				int height = attr(section, "height") != null
						? intAttr(section, "height", 380)
						: (width * 380 / 530);
				MyXWPFRun.addPicture(
						run, stream, getFormat(path), file,
						Units.toEMU(width),
						Units.toEMU(height));
			}
			finally {
				stream.close();
			}

			String caption = attr(section, "caption");
			if (!Strings.isBlank(caption)) {
				XWPFRun run = manager.getNewParagraph(Style.caption).createRun();
				run.setText(caption);
			}
		}
		catch (IOException e) {
			throw new ExportException("could not load image", e);
		}
		catch (InvalidFormatException e) {
			throw new ExportException("image has invalid format", e);
		}
	}

	private int getFormat(String path) throws InvalidFormatException {
		path = path.toLowerCase();
		if (path.endsWith(".emf")) return XWPFDocument.PICTURE_TYPE_EMF;
		else if (path.endsWith(".wmf")) return XWPFDocument.PICTURE_TYPE_WMF;
		else if (path.endsWith(".pict")) return XWPFDocument.PICTURE_TYPE_PICT;
		else if (path.endsWith(".jpeg") || path.endsWith(".jpg")) {
			return XWPFDocument.PICTURE_TYPE_JPEG;
		}
		else if (path.endsWith(".png")) return XWPFDocument.PICTURE_TYPE_PNG;
		else if (path.endsWith(".dib")) return XWPFDocument.PICTURE_TYPE_DIB;
		else if (path.endsWith(".gif")) return XWPFDocument.PICTURE_TYPE_GIF;
		else if (path.endsWith(".tiff")) return XWPFDocument.PICTURE_TYPE_TIFF;
		else if (path.endsWith(".eps")) return XWPFDocument.PICTURE_TYPE_EPS;
		else if (path.endsWith(".bmp")) return XWPFDocument.PICTURE_TYPE_BMP;
		else if (path.endsWith(".wpg")) return XWPFDocument.PICTURE_TYPE_WPG;
		throw new InvalidFormatException("unrecognized format of " + path);
	}

	private int intAttr(Section<PluginType> section, String attribute, int defaultValue) {
		String text = Strings.trim(attr(section, attribute));
		if (Strings.isBlank(text)) return defaultValue;
		boolean percent = text.endsWith("%");
		text = text.replace("%", "").trim();
		float d = Float.valueOf(text);
		return Math.round(percent ? defaultValue * d : d);
	}

	private String attr(Section<PluginType> section, String attribute) {
		String text = section.getText();

		// single quotes
		Pattern pattern = Pattern.compile(
				Pattern.quote(attribute) + "\\s*=\\s*'([^']*)'",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(text);
		if (matcher.find()) return matcher.group(1);

		// double quotes
		pattern = Pattern.compile(
				Pattern.quote(attribute) + "\\s*=\\s*\"([^\"]*)\"",
				Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(text);
		if (matcher.find()) return matcher.group(1);

		// no quotes
		pattern = Pattern.compile(
				Pattern.quote(attribute) + "\\s*=\\s*([^\\s]*)",
				Pattern.CASE_INSENSITIVE);
		matcher = pattern.matcher(text);
		if (matcher.find()) return matcher.group(1);

		return null;
	}
}