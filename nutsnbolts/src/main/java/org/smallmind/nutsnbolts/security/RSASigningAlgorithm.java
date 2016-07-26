/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.security;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import org.smallmind.nutsnbolts.http.Base64Codec;

public enum RSASigningAlgorithm implements SecurityAlgorithm, SigningAlgorithm {

  SHA_256_WITH_RSA("SHA256withRSA");

  private String algorithmName;

  RSASigningAlgorithm (String algorithmName) {

    this.algorithmName = algorithmName;
  }

  public String getAlgorithmName () {

    return algorithmName;
  }

  public Key generateKey (AsymmetricKeyType keyType, byte[] secret)
    throws NoSuchAlgorithmException, InvalidKeySpecException {

    return keyType.generateKey(secret);
  }

  @Override
  public byte[] sign (Key key, byte[] data)
    throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    return EncryptionUtility.sign(this, (PrivateKey)key, data);
  }

  @Override
  public boolean verify (Key key, String[] parts)
    throws IOException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

    return EncryptionUtility.verify(this, (PublicKey)key, (parts[0] + "." + parts[1]).getBytes(), Base64Codec.decode(parts[2]));
  }
}
