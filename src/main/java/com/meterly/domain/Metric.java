package com.meterly.domain;

/** The kinds of usage we bill for. Stored in the database as text, e.g. "API_CALLS". */
public enum Metric {
    API_CALLS,
    AI_TOKENS,
    STORAGE,
    EGRESS
}
