package org.smallmind.persistence.orm.aop;

import java.util.HashSet;
import org.smallmind.persistence.orm.ProxySession;

public class BoundarySet<T> extends HashSet<T> {

   private String[] dataSources;
   private boolean implicit;

   public BoundarySet (String dataSources[], boolean implicit) {

      super();

      this.dataSources = dataSources;
      this.implicit = implicit;
   }

   public boolean isImplicit () {

      return implicit && (dataSources.length == 0);
   }

   public boolean allows (ProxySession proxySession) {

      return allows(proxySession.getDataSource());
   }

   public boolean allows (String dataSource) {

      if (dataSources.length == 0) {
         return isImplicit() || (dataSource == null);
      }
      else if (isImplicit()) {
         throw new IllegalArgumentException("Boundary annotation (@NonTransaction or @Transactional) is marked as implicit, but explicitly lists data sources");
      }
      else if (dataSource != null) {
         for (String boundarySource : dataSources) {
            if (dataSource.equals(boundarySource)) {
               return true;
            }
         }
      }

      return false;
   }
}