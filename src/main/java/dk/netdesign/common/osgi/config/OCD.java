/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.netdesign.common.osgi.config;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 *
 * @author mnn
 */
public class OCD implements ObjectClassDefinition {

    private String description;
    private String name;
    // Represents the attributes of this object class
    private AttributeDefinition[] requiredADs = new AttributeDefinition[2];
    private String id;
    private File iconFile;

    protected OCD(String id, AttributeDefinition[] requiredADs) {
	this.id = id;
	this.requiredADs = requiredADs;
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions(int filter) {
	if (filter == ObjectClassDefinition.OPTIONAL) {
	    return null;
	}
	return requiredADs;
    }

    @Override
    public String getDescription() {
	return description;
    }

    @Override
    public InputStream getIcon(int iconDimension) throws IOException {
	BufferedImage img = null;
	img = ImageIO.read(iconFile);

	BufferedImage resizedImage = scaleImage(img, iconDimension, iconDimension, Color.DARK_GRAY);

	ByteArrayOutputStream os = new ByteArrayOutputStream();
	ImageIO.write(resizedImage, "jpg", os);
	return new ByteArrayInputStream(os.toByteArray());

    }

    @Override
    public String getID() {
	return id;
    }

    @Override
    public String getName() {
	return name;
    }

    protected void setName(String name) {
	this.name = name;
    }

    protected void setDescription(String description) {
	this.description = description;
    }

    protected void setIconFile(File iconFile) {
	this.iconFile = iconFile;
    }

    public BufferedImage scaleImage(BufferedImage img, int width, int height,
	    Color background) {
	int imgWidth = img.getWidth();
	int imgHeight = img.getHeight();
	if (imgWidth * height < imgHeight * width) {
	    width = imgWidth * height / imgHeight;
	} else {
	    height = imgHeight * width / imgWidth;
	}
	BufferedImage newImage = new BufferedImage(width, height,
		BufferedImage.TYPE_INT_RGB);
	Graphics2D g = newImage.createGraphics();
	try {
	    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
	    g.setBackground(background);
	    g.clearRect(0, 0, width, height);
	    g.drawImage(img, 0, 0, width, height, null);
	} finally {
	    g.dispose();
	}
	return newImage;
    }

    @Override
    public String toString() {
	ToStringBuilder builder = new ToStringBuilder(this);
	builder.append("id", id).append("name", name).append("description", description).append("iconFile", iconFile).append("ADs", requiredADs);
	return builder.toString();
    }
}
