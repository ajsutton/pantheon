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

import static com.google.common.base.Preconditions.checkNotNull;

import tech.pegasys.pantheon.ethereum.core.BlockHashFunction;
import tech.pegasys.pantheon.ethereum.core.BlockImporter;
import tech.pegasys.pantheon.ethereum.core.Wei;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetBlockProcessor.TransactionReceiptFactory;
import tech.pegasys.pantheon.ethereum.mainnet.account.AccountInit;
import tech.pegasys.pantheon.ethereum.mainnet.staterent.RentProcessor;
import tech.pegasys.pantheon.ethereum.vm.EVM;
import tech.pegasys.pantheon.ethereum.vm.GasCalculator;

import java.util.function.Function;
import java.util.function.Supplier;

public class ProtocolSpecBuilder<T> {
  private Supplier<GasCalculator> gasCalculatorBuilder;
  private Wei blockReward;
  private BlockHashFunction blockHashFunction;
  private TransactionReceiptFactory transactionReceiptFactory;
  private DifficultyCalculator<T> difficultyCalculator;
  private Function<GasCalculator, EVM> evmBuilder;
  private Function<GasCalculator, TransactionValidator> transactionValidatorBuilder;
  private Function<DifficultyCalculator<T>, BlockHeaderValidator<T>> blockHeaderValidatorBuilder;
  private Function<DifficultyCalculator<T>, BlockHeaderValidator<T>> ommerHeaderValidatorBuilder;
  private Function<ProtocolSchedule<T>, BlockBodyValidator<T>> blockBodyValidatorBuilder;
  private ContractCreationProcessorBuilder contractCreationProcessorBuilder;
  private Function<GasCalculator, PrecompileContractRegistry> precompileContractRegistryBuilder;
  private MessageCallProcessorBuilder messageCallProcessorBuilder;
  private TransactionProcessorBuilder transactionProcessorBuilder;
  private BlockProcessorBuilder blockProcessorBuilder;
  private BlockImporterBuilder<T> blockImporterBuilder;
  private TransactionReceiptType transactionReceiptType;
  private String name;
  private MiningBeneficiaryCalculator miningBeneficiaryCalculator;
  private Function<Wei, RentProcessor> rentProcessorBuilder;
  private Wei rentCost;
  private Supplier<AccountInit> accountInitBuilder;

