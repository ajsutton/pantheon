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

import static tech.pegasys.pantheon.ethereum.mainnet.account.FrontierAccountInit.FRONTIER_ACCOUNT_INIT;

import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.MutableAccount;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.TransactionReceipt;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.core.WorldState;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.mainnet.account.AccountInit;
import tech.pegasys.pantheon.ethereum.mainnet.account.StateRentOwnedAccountsAccountInit;
import tech.pegasys.pantheon.ethereum.mainnet.staterent.ActiveRentProcessor;
import tech.pegasys.pantheon.ethereum.mainnet.staterent.InactiveRentProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import io.vertx.core.json.JsonArray;

/** Provides the various @{link ProtocolSpec}s on mainnet hard forks. */
public abstract class MainnetProtocolSpecs {

  public static final int FRONTIER_CONTRACT_SIZE_LIMIT = Integer.MAX_VALUE;

  public static final int SPURIOUS_DRAGON_CONTRACT_SIZE_LIMIT = 24576;

  private static final Address RIPEMD160_PRECOMPILE =
      Address.fromHexString("0x0000000000000000000000000000000000000003");

  // A consensus bug at Ethereum mainnet transaction 0xcf416c53
  // deleted an empty account even when the message execution scope
  // failed, but the transaction itself succeeded.
  private static final ImmutableSet<Address> SPURIOUS_DRAGON_FORCE_DELETE_WHEN_EMPTY_ADDRESSES =
      ImmutableSet.of(RIPEMD160_PRECOMPILE);

  private static final Wei FRONTIER_BLOCK_REWARD = Wei.fromEth(5);

  private static final Wei BYZANTIUM_BLOCK_REWARD = Wei.fromEth(3);

  private static final Wei CONSTANTINOPLE_BLOCK_REWARD = Wei.fromEth(2);

  private MainnetProtocolSpecs() {}

  public static ProtocolSpecBuilder<Void> frontierDefinition() {
    return new ProtocolSpecBuilder<Void>()
        .gasCalculator(FrontierGasCalculator::new)
        .rentProcessor(rentCost -> new InactiveRentProcessor())
        .rentCost(Wei.ZERO)
        .evmBuilder(MainnetEvmRegistries::frontier)
        .precompileContractRegistryBuilder(MainnetPrecompiledContractRegistries::frontier)
        .messageCallProcessorBuilder(MainnetMessageCallProcessor::new)
        .contractCreationProcessorBuilder(
            (gasCalculator, evm, accountInit) ->
                new MainnetContractCreationProcessor(
                    gasCalculator, evm, false, FRONTIER_CONTRACT_SIZE_LIMIT, 0, accountInit))
        .transactionValidatorBuilder(
            gasCalculator -> new MainnetTransactionValidator(gasCalculator, false))
        .transactionProcessorBuilder(
            (gasCalculator,
                transactionValidator,
                contractCreationProcessor,
                messageCallProcessor,
                accountInit,
                rentProcessor) ->
                new MainnetTransactionProcessor(
                    gasCalculator,
                    transactionValidator,
                    contractCreationProcessor,
                    messageCallProcessor,
                    accountInit,
                    rentProcessor,
                    false))
        .difficultyCalculator(MainnetDifficultyCalculators.FRONTIER)
        .blockHeaderValidatorBuilder(MainnetBlockHeaderValidator::create)
        .ommerHeaderValidatorBuilder(MainnetBlockHeaderValidator::createOmmerValidator)
        .blockBodyValidatorBuilder(MainnetBlockBodyValidator::new)
        .transactionReceiptFactory(MainnetProtocolSpecs::frontierTransactionReceiptFactory)
        .blockReward(FRONTIER_BLOCK_REWARD)
        .blockProcessorBuilder(MainnetBlockProcessor::new)
        .blockImporterBuilder(MainnetBlockImporter::new)
        .transactionReceiptType(TransactionReceiptType.ROOT)
        .blockHashFunction(MainnetBlockHashFunction::createHash)
        .miningBeneficiaryCalculator(BlockHeader::getCoinbase)
        .accountInit(() -> FRONTIER_ACCOUNT_INIT)
        .name("Frontier");
  }

