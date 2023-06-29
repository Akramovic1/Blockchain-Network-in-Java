// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private Map<byte[], BlockNode> blockchain = new HashMap<>();
    private TransactionPool transactionPool = new TransactionPool();
    private MaxBlock maxBlock;


    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        //Handles transactions using handleTxs() from TxHandler class
        TxHandler txHandler = new TxHandler(new UTXOPool());
        txHandler.handleTxs(genesisBlock.getTransactions().toArray(new Transaction[0]));
        UTXOPool utxoPool = txHandler.getUTXOPool();
        //Add coinbase transactions
        addCoinbaseTxs(genesisBlock.getCoinbase(), utxoPool);
        blockchain.put(genesisBlock.getHash(), new BlockNode(genesisBlock, 1, utxoPool));
        //Set the max height block is genesis block
        maxBlock = new MaxBlock(1, genesisBlock.getHash());
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return blockchain.get(maxBlock.getHash()).getBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return blockchain.get(maxBlock.getHash()).getUTXOPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	 * <p>
	 * Assume the Genesis block is at height 1.
     * For example, you can try creating a new block over the genesis block (i.e. create a block at 
	 * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	 * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        //Check prev hash is valid, and it isn't genesis block
        byte[] prevHash = block.getPrevBlockHash();
        if(prevHash == null || !blockchain.containsKey(prevHash)) return false;
        //Check if height of new block is valid
        if(blockchain.get(prevHash).getHeight() + 1 <= maxBlock.getHeight() - CUT_OFF_AGE) return false;
        //Check if all transactions are valid
        TxHandler txHandler = new TxHandler(blockchain.get(prevHash).getUTXOPool());
        Transaction[] validTransactions = txHandler.handleTxs(block.getTransactions().toArray(new Transaction[0]));
        if(validTransactions.length != block.getTransactions().size()) return false;
        //Check the max height of current block
        int newHeight = maxHeightChecker( blockchain.get(prevHash).getHeight() + 1, block.getHash());
        //Handles transactions using handleTxs() from TxHandler class
        UTXOPool utxoPool = txHandler.getUTXOPool();
        //Add coinbase transactions
        addCoinbaseTxs(block.getCoinbase(), utxoPool);
        //Update blockchain
        blockchain.put(block.getHash(), new BlockNode(block, newHeight, utxoPool));
        //Remove transactions from transaction pool
        removeFromTransactionPool(block.getTransactions());
        //Remove old blocks from blockchain
        removeOldBlocks();
        return true;

    }

    private void removeOldBlocks(){
        ArrayList<byte[]> oldBlocks = new ArrayList<>();
        for(byte[] hash : blockchain.keySet())
            if(blockchain.get(hash).getHeight() < maxBlock.getHeight() - CUT_OFF_AGE)
                oldBlocks.add(hash);
        for(byte[] hash : oldBlocks)
            blockchain.remove(hash);
    }

    private void removeFromTransactionPool(ArrayList<Transaction> transactions){
        for(Transaction transaction : transactions)
            transactionPool.removeTransaction(transaction.getHash());
    }

    private int maxHeightChecker(int newHeight, byte[] hash){
        if(newHeight > maxBlock.getHeight()){
            maxBlock.setHash(hash);
            maxBlock.setHeight(newHeight);
        }
        return newHeight;
    }

    private void addCoinbaseTxs(Transaction coinbase, UTXOPool utxoPool){
        for(int index = 0; index < coinbase.numOutputs(); index++)
            utxoPool.addUTXO(new UTXO(coinbase.getHash(), index), coinbase.getOutput(index));
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

    private class BlockNode{
        Block block;
        int height;
        UTXOPool utxoPool;

        BlockNode(Block block, int height, UTXOPool utxoPool){
            this.block = block;
            this.height = height;
            this.utxoPool = utxoPool;
        }

        public Block getBlock() {
            return block;
        }

        public int getHeight(){
            return height;
        }

        public UTXOPool getUTXOPool(){
            return utxoPool;
        }
    }
    private class MaxBlock{
        private int height;
        private byte[] hash;
        MaxBlock(int height, byte[] hash){
            this.height = height;
            this.hash = hash;
        }
        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public byte[] getHash() {
            return hash;
        }

        public void setHash(byte[] hash) {
            this.hash = hash;
        }

    }
}

//“I acknowledge that I am aware of the academic integrity guidelines of this course, and that I worked on this assignment independently without any unauthorized help” - <Ahmed Akram>

