package org.smallmind.web.jersey.spring;

import java.util.Set;
import java.util.function.Supplier;
import javax.servlet.ServletContext;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.internal.inject.Binding;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.jvnet.hk2.spring.bridge.api.SpringBridge;
import org.jvnet.hk2.spring.bridge.api.SpringIntoHK2Bridge;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.aop.framework.Advised;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

public class SpringComponentProvider implements ComponentProvider {

  private volatile InjectionManager injectionManager;
  private volatile ApplicationContext applicationContext;

  @Override
  public void initialize (InjectionManager injectionManager) {

    this.injectionManager = injectionManager;

    ServletContext sc = injectionManager.getInstance(ServletContext.class);

    LoggerManager.getLogger(SpringComponentProvider.class).info("Searching for Spring application context on Thread(%s)", Thread.currentThread().getName());
    if ((applicationContext = ExposedApplicationContext.getApplicationContext()) == null) {
      throw new SpringHK2IntegrationException("Spring application context has not been created prior to HK2 application initialization");
    }

    // initialize HK2 spring-bridge
    ImmediateHk2InjectionManager hk2InjectionManager = (ImmediateHk2InjectionManager)injectionManager;
    SpringBridge.getSpringBridge().initializeSpringBridge(hk2InjectionManager.getServiceLocator());
    SpringIntoHK2Bridge springBridge = injectionManager.getInstance(SpringIntoHK2Bridge.class);
    springBridge.bridgeSpringBeanFactory(applicationContext);

    injectionManager.register(Bindings.injectionResolver(new AutowiredInjectResolver(applicationContext)));
    injectionManager.register(Bindings.service(applicationContext).to(ApplicationContext.class).named("SpringContext"));
  }

  // detect JAX-RS classes that are also Spring @Components.
  // register these with HK2 ServiceLocator to manage their lifecycle using Spring.
  @Override
  public boolean bind (Class<?> component, Set<Class<?>> providerContracts) {

    if (applicationContext == null) {
      return false;
    }

    if (AnnotationUtils.findAnnotation(component, Component.class) == null) {

      return false;
    } else {

      String[] beanNames = applicationContext.getBeanNamesForType(component);

      if (beanNames == null || beanNames.length != 1) {

        return false;
      } else {

        String beanName = beanNames[0];

        Binding binding = Bindings.supplier(new SpringManagedBeanFactory(applicationContext, injectionManager, beanName)).to(component).to(providerContracts);
        injectionManager.register(binding);

        return true;
      }
    }
  }

  @Override
  public void done () {

  }

  private static class SpringManagedBeanFactory implements Supplier {

    private final ApplicationContext ctx;
    private final InjectionManager injectionManager;
    private final String beanName;

    private SpringManagedBeanFactory (ApplicationContext ctx, InjectionManager injectionManager, String beanName) {

      this.ctx = ctx;
      this.injectionManager = injectionManager;
      this.beanName = beanName;
    }

    @Override
    public Object get () {

      Object bean = ctx.getBean(beanName);
      if (bean instanceof Advised) {
        try {
          // Unwrap the bean and inject the values inside of it
          Object localBean = ((Advised)bean).getTargetSource().getTarget();
          injectionManager.inject(localBean);
        } catch (Exception e) {
          // Ignore and let the injection happen as it normally would.
          injectionManager.inject(bean);
        }
      } else {
        injectionManager.inject(bean);
      }
      return bean;
    }
  }
}
