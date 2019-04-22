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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MovingAverageTest {
  private final MovingAverage average = new MovingAverage(5);

  @Test
  public void shouldHaveZeroMovingAverageOnCreation() {
    assertThat(average.getAverage()).isEqualTo(0);
  }

  @Test
  public void shouldCalculateAverageWhenMaxSamplesHasNotBeenReached() {
    average.recordValue(500);
    assertThat(average.getAverage()).isEqualTo(500);

    average.recordValue(1000);
    assertThat(average.getAverage()).isEqualTo(750);

    average.recordValue(0);
    assertThat(average.getAverage()).isEqualTo(500);

    average.recordValue(250);
    assertThat(average.getAverage()).isEqualTo(437);

    average.recordValue(600);
    assertThat(average.getAverage()).isEqualTo(470);
  }

  @Test
  public void shouldDropOldestValueWhenMaxSamplesExceeded() {
    average.recordValue(500);
    average.recordValue(1000);
    average.recordValue(0);
    average.recordValue(250);
    average.recordValue(600);
    assertThat(average.getAverage()).isEqualTo(470);

    average.recordValue(800);
    assertThat(average.getAverage()).isEqualTo(530);
  }
}
