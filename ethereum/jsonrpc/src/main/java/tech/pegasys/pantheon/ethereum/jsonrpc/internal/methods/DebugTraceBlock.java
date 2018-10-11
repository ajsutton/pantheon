package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods;

import static org.apache.logging.log4j.LogManager.getLogger;

import tech.pegasys.pantheon.ethereum.chain.Blockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.MutableWorldState;
import tech.pegasys.pantheon.ethereum.core.ProcessableBlockHeader;
import tech.pegasys.pantheon.ethereum.core.Transaction;
import tech.pegasys.pantheon.ethereum.core.WorldUpdater;
import tech.pegasys.pantheon.ethereum.db.WorldStateArchive;
import tech.pegasys.pantheon.ethereum.debug.TraceOptions;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTrace;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.processor.TransactionTraceParams;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.queries.BlockchainQueries;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.results.DebugTraceTransactionResult;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSpec;
import tech.pegasys.pantheon.ethereum.mainnet.ScheduleBasedBlockHashFunction;
import tech.pegasys.pantheon.ethereum.mainnet.TransactionProcessor;
import tech.pegasys.pantheon.ethereum.rlp.BytesValueRLPInput;
import tech.pegasys.pantheon.ethereum.vm.BlockHashLookup;
import tech.pegasys.pantheon.ethereum.vm.DebugOperationTracer;
import tech.pegasys.pantheon.ethereum.vm.OperationTracer;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

public class DebugTraceBlock implements JsonRpcMethod {
  private static final Logger LOG = getLogger();
  private final JsonRpcParameter parameters;
  private final BlockchainQueries blockchain;
  private final ProtocolSchedule<?> protocolSchedule;

  public DebugTraceBlock(
      final BlockchainQueries blockchain,
      final JsonRpcParameter parameters,
      final ProtocolSchedule<?> protocolSchedule) {
    this.blockchain = blockchain;
    this.parameters = parameters;
    this.protocolSchedule = protocolSchedule;
  }

  @Override
  public String getName() {
    return "debug_traceBlock";
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequest request) {

    final String rawBlock = parameters.required(request.getParams(), 0, String.class);
    final Optional<TransactionTraceParams> transactionTraceParams =
        parameters.optional(request.getParams(), 1, TransactionTraceParams.class);
    final Block block =
        Block.readFrom(
            new BytesValueRLPInput(BytesValue.fromHexString(rawBlock), false),
            ScheduleBasedBlockHashFunction.create(protocolSchedule));

    LOG.error(
        "Tracing block {} with hash {}. Parent hash {}",
        block.getHeader().getNumber(),
        block.getHash(),
        block.getHeader().getParentHash());
    final TraceOptions traceOptions =
        transactionTraceParams
            .map(TransactionTraceParams::traceOptions)
            .orElse(TraceOptions.DEFAULT);

    final ProtocolSpec<?> protocolSpec =
        protocolSchedule.getByBlockNumber(block.getHeader().getNumber());

    final DebugTransactionProcessor debugTransactionProcessor =
        new DebugTransactionProcessor(traceOptions, protocolSpec.getTransactionProcessor());

    return initialWorldState(
            blockchain.getBlockchain(), blockchain.getWorldStateArchive(), block.getHeader())
        .map(
            initialWorldState -> {
              protocolSpec
                  .getBlockProcessor()
                  .processBlock(
                      blockchain.getBlockchain(),
                      initialWorldState,
                      block,
                      Optional.of(debugTransactionProcessor));
              return new JsonRpcSuccessResponse(
                  request.getId(),
                  debugTransactionProcessor
                      .traces()
                      .stream()
                      .map(DebugTraceTransactionResult::new)
                      .map(trace -> Collections.singletonMap("result", trace))
                      .collect(Collectors.toList()));
            })
        .orElseGet(() -> new JsonRpcSuccessResponse(request.getId(), null));
  }

  private static Optional<MutableWorldState> initialWorldState(
      final Blockchain blockchain,
      final WorldStateArchive worldStateArchive,
      final BlockHeader header) {
    // Parent is assumed to exist if this is called.
    return blockchain
        .getBlockHeader(header.getParentHash())
        .map(
            parentHeader -> {
              LOG.error("Parent exists looking up world state");
              return worldStateArchive.getMutable(parentHeader.getStateRoot());
            });
  }

  private static class DebugTransactionProcessor implements TransactionProcessor {

    private final TraceOptions options;
    private final TransactionProcessor delegate;
    private final List<TransactionTrace> traces = new ArrayList<>();

    private DebugTransactionProcessor(
        final TraceOptions options, final TransactionProcessor delegate) {
      this.options = options;
      this.delegate = delegate;
    }

    public List<TransactionTrace> traces() {
      return traces;
    }

    @Override
    public Result processTransaction(
        final Blockchain blockchain,
        final WorldUpdater worldState,
        final ProcessableBlockHeader blockHeader,
        final Transaction transaction,
        final Address miningBeneficiary,
        final BlockHashLookup blockHashLookup) {
      final DebugOperationTracer operationTracer = new DebugOperationTracer(options);
      final Result result =
          delegate.processTransaction(
              blockchain,
              worldState,
              blockHeader,
              transaction,
              miningBeneficiary,
              operationTracer,
              blockHashLookup);
      traces.add(new TransactionTrace(transaction, result, operationTracer.getTraceFrames()));
      return result;
    }

    @Override
    public Result processTransaction(
        final Blockchain blockchain,
        final WorldUpdater worldState,
        final ProcessableBlockHeader blockHeader,
        final Transaction transaction,
        final Address miningBeneficiary,
        final OperationTracer operationTracer,
        final BlockHashLookup blockHashLookup) {
      // We insist on using our own OperationTracer.
      return processTransaction(
          blockchain, worldState, blockHeader, transaction, miningBeneficiary, blockHashLookup);
    }
  }
}
