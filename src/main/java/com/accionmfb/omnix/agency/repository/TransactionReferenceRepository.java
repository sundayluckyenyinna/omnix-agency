package com.accionmfb.omnix.agency.repository;

import com.accionmfb.omnix.agency.model.TransactionReference;

public interface TransactionReferenceRepository {
    TransactionReference getByReferenceNumber(String referenceNumber);

    TransactionReference createTransactionReference(TransactionReference transactionReference);
}
