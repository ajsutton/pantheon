package tech.pegasys.pantheon.ethereum.worldstate;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.BlockAddedEvent;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Hash;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Pruner {
  private static final Logger LOG = LogManager.getLogger();
  private final MarkSweepPruner pruningStrategy;
  private final ProtocolContext<?> protocolContext;
  private final ExecutorService executor;
  private final long retentionPeriodInBlocks;
  private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
  private volatile long markedBlockNumber = 0;

  public Pruner(
      final MarkSweepPruner pruningStrategy,
      final ProtocolContext<?> protocolContext,
      final long retentionPeriodInBlocks) {
    this.pruningStrategy = pruningStrategy;
    this.protocolContext = protocolContext;
    this.executor =
        Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder()
                .setDaemon(true)
                .setPriority(Thread.MIN_PRIORITY)
                .setNameFormat("StatePruning-%d")
                .build());
    this.retentionPeriodInBlocks = retentionPeriodInBlocks;
  }

  public void start() {
    protocolContext.getBlockchain().observeBlockAdded((event, blockchain) -> handleNewBlock(event));
  }

  public void stop() {
    executor.shutdownNow();
  }

  private void handleNewBlock(final BlockAddedEvent event) {
    if (!event.isNewCanonicalHead()) {
      return;
    }
    final BlockHeader header = event.getBlock().getHeader();
    if (state.compareAndSet(State.IDLE, State.MARKING)) {
      mark(header);
    } else if (header.getNumber() > markedBlockNumber + retentionPeriodInBlocks
        && state.compareAndSet(State.MARKING_COMPLETE, State.SWEEPING)) {
      sweep();
    }
  }

  private void mark(final BlockHeader header) {
    markedBlockNumber = header.getNumber();
    final Hash stateRoot = header.getStateRoot();
    LOG.info(
        "Begin marking used nodes for pruning. Block number: {} State root: {}",
        markedBlockNumber,
        stateRoot);
    execute(
        () -> {
          pruningStrategy.mark(stateRoot);
          state.compareAndSet(State.MARKING, State.MARKING_COMPLETE);
        });
  }

  private void sweep() {
    execute(
        () -> {
          pruningStrategy.sweep();
          state.compareAndSet(State.SWEEPING, State.IDLE);
        });
  }

  private void execute(final Runnable action) {
    try {
      executor.execute(action);
    } catch (final Throwable t) {
      LOG.error("Pruning failed", t);
      state.set(State.IDLE);
      System.exit(1);
    }
  }

  private enum State {
    IDLE,
    MARKING,
    MARKING_COMPLETE,
    SWEEPING
  }
}
