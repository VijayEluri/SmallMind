package org.smallmind.nutsnbolts.spring.maven;

import java.util.Properties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionVisitor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;

public class ProfilePlaceholderConfigurer extends PropertyResourceConfigurer implements BeanFactoryAware, BeanNameAware {

   private BeanFactory beanFactory;
   private String beanName;
   private String profile;
   private boolean ignoreResourceNotFound = false;
   private boolean ignoreUnresolvableProperties = false;

   public void setBeanFactory (BeanFactory beanFactory) {

      this.beanFactory = beanFactory;
   }

   public void setBeanName (String beanName) {

      this.beanName = beanName;
   }

   public void setProfile (String profile) {

      this.profile = profile;
   }

   public void setIgnoreResourceNotFound (boolean ignoreResourceNotFound) {

      this.ignoreResourceNotFound = ignoreResourceNotFound;
   }

   public void setIgnoreUnresolvableProperties (boolean ignoreUnresolvableProperties) {

      this.ignoreUnresolvableProperties = ignoreUnresolvableProperties;
   }

   @Override
   protected void processProperties (ConfigurableListableBeanFactory beanFactoryToProcess, Properties properties)
      throws BeansException {

      ProfilePropertyStringValueResolver valueResolver;
      BeanDefinitionVisitor beanDefinitionVisitor;
      BeanDefinition beanDefinition;

      if ((valueResolver = new ProfilePropertyStringValueResolver(profile, ignoreResourceNotFound, ignoreUnresolvableProperties)).isActive()) {
         beanDefinitionVisitor = new BeanDefinitionVisitor(valueResolver);
         for (String beanName : beanFactoryToProcess.getBeanDefinitionNames()) {
            if ((!(beanName.equals(this.beanName)) && beanFactoryToProcess.equals(this.beanFactory))) {
               beanDefinition = beanFactoryToProcess.getBeanDefinition(beanName);
               try {
                  beanDefinitionVisitor.visitBeanDefinition(beanDefinition);
               }
               catch (BeanDefinitionStoreException beanDefinitionStoreException) {
                  throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, beanDefinitionStoreException.getMessage());
               }
            }
         }

         beanFactoryToProcess.resolveAliases(valueResolver);
      }
   }
}
