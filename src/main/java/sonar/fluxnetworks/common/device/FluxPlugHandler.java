package sonar.fluxnetworks.common.device;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;

public class FluxPlugHandler extends FluxConnectorHandler {

    // external received energy happen outside the transfer cycle
    private long mReceived;

    // internal removed energy happen inside the transfer cycle
    private long mRemoved;
    private final TransactionJournal mTransactionJournal = new TransactionJournal();

    public FluxPlugHandler() {
    }

    @Override
    public void onCycleEnd() {
        mChange = mReceived;
        mReceived = 0;
        mRemoved = 0;
    }

    @Override
    public long removeFromBuffer(long energy) {
        long op = Math.min(Math.min(energy, mBuffer), getLimit() - mRemoved);
        assert op >= 0;
        mBuffer -= op;
        mRemoved += op;
        return op;
    }

    public long receive(long maxReceive, @Nullable Direction side, boolean simulate, long bufferLimiter) {
        return receive(maxReceive, side, null, simulate, bufferLimiter);
    }

    public long receive(long maxReceive, @Nullable Direction side, TransactionContext transaction, long bufferLimiter) {
        return receive(maxReceive, side, transaction, false, bufferLimiter);
    }

    private long receive(long maxReceive, @Nullable Direction side, @Nullable TransactionContext transaction,
                         boolean simulate, long bufferLimiter) {
        long op = Math.min(Math.min(getLimit(), bufferLimiter) - mBuffer, maxReceive);
        if (op > 0) {
            if (!simulate) {
                if (transaction != null) {
                    mTransactionJournal.updateSnapshots(transaction);
                }
                mBuffer += op;
                mReceived += op;
                if (side != null) {
                    SideTransfer transfer = mTransfers[side.get3DDataValue()];
                    if (transfer != null) {
                        transfer.receive(op);
                    }
                }
            }
            return op;
        }
        return 0;
    }

    private TransactionSnapshot createTransactionSnapshot() {
        long[] sideChanges = new long[mTransfers.length];
        for (int i = 0; i < mTransfers.length; i++) {
            SideTransfer transfer = mTransfers[i];
            sideChanges[i] = transfer == null ? 0 : transfer.mChange;
        }
        return new TransactionSnapshot(mBuffer, mChange, mReceived, mRemoved, sideChanges);
    }

    private void revertTransactionSnapshot(TransactionSnapshot snapshot) {
        mBuffer = snapshot.buffer;
        mChange = snapshot.change;
        mReceived = snapshot.received;
        mRemoved = snapshot.removed;
        for (int i = 0; i < mTransfers.length; i++) {
            SideTransfer transfer = mTransfers[i];
            if (transfer != null) {
                transfer.mChange = snapshot.sideChanges[i];
            }
        }
    }

    private class TransactionJournal extends SnapshotJournal<TransactionSnapshot> {

        @Override
        protected TransactionSnapshot createSnapshot() {
            return createTransactionSnapshot();
        }

        @Override
        protected void revertToSnapshot(TransactionSnapshot snapshot) {
            revertTransactionSnapshot(snapshot);
        }
    }

    private record TransactionSnapshot(long buffer, long change, long received, long removed, long[] sideChanges) {
    }
}
