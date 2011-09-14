/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.nutsnbolts.email;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.smallmind.nutsnbolts.security.EncryptionUtilities;
import org.smallmind.nutsnbolts.security.HashAlgorithm;

public class Postman {

  private final HashMap<MD5Key, Template> templateMap = new HashMap<MD5Key, Template>();

  private Session session;
  private Configuration freemarkerConf;

  public Postman () {

  }

  public Postman (String host, int port) {

    this(host, port, new Authentication(AuthType.NONE), false);
  }

  public Postman (String host, int port, Authentication authentication) {

    this(host, port, authentication, false);
  }

  public Postman (String host, int port, boolean secure) {

    this(host, port, new Authentication(AuthType.NONE), secure);
  }

  public Postman (String host, int port, Authentication authentication, boolean secure) {

    session = (!secure) ? Protocol.SMTP.getSession(host, port, authentication) : Protocol.SMTPS.getSession(host, port, authentication);
    freemarkerConf = new Configuration();
    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
  }

  public void send (Mail mail)
    throws MailDeliveryException {

    send(mail, null);
  }

  public void send (Mail mail, HashMap<String, Object> interpolationMap)
    throws MailDeliveryException {

    MimeMessage message = new MimeMessage(session);
    Multipart multipart = new MimeMultipart();

    try {
      message.setFrom(new InternetAddress(mail.getFrom().trim()));

      if (mail.getReplyTo() != null) {
        message.setReplyTo(new Address[] {new InternetAddress(mail.getReplyTo())});
      }
      if (mail.getTo() != null) {
        addRecipients(message, Message.RecipientType.TO, mail.getTo());
      }
      if (mail.getCc() != null) {
        addRecipients(message, Message.RecipientType.CC, mail.getCc());
      }
      if (mail.getBcc() != null) {
        addRecipients(message, Message.RecipientType.BCC, mail.getBcc());
      }

      message.setSentDate(new Date());

      if (mail.getSubject() != null) {
        message.setSubject(mail.getSubject());
      }

      if (mail.getBodyReader() != null) {

        StringBuilder bodyBuilder;
        char[] buffer;
        int charsRead;

        buffer = new char[256];
        bodyBuilder = new StringBuilder();
        while ((charsRead = mail.getBodyReader().read(buffer)) >= 0) {
          bodyBuilder.append(buffer, 0, charsRead);
        }
        mail.getBodyReader().close();

        MimeBodyPart textPart = new MimeBodyPart();

        if (interpolationMap == null) {
          textPart.setText(bodyBuilder.toString());
        }
        else {

          Template template;
          StringWriter templateWriter;
          MD5Key md5Key = new MD5Key(EncryptionUtilities.hash(HashAlgorithm.MD5, bodyBuilder.toString()));

          synchronized (templateMap) {
            if ((template = templateMap.get(md5Key)) == null) {
              templateMap.put(md5Key, template = new Template(new String(md5Key.getMd5Hash()), new StringReader(bodyBuilder.toString()), freemarkerConf));
            }
          }

          template.process(interpolationMap, templateWriter = new StringWriter());
          textPart.setText(templateWriter.toString());
        }

        multipart.addBodyPart(textPart);
      }

      if ((mail.getAttachments() != null) && (mail.getAttachments().length > 0)) {
        for (File attachment : mail.getAttachments()) {

          MimeBodyPart filePart = new MimeBodyPart();
          FileDataSource dataSource = new FileDataSource(attachment);

          filePart.setDataHandler(new DataHandler(dataSource));
          filePart.setFileName(dataSource.getName());
          multipart.addBodyPart(filePart);
        }
      }

      message.setContent(multipart);
      Transport.send(message);
    }
    catch (Exception exception) {
      throw new MailDeliveryException(exception);
    }
  }

  private void addRecipients (Message message, Message.RecipientType type, String addresses)
    throws MessagingException {

    for (String address : addresses.split(",")) {
      message.addRecipient(type, new InternetAddress(address.trim()));
    }
  }

  private class MD5Key {

    private byte[] md5Hash;

    public MD5Key (byte[] md5Hash) {

      this.md5Hash = md5Hash;
    }

    public byte[] getMd5Hash () {

      return md5Hash;
    }

    @Override
    public int hashCode () {

      return Arrays.hashCode(md5Hash);
    }

    @Override
    public boolean equals (Object obj) {

      return (obj instanceof MD5Key) && Arrays.equals(md5Hash, ((MD5Key)obj).getMd5Hash());
    }
  }
}
