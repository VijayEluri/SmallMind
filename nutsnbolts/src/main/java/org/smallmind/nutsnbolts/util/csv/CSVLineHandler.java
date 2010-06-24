package org.smallmind.nutsnbolts.util.csv;

public interface CSVLineHandler {

   public abstract void startDocument ();

   public abstract void handleFields (String[] fields)
      throws CSVParseException;

   public abstract void endDocument ();
}
