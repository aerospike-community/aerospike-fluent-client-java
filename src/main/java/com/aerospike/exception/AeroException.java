package com.aerospike.exception;

import java.util.List;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.policy.Policy;

public class AeroException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    protected transient Node node;
    protected transient Policy policy;
    protected List<AerospikeException> subExceptions;
    protected int resultCode = ResultCode.CLIENT_ERROR;
    protected int iteration = -1;
    protected boolean inDoubt;

    public AeroException(int resultCode, String message, boolean inDoubt) {
        super(message);
        this.resultCode = resultCode;
        this.inDoubt = inDoubt;
    }
    
    public AeroException(int resultCode, String message, Throwable e) {
        super(message, e);
        this.resultCode = resultCode;
    }
    public AeroException(int resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
    
    public static AeroException resultCodeToException(int resultCode, String message, boolean inDoubt) {
        switch (resultCode) {
        case ResultCode.QUOTA_EXCEEDED:
        case ResultCode.QUOTAS_NOT_ENABLED:
        case ResultCode.INVALID_QUOTA:
            return new QuotaException(resultCode, message, inDoubt);
        
        case ResultCode.INVALID_USER:
        case ResultCode.INVALID_PASSWORD:
        case ResultCode.INVALID_CREDENTIAL:
        case ResultCode.EXPIRED_PASSWORD:
        case ResultCode.NOT_AUTHENTICATED:
            return new AuthenticationException(resultCode, message, inDoubt);
            
        case ResultCode.ROLE_VIOLATION:
            return new AuthorizationException(resultCode, message, inDoubt);
            
        case ResultCode.ILLEGAL_STATE:
        case ResultCode.USER_ALREADY_EXISTS:
        case ResultCode.FORBIDDEN_PASSWORD:
        case ResultCode.SECURITY_NOT_ENABLED:
        case ResultCode.SECURITY_NOT_SUPPORTED:
        case ResultCode.SECURITY_SCHEME_NOT_SUPPORTED:
        case ResultCode.EXPIRED_SESSION:
        case ResultCode.INVALID_ROLE:
        case ResultCode.ROLE_ALREADY_EXISTS:
        case ResultCode.INVALID_PRIVILEGE:
        case ResultCode.INVALID_WHITELIST:
            return new SecurityException(resultCode, message, inDoubt);
            
        case ResultCode.GENERATION_ERROR:
            return new GenerationException(resultCode, message, inDoubt);
            
        default:
            return new AeroException(resultCode, message, inDoubt);
        }
    }
}
