package com.nubank.authorizer.businessRules.transactionRules;

import com.nubank.authorizer.businessRules.Rule;
import com.nubank.authorizer.entities.Account;
import com.nubank.authorizer.entities.AuthorizedTransaction;
import com.nubank.authorizer.entities.ValidatedTransaction;
import com.nubank.authorizer.enums.RuleValidator;
import com.nubank.authorizer.enums.TransactionType;

import java.util.List;
import java.util.Optional;

public class CardNotActive extends Rule {

    public CardNotActive(Rule nextRule) {
        super(nextRule);
    }

    @Override
    protected List<ValidatedTransaction> validate(List<ValidatedTransaction> data) {
        Optional<Account> accountInitializedOpt = Optional.empty();

        for (ValidatedTransaction currentItem : data) {
            if (currentItem.getTransactionType().equals(TransactionType.ACCOUNT)) {
                if (!accountInitializedOpt.isPresent()) {
                    accountInitializedOpt = Optional.of((Account) currentItem.getTransaction());
                }
            } else { // transaction
                if (accountInitializedOpt.isPresent() && !accountInitializedOpt.get().getActiveCard()) {
                    AuthorizedTransaction authorizedTransaction = currentItem.getAuthorizedTransaction();
                    authorizedTransaction.getViolations().add(RuleValidator.CARD_NOT_ACTIVE.getValidation());
                }
            }
        }

        return data;
    }
}