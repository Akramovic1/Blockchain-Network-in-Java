import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double inputsSum = 0.0, outputsSum = 0.0;
        Set<UTXO> consumedUTXO = new HashSet<>();
        Transaction.Input input;
        Transaction.Output output;
        for (int index = 0; index < tx.numInputs(); index++) {
            input = tx.getInput(index);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            output = this.utxoPool.getTxOutput(utxo);
            //Check all outputs are in the current UTXO pool, the signatures on each input are valid, and no UTXO is claimed multiple times
            if (!this.utxoPool.contains(utxo) || !Crypto.verifySignature(output.address, tx.getRawDataToSign(index), input.signature) || consumedUTXO.contains(utxo))
                return false;
            consumedUTXO.add(utxo);
            inputsSum += output.value;
        }
        for (int index = 0; index < tx.numOutputs(); index++) {
            output = tx.getOutput(index);
            //Check all of txs output values are non-negative
            if (output.value < 0) return false;
            outputsSum += output.value;
        }
        return (inputsSum >= outputsSum);
    }


    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> result = new ArrayList<>();
        for(Transaction transaction : possibleTxs){
            if(isValidTx(transaction)){
                result.add(transaction);
                removeFromPool(transaction);
                addToPool(transaction);
            }
        }
        return result.toArray(new Transaction[0]);
    }

    private void removeFromPool(Transaction transaction){
        for(Transaction.Input input : transaction.getInputs())
            this.utxoPool.removeUTXO(new UTXO(input.prevTxHash,input.outputIndex));
    }

    private void addToPool(Transaction transaction){
        for(int index = 0; index < transaction.numOutputs(); index++)
            this.utxoPool.addUTXO(new UTXO(transaction.getHash(), index), transaction.getOutput(index));
    }

    public UTXOPool getUTXOPool(){
        return this.utxoPool;
    }

}

//“I acknowledge that I am aware of the academic integrity guidelines of this course, and that I worked on this assignment independently without any unauthorized help” - <Ahmed Akram>
