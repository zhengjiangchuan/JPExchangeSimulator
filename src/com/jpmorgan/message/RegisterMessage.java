package com.jpmorgan.message;

import com.jpmorgan.client.ClientState;

public class RegisterMessage extends ExchangeMessage {

    private final ClientState clientState;
    private final long clientID;
    private final String reason;

    public RegisterMessage(ClientState clientState, long clientID, String reason)
    {
        this.clientState = clientState;
        this.clientID = clientID;
        this.reason = reason;
    }

    public String toString()
    {
        return "[clientState=" + clientState + ", clientID=" + clientID + ", reason=" + reason + "]";
    }


}