  public static ProtocolSpecBuilder<Void> homesteadDefinition() {
    return frontierDefinition()
        .gasCalculator(HomesteadGasCalculator::new)
        .evmBuilder(MainnetEvmRegistries::homestead)
        .contractCreationProcessorBuilder(
            (gasCalculator, evm, accountInit) ->
                new MainnetContractCreationProcessor(
                    gasCalculator, evm, true, FRONTIER_CONTRACT_SIZE_LIMIT, 0, accountInit))
        .transactionValidatorBuilder(
            gasCalculator -> new MainnetTransactionValidator(gasCalculator, true))
        .difficultyCalculator(MainnetDifficultyCalculators.HOMESTEAD)
        .name("Homestead");
  }

  public static ProtocolSpecBuilder<Void> daoRecoveryInitDefinition() {
    return homesteadDefinition()
        .blockHeaderValidatorBuilder(MainnetBlockHeaderValidator::createDaoValidator)
        .blockProcessorBuilder(
            (transactionProcessor,
                transactionReceiptFactory,
                rentProcessor,
                blockReward,
                miningBeneficiaryCalculator,
                accountInit) ->
                new DaoBlockProcessor(
                    new MainnetBlockProcessor(
                        transactionProcessor,
                        transactionReceiptFactory,
                        rentProcessor,
                        blockReward,
                        miningBeneficiaryCalculator,
                        accountInit),
                    accountInit))
        .name("DaoRecoveryInit");
  }

  public static ProtocolSpecBuilder<Void> daoRecoveryTransitionDefinition() {
    return daoRecoveryInitDefinition()
        .blockProcessorBuilder(MainnetBlockProcessor::new)
        .name("DaoRecoveryTransition");
  }

  public static ProtocolSpecBuilder<Void> tangerineWhistleDefinition() {
    return homesteadDefinition()
        .gasCalculator(TangerineWhistleGasCalculator::new)
        .name("TangerineWhistle");
  }

  public static ProtocolSpecBuilder<Void> spuriousDragonDefinition(final int chainId) {
    return tangerineWhistleDefinition()
        .gasCalculator(SpuriousDragonGasCalculator::new)
        .messageCallProcessorBuilder(
            (evm, precompileContractRegistry, accountInit) ->
                new MainnetMessageCallProcessor(
                    evm,
                    precompileContractRegistry,
                    accountInit,
                    SPURIOUS_DRAGON_FORCE_DELETE_WHEN_EMPTY_ADDRESSES))
        .contractCreationProcessorBuilder(
            (gasCalculator, evm, accountInit) ->
                new MainnetContractCreationProcessor(
                    gasCalculator,
                    evm,
                    true,
                    SPURIOUS_DRAGON_CONTRACT_SIZE_LIMIT,
                    1,
                    accountInit,
                    SPURIOUS_DRAGON_FORCE_DELETE_WHEN_EMPTY_ADDRESSES))
        .transactionValidatorBuilder(
            gasCalculator -> new MainnetTransactionValidator(gasCalculator, true, chainId))
        .transactionProcessorBuilder(
            (gasCalculator,
                transactionValidator,
                contractCreationProcessor,
                messageCallProcessor,
                accountInit,
                rentProcessor) ->
                new MainnetTransactionProcessor(
                    gasCalculator,
                    transactionValidator,
                    contractCreationProcessor,
                    messageCallProcessor,
                    accountInit,
                    rentProcessor,
                    true))
        .name("SpuriousDragon");
  }

  public static ProtocolSpecBuilder<Void> byzantiumDefinition(final int chainId) {
    return spuriousDragonDefinition(chainId)
        .evmBuilder(MainnetEvmRegistries::byzantium)
        .precompileContractRegistryBuilder(MainnetPrecompiledContractRegistries::byzantium)
        .difficultyCalculator(MainnetDifficultyCalculators.BYZANTIUM)
        .transactionReceiptFactory(MainnetProtocolSpecs::byzantiumTransactionReceiptFactory)
        .blockReward(BYZANTIUM_BLOCK_REWARD)
        .transactionReceiptType(TransactionReceiptType.STATUS)
        .name("Byzantium");
  }

