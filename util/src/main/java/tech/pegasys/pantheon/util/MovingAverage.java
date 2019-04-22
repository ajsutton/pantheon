/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.util;

public class MovingAverage {

  private final int maxSamples;

  private volatile long currentAverage;

  private final long[] currentSamples;
  private int sampleCount = 0;
  private long sumOfCurrentSamples = 0;

  public MovingAverage(final int maxSamples) {
    this.maxSamples = maxSamples;
    currentSamples = new long[maxSamples];
  }

  public long getAverage() {
    return currentAverage;
  }

  public synchronized void recordValue(final long value) {
    sampleCount++;
    final int sampleNumber = sampleCount % maxSamples;
    sumOfCurrentSamples -= currentSamples[sampleNumber];
    currentSamples[sampleNumber] = value;
    sumOfCurrentSamples += value;
    currentAverage = sumOfCurrentSamples / Math.min(sampleCount, maxSamples);
  }
}
