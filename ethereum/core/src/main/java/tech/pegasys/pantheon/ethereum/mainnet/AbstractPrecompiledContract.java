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
package tech.pegasys.pantheon.ethereum.mainnet;

import tech.pegasys.pantheon.ethereum.core.Gas;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;
import tech.pegasys.pantheon.util.bytes.BytesValue;

/** Skeleton class for @{link PrecompileContract} implementations. */
public abstract class AbstractPrecompiledContract implements PrecompiledContract {

  private final GasCalculator gasCalculator;

  private final String name;

  public AbstractPrecompiledContract(final String name, final GasCalculator gasCalculator) {
    this.name = name;
    this.gasCalculator = gasCalculator;
  }

  protected GasCalculator gasCalculator() {
    return gasCalculator;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public abstract Gas gasRequirement(BytesValue input);

  @Override
  public abstract BytesValue compute(BytesValue input);
}
