package peergos.shared.storage;

import peergos.shared.*;
import peergos.shared.cbor.*;
import peergos.shared.corenode.*;
import peergos.shared.crypto.hash.*;
import peergos.shared.io.ipfs.cid.*;
import peergos.shared.io.ipfs.multihash.*;
import peergos.shared.storage.auth.*;
import peergos.shared.user.fs.*;
import peergos.shared.util.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

public class BufferedStorage extends DelegatingStorage {

    private Map<Cid, OpLog.BlockWrite> storage = new EfficientHashMap<>();
    private final ContentAddressedStorage target;
    private final Hasher hasher;

    public BufferedStorage(ContentAddressedStorage target, Hasher hasher) {
        super(target);
        this.target = target;
        this.hasher = hasher;
    }

    @Override
    public ContentAddressedStorage directToOrigin() {
        return this;
    }

    @Override
    public CompletableFuture<TransactionId> startTransaction(PublicKeyHash owner) {
        TransactionId tid = new TransactionId(Long.toString(System.currentTimeMillis()));
        return CompletableFuture.completedFuture(tid);
    }

    @Override
    public CompletableFuture<Boolean> closeTransaction(PublicKeyHash owner, TransactionId tid) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<List<byte[]>> getChampLookup(PublicKeyHash owner, Cid root, byte[] champKey, Optional<BatWithId> bat) {
        // We must do a local champ look up because of the unwritten blocks
        return getChampLookup(root, champKey, bat, hasher);
    }

    @Override
    public CompletableFuture<List<Cid>> put(PublicKeyHash owner,
                                            PublicKeyHash writer,
                                            List<byte[]> signedHashes,
                                            List<byte[]> blocks,
                                            TransactionId tid) {
        return put(writer, blocks, signedHashes, false);
    }

    @Override
    public CompletableFuture<List<Cid>> putRaw(PublicKeyHash owner,
                                               PublicKeyHash writer,
                                               List<byte[]> signatures,
                                               List<byte[]> blocks,
                                               TransactionId tid,
                                               ProgressConsumer<Long> progressConsumer) {
        return put(writer, blocks, signatures, true);
    }

    private CompletableFuture<List<Cid>> put(PublicKeyHash writer,
                                             List<byte[]> blocks,
                                             List<byte[]> signatures,
                                             boolean isRaw) {
        return Futures.combineAllInOrder(IntStream.range(0, blocks.size())
                .mapToObj(i -> hashToCid(blocks.get(i), isRaw)
                        .thenApply(cid -> put(cid, new OpLog.BlockWrite(writer, signatures.get(i), blocks.get(i), isRaw))))
                .collect(Collectors.toList()));
    }

    private synchronized Cid put(Cid cid, OpLog.BlockWrite block) {
        storage.put(cid, block);
        return cid;
    }

    @Override
    public CompletableFuture<List<FragmentWithHash>> downloadFragments(List<Cid> hashes,
                                                                       List<BatWithId> bats,
                                                                       Hasher h,
                                                                       ProgressConsumer<Long> monitor,
                                                                       double spaceIncreaseFactor) {
        return NetworkAccess.downloadFragments(hashes, bats, this, h, monitor, spaceIncreaseFactor);
    }

    @Override
    public synchronized CompletableFuture<Optional<byte[]>> getRaw(Cid hash, Optional<BatWithId> bat) {
        OpLog.BlockWrite local = storage.get(hash);
        if (local != null)
            return Futures.of(Optional.of(local.block));
        return target.getRaw(hash, bat);
    }

    @Override
    public synchronized CompletableFuture<Optional<CborObject>> get(Cid hash, Optional<BatWithId> bat) {
        return getRaw(hash, bat)
                .thenApply(opt -> opt.map(CborObject::fromByteArray));
    }

    public synchronized CompletableFuture<Boolean> commit(PublicKeyHash owner, TransactionId tid) {
        // write blocks in batches of up to 50 all in 1 transaction
        int maxBlocksPerBatch = 50;
        List<List<OpLog.BlockWrite>> cborBatches = new ArrayList<>();
        List<List<OpLog.BlockWrite>> rawBatches = new ArrayList<>();
        int cborCount = 0, rawcount=0;
        for (Map.Entry<Cid, OpLog.BlockWrite> e : storage.entrySet()) {
            OpLog.BlockWrite val = e.getValue();
            List<List<OpLog.BlockWrite>> batches = val.isRaw ? rawBatches : cborBatches;
            int count = val.isRaw ? rawcount : cborCount;
            if (count % maxBlocksPerBatch == 0)
                batches.add(new ArrayList<>());
            batches.get(batches.size() - 1).add(val);
            count = (count + 1) % maxBlocksPerBatch;
            if (val.isRaw)
                rawcount = count;
            else
                cborCount = count;
        }
        return Futures.combineAllInOrder(rawBatches.stream()
                .map(batch -> target.putRaw(owner, batch.get(0).writer,
                        batch.stream().map(w -> w.signature).collect(Collectors.toList()),
                        batch.stream().map(w -> w.block).collect(Collectors.toList()), tid, x-> {}))
                .collect(Collectors.toList()))
                .thenCompose(a -> Futures.combineAllInOrder(cborBatches.stream()
                        .map(batch -> target.put(owner, batch.get(0).writer,
                                batch.stream().map(w -> w.signature).collect(Collectors.toList()),
                                batch.stream().map(w -> w.block).collect(Collectors.toList()), tid))
                        .collect(Collectors.toList())))
                .thenApply(a -> true);
    }

    public synchronized void clear() {
        storage.clear();
    }

    public synchronized int size() {
        return storage.size();
    }

    @Override
    public synchronized CompletableFuture<Optional<Integer>> getSize(Multihash block) {
        if (! storage.containsKey(block))
            return target.getSize(block);
        return CompletableFuture.completedFuture(Optional.of(storage.get(block).block.length));
    }

    public CompletableFuture<Cid> hashToCid(byte[] input, boolean isRaw) {
        return hasher.hash(input, isRaw);
    }

    public int totalSize() {
        return storage.values().stream().mapToInt(a -> a.block.length).sum();
    }
}