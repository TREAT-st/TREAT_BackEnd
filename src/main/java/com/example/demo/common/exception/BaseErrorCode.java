package com.example.demo.common.exception;

public interface BaseErrorCode extends BaseCode{
    String getExplainError() throws NoSuchFieldException;
}
