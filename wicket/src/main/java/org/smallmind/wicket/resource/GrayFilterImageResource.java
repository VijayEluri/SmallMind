/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.wicket.resource;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.apache.wicket.markup.html.image.resource.RenderedDynamicImageResource;
import org.smallmind.swing.SmallMindGrayFilter;

public class GrayFilterImageResource extends RenderedDynamicImageResource {

  Image grayImage;

  public GrayFilterImageResource (URL imageUrl, ImageType imageType)
    throws IOException {

    this(ImageIO.read(imageUrl), imageType);
  }

  private GrayFilterImageResource (BufferedImage image, ImageType imageType) {

    super(image.getWidth(), image.getHeight(), imageType.getFormat());

    setType(BufferedImage.TYPE_INT_ARGB);
    grayImage = SmallMindGrayFilter.createDisabledImage(image);
  }

  @Override
  protected boolean render (Graphics2D g2) {

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0F));
    g2.drawImage(grayImage, 0, 0, getWidth(), getHeight(), null);

    return true;
  }
}
