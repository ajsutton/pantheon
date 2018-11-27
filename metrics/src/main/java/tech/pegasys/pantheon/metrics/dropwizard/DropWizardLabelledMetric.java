package tech.pegasys.pantheon.metrics.dropwizard;

import tech.pegasys.pantheon.metrics.LabelledMetric;

import java.util.function.Function;

import com.codahale.metrics.Metric;

public class DropWizardLabelledMetric<T> implements LabelledMetric<T>, Metric {

  private final Function<String[], T> metricSupplier;

  public DropWizardLabelledMetric(final Function<String[], T> metricSupplier) {
    this.metricSupplier = metricSupplier;
  }

  @Override
  public T labels(final String... labels) {
    return metricSupplier.apply(labels);
  }
}
