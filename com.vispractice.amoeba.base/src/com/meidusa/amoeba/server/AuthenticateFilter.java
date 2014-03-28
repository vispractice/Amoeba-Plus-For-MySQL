package com.meidusa.amoeba.server;

import com.meidusa.amoeba.net.AuthResponseData;
import com.meidusa.amoeba.net.AuthingableConnection;

/**
 * @author struct
 */
public interface AuthenticateFilter {
    public boolean doFilte(AuthingableConnection conn, AuthResponseData rdata);
}
