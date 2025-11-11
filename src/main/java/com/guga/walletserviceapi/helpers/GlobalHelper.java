package com.guga.walletserviceapi.helpers;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class GlobalHelper {

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static Pageable getDefaultPageable() {
        return PageRequest.of(0, 200,
                sortDefaultByWalletAndTransaction()
        );
    }

    public static Sort sortDefaultByWalletAndTransaction() {
        return Sort.by(Sort.Order.asc("walletId"),
                Sort.Order.asc("transactionId")
        );
    }

}
