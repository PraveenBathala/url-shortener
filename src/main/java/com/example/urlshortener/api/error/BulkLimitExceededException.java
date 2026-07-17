package com.example.urlshortener.api.error;

import org.springframework.http.HttpStatus;

public class BulkLimitExceededException extends ApplicationException {

    public BulkLimitExceededException(int maxBatchSize) {
        super(
                ErrorCode.BULK_LIMIT_EXCEEDED,
                HttpStatus.BAD_REQUEST,
                "Bulk request exceeds max batch size of " + maxBatchSize);
    }
}
