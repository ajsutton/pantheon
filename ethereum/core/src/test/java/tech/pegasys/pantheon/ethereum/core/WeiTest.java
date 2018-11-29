/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.ethereum.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class WeiTest {

  @Test
  public void shouldConvertGweiToWei() {
    assertThat(Wei.fromGwei(1)).isEqualTo(Wei.of(1000000000));
    assertThat(Wei.fromGwei(2)).isEqualTo(Wei.of(2000000000));
  }
}
