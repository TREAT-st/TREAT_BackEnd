package com.example.demo.domain.userPortfolio.exception;

import com.example.demo.common.exception.BaseErrorCode;
import com.example.demo.common.exception.GeneralException;

public class UserPortfolioHandler extends GeneralException {
    public static final GeneralException NOT_FOUND
            = new UserPortfolioHandler(UserPortfolioErrorStatus.USER_PORTFOLIO_NOT_FOUND);

    public static final GeneralException ALREADY_EXISTS
            = new UserPortfolioHandler(UserPortfolioErrorStatus.USER_PORTFOLIO_ALREADY_EXISTS);

    public UserPortfolioHandler(BaseErrorCode baseErrorCode) {
        super(baseErrorCode);
    }
}
