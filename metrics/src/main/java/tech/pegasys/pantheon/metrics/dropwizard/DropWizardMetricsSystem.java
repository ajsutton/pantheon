package tech.pegasys.pantheon.metrics.dropwizard;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import tech.pegasys.pantheon.metrics.Counter;
import tech.pegasys.pantheon.metrics.LabelledMetric;
import tech.pegasys.pantheon.metrics.MetricsSystem;
import tech.pegasys.pantheon.metrics.Observation;
import tech.pegasys.pantheon.metrics.OperationTimer;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DropWizardMetricsSystem implements MetricsSystem {

  private static final Logger LOG = LogManager.getLogger();
  private final MetricRegistry metrics = new MetricRegistry();
  private final Map<Category, Collection<MetricAdapater>> collectors = new ConcurrentHashMap<>();
  private final Map<String, DropWizardLabelledMetric<?>> labelledMetrics = new ConcurrentHashMap<>();

  public static MetricsSystem init() {
    final DropWizardMetricsSystem metricsSystem = new DropWizardMetricsSystem();
    //    metricsSystem.collectors.put(Category.PROCESS, singleton(new StandardExports()));
    //    new FileDescriptorRatioGauge(),
    metricsSystem.register(
        Category.JVM,
        new ThreadStatesGaugeSet(),
        new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()),
        new GarbageCollectorMetricSet(),
        new MemoryUsageGaugeSet(),
        new ClassLoadingGaugeSet(),
        new JvmAttributeGaugeSet());
    final FileDescriptorRatioGauge fileDescriptorRatioGauge = new FileDescriptorRatioGauge();
    metricsSystem.metrics.register("process_file_descriptor_ratio", fileDescriptorRatioGauge);
    metricsSystem.addCollector(
        Category.PROCESS,
        "process_file_descriptor_ratio",
        "file_descriptor_ratio",
        fileDescriptorRatioGauge,
        emptyList());

    final JmxReporter jmxReporter = JmxReporter.forRegistry(metricsSystem.metrics).build();
    jmxReporter.start();
    return metricsSystem;
  }

  private void register(final Category category, final MetricSet... metricSets) {
    for (final MetricSet metricSet : metricSets) {
      metrics.registerAll(metricSet);
      metricSet.getMetrics()
          .keySet()
          .forEach(metricName -> addCollector(
              category,
              metricName,
              category.extractRawName(metricName),
              metrics.getMetrics().get(metricName),
              emptyList()));
    }
  }

  @Override
  public Counter createCounter(final Category category, final String name, final String help) {
    final String metricName = category.getNameForMetric(name);
    final com.codahale.metrics.Counter counter = metrics.counter(metricName);
    addCollector(category, metricName, name, counter, emptyList());
    return counter::inc;
  }

  @Override
  public LabelledMetric<Counter> createCounter(
      final Category category, final String name, final String help, final String... labelNames) {
    return createLabelledMetric(category, name, n -> metrics.counter(n)::inc);
  }

  @Override
  public LabelledMetric<OperationTimer> createTimer(
      final Category category, final String name, final String help, final String... labelNames) {
    return createLabelledMetric(category, name, this::createOperationTimer);
  }

  private OperationTimer createOperationTimer(final String metricName) {
    final Timer timer = metrics.timer(metricName);
    return () -> timer.time()::stop;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void createGauge(
      final Category category,
      final String name,
      final String help,
      final Supplier<Double> collector) {
    final String metricName = category.getNameForMetric(name);
    final Gauge<Double> gauge = metrics.gauge(metricName, () -> collector::get);
    addCollector(category, metricName, name, gauge, emptyList());
  }

  @Override
  public Stream<Observation> getMetrics(final Category category) {
    return collectors.getOrDefault(category, Collections.emptySet()).stream()
        .flatMap(this::observationsFor);
  }

  private Stream<Observation> observationsFor(final MetricAdapater metricAdapter) {
    final Metric metric = metricAdapter.getMetric();
    final Category category = metricAdapter.getCategory();
    final List<String> labels = metricAdapter.getLabels();
    final String simpleName = metricAdapter.getSimpleName();
    if (metric instanceof com.codahale.metrics.Counter) {
      return Stream.of(new Observation(category,
          simpleName, ((com.codahale.metrics.Counter) metric).getCount(), labels));
    } else if (metric instanceof Gauge) {
      final Object value = ((Gauge) metric).getValue();
      return Stream.of(new Observation(category, simpleName, value, labels));
    } else if (metric instanceof Timer) {
      final Timer timer = (Timer) metric;
      final Snapshot snapshot = timer.getSnapshot();

      return Stream.of(
          new Observation(category, simpleName, snapshot.getMin(), singletonList("min")),
          new Observation(category, simpleName, snapshot.getMax(), singletonList("max")),
          new Observation(category, simpleName, snapshot.getMean(), singletonList("mean")),
          new Observation(category,
              simpleName, snapshot.getMedian(), singletonList("median")),
          new Observation(category,
              simpleName, snapshot.getStdDev(), singletonList("stdDev")),
          new Observation(category,
              simpleName, snapshot.get75thPercentile(), singletonList("p75")),
          new Observation(category,
              simpleName, snapshot.get95thPercentile(), singletonList("p95")),
          new Observation(category,
              simpleName, snapshot.get98thPercentile(), singletonList("p98")),
          new Observation(category,
              simpleName, snapshot.get99thPercentile(), singletonList("p99")),
          new Observation(category,
              simpleName, snapshot.get999thPercentile(), singletonList("p999")),
          new Observation(category,
              simpleName, timer.getOneMinuteRate(), singletonList("oneMinuteRate")),
          new Observation(category,
              simpleName, timer.getFiveMinuteRate(), singletonList("fiveMinuteRate")),
          new Observation(category,
              simpleName, timer.getFifteenMinuteRate(), singletonList("fifteenMinuteRate")),
          new Observation(category,
              simpleName, timer.getFifteenMinuteRate(), singletonList("fifteenMinuteRate")),
          new Observation(category,
              simpleName, timer.getMeanRate(), singletonList("meanRate")),
          new Observation(category, simpleName, timer.getCount(), singletonList("count")));
    }
    return Stream.empty();
  }

  private <T> LabelledMetric<T> createLabelledMetric(
      final Category category, final String name, final Function<String, T> metricFactory) {
    final String metricName = category.getNameForMetric(name);
    final DropWizardLabelledMetric<T> labelledMetric = new DropWizardLabelledMetric<>(labels -> {
      final String combinedName = combinedName(metricName, labels);
      final T metric = metricFactory.apply(combinedName);
      addCollector(
          category,
          metricName,
          name,
          metrics.getMetrics().get(combinedName),
          asList(labels));
      return metric;
    });
    labelledMetrics.put(metricName, labelledMetric);
    return labelledMetric;
  }

  private String combinedName(final String baseName, final String... labels) {
    return baseName + Arrays.toString(labels);
  }

  private void addCollector(
      final Category category,
      final String metricName,
      final String simpleName,
      final Metric metric,
      final List<String> labels) {
    collectors
        .computeIfAbsent(category, key -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
        .add(new MetricAdapater(category, metricName, simpleName, labels, metric));
  }

  private static class MetricAdapater {

    private final Category category;
    private final String metricName;
    private final String simpleName;
    private final List<String> labels;
    private final Metric metric;

    private MetricAdapater(
        final Category category,
        final String metricName,
        final String simpleName,
        final List<String> labels,
        final Metric metric) {
      this.category = category;
      this.metricName = metricName;
      this.simpleName = simpleName;
      this.labels = labels;
      this.metric = metric;
    }

    public Category getCategory() {
      return category;
    }

    public String getMetricName() {
      return metricName;
    }

    public String getSimpleName() {
      return simpleName;
    }

    public List<String> getLabels() {
      return labels;
    }

    public Metric getMetric() {
      return metric;
    }
  }
}
