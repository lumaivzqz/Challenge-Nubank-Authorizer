package com.nubank.authorizer.businessRules;

import com.nubank.authorizer.businessRules.accountRules.AccountAlreadyInitialized;
import com.nubank.authorizer.businessRules.transactionRules.*;
import com.nubank.authorizer.entities.Account;
import com.nubank.authorizer.entities.AuthorizedTransaction;
import com.nubank.authorizer.entities.Transaction;
import com.nubank.authorizer.entities.ValidatedTransaction;
import com.nubank.authorizer.enums.TransactionType;
import com.nubank.authorizer.interfaces.GenericTransaction;
import com.nubank.authorizer.interfaces.RulesManager;

import java.util.ArrayList;
import java.util.List;

/**
 *  It is the business rules manager.
 */
public class BusinessRulesManager implements RulesManager {

    /**
     * Here will be the validation logic. The chain of responsibility pattern was applied to facilitate the validation of transactions.
     * @param data the list of objects (transactions and accounts) to be validated.
     * @return the list of validated transactions.
     */
    @Override
    public List<AuthorizedTransaction> runValidators(List<Object> data) {
        BusinessRule firstBusinessRule = getChainOfValidators();
        List<ValidatedTransaction> transactionsUnvalidated = formatData(data);
        List<ValidatedTransaction> validatedTransactions = firstBusinessRule.runValidator(transactionsUnvalidated);
        validatedTransactions = updateAvailableLimit(validatedTransactions);
        return formatValidatedTransactionList(validatedTransactions);
    }

    /**
     * This method defines the order of the validators according to the chain of responsibility pattern.
     * @return the chain of validators.
     */
    private BusinessRule getChainOfValidators() {
        BusinessRule highFrequencySmallInterval = new HighFrequencySmallInterval(null);
        BusinessRule doubleTransaction = new DoubleTransaction(highFrequencySmallInterval);
        BusinessRule insufficientLimit = new InsufficientLimit(doubleTransaction);
        BusinessRule cardNotActive = new CardNotActive(insufficientLimit);
        BusinessRule accountNotInitialized = new AccountNotInitialized(cardNotActive);
        BusinessRule accountAlreadyInitialized = new AccountAlreadyInitialized(accountNotInitialized);

        return accountAlreadyInitialized;
    }

    /**
     * Formats the data to a data type known to the rules.
     * @param data the data to be formatted.
     * @return the formatted data.
     */
    public List<ValidatedTransaction> formatData(List<Object> data) {
        List<ValidatedTransaction> validatedTransactions = new ArrayList<>();
        for (Object dataItem: data) {
            validatedTransactions.add(new ValidatedTransaction(
                    (GenericTransaction) dataItem,
                    new AuthorizedTransaction(null, null,new ArrayList<>())
            ));
        }
        return validatedTransactions;
    }

    /**
     * Updates the available limits in the list according to whether or not they have violations.
     * @param data the list of transactions to be updated.
     * @return the list of available limits updated on all transactions.
     */
    private List<ValidatedTransaction> updateAvailableLimit(List<ValidatedTransaction> data) {
        Boolean accountInitialized = false;
        Integer currentAvailableLimit = null;

        for (ValidatedTransaction currentItem: data) {
            if(currentItem.getTransactionType().equals(TransactionType.ACCOUNT)){
                // Determines the logic if it were an account TransactionType.
                if(!accountInitialized) {
                    accountInitialized = true;
                    Account account = (Account) currentItem.getTransaction();
                    currentAvailableLimit = account.getAvailableLimit();
                }
            } else {
                // Determines the logic if it were a transaction TransactionType.
                AuthorizedTransaction authorizedTransaction = currentItem.getAuthorizedTransaction();
                Transaction transaction = (Transaction) currentItem.getTransaction();
                if(authorizedTransaction.getViolations().isEmpty()) {
                    Integer newAvailableLimit = currentAvailableLimit - transaction.getAmount();
                    if (newAvailableLimit >= 0) {
                        currentAvailableLimit = newAvailableLimit;
                        authorizedTransaction.setAvailableLimit(currentAvailableLimit);
                    }
                } else {
                    authorizedTransaction.setAvailableLimit(currentAvailableLimit);
                }
            }
        }
        return data;
    }

    /**
     * Converts the list with all data into a simple list of authorized transactions.
     * @param validatedTransactions the uncompressed list of validated transactions.
     * @return the compressed list of validated transactions.
     */
    public List<AuthorizedTransaction> formatValidatedTransactionList(List<ValidatedTransaction> validatedTransactions) {
        List<AuthorizedTransaction> authorizedTransactions = new ArrayList<>();
        for (ValidatedTransaction validatedTransaction : validatedTransactions) {
            authorizedTransactions.add(validatedTransaction.getAuthorizedTransaction());
        }
        return authorizedTransactions;
    }
}