  public ProtocolSpecBuilder<T> gasCalculator(final Supplier<GasCalculator> gasCalculatorBuilder) {
    this.gasCalculatorBuilder = gasCalculatorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> blockReward(final Wei blockReward) {
    this.blockReward = blockReward;
    return this;
  }

  public ProtocolSpecBuilder<T> rentProcessor(final Function<Wei, RentProcessor> rentProcessor) {
    this.rentProcessorBuilder = rentProcessor;
    return this;
  }

  public ProtocolSpecBuilder<T> rentCost(final Wei rentCost) {
    this.rentCost = rentCost;
    return this;
  }

  public ProtocolSpecBuilder<T> blockHashFunction(final BlockHashFunction blockHashFunction) {
    this.blockHashFunction = blockHashFunction;
    return this;
  }

  public ProtocolSpecBuilder<T> transactionReceiptFactory(
      final TransactionReceiptFactory transactionReceiptFactory) {
    this.transactionReceiptFactory = transactionReceiptFactory;
    return this;
  }

  public ProtocolSpecBuilder<T> difficultyCalculator(
      final DifficultyCalculator<T> difficultyCalculator) {
    this.difficultyCalculator = difficultyCalculator;
    return this;
  }

  public ProtocolSpecBuilder<T> evmBuilder(final Function<GasCalculator, EVM> evmBuilder) {
    this.evmBuilder = evmBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> transactionValidatorBuilder(
      final Function<GasCalculator, TransactionValidator> transactionValidatorBuilder) {
    this.transactionValidatorBuilder = transactionValidatorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> blockHeaderValidatorBuilder(
      final Function<DifficultyCalculator<T>, BlockHeaderValidator<T>>
          blockHeaderValidatorBuilder) {
    this.blockHeaderValidatorBuilder = blockHeaderValidatorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> ommerHeaderValidatorBuilder(
      final Function<DifficultyCalculator<T>, BlockHeaderValidator<T>>
          ommerHeaderValidatorBuilder) {
    this.ommerHeaderValidatorBuilder = ommerHeaderValidatorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> blockBodyValidatorBuilder(
      final Function<ProtocolSchedule<T>, BlockBodyValidator<T>> blockBodyValidatorBuilder) {
    this.blockBodyValidatorBuilder = blockBodyValidatorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> contractCreationProcessorBuilder(
      final ContractCreationProcessorBuilder contractCreationProcessorBuilder) {
    this.contractCreationProcessorBuilder = contractCreationProcessorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> precompileContractRegistryBuilder(
      final Function<GasCalculator, PrecompileContractRegistry> precompileContractRegistryBuilder) {
    this.precompileContractRegistryBuilder = precompileContractRegistryBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> messageCallProcessorBuilder(
      final MessageCallProcessorBuilder messageCallProcessorBuilder) {
    this.messageCallProcessorBuilder = messageCallProcessorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> transactionProcessorBuilder(
      final TransactionProcessorBuilder transactionProcessorBuilder) {
    this.transactionProcessorBuilder = transactionProcessorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> blockProcessorBuilder(
      final BlockProcessorBuilder blockProcessorBuilder) {
    this.blockProcessorBuilder = blockProcessorBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> blockImporterBuilder(
      final BlockImporterBuilder<T> blockImporterBuilder) {
    this.blockImporterBuilder = blockImporterBuilder;
    return this;
  }

  public ProtocolSpecBuilder<T> transactionReceiptType(
      final TransactionReceiptType transactionReceiptType) {
    this.transactionReceiptType = transactionReceiptType;
    return this;
  }

  public ProtocolSpecBuilder<T> miningBeneficiaryCalculator(
      final MiningBeneficiaryCalculator miningBeneficiaryCalculator) {
    this.miningBeneficiaryCalculator = miningBeneficiaryCalculator;
    return this;
  }

  public ProtocolSpecBuilder<T> accountInit(final Supplier<AccountInit> accountInit) {
    this.accountInitBuilder = accountInit;
    return this;
  }

  public ProtocolSpecBuilder<T> name(final String name) {
    this.name = name;
    return this;
  }

  public <R> ProtocolSpecBuilder<R> changeConsensusContextType(
      final Function<DifficultyCalculator<R>, BlockHeaderValidator<R>> blockHeaderValidatorBuilder,
      final Function<DifficultyCalculator<R>, BlockHeaderValidator<R>> ommerHeaderValidatorBuilder,
      final Function<ProtocolSchedule<R>, BlockBodyValidator<R>> blockBodyValidatorBuilder,
      final BlockImporterBuilder<R> blockImporterBuilder,
      final DifficultyCalculator<R> difficultyCalculator) {
    return new ProtocolSpecBuilder<R>()
        .gasCalculator(gasCalculatorBuilder)
        .rentProcessor(rentProcessorBuilder)
        .rentCost(rentCost)
        .evmBuilder(evmBuilder)
        .transactionValidatorBuilder(transactionValidatorBuilder)
        .contractCreationProcessorBuilder(contractCreationProcessorBuilder)
        .precompileContractRegistryBuilder(precompileContractRegistryBuilder)
        .messageCallProcessorBuilder(messageCallProcessorBuilder)
        .transactionProcessorBuilder(transactionProcessorBuilder)
        .blockHeaderValidatorBuilder(blockHeaderValidatorBuilder)
        .ommerHeaderValidatorBuilder(ommerHeaderValidatorBuilder)
        .blockBodyValidatorBuilder(blockBodyValidatorBuilder)
        .blockProcessorBuilder(blockProcessorBuilder)
        .blockImporterBuilder(blockImporterBuilder)
        .blockHashFunction(blockHashFunction)
        .blockReward(blockReward)
        .difficultyCalculator(difficultyCalculator)
        .transactionReceiptFactory(transactionReceiptFactory)
        .transactionReceiptType(transactionReceiptType)
        .miningBeneficiaryCalculator(miningBeneficiaryCalculator)
        .accountInit(accountInitBuilder)
        .name(name);
  }

  public ProtocolSpec<T> build(final ProtocolSchedule<T> protocolSchedule) {
    checkNotNull(gasCalculatorBuilder, "Missing gasCalculator");
    checkNotNull(rentProcessorBuilder, "Missing rent processor");
    checkNotNull(rentCost, "Missing rent cost");
    checkNotNull(evmBuilder, "Missing operation registry");
    checkNotNull(transactionValidatorBuilder, "Missing transaction validator");
    checkNotNull(contractCreationProcessorBuilder, "Missing contract creation processor");
    checkNotNull(precompileContractRegistryBuilder, "Missing precompile contract registry");
    checkNotNull(messageCallProcessorBuilder, "Missing message call processor");
    checkNotNull(transactionProcessorBuilder, "Missing transaction processor");
    checkNotNull(blockHeaderValidatorBuilder, "Missing block header validator");
    checkNotNull(blockBodyValidatorBuilder, "Missing block body validator");
    checkNotNull(blockProcessorBuilder, "Missing block processor");
    checkNotNull(blockImporterBuilder, "Missing block importer");
    checkNotNull(blockHashFunction, "Missing block hash function");
    checkNotNull(blockReward, "Missing block reward");
    checkNotNull(difficultyCalculator, "Missing difficulty calculator");
    checkNotNull(transactionReceiptFactory, "Missing transaction receipt factory");
    checkNotNull(transactionReceiptType, "Missing transaction receipt type");
    checkNotNull(name, "Missing name");
    checkNotNull(miningBeneficiaryCalculator, "Missing Mining Beneficiary Calculator");
    checkNotNull(accountInitBuilder, "Missing account init");
    checkNotNull(protocolSchedule, "Missing protocol schedule");

    final AccountInit accountInit = accountInitBuilder.get();
    final GasCalculator gasCalculator = gasCalculatorBuilder.get();
    final RentProcessor rentProcessor = rentProcessorBuilder.apply(rentCost);
    final EVM evm = evmBuilder.apply(gasCalculator);
    final TransactionValidator transactionValidator =
        transactionValidatorBuilder.apply(gasCalculator);
    final AbstractMessageProcessor contractCreationProcessor =
        contractCreationProcessorBuilder.apply(gasCalculator, evm, accountInit);
    final PrecompileContractRegistry precompileContractRegistry =
        precompileContractRegistryBuilder.apply(gasCalculator);
    final AbstractMessageProcessor messageCallProcessor =
        messageCallProcessorBuilder.apply(evm, precompileContractRegistry, accountInit);
    final TransactionProcessor transactionProcessor =
        transactionProcessorBuilder.apply(
            gasCalculator,
            transactionValidator,
            contractCreationProcessor,
            messageCallProcessor,
            accountInit,
            rentProcessor);
    final BlockHeaderValidator<T> blockHeaderValidator =
        blockHeaderValidatorBuilder.apply(difficultyCalculator);
    final BlockHeaderValidator<T> ommerHeaderValidator =
        ommerHeaderValidatorBuilder.apply(difficultyCalculator);
    final BlockBodyValidator<T> blockBodyValidator =
        blockBodyValidatorBuilder.apply(protocolSchedule);
    final BlockProcessor blockProcessor =
        blockProcessorBuilder.apply(
            transactionProcessor,
            transactionReceiptFactory,
            rentProcessor,
            blockReward,
            miningBeneficiaryCalculator,
            accountInit);
    final BlockImporter<T> blockImporter =
        blockImporterBuilder.apply(blockHeaderValidator, blockBodyValidator, blockProcessor);
    return new ProtocolSpec<>(
        name,
        evm,
        transactionValidator,
        transactionProcessor,
        blockHeaderValidator,
        ommerHeaderValidator,
        blockBodyValidator,
        blockProcessor,
        blockImporter,
        blockHashFunction,
        transactionReceiptFactory,
        difficultyCalculator,
        blockReward,
        transactionReceiptType,
        miningBeneficiaryCalculator,
        rentProcessor,
        accountInit);
  }

  public interface TransactionProcessorBuilder {
    TransactionProcessor apply(
        GasCalculator gasCalculator,
        TransactionValidator transactionValidator,
        AbstractMessageProcessor contractCreationProcessor,
        AbstractMessageProcessor messageCallProcessor,
        AccountInit accountInit,
        RentProcessor rentProcessor);
  }

  public interface BlockProcessorBuilder {
    BlockProcessor apply(
        TransactionProcessor transactionProcessor,
        TransactionReceiptFactory transactionReceiptFactory,
        RentProcessor rentProcessor,
        Wei blockReward,
        MiningBeneficiaryCalculator miningBeneficiaryCalculator,
        AccountInit accountInit);
  }

  public interface BlockImporterBuilder<T> {
    BlockImporter<T> apply(
        BlockHeaderValidator<T> blockHeaderValidator,
        BlockBodyValidator<T> blockBodyValidator,
        BlockProcessor blockProcessor);
  }

  public interface ContractCreationProcessorBuilder {
    AbstractMessageProcessor apply(GasCalculator gasCalculator, EVM evm, AccountInit accountInit);
  }

  public interface MessageCallProcessorBuilder {
    AbstractMessageProcessor apply(
        EVM evm, PrecompileContractRegistry precompileContractRegistry, AccountInit accountInit);
  }
}