  public static ProtocolSpecBuilder<Void> constantinopleDefinition(final int chainId) {
    return byzantiumDefinition(chainId)
        .difficultyCalculator(MainnetDifficultyCalculators.CONSTANTINOPLE)
        .gasCalculator(ConstantinopleGasCalculator::new)
        .evmBuilder(MainnetEvmRegistries::constantinople)
        .blockReward(CONSTANTINOPLE_BLOCK_REWARD)
        .name("Constantinople");
  }

  public static ProtocolSpecBuilder<Void> stateRentOwnedAccountsDefinition(
      final int chainId, final long rentEnabledBlockNumber) {
    return constantinopleDefinition(chainId)
        .rentCost(Wei.fromGwei(2))
        .rentProcessor(rentCost -> new ActiveRentProcessor(rentCost, rentEnabledBlockNumber))
        .accountInit(StateRentOwnedAccountsAccountInit::new)
        .name("StateRentOwnedAccounts");
  }

  public static ProtocolSpecBuilder<Void> stateRentNewStorageDefinition(
      final int chainId, final long rentEnabledBlockNumber) {
    return stateRentOwnedAccountsDefinition(chainId, rentEnabledBlockNumber)
        .gasCalculator(StateRentNewStorageGasCalculator::new)
        .evmBuilder(MainnetEvmRegistries::stateRentNewStorage)
        .name("StateRentNewStorage");
  }

  private static TransactionReceipt frontierTransactionReceiptFactory(
      final TransactionProcessor.Result result, final WorldState worldState, final long gasUsed) {
    return new TransactionReceipt(worldState.rootHash(), gasUsed, result.getLogs());
  }

  private static TransactionReceipt byzantiumTransactionReceiptFactory(
      final TransactionProcessor.Result result, final WorldState worldState, final long gasUsed) {
    return new TransactionReceipt(result.isSuccessful() ? 1 : 0, gasUsed, result.getLogs());
  }

  private static class DaoBlockProcessor implements BlockProcessor {

    private final BlockProcessor wrapped;
    private final AccountInit accountInit;

    public DaoBlockProcessor(final BlockProcessor wrapped, final AccountInit accountInit) {
      this.wrapped = wrapped;
      this.accountInit = accountInit;
    }

    @Override
    public Result processBlock(
        final Blockchain blockchain,
        final MutableWorldState worldState,
        final BlockHeader blockHeader,
        final List<Transaction> transactions,
        final List<BlockHeader> ommers) {
      updateWorldStateForDao(worldState, blockHeader.getNumber());
      return wrapped.processBlock(blockchain, worldState, blockHeader, transactions, ommers);
    }

    private static final Address DAO_REFUND_CONTRACT_ADDRESS =
        Address.fromHexString("0xbf4ed7b27f1d666546e30d74d50d173d20bca754");

    private void updateWorldStateForDao(
        final MutableWorldState worldState, final long blockNumber) {
      try {
        final JsonArray json =
            new JsonArray(
                Resources.toString(
                    Resources.getResource("daoAddresses.json"), StandardCharsets.UTF_8));
        final List<Address> addresses =
            IntStream.range(0, json.size())
                .mapToObj(json::getString)
                .map(Address::fromHexString)
                .collect(Collectors.toList());
        final WorldUpdater worldUpdater = worldState.updater();
        final MutableAccount daoRefundContract =
            worldUpdater.getOrCreate(DAO_REFUND_CONTRACT_ADDRESS, accountInit, blockNumber);
        for (final Address address : addresses) {
          final MutableAccount account =
              worldUpdater.getOrCreate(address, accountInit, blockNumber);
          final Wei balance = account.getBalance();
          account.decrementBalance(balance);
          daoRefundContract.incrementBalance(balance);
        }
        worldUpdater.commit();
      } catch (final IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }
}
