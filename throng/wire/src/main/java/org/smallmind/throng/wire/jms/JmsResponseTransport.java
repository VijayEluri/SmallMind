package org.smallmind.throng.wire.jms;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import org.smallmind.throng.wire.MetricType;
import org.smallmind.throng.wire.ResponseTransport;
import org.smallmind.throng.wire.ResultSignal;
import org.smallmind.throng.wire.ServiceDefinitionException;
import org.smallmind.throng.wire.SignalCodec;
import org.smallmind.throng.wire.TransportException;
import org.smallmind.throng.wire.WireInvocationCircuit;
import org.smallmind.throng.wire.WireProperty;
import org.smallmind.throng.wire.WiredService;
import org.smallmind.throng.worker.WorkManager;
import org.smallmind.throng.worker.WorkerFactory;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.nutsnbolts.util.SnowflakeId;

public class JmsResponseTransport extends WorkManager<InvocationWorker, Message> implements MetricConfigurationProvider, WorkerFactory<InvocationWorker, Message>, ResponseTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final SignalCodec signalCodec;
  private final ConcurrentLinkedQueue<TopicOperator> responseQueue;
  private final RequestListener[] talkRequestListeners;
  private final RequestListener[] whisperRequestListeners;
  private final ConnectionManager[] responseConnectionManagers;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();
  private final int maximumMessageLength;

  public JmsResponseTransport (MetricConfiguration metricConfiguration, RoutingFactories routingFactories, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, int maximumMessageLength)
    throws InterruptedException, JMSException, TransportException {

    super(metricConfiguration, InvocationWorker.class, concurrencyLimit);

    int topicIndex = 0;

    this.signalCodec = signalCodec;
    this.maximumMessageLength = maximumMessageLength;

    talkRequestListeners = new RequestListener[clusterSize];
    for (int index = 0; index < talkRequestListeners.length; index++) {
      talkRequestListeners[index] = new RequestListener(this, new ConnectionManager(routingFactories.getRequestQueueFactory(), messagePolicy, reconnectionPolicy), routingFactories.getRequestQueueFactory().getDestination(), null);
    }
    whisperRequestListeners = new RequestListener[clusterSize];
    for (int index = 0; index < whisperRequestListeners.length; index++) {
      whisperRequestListeners[index] = new RequestListener(this, new ConnectionManager(routingFactories.getRequestQueueFactory(), messagePolicy, reconnectionPolicy), routingFactories.getRequestTopicFactory().getDestination(), instanceId);
    }

    responseConnectionManagers = new ConnectionManager[clusterSize];
    for (int index = 0; index < responseConnectionManagers.length; index++) {
      responseConnectionManagers[index] = new ConnectionManager(routingFactories.getResponseTopicFactory(), messagePolicy, reconnectionPolicy);
    }

    responseQueue = new ConcurrentLinkedQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      responseQueue.add(new TopicOperator(responseConnectionManagers[topicIndex], (Topic)routingFactories.getResponseTopicFactory().getDestination()));
      if (++topicIndex == responseConnectionManagers.length) {
        topicIndex = 0;
      }
    }

    startUp(this);
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public void register (Class<?> serviceInterface, WiredService targetService)
    throws NoSuchMethodException, ServiceDefinitionException {

    invocationCircuit.register(serviceInterface, targetService);
  }

  @Override
  public InvocationWorker createWorker (MetricConfiguration metricConfiguration, TransferQueue<Message> transferQueue) {

    return new InvocationWorker(metricConfiguration, transferQueue, this, invocationCircuit, signalCodec, maximumMessageLength);
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Exception {

    TopicOperator topicOperator;

    if ((topicOperator = responseQueue.poll()) == null) {
      throw new TransportException("Unable to take a TopicOperator, which should never happen - please contact your system administrator");
    }

    topicOperator.send(constructMessage(callerId, correlationId, topicOperator, new ResultSignal(error, nativeType, result)));
  }

  private Message constructMessage (final String callerId, final String correlationId, final TopicOperator topicOperator, final ResultSignal resultSignal)
    throws Exception {

    return InstrumentationManager.execute(new ChronometerInstrumentAndReturn<Message>(this, new MetricProperty("event", MetricType.CONSTRUCT_MESSAGE.getDisplay())) {

      @Override
      public Message withChronometer ()
        throws Exception {

        BytesMessage responseMessage;

        responseMessage = topicOperator.createMessage();

        responseMessage.writeBytes(signalCodec.encode(resultSignal));

        responseMessage.setJMSCorrelationID(correlationId);
        responseMessage.setStringProperty(WireProperty.CALLER_ID.getKey(), callerId);
        responseMessage.setStringProperty(WireProperty.CONTENT_TYPE.getKey(), signalCodec.getContentType());
        responseMessage.setLongProperty(WireProperty.CLOCK.getKey(), System.currentTimeMillis());

        return responseMessage;
      }
    });
  }

  @Override
  public void close ()
    throws Exception {

    if (closed.compareAndSet(false, true)) {
      for (RequestListener requestListener : talkRequestListeners) {
        requestListener.close();
      }
      for (RequestListener requestListener : whisperRequestListeners) {
        requestListener.close();
      }

      for (ConnectionManager responseConnectionManager : responseConnectionManagers) {
        responseConnectionManager.stop();
      }
      for (ConnectionManager responseConnectionManager : responseConnectionManagers) {
        responseConnectionManager.close();
      }

      shutDown();
    }
  }
}
