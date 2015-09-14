package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WiredService;
import org.smallmind.phalanx.worker.WorkManager;
import org.smallmind.phalanx.worker.WorkerFactory;

public class RabbitMQResponseTransport extends WorkManager<InvocationWorker, RabbitMQMessage> implements MetricConfigurationProvider, WorkerFactory<InvocationWorker, RabbitMQMessage>, ResponseTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final SignalCodec signalCodec;
  private final ConcurrentLinkedQueue<ResponseMessageRouter> responseQueue;
  private final ResponseMessageRouter[] responseMessageRouters;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  public RabbitMQResponseTransport (MetricConfiguration metricConfiguration, RabbitMQConnector rabbitMQConnector, NameConfiguration nameConfiguration, Class<InvocationWorker> workerClass, SignalCodec signalCodec, String serviceGroup, int clusterSize, int concurrencyLimit, int timeoutSeconds)
    throws IOException, InterruptedException {

    super(metricConfiguration, workerClass, concurrencyLimit);

    int routerIndex = 0;

    this.signalCodec = signalCodec;

    responseMessageRouters = new ResponseMessageRouter[clusterSize];
    for (int index = 0; index < responseMessageRouters.length; index++) {
      responseMessageRouters[index] = new ResponseMessageRouter(rabbitMQConnector, nameConfiguration, this, signalCodec, serviceGroup, instanceId, index, timeoutSeconds * 3);
      responseMessageRouters[index].initialize();
    }

    responseQueue = new ConcurrentLinkedQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      responseQueue.add(responseMessageRouters[routerIndex]);
      if (++routerIndex == responseMessageRouters.length) {
        routerIndex = 0;
      }
    }

    startUp(this);
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService) throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public InvocationWorker createWorker (MetricConfiguration metricConfiguration, TransferQueue<RabbitMQMessage> transferQueue) {

    return new InvocationWorker(metricConfiguration, transferQueue, this, invocationCircuit, signalCodec);
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result) throws Throwable {

    ResponseMessageRouter responseMessageRouter;

    if ((responseMessageRouter = responseQueue.poll()) == null) {
      throw new TransportException("Unable to take a ResponseMessageRouter, which should never happen - please contact your system administrator");
    }

    responseMessageRouter.publish(callerId, correlationId, error, nativeType, result);
    responseQueue.add(responseMessageRouter);
  }

  @Override
  public void close () throws Exception {

    if (closed.compareAndSet(false, true)) {
      for (ResponseMessageRouter responseMessageRouter : responseMessageRouters) {
        responseMessageRouter.close();
      }
    }
  }
}
